package com.security.app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*

class GeyserGraphsActivity : BaseActivity() {
    
    private lateinit var geyserTempChart: LineChart
    private lateinit var pressureChart: LineChart
    private lateinit var dateTitle: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geyser_graphs)
        
        // Initialize views
        geyserTempChart = findViewById(R.id.geyserTempChart)
        pressureChart = findViewById(R.id.pressureChart)
        dateTitle = findViewById(R.id.dateTitle)
        
        // Setup return button
        findViewById<TextView>(R.id.returnButton).setOnClickListener {
            finish()
        }
        
        // Set today's date
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateString = getString(R.string.geyser_performance_date, today)
        dateTitle.text = dateString

        // Setup charts
        setupCharts()
        
        // Setup swipe to refresh
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                GraphDataRepository.refreshGeyserData()
                swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observe data from the repository
        lifecycleScope.launch {
            GraphDataRepository.geyserGraphData.collect { readings ->
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
        listOf(geyserTempChart, pressureChart).forEach { chart ->
            chart.description.isEnabled = false
            chart.setTouchEnabled(true)
            chart.isDragEnabled = true
            chart.setScaleEnabled(true)
            chart.setPinchZoom(true)

            // Apply configurable graph background color (falls back to transparent)
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val graphBg = prefs.getString("graph_background_color", null)
            val bgColor = try {
                if (!graphBg.isNullOrBlank()) graphBg.toColorInt() else Color.TRANSPARENT
            } catch (_: IllegalArgumentException) {
                Color.TRANSPARENT
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
            xAxis.granularity = 60f
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
            val tempEntries = mutableListOf<Entry>()
            val pressureEntries = mutableListOf<Entry>()

            val orderedReadings = readings.reversed()

            orderedReadings.forEach { reading ->
                val minutes = parseTimeToMinutes(reading.timestamp)
                tempEntries.add(Entry(minutes.toFloat(), reading.waterTemp))
                pressureEntries.add(Entry(minutes.toFloat(), reading.waterPressure))
            }

            updateGeyserCharts(tempEntries, pressureEntries)
            dateTitle.text = getString(R.string.geyser_performance_count, readings.size)
        } else {
            showEmptyCharts()
            dateTitle.text = getString(R.string.geyser_performance_no_data)
        }
    }
    
    private fun updateGeyserCharts(tempEntries: List<Entry>, pressureEntries: List<Entry>) {
         // Temperature Chart
         val tempDataSet = LineDataSet(tempEntries, "Water Temperature °C")
         tempDataSet.color = Color.RED
         tempDataSet.setCircleColor(tempDataSet.color)
         tempDataSet.lineWidth = 3f
         tempDataSet.circleRadius = 1f
         tempDataSet.setDrawCircleHole(false)
         tempDataSet.valueTextSize = 8f
         tempDataSet.valueTextColor = Color.WHITE

         geyserTempChart.data = LineData(tempDataSet)

         // Constrain Y axis for geyser temperature to 0..70 °C and set granularity
         val tempLeftAxis = geyserTempChart.axisLeft
         tempLeftAxis.setAxisMinimum(0f)
         tempLeftAxis.setAxisMaximum(70f)
         tempLeftAxis.granularity = 5f
         tempLeftAxis.setDrawGridLines(true)
         tempLeftAxis.textColor = Color.WHITE
         tempLeftAxis.gridColor = Color.GRAY
         // Disable auto-scaling so manual axis limits are not overwritten
         geyserTempChart.setAutoScaleMinMaxEnabled(false)

         // Log axis settings for debugging (verify at runtime via Logcat)
         android.util.Log.d("GeyserGraphs", "Applied Y axis limits: min=${tempLeftAxis.axisMinimum}, max=${tempLeftAxis.axisMaximum}, granularity=${tempLeftAxis.granularity}")

         val timeFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }
        geyserTempChart.xAxis.valueFormatter = timeFormatter
        geyserTempChart.xAxis.axisMinimum = 0f
        geyserTempChart.xAxis.axisMaximum = (24 * 60).toFloat()
        // Refresh and ensure axis settings are applied (don't call fitScreen which can reset axes)
        geyserTempChart.data?.notifyDataChanged()
        geyserTempChart.notifyDataSetChanged()
        // Ensure tick granularity is enforced
        tempLeftAxis.isGranularityEnabled = true
        // Force viewport Y-range so 0..70 is visible (AxisDependency.LEFT)
        geyserTempChart.setVisibleYRangeMaximum(70f, com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)
        geyserTempChart.moveViewTo(0f, 0f, com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)
        geyserTempChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        geyserTempChart.moveViewToX(0f)
        geyserTempChart.invalidate()

        // Pressure Chart
        val pressureDataSet = LineDataSet(pressureEntries, "Water Pressure Bar")
        pressureDataSet.color = Color.BLUE
        pressureDataSet.setCircleColor(pressureDataSet.color)
        pressureDataSet.lineWidth = 3f
        pressureDataSet.circleRadius = 1f
        pressureDataSet.setDrawCircleHole(false)
        pressureDataSet.valueTextSize = 8f
        pressureDataSet.valueTextColor = Color.WHITE

        pressureChart.data = LineData(pressureDataSet)
        pressureChart.xAxis.valueFormatter = timeFormatter
        pressureChart.xAxis.axisMinimum = 0f
        pressureChart.xAxis.axisMaximum = (24 * 60).toFloat()
        // Set Y axis 0-5 bar, granularity 0.5, label count 11
        val pressureLeftAxis = pressureChart.axisLeft
        pressureLeftAxis.setAxisMinimum(0f)
        pressureLeftAxis.setAxisMaximum(5f)
        pressureLeftAxis.granularity = 0.5f
        pressureLeftAxis.setLabelCount(11, true)
        pressureLeftAxis.isGranularityEnabled = true
        pressureLeftAxis.setDrawGridLines(true)
        pressureLeftAxis.textColor = Color.WHITE
        pressureLeftAxis.gridColor = Color.GRAY
        pressureChart.setAutoScaleMinMaxEnabled(false)
        pressureChart.data?.notifyDataChanged()
        pressureChart.notifyDataSetChanged()
        pressureChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        pressureChart.moveViewToX(0f)
        pressureChart.invalidate()
     }

    private fun showEmptyCharts() {
        val emptyEntries = listOf(Entry(0f, 0f))
        updateGeyserCharts(emptyEntries, emptyEntries)
    }

    // Helper: parse a timestamp (expected like "YYYY-MM-DD HH:MM:SS" or "YYYY-MM-DD HH:MM") to minutes since midnight
    private fun parseTimeToMinutes(timestamp: String): Int {
        val timePart = timestamp.split(" ").getOrNull(1) ?: return 0
        val parts = timePart.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (hour * 60) + minute
    }

    override fun onResume() {
        super.onResume()
        // Re-apply Y-axis limits as a runtime safety net
        try {
            val leftAxis = geyserTempChart.axisLeft
            leftAxis.setAxisMinimum(0f)
            leftAxis.setAxisMaximum(70f)
            geyserTempChart.setAutoScaleMinMaxEnabled(false)
            leftAxis.isGranularityEnabled = true
            geyserTempChart.setVisibleYRangeMaximum(70f, com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)
            geyserTempChart.moveViewTo(0f, 0f, com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)
            geyserTempChart.invalidate()
            android.util.Log.d("GeyserGraphs", "onResume: reapplied Y axis limits")
        } catch (_: Exception) {
            // ignore if chart not yet initialized
        }
    }
 }
