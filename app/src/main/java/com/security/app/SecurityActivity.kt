package com.security.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog

import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import kotlinx.coroutines.*

class SecurityActivity : BaseActivity() {
    
    private lateinit var db: FirebaseFirestore
    private lateinit var systemStatusText: TextView
    private lateinit var garageStatus: TextView
    private lateinit var garageSideStatus: TextView
    private lateinit var southStatus: TextView
    private lateinit var backStatus: TextView
    private lateinit var northStatus: TextView
    private lateinit var frontStatus: TextView
    private lateinit var doorStatus: TextView
    private lateinit var securityAlarmsSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var securityScheduleSummary: TextView
    private lateinit var systemStatusCard: androidx.cardview.widget.CardView
    private lateinit var securityForceSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var securityForceOffSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var recentActivityText: TextView

    // Network indicator views (promoted so we can refresh them from lifecycle callbacks)
    private var networkHomeDot: android.widget.ImageView? = null
    private var networkBridgeDot: android.widget.ImageView? = null
    private var networkHomeDotInline: android.widget.ImageView? = null
    // Optional debug text view (present in layout if developer added it); shows SSID/method/gateway for quick debugging
    private var networkDebugText: TextView? = null

    // Small in-memory cache to keep recent activity between update cycles
    private val recentActivityCache: MutableList<String> = mutableListOf()

    // Classify whether a log line is security-related. Heuristics based on common zone/door events
    private fun isSecurityLine(line: String?): Boolean {
        if (line.isNullOrBlank()) return false
        val s = line.lowercase(Locale.getDefault())
        // Explicit tag or common security terms
        return s.contains("security") ||
               s.contains("zone") || s.contains("door") || s.contains("garage") || s.contains("front") ||
               s.contains("back") || s.contains("north") || s.contains("south") || s.contains("alarm") ||
               s.contains("breach") || s.contains("motion")
    }

    private fun filteredSecurityActivityAll(): List<String> {
        return recentActivityCache.filter { isSecurityLine(it) }
    }

    private fun renderRecentActivityPreview() {
        val lines = filteredSecurityActivityAll()
        if (lines.isNotEmpty()) {
            recentActivityText.text = lines.takeLast(5).reversed().joinToString("\n")
            try { recentActivityText.setTextColor(resources.getColor(android.R.color.white, theme)) } catch (_: Exception) {}
        } else {
            recentActivityText.text = "No recent activity"
            try { recentActivityText.setTextColor(resources.getColor(android.R.color.darker_gray, theme)) } catch (_: Exception) {}
        }
    }

    // Firestore listener registration for proper cleanup
    private var securityListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val CHANNEL_ID = "security_alerts"
    private val NOTIF_ID = 1001

    companion object {
        private const val REQ_POST_NOTIF = 2001
        private const val REQ_FINE_LOCATION = 3001

        // New default silent channel id for non-security notifications
        private const val DEFAULT_CHANNEL_ID = "default_notifications"
    }

    // Cached SSID value populated by NetworkCallback to avoid direct heavy use of deprecated WifiInfo getter
    @Volatile
    private var lastKnownSsid: String? = null

    // NetworkCallback used to monitor connectivity and update SSID when Wi-Fi is active
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Helper: check whether a gateway IP is within the RFC1918 172.16.0.0/12 private range
    private fun is172PrivateRange(ip: String?): Boolean {
        try {
            if (ip.isNullOrEmpty()) return false
            val parts = ip.split('.')
            if (parts.size < 2) return false
            val first = parts[0].toIntOrNull() ?: return false
            val second = parts[1].toIntOrNull() ?: return false
            return first == 172 && second in 16..31
        } catch (e: Exception) {
            return false
        }
    }

    // Actively fetch and cache SSID once (useful at startup when NetworkCallback hasn't fired yet)
    private fun fetchAndCacheSsidOnce(onComplete: (() -> Unit)? = null) {
        try {
            // Ensure we have permission to read SSID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SecurityActivity", "fetchAndCacheSsidOnce: no location permission, requesting")
                ensureLocationPermissionForSsid { fetchAndCacheSsidOnce(onComplete) }
                return
            }
            // Run on background thread to avoid blocking UI
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val active = cm?.activeNetwork
                    var ssid: String? = null
                    if (active != null) {
                        val caps = cm.getNetworkCapabilities(active)
                        if (caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                            try {
                                // Use the network-backed helper which prefers modern APIs and falls back safely
                                val helperSsid = readSsidFromWifiManager()
                                if (!helperSsid.isNullOrEmpty()) ssid = helperSsid
                            } catch (e: Exception) {
                                Log.w("SecurityActivity", "fetchAndCacheSsidOnce: wifi read failed", e)
                            }
                        }
                    }
                    if (!ssid.isNullOrEmpty()) {
                        lastKnownSsid = ssid
                        Log.d("SecurityActivity", "fetchAndCacheSsidOnce: cached ssid=$ssid")
                    } else {
                        Log.d("SecurityActivity", "fetchAndCacheSsidOnce: no SSID available")
                    }
                } catch (e: Exception) {
                    Log.w("SecurityActivity", "fetchAndCacheSsidOnce failed", e)
                } finally {
                    try { withContext(Dispatchers.Main) { onComplete?.invoke() } } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w("SecurityActivity", "fetchAndCacheSsidOnce outer failed", e)
            onComplete?.invoke()
        }
    }

    // Helper: request runtime location permission if needed to read connected SSID
    private fun ensureLocationPermissionForSsid(onGranted: () -> Unit) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_FINE_LOCATION)
                    return
                }
            }
            onGranted()
        } catch (e: Exception) {
            Log.w("SecurityActivity", "ensureLocationPermissionForSsid failed", e)
        }
    }

    private fun startSsidMonitoring() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            // Avoid registering multiple times
            if (networkCallback != null) return
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    try {
                        val caps = cm.getNetworkCapabilities(network)
                        if (caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                            try {
                                // Prefer network-backed SSID read helper
                                val rawSsid = readSsidFromWifiManager()
                                if (!rawSsid.isNullOrEmpty()) {
                                  lastKnownSsid = rawSsid
                                } else {
                                  lastKnownSsid = null
                                }
                                Log.d("SecurityActivity", "NetworkCallback onAvailable: ssid=${lastKnownSsid}")
                                try { runOnUiThread { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) } } catch (_: Exception) {}
                            } catch (e: Exception) {
                                Log.w("SecurityActivity", "NetworkCallback failed reading SSID", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("SecurityActivity", "NetworkCallback onAvailable error", e)
                    }
                }

                override fun onLost(network: Network) {
                    try {
                        // If Wi-Fi lost, clear cached SSID
                        lastKnownSsid = null
                        Log.d("SecurityActivity", "NetworkCallback onLost: cleared cached SSID")
                        try { runOnUiThread { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) } } catch (_: Exception) {}
                    } catch (_: Exception) {}
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: android.net.NetworkCapabilities) {
                    try {
                        if (networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                            try {
                                // Prefer network-backed SSID read helper
                                val rawSsid = readSsidFromWifiManager()
                                if (!rawSsid.isNullOrEmpty()) {
                                  lastKnownSsid = rawSsid
                                } else {
                                  lastKnownSsid = null
                                }
                                Log.d("SecurityActivity", "NetworkCallback onCapabilitiesChanged: ssid=${lastKnownSsid}")
                                try { runOnUiThread { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) } } catch (_: Exception) {}
                            } catch (e: Exception) {
                                Log.w("SecurityActivity", "NetworkCallback capabilities read SSID failed", e)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            networkCallback = cb
            try {
                val req = android.net.NetworkRequest.Builder()
                    .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                cm.registerNetworkCallback(req, cb)
                Log.d("SecurityActivity", "Registered SSID NetworkCallback")
                // Kick off an active single-shot fetch to populate lastKnownSsid quickly at startup
                fetchAndCacheSsidOnce(null)
            } catch (e: Exception) {
                Log.w("SecurityActivity", "Failed to register NetworkCallback for SSID monitoring", e)
                networkCallback = null
            }
        } catch (e: Exception) {
            Log.w("SecurityActivity", "startSsidMonitoring failed", e)
        }
    }

    private fun stopSsidMonitoring() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val cb = networkCallback ?: return
            try { cm.unregisterNetworkCallback(cb) } catch (e: Exception) { Log.w("SecurityActivity", "Failed to unregister NetworkCallback", e) }
            networkCallback = null
            lastKnownSsid = null
            Log.d("SecurityActivity", "Unregistered SSID NetworkCallback")
        } catch (e: Exception) {
            Log.w("SecurityActivity", "stopSsidMonitoring failed", e)
        }
    }

    // Helper: read current connected SSID (may return null if unavailable)
    private fun readSsidFromWifiManager(): String? {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            // First, try to use WifiManager.getConnectionInfo(Network) if available (API 30+); use reflection so code compiles on older APIs.
            try {
                val active = cm?.activeNetwork
                if (active != null && wifiManager != null) {
                    try {
                        val m = wifiManager.javaClass.getMethod("getConnectionInfo", Network::class.java)
                        val wifiInfo = m.invoke(wifiManager, active)
                        val raw = wifiInfo?.javaClass?.getMethod("getSSID")?.invoke(wifiInfo) as? String
                        if (!raw.isNullOrEmpty()) {
                          val cleaned = raw.trim('\"','\'').trim()
                          val lower = cleaned.lowercase(Locale.getDefault())
                          if (lower == "<unknown ssid>" || lower == "unknown" || lower.startsWith("0x") || lower == "<none>") return null
                          return cleaned
                        }
                    } catch (e: NoSuchMethodException) {
                        // method not available on this platform, fall through
                    } catch (e: Exception) {
                        Log.w("SecurityActivity", "readSsidFromWifiManager reflection read failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.w("SecurityActivity", "readSsidFromWifiManager active-network attempt failed", e)
            }

            // Modern network-backed method not available: as a last resort attempt deprecated WifiManager.connectionInfo
            // but only when we have runtime ACCESS_FINE_LOCATION permission. Suppress deprecation warning for this block.
            try {
                val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ContextCompat.checkSelfPermission(this@SecurityActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED else true
                if (hasPerm) {
                    try {
                        @Suppress("DEPRECATION")
                        val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        @Suppress("DEPRECATION")
                        val info = wifiMgr?.connectionInfo
                        val raw = info?.ssid
                        if (!raw.isNullOrEmpty()) {
                            val cleaned = raw.trim('\"','\'').trim()
                            val lower = cleaned.lowercase(Locale.getDefault())
                            if (lower == "<unknown ssid>" || lower == "unknown" || lower.startsWith("0x") || lower == "<none>") return null
                            Log.d("SecurityActivity", "readSsidFromWifiManager: deprecated connectionInfo fallback used")
                            return cleaned
                        }
                    } catch (e: Exception) {
                        Log.w("SecurityActivity", "readSsidFromWifiManager deprecated fallback failed", e)
                    }
                } else {
                    Log.d("SecurityActivity", "readSsidFromWifiManager: no location permission for deprecated fallback")
                }
            } catch (e: Exception) {
                Log.w("SecurityActivity", "readSsidFromWifiManager permission check failed", e)
            }
            return null
        } catch (e: Exception) {
            Log.w("SecurityActivity", "readSsidFromWifiManager failed", e)
            return null
        }
    }

    private fun getCurrentSsid(): String? {
        try {
            val cached = lastKnownSsid
            if (!cached.isNullOrEmpty()) return cached
            // Try the new helper which prefers network-backed read
            val s = readSsidFromWifiManager()
            if (!s.isNullOrEmpty()) return s
            return null
        } catch (e: Exception) {
            Log.w("SecurityActivity", "getCurrentSsid failed", e)
            return null
        }
    }

    // Save current SSID as the configured home SSID
    private fun saveCurrentSsidAsHome() {
        ensureLocationPermissionForSsid {
            try {
                val ssid = getCurrentSsid()
                if (ssid != null) {
                    getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putString("home_ssid", ssid).apply()
                    try { ToastHelper.show(this, "Saved home SSID: $ssid", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                } else {
                    try { ToastHelper.show(this, "Unable to read SSID", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w("SecurityActivity", "saveCurrentSsidAsHome failed", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)
        // Start SSID monitoring to populate a reliable cached SSID
        startSsidMonitoring()
        Log.d("SecurityActivity", "onCreate: SecurityActivity started")

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        
        // Initialize views
        systemStatusText = findViewById(R.id.systemStatusText)
        garageStatus = findViewById(R.id.garageStatus)
        garageSideStatus = findViewById(R.id.garageSideStatus)
        southStatus = findViewById(R.id.southStatus)
        backStatus = findViewById(R.id.backStatus)
        northStatus = findViewById(R.id.northStatus)
        frontStatus = findViewById(R.id.frontStatus)
        doorStatus = findViewById(R.id.doorStatus)
        recentActivityText = findViewById(R.id.recentActivityText)

        // Restore persisted recent activity (if any)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val persisted = prefs.getString("security_recent_activity", null)
        if (!persisted.isNullOrEmpty()) {
            // Normalize line endings and remove any accidental empty lines
            val normalized = persisted.replace("\r\n", "\n").replace("\r", "\n").trim('\n')
            val items = if (normalized.isEmpty()) emptyList() else normalized.split("\n")
            recentActivityCache.clear()
            recentActivityCache.addAll(items)
            // Show newest activity at the top: take the last 5 (most recent) then reverse so latest is first
            try {
                renderRecentActivityPreview()
            } catch (e: Exception) {
                Log.w("SecurityActivity", "Failed setting recentActivityText from persisted data", e)
            }
            Log.d("SecurityActivity", "Loaded ${recentActivityCache.size} persisted recentActivity entries")
        }

        // Backfill: merge any FCM alarm history entries received while app was backgrounded
        try {
            val alarmPrefs = getSharedPreferences("alarm_history", Context.MODE_PRIVATE)
            val key = "recent_alarms_json"
            val json = alarmPrefs.getString(key, "[]") ?: "[]"
            val arr = try { org.json.JSONArray(json) } catch (_: Exception) { org.json.JSONArray("[]") }
            if (arr.length() > 0) {
                val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                val newLines = mutableListOf<String>()
                var i = 0
                while (i < arr.length()) {
                    try {
                        val obj = arr.getJSONObject(i)
                        val t = obj.optLong("time_ms", System.currentTimeMillis())
                        val zone = obj.optString("zone", "Security")
                        val msg = obj.optString("message", obj.optString("title", "Activity"))
                        val timeStr = fmt.format(java.util.Date(t))
                        newLines.add("[$timeStr] $zone: $msg")
                    } catch (_: Exception) {}
                    i++
                }
                if (newLines.isNotEmpty()) {
                    // Append and persist, de-duplicating trivial consecutive duplicates
                    newLines.forEach { line ->
                        val last = recentActivityCache.lastOrNull()
                        if (last == null || last != line) recentActivityCache.add(line)
                    }
                    // Trim to last 50 entries
                    if (recentActivityCache.size > 50) {
                        val removeCount = recentActivityCache.size - 50
                        repeat(removeCount) { recentActivityCache.removeAt(0) }
                    }
                    prefs.edit().putString("security_recent_activity", recentActivityCache.joinToString("\n")).apply()
                    // Refresh UI
                    try {
                        renderRecentActivityPreview()
                    } catch (_: Exception) {}
                    Log.d("SecurityActivity", "Backfilled ${newLines.size} FCM alarms into recent activity")
                }
            }
        } catch (e: Exception) {
            Log.w("SecurityActivity", "Failed to backfill FCM alarm history", e)
        }

        // Setup back button
        findViewById<TextView>(R.id.securityBackButton).setOnClickListener {
            finish()
        }

        // Security alarms switch
        securityAlarmsSwitch = findViewById(R.id.securityAlarmsSwitch)
        val enabledPref = prefs.getBoolean("security_alarms_enabled", true)
        securityAlarmsSwitch.isChecked = enabledPref
        securityAlarmsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("security_alarms_enabled", isChecked).apply()
            try { ToastHelper.show(this, if (isChecked) "Security alarms enabled" else "Security alarms disabled", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
            updateScheduleUi()
        }

        // Re-bind force switches to the new IDs in the layout (clearer labels in XML)
        securityForceSwitch = findViewById(R.id.securityForceAwaySwitch)
        val forcePref = prefs.getBoolean("security_alarms_force_on", false)
        securityForceSwitch.isChecked = forcePref
        securityForceSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("security_alarms_force_on", isChecked).apply()
            try { ToastHelper.show(this, if (isChecked) "Security alarms forced ON" else "Security alarms no longer forced", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
            updateScheduleUi()
        }

        securityForceOffSwitch = findViewById(R.id.securityForceHomeSwitch)
        val forceOffPref = prefs.getBoolean("security_alarms_force_off", false)
        securityForceOffSwitch.isChecked = forceOffPref
        securityForceOffSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("security_alarms_force_off", isChecked).apply()
            try { ToastHelper.show(this, if (isChecked) "Security alarms forced OFF" else "Security alarms no longer forced OFF", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
            updateScheduleUi()
        }

        // Bind schedule summary and status card for dimming
        securityScheduleSummary = findViewById(R.id.securityScheduleSummary)
        systemStatusCard = findViewById(R.id.systemStatusCard)
        updateScheduleUi()

        // Bind the spanner icon + advanced panel and network LEDs
         try {
             val spanner = findViewById<android.widget.ImageView>(R.id.securitySettingsSpanner)
             val advancedPanel = findViewById<android.widget.LinearLayout>(R.id.securityAdvancedPanel)
             // Assign to class members so we can refresh them later
             networkHomeDot = findViewById<android.widget.ImageView>(R.id.securityNetworkHomeDot)
             networkBridgeDot = findViewById<android.widget.ImageView>(R.id.securityNetworkBridgeDot)
             // Inline dot placed on the main card header so users see network state without opening advanced panel
             networkHomeDotInline = findViewById<android.widget.ImageView?>(R.id.securityNetworkHomeDotInline)

             // Bind optional debug text view if present in layout (id: securityNetworkDebugText)
             try {
                 val dbgId = resources.getIdentifier("securityNetworkDebugText", "id", packageName)
                 if (dbgId != 0) {
                     try { networkDebugText = findViewById<TextView>(dbgId) } catch (_: Exception) { networkDebugText = null }
                 }
             } catch (_: Exception) { networkDebugText = null }

            // Wire quick action buttons
            try {
                val refreshId = resources.getIdentifier("refreshNetworkButton", "id", packageName)
                if (refreshId != 0) {
                    try {
                        findViewById<android.widget.Button>(refreshId).setOnClickListener {
                            try { ToastHelper.show(this, "Refreshing network status...", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                            // Active fetch then update indicators when done
                            fetchAndCacheSsidOnce { runOnUiThread { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) } }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            try {
                val saveId = resources.getIdentifier("saveHomeButton", "id", packageName)
                if (saveId != 0) {
                    try {
                        findViewById<android.widget.Button>(saveId).setOnClickListener {
                            // Prompt user: will request permission if needed
                            saveCurrentSsidAsHome()
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

              // Initialize network indicators immediately
             updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline)

             // Toggle the advanced panel when spanner is clicked
             spanner?.setOnClickListener {
                 try {
                     if (advancedPanel.visibility == android.view.View.GONE) {
                         advancedPanel.visibility = android.view.View.VISIBLE
                         // refresh indicators when opening
                         updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline)
                     } else {
                         advancedPanel.visibility = android.view.View.GONE
                     }
                 } catch (e: Exception) {
                     Log.w("SecurityActivity", "Failed toggling advanced panel", e)
                 }
             }
         } catch (_: Exception) {}

        createNotificationChannel()

        // Request runtime POST_NOTIFICATIONS permission on Android 13+
        requestNotificationPermissionIfNeeded()

        // Listen for real-time security updates
        listenForSecurityUpdates()

        // Setup refresh listener
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            listenForSecurityUpdates()
            swipeRefreshLayout.isRefreshing = false
        }

        // Setup click listeners for camera views
        setupCameraClickListeners()

        // Long-press on the recent activity area to change appearance (card color / graph bg)
        recentActivityText.setOnLongClickListener {
            val options = arrayOf("Card background color", "Graph background color")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Security appearance")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> showColorPicker("card_security_color", "Choose Security card color") {
                        try { ToastHelper.show(this, "Security card color saved. Return to dashboard to see change.", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                    }
                    1 -> showColorPicker("graph_background_color", "Choose Security graph background") {
                        try { ToastHelper.show(this, "Security graph background saved.", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                    }
                }
            }
            builder.show()
            true
        }

        // Wire presence check button
        try {
            // Resolve the id at runtime to avoid a compile-time reference to a missing R.id field
            val checkPresenceId = resources.getIdentifier("checkPresenceButton", "id", packageName)
            if (checkPresenceId != 0) {
                findViewById<android.widget.Button>(checkPresenceId).setOnClickListener {
                    // Show presence dialog which now includes a Save SSID button for configuring home SSID
                    showPresenceDialog()
                }
            }
        } catch (_: Exception) {}
    }

    private fun setupCameraClickListeners() {
        // Full list of Drive image URLs (nullable so missing cameras show a friendly message)
        // These are defaults; the user can override any of them by setting SharedPreferences keys like "camera_door_url"
        val defaults: Map<String, String?> = mapOf(
            "garage" to "https://lh3.googleusercontent.com/d/1i7yQEZAIEwjB1aYfN0jKUwVdOKhL2uBr",
            "garage_side" to "https://lh3.googleusercontent.com/d/1mZTC_6Nb4thZAQvH4bafJVwO6p8yB6KH",
            "south" to "https://lh3.googleusercontent.com/d/1Fw9dvmwaWkw8RjCi18Zr_fUdXzN5MApw",
            "back" to "https://lh3.googleusercontent.com/d/1D-RC21Z5p9atX1xTle31uDlA1dgqAAJ-",
            "north" to "https://lh3.googleusercontent.com/d/1uPhu1YBPCTVtDog8a7TdTOt8zI2x3s_L",
            "front" to "https://lh3.googleusercontent.com/d/1aGcQTvdpYNsmUNOLN-4TFruxk7MTu-wg",
            // Door default: use the user-provided Drive file (converted to direct GoogleUserContent URL)
            "door" to "https://lh3.googleusercontent.com/d/1Ngq-uxaXuoHFySX-ynU1vwafeRh5InU5"
        )

        // Allow overrides from shared prefs: keys of the form camera_<key>_url, e.g. camera_door_url
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val imageUrls = defaults.mapValues { (key, defaultVal) ->
            val prefKey = "camera_${key}_url"
            val fromPref = prefs.getString(prefKey, null)
            if (!fromPref.isNullOrBlank()) fromPref else defaultVal
        }

        fun openOrNotify(urlKey: String, zoneName: String) {
            val url = imageUrls[urlKey]
            if (url != null) {
                Log.d("SecurityActivity", "Card pressed: $zoneName -> opening image URL: $url")
                // When opening from a zone card, request the image view to force a network reload
                openImageView(url, true)
            } else {
                Log.w("SecurityActivity", "Card pressed: $zoneName -> no image URL configured")
                // Use a more conservative approach to avoid system toast conflicts
                Handler(Looper.getMainLooper()).postDelayed({
                    try { ToastHelper.show(this, "No camera image available for $zoneName", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                }, 100)
            }
        }

        findViewById<CardView>(R.id.garageCard).setOnClickListener { openOrNotify("garage", "Garage") }
        findViewById<CardView>(R.id.garageSideCard).setOnClickListener { openOrNotify("garage_side", "Garage Side") }
        findViewById<CardView>(R.id.southCard).setOnClickListener { openOrNotify("south", "South") }
        findViewById<CardView>(R.id.backCard).setOnClickListener { openOrNotify("back", "Back") }
        findViewById<CardView>(R.id.northCard).setOnClickListener { openOrNotify("north", "North") }
        findViewById<CardView>(R.id.frontCard).setOnClickListener { openOrNotify("front", "Front") }
        findViewById<CardView>(R.id.doorCard).setOnClickListener { openOrNotify("door", "Door") }
        // Long-press the Door card to set or remove a camera URL (saved in prefs as camera_door_url)
        try {
            val doorCardView = findViewById<CardView?>(R.id.doorCard)
            doorCardView?.setOnLongClickListener {
                try {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val current = prefs.getString("camera_door_url", "") ?: ""
                    val edit = android.widget.EditText(this@SecurityActivity)
                    edit.setText(current)
                    edit.hint = "https://..."
                    val dlg = AlertDialog.Builder(this@SecurityActivity)
                        .setTitle("Door camera URL")
                        .setView(edit)
                        .setPositiveButton("Save") { _, _ ->
                            val v = edit.text.toString().trim()
                            if (v.isNotEmpty()) prefs.edit().putString("camera_door_url", v).apply() else prefs.edit().remove("camera_door_url").apply()
                            try { ToastHelper.show(this@SecurityActivity, "Door camera URL saved", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                        }
                        .setNeutralButton("Remove") { _, _ ->
                            prefs.edit().remove("camera_door_url").apply()
                            try { ToastHelper.show(this@SecurityActivity, "Door camera URL removed", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                        }
                        .setNegativeButton("Cancel", null)
                    dlg.show()
                } catch (e: Exception) {
                    Log.w("SecurityActivity", "Failed to show door URL dialog", e)
                    try { ToastHelper.show(this@SecurityActivity, "Unable to edit URL", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                }
                true
            }
        } catch (_: Exception) {}
    }

    private fun openImageView(imageUrl: String?, forceReload: Boolean = false) {
        if (imageUrl != null) {
            val intent = Intent(this, ImageViewActivity::class.java)
            intent.putExtra("IMAGE_URL", imageUrl)
            // Signal the ImageViewActivity to bypass caches and force a network reload when opened from a zone card
            intent.putExtra("FORCE_RELOAD", forceReload)
            startActivity(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("SecurityActivity", "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIF)
            } else {
                Log.d("SecurityActivity", "POST_NOTIFICATIONS already granted")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIF) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SecurityActivity", "POST_NOTIFICATIONS granted by user")
            } else {
                Log.w("SecurityActivity", "POST_NOTIFICATIONS denied by user; alerts may be suppressed")
            }
        }
        // Handle location permission request result
        if (requestCode == REQ_FINE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SecurityActivity", "ACCESS_FINE_LOCATION granted by user")
                // Permission granted, refresh network indicators so SSID read is attempted now
                try {
                    runOnUiThread { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) }
                } catch (e: Exception) {
                    Log.w("SecurityActivity", "Failed to refresh network indicators after permission grant", e)
                }
            } else {
                Log.w("SecurityActivity", "ACCESS_FINE_LOCATION denied by user; SSID read will be unavailable")
            }
        }
    }

    private fun listenForSecurityUpdates() {
        securityListener = db.collection("security sensors").document("live_status")
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("SecurityActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }
                
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    updateSecurityDisplay(documentSnapshot.data ?: emptyMap())
                }
            }
    }
    
    private fun updateSecurityDisplay(data: Map<String, Any>) {
        val zones = mapOf(
            "Garage" to Pair(listOf("garage_motion", "garage_sensor"), garageStatus),
            "Garage Side" to Pair(listOf("garage_side_motion", "garage_side_sensor"), garageSideStatus),
            "South" to Pair(listOf("south_motion", "south_sensor"), southStatus),
            "Back" to Pair(listOf("back_motion", "back_sensor"), backStatus),
            "North" to Pair(listOf("north_motion", "north_sensor"), northStatus),
            "Front" to Pair(listOf("front_motion", "front_sensor"), frontStatus),
            // Door left in place (sensors may be added later)
            "Door" to Pair(listOf("door_motion", "door_sensor"), doorStatus)
        )
        
        var activeZones = 0
        val activityLog = mutableListOf<String>()
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // Collect UI updates first (so we can apply them on the UI thread)
        val zoneUiUpdates = mutableListOf<Triple<TextView, String, Int>>()

        // Track whether we should show a full-screen alarm and which zone triggered it
        var alarmZoneName: String? = null

        zones.forEach { (zoneName, zoneData) ->
            val (sensors, statusView) = zoneData
            val sensor1Raw = data[sensors[0]]
            val sensor2Raw = data[sensors[1]]

            val sensor1 = sensorValueToBoolean(sensor1Raw)
            val sensor2 = sensorValueToBoolean(sensor2Raw)

            when {
                sensor1 && sensor2 -> {
                    zoneUiUpdates.add(Triple(statusView, "🔴 BREACH!", android.R.color.holo_red_light))
                    activeZones++ // dual-sensor breach counts as active zone
                    activityLog.add("[$currentTime] $zoneName: BOTH SENSORS ACTIVE")
                    // mark the first zone with a dual breach to launch the full-screen alarm
                    if (alarmZoneName == null) alarmZoneName = zoneName
                }
                sensor1 || sensor2 -> {
                    zoneUiUpdates.add(Triple(statusView, "🟡 Motion", android.R.color.holo_orange_light))
                    val sensorType = if (sensor1) "Motion" else "Sensor"
                    activityLog.add("[$currentTime] $zoneName: $sensorType detected (monitoring only)")
                }
                else -> {
                    zoneUiUpdates.add(Triple(statusView, "🟢 Clear", android.R.color.holo_green_light))
                }
            }
        }

        // Apply collected UI updates on the main thread
        runOnUiThread {
            zoneUiUpdates.forEach { (view, text, colorRes) ->
                view.text = text
                view.setTextColor(resources.getColor(colorRes, theme))
            }

            // Update system status
            systemStatusText.text = when (activeZones) {
                0 -> "🟢 All Zones Armed - System Online"
                1 -> "🟡 1 Zone Active - Monitoring"
                else -> "🔴 $activeZones Zones Active - ALERT!"
            }

            systemStatusText.setTextColor(
                when (activeZones) {
                    0 -> resources.getColor(android.R.color.holo_green_light, theme)
                    1 -> resources.getColor(android.R.color.holo_orange_light, theme)
                    else -> resources.getColor(android.R.color.holo_red_light, theme)
                }
            )

            // Update recent activity: append new entries to an in-memory cache so the card doesn't clear when there are no events
            if (activityLog.isNotEmpty()) {
                recentActivityCache.addAll(activityLog)
                // keep cache to a reasonable size
                if (recentActivityCache.size > 50) {
                    val removeCount = recentActivityCache.size - 50
                    repeat(removeCount) { recentActivityCache.removeAt(0) }
                }
                // persist recent activity
                val prefsSave = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefsSave.edit().putString("security_recent_activity", recentActivityCache.joinToString("\n")).apply()

                // Display the most recent entries first (filtered)
                renderRecentActivityPreview()
            } else {
                if (recentActivityCache.isNotEmpty()) {
                    renderRecentActivityPreview()
                } else {
                    renderRecentActivityPreview()
                }
            }

            // Launch full-screen alarm activity for the zone with a dual-sensor breach, if any
            if (alarmZoneName != null && shouldTriggerSecurityAlarms()) {
                val now = System.currentTimeMillis()
                val lastZone = AlarmFullscreenActivity.lastAlarmZone
                val lastTime = AlarmFullscreenActivity.lastAlarmTimeMs
                if (lastZone == alarmZoneName && now - lastTime < AlarmFullscreenActivity.MIN_RESTART_MS) {
                    Log.d("SecurityActivity", "Alarm for $alarmZoneName recently shown, skipping relaunch")
                } else {
                    AlarmFullscreenActivity.lastAlarmZone = alarmZoneName
                    AlarmFullscreenActivity.lastAlarmTimeMs = now
                    Log.i("SecurityActivity", "Requesting NotificationService to show full-screen alarm for $alarmZoneName")
                    // Ask NotificationService to post a full-screen security alarm (service will start the fullscreen activity)
                    val svcIntent = Intent(this, NotificationService::class.java)
                    svcIntent.action = NotificationService.ACTION_SECURITY_ALARM
                    svcIntent.putExtra("alarm_title", alarmZoneName)
                    svcIntent.putExtra("alarm_message", "Dual sensor breach in $alarmZoneName")
                    startService(svcIntent)
                }
            }
        }

        // Send notification for breaches (if any) only when alarms enabled/scheduled
        if (activeZones > 0 && shouldTriggerSecurityAlarms()) {
            showSecurityNotification(activeZones, activityLog.takeLast(3))
        }
    }

    // Robust converter for Firestore values -> boolean
    private fun sensorValueToBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Long -> value == 1L
            is Int -> value == 1
            is Double -> value == 1.0
            is String -> value == "1" || value.equals("true", ignoreCase = true)
            else -> false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Alerts"
            val descriptionText = "Notifications for security breaches"
            val importance = NotificationManager.IMPORTANCE_HIGH
            // Use bundled alarm tone for this channel so it matches other app alarm sounds
            val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}")
            val audioAttrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttrs)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // Create a default, silent channel for all other app notifications so only security alerts play sound
            val defaultName = "App Notifications"
            val defaultDescription = "General app notifications (silent)"
            val defaultImportance = NotificationManager.IMPORTANCE_LOW
            val defaultChannel = NotificationChannel(DEFAULT_CHANNEL_ID, defaultName, defaultImportance).apply {
                description = defaultDescription
                enableLights(false)
                enableVibration(false)
                // Explicitly set no sound for this channel
                setSound(null, null)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(defaultChannel)
        }
    }

    private fun showSecurityNotification(activeZones: Int, recent: List<String>) {
        val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}")

        val content = if (recent.isNotEmpty()) {
            recent.joinToString("\n")
        } else {
            "$activeZones zone(s) active"
        }

        // Use a platform icon fallback to avoid missing drawable resources
        val smallIcon = android.R.drawable.ic_dialog_alert

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("Security Alert: $activeZones breach(s)")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("SecurityActivity", "Skipping notification send: POST_NOTIFICATIONS not granted")
                return
            }
        }

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIF_ID, builder.build())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove Firestore listener to prevent memory leaks
        try { securityListener?.remove() } catch (_: Exception) {}
        // Unregister network callback used for SSID monitoring
        try { stopSsidMonitoring() } catch (_: Exception) {}
    }

    // Check whether security alarms should be active now (enabled toggle + daily schedule)
    private fun shouldTriggerSecurityAlarms(): Boolean {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val forceOff = prefs.getBoolean("security_alarms_force_off", false)
            val forceOn = prefs.getBoolean("security_alarms_force_on", false)
            val enabled = prefs.getBoolean("security_alarms_enabled", true)

            // New: explicit opt-in flag for enabling alarms when away from the home network
            val enableWhenAway = prefs.getBoolean("security_enable_when_away", false)

            // Schedule values in minutes since midnight or null if not configured
            val enableHour = prefs.getInt("security_enable_hour", -1)
            val enableMin = prefs.getInt("security_enable_min", 0)
            val disableHour = prefs.getInt("security_disable_hour", -1)
            val disableMin = prefs.getInt("security_disable_min", 0)
            val scheduleStart = if (enableHour < 0 || disableHour < 0) null else enableHour * 60 + enableMin
            val scheduleEnd = if (enableHour < 0 || disableHour < 0) null else disableHour * 60 + disableMin

            // Determine presence using modern ConnectivityManager + LinkProperties
            val isAtHome = try {
                // Use the unified helper isOnHomeNetwork() which prefers saved SSID, known gateways
                // and RFC1918 heuristics. This avoids a brittle equality check against a single gateway.
                val onHome = isOnHomeNetwork()
                Log.d("SecurityActivity", "Presence detection via isOnHomeNetwork -> $onHome")
                onHome
            } catch (e: Exception) {
                Log.w("SecurityActivity", "presence check failed", e)
                // Conservative default: assume at home if presence check fails
                true
            }

            return SecurityPolicy.shouldTriggerSecurityAlarms(
                enabled = enabled,
                forceOn = forceOn,
                forceOff = forceOff,
                isAtHome = isAtHome,
                enableWhenAway = enableWhenAway,
                scheduleStartMinutes = scheduleStart,
                scheduleEndMinutes = scheduleEnd
            )
        } catch (e: Exception) {
            Log.w("SecurityActivity", "shouldTriggerSecurityAlarms check failed", e)
            return true
        }
    }

    // Helper: return gateway IP like ScadaActivity used, or null
    private fun getGatewayIp(): String? {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active: Network? = cm.activeNetwork
            if (active == null) return null
            val lp: LinkProperties? = cm.getLinkProperties(active)
            if (lp == null) return null
            val routes = lp.routes
            // Log and sanitize all discovered gateway addresses to help diagnose odd formats (zone ids, IPv6, etc.)
            for (route in routes) {
                try {
                    val gw = route.gateway
                    if (gw != null) {
                        var host = gw.hostAddress
                        if (!host.isNullOrEmpty() && host != "0.0.0.0") {
                            // Remove zone identifier (e.g. %wlan0) if present, trim whitespace
                            host = host.split('%')[0].trim()
                            Log.d("SecurityActivity", "getGatewayIp: discovered gateway route -> $host")
                            return host
                        }
                    }
                } catch (ex: Exception) {
                    Log.w("SecurityActivity", "getGatewayIp: route processing failed", ex)
                }
            }
             return null
         } catch (e: Exception) {
             Log.w("SecurityActivity", "getGatewayIp failed", e)
             return null
         }
     }

    // Helper: return true if the currently active network transport is Wi‑Fi
    private fun isActiveNetworkWifi(): Boolean {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val active = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(active) ?: return false
            return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            Log.w("SecurityActivity", "isActiveNetworkWifi failed", e)
            return false
        }
    }

    private fun isOnHomeNetwork(): Boolean {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedHome = prefs.getString("home_ssid", null)
            val savedHomeGateway = prefs.getString("home_gateway", null)
            val currentSsid = try { getCurrentSsid() } catch (_: Exception) { null }

            // If a saved SSID exists and we can read SSID, prefer that (trimmed, case-insensitive)
            if (!savedHome.isNullOrEmpty() && !currentSsid.isNullOrEmpty()) {
                return savedHome.trim().equals(currentSsid.trim(), ignoreCase = true)
            }

            // Fallback to gateway heuristics
            val gw = try { getGatewayIp() } catch (_: Exception) { null } ?: return false

            // If user explicitly saved a gateway, trust it
            if (!savedHomeGateway.isNullOrEmpty() && savedHomeGateway.trim() == gw.trim()) return true

            val knownHomeGateways = setOf("192.168.8.1", "192.168.0.1", "192.168.1.1")
            if (gw in knownHomeGateways) return true

            // Treat private-range gateways as home ONLY when the active transport is Wi‑Fi. This avoids
            // misclassifying carrier-private addresses (e.g., 10.x) on mobile data as the home network.
            val isWifi = isActiveNetworkWifi()
            if (isWifi) {
                if (gw.startsWith("10.") || gw.startsWith("192.168.") || is172PrivateRange(gw)) return true
            }

            return false
        } catch (e: Exception) {
            Log.w("SecurityActivity", "isOnHomeNetwork check failed", e)
            return false
        }
    }

     private fun isNetworkAvailable(): Boolean {
         try {
             val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
             val active = cm.activeNetwork
             return active != null
         } catch (_: Exception) { return false }
     }

     // Updated: accept an optional inline dot to mirror homeDot state on the main card header
     private fun updateNetworkIndicators(homeDot: android.widget.ImageView?, bridgeDot: android.widget.ImageView?, inlineDot: android.widget.ImageView? = null) {
         try {
             val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
             val savedHome = prefs.getString("home_ssid", null)

             // If we have a saved home SSID but don't have location permission, request it asynchronously
             // but continue with gateway heuristics so UI still shows network/bridge status immediately.
             if (!savedHome.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                     Log.d("SecurityActivity", "Location permission not granted; requesting to enable SSID-aware home detection")
                     ensureLocationPermissionForSsid {
                         try { updateNetworkIndicators(homeDot, bridgeDot, inlineDot) } catch (e: Exception) { Log.w("SecurityActivity", "Re-run updateNetworkIndicators failed", e) }
                     }
                     // Do NOT return here — continue and use gateway-based detection so indicators update immediately.
                 }
             }

             // Determine whether we're on the home network. Prefer SSID comparison when available, otherwise fall back to gateway heuristics.
             var currentSsid = try { getCurrentSsid() } catch (e: Exception) { Log.w("SecurityActivity", "getCurrentSsid failed", e); null }
             // If we couldn't read SSID synchronously, try an active fetch (only if permission is present)
             if (currentSsid.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                 try {
                     fetchAndCacheSsidOnce { try { updateNetworkIndicators(homeDot, bridgeDot, inlineDot) } catch (_: Exception) {} }
                 } catch (_: Exception) {}
                 // Use lastKnownSsid if updated by the background fetch, otherwise continue with gateway heuristics for now
                 currentSsid = lastKnownSsid
             }

             val onHome = try {
                 if (!savedHome.isNullOrEmpty() && !currentSsid.isNullOrEmpty()) {
                     // Use trimmed case-insensitive comparisons to avoid quoting/ case issues
                     savedHome.trim().equals(currentSsid.trim(), ignoreCase = true)
                 } else {
                     // Gateway heuristic fallback
                    val gw = try { getGatewayIp() } catch (e: Exception) { Log.w("SecurityActivity", "getGatewayIp failed", e); null }
                    val knownHomeGateways = setOf("192.168.8.1", "192.168.0.1", "192.168.1.1")
                    if (gw == null) {
                        false
                    } else if (gw in knownHomeGateways) {
                        true
                    } else {
                        // Only treat private gateways as "home" when active transport is Wi‑Fi. Cellular carriers
                        // often use private NAT addresses (10.x) and should not be classified as home.
                        val isWifi = isActiveNetworkWifi()
                        if (isWifi) {
                            gw.startsWith("10.") || gw.startsWith("192.168.") || is172PrivateRange(gw)
                        } else {
                            false
                        }
                    }
                 }
             } catch (e: Exception) {
                 Log.w("SecurityActivity", "Determining home network failed", e)
                 false
             }

            // Check general network availability for bridge indicator
            val net = try { isNetworkAvailable() } catch (e: Exception) { Log.w("SecurityActivity", "isNetworkAvailable failed", e); false }

            // Colors
            val homeGreen = Color.parseColor("#4CAF50")
            val grey = Color.parseColor("#9E9E9E")
            val bridgeGreen = Color.parseColor("#4CAF50")

            // Update UI on main thread to be safe
            runOnUiThread {
                try {
                    if (homeDot != null) {
                        val color = if (onHome) homeGreen else grey
                        homeDot.imageTintList = android.content.res.ColorStateList.valueOf(color)
                        // Mirror onto inline dot if provided
                        try { inlineDot?.imageTintList = android.content.res.ColorStateList.valueOf(color) } catch (_: Exception) {}
                    } else {
                        try { inlineDot?.imageTintList = android.content.res.ColorStateList.valueOf(grey) } catch (_: Exception) {}
                    }

                    if (bridgeDot != null) {
                        bridgeDot.imageTintList = android.content.res.ColorStateList.valueOf(if (net) bridgeGreen else grey)
                    }
                    // Update optional debug text (if layout includes it) with short runtime diagnostics
                    try {
                        val method = if (!savedHome.isNullOrEmpty() && !currentSsid.isNullOrEmpty()) "SSID" else "gateway"
                        val gw = try { getGatewayIp() } catch (_: Exception) { null }
                        val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ContextCompat.checkSelfPermission(this@SecurityActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED else true
                        networkDebugText?.text = "savedHome:${savedHome ?: "(none)"}  lastKnown:${lastKnownSsid ?: "n/a"}  SSID:${currentSsid ?: "n/a"}  method:$method  onHome:$onHome  gw:${gw ?: "n/a"}  net:$net  perm:${hasPerm}"
                    } catch (_: Exception) {}
                 } catch (e: Exception) {
                     Log.w("SecurityActivity", "Failed applying network indicator colors to UI", e)
                 }
             }

            // Diagnostic log showing what we determined
            Log.d("SecurityActivity", "updateNetworkIndicators: savedHome=$savedHome currentSsid=$currentSsid onHome=$onHome net=$net")
        } catch (e: Exception) {
            Log.w("SecurityActivity", "updateNetworkIndicators failed", e)
        }
    }

    private fun showPresenceDialog() {
         val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
         val gateway = try { getGatewayIp() } catch (e: Exception) { null }
         val detectedSsid = try { getCurrentSsid() } catch (_: Exception) { null }
         val savedHomeSsid = prefs.getString("home_ssid", null)
         val savedHomeGateway = prefs.getString("home_gateway", null)
         val isHomeBySsid = if (!savedHomeSsid.isNullOrEmpty() && !detectedSsid.isNullOrEmpty()) detectedSsid == savedHomeSsid else false
         val isHomeByGateway = gateway == "192.168.8.1"
         val atHome = if (!savedHomeSsid.isNullOrEmpty()) isHomeBySsid else isHomeByGateway
         val gwText = gateway ?: "unknown"
         val ssidText = detectedSsid ?: "unknown"
         val savedText = savedHomeSsid ?: "(not set)"
         val savedGwText = savedHomeGateway ?: "(not set)"
         val message = "Gateway: $gwText\nSSID: $ssidText\nSaved home SSID: $savedText\nAt home: $atHome"
         runOnUiThread {
             val builder = AlertDialog.Builder(this)
                 .setTitle("Presence check")
                 .setMessage(message)
                 .setPositiveButton("OK", null)
             // Provide a Save... action which opens a small menu allowing saving either SSID or Gateway when available
             if (!detectedSsid.isNullOrEmpty() || !gateway.isNullOrEmpty()) {
                 builder.setNeutralButton("Save...") { _, _ ->
                     try {
                         val options = mutableListOf<String>()
                         if (!detectedSsid.isNullOrEmpty()) options.add("Save SSID as Home")
                         if (!gateway.isNullOrEmpty()) options.add("Save Gateway as Home")
                         val opts = options.toTypedArray()
                         val sub = AlertDialog.Builder(this)
                             .setTitle("Save home identifier")
                             .setItems(opts) { _, which ->
                                 when (opts[which]) {
                                     "Save SSID as Home" -> saveCurrentSsidAsHome()
                                     "Save Gateway as Home" -> {
                                         try {
                                             prefs.edit().putString("home_gateway", gateway).apply()
                                             try { ToastHelper.show(this, "Saved home gateway: $gateway", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                                             // Refresh indicators to pick up new saved gateway immediately
                                             runOnUiThread { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) }
                                         } catch (e: Exception) { Log.w("SecurityActivity", "Failed saving home gateway", e) }
                                     }
                                 }
                             }
                             .setNegativeButton("Cancel", null)
                         sub.show()
                     } catch (e: Exception) {
                         Log.w("SecurityActivity", "Failed showing save options", e)
                     }
                 }
             } else {
                 // Request permission then re-open the dialog so user can save SSID
                 builder.setNeutralButton("Enable SSID read") { _, _ -> ensureLocationPermissionForSsid { showPresenceDialog() } }
             }
             val dlg = builder.create()
             dlg.show()
         }
     }

    private fun updateScheduleUi() {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val eh = prefs.getInt("security_enable_hour", -1)
            val em = prefs.getInt("security_enable_min", 0)
            val dh = prefs.getInt("security_disable_hour", -1)
            val dm = prefs.getInt("security_disable_min", 0)
            val summary = SecuritySchedule.formatSchedule(eh, em, dh, dm)
            securityScheduleSummary.text = "Scheduled: $summary"

            val forced = prefs.getBoolean("security_alarms_force_on", false)
             // Always keep system status card fully opaque (no dimming)
             systemStatusCard.alpha = 1.0f


            // Reflect force switch state in UI (ensure switch matches persisted pref)
            securityForceSwitch.isChecked = forced
            // Reflect force-off switch in UI
            val forcedOff = prefs.getBoolean("security_alarms_force_off", false)
            securityForceOffSwitch.isChecked = forcedOff
        } catch (e: Exception) {
            Log.w("SecurityActivity", "updateScheduleUi failed", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh network indicators in case network state changed while activity was not visible
        try { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) } catch (_: Exception) {}

        // If a home SSID has been saved, ensure we have permission to read the SSID so the indicator can use it.
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedHome = prefs.getString("home_ssid", null)
            if (!savedHome.isNullOrEmpty()) {
                ensureLocationPermissionForSsid {
                    try { updateNetworkIndicators(networkHomeDot, networkBridgeDot, networkHomeDotInline) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }
}
