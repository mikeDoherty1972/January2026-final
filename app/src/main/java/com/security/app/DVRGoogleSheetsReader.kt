package com.security.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class DVRGoogleSheetsReader {
    companion object {
        private const val SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA"
        private const val DVR_DATA_URL = "https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv&gid=2109322930"
    }

    suspend fun fetchLatestDvrReadings(maxRows: Int = 1000): List<GoogleSheetsReader.SensorReading> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(DVR_DATA_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val csvData = reader.readText()
                reader.close()
                parseDvrData(csvData, maxRows)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseDvrData(csvData: String, maxRows: Int): List<GoogleSheetsReader.SensorReading> {
        val readings = mutableListOf<GoogleSheetsReader.SensorReading>()
        val lines = csvData.lines()
        val startIndex = (lines.size - maxRows).coerceAtLeast(1)
        for (i in startIndex until lines.size) {
            val line = lines[i]
            val columns = line.split(",")
            if (columns.size > 1) {
                try {
                    val dvrTemp = columns[0].toFloatOrNull() ?: 0f
                    val timestamp = columns[1]
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = dateFormat.parse(timestamp)
                    if (date != null) {
                        readings.add(
                            GoogleSheetsReader.SensorReading(
                                timestamp = timestamp,
                                indoorTemp = 0f,
                                outdoorTemp = 0f,
                                humidity = 0f,
                                windSpeed = 0f,
                                waterPressure = 0f,
                                currentPower = 0.0f,
                                currentAmps = 0.0f,
                                dailyPower = 0.0f,
                                windDirection = 0f,
                                dvrTemp = dvrTemp,
                                waterTemp = 0f,
                                dailyWater = 0f
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore malformed lines
                }
            }
        }
        return readings
    }
}
