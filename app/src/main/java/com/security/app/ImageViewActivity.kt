package com.security.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import com.github.chrisbanes.photoview.PhotoView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ImageViewActivity : BaseActivity() {

    private var imageUrl: String? = null
    private lateinit var imageView: PhotoView
    private lateinit var loadingSpinner: ProgressBar
    private var forceReload: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        imageView = findViewById(R.id.imageView)
        val backButton = findViewById<Button>(R.id.backButton)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        imageUrl = intent.getStringExtra("IMAGE_URL")
        // If caller requested a forced network reload (card click), honor it
        forceReload = intent.getBooleanExtra("FORCE_RELOAD", false)

        Log.d("ImageViewActivity", "Starting ImageViewActivity with URL: $imageUrl")

        val mainHandler = Handler(Looper.getMainLooper())

        fun withCacheBusterIfNeeded(url: String): String {
            if (!forceReload) return url
            val ts = System.currentTimeMillis()
            val sep = if (url.contains("?")) "&" else "?"
            val busted = "$url${sep}_cb=$ts"
            Log.d("ImageViewActivity", "Using cache-busted URL: $busted")
            return busted
        }

        // Convert common Google Drive share URLs into a direct image URL usable by Glide.
        fun normalizeDriveUrl(raw: String): String {
            try {
                val url = raw.trim()
                // Pattern: /d/<id>/ (e.g. https://drive.google.com/file/d/FILE_ID/view)
                val fileIdRegex = Regex("/d/([A-Za-z0-9_-]+)")
                val m1 = fileIdRegex.find(url)
                if (m1 != null) {
                    // Use the direct view URL without download - works better with SSL and public images
                    return "https://lh3.googleusercontent.com/d/${m1.groupValues[1]}"
                }
                // Pattern: ?id=<id> or &id=<id> (e.g. open?id=FILE_ID or uc?export=download&id=FILE_ID or thumbnail?id=FILE_ID)
                val idParam = Regex("[?&]id=([A-Za-z0-9_-]+)").find(url)
                if (idParam != null) {
                    // Use the direct view URL without download - works better with SSL and public images
                    return "https://lh3.googleusercontent.com/d/${idParam.groupValues[1]}"
                }
                // Fallback: return original
                return url
            } catch (e: Exception) {
                Log.w("ImageViewActivity", "normalizeDriveUrl failed", e)
                return raw
            }
        }

        // Check whether the URL looks like a direct image resource by performing a HEAD and inspecting Content-Type
        suspend fun isDirectImageUrl(urlStr: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlStr)
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    instanceFollowRedirects = true
                }
                try {
                    conn.connect()
                    val code = conn.responseCode
                    if (code !in 200..399) return@withContext false
                    val ct = conn.contentType ?: return@withContext false
                    return@withContext ct.startsWith("image/")
                } finally {
                    try { conn.disconnect() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w("ImageViewActivity", "isDirectImageUrl check failed", e)
                return@withContext false
            }
        }

        fun loadImage() {
            val url = imageUrl
            if (url == null) {
                Log.w("ImageViewActivity", "No IMAGE_URL provided in intent")
                finish() // Close activity instead of showing toast
                return
            }

            // Normalize Drive share links into a direct image URL, then apply cache-busting if requested
            val normalized = normalizeDriveUrl(url)
            val urlToLoad = withCacheBusterIfNeeded(normalized)
            Log.d("ImageViewActivity", "Original URL: $url")
            Log.d("ImageViewActivity", "Normalized URL: $normalized")
            Log.d("ImageViewActivity", "Loading image: $urlToLoad (forceReload=$forceReload)")
            loadingSpinner.visibility = View.VISIBLE

            // Heuristic check: if the URL doesn't look like a direct image, inform the user (but still try Glide as fallback)
            CoroutineScope(Dispatchers.Main).launch {
                // Skip the HEAD request check for Google Drive URLs to avoid SSL issues
                val isGoogleDrive = normalized.contains("lh3.googleusercontent.com") || normalized.contains("drive.google.com")
                val looksImage = if (isGoogleDrive) {
                    Log.d("ImageViewActivity", "Skipping HEAD check for Google Drive URL")
                    true // Assume Google Drive URLs are valid images
                } else {
                    try { withTimeoutOrNull(6000) { isDirectImageUrl(normalized) } ?: false } catch (e: Exception) { false }
                }

                if (!looksImage) {
                    Log.w("ImageViewActivity", "URL may not point to a direct image: $normalized")
                    // Removed toast to prevent system conflicts
                }

                // Now perform the Glide load (existing code continues)
                val request = Glide.with(this@ImageViewActivity)
                    .load(urlToLoad)
                    .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>?, isFirstResource: Boolean): Boolean {
                            val errorMsg = e?.message ?: "Unknown error"
                            val rootCauses = e?.rootCauses?.joinToString("; ") { it.message ?: "Unknown cause" } ?: "No root causes"
                            Log.w("ImageViewActivity", "Glide load failed for: $urlToLoad")
                            Log.w("ImageViewActivity", "Error: $errorMsg")
                            Log.w("ImageViewActivity", "Root causes: $rootCauses")
                            loadingSpinner.visibility = View.GONE
                            mainHandler.post {
                                // Show error in a less aggressive way to prevent system toast conflicts
                                Log.e("ImageViewActivity", "Failed to load image - Error: $errorMsg, Root causes: $rootCauses")
                            }
                            // reset forceReload so subsequent opens use normal caching
                            forceReload = false
                            return true // handled
                        }

                        override fun onResourceReady(resource: android.graphics.drawable.Drawable?, model: Any?, target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Log.i("ImageViewActivity", "Glide resource ready for: $urlToLoad (dataSource=$dataSource)")
                            loadingSpinner.visibility = View.GONE
                            // reset forceReload after successful load
                            forceReload = false
                            return false
                        }
                    })

                if (forceReload) {
                    request
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView)
                } else {
                    request
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imageView)
                }
            }
        }

        loadImage()

        refreshButton.setOnClickListener {
            Log.d("ImageViewActivity", "Refresh requested (forcing network reload)")
            forceReload = true
            loadImage()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
