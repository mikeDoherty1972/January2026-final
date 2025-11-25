package com.security.app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PowerGraphsActivity : BaseActivity() {
    
    private lateinit var currentPowerChart: LineChart
    private lateinit var currentAmpsChart: LineChart
    private lateinit var dailyTotalChart: LineChart
    private lateinit var dateTitle: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_power_graphs)
        
        // Initialize views
        currentPowerChart = findViewById(R.id.currentPowerChart)
        currentAmpsChart = findViewById(R.id.currentAmpsChart)
        dailyTotalChart = findViewById(R.id.dailyTotalChart)
        dateTitle = findViewById(R.id.dateTitle)
        
        // Setup return button
        findViewById<TextView>(R.id.returnButton).setOnClickListener {
            finish()
        }
        
        // Set today's date
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateString = getString(R.string.power_consumption_date, today)
        dateTitle.text = dateString

        // Setup charts
        setupCharts()
        
        // Setup swipe to refresh
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                GraphDataRepository.refreshPowerData()
                swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observe data from the repository
        lifecycleScope.launch {
            GraphDataRepository.powerGraphData.collect { readings ->
                if (readings.isNotEmpty()) {
                    dateTitle.text = dateString
                    updateCharts(readings)
                } else {
                    dateTitle.text = getString(R.string.loading_data)
                }
            }
        }
    }
    
    private fun setupCharts() {
        listOf(currentPowerChart, currentAmpsChart, dailyTotalChart).forEach { chart ->
            chart.description.isEnabled = false
            chart.setTouchEnabled(true)
            chart.isDragEnabled = true
            chart.setScaleEnabled(true)
            chart.setPinchZoom(true)

            // Apply configurable graph background color (falls back to transparent)
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val graphBg = prefs.getString("graph_background_color", null)
            val bgColor = try {
                if (graphBg != null) android.graphics.Color.parseColor(graphBg) else android.graphics.Color.TRANSPARENT
            } catch (e: IllegalArgumentException) {
                android.graphics.Color.TRANSPARENT
            }
            chart.setBackgroundColor(bgColor)

            val mv = CustomMarkerView(this, R.layout.custom_marker_view)
            chart.marker = mv
            
            // X-axis setup
            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(true)
            xAxis.textColor = Color.WHITE
            xAxis.gridColor = Color.GRAY
            xAxis.granularity = 60f // label every hour by default
            xAxis.labelRotationAngle = -45f

            // Y-axis setup
            val leftAxis = chart.axisLeft
            leftAxis.setDrawGridLines(true)
            leftAxis.textColor = Color.WHITE
            leftAxis.gridColor = Color.GRAY
            
            val rightAxis = chart.axisRight
            rightAxis.isEnabled = false
        }
    }

    private fun updateCharts(readings: List<GoogleSheetsReader.SensorReading>) {
        if (readings.isNotEmpty()) {
            val powerEntries = mutableListOf<Entry>()
            val ampsEntries = mutableListOf<Entry>()
            val dailyEntries = mutableListOf<Entry>()

            val orderedReadings = readings.reversed()

            orderedReadings.forEach { reading ->
                val minutes = parseTimeToMinutes(reading.timestamp).toFloat()
                powerEntries.add(Entry(minutes, reading.currentPower))
                ampsEntries.add(Entry(minutes, reading.currentAmps))
                dailyEntries.add(Entry(minutes, reading.dailyPower))
            }

            updatePowerCharts(powerEntries, ampsEntries, dailyEntries)
            dateTitle.text = getString(R.string.power_consumption_count, readings.size)
        } else {
            showEmptyCharts()
            dateTitle.text = getString(R.string.power_consumption_no_data)
        }
    }

    private fun updatePowerCharts(powerEntries: List<Entry>, ampsEntries: List<Entry>, dailyEntries: List<Entry>) {
        // Formatter to convert minutes-since-base to HH:mm (wraps every 24h)
        val timeFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }

        // Current Power Chart
        val powerDataSet = LineDataSet(powerEntries, "Power (kW)")
        powerDataSet.color = Color.YELLOW
        powerDataSet.setCircleColor(powerDataSet.color)
        powerDataSet.lineWidth = 3f
        powerDataSet.circleRadius = 1f
        powerDataSet.setDrawCircleHole(false)
        powerDataSet.valueTextSize = 8f
        powerDataSet.valueTextColor = Color.WHITE
        
        currentPowerChart.data = LineData(powerDataSet)
        currentPowerChart.xAxis.valueFormatter = timeFormatter
        currentPowerChart.xAxis.axisMinimum = 0f
        currentPowerChart.xAxis.axisMaximum = (24 * 60).toFloat()
        currentPowerChart.isAutoScaleMinMaxEnabled = false
        currentPowerChart.xAxis.setLabelCount(6, true)
        currentPowerChart.setVisibleXRangeMaximum((24*60).toFloat())
        currentPowerChart.moveViewToX(0f)
        currentPowerChart.invalidate()
        
        // Current Amps Chart
        val ampsDataSet = LineDataSet(ampsEntries, "Current (A)")
        ampsDataSet.color = Color.MAGENTA
        ampsDataSet.setCircleColor(ampsDataSet.color)
        ampsDataSet.lineWidth = 3f
        ampsDataSet.circleRadius = 1f
        ampsDataSet.setDrawCircleHole(false)
        ampsDataSet.valueTextSize = 8f
        ampsDataSet.valueTextColor = Color.WHITE
        
        currentAmpsChart.data = LineData(ampsDataSet)
        currentAmpsChart.xAxis.valueFormatter = timeFormatter
        currentAmpsChart.xAxis.axisMinimum = 0f
        currentAmpsChart.xAxis.axisMaximum = (24 * 60).toFloat()
        currentAmpsChart.isAutoScaleMinMaxEnabled = false
        currentAmpsChart.xAxis.setLabelCount(6, true)
        currentAmpsChart.setVisibleXRangeMaximum((24*60).toFloat())
        currentAmpsChart.moveViewToX(0f)
        currentAmpsChart.invalidate()
        
        // Daily Total Chart
        val dailyDataSet = LineDataSet(dailyEntries, "Daily Total (kWh)")
        dailyDataSet.color = Color.GREEN
        dailyDataSet.setCircleColor(dailyDataSet.color)
        dailyDataSet.lineWidth = 3f
        dailyDataSet.circleRadius = 1f
        dailyDataSet.setDrawCircleHole(false)
        dailyDataSet.valueTextSize = 8f
        dailyDataSet.valueTextColor = Color.WHITE
        
        dailyTotalChart.data = LineData(dailyDataSet)
        dailyTotalChart.xAxis.valueFormatter = timeFormatter
        dailyTotalChart.xAxis.axisMinimum = 0f
        dailyTotalChart.xAxis.axisMaximum = (24 * 60).toFloat()
        dailyTotalChart.isAutoScaleMinMaxEnabled = false
        dailyTotalChart.xAxis.setLabelCount(6, true)
        dailyTotalChart.setVisibleXRangeMaximum((24*60).toFloat())
        dailyTotalChart.moveViewToX(0f)
        dailyTotalChart.invalidate()
    }
    
    private fun showEmptyCharts() {
        val emptyEntries = listOf(Entry(0f, 0f))
        updatePowerCharts(emptyEntries, emptyEntries, emptyEntries)
    }

    // Helper: parse a timestamp (expected like "YYYY-MM-DD HH:MM:SS" or "YYYY-MM-DD HH:MM") to minutes since midnight
    private fun parseTimeToMinutes(timestamp: String): Int {
        val timePart = timestamp.split(" ").getOrNull(1) ?: return 0
        val parts = timePart.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (hour * 60) + minute
    }
}