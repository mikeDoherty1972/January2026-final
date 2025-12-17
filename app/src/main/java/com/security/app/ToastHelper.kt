package com.security.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object ToastHelper {
    @Volatile
    private var currentToast: Toast? = null
    @Volatile
    private var lastToastTime: Long = 0

    fun show(context: Context, text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        try {
            val appCtx = context.applicationContext
            val currentTime = System.currentTimeMillis()

            // Only run on main thread if we're already on main thread, otherwise post
            if (Looper.myLooper() == Looper.getMainLooper()) {
                showToastInternal(appCtx, text, duration, currentTime)
            } else {
                Handler(Looper.getMainLooper()).post {
                    showToastInternal(appCtx, text, duration, currentTime)
                }
            }
        } catch (e: Exception) {
            Log.w("ToastHelper", "Failed to show toast", e)
        }
    }

    private fun showToastInternal(context: Context, text: CharSequence, duration: Int, currentTime: Long) {
        try {
            // Only cancel previous toast if it's been less than 500ms since last toast
            // This prevents rapid cancellation issues
            if (currentTime - lastToastTime > 500) {
                currentToast?.cancel()
            }

            currentToast = Toast.makeText(context, text, duration)
            currentToast?.show()
            lastToastTime = currentTime
        } catch (e: Exception) {
            Log.w("ToastHelper", "Failed to show toast internal", e)
        }
    }
}

