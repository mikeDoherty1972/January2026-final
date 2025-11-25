package com.security.app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MikeWaterGraphsActivity : BaseActivity() {

    private lateinit var mikeWaterChart: LineChart
    private lateinit var dateTitle: TextView
    private var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mike_water_graphs)

        mikeWaterChart = findViewById(R.id.mikeWaterChart)
        dateTitle = findViewById(R.id.dateTitle)
        swipeRefresh = findViewById(R.id.mikeSwipeRefresh)

        // Return button
        findViewById<TextView>(R.id.returnButton).setOnClickListener {
            finish()
        }

        // Setup pull-to-refresh to force reload the water meter CSV and update repository
        swipeRefresh?.setOnRefreshListener {
            // Force refresh cache and repository
            GraphDataRepository.refreshWaterMeterData(forceRefresh = true)
            // We'll stop the spinner when the repository emits new data in observeWaterMeterData
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dateTitle.text = getString(R.string.mike_water_title_format, today)

        setupChart()
        observeWaterMeterData()
    }

    private fun setupChart() {
        mikeWaterChart.description.isEnabled = false
        mikeWaterChart.setTouchEnabled(true)
        mikeWaterChart.isDragEnabled = true
        mikeWaterChart.setScaleEnabled(true)
        mikeWaterChart.setPinchZoom(true)

        // Apply configurable graph background color (falls back to transparent)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val graphBg = prefs.getString("graph_background_color", null)
        val bgColor = try {
            if (graphBg != null) Color.parseColor(graphBg) else Color.TRANSPARENT
        } catch (_: IllegalArgumentException) {
            Color.TRANSPARENT
        }
        mikeWaterChart.setBackgroundColor(bgColor)

        val mv = CustomMarkerView(this, R.layout.custom_marker_view)
        mikeWaterChart.marker = mv

        val xAxis = mikeWaterChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.textColor = Color.WHITE
        xAxis.gridColor = Color.GRAY
        xAxis.granularity = 60f
        xAxis.labelRotationAngle = -45f

        val leftAxis = mikeWaterChart.axisLeft
        mikeWaterChart.setAutoScaleMinMaxEnabled(true)
        leftAxis.removeAllLimitLines()
        leftAxis.resetAxisMinimum()
        leftAxis.resetAxisMaximum()
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = Color.WHITE
        leftAxis.gridColor = Color.GRAY

        val rightAxis = mikeWaterChart.axisRight
        rightAxis.isEnabled = false
    }

    private fun observeWaterMeterData() {
        lifecycleScope.launch {
            GraphDataRepository.waterMeterGraphData.collect { readings ->
                // Ensure pull-to-refresh spinner stops when data arrives (or empty list)
                try { swipeRefresh?.isRefreshing = false } catch (_: Exception) {}
                if (readings.isNotEmpty()) {
                    val entries = readings.map { reading ->
                        val calendar = Calendar.getInstance().apply { time = reading.date }
                        val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                        Entry(minutes.toFloat(), reading.meterReading)
                    }
                    updateChart(entries)
                    dateTitle.text = getString(R.string.mike_water_today_usage)
                } else {
                    showEmpty()
                    dateTitle.text = getString(R.string.loading_data)
                }
            }
        }
    }

    private fun updateChart(entries: List<Entry>) {
        val ds = LineDataSet(entries, "Meter Reading")
        ds.color = Color.CYAN
        ds.setCircleColor(ds.color)
        ds.lineWidth = 3f
        ds.circleRadius = 1f
        ds.setDrawCircleHole(false)
        ds.valueTextSize = 8f
        ds.valueTextColor = Color.WHITE

        mikeWaterChart.data = LineData(ds)

        val leftAxis = mikeWaterChart.axisLeft
        mikeWaterChart.setAutoScaleMinMaxEnabled(true)
        leftAxis.removeAllLimitLines()
        leftAxis.resetAxisMinimum()
        leftAxis.resetAxisMaximum()
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = Color.WHITE
        leftAxis.gridColor = Color.GRAY

        val timeFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val minutes = ((value.toInt() % (24 * 60)) + (24 * 60)) % (24 * 60)
                val h = minutes / 60
                val m = minutes % 60
                return String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }
        }
        mikeWaterChart.xAxis.valueFormatter = timeFormatter
        mikeWaterChart.xAxis.axisMinimum = 0f
        mikeWaterChart.xAxis.axisMaximum = (24 * 60).toFloat()
        mikeWaterChart.data?.notifyDataChanged()
        mikeWaterChart.notifyDataSetChanged()
        mikeWaterChart.setVisibleXRangeMaximum((24 * 60).toFloat())
        mikeWaterChart.moveViewToX(0f)
        mikeWaterChart.invalidate()
    }

    private fun showEmpty() {
        val emptyEntries = listOf(Entry(0f, 0f))
        updateChart(emptyEntries)
    }
}
