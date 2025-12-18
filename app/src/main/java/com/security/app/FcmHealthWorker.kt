package com.security.app

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Periodic health check for Firebase Cloud Messaging.
 *
 * What it does:
 * - Fetches current FCM registration token.
 * - Compares with last stored token; if changed, stores it and resubscribes topics.
 * - Optionally can report status to logs/backend.
 */
class FcmHealthWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appCtx = applicationContext
        val prefs = appCtx.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

        return try {
            // Get the current token using coroutine Task await (we're already in a suspend function)
            val currentToken: String? = FirebaseMessaging.getInstance().token.await()

            val lastToken = prefs.getString(KEY_LAST_FCM_TOKEN, null)
            val changed = currentToken != null && currentToken != lastToken

            if (currentToken.isNullOrBlank()) {
                Log.w(TAG, "FCM Health: token is null/blank; will retry next cycle")
            } else {
                if (changed) {
                    Log.i(TAG, "FCM Health: token changed. Updating and resubscribing to topics")
                    prefs.edit().putString(KEY_LAST_FCM_TOKEN, currentToken).apply()
                    resubscribeTopicsSafely()
                } else {
                    Log.d(TAG, "FCM Health: token OK and unchanged")
                }
            }

            // Minimal success; WorkManager will schedule next run
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "FCM Health: check failed", t)
            Result.retry()
        }
    }

    private fun resubscribeTopicsSafely() {
        // Keep this list small and stable; you can customize
        val topics = listOf(
            "alerts-security",
            "alerts-water",
            "alerts-dvr"
        )
        topics.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnFailureListener { e -> Log.w(TAG, "Failed to subscribe to $topic", e) }
                .addOnSuccessListener { Log.d(TAG, "Subscribed to $topic") }
        }
    }

    companion object {
        private const val TAG = "FcmHealth"
        private const val UNIQUE_WORK = "fcm_health_check"
        private const val KEY_LAST_FCM_TOKEN = "last_fcm_token"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Use a 1-hour interval with some flex to let the scheduler batch efficiently
            val request = PeriodicWorkRequestBuilder<FcmHealthWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
