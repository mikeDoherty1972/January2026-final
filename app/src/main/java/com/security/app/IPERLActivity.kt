package com.security.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class IPERLActivity : BaseActivity() {
    private lateinit var iperlGoogleSheetsReader: IPERLGoogleSheetsReader
    private lateinit var mikeWaterReading: TextView
    private lateinit var mikeWaterStatus: TextView
    private lateinit var mikeRSSI: TextView
    private lateinit var mikeSignalStatus: TextView
    private lateinit var dayUsage: TextView
    private lateinit var monthlyUsage: TextView
    private lateinit var totalUsage: TextView
    private lateinit var lastUpdate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iperl)

        // Initialize Google Sheets reader (used for RSSI + usage stats)
        iperlGoogleSheetsReader = IPERLGoogleSheetsReader()

        // SwipeRefresh for manual reload of IPERL sheets data
        val swipe = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.iperlSwipeRefresh)
        swipe.setOnRefreshListener {
            // Force refresh water meter CSV and IPERL stats and signals
            GraphDataRepository.refreshWaterMeterData(forceRefresh = true)
            // Also refresh iperl usage/stats by re-calling preload segment (small cost)
            GraphDataRepository.preLoadAllGraphData()
            // We'll turn off spinner when repository emits updated data in observers
        }

        // Initialize views
        mikeWaterReading = findViewById(R.id.mikeWaterReading)
        mikeWaterStatus = findViewById(R.id.mikeWaterStatus)
        mikeRSSI = findViewById(R.id.mikeRSSI)
        mikeSignalStatus = findViewById(R.id.mikeSignalStatus)
        dayUsage = findViewById(R.id.dayUsage)
        monthlyUsage = findViewById(R.id.monthlyUsage)
        totalUsage = findViewById(R.id.totalUsage)
        lastUpdate = findViewById(R.id.lastUpdate)

        // Setup back button
        findViewById<TextView>(R.id.iperlBackButton).setOnClickListener {
            finish()
        }

        // Long-press on the main IPERL area to change appearance (card color or graph background)
        findViewById<androidx.cardview.widget.CardView>(R.id.mikeWaterSection).setOnLongClickListener {
            val options = arrayOf("Card background color", "Graph background color")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("IPERL appearance")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> showColorPicker("card_iperl_color", "Choose IPERL card color") {
                        try { ToastHelper.show(this@IPERLActivity, "IPERL card color saved. Return to dashboard to see change.", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                    }
                    1 -> showColorPicker("graph_background_color", "Choose IPERL graph background") {
                        try { ToastHelper.show(this@IPERLActivity, "IPERL graph background saved.", android.widget.Toast.LENGTH_SHORT) } catch (_: Exception) {}
                    }
                }
            }
            builder.show()
            true
        }

        // Wire Mike's Card to open the Mike water graphs
        findViewById<androidx.cardview.widget.CardView>(R.id.mikeWaterSection).setOnClickListener {
            val intent = Intent(this, MikeWaterGraphsActivity::class.java)
            startActivity(intent)
        }

        // Observe preloaded water meter data from GraphDataRepository for fast initial load
        observeWaterMeterRepository()

        // Observe preloaded IPERL usage stats and signal readings from GraphDataRepository
        observeIperlUsageAndSignal()

        // Kick off repository pre-load (if not already running) — safe to call multiple times
        GraphDataRepository.preLoadAllGraphData()
    }

    private fun observeWaterMeterRepository() {
        lifecycleScope.launch {
            GraphDataRepository.waterMeterGraphData.collect { readings ->
                // Stop pull-to-refresh spinner if active
                try { findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.iperlSwipeRefresh).isRefreshing = false } catch (_: Exception) {}
                if (readings.isNotEmpty()) {
                    // Show most recent reading for mike water meter display only
                    val latest = readings.last()
                    val meterVal = latest.meterReading
                    mikeWaterReading.text = getString(R.string.mike_water_liters_format, meterVal.toInt())
                    mikeWaterStatus.text = getString(R.string.status_active)
                    mikeWaterStatus.setTextColor(getColor(android.R.color.holo_green_light))

                    // Note: totalUsage and lastUpdate are handled by iperlUsageStats from stats sheet
                } else {
                    mikeWaterReading.text = getString(R.string.no_data_label)
                    mikeWaterStatus.text = getString(R.string.no_data_label)
                    mikeWaterStatus.setTextColor(getColor(android.R.color.holo_orange_light))

                    // Only clear these if no stats data is available
                    if (totalUsage.text == "-- L") {
                        totalUsage.text = "-- L"
                        lastUpdate.text = getString(R.string.last_update_format, "--")
                    }
                }
            }
        }
    }

    private fun observeIperlUsageAndSignal() {
        // Observe usage stats
        lifecycleScope.launch {
            GraphDataRepository.iperlUsageStats.collect { usage ->
                // Stop spinner when usage stats arrive
                try { findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.iperlSwipeRefresh).isRefreshing = false } catch (_: Exception) {}
                if (usage != null) {
                    totalUsage.text = String.format(Locale.getDefault(), "%.3f L", usage.totalUsage.toFloat())
                    dayUsage.text = String.format(Locale.getDefault(), "%.1f L", usage.dayUsage)
                    monthlyUsage.text = String.format(Locale.getDefault(), "%.1f L", usage.monthUsage)
                    lastUpdate.text = getString(R.string.last_update_format, usage.lastUpdate)
                }
            }
        }

        // Observe signal readings
        lifecycleScope.launch {
            GraphDataRepository.iperlSignalReadings.collect { signals ->
                if (signals.isNotEmpty()) {
                    val latestSignal = signals.last()
                    val rssiValue = latestSignal.rssi
                    mikeRSSI.text = String.format(Locale.getDefault(), "%d dBm", rssiValue.toInt())
                    mikeSignalStatus.text = when {
                        rssiValue > -50 -> "Excellent"
                        rssiValue > -70 -> "Good"
                        rssiValue > -85 -> "Fair"
                        else -> "Poor"
                    }
                    mikeSignalStatus.setTextColor(
                        when {
                            rssiValue > -70 -> getColor(android.R.color.holo_green_light)
                            rssiValue > -85 -> getColor(android.R.color.holo_orange_light)
                            else -> getColor(android.R.color.holo_red_light)
                        }
                    )
                } else {
                    // keep existing state if empty; repository may populate later
                }
            }
        }
    }
}