package com.security.app

import android.app.Service
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.Timer
import java.util.TimerTask
import android.app.PendingIntent
import android.os.Build
import android.annotation.SuppressLint
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.media.RingtoneManager

class NotificationService : Service() {

    companion object {
        // Actions the service can handle directly
        const val ACTION_TEST_ALARMS = "com.security.app.ACTION_TEST_ALARMS"
        const val ACTION_SECURITY_ALARM = "com.security.app.ACTION_SECURITY_ALARM"
        // Action used by ScadaActivity to push DVR pulse updates to the running service
        const val ACTION_UPDATE_DVR_PULSE = "com.security.app.ACTION_UPDATE_DVR_PULSE"
        var isRunning = false

        // Default fallback thresholds if prefs missing
        private const val DEFAULT_LOW_WATER_PRESSURE = 1.0
        private const val DEFAULT_HIGH_AMPS = 6.0
        private const val DEFAULT_HIGH_AMPS_DURATION_MS = 60 * 60 * 1000L
        private const val DEFAULT_WATER_VARIANCE = 0.1f
        private const val DEFAULT_DVR_STALE_MINUTES = 60L

        // Foreground notification id for the monitoring service
        private const val FOREGROUND_NOTIF_ID = 1000
    }

    private var dvrUpdateReceiver: android.content.BroadcastReceiver? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var sheetsReader: GoogleSheetsReader
    // Use DVR-specific reader to get the dedicated DVR gid values (keeps stale detection consistent with UI)
    private val dvrSheetsReader = DVRGoogleSheetsReader()
    private var timer: Timer? = null
    private val waterUsageHistory = mutableListOf<Double>()
    private var highAmpsStartTime = 0L

    // DVR pulse tracking (pulse value can be any numeric field we read from the sheet; here we use dvrTemp as a proxy if pulse not provided)
    private var lastDvrPulse: Double? = null
    private var lastDvrPulseTime = 0L

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        sheetsReader = GoogleSheetsReader()
        // Register a receiver so ScadaActivity can push DVR pulse updates to this running service
        try {
            val filter = android.content.IntentFilter(ACTION_UPDATE_DVR_PULSE)
            dvrUpdateReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: android.content.Intent?) {
                    try {
                        if (intent == null) return
                        val valDouble = intent.getDoubleExtra("last_dvr_pulse_value", Double.NaN)
                        val timeMs = intent.getLongExtra("last_dvr_pulse_time_ms", 0L)
                        if (!valDouble.isNaN() && timeMs > 0L) {
                            lastDvrPulse = valDouble
                            lastDvrPulseTime = timeMs
                            // persist to prefs so other processes see it
                            try {
                                val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                                alarmPrefs.edit().putFloat("last_dvr_pulse_value", lastDvrPulse!!.toFloat()).putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                            } catch (e: Exception) { android.util.Log.w("NotificationService", "Failed to persist DVR pulse from broadcast", e) }
                            android.util.Log.d("NotificationService", "Broadcast updated lastDvrPulse=$lastDvrPulse at $lastDvrPulseTime")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("NotificationService", "DVR update broadcast handling failed", e)
                    }
                }
            }
            // Register an unexported receiver (only for in-app broadcasts). Use the flag on API 33+;
            // on older platforms call the legacy overload and suppress the lint warning about missing flags.
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(dvrUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(dvrUpdateReceiver, filter)
                }
            } catch (e: Exception) {
                android.util.Log.w("NotificationService", "Receiver registration failed", e)
            }
         } catch (e: Exception) {
             android.util.Log.w("NotificationService", "Failed registering DVR update receiver", e)
         }
         // Notification channels are created centrally in App.onCreate() to avoid race
         // conditions and duplicated channel recreation. No-op here.

        // Sync last DVR pulse from persistent prefs so service doesn't treat a freshly-updated UI
        // pulse as stale when it starts or resumes.
        try {
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            if (prefs.contains("last_dvr_pulse_time_ms") && prefs.contains("last_dvr_pulse_value")) {
                val storedTime = prefs.getLong("last_dvr_pulse_time_ms", 0L)
                val storedVal = prefs.getFloat("last_dvr_pulse_value", 0f).toDouble()
                if (storedTime > 0L) {
                    lastDvrPulse = storedVal
                    lastDvrPulseTime = storedTime
                    android.util.Log.d("NotificationService", "Loaded persisted lastDvrPulse=$lastDvrPulse lastDvrPulseTime=$lastDvrPulseTime from prefs")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NotificationService", "Failed reading persisted last DVR pulse", e)
        }
    }

    private suspend fun loadLatestReading(): GoogleSheetsReader.SensorReading? {
        // If the app has configured a Drive file id in prefs, attempt to fetch CSV from Drive
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val fileId = prefs.getString("sensor_drive_file_id", null)
        if (!fileId.isNullOrEmpty()) {
            // Attempt to fetch via DriveCsvFetcher (use driveService if available via ScadaActivity.initializeDriveService)
            // driveService isn't directly available here; use public download fallback
            val csv = DriveCsvFetcher.fetchCsv(null, fileId)
            if (!csv.isNullOrEmpty()) {
                val parsed = GoogleSheetsReader().parseCsvStringToSensorReadings(csv, 1)
                if (parsed.isNotEmpty()) return parsed[0]
            }
        }

        // Primary: fetch the main sheet reading (contains waterPressure and other fields)
        try {
            val mainReadings = sheetsReader.fetchLatestReadings(1)
            if (!mainReadings.isNullOrEmpty()) {
                var main = mainReadings[0]
                // If DVR gid provides a fresher/more accurate dvrTemp, override only the dvrTemp field
                try {
                    val dvrRow = dvrSheetsReader.fetchLatestDvrReading()
                    if (dvrRow != null) {
                        // create a merged reading: copy main but replace dvrTemp
                        main = GoogleSheetsReader.SensorReading(
                            timestamp = main.timestamp,
                            indoorTemp = main.indoorTemp,
                            outdoorTemp = main.outdoorTemp,
                            humidity = main.humidity,
                            windSpeed = main.windSpeed,
                            windDirection = main.windDirection,
                            currentPower = main.currentPower,
                            currentAmps = main.currentAmps,
                            dailyPower = main.dailyPower,
                            dvrTemp = dvrRow.dvrTemp, // override from DVR gid
                            waterTemp = main.waterTemp,
                            waterPressure = main.waterPressure,
                            dailyWater = main.dailyWater
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("NotificationService", "DVR-specific reader failed (non-fatal)", e)
                }
                return main
            }
        } catch (e: Exception) {
            android.util.Log.w("NotificationService", "Failed fetching main sheet readings", e)
        }

        // Fallback: if main sheet unavailable, try DVR-only reader (best-effort for dvrTemp)
        try {
            val dvrRow = dvrSheetsReader.fetchLatestDvrReading()
            if (dvrRow != null) return dvrRow
        } catch (e: Exception) {
            android.util.Log.w("NotificationService", "DVR-specific reader failed (fallback)", e)
        }

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         // Handle service actions: test alarms and security alarm triggers
         val action = intent?.action
         if (action == ACTION_TEST_ALARMS) {
             android.util.Log.d("NotificationService", "Received ACTION_TEST_ALARMS: posting single full-screen test alarm")
             // Keep test alarms audible for developer testing
             sendNotification("TEST ALARM", "This is a single full-screen test alarm.", true, true)
             return START_NOT_STICKY
         }

         if (action == ACTION_SECURITY_ALARM) {
             android.util.Log.d("NotificationService", "Received ACTION_SECURITY_ALARM: posting security full-screen alarm")
             val i = intent
             if (i != null) {
                 val title = i.getStringExtra("alarm_title") ?: "Security Alarm"
                 val message = i.getStringExtra("alarm_message") ?: "Security breach detected"
                 // Security alarms should be audible on the full-screen alarm page
                 sendNotification(title, message, true, true)
             }
             return START_NOT_STICKY
         }

         startMonitoring()
         return START_STICKY
    }

    // Channels are managed by Application class (App.kt). No-op here to avoid duplicate
    // channel creation at service startup which can lead to inconsistent sound behavior.
    private fun createNotificationChannel() {
        // intentionally empty
    }

    private fun startMonitoring() {
        // Ensure we're running as a foreground service so system is more likely to allow activity launches
        ensureForeground()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                scope.launch {
                    checkSensorData()
                }
            }
        }, 0, 60000) // Check every minute
    }

    private fun ensureForeground() {
        try {
            val channelId = getString(R.string.default_notification_channel_id)
            val builder = NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Monitoring for alarms")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)

            // On Android O+ starting a foreground service requires a call to startForeground
            startForeground(FOREGROUND_NOTIF_ID, builder.build())
        } catch (e: Exception) {
            android.util.Log.w("NotificationService", "Failed to start foreground notification", e)
        }
    }

    private suspend fun checkSensorData() {
        try {
            // Load thresholds from SharedPreferences each run so changes are immediate
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val lowPressure = prefs.getFloat("low_pressure_threshold", DEFAULT_LOW_WATER_PRESSURE.toFloat()).toDouble()
            val highAmps = prefs.getFloat("high_amps_threshold", DEFAULT_HIGH_AMPS.toFloat()).toDouble()
            val highAmpsDuration = prefs.getLong("high_amps_duration_ms", DEFAULT_HIGH_AMPS_DURATION_MS)
            val waterVariance = prefs.getFloat("water_variance_threshold", DEFAULT_WATER_VARIANCE)
            val dvrStaleMinutes = prefs.getLong("dvr_stale_minutes", DEFAULT_DVR_STALE_MINUTES)

            val latestReading = loadLatestReading()

            // Re-sync persisted DVR pulse/time in case UI (ScadaActivity) updated it while the service is running.
            try {
                val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                if (alarmPrefs.contains("last_dvr_pulse_time_ms") && alarmPrefs.contains("last_dvr_pulse_value")) {
                    val storedTime = alarmPrefs.getLong("last_dvr_pulse_time_ms", 0L)
                    val storedVal = alarmPrefs.getFloat("last_dvr_pulse_value", 0f).toDouble()
                    if (storedTime > lastDvrPulseTime) {
                        lastDvrPulseTime = storedTime
                        lastDvrPulse = storedVal
                        android.util.Log.d("NotificationService", "Resynced lastDvrPulse from prefs: $lastDvrPulse at $lastDvrPulseTime")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("NotificationService", "Failed to resync last DVR pulse from prefs", e)
            }

            if (latestReading != null) {
                // Check low water pressure
                val wp = latestReading.waterPressure.toDouble()
                if (wp <= 0.0) {
                    // Missing or invalid water pressure reading — skip low-pressure alert and log for diagnosis
                    android.util.Log.d("NotificationService", "Skipping low-pressure check: waterPressure=$wp (missing/invalid)")
                } else if (wp < lowPressure) {
                    // Deduplicate: only send when not already alerted
                    try {
                        val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                        val wasActive = alarmPrefs.getBoolean("low_pressure_alert_active", false)
                        val lastAlertMs = alarmPrefs.getLong("low_pressure_last_alert_ms", 0L)
                        val cooldownMs = alarmPrefs.getLong("low_pressure_cooldown_ms", 15 * 60 * 1000L) // default 15 minutes
                        val now = System.currentTimeMillis()
                        if (!wasActive || (now - lastAlertMs) > cooldownMs) {
                            // Use full-screen page (big red) but silent for non-security system alerts
                            sendNotification("Low Water Pressure Alert", "Water pressure is below ${lowPressure} bar! Current: ${String.format(java.util.Locale.getDefault(), "%.2f", wp)} bar", true, false)
                            try { alarmPrefs.edit().putBoolean("low_pressure_alert_active", true).putLong("low_pressure_last_alert_ms", now).apply() } catch (_: Exception) {}
                        } else {
                            android.util.Log.d("NotificationService", "Low pressure already alerted; skipping repeat. wp=$wp threshold=$lowPressure")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("NotificationService", "Failed handling low-pressure deduplication", e)
                        sendNotification("Low Water Pressure Alert", "Water pressure is below ${lowPressure} bar! Current: ${String.format(java.util.Locale.getDefault(), "%.2f", wp)} bar", true, false)
                    }
                } else {
                    // Pressure is OK — clear any active low-pressure alert flag so future low events will alert again
                    try {
                        val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                        val wasActive = alarmPrefs.getBoolean("low_pressure_alert_active", false)
                        if (wasActive) {
                            alarmPrefs.edit().putBoolean("low_pressure_alert_active", false).apply()
                            // Optionally send a recovery/clear notification (silent)
                            sendNotification("Water Pressure Restored", "Water pressure is back to ${String.format(java.util.Locale.getDefault(), "%.2f", wp)} bar", true, false)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("NotificationService", "Failed clearing low-pressure alert flag", e)
                    }
                }

                // Check high amps
                if (latestReading.currentAmps.toDouble() > highAmps) {
                    if (highAmpsStartTime == 0L) highAmpsStartTime = System.currentTimeMillis()
                    if (System.currentTimeMillis() - highAmpsStartTime >= highAmpsDuration) {
                        // System alert: non-security
                        sendNotification("High Electricity Usage Alert", "Electricity usage has been over ${highAmps} amps for the configured duration!", true, false)
                        highAmpsStartTime = 0L
                    }
                } else {
                    highAmpsStartTime = 0L
                }

                // Check constant water usage (use dailyWater field)
                checkConstantWaterUsage(latestReading.dailyWater.toDouble(), waterVariance)

                // DVR pulse stale detection
                // Use numeric comparison for pulse so small formatting/rounding doesn't suppress real changes
                val pulseVal = latestReading.dvrTemp.toDouble()
                val tol = prefs.getFloat("dvr_pulse_tolerance", 0.01f).toDouble()
                if (lastDvrPulse == null) {
                    lastDvrPulse = pulseVal
                    lastDvrPulseTime = System.currentTimeMillis()
                    // persist initial pulse so UI and other processes can see it
                    try {
                        val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                        alarmPrefs.edit().putFloat("last_dvr_pulse_value", lastDvrPulse!!.toFloat()).putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                    } catch (e: Exception) {
                        android.util.Log.w("NotificationService", "Failed to persist initial last DVR pulse", e)
                    }
                } else {
                    val changed = kotlin.math.abs(pulseVal - lastDvrPulse!!) > tol
                    if (!changed) {
                        val minutesElapsed = (System.currentTimeMillis() - lastDvrPulseTime) / 60000L
                        if (minutesElapsed >= dvrStaleMinutes) {
                            // System-level DVR stale: non-security, post silently (no full-screen)
                            sendNotification("DVR Stale Pulse Alert", "DVR pulse has not changed in $minutesElapsed minutes (configured $dvrStaleMinutes minutes).", true, false)
                            // reset timer to avoid repeated notifications
                            lastDvrPulseTime = System.currentTimeMillis()
                            try {
                                val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                                alarmPrefs.edit().putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                            } catch (e: Exception) {
                                android.util.Log.w("NotificationService", "Failed to persist last DVR pulse time after stale alert", e)
                            }
                        }
                    } else {
                        lastDvrPulse = pulseVal
                        lastDvrPulseTime = System.currentTimeMillis()
                        // persist updated pulse so UI and other processes can see it
                        try {
                            val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                            alarmPrefs.edit().putFloat("last_dvr_pulse_value", lastDvrPulse!!.toFloat()).putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                        } catch (e: Exception) {
                            android.util.Log.w("NotificationService", "Failed to persist updated last DVR pulse", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Error in checkSensorData", e)
        }
    }

    private fun checkConstantWaterUsage(latestWaterUsage: Double, varianceThreshold: Float) {
        waterUsageHistory.add(latestWaterUsage)
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val windowHours = prefs.getInt("water_hourly_window_hours", 1)
        val windowSamples = (windowHours * 60).coerceAtLeast(1)

        if (waterUsageHistory.size > windowSamples) {
            while (waterUsageHistory.size > windowSamples) waterUsageHistory.removeAt(0)
            val firstUsage = waterUsageHistory.first()
            val difference = latestWaterUsage - firstUsage

            // Read absolute volume threshold from prefs (default 500 units)
            val hourlyThreshold = prefs.getFloat("water_hourly_volume_threshold", 500f).toDouble()

            // If absolute increase exceeds threshold, raise a distinct alarm
            if (difference >= hourlyThreshold) {
                // System-level alert: non-security
                sendNotification("High Water Usage Alert", "Water usage increased by ${String.format("%.1f", difference)} units in the last ${windowHours} hour(s) (threshold ${String.format("%.1f", hourlyThreshold)}).", true, false)
                // return early to avoid double alarming (variance check is less relevant if absolute threshold hit)
                return
            }

            if (difference > 0 && waterUsageHistory.all { it >= firstUsage }) {
                val rate = difference / waterUsageHistory.size.toDouble()
                val variance = waterUsageHistory.mapIndexed { idx, v ->
                    val expected = firstUsage + rate * idx
                    (v - expected) * (v - expected)
                }.average()
                if (variance < varianceThreshold) {
                    // System-level alert: non-security
                    sendNotification("Constant Water Usage Alert", "Water usage has been constant for the last ${windowHours} hour(s). Possible pipe burst!", true, false)
                }
            }
        }
    }

    private fun ensureSecurityAlertChannel(notificationManager: NotificationManager) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val chId = "security_alerts"
                val existing = notificationManager.getNotificationChannel(chId)
                if (existing == null) {
                    val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}")
                    val audioAttrs = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                    val secChannel = android.app.NotificationChannel(chId, "Security Alerts", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notifications for security and critical system alerts"
                        setSound(soundUri, audioAttrs)
                        enableLights(true)
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(secChannel)
                    android.util.Log.d("NotificationService", "Created security_alerts notification channel with bundled alarm tone")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NotificationService", "Failed ensuring security_alerts channel", e)
        }
    }

    /**
     * Send a notification.
     * - fullScreen=true: starts AlarmFullscreenActivity directly (keeps previous behavior)
     * - audible=true: use the security_alerts channel (with bundled mp3) for non-fullscreen alerts
     */
    private fun sendNotification(title: String, message: String, fullScreen: Boolean = false, audible: Boolean = false) {
         // Record alarm in recent alarms history as JSON objects (keep last 5)
         try {
             val prefs = getSharedPreferences("alarm_history", Context.MODE_PRIVATE)
             val key = "recent_alarms_json"
             val existing = prefs.getString(key, "[]") ?: "[]"
             val arr = org.json.JSONArray(existing)
             val obj = org.json.JSONObject()
             obj.put("time_ms", System.currentTimeMillis())
             obj.put("title", title)
             obj.put("message", message)
             // Optional richer fields - left blank or defaults here
             obj.put("zone", org.json.JSONObject.NULL)
             obj.put("severity", if (fullScreen) "critical" else "warning")
             obj.put("sensors", org.json.JSONArray())

             val newArr = org.json.JSONArray()
             newArr.put(obj)
             var i = 0
             while (i < arr.length() && newArr.length() < 5) {
                 try { newArr.put(arr.getJSONObject(i)) } catch (_: Exception) {}
                 i++
             }
             prefs.edit().putString(key, newArr.toString()).apply()
         } catch (e: Exception) {
             android.util.Log.w("NotificationService", "Failed to record alarm history", e)
         }

         val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
         // Choose channel:
         // - fullScreen -> keep previous flow (silent channel + direct activity launch)
         // - non-fullscreen audible -> use 'security_alerts' channel with bundled mp3
         // - non-fullscreen silent/default -> use default channel id
         val channelId = when {
             fullScreen && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> "security_notifications_silent"
             !fullScreen && audible -> {
                 ensureSecurityAlertChannel(notificationManager)
                 "security_alerts"
             }
             else -> getString(R.string.default_notification_channel_id)
         }

         val builder = NotificationCompat.Builder(this, channelId)
             .setContentTitle(title)
             .setContentText(message)
             .setSmallIcon(R.drawable.ic_notification)
             .setPriority(NotificationCompat.PRIORITY_HIGH)
             .setAutoCancel(true)
             .setCategory(NotificationCompat.CATEGORY_ALARM)

         // For full-screen alarms we intentionally do NOT set a notification sound here.
         // The notification is posted to a silent channel and the app's AlarmFullscreenActivity
         // is explicitly started to play the bundled `res/raw/alarm_tone.mp3` via MediaPlayer.
         if (fullScreen) {
             android.util.Log.d("NotificationService", "Full-screen alarm: posting to channel=$channelId without builder.setSound; activity will play audio")
         } else {
             // For non-full-screen notifications we can leave the default channel behavior (channel may have sound)
             android.util.Log.d("NotificationService", "Notification (non-fullscreen) will use channel=$channelId")
         }

         // Debug: explicitly log the channel sound for visibility (API 26+)
         try {
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                 val ch = notificationManager.getNotificationChannel(channelId)
                 android.util.Log.d("NotificationService", "Channel '$channelId' sound=${ch?.sound}")
             }
         } catch (_: Exception) {}

         // If this is a full-screen alarm, start the AlarmFullscreenActivity directly and skip posting a notification.
         // Posting a full-screen notification can cause the system to play its own alarm sound on some devices. Starting
         // the activity directly guarantees only the app's bundled MediaPlayer plays the alarm.
         if (fullScreen) {
            try {
                val directIntent = Intent(this, AlarmFullscreenActivity::class.java)
                // Legacy extras for compatibility
                directIntent.putExtra("alarm_title", title)
                directIntent.putExtra("alarm_message", message)
                directIntent.putExtra("alarm_time_ms", System.currentTimeMillis())

                // Build richer JSON payload so full-screen activity always has structured data
                try {
                    val alarmObj = org.json.JSONObject()
                    alarmObj.put("zone", title)
                    alarmObj.put("title", title)
                    alarmObj.put("message", message)
                    val nowMs = System.currentTimeMillis()
                    alarmObj.put("time_ms", nowMs)
                    alarmObj.put("severity", if (fullScreen) "critical" else "warning")
                    alarmObj.put("sensors", org.json.JSONArray())
                    directIntent.putExtra("alarm_json", alarmObj.toString())
                } catch (e: Exception) {
                    android.util.Log.w("NotificationService", "Failed to build alarm_json payload", e)
                }
                // Indicate whether the full-screen activity should play the bundled sound
                try { directIntent.putExtra("play_sound", audible) } catch (_: Exception) {}

                 directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                 android.os.Handler(android.os.Looper.getMainLooper()).post {
                     try {
                         startActivity(directIntent)
                         android.util.Log.d("NotificationService", "Started AlarmFullscreenActivity directly for full-screen alarm (no notification posted)")
                     } catch (e: Exception) {
                         android.util.Log.e("NotificationService", "Failed to start AlarmFullscreenActivity directly", e)
                     }
                 }
            } catch (e: Exception) {
                android.util.Log.e("NotificationService", "Failed to prepare direct AlarmFullscreenActivity intent", e)
            }
            return
        }

        // Create content intent for non-fullscreen notification: open app when tapped
        try {
            val alarmIntent = Intent(this, MainActivity::class.java)
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            val pending = PendingIntent.getActivity(this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), alarmIntent, flags)
            builder.setContentIntent(pending)
        } catch (e: Exception) {
            android.util.Log.w("NotificationService", "Failed to create content intent for notification", e)
        }

        val notif = builder.build()
        // Notify with a unique id
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notifId, notif)
        android.util.Log.d("NotificationService", "Sent notification: $title - $message (fullScreen=$fullScreen) notifId=$notifId channel=$channelId")

        // If this is a full-screen alarm, cancel the posted notification shortly after posting.
        // This prevents the system from concurrently playing a separate phone alarm while our activity plays the bundled sound.
        if (fullScreen) {
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        notificationManager.cancel(notifId)
                        android.util.Log.d("NotificationService", "Cancelled full-screen notification id=$notifId to avoid duplicate system sound")
                    } catch (_: Exception) {}
                }, 500)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { if (dvrUpdateReceiver != null) { unregisterReceiver(dvrUpdateReceiver); dvrUpdateReceiver = null } } catch (_: Exception) {}
        isRunning = false
        timer?.cancel()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
