package com.security.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACKNOWLEDGE_ALARM" -> {
                Log.d("NotificationAction", "Alarm acknowledged")
                
                // Get the notification ID from the intent
                val notificationId = intent.getIntExtra("notification_id", 0)
                
                // Cancel the notification using the correct ID
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
                
                // Show acknowledgment message using ToastHelper
                try { ToastHelper.show(context, "Alarm acknowledged", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}

                Log.d("NotificationAction", "Notification $notificationId dismissed")
            }
        }
    }
}