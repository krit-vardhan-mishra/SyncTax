package com.just_for_fun.synctax.core.player

import android.content.Context
import android.util.Log
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.just_for_fun.synctax.core.network.OnlineSearchManager
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Manages preloading of adjacent songs (next/previous) for instant playback switching.
 * 
 * Features:
 * - Pre-fetches stream URLs before they're needed
 * - Pre-downloads first 3MB of audio data for instant start
 * - Maintains a sliding window of preloaded songs
 * - Intelligently cancels outdated preload jobs
 */
class PreloadManager(private val context: Context) {

    companion object {
        private const val TAG = "PreloadManager"
        private const val PRELOAD_BYTES = 3 * 1024 * 1024L // 3 MB per song
        private const val MAX_PRELOAD_SONGS = 3 // How many songs to keep preloaded
        private const val PRELOAD_TIMEOUT_MS = 30000L // 30 second timeout
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val onlineManager = OnlineSearchManager()
    
    // Track preload jobs by video ID
    private val preloadJobs = mutableMapOf<String, Job>()
    
    // Track which songs are currently preloaded
    private val preloadedSongs = mutableSetOf<String>()

    data class PreloadState(
        val preloadingVideoIds: Set<String> = emptySet(),
        val preloadedVideoIds: Set<String> = emptySet(),
        val currentlyPreloading: String? = null
    )

    private val _state = MutableStateFlow(PreloadState())
    val state: StateFlow<PreloadState> = _state.asStateFlow()

    /**
     * Preload songs around the current playing song.
     * Call this when a new song starts playing.
     * 
     * @param currentSong The currently playing song
     * @param nextSongs List of upcoming songs (in order)
     * @param previousSong The previous song (for going back)
     */
    fun preloadAround(currentSong: Song?, nextSongs: List<Song>, previousSong: Song?) {
        if (currentSong == null) return

        // Only preload online songs
        if (!isOnlineSong(currentSong)) return

        scope.launch {
            val songsToPreload = mutableListOf<Song>()

            // Add previous song if it's online
            previousSong?.let {
                if (isOnlineSong(it)) songsToPreload.add(it)
            }

            // Add next songs (up to MAX_PRELOAD_SONGS)
            nextSongs.take(MAX_PRELOAD_SONGS).forEach {
                if (isOnlineSong(it)) songsToPreload.add(it)
            }

            // Cancel preload jobs for songs no longer in the window
            val videoIdsToPreload = songsToPreload.mapNotNull { extractVideoId(it) }.toSet()
            cancelOutdatedJobs(videoIdsToPreload)

            // Start preloading
            songsToPreload.forEach { song ->
                preloadSong(song)
            }
        }
    }

    /**
     * Preload a single song (stream URL + initial audio data).
     */
    fun preloadSong(song: Song) {
        val videoId = extractVideoId(song) ?: return
        
        // Skip if already preloaded or currently preloading
        if (preloadedSongs.contains(videoId) || preloadJobs.containsKey(videoId)) {
            Log.d(TAG, "Song already preloaded/preloading: ${song.title}")
            return
        }

        val job = scope.launch {
            try {
                Log.d(TAG, "🔄 Starting preload for: ${song.title} ($videoId)")
                updateState { copy(preloadingVideoIds = preloadingVideoIds + videoId, currentlyPreloading = videoId) }

                // Step 1: Get stream URL (may be cached)
                val streamUrl = getOrFetchStreamUrl(videoId)
                if (streamUrl == null) {
                    Log.w(TAG, "⚠️ Failed to get stream URL for: ${song.title}")
                    return@launch
                }

                // Step 2: Pre-download first PRELOAD_BYTES of audio
                if (isActive) {
                    predownloadAudio(videoId, streamUrl)
                }

                // Mark as preloaded
                preloadedSongs.add(videoId)
                Log.d(TAG, "✅ Preload complete for: ${song.title}")
                
                updateState { 
                    copy(
                        preloadingVideoIds = preloadingVideoIds - videoId,
                        preloadedVideoIds = preloadedVideoIds + videoId,
                        currentlyPreloading = if (currentlyPreloading == videoId) null else currentlyPreloading
                    )
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Preload cancelled for: ${song.title}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Preload failed for: ${song.title}", e)
            } finally {
                preloadJobs.remove(videoId)
                updateState { 
                    copy(
                        preloadingVideoIds = preloadingVideoIds - videoId,
                        currentlyPreloading = if (currentlyPreloading == videoId) null else currentlyPreloading
                    )
                }
            }
        }

        preloadJobs[videoId] = job
    }

    /**
     * Get a cached stream URL or fetch a new one.
     */
    private suspend fun getOrFetchStreamUrl(videoId: String): String? {
        // Check cache first
        StreamUrlCache.get(videoId)?.let {
            Log.d(TAG, "Using cached stream URL for: $videoId")
            return it
        }

        // Fetch new stream URL
        return withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
            try {
                val url = onlineManager.getStreamUrl(videoId)
                if (url != null) {
                    StreamUrlCache.put(videoId, url)
                }
                url
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch stream URL: ${e.message}")
                null
            }
        }
    }

    /**
     * Pre-download the first PRELOAD_BYTES of audio data into cache.
     */
    private suspend fun predownloadAudio(videoId: String, streamUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDataSourceFactory = StreamCache.createPreloadDataSourceFactory(context)
                val dataSource = cacheDataSourceFactory.createDataSource()

                // Use video ID as cache key
                val dataSpec = DataSpec.Builder()
                    .setUri(streamUrl)
                    .setKey(videoId)
                    .setLength(PRELOAD_BYTES)
                    .build()

                // Use CacheWriter for efficient caching
                val cacheWriter = CacheWriter(
                    dataSource as CacheDataSource,
                    dataSpec,
                    null // No progress listener needed
                ) { _, _, _ -> !isActive } // Cancel check

                cacheWriter.cache()

                val cachedBytes = StreamCache.getCachedBytes(context, videoId)
                Log.d(TAG, "Pre-cached ${cachedBytes / 1024}KB for video: $videoId")

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.w(TAG, "Failed to pre-cache audio: ${e.message}")
                }
            }
        }
    }

    /**
     * Cancel preload jobs for songs no longer in the preload window.
     */
    private fun cancelOutdatedJobs(currentVideoIds: Set<String>) {
        val outdated = preloadJobs.keys.filter { it !in currentVideoIds }
        outdated.forEach { videoId ->
            preloadJobs[videoId]?.cancel()
            preloadJobs.remove(videoId)
            Log.d(TAG, "Cancelled outdated preload for: $videoId")
        }

        // Also remove from preloaded set (they may be evicted from cache soon)
        preloadedSongs.removeAll { it !in currentVideoIds }
    }

    /**
     * Check if a song has been preloaded (has cached data).
     */
    fun isPreloaded(song: Song): Boolean {
        val videoId = extractVideoId(song) ?: return false
        return preloadedSongs.contains(videoId) || StreamCache.isCached(context, videoId)
    }

    /**
     * Get the cached stream URL for a song if available.
     */
    fun getCachedStreamUrl(song: Song): String? {
        val videoId = extractVideoId(song) ?: return null
        return StreamUrlCache.get(videoId)
    }

    /**
     * Check if a song is an online song (YouTube).
     */
    private fun isOnlineSong(song: Song): Boolean {
        return song.id.startsWith("online:") || song.id.startsWith("youtube:")
    }

    /**
     * Extract video ID from song.
     */
    private fun extractVideoId(song: Song): String? {
        return when {
            song.id.startsWith("online:") -> song.id.removePrefix("online:")
            song.id.startsWith("youtube:") -> song.id.removePrefix("youtube:")
            else -> null
        }
    }

    private inline fun updateState(update: PreloadState.() -> PreloadState) {
        _state.value = _state.value.update()
    }

    /**
     * Cancel all preload jobs.
     */
    fun cancelAll() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        _state.value = PreloadState()
    }

    /**
     * Release resources.
     */
    fun release() {
        cancelAll()
        scope.cancel()
        preloadedSongs.clear()
    }
}
