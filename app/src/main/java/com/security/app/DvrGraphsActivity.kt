package com.security.app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class DvrGraphsActivity : BaseActivity() {
    
    private lateinit var dvrTempChart: LineChart
    private lateinit var currentDvrStatus: TextView
    private lateinit var dateTitle: TextView
    private lateinit var dvrRawCsvPreview: TextView
    private lateinit var dvrRawCsvCard: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dvr_graphs)
        
        // Initialize views
        dvrTempChart = findViewById(R.id.dvrTempChart)
        currentDvrStatus = findViewById(R.id.currentDvrStatus)
        dateTitle = findViewById(R.id.dateTitle)
        dvrRawCsvPreview = findViewById(R.id.dvrRawCsvPreview)
        dvrRawCsvCard = findViewById(R.id.dvrRawCsvCard)

        // Setup return button
        findViewById<TextView>(R.id.returnButton).setOnClickListener {
            finish()
        }
        
        // Set today's date
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateString = getString(R.string.dvr_trends_date, today)
        dateTitle.text = dateString

        // Setup charts
        setupChart()

        // Setup swipe to refresh
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                GraphDataRepository.refreshDvrData()
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observe data from the repository
        lifecycleScope.launch {
            GraphDataRepository.dvrGraphData.collect { readings ->
                if (readings.isNotEmpty()) {
                    dateTitle.text = dateString
                    updateChart(readings)
                } else {
                    dateTitle.text = getString(R.string.loading_data)
                }
            }
        }
    }
    
    private fun setupChart() {
        dvrTempChart.description.isEnabled = false
        dvrTempChart.setTouchEnabled(true)
        dvrTempChart.isDragEnabled = true
        dvrTempChart.setScaleEnabled(true)
        dvrTempChart.setPinchZoom(true)
        dvrTempChart.setBackgroundColor(Color.TRANSPARENT)
        
        // X-axis setup
        val xAxis = dvrTempChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.textColor = Color.WHITE
        xAxis.gridColor = Color.GRAY
        xAxis.granularity = 60f
        xAxis.labelRotationAngle = -45f

        // Y-axis setup
        val leftAxis = dvrTempChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = Color.WHITE
        leftAxis.gridColor = Color.GRAY
        
        val rightAxis = dvrTempChart.axisRight
        rightAxis.isEnabled = false

        // Enable touch gestures
        dvrTempChart.setTouchEnabled(true)
        dvrTempChart.isDragEnabled = true
        dvrTempChart.setScaleEnabled(true)
        dvrTempChart.setPinchZoom(true)

        // Add a marker view
        val mv = CustomMarkerView(this, R.layout.custom_marker_view)
        dvrTempChart.marker = mv
    }

    private fun updateChart(readings: List<GoogleSheetsReader.SensorReading>) {
        if (readings.isNotEmpty()) {
            dvrRawCsvCard.visibility = android.view.View.GONE
            val tempEntries = mutableListOf<Entry>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            readings.forEach { reading ->
                try {
                    val date = dateFormat.parse(reading.timestamp)
                    if (date != null) {
                        tempEntries.add(Entry(date.time.toFloat(), reading.dvrTemp))
                    }
                } catch (_: Exception) {
                    // Ignore malformed timestamps
                }
            }

            val dataSet = LineDataSet(tempEntries, "DVR Temp")
            dataSet.color = "#FF5722".toColorInt()
            dataSet.setCircleColor(dataSet.color)
            dataSet.setDrawCircles(true)
            dataSet.circleRadius = 1f
            dataSet.lineWidth = 1.5f
            dataSet.valueTextColor = Color.WHITE
            dataSet.setDrawValues(false)

            dvrTempChart.clear() // Clear all chart data and settings
            setupChart() // Re-apply chart settings
            val lineData = LineData(dataSet)
            dvrTempChart.data = lineData

            val xAxis = dvrTempChart.xAxis
            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return dateFormat.format(Date(value.toLong()))
                }
            }
            dvrTempChart.invalidate()

            val latestTempForStatus = readings.last().dvrTemp.toDouble()
            val tempStr = String.format(Locale.getDefault(), "%.1f", latestTempForStatus)
            currentDvrStatus.text = getString(R.string.dvr_running_normal, tempStr)
        } else {
            dvrRawCsvCard.visibility = android.view.View.VISIBLE
            dvrRawCsvPreview.text = getString(R.string.dvr_no_dvr_data)
        }
    }
}
