package com.security.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class IDSActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the XML layout provided by the project
        setContentView(R.layout.activity_ids)

        // Back button
        val back = findViewById<TextView>(R.id.idsBackButton)
        back.setOnClickListener { finish() }

        // Main card
        val card = findViewById<CardView>(R.id.idsMainCard)

        // Apply saved per-card color if present
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val idsPref = prefs.getString("card_ids_color", null)
        if (!idsPref.isNullOrEmpty()) {
            try {
                card.setCardBackgroundColor(Color.parseColor(idsPref))
            } catch (e: IllegalArgumentException) {
                // ignore invalid color
            }
        }

        // Long-press on the card opens appearance choices (per-card and graph background)
        card.setOnLongClickListener {
            val options = arrayOf("Card background color", "Graph background color")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("IDS appearance")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> showColorPicker("card_ids_color", "Choose IDS card color") {
                        android.widget.Toast.makeText(this, "IDS card color saved. Return to dashboard to see change.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    1 -> showColorPicker("graph_background_color", "Choose IDS graph background") {
                        android.widget.Toast.makeText(this, "IDS graph background saved.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            builder.show()
            true
        }
    }
}
