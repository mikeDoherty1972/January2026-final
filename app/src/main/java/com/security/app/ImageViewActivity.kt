package com.security.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import com.github.chrisbanes.photoview.PhotoView
import android.widget.ProgressBar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.DiskCacheStrategy

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

        fun loadImage() {
            val url = imageUrl
            if (url == null) {
                Log.w("ImageViewActivity", "No IMAGE_URL provided in intent")
                Toast.makeText(this, "No image URL provided", Toast.LENGTH_SHORT).show()
                return
            }

            val urlToLoad = withCacheBusterIfNeeded(url)
            Log.d("ImageViewActivity", "Loading image: $urlToLoad (forceReload=$forceReload)")
            loadingSpinner.visibility = View.VISIBLE

            val request = Glide.with(this)
                .load(urlToLoad)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>?, isFirstResource: Boolean): Boolean {
                        Log.w("ImageViewActivity", "Glide load failed for: $urlToLoad", e)
                        loadingSpinner.visibility = View.GONE
                        mainHandler.post {
                            Toast.makeText(this@ImageViewActivity, "Failed to load image", Toast.LENGTH_LONG).show()
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
