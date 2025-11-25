package com.security.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

open class BaseActivity : AppCompatActivity() {

    // App-wide Firestore monitor to detect missing command/status docs/fields
    private var firestoreDataMonitor: FirestoreDataMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val backgroundColor = sharedPreferences.getString("background_color", "#FFFFFF")
        window.decorView.setBackgroundColor(Color.parseColor(backgroundColor))

        // Start a lightweight Firestore monitor so the app logs when expected docs/fields are missing
        try {
            firestoreDataMonitor = FirestoreDataMonitor(this) { status -> android.util.Log.d("DataMonitor", status) }
            firestoreDataMonitor?.start()
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        try { firestoreDataMonitor?.stop() } catch (_: Exception) { }
        firestoreDataMonitor = null
        super.onDestroy()
    }

    // Reusable color picker dialog — saves the selected color to the given prefKey and calls optional callback
    fun showColorPicker(prefKey: String, dialogTitle: String, onPicked: (() -> Unit)? = null) {
        val colors = arrayOf(
            "#000000", "#FFFFFF", "#800000", "#008000", "#808000", "#000080", "#800080", "#008080",
            "#C0C0C0", "#FF0000", "#00FF00", "#FFFF00", "#0000FF", "#FF00FF", "#00FFFF", "#808080"
        )
        val colorNames = arrayOf(
            "Black", "White", "Maroon", "Green", "Olive", "Navy", "Purple", "Teal",
            "Silver", "Red", "Lime", "Yellow", "Blue", "Fuchsia", "Aqua", "Gray"
        )

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        val adapter = object : android.widget.ArrayAdapter<String>(this, R.layout.color_list_item, colorNames) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = convertView ?: layoutInflater.inflate(R.layout.color_list_item, parent, false)
                val colorSwatch = view.findViewById<android.view.View>(R.id.colorSwatch)
                val colorName = view.findViewById<TextView>(R.id.colorName)
                colorSwatch.setBackgroundColor(Color.parseColor(colors[position]))
                colorName.text = colorNames[position]
                return view
            }
        }
        builder.setAdapter(adapter) { _, which ->
            val selectedColor = colors[which]
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putString(prefKey, selectedColor)
                apply()
            }
            onPicked?.invoke()
        }
        builder.show()
    }
}
