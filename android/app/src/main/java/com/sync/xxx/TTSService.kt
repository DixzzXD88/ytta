package com.sync.xxx

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_SPEAK = "com.sync.xxx.TTS_SPEAK"
        const val ACTION_STOP  = "com.sync.xxx.TTS_STOP"
        const val EXTRA_TEXT   = "text"
        const val EXTRA_LANG   = "lang"  // "id" = Indonesia, "en" = English
        const val EXTRA_PITCH  = "pitch" // default 1.0
        const val EXTRA_SPEED  = "speed" // default 1.0
    }

    private var tts: TextToSpeech? = null
    private var pendingText: String? = null
    private var pendingLang: String  = "id"
    private var pendingPitch: Float  = 1.0f
    private var pendingSpeed: Float  = 1.0f

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SPEAK -> {
                    val text  = intent.getStringExtra(EXTRA_TEXT)  ?: return
                    val lang  = intent.getStringExtra(EXTRA_LANG)  ?: "id"
                    val pitch = intent.getFloatExtra(EXTRA_PITCH, 1.0f)
                    val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                    speak(text, lang, pitch, speed)
                }
                ACTION_STOP -> stopSpeak()
            }
        }
    }

    override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startForeground(99, buildNotification())
    tts = TextToSpeech(this, this)

        val filter = IntentFilter().apply {
            addAction(ACTION_SPEAK)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Kalau ada pending text, langsung speak
            pendingText?.let {
                speak(it, pendingLang, pendingPitch, pendingSpeed)
                pendingText = null
            }
        } else {
            Log.e("TTSService", "TTS init failed")
        }
    }

    private fun speak(text: String, lang: String, pitch: Float, speed: Float) {
        val ttsEngine = tts ?: return
        val locale = when (lang.lowercase()) {
            "en" -> Locale.ENGLISH
            "id" -> Locale("id", "ID")
            else -> Locale("id", "ID")
        }

        val result = ttsEngine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback ke default
            ttsEngine.setLanguage(Locale.getDefault())
        }

        ttsEngine.setPitch(pitch)
        ttsEngine.setSpeechRate(speed)
        ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    private fun stopSpeak() {
        tts?.stop()
    }
    
    
    private fun buildNotification(): android.app.Notification =
    androidx.core.app.NotificationCompat.Builder(this, "tts_channel")
        .setContentTitle("TTS Active")
        .setContentText("")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
        .build()

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val ch = android.app.NotificationChannel(
            "tts_channel", "TTS Service",
            android.app.NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(android.app.NotificationManager::class.java)
            .createNotificationChannel(ch)
    }
}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
    }
}
