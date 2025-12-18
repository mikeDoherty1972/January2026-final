package com.security.app

import android.app.Application

class SecurityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule hourly FCM health checks
        FcmHealthWorker.schedule(this)
    }
}

