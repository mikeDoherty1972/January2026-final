package com.security.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory cache for Google Sheets CSV exports by gid.
 * Fetches CSV once per gid and caches it for the app lifetime unless forceRefresh is true.
 */
object SheetsCache {
    private const val SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA"
    private val cache = ConcurrentHashMap<Int, String>()

    suspend fun fetchCsv(gid: Int = 0, forceRefresh: Boolean = false): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh) {
                    val cached = cache[gid]
                    if (cached != null) return@withContext cached
                }

                val url = URL("https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv&gid=$gid")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val csv = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                cache[gid] = csv
                csv
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun clearCache(gid: Int? = null) {
        if (gid == null) cache.clear() else cache.remove(gid)
    }
}

