package com.security.app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherGraphsActivity : BaseActivity() {
    
    private lateinit var temperatureChart: LineChart
    private lateinit var humidityChart: LineChart
    private lateinit var windChart: LineChart
    private lateinit var windDirectionChart: LineChart
    private lateinit var dateTitle: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_graphs)
        
        // Initialize views
        temperatureChart = findViewById(R.id.temperatureChart)
        humidityChart = findViewById(R.id.humidityChart)
        windChart = findViewById(R.id.windChart)
        windDirectionChart = findViewById(R.id.windDirectionChart)
        dateTitle = findViewById(R.id.dateTitle)
        
        // Setup return button
        findViewById<TextView>(R.id.returnButton).setOnClickListener {
            finish()
        }
        
        // Set today's date
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateString = getString(R.string.weather_trends_date, today)
        dateTitle.text = dateString

        // Setup charts
        setupCharts()
        
        // Setup swipe to refresh
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                GraphDataRepository.refreshWeatherData()
                swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observe data from the repository
        lifecycleScope.launch {
            GraphDataRepository.weatherGraphData.collect { readings ->
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
        listOf(temperatureChart, humidityChart, windChart, windDirectionChart).forEach { chart ->
            chart.description.isEnabled = false
            chart.setTouchEnabled(true)
            chart.isDragEnabled = true
            chart.setScaleEnabled(true)
            chart.setPinchZoom(true)

            // Apply configurable graph background color (falls back to transparent)
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val graphBg = prefs.getString("graph_background_color", null)
            val bgColor = try {
                if (graphBg != null) Color.parseColor(graphBg) else Color.TRANSPARENT
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
        // This function will be called when the data is loaded
        // It will update the charts with the new data
        if (readings.isNotEmpty()) {
            val indoorTempEntries = mutableListOf<Entry>()
            val outdoorTempEntries = mutableListOf<Entry>()
            val humidityEntries = mutableListOf<Entry>()
            val windEntries = mutableListOf<Entry>()
            val windDirEntries = mutableListOf<Entry>()

            val orderedReadings = readings.reversed()

            orderedReadings.forEach { reading ->
                val minutes = parseTimeToMinutes(reading.timestamp).toFloat()
                indoorTempEntries.add(Entry(minutes, reading.indoorTemp))
                outdoorTempEntries.add(Entry(minutes, reading.outdoorTemp))
                humidityEntries.add(Entry(minutes, reading.humidity))
                windEntries.add(Entry(minutes, reading.windSpeed))
                // Use the raw wind direction degrees for graph data (normalize to 0..360).
                // The visual "invert" toggle should only affect the dashboard arrow (display),
                // not the stored/graph data. This keeps graphs accurate and prevents double-flipping.
                val rawDir = reading.windDirection
                val normalizedDir = ((rawDir) % 360f + 360f) % 360f
                windDirEntries.add(Entry(minutes, normalizedDir))
            }

            updateTemperatureChart(indoorTempEntries, outdoorTempEntries)
            updateHumidityChart(humidityEntries)
            updateWindChart(windEntries)
            updateWindDirectionChart(windDirEntries)

            dateTitle.text = getString(R.string.weather_trends_count, readings.size)
        } else {
            showEmptyCharts()
            dateTitle.text = getString(R.string.weather_trends_no_data)
        }
    }
    
    private fun updateTemperatureChart(indoorEntries: List<Entry>, outdoorEntries: List<Entry>) {
         // Indoor temperature dataset
         val indoorDataSet = LineDataSet(indoorEntries, "Indoor °C")
         indoorDataSet.color = Color.RED
         indoorDataSet.setCircleColor(indoorDataSet.color)
         indoorDataSet.lineWidth = 3f
         indoorDataSet.circleRadius = 1f
         indoorDataSet.setDrawCircleHole(false)
         indoorDataSet.valueTextSize = 8f
         indoorDataSet.valueTextColor = Color.WHITE

         // Outdoor temperature dataset
         val outdoorDataSet = LineDataSet(outdoorEntries, "Outdoor °C")
         outdoorDataSet.color = Color.CYAN
         outdoorDataSet.setCircleColor(outdoorDataSet.color)
         outdoorDataSet.lineWidth = 3f
         outdoorDataSet.circleRadius = 1f
         outdoorDataSet.setDrawCircleHole(false)
         outdoorDataSet.valueTextSize = 8f
         outdoorDataSet.valueTextColor = Color.WHITE

         val lineData = LineData(indoorDataSet, outdoorDataSet)
         temperatureChart.data = lineData

         // Dynamic Y axis for temperature: remove min/max, enable auto-scale
         val leftAxis = temperatureChart.axisLeft
         temperatureChart.setAutoScaleMinMaxEnabled(true)
         leftAxis.removeAllLimitLines()
         // Remove any fixed min/max
         leftAxis.resetAxisMinimum()
         leftAxis.resetAxisMaximum()
         // Ensure grid/label styling (keeps behavior consistent)
         leftAxis.setDrawGridLines(true)
         leftAxis.textColor = Color.WHITE
         leftAxis.gridColor = Color.GRAY

         // Formatter for minutes -> HH:mm
         val timeFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }
        temperatureChart.xAxis.valueFormatter = timeFormatter
        temperatureChart.xAxis.axisMinimum = 0f
        temperatureChart.xAxis.axisMaximum = (24 * 60).toFloat()
        temperatureChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        temperatureChart.moveViewToX(0f)
        temperatureChart.invalidate()
     }

    private fun updateHumidityChart(humidityEntries: List<Entry>) {
         val humidityDataSet = LineDataSet(humidityEntries, "Humidity %")
         humidityDataSet.color = Color.BLUE
         humidityDataSet.setCircleColor(humidityDataSet.color)
         humidityDataSet.lineWidth = 3f
         humidityDataSet.circleRadius = 1f
         humidityDataSet.setDrawCircleHole(false)
         humidityDataSet.valueTextSize = 8f
         humidityDataSet.valueTextColor = Color.WHITE

         val lineData = LineData(humidityDataSet)
         humidityChart.data = lineData
         val leftAxis = humidityChart.axisLeft
         // Set Y axis 0-100%, granularity 10, label count 11
         leftAxis.setAxisMinimum(0f)
         leftAxis.setAxisMaximum(100f)
         leftAxis.granularity = 10f
         leftAxis.setLabelCount(11, true)
         leftAxis.isGranularityEnabled = true
         leftAxis.setDrawGridLines(true)
         leftAxis.textColor = Color.WHITE
         leftAxis.gridColor = Color.GRAY
         humidityChart.setAutoScaleMinMaxEnabled(false)
         val timeFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }
        humidityChart.xAxis.valueFormatter = timeFormatter
        humidityChart.xAxis.axisMinimum = 0f
        humidityChart.xAxis.axisMaximum = (24 * 60).toFloat()
        humidityChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        humidityChart.moveViewToX(0f)
        humidityChart.invalidate()
     }

    private fun updateWindChart(windEntries: List<Entry>) {
         val windDataSet = LineDataSet(windEntries, "Wind km/h")
         windDataSet.color = Color.GREEN
         windDataSet.setCircleColor(windDataSet.color)
         windDataSet.lineWidth = 3f
         windDataSet.circleRadius = 1f
         windDataSet.setDrawCircleHole(false)
         windDataSet.valueTextSize = 8f
         windDataSet.valueTextColor = Color.WHITE

         val lineData = LineData(windDataSet)
         windChart.data = lineData
         val timeFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }
        windChart.xAxis.valueFormatter = timeFormatter
        windChart.xAxis.axisMinimum = 0f
        windChart.xAxis.axisMaximum = (24 * 60).toFloat()
        windChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        windChart.moveViewToX(0f)
        windChart.invalidate()
     }

    private fun updateWindDirectionChart(dirEntries: List<Entry>) {
         val dirDataSet = LineDataSet(dirEntries, "Wind Dir °")
         dirDataSet.color = Color.MAGENTA
         dirDataSet.setCircleColor(dirDataSet.color)
         dirDataSet.lineWidth = 2.5f
         dirDataSet.circleRadius = 1f
         dirDataSet.setDrawCircleHole(false)
         dirDataSet.valueTextSize = 8f
         dirDataSet.valueTextColor = Color.WHITE

         val lineData = LineData(dirDataSet)
         windDirectionChart.data = lineData
         val timeFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }
        windDirectionChart.xAxis.valueFormatter = timeFormatter
        windDirectionChart.xAxis.axisMinimum = 0f
        windDirectionChart.xAxis.axisMaximum = (24 * 60).toFloat()
        windDirectionChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        windDirectionChart.moveViewToX(0f)

         // Constrain Y axis to 0..360 degrees for clarity
         val leftAxis = windDirectionChart.axisLeft
         leftAxis.axisMinimum = 0f
         leftAxis.axisMaximum = 360f
         leftAxis.granularity = 45f

         windDirectionChart.invalidate()
     }

     private fun showEmptyCharts() {
         // Show empty charts with placeholder data
         val emptyEntries = listOf(Entry(0f, 0f))

        updateTemperatureChart(emptyEntries, emptyEntries)
        updateHumidityChart(emptyEntries)
        updateWindChart(emptyEntries)
        updateWindDirectionChart(emptyEntries)
    }

    // Helper: parse a timestamp (expected like "YYYY-MM-DD HH:MM:SS" or "YYYY-MM-DD HH:MM") to minutes since midnight
     private fun parseTimeToMinutes(timestamp: String): Int {
        // Prefer a flexible regex that finds the first HH:MM occurrence (handles missing seconds and different separators)
        val regex = Regex("(\\d{1,2}):(\\d{2})")
        val match = regex.find(timestamp)
        if (match != null) {
            val h = match.groupValues[1].toIntOrNull() ?: 0
            val m = match.groupValues[2].toIntOrNull() ?: 0
            return ((h % 24) * 60) + (m % 60)
        }

        // Fallback: existing simple split-on-space approach
        val timePart = timestamp.split(" ").getOrNull(1) ?: return 0
        val parts = timePart.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (hour * 60) + minute
     }
 }
