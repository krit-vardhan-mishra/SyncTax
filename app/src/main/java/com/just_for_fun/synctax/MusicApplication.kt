package com.just_for_fun.synctax

import android.app.Application
import android.util.Log
import android.webkit.WebView
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.Options
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.core.network.NewPipeUtils
import com.just_for_fun.synctax.core.utils.YTMusicRecommender
import com.just_for_fun.synctax.core.worker.RecommendationUpdateWorker
import com.just_for_fun.synctax.core.worker.scheduleUpdateCheckIfEnabled
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import java.io.File

class MusicApplication : Application(), ImageLoaderFactory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // FFmpeg initialization status
    var isFFmpegInitialized = false
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize WebView on main thread (required for Android 10+)
        try {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            Log.d(TAG, "✅ WebView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize WebView", e)
        }

        // Initialize Python runtime on background thread to avoid blocking main thread
        applicationScope.launch {
            initializePython()
        }

        // Initialize YoutubeDL and SpotDL on background thread
        applicationScope.launch {
            initializeYoutubeDLAndFFmpeg()
        }

        // Initialize NewPipe early to avoid delays on first use
        try {
            NewPipeUtils.ensureInitialized()
            Log.d(TAG, "✅ NewPipe initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize NewPipe", e)
        }
        
        // Schedule periodic recommendation updates (every 12 hours)
        try {
            RecommendationUpdateWorker.schedule(this)
            Log.d(TAG, "✅ Recommendation worker scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule recommendation worker", e)
        }

        // Schedule periodic update checks (every 12 hours)
        try {
            scheduleUpdateCheckIfEnabled()
            Log.d(TAG, "✅ Update check worker scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule update check worker", e)
        }

        Log.d(TAG, "Music Application initialized")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // Add custom fetcher for file paths
                add(FilePathFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)  // 30% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)  // 512MB
                    .build()
            }
            // Optimize for smoothness like OuterTune/SimpMusic
            .crossfade(300) // Faster crossfade (300ms vs default 1000ms)
            .respectCacheHeaders(false) // Ignore server cache headers
            .allowHardware(false) // Disable hardware bitmaps for better thread safety
            .build()
    }

    private fun initializePython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
                Log.d(TAG, "Python runtime initialized")

                // Initialize YTMusicRecommender for song-only recommendations
                YTMusicRecommender.initialize()
                Log.d(TAG, "YTMusicRecommender initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Python runtime", e)
        }
    }

    private fun initializeYoutubeDLAndFFmpeg() {
        try {
            // Initialize YoutubeDL
            Log.d(TAG, "🔧 Initializing YoutubeDL...")
            YoutubeDL.getInstance().init(this)
            Log.d(TAG, "✅ YoutubeDL initialized successfully")

            // FFmpeg library removed to reduce APK size (~136MB savings)
            // We use Mutagen (Python) for metadata embedding instead
            isFFmpegInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize YoutubeDL: ${e.message}", e)
            isFFmpegInitialized = false
        }
    }

    companion object {
        private const val TAG = "MusicApplication"
        lateinit var instance: MusicApplication
    }
}

/**
 * Custom Coil fetcher to handle absolute file paths
 * This allows loading images from Download directory and other file paths
 */
class FilePathFetcher(
    private val data: String,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val file = File(data)

        return SourceResult(
            source = ImageSource(file.source().buffer(), options.context),
            mimeType = "image/*",
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle absolute file paths (not URLs or content URIs)
            if (data.startsWith("/") || data.contains(":\\")) {
                val file = File(data)
                if (file.exists() && file.canRead()) {
                    return FilePathFetcher(data, options)
                }
            }
            return null
        }
    }
}
