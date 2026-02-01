package com.just_for_fun.synctax.core.player

import android.content.Context
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Singleton cache manager for ExoPlayer streaming.
 * Provides a shared SimpleCache instance for all media playback,
 * enabling instant switching between songs and efficient streaming of long content.
 * 
 * Optimized for streaming like YouTube Music/Spotify:
 * - 500MB cache for longer content retention
 * - Efficient seeking via cached chunks
 * - LRU eviction keeps recent content accessible
 */
object StreamCache {

    private const val TAG = "StreamCache"
    
    // 500 MB cache - sufficient for streaming long videos while keeping recent songs cached
    // A 10-hour video at 128kbps audio = ~576MB, so this allows partial caching with LRU
    private const val CACHE_SIZE_BYTES = 500L * 1024 * 1024
    private const val CACHE_DIR_NAME = "exo_stream_cache"

    @Volatile
    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null

    /**
     * Get or create the SimpleCache instance.
     * Thread-safe singleton pattern.
     */
    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            databaseProvider = StandaloneDatabaseProvider(context)
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
            
            simpleCache = SimpleCache(
                cacheDir,
                cacheEvictor,
                databaseProvider!!
            )
            
            Log.d(TAG, "Initialized SimpleCache with ${CACHE_SIZE_BYTES / (1024 * 1024)}MB capacity")
        }
        return simpleCache!!
    }

    /**
     * Create a CacheDataSource.Factory for cached playback.
     * This wraps the default HTTP data source with caching.
     * 
     * Configured for efficient streaming:
     * - Allows reading from cache while writing (for long content)
     * - Ignores cache errors to fallback to network
     * - Uses cache for all content types
     */
    fun createCachedDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        
        // Create HTTP data source with appropriate timeouts for long content
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(60000)  // Increased read timeout for large chunks
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

        // Wrap with default data source (handles file:// URIs too)
        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Create cached data source with flags optimized for streaming
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // Use default cache sink
            .setFlags(
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or  // Fallback to network on cache error
                CacheDataSource.FLAG_BLOCK_ON_CACHE            // Block reads while data is being cached
            )
    }

    /**
     * Create a CacheDataSource.Factory specifically for preloading.
     * This is optimized for background prefetching.
     */
    fun createPreloadDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or CacheDataSource.FLAG_BLOCK_ON_CACHE)
    }

    /**
     * Check if content for a given key is cached.
     */
    fun isCached(context: Context, key: String): Boolean {
        val cache = getCache(context)
        return cache.getCachedSpans(key).isNotEmpty()
    }

    /**
     * Get cached bytes for a key.
     */
    fun getCachedBytes(context: Context, key: String): Long {
        val cache = getCache(context)
        return cache.getCachedBytes(key, 0, Long.MAX_VALUE)
    }

    /**
     * Remove cached content for a specific key.
     */
    fun removeFromCache(context: Context, key: String) {
        val cache = getCache(context)
        cache.getCachedSpans(key).forEach { span ->
            try {
                cache.removeSpan(span)
            } catch (e: Exception) {
                // Ignore removal errors
            }
        }
    }

    /**
     * Clear the entire cache.
     */
    @Synchronized
    fun clearCache() {
        try {
            simpleCache?.keys?.toList()?.forEach { key ->
                simpleCache?.getCachedSpans(key)?.forEach { span ->
                    try {
                        simpleCache?.removeSpan(span)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Release the cache. Should be called when app is destroyed.
     */
    @Synchronized
    fun release() {
        try {
            simpleCache?.release()
            simpleCache = null
            databaseProvider = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
