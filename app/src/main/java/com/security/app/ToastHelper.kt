package com.security.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object ToastHelper {
    @Volatile
    private var currentToast: Toast? = null

    fun show(context: Context, text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        try {
            val appCtx = context.applicationContext
            Handler(Looper.getMainLooper()).post {
                try {
                    currentToast?.cancel()
                } catch (_: Exception) {}
                try {
                    currentToast = Toast.makeText(appCtx, text, duration).apply { show() }
                } catch (e: Exception) {
                    Log.w("ToastHelper", "Failed to show toast", e)
                }
            }
        } catch (e: Exception) {
            Log.w("ToastHelper", "Failed to post toast to main looper", e)
        }
    }
}

