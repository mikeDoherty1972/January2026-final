package com.security.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : BaseActivity() {
    
    private lateinit var db: FirebaseFirestore
    private lateinit var timestampText: TextView
    private lateinit var securityStatus: TextView
    private lateinit var scadaStatus: TextView
    private lateinit var idsStatus: TextView
    private lateinit var iperlStatus: TextView
    
    // Card views for alarm color changes
    private lateinit var securityCard: androidx.cardview.widget.CardView
    private lateinit var scadaCard: androidx.cardview.widget.CardView
    private lateinit var idsCard: androidx.cardview.widget.CardView
    private lateinit var iperlCard: androidx.cardview.widget.CardView

    // Dashboard lights controller views
    private var dashboardLightsOnButton: android.widget.Button? = null
    private var dashboardLightsOffButton: android.widget.Button? = null
    private var dashboardLightsStatusText: TextView? = null
    private var dashboardLightsStatusIcon: android.widget.ImageView? = null
    private var dashboardLightsDetails: TextView? = null

    // Firestore listener registrations for proper cleanup
    private var sensorListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var gaugeListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_dashboard)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()
        
        // Initialize views
        timestampText = findViewById(R.id.timestampText)
        securityStatus = findViewById(R.id.securityStatus)
        scadaStatus = findViewById(R.id.scadaStatus)
        idsStatus = findViewById(R.id.idsStatus)
        iperlStatus = findViewById(R.id.iperlStatus)
        
        // Initialize card views for alarm colors
        securityCard = findViewById(R.id.securityCard)
        scadaCard = findViewById(R.id.scadaCard)
        idsCard = findViewById(R.id.idsCard)
        iperlCard = findViewById(R.id.iperlCard)

        // Hook dashboard lights controller
        dashboardLightsOnButton = findViewById(R.id.dashboardLightsOnButton)
        dashboardLightsOffButton = findViewById(R.id.dashboardLightsOffButton)
        dashboardLightsStatusText = findViewById(R.id.dashboardLightsStatusText)
        dashboardLightsStatusIcon = findViewById(R.id.dashboardLightsStatusIcon)
        dashboardLightsDetails = findViewById(R.id.dashboardLightsDetails)

        // Write directly via LightsService (no navigation needed)
        dashboardLightsOnButton?.setOnClickListener {
            Log.d("MainActivity", "Dashboard Lights ON clicked — using LightsService")
            val ok = try { LightsService().writeOutsideLights(this@MainActivity, true) } catch (e: Exception) {
                Log.w("MainActivity", "LightsService ON failed", e); false
            }
            try {
                val msg = if (ok) getString(R.string.bridge_command_on_sent) else getString(R.string.failed_send_bridge_command, "service write failed")
                ToastHelper.show(this@MainActivity, msg, Toast.LENGTH_SHORT)
            } catch (_: Exception) {}
        }
        dashboardLightsOffButton?.setOnClickListener {
            Log.d("MainActivity", "Dashboard Lights OFF clicked — using LightsService")
            val ok = try { LightsService().writeOutsideLights(this@MainActivity, false) } catch (e: Exception) {
                Log.w("MainActivity", "LightsService OFF failed", e); false
            }
            try {
                val msg = if (ok) getString(R.string.bridge_command_off_sent) else getString(R.string.failed_send_bridge_command, "service write failed")
                ToastHelper.show(this@MainActivity, msg, Toast.LENGTH_SHORT)
            } catch (_: Exception) {}
        }

        // Apply per-card colors (reads prefs and falls back to defaults)
        applyCardColors()

        // Create notification channel
        createNotificationChannel()
        
        // Get FCM token and save to Firestore
        getFCMToken()

        // Listen for sensor status updates
        listenForSensorUpdates()

        // Pre-load all graph data
        GraphDataRepository.preLoadAllGraphData()

        // Start the notification service
        val serviceIntent = android.content.Intent(this, NotificationService::class.java)
        startService(serviceIntent)

        // Setup navigation
        setupNavigation()

        // Settings icon removed from layout; navigation to setup remains available from SCADA card

        // Setup swipe to refresh
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
            swipeRefreshLayout.isRefreshing = false
        }

        // Long-press timestamp to open AlarmTestActivity (quick access for debugging)
        findViewById<TextView>(R.id.timestampText).setOnLongClickListener {
            try {
                val intent = android.content.Intent(this, AlarmTestActivity::class.java)
                startActivity(intent)
            } catch (_: Exception) {}
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply per-card colors in case they were changed in a child activity
        applyCardColors()
    }

    private fun applyCardColors() {
        val prefsInit = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val securityPref = prefsInit.getString("card_security_color", null)
        val scadaPref = prefsInit.getString("card_scada_color", null)
        val idsPref = prefsInit.getString("card_ids_color", null)
        val iperlPref = prefsInit.getString("card_iperl_color", null)

        if (!securityPref.isNullOrEmpty()) {
            securityCard.setCardBackgroundColor(try { Color.parseColor(securityPref) } catch (e: IllegalArgumentException) { Color.parseColor("#2D5016") })
        } else {
            securityCard.setCardBackgroundColor(Color.parseColor("#2D5016"))
        }
        if (!scadaPref.isNullOrEmpty()) {
            scadaCard.setCardBackgroundColor(try { Color.parseColor(scadaPref) } catch (e: IllegalArgumentException) { Color.parseColor("#1A237E") })
        } else {
            scadaCard.setCardBackgroundColor(Color.parseColor("#1A237E"))
        }
        if (!idsPref.isNullOrEmpty()) {
            idsCard.setCardBackgroundColor(try { Color.parseColor(idsPref) } catch (e: IllegalArgumentException) { Color.parseColor("#BF360C") })
        } else {
            idsCard.setCardBackgroundColor(Color.parseColor("#BF360C"))
        }
        if (!iperlPref.isNullOrEmpty()) {
            iperlCard.setCardBackgroundColor(try { Color.parseColor(iperlPref) } catch (e: IllegalArgumentException) { Color.parseColor("#4A148C") })
        } else {
            iperlCard.setCardBackgroundColor(Color.parseColor("#4A148C"))
        }
    }

    private fun refreshData() {
        listenForSensorUpdates()
        GraphDataRepository.preLoadAllGraphData()
        try { ToastHelper.show(this, "Data refreshed", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
    }

    // New helper: pick a color and apply to all four main dashboard cards
    private fun showColorPickerForAllCards(dialogTitle: String) {
        val colors = arrayOf(
            "#000000", "#FFFFFF", "#800000", "#008000", "#808000", "#000080", "#800080", "#008080",
            "#C0C0C0", "#FF0000", "#00FF00", "#FFFF00", "#0000FF", "#FF00FF", "#00FFFF", "#808080"
        )
        val colorNames = arrayOf(
            "Black", "White", "Maroon", "Green", "Olive", "Navy", "Purple", "Teal",
            "Silver", "Red", "Lime", "Yellow", "Blue", "Fuchsia", "Aqua", "Gray"
        )

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        val adapter = ColorAdapter(this, colors, colorNames)
        builder.setAdapter(adapter) { _, which ->
            val selectedColor = colors[which]
            val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("card_security_color", selectedColor)
                putString("card_scada_color", selectedColor)
                putString("card_ids_color", selectedColor)
                putString("card_iperl_color", selectedColor)
                apply()
            }
            // Immediately apply to UI
            applyCardColors()
        }
        builder.show()
    }

    // New: show settings dialog with two pickers
    private fun showSettingsDialog() {
        val options = arrayOf("Card background color", "Graph background color")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Appearance settings")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showColorPickerForAllCards("Choose card background color")
                1 -> showColorPicker("graph_background_color", "Choose graph background color")
            }
        }
        builder.show()
    }

    private fun showColorPicker(prefKey: String, dialogTitle: String) {
        val colors = arrayOf(
            "#000000", "#FFFFFF", "#800000", "#008000", "#808000", "#000080", "#800080", "#008080",
            "#C0C0C0", "#FF0000", "#00FF00", "#FFFF00", "#0000FF", "#FF00FF", "#00FFFF", "#808080"
        )
        val colorNames = arrayOf(
            "Black", "White", "Maroon", "Green", "Olive", "Navy", "Purple", "Teal",
            "Silver", "Red", "Lime", "Yellow", "Blue", "Fuchsia", "Aqua", "Gray"
        )

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        val adapter = ColorAdapter(this, colors, colorNames)
        builder.setAdapter(adapter) { _, which ->
            val selectedColor = colors[which]
            val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString(prefKey, selectedColor)
                apply()
            }
            // Recreate the activity so card colors update immediately on the main screen.
            recreate()
        }
        builder.show()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.default_notification_channel_id)
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Try to delete existing channel if possible, but don't crash if system prevents it
            try {
                notificationManager.deleteNotificationChannel(channelId)
            } catch (se: SecurityException) {
                // Some OEM/Android versions prevent deleting a channel while a foreground service is active.
                // Log and proceed to create or update the channel instead.
                Log.w("MainActivity", "Could not delete notification channel (ignored): ${se.message}")
            } catch (e: Exception) {
                Log.w("MainActivity", "Unexpected error deleting notification channel (ignored): ${e.message}")
            }

            // Create or update channel with bundled alarm sound (res/raw/alarm_tone.mp3)
            val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}")
            val audioAttrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()
            val existing = notificationManager.getNotificationChannel(channelId)
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttrs)
            }
            if (existing == null) {
                notificationManager.createNotificationChannel(channel)
            } else {
                // Update settings on the existing channel where supported
                try {
                    existing.description = channel.description
                    existing.setSound(soundUri, audioAttrs)
                    existing.enableVibration(true)
                    notificationManager.createNotificationChannel(existing)
                } catch (e: Exception) {
                    Log.w("MainActivity", "Failed to update existing notification channel (ignored): ${e.message}")
                }
            }
          }
      }

    private inner class ColorAdapter(
        context: Context,
        private val colors: Array<String>,
        private val colorNames: Array<String>
    ) : ArrayAdapter<String>(context, R.layout.color_list_item, colorNames) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.color_list_item, parent, false)
            val colorSwatch = view.findViewById<View>(R.id.colorSwatch)
            val colorName = view.findViewById<TextView>(R.id.colorName)

            colorSwatch.setBackgroundColor(Color.parseColor(colors[position]))
            colorName.text = colorNames[position]

            return view
        }
    }
    
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                // Show explicit toast so user/dev sees failure
                runOnUiThread {
                    try { ToastHelper.show(this, "FCM token fetch failed: ${task.exception?.localizedMessage ?: "Unknown error"}", android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                }
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Registration Token: $token")

            if (token.isNullOrEmpty()) {
                Log.w("FCM", "FCM token is null or empty")
                runOnUiThread {
                    try { ToastHelper.show(this, "FCM token empty", android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                }
                return@addOnCompleteListener
            }

            try {
                // Save token to Firestore
                saveTokenToFirestore(token)

                // Show token in UI for debugging
                runOnUiThread {
                    try { ToastHelper.show(this, "FCM Token saved", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("FCM", "Exception while saving token", e)
                runOnUiThread {
                    try { ToastHelper.show(this, "FCM save failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                }
            }
        }
    }
    
    private fun saveTokenToFirestore(token: String) {
        val tokenData = hashMapOf(
            "token" to token,
            "updated" to com.google.firebase.Timestamp.now(),
            "platform" to "android"
        )
        
        db.collection("fcm_tokens")
            .document(token)
            .set(tokenData)
            .addOnSuccessListener {
                Log.d("Firestore", "Token saved successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving token", e)
            }
    }
    
    private fun listenForSensorUpdates() {
        // Listen for security sensor updates
        sensorListener = db.collection("security sensors")
            .document("live_status")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    updateUI(data)
                } else {
                    Log.d("Firestore", "Current data: null")
                }
            }
            
        // Listen for gauge/water meter updates
        gaugeListener = db.collection("security sensors")
            .document("gauge_status")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Gauge listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val gaugeData = snapshot.data
                    updateGaugeUI(gaugeData)
                } else {
                    Log.d("Firestore", "Gauge data: null")
                }
            }
    }
    
    private fun updateUI(data: Map<String, Any>?) {
        if (data == null) return
        
        val zones = mapOf(
            "Garage" to listOf("garage_motion", "garage_sensor"),
            "Garage Side" to listOf("garage_side_motion", "garage_side_sensor"),
            "South" to listOf("south_motion", "south_sensor"),
            "Back" to listOf("back_motion", "back_sensor"),
            "North" to listOf("north_motion", "north_sensor"),
            "Front" to listOf("front_motion", "front_sensor")
        )
        
        val statusBuilder = StringBuilder("Security Zone Status:\n\n")
        
        zones.forEach { (zoneName, sensors) ->
            val sensor1 = data[sensors[0]] as? Long ?: 0
            val sensor2 = data[sensors[1]] as? Long ?: 0
            
            val status = if (sensor1 == 1L && sensor2 == 1L) {
                "🚨 MOTION DETECTED"
            } else {
                "✅ CLEAR"
            }
            
            statusBuilder.append("$zoneName: $status\n")
        }
        
        // Add timestamp if available
        val timestamp = data["timestamp"]
        if (timestamp != null) {
            statusBuilder.append("\nLast Update: ${timestamp}")
        }
        
        runOnUiThread {
            // statusText removed from layout per UI update
            timestampText.text = "Last Update: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
            // Update individual status blocks
            updateStatusBlocks(data)
        }
    }
    
    private fun updateGaugeUI(gaugeData: Map<String, Any>?) {
        if (gaugeData == null) return
        // Robust parsing for water readings and RSSI
        fun safeDouble(value: Any?): Double = when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val mikeWaterReading = safeDouble(gaugeData["MikeWaterReading"])
        val allenWaterReading = safeDouble(gaugeData["AllenWaterReading"])
        val mikeRSSI = safeDouble(gaugeData["MikeRSSI"])
        val allenRSSI = safeDouble(gaugeData["AllenRSSI"])

        val metersOnline = listOf(mikeRSSI, allenRSSI).count { it != -999.0 }
        val waterDataAvailable = listOf(mikeWaterReading, allenWaterReading).count { it > 0 }

        iperlStatus.text = when {
            waterDataAvailable == 2 -> "2 Meters: ${String.format("%.0f", mikeWaterReading)}L / ${String.format("%.0f", allenWaterReading)}L"
            waterDataAvailable == 1 -> "1 Meter Reading Available"
            metersOnline > 0 -> "$metersOnline Meters Online"
            else -> "No Meter Data"
        }
    }

    private fun updateStatusBlocks(data: Map<String, Any>) {
        // Update Security Status with Alarm Detection
        val zones = mapOf(
            "Garage" to listOf("garage_motion", "garage_sensor"),
            "Garage Side" to listOf("garage_side_motion", "garage_side_sensor"),
            "South" to listOf("south_motion", "south_sensor"),
            "Back" to listOf("back_motion", "back_sensor"),
            "North" to listOf("north_motion", "north_sensor"),
            "Front" to listOf("front_motion", "front_sensor")
        )

        var activeZones = 0
        zones.forEach { (_, sensors) ->
            val sensor1 = data[sensors[0]] as? Long ?: 0
            val sensor2 = data[sensors[1]] as? Long ?: 0
            if (sensor1 == 1L || sensor2 == 1L) activeZones++
        }

        // Read preferred card background color (applied for non-alarm state)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val securityPref = prefs.getString("card_security_color", null)

        // Security Alarm Logic
        if (activeZones > 0) {
            securityStatus.text = "🔴 $activeZones Active Zones"
            securityCard.setCardBackgroundColor(Color.parseColor("#D32F2F")) // Red alarm
        } else {
            securityStatus.text = "🟢 6 Zones Armed"
            val colorToUse = try {
                if (!securityPref.isNullOrEmpty()) Color.parseColor(securityPref) else Color.parseColor("#2D5016")
            } catch (e: IllegalArgumentException) {
                Color.parseColor("#2D5016")
            }
            securityCard.setCardBackgroundColor(colorToUse)
        }

        // SCADA Alarm Logic - Check for sensor faults
        updateSCADAAlarms(data)

        // IDS Status - Check for network threats
        updateIDSAlarms(data)

        // IPERL Status - Check for water meter issues
        updateIPERLAlarms(data)
    }
    
    private fun updateSCADAAlarms(data: Map<String, Any>) {
        // Check temperature sensors
        val tempIn = (data["temp in"] as? Double) ?: 20.0
        val tempOut = (data["temp out"] as? Double) ?: 15.0
        val dvrTemp = (data["dvr_temp"] as? Double) ?: 25.0
        val power = (data["kw"] as? Double) ?: 0.0
        val amps = (data["amps"] as? Double) ?: 0.0
        
        val hasAlarm = when {
            tempIn > 35.0 || tempIn < 5.0 -> true // Indoor temp alarm
            tempOut > 45.0 || tempOut < -10.0 -> true // Outdoor temp alarm  
            dvrTemp > 45.0 -> true // DVR overheating
            power > 15.0 -> true // Power consumption too high
            amps > 60.0 -> true // Current draw too high
            else -> false
        }
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val scadaPref = prefs.getString("card_scada_color", null)

        if (hasAlarm) {
            scadaStatus.text = "🔴 System Fault"
            scadaCard.setCardBackgroundColor(Color.parseColor("#D32F2F")) // Red alarm
        } else {
            scadaStatus.text = "🟢 All Normal"
            val colorToUse = try {
                if (!scadaPref.isNullOrEmpty()) Color.parseColor(scadaPref) else Color.parseColor("#1A237E")
            } catch (e: IllegalArgumentException) {
                Color.parseColor("#1A237E")
            }
            scadaCard.setCardBackgroundColor(colorToUse)
        }
    }
    
    private fun updateIDSAlarms(data: Map<String, Any>) {
        // Simulate IDS threat detection (you can connect real IDS data later)
        val networkThreats = (data["network_threats"] as? Long) ?: 0
        val suspiciousActivity = (data["suspicious_activity"] as? Boolean) ?: false
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val idsPref = prefs.getString("card_ids_color", null)

        if (networkThreats > 0 || suspiciousActivity) {
            idsStatus.text = "🔴 Threats Detected"
            idsCard.setCardBackgroundColor(Color.parseColor("#D32F2F")) // Red alarm
        } else {
            idsStatus.text = "🟢 Controller Ready"
            val colorToUse = try {
                if (!idsPref.isNullOrEmpty()) Color.parseColor(idsPref) else Color.parseColor("#BF360C")
            } catch (e: IllegalArgumentException) {
                Color.parseColor("#BF360C")
            }
            idsCard.setCardBackgroundColor(colorToUse)
        }
    }
    
    private fun updateIPERLAlarms(data: Map<String, Any>) {
        // This will be updated when we integrate Google Sheets data
        // For now, simulate water meter alarms
        val waterPressure = (data["water_pressure"] as? Double) ?: 3.5
        val flowRate = (data["flow_rate"] as? Double) ?: 0.0
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val iperlPref = prefs.getString("card_iperl_color", null)

        val hasWaterAlarm = when {
            waterPressure < 1.5 || waterPressure > 6.0 -> true // Pressure alarm
            flowRate > 15.0 -> true // High flow alarm (possible leak)
            else -> false
        }
        
        if (hasWaterAlarm) {
            iperlStatus.text = "🔴 Water Alert"
            iperlCard.setCardBackgroundColor(Color.parseColor("#D32F2F")) // Red alarm
        } else {
            iperlStatus.text = "🟢 Meters Normal"
            val colorToUse = try {
                if (!iperlPref.isNullOrEmpty()) Color.parseColor(iperlPref) else Color.parseColor("#4A148C")
            } catch (e: IllegalArgumentException) {
                Color.parseColor("#4A148C")
            }
            iperlCard.setCardBackgroundColor(colorToUse)
        }
    }
    
    private fun setupNavigation() {
        // When a dashboard card is tapped, trigger the appropriate repository refresh so the
        // destination activity's graphs observe fresh data immediately.
        findViewById<androidx.cardview.widget.CardView>(R.id.securityCard).setOnClickListener {
            // Security screen doesn't host heavy graphs, but ensure repository preloads data
            GraphDataRepository.preLoadAllGraphData()
            val intent = android.content.Intent(this, SecurityActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<androidx.cardview.widget.CardView>(R.id.scadaCard).setOnClickListener {
            // Refresh SCADA-related graph sources before opening SCADA view
            GraphDataRepository.refreshPowerData()
            GraphDataRepository.refreshWeatherData()
            GraphDataRepository.refreshGeyserData()
            GraphDataRepository.refreshDvrData()
            val intent = android.content.Intent(this, ScadaActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<androidx.cardview.widget.CardView>(R.id.idsCard).setOnClickListener {
            // IDS doesn't have separate graph endpoints yet; perform a preload for safety
            GraphDataRepository.preLoadAllGraphData()
            val intent = android.content.Intent(this, IDSActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<androidx.cardview.widget.CardView>(R.id.iperlCard).setOnClickListener {
            // Force-refresh water meter CSV and parsing so IPERL graphs show latest values
            GraphDataRepository.refreshWaterMeterData(forceRefresh = true)
            val intent = android.content.Intent(this, IPERLActivity::class.java)
            startActivity(intent)
        }

         // Add long-press listeners to test alarms
         securityCard.setOnLongClickListener {
             testSecurityAlarm()
             true
         }

         scadaCard.setOnLongClickListener {
             testSCADAAlarm()
             true
         }

         idsCard.setOnLongClickListener {
             testIDSAlarm()
             true
         }

         iperlCard.setOnLongClickListener {
             testIPERLAlarm()
             true
         }
     }

    // Test alarm functions - Long press cards to test alarm states
    private fun testSecurityAlarm() {
        // Simulate motion detection
        val testData = mapOf(
            "garage_motion" to 1L,
            "south_motion" to 1L,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        updateStatusBlocks(testData)
        try { ToastHelper.show(this, "Security Alarm Test - Motion Detected", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}

        // Reset after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val normalData = mapOf(
                "garage_motion" to 0L,
                "south_motion" to 0L,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            updateStatusBlocks(normalData)
        }, 5000)
    }
    
    private fun testSCADAAlarm() {
        // Simulate high temperature alarm
        val testData = mapOf(
            "temp in" to 45.0, // High indoor temp
            "dvr_temp" to 50.0, // DVR overheating
            "kw" to 18.0, // High power consumption
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        updateStatusBlocks(testData)
        try { ToastHelper.show(this, "SCADA Alarm Test - High Temperature", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}

        // Reset after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val normalData = mapOf(
                "temp in" to 22.0,
                "dvr_temp" to 25.0,
                "kw" to 5.0,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            updateStatusBlocks(normalData)
        }, 5000)
    }
    
    private fun testIDSAlarm() {
        // Simulate network threat
        val testData = mapOf(
            "network_threats" to 3L,
            "suspicious_activity" to true,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        updateStatusBlocks(testData)
        try { ToastHelper.show(this, "IDS Alarm Test - Threats Detected", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}

        // Reset after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val normalData = mapOf(
                "network_threats" to 0L,
                "suspicious_activity" to false,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            updateStatusBlocks(normalData)
        }, 5000)
    }
    
    private fun testIPERLAlarm() {
        // Simulate water pressure alarm
        val testData = mapOf(
            "water_pressure" to 0.8, // Low pressure alarm
            "flow_rate" to 20.0, // High flow rate (possible leak)
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        updateStatusBlocks(testData)
        try { ToastHelper.show(this, "IPERL Alarm Test - Water Pressure Low", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}

        // Reset after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val normalData = mapOf(
                "water_pressure" to 3.5,
                "flow_rate" to 2.0,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            updateStatusBlocks(normalData)
        }, 5000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove Firestore listeners to prevent memory leaks
        try { sensorListener?.remove() } catch (_: Exception) {}
        try { gaugeListener?.remove() } catch (_: Exception) {}
    }
}
