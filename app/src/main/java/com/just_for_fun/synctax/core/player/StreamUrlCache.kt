package com.just_for_fun.synctax.core.player

import android.util.Log

/**
 * In-memory cache for YouTube stream URLs with TTL (Time-To-Live).
 * YouTube stream URLs typically expire after ~6 hours, so we cache them
 * for 30 minutes to ensure they're still valid when used.
 */
object StreamUrlCache {

    private const val TAG = "StreamUrlCache"
    private const val DEFAULT_TTL_MS = 30 * 60 * 1000L // 30 minutes
    private const val MAX_CACHE_SIZE = 50 // Maximum entries to keep

    private data class CachedUrl(
        val streamUrl: String,
        val expiresAt: Long
    )

    private val cache = mutableMapOf<String, CachedUrl>()

    /**
     * Store a stream URL in cache.
     * 
     * @param videoId The YouTube video ID
     * @param streamUrl The extracted stream URL
     * @param ttlMs Time-to-live in milliseconds (default: 30 minutes)
     */
    @Synchronized
    fun put(videoId: String, streamUrl: String, ttlMs: Long = DEFAULT_TTL_MS) {
        // Evict oldest entries if cache is full
        if (cache.size >= MAX_CACHE_SIZE) {
            evictOldest()
        }

        val expiresAt = System.currentTimeMillis() + ttlMs
        cache[videoId] = CachedUrl(streamUrl, expiresAt)
        Log.d(TAG, "Cached stream URL for: $videoId (expires in ${ttlMs / 1000}s)")
    }

    /**
     * Get a cached stream URL if it exists and hasn't expired.
     * 
     * @param videoId The YouTube video ID
     * @return The cached stream URL, or null if not found/expired
     */
    @Synchronized
    fun get(videoId: String): String? {
        val cached = cache[videoId] ?: return null

        if (System.currentTimeMillis() > cached.expiresAt) {
            // URL has expired, remove it
            cache.remove(videoId)
            Log.d(TAG, "Stream URL expired for: $videoId")
            return null
        }

        Log.d(TAG, "Cache hit for: $videoId")
        return cached.streamUrl
    }

    /**
     * Check if a stream URL is cached and not expired.
     */
    @Synchronized
    fun contains(videoId: String): Boolean {
        val cached = cache[videoId] ?: return false
        
        if (System.currentTimeMillis() > cached.expiresAt) {
            cache.remove(videoId)
            return false
        }
        
        return true
    }

    /**
     * Remove a specific video from cache.
     */
    @Synchronized
    fun remove(videoId: String) {
        cache.remove(videoId)
    }

    /**
     * Clear all expired entries.
     */
    @Synchronized
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { it.value.expiresAt < now }
            .map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${expiredKeys.size} expired entries")
        }
    }

    /**
     * Evict the oldest entry to make room for new ones.
     */
    private fun evictOldest() {
        val oldest = cache.entries.minByOrNull { it.value.expiresAt }
        oldest?.let {
            cache.remove(it.key)
            Log.d(TAG, "Evicted oldest entry: ${it.key}")
        }
    }

    /**
     * Clear the entire cache.
     */
    @Synchronized
    fun clear() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Get current cache size.
     */
    fun size(): Int = cache.size
}
