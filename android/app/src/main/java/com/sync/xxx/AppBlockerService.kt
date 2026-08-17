package com.sync.xxx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.content.pm.PackageManager

class AppBlockerService : AccessibilityService() {

    companion object {
        var instance: AppBlockerService? = null
        val blockedPackages = mutableSetOf<String>()

        fun isEnabled(ctx: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val component = "${ctx.packageName}/.AppBlockerService"
            val componentFull = "${ctx.packageName}/${ctx.packageName}.AppBlockerService"
            return enabledServices.split(":").any { svc ->
                svc.equals(component, ignoreCase = true) ||
                svc.equals(componentFull, ignoreCase = true)
            }
        }

        fun isUsageAccessGranted(ctx: Context): Boolean {
            return try {
                val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), ctx.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), ctx.packageName
                    )
                }
                mode == android.app.AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) { false }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastBlockedPkg = ""
    private var lastBlockTime = 0L

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.sync.xxx.BLOCK_APP" -> {
                    val pkg = intent.getStringExtra("package") ?: return
                    blockedPackages.add(pkg)
                    android.util.Log.d("AppBlocker", "Blocked: $pkg")
                }
                "com.sync.xxx.UNBLOCK_APP" -> {
                    val pkg = intent.getStringExtra("package") ?: return
                    blockedPackages.remove(pkg)
                    android.util.Log.d("AppBlocker", "Unblocked: $pkg")
                }
                "com.sync.xxx.UNBLOCK_ALL" -> {
                    blockedPackages.clear()
                    android.util.Log.d("AppBlocker", "All unblocked")
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 0 // 0 = secepat mungkin
        }

        val filter = IntentFilter().apply {
            addAction("com.sync.xxx.BLOCK_APP")
            addAction("com.sync.xxx.UNBLOCK_APP")
            addAction("com.sync.xxx.UNBLOCK_ALL")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
        android.util.Log.d("AppBlocker", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "android") return
        if (pkg == "com.android.systemui") return

        if (!blockedPackages.contains(pkg)) return

        // Anti spam — jangan block pkg yang sama dalam 1 detik
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockTime < 1000) return
        lastBlockedPkg = pkg
        lastBlockTime  = now

        // Langsung HOME
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Backup HOME 150ms kemudian biar ga balik lagi
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
        }, 50)

        // Toast
        val appName = try {
            val pm = packageManager
            pm.getApplicationLabel(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                else
                    @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }

        mainHandler.postDelayed({
            Toast.makeText(this, "🚫 $appName diblokir", Toast.LENGTH_SHORT).show()
        }, 50)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        instance = null
    }
}
