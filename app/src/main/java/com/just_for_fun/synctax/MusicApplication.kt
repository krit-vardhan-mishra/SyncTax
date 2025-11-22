package com.just_for_fun.synctax

import android.app.Application
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import java.io.File

class MusicApplication : Application(), ImageLoaderFactory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Python runtime on background thread to avoid blocking main thread
        applicationScope.launch {
            initializePython()
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
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private fun initializePython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
                Log.d(TAG, "Python runtime initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Python runtime", e)
        }
    }

    companion object {
        private const val TAG = "MusicApplication"
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