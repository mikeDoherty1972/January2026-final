package com.security.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ImageView
import android.content.res.ColorStateList
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.client.http.javanet.NetHttpTransport
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

class ScadaActivity : BaseActivity() {

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
    private var drivePollingJob: kotlinx.coroutines.Job? = null

    // Google Drive API
    private lateinit var googleSignInClient: GoogleSignInClient
    private var driveService: Drive? = null
    // Activity Result launcher replaces deprecated startActivityForResult
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    private lateinit var auth: FirebaseAuth
    // Firestore listener registration for lights_status
    private var lightsStatusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ScadaActivity", "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scada)

        // Initialize Activity Result launcher for Google Sign-In
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val dataIntent = result.data
                    val task = GoogleSignIn.getSignedInAccountFromIntent(dataIntent)
                    if (task.isSuccessful) {
                        val account = task.result
                        if (account?.idToken.isNullOrEmpty()) {
                            runOnUiThread {
                                android.widget.Toast.makeText(this, getString(R.string.google_signin_failed_missing_idtoken), android.widget.Toast.LENGTH_LONG).show()
                            }
                            return@registerForActivityResult
                        }
                        // account non-null here
                        firebaseAuthWithGoogle(account)
                        initializeDriveService(account)
                    } else {
                        runOnUiThread {
                            android.widget.Toast.makeText(this, getString(R.string.google_signin_failed_generic, task.exception?.localizedMessage ?: ""), android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    runOnUiThread { android.widget.Toast.makeText(this, getString(R.string.google_signin_cancelled), android.widget.Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) {
                Log.w("ScadaActivity", "Sign-in launcher callback error", e)
                runOnUiThread { android.widget.Toast.makeText(this, getString(R.string.google_signin_failed_generic, e.localizedMessage ?: ""), android.widget.Toast.LENGTH_LONG).show() }
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
                            val card = vp as androidx.cardview.widget.CardView
                            card.isClickable = true
                            card.isLongClickable = true
                            card.setOnClickListener { showStormglassKeyDialog() }
                            card.setOnLongClickListener { showStormglassKeyDialog(); true }
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
        // Setup lights control buttons
        val lightsOnButton = findViewById<android.widget.Button>(R.id.lightsOnButton)
        val lightsOffButton = findViewById<android.widget.Button>(R.id.lightsOffButton)
        // Enable buttons immediately so users can send Firebase/bridge commands even when Drive isn't signed-in.
        lightsOnButton.isEnabled = true
        lightsOffButton.isEnabled = true
        Log.d("ScadaActivity", "Lights buttons enabled at startup")
        // New: Firestore-based lights commands (replace Drive-based flow)
        val dbFs = FirebaseFirestore.getInstance()
        // Bridge expects Flights_commands/current_command — write there as well for compatibility with the local bridge
        val bridgeCmdDoc = dbFs.collection("Flights_commands").document("current_command")

        // Helper that writes a bridge-compatible payload to Flights_commands/current_command
        fun writeBridgeCommand(desired: Boolean, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
            try {
                val bridgePayload = hashMapOf<String, Any>(
                    "desired" to desired,
                    "desired_ts" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "command" to if (desired) "on" else "off",
                    "command_ts" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "command_source" to "android_app"
                )
                bridgeCmdDoc.set(bridgePayload, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("ScadaActivity", "Wrote bridge command doc (desired=$desired)")
                        // Read back the document to confirm the write landed and log/notify user for debugging
                        try {
                            bridgeCmdDoc.get().addOnSuccessListener { bdoc ->
                                Log.d("ScadaActivity", "bridgeCmdDoc after write: ${bdoc.data}")
                                val sb = bdoc.data?.toString() ?: "(no bridge doc)"
                                try { runOnUiThread { android.widget.Toast.makeText(this@ScadaActivity, getString(R.string.bridge_doc_readback, sb), android.widget.Toast.LENGTH_LONG).show() } } catch (_: Exception) {}
                            }.addOnFailureListener { e -> Log.w("ScadaDiag", "Failed reading bridgeCmdDoc", e) }
                        } catch (e: Exception) {
                            Log.w("ScadaDiag", "Readback failed", e)
                        }
                        onSuccess?.invoke()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ScadaActivity", "Failed writing bridge command doc", e)
                        onFailure?.invoke(e)
                    }
            } catch (e: Exception) {
                Log.e("ScadaActivity", "Exception preparing bridge payload", e)
                onFailure?.invoke(e)
            }
        }

        lightsOnButton.setOnClickListener {
             Log.d("ScadaActivity", "Lights ON clicked — writing bridge command")
             writeBridgeCommand(true,
                 onSuccess = {
                     runOnUiThread { android.widget.Toast.makeText(this, getString(R.string.bridge_command_on_sent), android.widget.Toast.LENGTH_SHORT).show() }
                     try { startLightsPolling() } catch (_: Exception) {}
                 },
                 onFailure = { e ->
                     runOnUiThread { android.widget.Toast.makeText(this, getString(R.string.failed_send_bridge_command, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show() }
                     try { ensureFreshGoogleSignIn() } catch (_: Exception) {}
                 }
             )
         }

         lightsOffButton.setOnClickListener {
             Log.d("ScadaActivity", "Lights OFF clicked — writing bridge command")
             writeBridgeCommand(false,
                 onSuccess = {
                     runOnUiThread { android.widget.Toast.makeText(this, getString(R.string.bridge_command_off_sent), android.widget.Toast.LENGTH_SHORT).show() }
                     try { startLightsPolling() } catch (_: Exception) {}
                 },
                 onFailure = { e ->
                     runOnUiThread { android.widget.Toast.makeText(this, getString(R.string.failed_send_bridge_command, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show() }
                     try { ensureFreshGoogleSignIn() } catch (_: Exception) {}
                 }
             )
         }

        // Setup click listeners for graphs
        setupGraphClickListeners()

        // diagnosticsText long-press removed — appearance controls moved into the Settings dialog (alarmStatusCard).

        // Firebase monitoring removed - all data now from Google Sheets

        // Start Google Sheets monitoring for geyser data
        monitorGeyserData()

        // Also update sun/tide info now
        try { updateSunAndTides() } catch (e: Exception) { Log.w("ScadaActivity", "updateSunAndTides failed", e) }

        // Setup Google Drive sign-in
        setupGoogleDriveSignIn()
        // Start polling for Y21_read_status from Drive file
        startY21ReadPolling()

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
                .requestScopes(Scope(DriveScopes.DRIVE)) // Changed from DRIVE_FILE to DRIVE
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
    }

    private fun refreshData() {
        monitorGeyserData()
        android.widget.Toast.makeText(this, "Data refreshed", android.widget.Toast.LENGTH_SHORT).show()
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
                                    val fetched = withContext(Dispatchers.IO) { dvrSheetsReader.fetchLatestDvrReadings(1) }
                                    if (!fetched.isNullOrEmpty()) {
                                        sourceDvr = fetched[0].dvrTemp.toDouble()
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
                                    val dvrRows = dvrSheetsReader.fetchLatestDvrReadings(1)
                                    if (dvrRows.isNotEmpty()) {
                                        val d = dvrRows[0]
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
                                val dvrRows = dvrSheetsReader.fetchLatestDvrReadings(1)
                                if (dvrRows.isNotEmpty()) {
                                    val d = dvrRows[0]
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
            initializeDriveService(account)
        }
    }

    private fun setupGoogleDriveSignIn() {
        Log.d("ScadaActivity", "setupGoogleDriveSignIn called")
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE)) // Changed from DRIVE_FILE to DRIVE
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInClient.silentSignIn().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val account = task.result
                firebaseAuthWithGoogle(account)
                initializeDriveService(account)
            } else {
                // Silent sign-in failed, fallback to interactive sign-in
                signInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    // Remove onActivityResult override (replaced by signInLauncher)

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d("ScadaActivity", "firebaseAuthWithGoogle called with account: ${account.email}")
        if (account.idToken.isNullOrEmpty()) {
            Log.e("ScadaActivity", "idToken is null or empty. Cannot authenticate with Firebase.")
            runOnUiThread {
                android.widget.Toast.makeText(this, "Google Sign-In failed: Missing idToken. Please try again.", android.widget.Toast.LENGTH_LONG).show()
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
                        android.widget.Toast.makeText(this, "Firebase authentication failed.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun initializeDriveService(account: GoogleSignInAccount?) {
        Log.d("ScadaActivity", "initializeDriveService called with account: $account")
        if (account == null) return
        Log.i("ScadaActivity", "Signed-in Google account: ${account.email}")
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(DriveScopes.DRIVE) // Changed from DRIVE_FILE to DRIVE
        )
        credential.selectedAccount = account.account
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Home Automation").build()
        // Enable lights buttons now that Drive is ready
        runOnUiThread {
            findViewById<android.widget.Button>(R.id.lightsOnButton).isEnabled = true
            findViewById<android.widget.Button>(R.id.lightsOffButton).isEnabled = true
            Log.d("ScadaActivity", "Lights buttons enabled after Drive initialization")
        }
        // List all accessible files for debugging
        CoroutineScope(Dispatchers.IO).launch {
            listAllDriveFilesForDebug()
        }
        // We no longer poll the Drive lights file. Instead listen for status updates in Firestore.
        // Start Firestore listener for lights status so UI shows canonical confirmation from the bridge.
        try { fetchLightsStatusOnce() } catch (_: Exception) {}
        // Ensure lights backend selection is applied now that Drive is initialized. If the user
        // selected 'drive' as the backend, this will start the drive polling job.
        try { applyLightsBackendSelection() } catch (_: Exception) {}
    }

    // List all accessible files and log their names and IDs
    private suspend fun listAllDriveFilesForDebug() = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext
        try {
            val result = drive.files().list().setPageSize(100).setFields("files(id, name)").execute()
            val files = result.files
            if (files == null || files.isEmpty()) {
                Log.d("ScadaActivity", "[DEBUG] No files found in Drive.")
            } else {
                Log.d("ScadaActivity", "[DEBUG] Files accessible to app:")
                for (file in files) {
                    Log.d("ScadaActivity", "[DEBUG] File: ${file.name} (ID: ${file.id})")
                }
            }
        } catch (e: Exception) {
            Log.e("ScadaActivity", "[DEBUG] Error listing Drive files", e)
        }
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
                    // process on main dispatcher
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            if (snapshot == null || !snapshot.exists()) return@launch

                            val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            val backend = appPrefs.getString("lights_control_backend", "firebase") ?: "firebase"

                            val lightsState: Boolean? = if (backend == "drive") {
                                val y21 = snapshot.getStringSafe("Y21_read_status") ?: snapshot.getStringSafe("Y21_read") ?: snapshot.getStringSafe("Y21")
                                val m21 = snapshot.getStringSafe("M21_read") ?: snapshot.getStringSafe("M21")
                                val driveValue = m21 ?: y21
                                when {
                                    driveValue?.contains("ON", ignoreCase = true) == true -> true
                                    driveValue?.contains("1") == true -> true
                                    driveValue?.contains("OFF", ignoreCase = true) == true -> false
                                    driveValue?.contains("0") == true -> false
                                    else -> null
                                }
                            } else {
                                val state = when {
                                    snapshot.contains("coil_1298_state") -> snapshot.getBoolean("coil_1298_state")
                                    snapshot.contains("coil_1298") -> snapshot.getBoolean("coil_1298")
                                    snapshot.contains("coil_1298_state_bool") -> snapshot.getBoolean("coil_1298_state_bool")
                                    snapshot.contains("coil_1298_state_str") -> try { snapshot.getString("coil_1298_state_str")?.toBoolean() } catch (_: Exception) { null }
                                    snapshot.contains("state") -> snapshot.getBoolean("state")
                                    snapshot.contains("actual_state") -> snapshot.getBoolean("actual_state")
                                    else -> null
                                }
                                if (state == null) Log.w("ScadaActivity", "FCM mode: No coil_1298 state found in snapshot. Available fields: ${snapshot.data?.keys}")
                                state
                            }

                            val lastCmd = snapshot.getString("last_command") ?: "--"
                            val lastActionAny = snapshot.get("last_action_ts")
                            val lastAction = when (lastActionAny) {
                                is com.google.firebase.Timestamp -> try { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(java.util.Date(lastActionAny.toDate().time)) } catch (_: Exception) { "" }
                                is String -> lastActionAny
                                else -> ""
                            }

                            // Update UI
                            try {
                                val color = when (lightsState) {
                                    true -> "#4CAF50".toColorInt()
                                    false -> "#FF5722".toColorInt()
                                    null -> "#9E9E9E".toColorInt()
                                }
                                lightsStatusIcon.imageTintList = ColorStateList.valueOf(color)
                                lightsStatusText.setTextColor(color)
                                lightsStatusText.text = when (lightsState) {
                                    true -> getString(R.string.text_on)
                                    false -> getString(R.string.text_off)
                                    null -> getString(R.string.text_unknown)
                                }

                                if (backend == "drive") {
                                     val y21 = snapshot.getStringSafe("Y21_read_status") ?: snapshot.getStringSafe("Y21_read") ?: snapshot.getStringSafe("Y21")
                                     val m21 = snapshot.getStringSafe("M21_read") ?: snapshot.getStringSafe("M21")
                                     val driveText = when {
                                         !m21.isNullOrEmpty() -> m21
                                         !y21.isNullOrEmpty() -> y21
                                         else -> "--"
                                     }
                                     lightsDetails.text = getString(R.string.drive_prefix, driveText)
                                 } else {
                                    val yVal = lightsState?.toString() ?: "--"
                                    val details = mutableListOf<String>()
                                    details.add(getString(R.string.y1298_format, yVal))
                                    details.add(getString(R.string.cmd_format, lastCmd))
                                    if (lastAction.isNotEmpty()) details.add(getString(R.string.at_format, lastAction))
                                    lightsDetails.text = details.joinToString(" ")
                                }
                            } catch (e: Exception) {
                                Log.w("ScadaActivity", "Failed update lights indicator from snapshot", e)
                            }

                        } catch (e: Exception) {
                            Log.w("ScadaActivity", "Error processing lights_status snapshot", e)
                        }
                    }
                }
                .addOnFailureListener { e -> Log.w("ScadaActivity", "Failed fetching lights_status", e) }
        } catch (e: Exception) {
            Log.w("ScadaActivity", "setupLightsListener failed", e)
        }
    }

    // ---- Stubs to satisfy references during cleanup. These are small no-ops that log actions.
    private fun setupLightsListener() {
        // Previously this started a realtime listener. For now we perform a single fetch.
        try { fetchLightsStatusOnce() } catch (_: Exception) { Log.w("ScadaActivity", "setupLightsListener no-op") }
    }

    private fun startLightsPolling() {
        // Starts a short polling job; keep as no-op here (existing code conditionally calls it)
        Log.d("ScadaActivity", "startLightsPolling called - no-op stub")
    }

    private fun startY21ReadPolling() {
        Log.d("ScadaActivity", "startY21ReadPolling called - no-op stub")
    }

    private fun openAlarmSettingsDialog() {
        // Debugging: log entry so we can confirm clicks reach this method via Logcat
        Log.d("ScadaActivity", "openAlarmSettingsDialog called")
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        // App-level prefs used for appearance/wind/backend selection
        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_alarm_settings, null)

        // Bind dialog views (use nullable types to tolerate layout variants)
        val etLowPressure = dialogView.findViewById<android.widget.EditText?>(R.id.et_low_pressure)
        val etHighAmps = dialogView.findViewById<android.widget.EditText?>(R.id.et_high_amps)
        val etHighAmpsDuration = dialogView.findViewById<android.widget.EditText?>(R.id.et_high_amps_duration)
        val etWaterVariance = dialogView.findViewById<android.widget.EditText?>(R.id.et_water_variance)
        val etDvrMinutes = dialogView.findViewById<android.widget.EditText?>(R.id.et_dvr_stale_minutes)
        val etWaterHourlyThreshold = dialogView.findViewById<android.widget.EditText?>(R.id.et_water_hourly_threshold)
        val etWaterHourlyHours = dialogView.findViewById<android.widget.EditText?>(R.id.et_water_hourly_hours)
        val switchInvertWind = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat?>(R.id.switch_invert_wind)
        // Appearance buttons (moved here from diagnostics long-press)
        val btnCardColor = dialogView.findViewById<android.widget.Button?>(R.id.btn_card_color)
        val btnGraphColor = dialogView.findViewById<android.widget.Button?>(R.id.btn_graph_color)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btn_save_alarms)

        // New: time picker buttons for security schedule
        val btnEnableTime = dialogView.findViewById<android.widget.Button?>(R.id.btn_security_enable_time)
        val btnDisableTime = dialogView.findViewById<android.widget.Button?>(R.id.btn_security_disable_time)
        val btnClearSecuritySchedule = dialogView.findViewById<android.widget.Button?>(R.id.btn_clear_security_schedule)

        // Load stored schedule and set button labels
        try {
            val seH = appPrefs.getInt("security_enable_hour", -1)
            val seM = appPrefs.getInt("security_enable_min", 0)
            val sdH = appPrefs.getInt("security_disable_hour", -1)
            val sdM = appPrefs.getInt("security_disable_min", 0)
            if (seH >= 0) btnEnableTime?.text = String.format(Locale.getDefault(), "%02d:%02d", seH, seM) else btnEnableTime?.text = getString(R.string.set_time)
            if (sdH >= 0) btnDisableTime?.text = String.format(Locale.getDefault(), "%02d:%02d", sdH, sdM) else btnDisableTime?.text = getString(R.string.set_time)
        } catch (_: Exception) {}

        // Helper to show TimePicker and write selection to button text
        fun openTimePicker(initialH: Int?, initialM: Int?, onPicked: (Int, Int) -> Unit) {
            val now = java.util.Calendar.getInstance()
            val h = initialH ?: now.get(java.util.Calendar.HOUR_OF_DAY)
            val m = initialM ?: now.get(java.util.Calendar.MINUTE)
            val picker = android.app.TimePickerDialog(this, { _, hourOfDay, minute -> onPicked(hourOfDay, minute) }, h, m, true)
            picker.show()
        }

        btnEnableTime?.setOnClickListener {
            try {
                val seH = appPrefs.getInt("security_enable_hour", -1).takeIf { it >= 0 }
                val seM = appPrefs.getInt("security_enable_min", 0)
                openTimePicker(seH, seM) { hh, mm -> btnEnableTime.text = String.format(Locale.getDefault(), "%02d:%02d", hh, mm); appPrefs.edit { putInt("security_enable_hour", hh); putInt("security_enable_min", mm) } }
            } catch (e: Exception) { android.util.Log.w("ScadaActivity", "Enable time picker failed", e) }
        }

        btnDisableTime?.setOnClickListener {
            try {
                val sdH = appPrefs.getInt("security_disable_hour", -1).takeIf { it >= 0 }
                val sdM = appPrefs.getInt("security_disable_min", 0)
                openTimePicker(sdH, sdM) { hh, mm -> btnDisableTime.text = String.format(Locale.getDefault(), "%02d:%02d", hh, mm); appPrefs.edit { putInt("security_disable_hour", hh); putInt("security_disable_min", mm) } }
            } catch (e: Exception) { android.util.Log.w("ScadaActivity", "Disable time picker failed", e) }
        }

        // Wire preset spinners: liters and hours
        val spinnerLiters = dialogView.findViewById<android.widget.Spinner?>(R.id.spinner_hourly_preset)
        val spinnerHours = dialogView.findViewById<android.widget.Spinner?>(R.id.spinner_hourly_hours_preset)
        try {
            val literOptions = arrayOf("100 L", "250 L", "500 L", "1000 L")
            val literAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, literOptions)
            literAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLiters?.adapter = literAdapter
            spinnerLiters?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    val text = literOptions[position]
                    val num = text.split(" ").firstOrNull()?.toFloatOrNull() ?: return
                    etWaterHourlyThreshold?.setText(String.format(Locale.getDefault(), "%.0f", num))
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }

            val hourOptions = arrayOf("1 h", "2 h", "4 h", "8 h")
            val hourAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, hourOptions)
            hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHours?.adapter = hourAdapter
            spinnerHours?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    val text = hourOptions[position]
                    val num = text.split(" ").firstOrNull()?.toIntOrNull() ?: return
                    etWaterHourlyHours?.setText(num.toString())
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        } catch (_: Exception) {}

        // Load security schedule into dialog from appPrefs
        try {
            val seH = appPrefs.getInt("security_enable_hour", -1)
            val seM = appPrefs.getInt("security_enable_min", 0)
            val sdH = appPrefs.getInt("security_disable_hour", -1)
            val sdM = appPrefs.getInt("security_disable_min", 0)
            if (seH >= 0) btnEnableTime?.text = String.format(Locale.getDefault(), "%02d:%02d", seH, seM) else btnEnableTime?.text = getString(R.string.set_time)
            if (sdH >= 0) btnDisableTime?.text = String.format(Locale.getDefault(), "%02d:%02d", sdH, sdM) else btnDisableTime?.text = getString(R.string.set_time)
        } catch (_: Exception) {}

        // Load current prefs into dialog fields (safe calls)
        try { etLowPressure?.setText(prefs.getFloat("low_pressure_threshold", 1.0f).toString()) } catch (_: Exception) {}
        try { etHighAmps?.setText(prefs.getFloat("high_amps_threshold", 6.0f).toString()) } catch (_: Exception) {}
        try { etHighAmpsDuration?.setText((prefs.getLong("high_amps_duration_ms", 3600000L) / 60000L).toString()) } catch (_: Exception) {}
        try { etWaterVariance?.setText(prefs.getFloat("water_variance_threshold", 0.1f).toString()) } catch (_: Exception) {}
        try { etDvrMinutes?.setText((prefs.getLong("dvr_stale_minutes", 60L)).toString()) } catch (_: Exception) {}
        try { etWaterHourlyThreshold?.setText(prefs.getFloat("water_hourly_volume_threshold", 500f).toString()) } catch (_: Exception) {}
        try { etWaterHourlyHours?.setText(prefs.getInt("water_hourly_window_hours", 1).toString()) } catch (_: Exception) {}

        // Ensure spinner choices and radio group reflect the saved values (avoid showing defaults)
        try {
            val literOptions = arrayOf("100 L", "250 L", "500 L", "1000 L")
            val currentHourly = etWaterHourlyThreshold?.text.toString().toFloatOrNull()
            if (currentHourly != null) {
                val idx = literOptions.indexOfFirst { opt ->
                    opt.split(" ").firstOrNull()?.toFloatOrNull() == currentHourly
                }.coerceAtLeast(0)
                spinnerLiters?.setSelection(idx)
            }
        } catch (_: Exception) {}

        try {
            val hourOptions = arrayOf("1 h", "2 h", "4 h", "8 h")
            val currentHours = etWaterHourlyHours?.text.toString().toIntOrNull() ?: 1
            val idx = hourOptions.indexOfFirst { opt -> opt.split(" ").firstOrNull()?.toIntOrNull() == currentHours }
            if (idx >= 0) spinnerHours?.setSelection(idx) else spinnerHours?.setSelection(0)
        } catch (_: Exception) {}

        try {
            val rg = dialogView.findViewById<android.widget.RadioGroup?>(R.id.rg_lights_control)
            val backendPref = appPrefs.getString("lights_control_backend", "firebase") ?: "firebase"
            if (backendPref == "drive") {
                rg?.check(R.id.rb_control_drive)
            } else {
                rg?.check(R.id.rb_control_firebase)
            }
        } catch (_: Exception) {}

        // Initialize invert wind and live preview from global app prefs
        val currentOffset = appPrefs.getFloat("wind_arrow_offset_deg", 0f)

        // Dialog preview controls
        val previewArrow = dialogView.findViewById<ImageView?>(R.id.preview_wind_arrow)
        val tvOffset = dialogView.findViewById<TextView?>(R.id.tv_wind_offset_value)
        val seekOffset = dialogView.findViewById<android.widget.SeekBar?>(R.id.seek_wind_offset)

        // Set initial preview and seek position
        val normalizedCurrent = ((currentOffset) % 360f + 360f) % 360f
        seekOffset?.progress = normalizedCurrent.toInt()
        tvOffset?.text = getString(R.string.offset_format, normalizedCurrent.toInt())
        try { previewArrow?.rotation = normalizedCurrent } catch (_: Exception) {}

        // Switch reflects zero vs non-zero offset (invert convenience)
        switchInvertWind?.isChecked = (normalizedCurrent % 360f) != 0f

        // SeekBar listener updates preview arrow smoothly (animate rotation)
        seekOffset?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvOffset?.text = getString(R.string.offset_format, progress)
                try {
                    val from = previewArrow?.rotation ?: 0f
                    val to = progress.toFloat()
                    val animator = android.animation.ObjectAnimator.ofFloat(previewArrow, "rotation", from, to)
                    animator.duration = 250
                    animator.start()
                } catch (_: Exception) {}
                try { switchInvertWind?.isChecked = (progress % 360) != 0 } catch (_: Exception) {}
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Switch toggles quick 180° vs 0° and updates preview/seek
        switchInvertWind?.setOnCheckedChangeListener { _, isChecked ->
            val newOffset = if (isChecked) 180f else 0f
            try { seekOffset?.progress = newOffset.toInt() } catch (_: Exception) {}
        }

        // Appearance button handlers: reuse showColorPicker helper already present in the activity
        btnCardColor?.setOnClickListener {
            showColorPicker("card_scada_color", "Choose card background color") {
                android.widget.Toast.makeText(this, "Card color saved", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        btnGraphColor?.setOnClickListener {
            showColorPicker("graph_background_color", "Choose graph background color") {
                android.widget.Toast.makeText(this, "Graph color saved", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Load recent alarms from shared prefs and populate the Recent Alarms card
        try {
            val historyPrefs = getSharedPreferences("alarm_history", Context.MODE_PRIVATE)
            val rawJson = historyPrefs.getString("recent_alarms_json", "[]") ?: "[]"
            val arr = org.json.JSONArray(rawJson)
            val slots = listOf(
                dialogView.findViewById<TextView?>(R.id.recent_alarm_1),
                dialogView.findViewById<TextView?>(R.id.recent_alarm_2),
                dialogView.findViewById<TextView?>(R.id.recent_alarm_3),
                dialogView.findViewById<TextView?>(R.id.recent_alarm_4),
                dialogView.findViewById<TextView?>(R.id.recent_alarm_5)
            )
            for (i in 0 until 5) {
                val tv = slots[i]
                val obj = if (i < arr.length()) try { arr.getJSONObject(i) } catch (_: Exception) { null } else null
                if (obj != null) {
                    val timeMs = obj.optLong("time_ms", 0L)
                    val title = obj.optString("title", "")
                    val msg = obj.optString("message", "")
                    val timeStr = if (timeMs > 0L) try { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(java.util.Date(timeMs)) } catch (_: Exception) { "" } else ""
                    if (timeStr.isNotEmpty()) tv?.text = getString(R.string.recent_alarm_with_time, i+1, timeStr, title, msg) else if (title.isNotEmpty()) tv?.text = getString(R.string.recent_alarm_no_time, i+1, title, msg) else tv?.text = getString(R.string.recent_alarm_empty, i+1)
                } else {
                    tv?.text = getString(R.string.recent_alarm_empty, i+1)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ScadaActivity", "Failed to load recent alarms", e)
        }

        // Wire Clear History button
        try {
            val btnClear = dialogView.findViewById<android.widget.Button>(R.id.btn_clear_history)
            btnClear.setOnClickListener {
                try {
                    val historyPrefs = getSharedPreferences("alarm_history", Context.MODE_PRIVATE)
                    historyPrefs.edit { putString("recent_alarms_json", "[]") }
                    for (i in 1..5) {
                        val tv = dialogView.findViewById<TextView?>(resources.getIdentifier("recent_alarm_$i", "id", packageName))
                        tv?.text = getString(R.string.recent_alarm_empty, i)
                    }
                    android.widget.Toast.makeText(this, "Alarm history cleared", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.w("ScadaActivity", "Failed to clear alarm history", e)
                }
            }
        } catch (_: Exception) {}

        // Save button handler: persist offsets + other prefs
        btnSave?.setOnClickListener {
            val lowP = etLowPressure?.text.toString().toFloatOrNull() ?: 1.0f
            val highA = try { etHighAmps?.text.toString().toFloatOrNull() ?: prefs.getFloat("high_amps_threshold", 6.0f) } catch (_: Exception) { prefs.getFloat("high_amps_threshold", 6.0f) }
            val highADurMin = etHighAmpsDuration?.text.toString().toLongOrNull() ?: 60L
            val variance = etWaterVariance?.text.toString().toFloatOrNull() ?: 0.1f
            val dvrMin = etDvrMinutes?.text.toString().toLongOrNull() ?: 60L
            val hourlyThreshold = etWaterHourlyThreshold?.text.toString().toFloatOrNull() ?: 500f
            val hourlyWindowHours = etWaterHourlyHours?.text.toString().toIntOrNull() ?: 1

            try {
                prefs.edit {
                    putFloat("low_pressure_threshold", lowP)
                    putFloat("high_amps_threshold", highA)
                    putLong("high_amps_duration_ms", highADurMin * 60000L)
                    putFloat("water_variance_threshold", variance)
                    putLong("dvr_stale_minutes", dvrMin)
                    putFloat("water_hourly_volume_threshold", hourlyThreshold)
                    putInt("water_hourly_window_hours", hourlyWindowHours)
                }
            } catch (e: Exception) {
                android.util.Log.w("ScadaActivity", "Failed to save alarm prefs", e)
            }

            // Also save current seek offset & lights backend into app prefs synchronously
            try {
                val selectedOffset = dialogView.findViewById<android.widget.SeekBar?>(R.id.seek_wind_offset)?.progress?.toFloat() ?: currentOffset
                appPrefs.edit {
                    putFloat("wind_arrow_offset_deg", selectedOffset)
                    val rg = dialogView.findViewById<android.widget.RadioGroup?>(R.id.rg_lights_control)
                    val selected = when (rg?.checkedRadioButtonId) {
                        R.id.rb_control_drive -> "drive"
                        else -> "firebase"
                    }
                    putString("lights_control_backend", selected)
                }
            } catch (e: Exception) {
                android.util.Log.w("ScadaActivity", "Failed to save app prefs", e)
            }

            android.widget.Toast.makeText(this, "Alarm settings saved", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            updateAlarmStatus()
            applyLightsBackendSelection()
        }

        // Clear schedule handler
        try {
            btnClearSecuritySchedule?.setOnClickListener {
                try {
                    appPrefs.edit { remove("security_enable_hour"); remove("security_enable_min"); remove("security_disable_hour"); remove("security_disable_min") }
                    // update buttons to default label
                    dialogView.findViewById<android.widget.Button?>(R.id.btn_security_enable_time)?.text = getString(R.string.set_time)
                    dialogView.findViewById<android.widget.Button?>(R.id.btn_security_disable_time)?.text = getString(R.string.set_time)
                    android.widget.Toast.makeText(this, "Security schedule cleared", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { android.util.Log.w("ScadaActivity", "Failed clearing security schedule", e) }
            }
        } catch (_: Exception) {}

        // Play tone preview handler
        val btnPlayTone = dialogView.findViewById<android.widget.Button>(R.id.btn_play_tone)
        var previewPlayer: android.media.MediaPlayer? = null
        btnPlayTone.setOnClickListener {
            try {
                if (previewPlayer == null) {
                    val afd = resources.openRawResourceFd(R.raw.alarm_tone)
                    previewPlayer = android.media.MediaPlayer()
                    previewPlayer?.setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    previewPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    previewPlayer?.isLooping = false
                    previewPlayer?.prepare()
                    previewPlayer?.start()
                    btnPlayTone.text = getString(R.string.stop)
                } else {
                    try { previewPlayer?.stop() } catch (_: Exception) {}
                    try { previewPlayer?.release() } catch (_: Exception) {}
                    previewPlayer = null
                    btnPlayTone.text = getString(R.string.play_tone)
                }
            } catch (e: Exception) {
                android.util.Log.e("ScadaActivity", "Failed to play preview tone", e)
                android.widget.Toast.makeText(this, "Unable to play tone", android.widget.Toast.LENGTH_SHORT).show()
                try { previewPlayer?.release() } catch (_: Exception) {}
                previewPlayer = null
                btnPlayTone.text = getString(R.string.play_tone)
            }
        }

        // Ensure we stop preview if dialog dismissed
        dialog.setOnDismissListener {
            try { previewPlayer?.stop() } catch (_: Exception) {}
            try { previewPlayer?.release() } catch (_: Exception) {}
            previewPlayer = null
        }

        // Reset alarm channel handler: delete and recreate app alarm channels using bundled tone
        val btnResetChannel = dialogView.findViewById<android.widget.Button>(R.id.btn_reset_channel)
        btnResetChannel.setOnClickListener {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val defaultChannelId = getString(R.string.default_notification_channel_id)
                val securityChannelId = "security_alerts"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try { notificationManager.deleteNotificationChannel(defaultChannelId) } catch (_: Exception) {}
                    try { notificationManager.deleteNotificationChannel(securityChannelId) } catch (_: Exception) {}
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Default channel: silent, low importance
                    val defaultChannel = android.app.NotificationChannel(defaultChannelId, "App Notifications", android.app.NotificationManager.IMPORTANCE_LOW).apply {
                        description = "General app notifications (silent)"
                        setSound(null, null)
                        enableVibration(false)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                    }
                    notificationManager.createNotificationChannel(defaultChannel)

                    // Security channel: audible, high importance
                    val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}")
                    val audioAttrs = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()

                    val secChannel = android.app.NotificationChannel(securityChannelId, "Security Alerts", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notifications for security breaches"
                        setSound(soundUri, audioAttrs)
                        enableLights(true)
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(secChannel)
                }

                android.widget.Toast.makeText(this, "Alarm channels reset (default silent, security audible)", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("ScadaActivity", "Failed to reset alarm channels", e)
                android.widget.Toast.makeText(this, "Failed to reset channels", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()

        // Pre-select the radio group based on stored app pref
        try {
            val backendPref = appPrefs.getString("lights_control_backend", "firebase") ?: "firebase"
            val rg = dialogView.findViewById<android.widget.RadioGroup?>(R.id.rg_lights_control)
            when (backendPref) {
                "drive" -> rg?.check(R.id.rb_control_drive)
                else -> rg?.check(R.id.rb_control_firebase)
            }
        } catch (_: Exception) {}

        // Save backend explicit button: persist selected backend and apply immediately
        try {
            val btnSaveBackend = dialogView.findViewById<android.widget.Button>(R.id.btn_save_backend)
            val btnReloadBackend = dialogView.findViewById<android.widget.Button>(R.id.btn_reload_backend)
            btnSaveBackend.setOnClickListener {
                try {
                    val rg = dialogView.findViewById<android.widget.RadioGroup?>(R.id.rg_lights_control)
                    val selected = when (rg?.checkedRadioButtonId) {
                        R.id.rb_control_drive -> "drive"
                        else -> "firebase"
                    }
                    appPrefs.edit { putString("lights_control_backend", selected) }
                    android.widget.Toast.makeText(this, getString(R.string.lights_backend_saved, selected), android.widget.Toast.LENGTH_SHORT).show()
                    applyLightsBackendSelection()
                } catch (e: Exception) {
                    Log.w("ScadaActivity", "Failed saving backend", e)
                    android.widget.Toast.makeText(this, "Failed to save backend", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            btnReloadBackend.setOnClickListener {
                try {
                    android.widget.Toast.makeText(this, "Reloading backend selection...", android.widget.Toast.LENGTH_SHORT).show()
                    applyLightsBackendSelection()
                } catch (e: Exception) {
                    Log.w("ScadaActivity", "Failed reloading backend", e)
                }
            }
        } catch (_: Exception) {}
    }

    // Ensure alarm status text/dot and lights backend selection helpers exist.
    private fun updateAlarmStatus() {
        try {
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val highAmpsThreshold = prefs.getFloat("high_amps_threshold", 6.0f).toDouble()
            val lowPressureThreshold = prefs.getFloat("low_pressure_threshold", 1.0f).toDouble()

            // Extract numeric parts from displayed text (e.g. "6.2 A" or "1.2 bar")
            val ampsText = try { ampsTextView.text.toString() } catch (_: Exception) { "" }
            val pressureText = try { geyserPressureTextView.text.toString() } catch (_: Exception) { "" }
            val ampsVal = Regex("([-+]?[0-9]*\\.?[0-9]+)").find(ampsText)?.value?.toDoubleOrNull()
            val pressureVal = Regex("([-+]?[0-9]*\\.?[0-9]+)").find(pressureText)?.value?.toDoubleOrNull()

            var alarmActive = false
            if (ampsVal != null && ampsVal > highAmpsThreshold) alarmActive = true
            if (pressureVal != null && pressureVal < lowPressureThreshold) alarmActive = true

            runOnUiThread {
                try {
                    if (alarmActive) {
                        try { alarmActiveStatus.text = getString(R.string.alarm_active) } catch (_: Exception) { alarmActiveStatus.text = "ALARM" }
                        val red = "#B71C1C".toColorInt()
                        alarmActiveStatus.setTextColor(red)
                        alarmStatusDot.imageTintList = ColorStateList.valueOf(red)
                        Log.d("ScadaActivity", "Alarm active (amps=$ampsVal, pressure=$pressureVal)")
                        // Attempt to trigger the NotificationService test action (safe no-op if service missing)
                        try {
                            val intent = Intent(this, NotificationService::class.java)
                            intent.action = "com.security.app.ACTION_TEST_ALARMS"
                            startService(intent)
                        } catch (e: Exception) {
                            Log.w("ScadaActivity", "Failed to start NotificationService", e)
                        }
                    } else {
                        try { alarmActiveStatus.text = getString(R.string.alarm_inactive) } catch (_: Exception) { alarmActiveStatus.text = "OK" }
                        val green = "#4CAF50".toColorInt()
                        alarmActiveStatus.setTextColor(green)
                        alarmStatusDot.imageTintList = ColorStateList.valueOf(green)
                    }
                } catch (e: Exception) {
                    Log.w("ScadaActivity", "UI update in updateAlarmStatus failed", e)
                }
            }
        } catch (e: Exception) {
            Log.w("ScadaActivity", "updateAlarmStatus failed", e)
        }
    }

    private fun applyLightsBackendSelection() {
        try {
            val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val backend = appPrefs.getString("lights_control_backend", "firebase") ?: "firebase"
            Log.d("ScadaActivity", "applyLightsBackendSelection backend=$backend")
            if (backend == "drive") {
                try {
                    // If the Drive service is already initialized, avoid triggering sign-in again
                    // which would cause initializeDriveService() -> applyLightsBackendSelection() recursion.
                    if (driveService != null) {
                        startLightsPolling()
                    } else {
                        // Drive not yet initialized: ensure we have a signed-in account which will
                        // initialize Drive (initializeDriveService) once available.
                        try { ensureFreshGoogleSignIn() } catch (e: Exception) { Log.w("ScadaActivity", "ensureFreshGoogleSignIn failed", e) }
                    }
                } catch (e: Exception) {
                    Log.w("ScadaActivity", "startLightsPolling failed", e)
                }
            } else {
                try { drivePollingJob?.cancel() } catch (e: Exception) { Log.w("ScadaActivity", "cancel drivePollingJob", e) }
                try { fetchLightsStatusOnce() } catch (e: Exception) { Log.w("ScadaActivity", "fetchLightsStatusOnce failed", e) }
            }
        } catch (e: Exception) {
            Log.w("ScadaActivity", "applyLightsBackendSelection failed", e)
        }
    }

    private fun updatePresenceUI(triggeredByUser: Boolean) {
        // Run in background to avoid blocking UI
        CoroutineScope(Dispatchers.IO).launch {
            val gateway = getGatewayIp()
            val atHome = gateway == "192.168.8.1"
            val color: Int
            val text: String
            if (atHome) {
                color = "#4CAF50".toColorInt()
                text = "At home"
            } else {
                color = "#B0BEC5".toColorInt()
                text = "Away"
            }
            try {
                withContext(Dispatchers.Main) {
                    try {
                        setupPresenceText.text = text
                        setupPresenceDot.imageTintList = ColorStateList.valueOf(color)
                        if (triggeredByUser) {
                            val gwText = gateway ?: "unknown"
                            val msg = "Gateway: $gwText — $text"
                            try { android.widget.Toast.makeText(this@ScadaActivity, msg, android.widget.Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                        } else {
                            // no-op else branch to ensure `if` is not treated as an expression missing an else
                        }
                    } catch (e: Exception) {
                        Log.w("ScadaActivity", "Failed to update presence UI", e)
                    }
                }
            } catch (e: Exception) {
                Log.w("ScadaActivity", "updatePresenceUI outer failure", e)
            }
        }
    }

    // Obtain gateway IP using ConnectivityManager + LinkProperties (safe alternative to dhcpInfo)
    private fun getGatewayIp(): String? {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active: Network? = cm.activeNetwork
            if (active == null) return null
            val lp: LinkProperties? = cm.getLinkProperties(active)
            if (lp == null) return null
            val routes = lp.routes
            for (route in routes) {
                try {
                    val gw = route.gateway
                    if (gw != null) {
                        val host = gw.hostAddress
                        if (!host.isNullOrEmpty() && host != "0.0.0.0") return host
                    }
                } catch (_: Exception) {}
            }
            return null
        } catch (e: Exception) {
            Log.w("ScadaActivity", "getGatewayIp failed", e)
            return null
        }
    }

    // --- New helpers: Sun position + Tide fetch ---
    private fun updateSunAndTides() {
        // Launch coroutine to compute sun position and fetch sunrise/sunset + tides
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1) fetch sunrise/sunset from Open-Meteo daily endpoint
                val sunPair = withContext(Dispatchers.IO) { fetchSunriseSunset(SWAK_LAT.toDouble(), SWAK_LON.toDouble()) }
                val sunriseStr = sunPair?.first ?: "--:--"
                val sunsetStr = sunPair?.second ?: "--:--"

                // 2) compute current sun elevation/azimuth locally
                val (altDeg, azDeg) = computeSunPosition(SWAK_LAT.toDouble(), SWAK_LON.toDouble(), System.currentTimeMillis())
                val altFmt = String.format(Locale.getDefault(), "%.0f°", altDeg)
                val azFmt = String.format(Locale.getDefault(), "%.0f°", azDeg)

                // 3) update sun UI (show only times: sunrise / sunset)
                try {
                    if (::sunInfoTextView.isInitialized) sunInfoTextView.text = "${sunriseStr} / ${sunsetStr}"
                } catch (_: Exception) {}

                // 4) fetch tide (hourly water level) and compute next high/low
                val tideText = withContext(Dispatchers.IO) { fetchNextTideInfo(SWAK_LAT.toDouble(), SWAK_LON.toDouble()) } ?: "Tide: N/A"
                try { if (::tideInfoTextView.isInitialized) tideInfoTextView.text = tideText } catch (_: Exception) {}

            } catch (e: Exception) {
                Log.w("ScadaActivity", "updateSunAndTides error", e)
                try { if (::sunInfoTextView.isInitialized) sunInfoTextView.text = "--:-- / --:--" } catch (_: Exception) {}
                try { if (::tideInfoTextView.isInitialized) tideInfoTextView.text = "Tide: N/A" } catch (_: Exception) {}
            }
        }
    }

    // Fetch sunrise/sunset (local time) using Open-Meteo daily API
    private fun fetchSunriseSunset(lat: Double, lon: Double): Pair<String, String>? {
        return try {
            val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&daily=sunrise,sunset&timezone=auto"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.connect()
            val code = conn.responseCode
            if (code != 200) return null
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) { sb.append(line); line = reader.readLine() }
            reader.close()
            val jo = JSONObject(sb.toString())
            val daily = jo.optJSONObject("daily") ?: return null
            val sunriseArr = daily.optJSONArray("sunrise") ?: return null
            val sunsetArr = daily.optJSONArray("sunset") ?: return null
            val sunrise = sunriseArr.optString(0, "")
            val sunset = sunsetArr.optString(0, "")
            // sunrise/sunset are returned in ISO format with local date/time; extract time portion
            val sTime = try { sunrise.split("T").getOrNull(1)?.split(":")?.let { parts -> "${parts[0]}:${parts[1]}" } ?: sunrise } catch (_: Exception) { sunrise }
            val ssTime = try { sunset.split("T").getOrNull(1)?.split(":")?.let { parts -> "${parts[0]}:${parts[1]}" } ?: sunset } catch (_: Exception) { sunset }
            Pair(sTime, ssTime)
        } catch (e: Exception) {
            Log.w("ScadaActivity", "fetchSunriseSunset failed", e)
            null
        }
    }

    // Fetch hourly water level from Open-Meteo Marine API and compute next high/low tide
    // Returns a short human-readable string like "High 14:20 (1.2m)"
    private fun fetchNextTideInfo(lat: Double, lon: Double): String? {
        return try {
            val sg = fetchTideFromStormglass(lat, lon)
            if (!sg.isNullOrEmpty()) {
                Log.d("ScadaActivity", "fetchNextTideInfo: using Stormglass result")
                sg
            } else {
                Log.w("ScadaActivity", "fetchNextTideInfo: Stormglass returned no data")
                null
            }
        } catch (e: Exception) {
            Log.w("ScadaActivity", "fetchNextTideInfo failed", e)
            null
        }
    }

    // Try Stormglass v2 extremes API as primary tide source.
    // Stormglass v2 extremes endpoint (common pattern): /v2/tide/extremes?lat=...&lng=...&start=...&end=...
    private fun fetchTideFromStormglass(lat: Double, lon: Double): String? {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            var apiKey = prefs.getString("stormglass_api_key", null)
            if (apiKey.isNullOrEmpty()) {
                // Prefer the embedded key when no preference is set
                apiKey = EMBEDDED_STORMGLASS_API_KEY.takeIf { it.isNotEmpty() }
                if (apiKey.isNullOrEmpty()) {
                    Log.d("ScadaActivity", "fetchTideFromStormglass: no API key configured (stormglass_api_key) and no embedded key")
                    return null
                } else {
                    Log.d("ScadaActivity", "fetchTideFromStormglass: using embedded Stormglass API key")
                }
            }

            // Compute UTC start/end (use date strings yyyy-MM-dd because the /v2/tide/extremes/point
            // endpoint accepts human-readable dates and the plain key header form works with it).
            val now = System.currentTimeMillis()
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdfDate.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = sdfDate.format(java.util.Date(now))
            val endDate = sdfDate.format(java.util.Date(now + 48 * 3600L * 1000L))

            // Use the /v2/tide/extremes/point path (works per curl sample). Stormglass accepts
            // Authorization: <key> (no 'Bearer') for many accounts; the code will still try 'Bearer' if needed.
            val urlStr = "https://api.stormglass.io/v2/tide/extremes/point?lat=${lat}&lng=${lon}&start=${startDate}&end=${endDate}"
            Log.d("ScadaActivity", "fetchTideFromStormglass url=$urlStr")

            // Try with Authorization: {key} first, then 'Bearer {key}' if 401
            var lastErrBody = ""
            for (attempt in 1..2) {
                 val url = URL(urlStr)
                 val conn = url.openConnection() as HttpURLConnection
                 conn.requestMethod = "GET"
                 conn.connectTimeout = 10000
                 conn.readTimeout = 10000
                 // Header formats
                 val headerVal = if (attempt == 1) apiKey else "Bearer $apiKey"
                 conn.setRequestProperty("Authorization", headerVal)
                 conn.setRequestProperty("Accept", "application/json")
                 try { conn.connect() } catch (e: Exception) { Log.w("ScadaActivity", "Stormglass connect failed (attempt $attempt)", e); continue }
                val code = try { conn.responseCode } catch (_: Exception) { -1 }
                val body = try {
                    if (code == 200) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    Log.w("ScadaActivity", "Failed reading Stormglass response body", e)
                    ""
                }
                Log.d("ScadaActivity", "fetchTideFromStormglass HTTP $code (attempt $attempt); body.len=${body.length}")
                if (code != 200) {
                    lastErrBody = body
                    // If unauthorized on first attempt, try Bearer form once more
                    if (code == 401 && attempt == 1) { continue }
                    // Return a readable error so the UI can show it
                    try {
                        val errJo = if (body.isNotEmpty()) JSONObject(body) else null
                        if (errJo != null && errJo.has("errors")) {
                            val errObj = errJo.opt("errors")
                            return "Stormglass error HTTP $code: ${errObj?.toString() ?: errObj}"
                        }
                    } catch (_: Exception) {}
                    return "Stormglass HTTP $code: ${body.take(512)}"
                }
                Log.d("ScadaActivity", "fetchTideFromStormglass response len=${body.length}")
                 // Parse flexible JSON: many Stormglass responses include 'data' array
                 val jo = JSONObject(body)
                // If the response contains an errors object, return it so the UI shows the reason
                if (jo.has("errors")) {
                    try { return "Stormglass error: ${jo.opt("errors").toString()}" } catch (_: Exception) { return "Stormglass error: ${jo.opt("errors")}" }
                }
                 // If top-level 'data' is an array of extremes use it
                 val candidateArr = when {
                     jo.has("data") && jo.opt("data") is org.json.JSONArray -> jo.optJSONArray("data")
                     jo.has("extremes") && jo.opt("extremes") is org.json.JSONArray -> jo.optJSONArray("extremes")
                     jo.has("hours") && jo.opt("hours") is org.json.JSONArray -> jo.optJSONArray("hours")
                     else -> null
                 }
                 if (candidateArr != null) {
                    // If the array is empty, return meta information so the UI can show why no data was returned
                    if (candidateArr.length() == 0) {
                        try {
                            val meta = jo.optJSONObject("meta")
                            if (meta != null) {
                                val station = meta.optJSONObject("station")?.optString("name") ?: meta.optString("station", "(no-station)")
                                val dist = meta.optJSONObject("station")?.optInt("distance") ?: meta.optInt("distance", -1)
                                val quota = meta.optInt("dailyQuota", -1)
                                val cost = meta.optInt("cost", -1)
                                return "Stormglass: no tide data for dates $startDate..$endDate (station=${station}, distance=${dist}km, dailyQuota=${quota}, cost=${cost})"
                            }
                        } catch (_: Exception) {}
                        return "Stormglass: no tide data returned for dates $startDate..$endDate"
                    }
                    // Stormglass typically returns objects with keys like 'time' or 't' and 'height'
                    return parseHeightsArrayToHighLow(candidateArr)
                }

                // Some responses use an object with a 'data' object keyed by source — try to find first array of objects
                val joKeysIt = jo.keys()
                while (joKeysIt.hasNext()) {
                    val k = joKeysIt.next()
                    val opt = jo.opt(k)
                    if (opt is org.json.JSONArray && opt.length() > 0 && opt.optJSONObject(0) != null) {
                        Log.d("ScadaActivity", "fetchTideFromStormglass: using array key=$k as candidate")
                        return parseHeightsArrayToHighLow(opt as org.json.JSONArray)
                    }
                }
                // Nothing found — return the raw response keys for debugging
                Log.w("ScadaActivity", "fetchTideFromStormglass: no usable array found in response; lastErr=${lastErrBody.take(512)}")
                val keysList = mutableListOf<String>()
                val it = jo.keys()
                while (it.hasNext()) keysList.add(it.next())
                return "Stormglass: no usable tide array found. Response keys=${keysList.joinToString(",")}"
             }
             return null
         } catch (e: Exception) {
             Log.w("ScadaActivity", "fetchTideFromStormglass failed", e)
             return null
         }
     }

    // Show a simple prompt to view/edit the Stormglass API key (embedded key used if prefs empty)
    private fun showStormglassKeyDialog() {
         try {
            Log.d("ScadaActivity", "showStormglassKeyDialog called")
             val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
             val currentStorm = prefs.getString("stormglass_api_key", "") ?: ""
             val layout = android.widget.LinearLayout(this)
             layout.orientation = android.widget.LinearLayout.VERTICAL
             val stormInput = android.widget.EditText(this)
             stormInput.hint = "Stormglass API key"
             stormInput.setText(currentStorm)
             val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
             lp.setMargins(8,8,8,8)
             layout.addView(stormInput, lp)

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Stormglass API Key")
                .setMessage("Enter your Stormglass API key. If left blank, the embedded key from the repo will be used.")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val storm = stormInput.text.toString().trim()
                    prefs.edit { putString("stormglass_api_key", storm) }
                    android.widget.Toast.makeText(this, "Stormglass key saved", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.w("ScadaActivity", "showStormglassKeyDialog failed", e)
        }
    }

    // Fallback: query WorldTides for heights (kept as flexible parser)
    private fun fetchTideFromWorldTides(lat: Double, lon: Double): String? {
        // WorldTides support removed — Stormglass is the sole tide provider.
        // If WorldTides is needed later, re-add a dedicated fetch function here.
        return null
    }

    // Parse a generic 'heights' JSONArray (flexible key names) and return a "High HH:MM (1.2m)\nLow ..." string
    private fun parseHeightsArrayToHighLow(arr: JSONArray): String? {
        try {
            val data = mutableListOf<Pair<Long, Double>>()
            for (i in 0 until arr.length()) {
                val el = arr.optJSONObject(i) ?: continue
                var tMillis: Long? = null
                if (el.has("dt")) {
                    val dtVal = el.optLong("dt", 0L)
                    if (dtVal > 1000000000L) tMillis = dtVal * 1000L
                }
                if (tMillis == null && el.has("timestamp")) {
                    val ts = el.optLong("timestamp", 0L)
                    if (ts > 1000000000L) tMillis = ts * 1000L
                }
                if (tMillis == null && el.has("date")) {
                    val ds = el.optString("date", "")
                    tMillis = parseIsoDateToMillis(ds)
                }
                if (tMillis == null && el.has("time")) {
                    val ds = el.optString("time", "")
                    tMillis = parseIsoDateToMillis(ds)
                }
                var h: Double = Double.NaN
                if (el.has("height")) h = el.optDouble("height", Double.NaN)
                if (h.isNaN() && el.has("h")) h = el.optDouble("h", Double.NaN)
                if (h.isNaN() && el.has("height_m")) h = el.optDouble("height_m", Double.NaN)
                if (h.isNaN() && el.has("value")) h = el.optDouble("value", Double.NaN)
                if (tMillis != null && !h.isNaN()) data.add(Pair(tMillis, h))
            }
            if (data.isEmpty()) return null
            data.sortBy { it.first }
            val now = System.currentTimeMillis()
            val idxNow = data.indexOfFirst { it.first > now }
            val searchStart = if (idxNow >= 0) idxNow else 0
            var nextHighIdx: Int? = null
            var nextLowIdx: Int? = null
            for (i in searchStart until data.size - 1) {
                val prev = data.getOrNull(i - 1)?.second ?: continue
                val cur = data[i].second
                val next = data.getOrNull(i + 1)?.second ?: continue
                if (cur > prev && cur > next && nextHighIdx == null) nextHighIdx = i
                if (cur < prev && cur < next && nextLowIdx == null) nextLowIdx = i
                if (nextHighIdx != null && nextLowIdx != null) break
            }
            val sb = StringBuilder()
            if (nextHighIdx != null) {
                val (t, lvl) = data[nextHighIdx]
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = t
                val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val mm = cal.get(java.util.Calendar.MINUTE)
                sb.append("High ${String.format(Locale.getDefault(), "%02d:%02d", hh, mm)} (${String.format(Locale.getDefault(), "%.2fm", lvl)})")
            }
            if (nextLowIdx != null) {
                if (sb.isNotEmpty()) sb.append("\n")
                val (t, lvl) = data[nextLowIdx]
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = t
                val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val mm = cal.get(java.util.Calendar.MINUTE)
                sb.append("Low ${String.format(Locale.getDefault(), "%02d:%02d", hh, mm)} (${String.format(Locale.getDefault(), "%.2fm", lvl)})")
            }
            return if (sb.isEmpty()) null else sb.toString()
        } catch (e: Exception) {
            Log.w("ScadaActivity", "parseHeightsArrayToHighLow failed", e)
            return null
        }
    }

    // Small helper to parse common ISO date/time strings to millis (UTC-aware)
    private fun parseIsoDateToMillis(s: String): Long? {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return null
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mmX", "yyyy-MM-dd HH:mm:ss")
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val d = sdf.parse(trimmed)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        try {
            val n = trimmed.toLongOrNull()
            if (n != null && n > 1000000000L) return if (trimmed.length == 10) n * 1000L else n
        } catch (_: Exception) {}
        return null
    }

    // Compute approximate sun position (altitude, azimuth in degrees) using simple astronomy formulas
    private fun computeSunPosition(lat: Double, lon: Double, timeMillis: Long): Pair<Double, Double> {
        try {
            val jd = julianDate(timeMillis)
            val n = jd - 2451545.0
            val L = (280.460 + 0.9856474 * n) % 360.0
            val g = Math.toRadians((357.528 + 0.9856003 * n) % 360.0)
            val lambda = Math.toRadians((L + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
            val epsilon = Math.toRadians(23.439 - 0.0000004 * n)
            val sinDec = sin(epsilon) * sin(lambda)
            val dec = asin(sinDec)
            val ra = atan2(cos(epsilon) * sin(lambda), cos(lambda))
            val gmst = (280.46061837 + 360.98564736629 * (jd - 2451545.0)) % 360.0
            val lst = Math.toRadians((gmst + lon) % 360.0)
            val hourAngle = lst - ra
            val latRad = Math.toRadians(lat)
            val altitude = asin(sin(latRad) * sin(dec) + cos(latRad) * cos(dec) * cos(hourAngle))
            val azimuth = atan2(-sin(hourAngle), tan(dec) * cos(latRad) - sin(latRad) * cos(hourAngle))
            val altDeg = Math.toDegrees(altitude)
            val azDeg = (Math.toDegrees(azimuth) + 360.0) % 360.0
            return Pair(altDeg, azDeg)
        } catch (e: Exception) {
            Log.w("ScadaActivity", "computeSunPosition failed", e)
            return Pair(0.0, 0.0)
        }
    }

    private fun julianDate(timeMillis: Long): Double {
        val dt = timeMillis.toDouble() / 1000.0
        val jd = dt / 86400.0 + 2440587.5
        return jd
    }

}

