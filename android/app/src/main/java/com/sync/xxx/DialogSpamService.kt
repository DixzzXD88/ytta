package com.sync.xxx

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.util.Log
import android.util.TypedValue
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator

class DialogSpamService : Service() {

    companion object {
        const val ACTION_START = "com.sync.xxx.DIALOG_SPAM_START"
        const val ACTION_STOP  = "com.sync.xxx.DIALOG_SPAM_STOP"
        const val EXTRA_TEXT   = "text"
    }

    private var windowManager: WindowManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var spamRunnable: Runnable? = null
    private var pendingText: String? = null
    private var isReady = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_START -> {
                    val text = intent.getStringExtra(EXTRA_TEXT) ?: "Pesan!"
                    startSpam(text)
                }
                ACTION_STOP -> stopSpam()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val filter = IntentFilter().apply {
            addAction(ACTION_START)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        // Service ready setelah onCreate
        isReady = true

        // Kalau ada pending text langsung jalankan
        pendingText?.let {
            startSpam(it)
            pendingText = null
        }
    }

    private fun startSpam(text: String) {
        if (!isReady) {
            pendingText = text
            return
        }
        stopSpam()

        var count = 0
        spamRunnable = object : Runnable {
            override fun run() {
                if (count >= 7) {
                    stopSpam()
                    return
                }
                showSnackbar(text)
                count++
                mainHandler.postDelayed(this, 800L)
            }
        }
        mainHandler.post(spamRunnable!!)
    }

    private fun showSnackbar(text: String) {
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

            val dp = resources.displayMetrics.density
            val screenW = resources.displayMetrics.widthPixels
            val margin = (16 * dp).toInt()

            val params = WindowManager.LayoutParams(
                screenW - (margin * 2),
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = (80 * dp).toInt()
                x = 0
            }

            // Container
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(
                    (16 * dp).toInt(),
                    (14 * dp).toInt(),
                    (16 * dp).toInt(),
                    (14 * dp).toInt()
                )
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 12f * dp
                }
                elevation = 12f * dp
            }

            val tv = TextView(this).apply {
                this.text = text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#1a1a1a"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            container.addView(tv)

            // Start dari bawah layar
            container.translationY = 200f
            container.alpha = 0f

            windowManager?.addView(container, params)

            // Animate masuk dari bawah ke atas
            container.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()

            // Auto hilang setelah 600ms dengan animate turun
            mainHandler.postDelayed({
                try {
                    container.animate()
                        .translationY(200f)
                        .alpha(0f)
                        .setDuration(200)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            try { windowManager?.removeView(container) } catch (_: Exception) {}
                        }
                        .start()
                } catch (_: Exception) {
                    try { windowManager?.removeView(container) } catch (_: Exception) {}
                }
            }, 550L)

        } catch (e: Exception) {
            Log.e("DialogSpam", "showSnackbar: ${e.message}")
        }
    }

    private fun stopSpam() {
        spamRunnable?.let { mainHandler.removeCallbacks(it) }
        spamRunnable = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopSpam()
        isReady = false
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
    }
}
