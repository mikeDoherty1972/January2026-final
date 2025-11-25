package com.security.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        var title: String = "🚨 SECURITY ALERT 🚨"
        var body: String = "Motion detected!"

        // Check if message contains data payload (preferred for custom sound)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Data map returns nullable Strings; only replace when non-null
            remoteMessage.data["title"]?.let { title = it }
            remoteMessage.data["body"]?.let { body = it }
            sendNotification(title, body)
            return
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val nTitle = it.title ?: title
            val nBody = it.body ?: body
            sendNotification(nTitle, nBody)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(title: String, messageBody: String) {
        // Append to alarm history as JSON objects (keep last 5)
        try {
            val prefs = getSharedPreferences("alarm_history", Context.MODE_PRIVATE)
            val key = "recent_alarms_json"
            val existing = prefs.getString(key, "[]") ?: "[]"
            val arr = org.json.JSONArray(existing)
            val obj = org.json.JSONObject()
            obj.put("time_ms", System.currentTimeMillis())
            obj.put("title", title)
            obj.put("message", messageBody)
            obj.put("zone", org.json.JSONObject.NULL)
            obj.put("severity", "warning")
            obj.put("sensors", org.json.JSONArray())

            val newArr = org.json.JSONArray()
            newArr.put(obj)
            var i = 0
            while (i < arr.length() && newArr.length() < 5) {
                try { newArr.put(arr.getJSONObject(i)) } catch (_: Exception) {}
                i++
            }
            prefs.edit().putString(key, newArr.toString()).apply()
        } catch (_: Exception) {}

        // Determine if this incoming FCM should trigger a full-screen alarm
        val isFullScreen = try {
            title.contains("alert", ignoreCase = true) || messageBody.contains("both sensors", ignoreCase = true) || messageBody.contains("active", ignoreCase = true)
        } catch (_: Exception) { false }

        if (isFullScreen) {
            // Build alarm_json payload (mirror NotificationService behavior) and start full-screen activity
            try {
                val nowMs = System.currentTimeMillis()
                val alarmObj = org.json.JSONObject()
                alarmObj.put("zone", title)
                alarmObj.put("title", title)
                alarmObj.put("message", messageBody)
                alarmObj.put("time_ms", nowMs)
                alarmObj.put("severity", "critical")
                alarmObj.put("sensors", org.json.JSONArray())

                val directIntent = Intent(this, AlarmFullscreenActivity::class.java)
                directIntent.putExtra("alarm_title", title)
                directIntent.putExtra("alarm_message", messageBody)
                directIntent.putExtra("alarm_time_ms", nowMs)
                directIntent.putExtra("alarm_json", alarmObj.toString())
                directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        startActivity(directIntent)
                        Log.d(TAG, "Started AlarmFullscreenActivity directly for FCM full-screen alarm")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start AlarmFullscreenActivity from FCM", e)
                    }
                }

                // We intentionally do not post a notification for full-screen FCM messages
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch full-screen alarm from FCM: ${e.message}")
                // Fall through to posting a regular notification below
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        // Choose channel: security channel for alerts, default silent otherwise
        val securityChannelId = "security_alerts"
        val defaultChannelId = getString(R.string.default_notification_channel_id)
        val channelToUse = if (title.contains("security", ignoreCase = true) || title.contains("alert", ignoreCase = true)) securityChannelId else defaultChannelId

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = System.currentTimeMillis().toInt()

        val notificationBuilder = NotificationCompat.Builder(this, channelToUse)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setDefaults(0) // Don't use default sound, use channel sound
            .addAction(
                R.drawable.ic_notification,
                "Acknowledge",
                createAcknowledgeIntent(notificationId)
            )

        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Security alarm notification sent with ID: $notificationId (channel=$channelToUse)")
    }

    private fun createAcknowledgeIntent(notificationId: Int): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java)
        intent.action = "ACKNOWLEDGE_ALARM"
        intent.putExtra("notification_id", notificationId)
        return PendingIntent.getBroadcast(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}