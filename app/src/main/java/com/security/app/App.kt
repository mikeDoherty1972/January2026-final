package com.security.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = getSystemService(NotificationManager::class.java)
                if (notificationManager != null) {
                    val defaultChannelId = getString(R.string.default_notification_channel_id)
                    val securityChannelId = "security_alerts"
                    val silentChannelId = "security_notifications_silent"

                    // Delete existing channels to ensure our settings take effect
                    try { notificationManager.deleteNotificationChannel(defaultChannelId) } catch (_: Exception) {}
                    try { notificationManager.deleteNotificationChannel(securityChannelId) } catch (_: Exception) {}
                    try { notificationManager.deleteNotificationChannel(silentChannelId) } catch (_: Exception) {}

                    val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}")
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()

                    // Default channel: silent for all non-security notifications
                    val defaultChannel = NotificationChannel(defaultChannelId, "App Notifications", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "General app notifications (silent)"
                        // Explicitly no sound so only security channel plays audio
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(true)
                    }
                    notificationManager.createNotificationChannel(defaultChannel)

                    // Security channel: high importance with bundled alarm sound
                    val secChannel = NotificationChannel(securityChannelId, "Security Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notifications for security breaches"
                        setSound(soundUri, audioAttrs)
                        enableLights(true)
                        enableVibration(true)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    }
                    notificationManager.createNotificationChannel(secChannel)

                    // Silent channel used for full-screen notifications - the activity will play the sound
                    val silentChannel = NotificationChannel(silentChannelId, "Security Alerts (Silent)", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Silent channel for full-screen alarms (app plays alarm tone)"
                        setSound(null, null)
                        enableVibration(false)
                    }
                    notificationManager.createNotificationChannel(silentChannel)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("App", "Failed to ensure alarm channels", e)
        }
    }
}
