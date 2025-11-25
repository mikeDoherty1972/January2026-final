package com.security.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmFullscreenActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        // simple in-memory rate-limit to avoid relaunch storms
        var lastAlarmZone: String? = null
        var lastAlarmTimeMs: Long = 0
        const val MIN_RESTART_MS = 30_000L // 30 seconds
    }

    @Suppress("DEPRECATION", "RedundantInitializer")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity full-screen and show over lockscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm)

        // Cancel any existing app notifications (prevent system from playing notification sounds)
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.cancelAll()
            android.util.Log.d("AlarmFullscreenActivity", "Cancelled existing app notifications before starting alarm activity")
        } catch (_: Exception) {}

        // Request audio focus for alarm playback so other audio (e.g., system ringtone) is paused/ducked
        var audioFocusRequest: Any? = null
        var audioManager: android.media.AudioManager? = null
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val afrBuilder = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { /* no-op */ }
                val afr = afrBuilder.build()
                val afRes = audioManager.requestAudioFocus(afr)
                audioFocusRequest = afr
                android.util.Log.d("AlarmFullscreenActivity", "Audio focus request result=$afRes")
            } else {
                val afRes = audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_ALARM, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                android.util.Log.d("AlarmFullscreenActivity", "Audio focus request result=$afRes")
            }
        } catch (e: Exception) {
            android.util.Log.w("AlarmFullscreenActivity", "Audio focus request failed: ${e.message}")
        }

        val messageView = findViewById<TextView>(R.id.alarmZoneText)
        val infoText = findViewById<TextView>(R.id.alarmInfoText)

        // Prefer a richer JSON payload if present. Expected format:
        // { "zone": "Zone name", "title": "Title", "message": "Details", "time_ms": 1234567890, "severity": "critical", "sensors": ["s1","s2"] }
        try {
            val alarmJson = intent.getStringExtra("alarm_json")
            if (!alarmJson.isNullOrEmpty()) {
                val obj = org.json.JSONObject(alarmJson)
                val zoneFromJson = obj.optString("zone", intent.getStringExtra("alarm_title") ?: intent.getStringExtra("zone") ?: "Unknown")
                val title = obj.optString("title", if (zoneFromJson.isNotEmpty()) zoneFromJson else "Alarm")
                val message = obj.optString("message", intent.getStringExtra("alarm_message") ?: "")
                val timeMs = obj.optLong("time_ms", intent.getLongExtra("alarm_time_ms", 0L))
                val severity = obj.optString("severity", "")
                val sensorsArr = obj.optJSONArray("sensors")

                val timeStr = if (timeMs > 0L) try { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timeMs)) } catch (_: Exception) { "" } else ""
                val header = if (timeStr.isNotEmpty()) "$timeStr — $title" else title
                val details = StringBuilder()
                if (message.isNotEmpty()) details.append(message)
                if (severity.isNotEmpty()) {
                    if (details.isNotEmpty()) details.append("\n")
                    details.append("Severity: $severity")
                }
                if (sensorsArr != null && sensorsArr.length() > 0) {
                    val sensors = mutableListOf<String>()
                    for (i in 0 until sensorsArr.length()) sensors.add(sensorsArr.optString(i))
                    if (sensors.isNotEmpty()) {
                        if (details.isNotEmpty()) details.append("\n")
                        details.append("Sensors: ${sensors.joinToString(", ")}")
                    }
                }

                messageView.text = getString(R.string.breach_format, zoneFromJson)
                infoText.text = if (details.isNotEmpty()) "$header\n${details.toString()}" else header

                // Update companion rate-limit fields so other code can avoid restart storms
                lastAlarmZone = zoneFromJson
                lastAlarmTimeMs = if (timeMs > 0L) timeMs else System.currentTimeMillis()
            } else {
                // Fallback to simple extras
                val zone = intent.getStringExtra("zone") ?: intent.getStringExtra("alarm_title") ?: "Unknown"
                val alarmMessage = intent.getStringExtra("alarm_message") ?: ""
                val alarmTimeMs = intent.getLongExtra("alarm_time_ms", 0L)
                val timeStr = if (alarmTimeMs > 0L) try { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(alarmTimeMs)) } catch (_: Exception) { "" } else ""
                messageView.text = getString(R.string.breach_format, zone)
                infoText.text = if (timeStr.isNotEmpty()) "$timeStr — $alarmMessage" else alarmMessage

                lastAlarmZone = zone
                lastAlarmTimeMs = if (alarmTimeMs > 0L) alarmTimeMs else System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Best-effort fallback
            android.util.Log.w("AlarmFullscreenActivity", "Failed to parse alarm_json: ${e.message}")
            val zone = intent.getStringExtra("zone") ?: intent.getStringExtra("alarm_title") ?: "Unknown"
            val alarmMessage = intent.getStringExtra("alarm_message") ?: ""
            val alarmTimeMs = intent.getLongExtra("alarm_time_ms", 0L)
            val timeStr = if (alarmTimeMs > 0L) try { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(alarmTimeMs)) } catch (_: Exception) { "" } else ""
            messageView.text = getString(R.string.breach_format, zone)
            infoText.text = if (timeStr.isNotEmpty()) "$timeStr — $alarmMessage" else alarmMessage
            lastAlarmZone = zone
            lastAlarmTimeMs = if (alarmTimeMs > 0L) alarmTimeMs else System.currentTimeMillis()
        }

        // Play bundled alarm with explicit alarm audio attributes (fallback to system ringtone)
        try {
            try {
                android.util.Log.d("AlarmFullscreenActivity", "Attempting to play bundled alarm_tone from res/raw")
                val afd = resources.openRawResourceFd(R.raw.alarm_tone)
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer?.isLooping = true
                mediaPlayer?.prepare()
                mediaPlayer?.start()
                android.util.Log.d("AlarmFullscreenActivity", "Started bundled MediaPlayer for alarm_tone")
            } catch (e: Exception) {
                android.util.Log.w("AlarmFullscreenActivity", "Failed to play bundled alarm_tone, falling back to system alarm: ${e.message}")
                // Fallback to system alarm sound via RingtoneManager
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(this, fallback)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                android.util.Log.d("AlarmFullscreenActivity", "Started fallback MediaPlayer with system alarm URI")
            }
        } catch (e: Exception) {
            mediaPlayer = null
        }

        // Vibrate continuously in a pattern
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        try {
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 250, 500), 0)
                }
            }
        } catch (_: Exception) {
        }

        findViewById<Button>(R.id.dismissAlarmButton).setOnClickListener {
            // abandon audio focus when dismissing
            try {
                if (audioManager != null && audioFocusRequest != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest as android.media.AudioFocusRequest)
                } else if (audioManager != null) {
                    audioManager.abandonAudioFocus(null)
                }
            } catch (_: Exception) {}

            stopAlarmAndFinish()
        }
    }

    private fun stopAlarmAndFinish() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (ignored: Exception) {
        }
        try {
            vibrator?.cancel()
        } catch (ignored: Exception) {
        }
        // reset rate-limit so a future alarm can be triggered
        lastAlarmZone = null
        lastAlarmTimeMs = 0
        finish()
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (ignored: Exception) {
        }
        try {
            vibrator?.cancel()
        } catch (ignored: Exception) {
        }
        super.onDestroy()
    }

    @Suppress("MissingSuperCall", "DEPRECATION")
    override fun onBackPressed() {
        // prevent back from dismissing the alarm accidentally
        // Intentionally do not call super.onBackPressed() so user must press Dismiss button.
    }
}
