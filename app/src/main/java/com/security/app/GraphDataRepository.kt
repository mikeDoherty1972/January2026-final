package com.security.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object GraphDataRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _weatherGraphData = MutableStateFlow<List<GoogleSheetsReader.SensorReading>>(emptyList())
    val weatherGraphData: StateFlow<List<GoogleSheetsReader.SensorReading>> = _weatherGraphData

    private val _powerGraphData = MutableStateFlow<List<GoogleSheetsReader.SensorReading>>(emptyList())
    val powerGraphData: StateFlow<List<GoogleSheetsReader.SensorReading>> = _powerGraphData

    private val _geyserGraphData = MutableStateFlow<List<GoogleSheetsReader.SensorReading>>(emptyList())
    val geyserGraphData: StateFlow<List<GoogleSheetsReader.SensorReading>> = _geyserGraphData

    private val _dvrGraphData = MutableStateFlow<List<GoogleSheetsReader.SensorReading>>(emptyList())
    val dvrGraphData: StateFlow<List<GoogleSheetsReader.SensorReading>> = _dvrGraphData

    private val _waterMeterGraphData = MutableStateFlow<List<IPERLGoogleSheetsReader.WaterMeterReading>>(emptyList())
    val waterMeterGraphData: StateFlow<List<IPERLGoogleSheetsReader.WaterMeterReading>> = _waterMeterGraphData

    // New: IPERL usage statistics and signal readings flows
    private val _iperlUsageStats = MutableStateFlow<IPERLGoogleSheetsReader.UsageStats?>(null)
    val iperlUsageStats: StateFlow<IPERLGoogleSheetsReader.UsageStats?> = _iperlUsageStats

    private val _iperlSignalReadings = MutableStateFlow<List<IPERLGoogleSheetsReader.SignalReading>>(emptyList())
    val iperlSignalReadings: StateFlow<List<IPERLGoogleSheetsReader.SignalReading>> = _iperlSignalReadings

    fun setWeatherGraphData(data: List<GoogleSheetsReader.SensorReading>) {
        _weatherGraphData.value = data
    }

    fun setPowerGraphData(data: List<GoogleSheetsReader.SensorReading>) {
        _powerGraphData.value = data
    }

    fun setGeyserGraphData(data: List<GoogleSheetsReader.SensorReading>) {
        _geyserGraphData.value = data
    }

    fun setDvrGraphData(data: List<GoogleSheetsReader.SensorReading>) {
        _dvrGraphData.value = data
    }

    fun setWaterMeterGraphData(data: List<IPERLGoogleSheetsReader.WaterMeterReading>) {
        _waterMeterGraphData.value = data
    }

    // New setters for IPERL usage and signal data
    fun setIperlUsageStats(stats: IPERLGoogleSheetsReader.UsageStats?) {
        _iperlUsageStats.value = stats
    }

    fun setIperlSignalReadings(readings: List<IPERLGoogleSheetsReader.SignalReading>) {
        _iperlSignalReadings.value = readings
    }

    fun preLoadAllGraphData() {
        repositoryScope.launch {
            val googleSheetsReader = GoogleSheetsReader()
            val dvrGoogleSheetsReader = DVRGoogleSheetsReader()
            val iperlGoogleSheetsReader = IPERLGoogleSheetsReader()

            val weatherData = googleSheetsReader.fetchLatestReadings(2000)
            setWeatherGraphData(weatherData)

            val powerData = googleSheetsReader.fetchLatestReadings(2000)
            setPowerGraphData(powerData)

            val geyserData = googleSheetsReader.fetchLatestReadings(2000)
            setGeyserGraphData(geyserData)

            val dvrData = dvrGoogleSheetsReader.fetchLatestDvrReadings(2000)
            setDvrGraphData(dvrData)

            // Use shared SheetsCache to fetch gid=0 CSV once and parse water readings
            try {
                var csv = SheetsCache.fetchCsv(gid = 0, forceRefresh = false)
                if (csv != null) {
                    val readings = iperlGoogleSheetsReader.parseWaterMeterCsv(csv, 2000)
                    if (readings.isNotEmpty()) {
                        setWaterMeterGraphData(readings)
                        Log.d("GraphDataRepository", "Preloaded waterMeterGraphData from SheetsCache: ${readings.size} rows")
                    } else {
                        Log.w("GraphDataRepository", "Parsed waterMeterGraphData empty from cached CSV; attempting force refresh")
                        try {
                            csv = SheetsCache.fetchCsv(gid = 0, forceRefresh = true)
                            if (csv != null) {
                                val freshReadings = iperlGoogleSheetsReader.parseWaterMeterCsv(csv, 2000)
                                if (freshReadings.isNotEmpty()) {
                                    setWaterMeterGraphData(freshReadings)
                                    Log.d("GraphDataRepository", "Preloaded waterMeterGraphData from forced refresh: ${freshReadings.size} rows")
                                } else {
                                    Log.w("GraphDataRepository", "Forced refresh yields no readings; falling back to network fetch")
                                    try {
                                        val waterMeterData = iperlGoogleSheetsReader.fetchLatestWaterReadings(2000)
                                        setWaterMeterGraphData(waterMeterData)
                                        Log.d("GraphDataRepository", "Preloaded waterMeterGraphData via network fallback: ${waterMeterData.size} rows")
                                    } catch (e: Exception) {
                                        Log.e("GraphDataRepository", "Error fetching waterMeterGraphData via network fallback", e)
                                    }
                                }
                            } else {
                                Log.w("GraphDataRepository", "SheetsCache forced refresh returned null; falling back to network fetch")
                                try {
                                    val waterMeterData = iperlGoogleSheetsReader.fetchLatestWaterReadings(2000)
                                    setWaterMeterGraphData(waterMeterData)
                                    Log.d("GraphDataRepository", "Preloaded waterMeterGraphData via network fallback: ${waterMeterData.size} rows")
                                } catch (e: Exception) {
                                    Log.e("GraphDataRepository", "Error fetching waterMeterGraphData via network fallback", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GraphDataRepository", "Error during forced SheetsCache refresh for waterMeterGraphData", e)
                        }
                    }
                } else {
                    val waterMeterData = iperlGoogleSheetsReader.fetchLatestWaterReadings(2000)
                    setWaterMeterGraphData(waterMeterData)
                    Log.d("GraphDataRepository", "Preloaded waterMeterGraphData via network fallback: ${waterMeterData.size} rows")
                }
            } catch (e: Exception) {
                val waterMeterData = iperlGoogleSheetsReader.fetchLatestWaterReadings(2000)
                setWaterMeterGraphData(waterMeterData)
                Log.e("GraphDataRepository", "Error preloading waterMeterGraphData; used network fallback", e)
            }

            // New: preload IPERL usage stats and signal readings so IPERLActivity can observe them immediately
            try {
                try {
                    val usage = iperlGoogleSheetsReader.getUsageStatistics()
                    setIperlUsageStats(usage)
                    Log.d("GraphDataRepository", "Preloaded IPERL usage stats: $usage")
                } catch (e: Exception) {
                    Log.e("GraphDataRepository", "Failed to preload IPERL usage stats", e)
                    setIperlUsageStats(null)
                }

                try {
                    val signals = iperlGoogleSheetsReader.fetchLatestRSSIReadings(1000)
                    setIperlSignalReadings(signals)
                    Log.d("GraphDataRepository", "Preloaded IPERL signal readings: ${signals.size} rows")
                } catch (e: Exception) {
                    Log.e("GraphDataRepository", "Failed to preload IPERL signal readings", e)
                    setIperlSignalReadings(emptyList())
                }
            } catch (_: Exception) {
                // ignore outer
            }
        }
    }

    /**
     * Public method to force-refresh the water meter CSV (gid=0) and re-parse
     * Updates the `waterMeterGraphData` flow on completion (may be empty on error)
     */
    fun refreshWaterMeterData(forceRefresh: Boolean = true) {
        repositoryScope.launch {
            val reader = IPERLGoogleSheetsReader()
            try {
                var csv = SheetsCache.fetchCsv(gid = 0, forceRefresh = forceRefresh)
                if (csv != null) {
                    val readings = reader.parseWaterMeterCsv(csv, 2000)
                    setWaterMeterGraphData(readings)
                    Log.d("GraphDataRepository", "Refreshed waterMeterGraphData from SheetsCache: ${readings.size} rows")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("GraphDataRepository", "Error during refreshWaterMeterData (cache), will try network", e)
            }

            // fallback to network fetch
            try {
                val netReadings = reader.fetchLatestWaterReadings(2000)
                setWaterMeterGraphData(netReadings)
                Log.d("GraphDataRepository", "Refreshed waterMeterGraphData via network: ${netReadings.size} rows")
            } catch (e: Exception) {
                Log.e("GraphDataRepository", "Error refreshing waterMeterGraphData via network", e)
                // ensure flow updated with empty list to unblock UI
                setWaterMeterGraphData(emptyList())
            }
        }
    }
    
    fun refreshPowerData() {
        repositoryScope.launch {
            val googleSheetsReader = GoogleSheetsReader()
            val powerData = googleSheetsReader.fetchLatestReadings(2000)
            setPowerGraphData(powerData)
        }
    }
    
    fun refreshWeatherData() {
        repositoryScope.launch {
            val googleSheetsReader = GoogleSheetsReader()
            val weatherData = googleSheetsReader.fetchLatestReadings(2000)
            setWeatherGraphData(weatherData)
        }
    }
    
    fun refreshGeyserData() {
        repositoryScope.launch {
            val googleSheetsReader = GoogleSheetsReader()
            val geyserData = googleSheetsReader.fetchLatestReadings(2000)
            setGeyserGraphData(geyserData)
        }
    }
    
    fun refreshDvrData() {
        repositoryScope.launch {
            val dvrGoogleSheetsReader = DVRGoogleSheetsReader()
            val dvrData = dvrGoogleSheetsReader.fetchLatestDvrReadings(2000)
            setDvrGraphData(dvrData)
        }
    }
}
