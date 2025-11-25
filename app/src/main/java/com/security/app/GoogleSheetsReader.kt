package com.security.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

/**
 * Utility class to read sensor data from Google Sheets
 * Uses CSV export format: /export?format=csv&gid=0
 */
class GoogleSheetsReader {
    
    companion object {
        // Your Google Sheets ID and CSV export URL
        private const val SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA"
        private const val CSV_URL = "https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv&gid=0"
    }
    
    data class SensorReading(
        val timestamp: String,
        val indoorTemp: Float,
        val outdoorTemp: Float,
        val humidity: Float,
        val windSpeed: Float,
        val windDirection: Float,
        val currentPower: Float,
        val currentAmps: Float,
        val dailyPower: Float,
        val dvrTemp: Float,
        val waterTemp: Float,
        val waterPressure: Float,
        val dailyWater: Float
    )
    
    /**
     * Fetch the latest sensor data from Google Sheets
     * Returns the most recent readings for today
     */
    suspend fun fetchLatestReadings(maxRows: Int = 100): List<SensorReading> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(CSV_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val csvData = reader.readText()
                reader.close()
                
                parseCsvData(csvData, maxRows)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Fetch sensor readings for a specific date
     */
    suspend fun fetchReadingsForDate(date: String, maxRows: Int = 500): List<SensorReading> {
        return withContext(Dispatchers.IO) {
            try {
                val allReadings = fetchLatestReadings(1000) // Get more data for date filtering
                
                // Filter readings for the specific date
                allReadings.filter { reading ->
                    reading.timestamp.startsWith(date)
                }.take(maxRows)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Parse CSV data into SensorReading objects - ALWAYS GET LAST ROW
     */
    private fun parseCsvData(csvData: String, maxRows: Int): List<SensorReading> {
        val readings = mutableListOf<SensorReading>()
        val lines = csvData.split("\n").filter { it.trim().isNotEmpty() }
        if (lines.size <= 1) return readings

        // Build a header -> index map if header row looks reasonable
        val headerFields = parseCsvLine(lines[0]).map { it.trim().trim('"').lowercase(Locale.getDefault()) }
        val headerMap = headerFields.mapIndexed { idx, name -> name to idx }.toMap()
        // Diagnostic: log header fields and mapping to help debug column mismatches
        try {
            Log.d("GoogleSheetsReader", "CSV header fields (${headerFields.size}): ${headerFields.joinToString(", ")}")
            Log.d("GoogleSheetsReader", "CSV header map: ${headerMap.toString()}")
        } catch (_: Exception) {}

        // Helper to resolve field by known header names or fallback to one of several column indices
        fun resolveField(fields: List<String>, headerNames: List<String>, fallbackIndices: List<Int> = emptyList()): String {
            // Try header-based match first (case-insensitive, normalized)
            for (hn in headerNames) {
                val key = hn.trim().lowercase(Locale.getDefault())
                val idx = headerMap[key]
                if (idx != null && idx < fields.size) return fields[idx]
            }
            // Try fallback indices in order (use the first valid index within field bounds)
            for (fi in fallbackIndices) {
                if (fi >= 0 && fi < fields.size) return fields[fi]
            }
            // Last resort: try to find the first numeric-looking field (helps when header was missing)
            for (f in fields) {
                val v = f.trim().trim('"')
                if (v.isNotEmpty() && v.matches(Regex("^-?\\d+(\\\\.\\d+)?$"))) return f
            }
            return ""
        }

        // Parse ALL rows first, then take the last ones
        for (i in 1 until lines.size) { // Skip header row (index 0)
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            try {
                val fields = parseCsvLine(line)
                // Diagnostic: for the first few rows, log field count and a short preview to help debugging
                if (i < 6) {
                    try { Log.d("GoogleSheetsReader", "Row #${i} fields=${fields.size} sample=${fields.take(8).joinToString("|")}") } catch (_: Exception) {}
                }
                // allow flexible column counts; try header-based mapping first then fallbacks
                val ts = resolveField(fields, listOf("timestamp", "time", "date", "datetime"), listOf(0)).trim('"')
                if (ts.isEmpty()) continue

                // Map names - include more synonyms and provide multiple fallback indices where reasonable
                val indoorRaw = resolveField(fields, listOf("indoor", "indoor_temp", "indoor temperature", "indoortemp", "temp_indoor"), listOf(1, 2))
                val outdoorRaw = resolveField(fields, listOf("outdoor", "outdoor_temp", "outdoor temperature", "outsidetemp", "outside", "temp_out", "temperature_out", "outside_temp"), listOf(2, 3))
                val humidityRaw = resolveField(fields, listOf("humidity", "humid", "rh", "relative_humidity"), listOf(3, 4))
                val windRaw = resolveField(fields, listOf("wind_speed", "wind speed", "wind", "wind_kmh", "wind_ms"), listOf(4, 5))
                val windDirRaw = resolveField(fields, listOf("wind_direction", "wind_dir", "winddeg", "wind_deg", "direction", "wind_heading"), listOf(11, 12, 5))
                val powerRaw = resolveField(fields, listOf("currentpower", "current_power", "power", "kw", "kW", "power_kw"), listOf(7, 6))
                val ampsRaw = resolveField(fields, listOf("amps", "currentamps", "current_amps", "amp", "amperes"), listOf(8, 7))
                val dailyPowerRaw = resolveField(fields, listOf("daily_power", "dailypower", "daily", "day_kwh"), listOf(9, 10))
                val dvrHeaderCandidates = listOf("dvrtemp", "dvr_temp", "dvr", "dvr_temperature")
                val dvrRaw = resolveField(fields, dvrHeaderCandidates, listOf(12, 13, 11))
                // Diagnostic: try to resolve which index was used for dvr (header match or fallback)
                try {
                    val normalizedHeaders = dvrHeaderCandidates.map { it.trim().lowercase(Locale.getDefault()) }
                    val matchedHeaderIndex = normalizedHeaders.mapNotNull { headerMap[it] }.firstOrNull()
                    val fallbackUsed = when {
                        matchedHeaderIndex != null && matchedHeaderIndex < fields.size -> "header_index=$matchedHeaderIndex"
                        else -> {
                            val usedFi = listOf(12,13,11).firstOrNull { it >=0 && it < fields.size }
                            if (usedFi != null) "fallback_index=$usedFi" else "no_index_found"
                        }
                    }
                    Log.d("GoogleSheetsReader", "DVR field resolved: $dvrRaw (source=$matchedHeaderIndex, fallbackCheck=$fallbackUsed, fields=${fields.size})")
                } catch (_: Exception) {}
                val waterTempRaw = resolveField(fields, listOf("water_temp", "water temperature", "water_temp_c", "geyser_temp", "hot_water_temp"), listOf(5, 6))
                val waterPressureRaw = resolveField(fields, listOf("water_pressure", "pressure", "geyser_pressure", "water_press"), listOf(6, 5))
                val dailyWaterRaw = resolveField(fields, listOf("daily_water", "dailywater", "daily_water_volume", "water_daily"), listOf(10, 11))

                val reading = SensorReading(
                    timestamp = ts,
                    indoorTemp = parseFloat(indoorRaw),
                    outdoorTemp = parseFloat(outdoorRaw),
                    humidity = parseFloat(humidityRaw),
                    windSpeed = parseFloat(windRaw),
                    windDirection = parseFloat(windDirRaw),
                    currentPower = parseFloat(powerRaw),
                    currentAmps = parseFloat(ampsRaw),
                    dailyPower = parseFloat(dailyPowerRaw),
                    dvrTemp = parseFloat(dvrRaw),
                    waterTemp = parseFloat(waterTempRaw),
                    waterPressure = parseFloat(waterPressureRaw),
                    dailyWater = parseFloat(dailyWaterRaw)
                )

                // Debug: log the first few parsed rows so we can diagnose missing weather values quickly
                if (readings.size < 5) {
                    Log.d("GoogleSheetsReader", "Parsed row ts=${reading.timestamp} outdoor=${reading.outdoorTemp} hum=${reading.humidity} wind=${reading.windSpeed} dir=${reading.windDirection}")
                }
                readings.add(reading)
            } catch (e: Exception) {
                // Skip invalid rows
                continue
            }
        }

        // Return LAST rows only (most recent data)
        return if (readings.size > maxRows) {
            readings.takeLast(maxRows).reversed() // Take last maxRows, most recent first
        } else {
            readings.reversed() // Most recent first
        }
    }
    
    /**
     * Parse a CSV line handling quoted fields
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                else -> currentField.append(char)
            }
        }
        
        // Add the final field
        fields.add(currentField.toString())
        return fields
    }
    
    /**
     * Safely parse float from string
     */
    private fun parseFloat(value: String): Float {
        return try {
            val cleanValue = value.trim().trim('"')
            if (cleanValue.isEmpty() || cleanValue == "null") 0.0f
            else cleanValue.toFloat()
        } catch (e: Exception) {
            0.0f
        }
    }
    
    /**
     * Get readings for the last N hours
     */
    suspend fun fetchRecentReadings(hours: Int = 24): List<SensorReading> {
        return withContext(Dispatchers.IO) {
            try {
                val allReadings = fetchLatestReadings(500)
                val cutoffTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000)
                
                allReadings.filter { reading ->
                    try {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(reading.timestamp)
                        timestamp?.time ?: 0 > cutoffTime
                    } catch (e: Exception) {
                        true // Include if we can't parse timestamp
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Get readings from a specific sheet gid (tab) - useful for alternate trend tabs
     */
    suspend fun fetchRecentReadingsFromGid(gid: Int, hours: Int = 24, maxRows: Int = 500): List<SensorReading> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv&gid=$gid")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val csvData = reader.readText()
                reader.close()

                // Heuristics: detect 2-column CSVs (value, timestamp) used by some trend tabs
                val lines = csvData.split("\n").filter { it.trim().isNotEmpty() }
                Log.d("GoogleSheetsReader", "fetchRecentReadingsFromGid: gid=$gid, lines=${lines.size}")
                if (lines.size >= 2) {
                    val headerFields = parseCsvLine(lines[0])
                    Log.d("GoogleSheetsReader", "headerFields=${headerFields.joinToString()}")
                    // If header has 2 columns or contains 'Average Temperature', use two-column parser
                    if (headerFields.size == 2 || headerFields.any { it.contains("Average", ignoreCase = true) }) {
                        val parsed = parseTwoColumnCsvData(csvData, maxRows)
                        Log.d("GoogleSheetsReader", "parseTwoColumnCsvData returned ${parsed.size} rows")
                        // For two-column trend tabs return the parsed historical series (up to maxRows)
                        return@withContext if (parsed.size > maxRows) parsed.takeLast(maxRows).reversed() else parsed
                    }
                }

                // Fallback to full parser for wide CSVs
                var parsed = parseCsvData(csvData, 1000)
                Log.d("GoogleSheetsReader", "parseCsvData returned ${parsed.size} rows")

                // If the wide parser returned nothing, attempt to parse as a 2-column trend (tab or csv)
                if (parsed.isEmpty()) {
                    val twoCol = parseTwoColumnCsvData(csvData, maxRows)
                    Log.d("GoogleSheetsReader", "fallback parseTwoColumnCsvData returned ${twoCol.size} rows")
                    if (twoCol.isNotEmpty()) {
                        return@withContext if (twoCol.size > maxRows) twoCol.takeLast(maxRows).reversed() else twoCol
                    }
                }

                // Filter by the requested hours window
                val cutoffTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000)
                return@withContext parsed.filter { reading ->
                    try {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(reading.timestamp)
                        timestamp?.time ?: 0 > cutoffTime
                    } catch (e: Exception) {
                        true // Include if we can't parse timestamp
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Parse CSVs that are in a simple two-column format: value, timestamp
     * Produces SensorReading objects with dvrTemp set to column0 and timestamp column1.
     */
    private fun parseTwoColumnCsvData(csvData: String, maxRows: Int): List<SensorReading> {
        val readings = mutableListOf<SensorReading>()
        val lines = csvData.split("\n").filter { it.trim().isNotEmpty() }
        for (i in 1 until lines.size) { // skip header
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            try {
                var fields = parseCsvLine(line)
                // If CSV parser didn't find two fields (maybe tab-separated), try splitting on tab
                if (fields.size < 2 && line.contains('\t')) {
                    fields = line.split('\t').map { it.trim() }
                }
                if (fields.size >= 2) {
                    val value = parseFloat(fields[0])
                    // timestamp may be quoted; trim quotes and spaces
                    val timestamp = fields[1].trim().trim('"')
                    val reading = SensorReading(
                        timestamp = timestamp,
                        indoorTemp = 0.0f,
                        outdoorTemp = 0.0f,
                        humidity = 0.0f,
                        windSpeed = 0.0f,
                        windDirection = 0.0f,
                        currentPower = 0.0f,
                        currentAmps = 0.0f,
                        dailyPower = 0.0f,
                        dvrTemp = value, // value column mapped to dvrTemp for trend parsing
                        waterTemp = 0.0f,
                        waterPressure = 0.0f,
                        dailyWater = 0.0f
                    )
                    readings.add(reading)
                }
            } catch (e: Exception) {
                // skip invalid rows
                continue
            }
        }

        return if (readings.size > maxRows) readings.takeLast(maxRows).reversed() else readings.reversed()
    }

    /**
     * Fetch raw CSV data for a specific gid (tab) - used for previewing CSV content
     */
    suspend fun fetchRawCsvForGid(gid: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv&gid=$gid")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val csvData = reader.readText()
                reader.close()
                csvData
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Public helper to parse a raw CSV string (from Sheets or Drive) into SensorReading objects.
     * This exposes the internal parser so other modules (e.g. Drive fetchers) can reuse it.
     */
    fun parseCsvStringToSensorReadings(csvData: String, maxRows: Int = 100): List<SensorReading> {
        return parseCsvData(csvData, maxRows)
    }
}