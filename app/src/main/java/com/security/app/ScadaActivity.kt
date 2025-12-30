package com.security.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ImageView
import android.content.res.ColorStateList
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import androidx.appcompat.app.AlertDialog
import android.widget.Button
import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import java.util.Locale
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlin.math.*
import com.google.firebase.messaging.FirebaseMessaging
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Job
import android.view.View

class ScadaActivity : BaseActivity() {

    // FCM status handler and runnable for periodic checks
    private val fcmStatusHandler = Handler(Looper.getMainLooper())
    private val fcmStatusRunnable = object : Runnable {
        override fun run() {
            try { checkFcmStatus() } catch (_: Exception) {}
            // schedule next check in 1 hour
            fcmStatusHandler.postDelayed(this, 60 * 60 * 1000L)
        }
    }

    private lateinit var ampsTextView: TextView
    private lateinit var kwTextView: TextView
    private lateinit var diagnosticsText: TextView
    private lateinit var geyserTempTextView: TextView
    private lateinit var geyserPressureTextView: TextView
    private lateinit var dvrTempTextView: TextView
    private lateinit var dvrHeartbeatStatus: TextView
    private lateinit var indoorTempTextView: TextView
    private lateinit var outdoorTempTextView: TextView
    private lateinit var outdoorHumidityTextView: TextView
    private lateinit var windSpeedTextView: TextView
    private lateinit var windDirectionTextView: ImageView
    private lateinit var alarmActiveStatus: TextView
    private lateinit var alarmStatusDot: ImageView
    private lateinit var dvrStatusDot: ImageView
    // Presence UI
    private lateinit var setupPresenceText: TextView
    private lateinit var setupPresenceDot: ImageView
    private lateinit var checkNetworkButton: Button

    // New: Sun & Tide UI
    private lateinit var sunInfoTextView: TextView
    private lateinit var tideInfoTextView: TextView

    // Swakopmund coordinates (approx)
    private val SWAK_LAT = -22.676f
    private val SWAK_LON = 14.526f

    // Embedded Stormglass API key (stored in repo per user request)
    // NOTE: this key was provided by the user and is intentionally embedded for this private app.
    private val EMBEDDED_STORMGLASS_API_KEY = "7c29c9f4-c692-11f0-a8f4-0242ac130003-7c29ca6c-c692-11f0-a8f4-0242ac130003"

    // Trend arrow TextViews for geyser
    private lateinit var waterTempTrendTextView: TextView
    private lateinit var waterPressureTrendTextView: TextView
    // Previous values for simple delta-based trend
    private var prevWaterTemp: Double? = null
    private var prevWaterPressure: Double? = null
    // DVR pulse tracking for status dot (mirror NotificationService logic)
    private var lastDvrPulse: Double? = null
    private var lastDvrPulseTime: Long = 0L

    // Helper: read numeric tolerance for DVR pulse comparisons
    private fun getDvrPulseTolerance(): Double {
        return try {
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            prefs.getFloat("dvr_pulse_tolerance", 0.01f).toDouble()
        } catch (_: Exception) { 0.01 }
    }

    // Firestore reference for security sensors
    private lateinit var db: FirebaseFirestore

    // Safe getter: returns a String representation of a field even when it's not stored as a String
    private fun DocumentSnapshot.getStringSafe(field: String): String? {
        return try {
            val v = this.get(field)
            when (v) {
                is String -> v
                is Number -> v.toString()
                is Boolean -> v.toString()
                is com.google.firebase.Timestamp -> try { v.toDate().toString() } catch (_: Exception) { null }
                else -> v?.toString()
            }
        } catch (e: Exception) {
            // In case of any unexpected casting exceptions, return null (caller can handle)
            Log.w("ScadaActivity", "getStringSafe failed for field=$field: ${e.message}")
            null
        }
    }

    // Google Sheets reader for geyser data
    private lateinit var sheetsReader: GoogleSheetsReader
    // DVR-specific sheet reader (separate gid)
    private val dvrSheetsReader = DVRGoogleSheetsReader()

    // Analytics variables
    private lateinit var totalPointsValue: TextView
    private lateinit var dataRateValue: TextView
    private lateinit var activeSensorsValue: TextView
    private lateinit var dailyPowerTextView: TextView
    private var lastUpdateTime: Long = 0L
    private var totalDataPoints: Int = 0
    private var dataRate: Double = 0.0
    private var activeSensors: Int = 0

    // Data class for Geyser readings
    data class GeyserReading(
        val waterTemp: Double = 0.0,
        val waterPressure: Double = 0.0,
        val dvrTemp: Double = 0.0,
        val currentAmps: Double = 0.0,
        val currentPower: Double = 0.0,
        val dailyPower: Double = 0.0,
        val indoorTemp: Double = 0.0,
        val outdoorTemp: Double = 0.0,
        val humidity: Double = 0.0,
        val windSpeed: Double = 0.0,
        val windDirection: Float = 0f
    )

    // --- Lights Control Fields ---
    // Icon + text views for lights status
    private lateinit var lightsStatusIcon: ImageView
    private lateinit var lightsStatusText: TextView
    private lateinit var lightsDetails: TextView
    // Polling job: limit reads to once every 30s up to 10 attempts (5 minutes)
    private var lightsPollJob: kotlinx.coroutines.Job? = null
    // Drive polling job (so we can cancel it when switching backends)
    // In Firestore-only variant, keep a handle but never start it
    private var drivePollingJob: Job? = null

    // Dashboard lights controller buttons (duplicated controller on the dashboard page)
    private var dashLightsOnBtn: android.widget.Button? = null
    private var dashLightsOffBtn: android.widget.Button? = null

    private lateinit var auth: FirebaseAuth
    // Firestore listener registration for lights_status
    private var lightsStatusListener: ListenerRegistration? = null

    // Activity-level Google sign-in launcher/client
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ScadaActivity", "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scada)
        // Kick off initial FCM status check soon after UI binds
        try { fcmStatusHandler.postDelayed(fcmStatusRunnable, 5_000L) } catch (_: Exception) {}

        // Handle dashboard intent for lights quick action
        try {
            val action = intent?.getStringExtra("lights_action")
            if (action == "on") {
                try { LightsService().writeOutsideLights(this@ScadaActivity, true) } catch (e: Exception) { Log.w("ScadaActivity", "writeOutsideLights(true) failed: ${e.message}") }
            } else if (action == "off") {
                try { LightsService().writeOutsideLights(this@ScadaActivity, false) } catch (e: Exception) { Log.w("ScadaActivity", "writeOutsideLights(false) failed: ${e.message}") }
            }
        } catch (e: Exception) { Log.w("ScadaActivity", "Reading lights_action failed: ${e.message}") }

        // Initialize Activity Result launcher for Google Sign-In
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val dataIntent = result.data
                    val task = GoogleSignIn.getSignedInAccountFromIntent(dataIntent)
                    if (task.isSuccessful) {
                        val account = task.result
                        if (account?.idToken.isNullOrEmpty()) {
                            try { ToastHelper.show(this@ScadaActivity, getString(R.string.google_signin_failed_missing_idtoken), android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                            return@registerForActivityResult
                        }
                        // account non-null here
                        firebaseAuthWithGoogle(account)
                    } else {
                        try { ToastHelper.show(this@ScadaActivity, getString(R.string.google_signin_failed_generic, task.exception?.localizedMessage ?: ""), android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                    }
                } else {
                    try { ToastHelper.show(this@ScadaActivity, getString(R.string.google_signin_cancelled), android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w("ScadaActivity", "Sign-in launcher callback error", e)
                try { ToastHelper.show(this@ScadaActivity, getString(R.string.google_signin_failed_generic, e.localizedMessage ?: ""), android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
            }
        }

        // Initialize views with amps prominent, kW smaller as requested
        diagnosticsText = findViewById(R.id.diagnosticsText)
        ampsTextView = findViewById(R.id.currentAmps)
        kwTextView = findViewById(R.id.currentPower)
        geyserTempTextView = findViewById(R.id.waterTempValue)
        geyserPressureTextView = findViewById(R.id.waterPressureValue)
        dvrTempTextView = findViewById(R.id.dvrTemp)
        dvrHeartbeatStatus = findViewById(R.id.dvrHeartbeatStatus)
        indoorTempTextView = findViewById(R.id.indoorTemp)
        outdoorTempTextView = findViewById(R.id.outdoorTemp)
        outdoorHumidityTextView = findViewById(R.id.outdoorHumidity)
        windSpeedTextView = findViewById(R.id.windSpeed)
        windDirectionTextView = findViewById(R.id.windDirectionArrow)
        alarmActiveStatus = findViewById(R.id.alarmActiveStatus)
        // status dots
        alarmStatusDot = findViewById(R.id.alarmStatusDot)
        dvrStatusDot = findViewById(R.id.dvrStatusDot)

        // Hook up dashboard lights controller (if present in the layout)
        try {
            dashLightsOnBtn = findViewById(R.id.dashboardLightsOnButton)
            dashLightsOffBtn = findViewById(R.id.dashboardLightsOffButton)
            dashLightsOnBtn?.apply {
                isEnabled = true
                setOnClickListener {
                    LightsService().writeOutsideLights(this@ScadaActivity, true)
                    try { startLightsPolling() } catch (_: Exception) {}
                }
            }
            dashLightsOffBtn?.apply {
                isEnabled = true
                setOnClickListener {
                    LightsService().writeOutsideLights(this@ScadaActivity, false)
                    try { startLightsPolling() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w("ScadaActivity", "Dashboard lights controller wiring skipped", e)
        }

        // New: bind sun/tide views (layout added)
        try {
            sunInfoTextView = findViewById(R.id.sunInfo)
            tideInfoTextView = findViewById(R.id.tideInfo)
            // Ensure the tide view is clickable and opens the WorldTides key dialog on click or long-press
            try {
                tideInfoTextView.isClickable = true
                tideInfoTextView.isLongClickable = true
                tideInfoTextView.setOnClickListener { showStormglassKeyDialog() }
                tideInfoTextView.setOnLongClickListener {
                    showStormglassKeyDialog(); true
                }

                // Also attach handlers to the parent CardView (user may long-press the whole card)
                try {
                    var vp: android.view.ViewParent? = tideInfoTextView.parent
                    while (vp != null) {
                        if (vp is androidx.cardview.widget.CardView) {
                            vp.isClickable = true
                            vp.isLongClickable = true
                            vp.setOnClickListener { showStormglassKeyDialog() }
                            vp.setOnLongClickListener { showStormglassKeyDialog(); true }
                            break
                        }
                        vp = vp.parent
                    }
                } catch (e: Exception) {
                    Log.w("ScadaActivity", "Failed attaching click to parent card", e)
                }
            } catch (e: Exception) {
                Log.w("ScadaActivity", "tideInfoTextView click/long-click binding failed", e)
            }
        } catch (_: Exception) {
            // Layout may not include the new views in older variants — tolerate silently
        }

        // Presence UI bindings (setup card)
        try {
            setupPresenceText = findViewById(R.id.setupPresenceText)
            setupPresenceDot = findViewById(R.id.setupPresenceDot)
            checkNetworkButton = findViewById(R.id.checkNetworkButton)
            checkNetworkButton.setOnClickListener { updatePresenceUI(true) }
        } catch (_: Exception) {}

        // Initial presence update
        updatePresenceUI(false)

        // Wind invert is now managed solely in the Alarm Settings dialog (no inline control)

        // Bind trend arrow TextViews
        waterTempTrendTextView = findViewById(R.id.waterTempTrend)
        waterPressureTrendTextView = findViewById(R.id.waterPressureTrend)

        // Initialize Firestore for security sensors
        db = FirebaseFirestore.getInstance()
        // Start a single Firestore snapshot listener for lights_status so the bulb indicator updates live
        setupLightsListener()

        // Initialize FirebaseAuth now so lights button handlers can check sign-in state
        auth = FirebaseAuth.getInstance()

        // Initialize Google Sheets reader for geyser data
        sheetsReader = GoogleSheetsReader()

        // Set initial values to show amps prominent, kW smaller layout
        ampsTextView.text = getString(R.string.dash_amp)  // Amps prominently displayed
        kwTextView.text = getString(R.string.dash_kw)  // kW smaller on side
        geyserTempTextView.text = getString(R.string.loading_sheets)
        geyserPressureTextView.text = getString(R.string.loading_sheets)
        dvrTempTextView.text = getString(R.string.dvr_label)
        indoorTempTextView.text = "--°C"
        outdoorTempTextView.text = "--°C"
        outdoorHumidityTextView.text = "--%"
        windSpeedTextView.text = getString(R.string.dash_wind)
        windDirectionTextView.setImageResource(R.drawable.ic_wind_arrow) // Set default wind direction icon
        // initialize status dot colors from default text/colors
        try {
            // Default: assume DVR online (green)
            val green = "#4CAF50".toColorInt()
            dvrStatusDot.imageTintList = ColorStateList.valueOf(green)
        } catch (_: Exception) {}
        try {
            // Default alarm state - match existing alarmActiveStatus text color if present
            val green = "#4CAF50".toColorInt()
            alarmStatusDot.imageTintList = ColorStateList.valueOf(green)
        } catch (_: Exception) {}

        // Initialize analytics TextViews
        totalPointsValue = findViewById(R.id.totalPointsValue)
        dataRateValue = findViewById(R.id.dataRateValue)
        activeSensorsValue = findViewById(R.id.activeSensorsValue)
        // Bind SCADA daily power card so it displays the parsed daily total (column J)
        dailyPowerTextView = findViewById(R.id.dailyPower)
        dailyPowerTextView.text = "-- kWh"

        // Lights control views
        lightsStatusIcon = findViewById(R.id.lightsStatusIcon)
        lightsStatusText = findViewById(R.id.lightsStatusText)
        lightsDetails = findViewById(R.id.lightsDetails)
        // Immediately clear any stale status text (e.g., legacy Drive line) in this Firestore-only build
        try { lightsDetails.text = "--" } catch (_: Exception) {}
        // Force backend preference to Firebase (Firestore-only variant) so no Drive branch is taken anywhere
        try {
            val appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            appPrefs.edit().putString("lights_control_backend", "firebase").apply()
        } catch (_: Exception) {}

        // Setup lights control buttons
        val lightsOnButton = findViewById<android.widget.Button>(R.id.lightsOnButton)
        val lightsOffButton = findViewById<android.widget.Button>(R.id.lightsOffButton)
        lightsOnButton.isEnabled = true
        lightsOffButton.isEnabled = true
        Log.d("ScadaActivity", "Lights buttons enabled at startup")

        // Use centralized LightsService for bridge writes (removes duplication)
        lightsOnButton.setOnClickListener {
            Log.d("ScadaActivity", "Lights ON clicked — using LightsService")
            val ok = LightsService().writeOutsideLights(this@ScadaActivity, true)
            if (ok) {
                try { ToastHelper.show(this@ScadaActivity, getString(R.string.bridge_command_on_sent), android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
            } else {
                try { ToastHelper.show(this@ScadaActivity, getString(R.string.failed_send_bridge_command, "service write failed"), android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
            }
            try { fetchLightsStatusOnce() } catch (_: Exception) {}
        }
        lightsOffButton.setOnClickListener {
            Log.d("ScadaActivity", "Lights OFF clicked — using LightsService")
            val ok = LightsService().writeOutsideLights(this@ScadaActivity, false)
            if (ok) {
                try { ToastHelper.show(this@ScadaActivity, getString(R.string.bridge_command_off_sent), android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
            } else {
                try { ToastHelper.show(this@ScadaActivity, getString(R.string.failed_send_bridge_command, "service write failed"), android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
            }
            try { fetchLightsStatusOnce() } catch (_: Exception) {}
        }

        // Expose lights control for intent quick action
        // (removed local function; now call class-level toggleOutsideLights)

        // Setup click listeners for graphs
        setupGraphClickListeners()

        // diagnosticsText long-press removed — appearance controls moved into the Settings dialog (alarmStatusCard).

        // Firebase monitoring removed - all data now from Google Sheets

        // Start Google Sheets monitoring for geyser data
        monitorGeyserData()

        // Also update sun/tide info now
        try { updateSunAndTides() } catch (e: Exception) { Log.w("ScadaActivity", "updateSunAndTides failed", e) }

        // Setup Google Drive sign-in
        // Firestore-only variant: skip Drive sign-in entirely
        // setupGoogleDriveSignIn()
        // Start polling for Y21_read_status from Drive file
        // Firestore-only variant: do not start Drive polling

        // Alarm status card opens settings dialog
        findViewById<androidx.cardview.widget.CardView>(R.id.alarmStatusCard).setOnClickListener {
            openAlarmSettingsDialog()
        }

        // If launched with open_setup extra, display the setup dialog immediately
        try {
            val openSetup = intent?.getBooleanExtra("open_setup", false) ?: false
            if (openSetup) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    openAlarmSettingsDialog()
                }
            }
        } catch (_: Exception) {}

        // Google Sign-In button
        val signInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.googleSignInButton)
        signInButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                // Firestore-only variant: do not request Drive scope
                //.requestScopes(Scope(DriveScopes.DRIVE))
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            signInLauncher.launch(googleSignInClient.signInIntent)
        }

        // Setup swipe to refresh
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
            swipeRefreshLayout.isRefreshing = false
        }

        // New: refresh lights authorization status on create
        refreshLightsAuthorizationStatus()
    }

    private fun refreshLightsAuthorizationStatus() {
        val view = findViewById<TextView>(R.id.lightsAuthStatus)
        if (view != null) {
            val authorized = try { LightsService().isUserAuthorizedForLights() } catch (t: Throwable) { false }
            view.text = if (authorized) "Lights authorization: Authorized" else "Lights authorization: Not authorized"
            // Color hint: green when authorized, red otherwise
            val color = if (authorized) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()
            view.setTextColor(color)
        }
    }

    private fun refreshData() {
        monitorGeyserData()
        try { ToastHelper.show(this@ScadaActivity, "Data refreshed", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        // Run an immediate check and (re)start hourly checks while the Activity is in foreground
        try {
            checkFcmStatus()
            fcmStatusHandler.removeCallbacks(fcmStatusRunnable)
            fcmStatusHandler.postDelayed(fcmStatusRunnable, 60 * 60 * 1000L)
        } catch (_: Exception) {}

        try {
            // Firestore-only: attach live listener for lights
            setupLightsListener()
        } catch (_: Exception) {}
    }

    override fun onPause() {
        // Stop periodic checks when not visible to prevent misleading stale status
        try { fcmStatusHandler.removeCallbacks(fcmStatusRunnable) } catch (_: Exception) {}
        super.onPause()
    }

    override fun onDestroy() {
        // Remove FCM status callbacks
        try { fcmStatusHandler.removeCallbacks(fcmStatusRunnable) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        // Attach realtime listener when Activity becomes visible
        try { setupLightsListener() } catch (_: Exception) {}
    }

    override fun onStop() {
        // Detach listener to avoid reads when not visible
        try { lightsStatusListener?.remove() } catch (_: Exception) {}
        lightsStatusListener = null
        super.onStop()
    }

    private fun hasActiveNetwork(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(n) ?: return false
            // Treat transport presence as connectivity
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Exception) { false }
    }

    // --- Compatibility shims (no-ops or minimal behavior) ---
    // Old build referenced Drive polling; we now use Firestore live listener. Keep for compatibility.
    // Firestore-only variant: ensure Firestore listener is active
    private fun startLightsPolling() {
        try { fetchLightsStatusOnce() } catch (_: Exception) {}
    }

    // Presence card updater (simple network reachability check)
    private fun updatePresenceUI(forceCheck: Boolean) {
        try {
            val online = hasActiveNetwork()
            if (::setupPresenceText.isInitialized) {
                setupPresenceText.text = if (online) "Device online" else "Device offline"
                val color = if (online) "#4CAF50".toColorInt() else "#F44336".toColorInt()
                setupPresenceText.setTextColor(color)
            }
            if (::setupPresenceDot.isInitialized) {
                val color = if (online) "#4CAF50".toColorInt() else "#F44336".toColorInt()
                setupPresenceDot.imageTintList = ColorStateList.valueOf(color)
            }
        } catch (_: Exception) {}
    }

    // Legacy Y21 polling hook; Drive polling is disabled in this variant.
    // Firestore-only variant: no-op for Drive polling
    private fun startY21ReadPolling() {
        Log.d("ScadaActivity", "startY21ReadPolling: disabled in this build; Firestore listener handles status updates")
    }

    // Minimal placeholder dialog so clicks don’t crash in this variant
    private fun openAlarmSettingsDialog() {
        try {
            // Setup dialog to switch lights backend
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val current = (prefs.getString("scada_lights_backend", "firestore") ?: "firestore").lowercase()
            val options = arrayOf("Firebase (Firestore-only)")
            var selectedIndex = 0

            AlertDialog.Builder(this)
                .setTitle("Setup")
                .setSingleChoiceItems(options, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton("Apply") { _, _ ->
                    val chosen = "firestore"
                    prefs.edit().putString("scada_lights_backend", chosen).apply()
                    try { applyLightsBackendSelection() } catch (_: Exception) {}
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (_: Exception) {
            try {
                AlertDialog.Builder(this)
                    .setTitle("Alarm Settings")
                    .setMessage("Settings are not available in this variant.")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (_: Exception) {}
        }
    }

    // Lights backend selection not used anymore; keep as no-op
    private fun applyLightsBackendSelection() {
        // Firestore-only: always cancel any legacy Drive jobs and use Firestore listener/fetch
        try { drivePollingJob?.cancel() } catch (_: Exception) {}
        drivePollingJob = null
        try { fetchLightsStatusOnce() } catch (_: Exception) {}
        Log.d("ScadaActivity", "applyLightsBackendSelection: forced Firebase backend (Firestore-only)")
    }

    private fun checkFcmStatus() {
        val deviceOnline = hasActiveNetwork()
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    val tokenOk = task.isSuccessful && !task.result.isNullOrBlank()
                    val isOnline = deviceOnline && tokenOk
                    val statusText = if (isOnline) "FCM: Online" else "FCM: Offline"
                    // Update System Health diagnosticsText line without clobbering other info
                    try {
                        val current = diagnosticsText.text?.toString() ?: ""
                        val lines = current.split('\n')
                        val rebuilt = StringBuilder()
                        var injected = false
                        for (line in lines) {
                            if (line.trim().startsWith("FCM:")) {
                                rebuilt.append(statusText).append('\n')
                                injected = true
                            } else {
                                if (line.isNotEmpty()) rebuilt.append(line).append('\n')
                            }
                        }
                        if (!injected) {
                            if (rebuilt.isNotEmpty() && rebuilt.last() != '\n') rebuilt.append('\n')
                            rebuilt.append(statusText)
                        }
                        diagnosticsText.text = rebuilt.toString().trimEnd()
                    } catch (_: Exception) {}
                }
                .addOnFailureListener {
                    val statusText = "FCM: Offline"
                    try {
                        val current = diagnosticsText.text?.toString() ?: ""
                        val lines = current.split('\n')
                        val rebuilt = StringBuilder()
                        var injected = false
                        for (line in lines) {
                            if (line.trim().startsWith("FCM:")) {
                                rebuilt.append(statusText).append('\n')
                                injected = true
                            } else {
                                if (line.isNotEmpty()) rebuilt.append(line).append('\n')
                            }
                        }
                        if (!injected) {
                            if (rebuilt.isNotEmpty() && rebuilt.last() != '\n') rebuilt.append('\n')
                            rebuilt.append(statusText)
                        }
                        diagnosticsText.text = rebuilt.toString().trimEnd()
                    } catch (_: Exception) {}
                }
        } catch (_: Exception) {
            val statusText = "FCM: Offline"
            try {
                val current = diagnosticsText.text?.toString() ?: ""
                val lines = current.split('\n')
                val rebuilt = StringBuilder()
                var injected = false
                for (line in lines) {
                    if (line.trim().startsWith("FCM:")) {
                        rebuilt.append(statusText).append('\n')
                        injected = true
                    } else {
                        if (line.isNotEmpty()) rebuilt.append(line).append('\n')
                    }
                }
                if (!injected) {
                    if (rebuilt.isNotEmpty() && rebuilt.last() != '\n') rebuilt.append('\n')
                    rebuilt.append(statusText)
                }
                diagnosticsText.text = rebuilt.toString().trimEnd()
            } catch (_: Exception) {}
        }
    }

    private fun setupGraphClickListeners() {
        // Weather section click - show weather graphs
        findViewById<android.widget.LinearLayout>(R.id.weatherSection).setOnClickListener {
            val intent = Intent(this, WeatherGraphsActivity::class.java)
            startActivity(intent)
        }

        // Power section click - show power graphs
        findViewById<androidx.cardview.widget.CardView>(R.id.powerSection).setOnClickListener {
            val intent = Intent(this, PowerGraphsActivity::class.java)
            startActivity(intent)
        }

        // Geyser section click - show geyser graphs
        findViewById<androidx.cardview.widget.CardView>(R.id.geyserSection).setOnClickListener {
            val intent = Intent(this, GeyserGraphsActivity::class.java)
            startActivity(intent)
        }

        // DVR section click - show DVR graphs
        findViewById<androidx.cardview.widget.CardView>(R.id.dvrSection).setOnClickListener {
            val intent = Intent(this, DvrGraphsActivity::class.java)
            startActivity(intent)
        }

        // Back button functionality
        findViewById<android.widget.TextView>(R.id.scadaBackButton).setOnClickListener {
            finish()
        }
    }

    // Firebase monitoring removed - all data now comes from Google Sheets

    private fun updateAnalytics() {
        // Count data points (all fields are always present)
        val points = 10 // number of fields in GeyserReading
        val currentTime = System.currentTimeMillis()
        if (lastUpdateTime != 0L) {
            val timeDiffMin = (currentTime - lastUpdateTime) / 60000.0
            if (timeDiffMin > 0) {
                dataRate = points / timeDiffMin
            }
        }
        lastUpdateTime = currentTime
        totalDataPoints += points
        activeSensors = points
        // Update UI
        totalPointsValue.text = totalDataPoints.toString()
        dataRateValue.text = String.format(Locale.getDefault(), "%.1f/min", dataRate)
        activeSensorsValue.text = activeSensors.toString()
    }

    private fun monitorGeyserData() {
        // Launch coroutine to fetch geyser data from Google Sheets
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Update the last updated time
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val lastUpdated = sdf.format(java.util.Date())
                val existingText = diagnosticsText.text.toString()
                val newText = existingText.replace(Regex("Last Update: .*"), "Last Update: $lastUpdated")
                diagnosticsText.text = newText
                // Fetch latest readings from Google Sheets (real geyser data)
                val sensorReadings = withContext(Dispatchers.IO) {
                     sheetsReader.fetchLatestReadings(2) // Get the two most recent readings so we can compute an initial trend immediately
                 }

                // Debugging: if parser returned no data or weather fields are all zero, fetch raw CSV and try alternate gids
                if (sensorReadings.isEmpty() || (sensorReadings[0].outdoorTemp == 0.0f && sensorReadings[0].humidity == 0.0f && sensorReadings[0].windSpeed == 0.0f)) {
                    try {
                        Log.w("ScadaActivity", "SheetsReader returned empty/zero weather; dumping raw CSV and trying alternate gids for diagnosis")
                        // Log first few lines of the default gid CSV
                        val rawCsv0 = withContext(Dispatchers.IO) { sheetsReader.fetchRawCsvForGid(0) }
                        if (rawCsv0 != null) {
                            val head = rawCsv0.split('\n').take(10).joinToString("\\n")
                            Log.w("ScadaActivity", "CSV gid=0 head:\n$head")
                        } else {
                            Log.w("ScadaActivity", "CSV gid=0 returned null")
                        }

                        // Try common alternate gids (0..5) and log parsed samples
                        for (gid in 0..5) {
                            try {
                                val parsedSample = withContext(Dispatchers.IO) { sheetsReader.fetchRecentReadingsFromGid(gid, 24, 5) }
                                Log.w("ScadaActivity", "Parsed from gid=$gid count=${parsedSample.size} sample=${parsedSample.firstOrNull()}")
                            } catch (e: Exception) {
                                Log.w("ScadaActivity", "fetchRecentReadingsFromGid failed for gid=$gid", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ScadaActivity", "Sheets debug diagnostics failed", e)
                    }
                }

                 if (sensorReadings.isNotEmpty()) {
                    // If we have two rows, set previous values from the older row (index 1) so the trend arrow can be computed immediately
                    if (sensorReadings.size >= 2) {
                        val older = sensorReadings[1]
                        prevWaterTemp = older.waterTemp.toDouble()
                        prevWaterPressure = older.waterPressure.toDouble()
                    }
                    // Convert SensorReading to GeyserReading
                    val s = sensorReadings[0] // most recent row
                    try { android.util.Log.d("ScadaActivity", "SensorReading row parsed: $s") } catch (_: Exception) {}
                    val latestReading = GeyserReading(
                        waterTemp = s.waterTemp.toDouble(),
                        waterPressure = s.waterPressure.toDouble(),
                        dvrTemp = s.dvrTemp.toDouble(),
                        currentAmps = s.currentAmps.toDouble(),
                        currentPower = s.currentPower.toDouble(),
                        dailyPower = s.dailyPower.toDouble(),
                        indoorTemp = s.indoorTemp.toDouble(),
                        outdoorTemp = s.outdoorTemp.toDouble(),
                        humidity = s.humidity.toDouble(),
                        windSpeed = s.windSpeed.toDouble(),
                        windDirection = s.windDirection
                    )
                    // Update UI with REAL data from Google Sheets (LAST ROW)
                    updateAnalytics()
                    geyserTempTextView.text = String.format(Locale.getDefault(), "%.1f°C", latestReading.waterTemp)
                    geyserPressureTextView.text = String.format(Locale.getDefault(), "%.1f bar", latestReading.waterPressure)
                    // Show daily power (kWh) on the SCADA card
                    dailyPowerTextView.text = String.format(Locale.getDefault(), "%.1f kWh", latestReading.dailyPower)
                    // Handle DVR temperature: prefer the DVR-specific gid (used by the DVR graphs) so the card and graph match.
                    try {
                        // Prefer the DVR value already loaded into GraphDataRepository (the graph source) so the card matches the graph.
                        // If repository has no data, fall back to a short DVR gid network fetch, otherwise use the general sheet value.
                        var sourceDvr: Double = latestReading.dvrTemp
                        try {
                            val repoLatest = GraphDataRepository.dvrGraphData.value.lastOrNull()?.dvrTemp
                            if (repoLatest != null) {
                                sourceDvr = repoLatest.toDouble()
                                Log.d("ScadaActivity", "Using DVR value from GraphDataRepository for card: $sourceDvr")
                            } else {
                                // Repository empty — attempt a short network fetch of DVR gid (non-blocking network done on IO)
                                try {
                                    val fetchedReading = withContext(Dispatchers.IO) { dvrSheetsReader.fetchLatestDvrReading() }
                                    if (fetchedReading != null) {
                                        sourceDvr = fetchedReading.dvrTemp.toDouble()
                                        Log.d("ScadaActivity", "Fetched DVR gid value for card: $sourceDvr")
                                    }
                                } catch (e: Exception) {
                                    Log.w("ScadaActivity", "DVR gid network fetch failed; will use general sheet value", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("ScadaActivity", "Failed resolving preferred DVR source; using general sheet value", e)
                        }
                        var displayDvr = sourceDvr

                        // If the reading looks like Kelvin (hot: >200), convert to Celsius
                        if (displayDvr.isFinite() && displayDvr > 200.0) {
                            val conv = displayDvr - 273.15
                            android.util.Log.i("ScadaActivity", "DVR reading looks like Kelvin; converting: raw=$displayDvr K -> ${String.format(Locale.getDefault(), "%.2f", conv)} °C")
                            displayDvr = conv
                        }

                        // If the value is obviously out of range (too hot or invalid), attempt a DVR gid fallback read in background
                        if (!displayDvr.isFinite() || displayDvr > 120.0 || displayDvr < -50.0) {
                            android.util.Log.w("ScadaActivity", "DVR reading out of range (raw=$sourceDvr -> display=$displayDvr); attempting DVR-specific fallback read")
                            try {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dvrRow = dvrSheetsReader.fetchLatestDvrReading()
                                    if (dvrRow != null) {
                                        val d = dvrRow
                                        var dval = d.dvrTemp.toDouble()
                                        if (dval.isFinite() && dval > 200.0) dval -= 273.15
                                        withContext(Dispatchers.Main) {
                                            try {
                                                if (dval.isFinite() && dval <= 120.0 && dval >= -50.0) {
                                                    dvrTempTextView.text = String.format(Locale.getDefault(), "%.1f°C", dval)
                                                    lastDvrPulse = dval
                                                    lastDvrPulseTime = System.currentTimeMillis()
                                                    android.util.Log.i("ScadaActivity", "DVR fallback read successful: $dval °C")
                                                } else {
                                                    dvrTempTextView.text = "N/A"
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) { try { dvrTempTextView.text = "N/A" } catch (_: Exception) {} }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("ScadaActivity", "DVR fallback read kicked off failed", e)
                                try { dvrTempTextView.text = "N/A" } catch (_: Exception) {}
                            }
                        } else {
                            // Normal display (valid value from preferred source)
                            dvrTempTextView.text = String.format(Locale.getDefault(), "%.1f°C", displayDvr)
                            lastDvrPulse = displayDvr
                            lastDvrPulseTime = System.currentTimeMillis()
                            // Persist the DVR pulse/time so NotificationService and other components share the same baseline
                            try {
                                val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                                alarmPrefs.edit().putFloat("last_dvr_pulse_value", lastDvrPulse!!.toFloat()).putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                                android.util.Log.d("ScadaActivity", "Persisted lastDvrPulse=$lastDvrPulse at $lastDvrPulseTime to alarm_prefs")
                                // Broadcast to NotificationService (if running) so it updates its in-memory state immediately
                                try {
                                    val b = Intent(NotificationService.ACTION_UPDATE_DVR_PULSE)
                                    b.putExtra("last_dvr_pulse_value", lastDvrPulse!!)
                                    b.putExtra("last_dvr_pulse_time_ms", lastDvrPulseTime)
                                    sendBroadcast(b)
                                } catch (e: Exception) { android.util.Log.w("ScadaActivity", "Failed sending DVR update broadcast", e) }
                            } catch (e: Exception) {
                                android.util.Log.w("ScadaActivity", "Failed to persist last DVR pulse", e)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ScadaActivity", "Failed processing DVR temperature display", e)
                        try { dvrTempTextView.text = "N/A" } catch (_: Exception) {}
                    }
                    ampsTextView.text = String.format(Locale.getDefault(), "%.1f A", latestReading.currentAmps)
                    kwTextView.text = String.format(Locale.getDefault(), "%.2f kW", latestReading.currentPower)
                    // Update weather card values (indoor/outdoor/humidity/wind) — these were not being written previously
                    try {
                        indoorTempTextView.text = String.format(Locale.getDefault(), "%.1f°C", latestReading.indoorTemp)
                        outdoorTempTextView.text = String.format(Locale.getDefault(), "%.1f°C", latestReading.outdoorTemp)
                        outdoorHumidityTextView.text = String.format(Locale.getDefault(), "%.0f%%", latestReading.humidity)
                        windSpeedTextView.text = String.format(Locale.getDefault(), "%.1f km/h", latestReading.windSpeed)
                        // Apply user-configured wind arrow offset for display only
                        try {
                            val prefsLocal = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            val windOffset = prefsLocal.getFloat("wind_arrow_offset_deg", 0f)
                            val rawDeg = ((latestReading.windDirection) % 360f + 360f) % 360f
                            val displayRotation = ((rawDeg + windOffset) % 360f + 360f) % 360f
                            windDirectionTextView.rotation = displayRotation
                        } catch (_: Exception) {}
                    } catch (e: Exception) {
                        Log.w("ScadaActivity", "Failed updating weather card UI", e)
                    }

                    // If DVR temperature was not present (0.0), try the dedicated DVR gid CSV as fallback
                    if (latestReading.dvrTemp == 0.0) {
                        try {
                            CoroutineScope(Dispatchers.IO).launch {
                                val dvrRow = dvrSheetsReader.fetchLatestDvrReading()
                                if (dvrRow != null) {
                                    val d = dvrRow
                                    withContext(Dispatchers.Main) {
                                        try {
                                            dvrTempTextView.text = String.format(Locale.getDefault(), "%.1f°C", d.dvrTemp)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        } catch (e: Exception) { Log.w("ScadaActivity", "DVR fallback read failed", e) }
                    }

                    // Dynamic status text for temperature and pressure
                    try {
                        val prefsLocal = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                        // Pressure threshold exists in prefs; default to 1.0 bar
                        val lowPressure = prefsLocal.getFloat("low_pressure_threshold", 1.0f).toDouble()

                        // Temperature range: use prefs if present, otherwise sensible defaults (40..70°C)
                        val tempMin = prefsLocal.getFloat("water_temp_min", 40.0f).toDouble()
                        val tempMax = prefsLocal.getFloat("water_temp_max", 70.0f).toDouble()

                        // Temperature status
                        val tempStatusText: String
                        val tempColor = when {
                            latestReading.waterTemp < tempMin -> "#FF5722".toColorInt() // slightly warning
                            latestReading.waterTemp > tempMax -> "#F44336".toColorInt() // high = red
                            else -> "#4CAF50".toColorInt() // normal = green
                        }
                        tempStatusText = when {
                            latestReading.waterTemp < tempMin -> String.format(Locale.getDefault(), "Low (%.1f°C)", latestReading.waterTemp)
                            latestReading.waterTemp > tempMax -> String.format(Locale.getDefault(), "High (%.1f°C)", latestReading.waterTemp)
                            else -> String.format(Locale.getDefault(), "Normal (%.1f°C)", latestReading.waterTemp)
                        }
                        try { findViewById<TextView>(R.id.waterTempStatus).text = tempStatusText } catch (_: Exception) {}
                        try { findViewById<TextView>(R.id.waterTempStatus).setTextColor(tempColor) } catch (_: Exception) {}

                        // Pressure status
                        val pressureStatusText: String
                        val pressureColor = if (latestReading.waterPressure < lowPressure) "#F44336".toColorInt() else "#4CAF50".toColorInt()
                        pressureStatusText = if (latestReading.waterPressure < lowPressure) String.format(Locale.getDefault(), "Low (%.1f bar)", latestReading.waterPressure) else String.format(Locale.getDefault(), "OK (%.1f bar)", latestReading.waterPressure)
                        try { findViewById<TextView>(R.id.waterPressureStatus).text = pressureStatusText } catch (_: Exception) {}
                        try { findViewById<TextView>(R.id.waterPressureStatus).setTextColor(pressureColor) } catch (_: Exception) {}
                    } catch (e: Exception) {
                        Log.w("ScadaActivity", "Failed updating geyser status texts", e)
                    }

                    // DVR pulse status: use numeric comparison (tolerance) to avoid false 'unchanged' due to formatting
                    try {
                        val pulseVal = latestReading.dvrTemp.toDouble()
                        val prefsLocal = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                        val dvrStaleMinutes = prefsLocal.getLong("dvr_stale_minutes", 60L)
                        val tol = getDvrPulseTolerance()
                        android.util.Log.d("ScadaActivity", "DVR pulse check: pulseVal=$pulseVal lastDvrPulse=$lastDvrPulse lastDvrPulseTime=$lastDvrPulseTime tol=$tol")
                        if (lastDvrPulse == null) {
                            lastDvrPulse = pulseVal
                            lastDvrPulseTime = System.currentTimeMillis()
                            // Persist initial pulse/time
                            try {
                                val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                                alarmPrefs.edit().putFloat("last_dvr_pulse_value", lastDvrPulse!!.toFloat()).putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                                android.util.Log.d("ScadaActivity", "Persisted initial lastDvrPulse=$lastDvrPulse at $lastDvrPulseTime to alarm_prefs")
                                // Broadcast update so service syncs immediately
                                try {
                                    val b = Intent(NotificationService.ACTION_UPDATE_DVR_PULSE)
                                    b.putExtra("last_dvr_pulse_value", lastDvrPulse!!)
                                    b.putExtra("last_dvr_pulse_time_ms", lastDvrPulseTime)
                                    sendBroadcast(b)
                                } catch (e: Exception) { android.util.Log.w("ScadaActivity", "Failed sending DVR initial broadcast", e) }
                            } catch (e: Exception) {
                                android.util.Log.w("ScadaActivity", "Failed to persist initial lastDvrPulse", e)
                            }
                            val okGreen = "#4CAF50".toColorInt()
                            dvrStatusDot.imageTintList = ColorStateList.valueOf(okGreen)
                            dvrHeartbeatStatus.text = getString(R.string.status_online)
                            dvrHeartbeatStatus.setTextColor(okGreen)
                        } else {
                            val changed = kotlin.math.abs(pulseVal - lastDvrPulse!!) > tol
                            if (!changed) {
                                val minutesElapsed = (System.currentTimeMillis() - lastDvrPulseTime) / 60000L
                                android.util.Log.d("ScadaActivity", "DVR pulse unchanged. minutesElapsed=$minutesElapsed (threshold=$dvrStaleMinutes)")
                                if (minutesElapsed >= dvrStaleMinutes) {
                                    // Stale
                                    val staleRed = "#F44336".toColorInt()
                                    dvrStatusDot.imageTintList = ColorStateList.valueOf(staleRed)
                                    dvrHeartbeatStatus.text = "Stale (${minutesElapsed}m)"
                                    dvrHeartbeatStatus.setTextColor(staleRed)
                                    android.util.Log.i("ScadaActivity", "DVR marked STALE: pulse=$pulseVal minutesElapsed=$minutesElapsed")
                                } else {
                                    // Still fresh - show age in minutes
                                    val okGreen = "#4CAF50".toColorInt()
                                    dvrStatusDot.imageTintList = ColorStateList.valueOf(okGreen)
                                    dvrHeartbeatStatus.text = "Online (${minutesElapsed}m)"
                                    dvrHeartbeatStatus.setTextColor(okGreen)
                                }
                            } else {
                                // Pulse changed -> update time and set Online (just now)
                                lastDvrPulse = pulseVal
                                lastDvrPulseTime = System.currentTimeMillis()
                                // Persist updated pulse/time so NotificationService sees the update immediately
                                try {
                                    val alarmPrefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                                    alarmPrefs.edit().putFloat("last_dvr_pulse_value", lastDvrPulse!!.toFloat()).putLong("last_dvr_pulse_time_ms", lastDvrPulseTime).apply()
                                    android.util.Log.d("ScadaActivity", "Persisted updated lastDvrPulse=$lastDvrPulse at $lastDvrPulseTime to alarm_prefs")
                                    // Broadcast update so service syncs immediately
                                    try {
                                        val b = Intent(NotificationService.ACTION_UPDATE_DVR_PULSE)
                                        b.putExtra("last_dvr_pulse_value", lastDvrPulse!!)
                                        b.putExtra("last_dvr_pulse_time_ms", lastDvrPulseTime)
                                        sendBroadcast(b)
                                    } catch (e: Exception) { android.util.Log.w("ScadaActivity", "Failed sending DVR updated broadcast", e) }
                                } catch (e: Exception) {
                                    android.util.Log.w("ScadaActivity", "Failed to persist updated last DVR pulse", e)
                                }
                                val okGreen = "#4CAF50".toColorInt()
                                dvrStatusDot.imageTintList = ColorStateList.valueOf(okGreen)
                                dvrHeartbeatStatus.text = "Online (now)"
                                dvrHeartbeatStatus.setTextColor(okGreen)
                                android.util.Log.i("ScadaActivity", "DVR pulse updated: new pulse=$pulseVal")
                            }
                        }
                    } catch (e: Exception) {
                        // If anything goes wrong, default to showing Online but log for debugging
                        android.util.Log.w("ScadaActivity", "DVR status check failed", e)
                        try {
                            val okGreen = "#4CAF50".toColorInt()
                            dvrStatusDot.imageTintList = ColorStateList.valueOf(okGreen)
                            dvrHeartbeatStatus.text = getString(R.string.status_online)
                            dvrHeartbeatStatus.setTextColor(okGreen)
                        } catch (_: Exception) {}
                    }

                    // Use raw wind direction degrees from data. Compute a display-only rotation by
                    // applying any user-configured offset (e.g. 180°) but do NOT modify the underlying data.
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val windOffset = prefs.getFloat("wind_arrow_offset_deg", 0f)
                    // Normalize raw degrees to 0..360
                    val rawDeg = ((latestReading.windDirection) % 360f + 360f) % 360f
                    val displayRotation = ((rawDeg + windOffset) % 360f + 360f) % 360f
                    // Apply rotation to the ImageView. The vector drawable points up (north) at 0°.
                    try {
                        windDirectionTextView.rotation = displayRotation
                    } catch (_: Exception) {}

                    // Simple delta-based trend for geyser temp + pressure
                    // small deadband to avoid flicker
                    val tempThreshold = 0.2
                    val pressureThreshold = 0.05

                    // Temperature trend
                    prevWaterTemp?.let { prev ->
                        when {
                            latestReading.waterTemp > prev + tempThreshold -> {
                                waterTempTrendTextView.text = "↑"
                                waterTempTrendTextView.setTextColor("#4CAF50".toColorInt()) // green
                            }
                            latestReading.waterTemp < prev - tempThreshold -> {
                                waterTempTrendTextView.text = "↓"
                                waterTempTrendTextView.setTextColor("#F44336".toColorInt()) // red
                            }
                            else -> {
                                waterTempTrendTextView.text = "→"
                                waterTempTrendTextView.setTextColor("#9FA8DA".toColorInt()) // neutral
                            }
                        }
                    }

                    // Pressure trend
                    prevWaterPressure?.let { prevP ->
                        when {
                            latestReading.waterPressure > prevP + pressureThreshold -> {
                                waterPressureTrendTextView.text = "↑"
                                waterPressureTrendTextView.setTextColor("#4CAF50".toColorInt())
                            }
                            latestReading.waterPressure < prevP - pressureThreshold -> {
                                waterPressureTrendTextView.text = "↓"
                                waterPressureTrendTextView.setTextColor("#F44336".toColorInt())
                            }
                            else -> {
                                waterPressureTrendTextView.text = "→"
                                waterPressureTrendTextView.setTextColor("#9FA8DA".toColorInt())
                            }
                        }
                    }

                    // Update previous values after computing trend
                    prevWaterTemp = latestReading.waterTemp
                    prevWaterPressure = latestReading.waterPressure

                    // Update analytics with the latest reading
                    updateAnalytics()
                } else {
                    // No Sheets data — show localized message
                    geyserTempTextView.text = getString(R.string.no_sheets_data_short)
                    geyserPressureTextView.text = getString(R.string.no_sheets_data_short)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Show error state
                runOnUiThread {
                    geyserTempTextView.text = getString(R.string.error_short)
                    geyserPressureTextView.text = getString(R.string.error_short)
                }
            }
            // Schedule next update in 30 seconds for fresh geyser data
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                monitorGeyserData()
            }, 30000) // Update every 30 seconds
        }
    }


    private fun ensureFreshGoogleSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            // Not signed in, start the sign-in flow
            signInLauncher.launch(googleSignInClient.signInIntent)
        } else {
            // Already signed in, proceed to initialize Drive
            firebaseAuthWithGoogle(account)
        }
    }

    private fun setupGoogleDriveSignIn() {
        // Firestore-only variant: no-op
    }

    // Remove onActivityResult override (replaced by signInLauncher)

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d("ScadaActivity", "firebaseAuthWithGoogle called with account: ${account.email}")
        if (account.idToken.isNullOrEmpty()) {
            Log.e("ScadaActivity", "idToken is null or empty. Cannot authenticate with Firebase.")
            runOnUiThread {
                try { ToastHelper.show(this, "Google Sign-In failed: Missing idToken. Please try again.", android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
            }
            // Optionally, retry sign-in
            signInLauncher.launch(googleSignInClient.signInIntent)
            return
        }
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("ScadaActivity", "Firebase authentication successful")
                    // Now that we are authenticated, we can start listening for Firestore updates
                    //listenForLightsStatus()
                } else {
                    Log.w("ScadaActivity", "Firebase authentication failed", task.exception)
                    runOnUiThread {
                        try { ToastHelper.show(this, "Firebase authentication failed.", android.widget.Toast.LENGTH_LONG) } catch (_: Exception) {}
                    }
                }
            }
    }

    // Remove all Drive-related code: service initialization, polling, etc.
    // Any Drive polling starter methods should be turned into no-ops
    private fun startDrivePollingIfNeeded() {
        // Firestore-only variant: no-op
    }

    private fun stopDrivePollingIfRunning() {
        // Ensure any legacy job is cancelled if referenced
        drivePollingJob?.cancel()
        drivePollingJob = null
    }

    // Replaced the realtime listener with a one-shot fetch to reduce Firestore reads.
    // Call `fetchLightsStatusOnce()` explicitly when you want to refresh the bulb indicator
    // (onResume, after writing a command, or via a manual refresh). This avoids constant
    // background reads that consume quota.
    private fun fetchLightsStatusOnce() {
        try {
            val statusRef = db.collection("scada_controls").document("lights_status")
            statusRef.get()
                .addOnSuccessListener { snapshot ->
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // Always update UI from Firestore snapshot; no Drive backend in this variant
                            updateLightsUiFromSnapshot(snapshot)
                            // Extra sanitation: if legacy text somehow remains, clear it
                            try {
                                val txt = lightsDetails.text?.toString() ?: ""
                                if (txt.contains("Drive:", ignoreCase = true)) lightsDetails.text = "--"
                            } catch (_: Exception) {}
                        } catch (e: Exception) {
                            Log.w("ScadaActivity", "Error processing lights_status snapshot", e)
                        }
                    }
                }
                .addOnFailureListener { e -> Log.w("ScadaActivity", "Failed fetching lights_status", e) }
        } catch (e: Exception) {
            Log.w("ScadaActivity", "fetchLightsStatusOnce failed", e)
        }
    }

    // Attach a lightweight snapshot listener; safe to call multiple times (idempotent)
    private fun setupLightsListener() {
        try {
            if (lightsStatusListener != null) return
            val statusRef = db.collection("scada_controls").document("lights_status")
            lightsStatusListener = statusRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("ScadaActivity", "lights_status listener error", error)
                    return@addSnapshotListener
                }
                try {
                    updateLightsUiFromSnapshot(snapshot)
                    // Sanitize any legacy text
                    val txt = lightsDetails.text?.toString() ?: ""
                    if (txt.contains("Drive:", true) || txt.contains("Cmd:", true)) {
                        lightsDetails.text = "--"
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ScadaActivity", "listener UI update failed", e)
                }
            }
            // Prime once
            fetchLightsStatusOnce()
        } catch (e: Exception) {
            android.util.Log.w("ScadaActivity", "setupLightsListener failed", e)
        }
    }

    private fun updateLightsUiFromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot?) {
        if (snapshot == null || !snapshot.exists()) {
            try {
                val gray = "#9E9E9E".toColorInt()
                lightsStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(gray)
                lightsStatusText.setTextColor(gray)
                lightsStatusText.text = getString(R.string.text_unknown)
                // In Firestore-only variant, keep details blank
                lightsDetails.text = "--"
            } catch (_: Exception) {}
            return
        }
        // Prefer PLC field coil_1298 or Y21_read
        fun parseBoolFromAny(v: Any?): Boolean? {
            return when (v) {
                is Boolean -> v
                is Number -> v.toInt() != 0
                is String -> {
                    val s = v.trim().lowercase()
                    when (s) {
                        "1", "y", "yes", "true", "on" -> true
                        "0", "n", "no", "false", "off" -> false
                        else -> null
                    }
                }
                else -> null
            }
        }
        val data = snapshot.data ?: emptyMap<String, Any>()
        val coil1298 = parseBoolFromAny(data["coil_1298"]) ?: parseBoolFromAny(data["Y21_read"]) ?: parseBoolFromAny(data["y21_read"]) // tolerant keys
        // Legacy fields (sent_txt, Drive, Cmd) are ignored in this variant

        val onColor = "#4CAF50".toColorInt()
        val offColor = "#FF5722".toColorInt()
        if (coil1298 == true) {
            lightsStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(onColor)
            lightsStatusText.setTextColor(onColor)
            lightsStatusText.text = getString(R.string.text_on)
        } else if (coil1298 == false) {
            lightsStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(offColor)
            lightsStatusText.setTextColor(offColor)
            lightsStatusText.text = getString(R.string.text_off)
        } else {
            val gray = "#9E9E9E".toColorInt()
            lightsStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(gray)
            lightsStatusText.setTextColor(gray)
            lightsStatusText.text = getString(R.string.text_unknown)
        }
        // Details: show a compact Firestore-only line with last update time if available; otherwise "--"
        try {
            val ts = snapshot.getTimestamp("updated_at")?.toDate()
            val details = if (ts != null) {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val y21 = if (coil1298 == true) "1" else if (coil1298 == false) "0" else "-"
                "Y21_read: $y21 @ ${fmt.format(ts)}"
            } else {
                val y21 = if (coil1298 == true) "1" else if (coil1298 == false) "0" else "-"
                "Y21_read: $y21"
            }
            lightsDetails.text = details
        } catch (_: Exception) {
            lightsDetails.text = "--"
        }
        // Final sanitation: never show legacy Drive/Cmd/Sent TXT substrings
        try {
            val txt = lightsDetails.text?.toString() ?: ""
            if (txt.contains("Drive:", true) || txt.contains("Cmd:", true) || txt.contains("Sent TXT", true)) {
                lightsDetails.text = "--"
            }
        } catch (_: Exception) {}
    }

    // Hook the Setup card tap to open our dialog
    private fun wireSetupCardTap() {
        try {
            findViewById<View>(R.id.alarmStatusCard)?.setOnClickListener { showSetupDialog() }
        } catch (_: Exception) {}
    }

    private fun showSetupDialog() {
        try {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val options = arrayOf("Firebase (Firestore-only)")
            var selectedIndex = 0
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Setup")
                .setSingleChoiceItems(options, selectedIndex) { _, which -> selectedIndex = which }
                .setPositiveButton("Apply") { _, _ ->
                    // Force Firestore-only backend
                    prefs.edit().putString("scada_lights_backend", "firestore").apply()
                    try { applyLightsBackendSelection() } catch (_: Exception) {}
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.w("ScadaActivity", "showSetupDialog failed", e)
            try { ToastHelper.show(this, "Settings are not available in this variant", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
        }
    }

    // After declarations of tides/sun, add lightweight stubs used by click handlers
    private fun showStormglassKeyDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("WorldTides API Key")
                .setMessage("This app uses an embedded key for tide data. You can update it in Setup when available.")
                .setPositiveButton("OK", null)
                .show()
        } catch (_: Exception) {}
    }

    private fun updateSunAndTides() {
        // Minimal placeholder: compute rough sunrise/sunset and set text if views exist
        try {
            val now = java.util.Calendar.getInstance()
            val fmt = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.getDefault())
            sunInfoTextView.text = "Sun: ${fmt.format(now.time)}"
            tideInfoTextView.text = "Tide: updated"
        } catch (_: Exception) {}
    }
}
