package com.security.app

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.util.*

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    // Provide additional constructors used by Android inflation/tools
    constructor(context: Context) : this(context, R.layout.custom_marker_view)
    constructor(context: Context, attrs: AttributeSet?) : this(context, R.layout.custom_marker_view) {
        // reference params to avoid unused parameter warnings
        attrs?.toString()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(context, R.layout.custom_marker_view) {
        // reference params to avoid unused parameter warnings
        attrs?.toString()
        defStyle.toString()
    }

    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) {
            return
        }
        // Show only the Y value (no timestamp)
        tvContent.text = String.format(Locale.getDefault(), "%.1f", e.y)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
