package com.security.app

import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object DriveCsvFetcher {
    /**
     * Try to fetch file content via Drive API if 'driveService' is provided, otherwise try public download URL
     * fileId is the Drive file ID. Returns file content or null on error.
     */
    suspend fun fetchCsv(driveService: Drive?, fileId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (driveService != null) {
                    // Use Drive API to download file
                    val input = driveService.files().get(fileId).executeMediaAsInputStream()
                    val reader = BufferedReader(InputStreamReader(input))
                    val text = reader.readText()
                    reader.close()
                    return@withContext text
                } else {
                    // Fallback to public file URL (export/download endpoint)
                    val urlStr = "https://drive.google.com/uc?export=download&id=$fileId"
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val text = reader.readText()
                    reader.close()
                    return@withContext text
                }
            } catch (e: Exception) {
                android.util.Log.e("DriveCsvFetcher", "Failed fetching Drive CSV for $fileId", e)
                null
            }
        }
    }
}

