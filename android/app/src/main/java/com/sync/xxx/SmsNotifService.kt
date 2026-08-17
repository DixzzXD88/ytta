package com.sync.xxx

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject

class SmsNotifService : NotificationListenerService() {

    companion object {
        fun isEnabled(ctx: android.content.Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(ctx.packageName)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // skip notif sendiri

        try {
            val extras = sbn.notification?.extras ?: return
            val title  = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text   = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
                ?: ""

            if (text.isBlank()) return

            // Ambil nama app yang readable
            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (_: Exception) { pkg }

            val payload = JSONObject().apply {
                put("pkg",     pkg)
                put("appName", appName)
                put("title",   title)
                put("text",    text)
                put("time",    System.currentTimeMillis())
            }

            val intent = android.content.Intent("com.sync.xxx.NEW_NOTIF").apply {
                setPackage(packageName)
                putExtra("payload", payload.toString())
            }
            sendBroadcast(intent)

        } catch (e: Exception) {
            Log.e("SmsNotifService", "onNotificationPosted: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
