package com.just_for_fun.synctax.core.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.ceil

data class ChunkDownloadState(
    val bufferedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val bufferedSeconds: Long = 0L,
    val isComplete: Boolean = false,
    val percent: Int = 0,
    val currentChunkStart: Long = 0L,  // For seek support
    val isLongContent: Boolean = false  // Content > 30 minutes
)

/**
 * Downloads a remote progressive audio stream in chunks and writes to a temp file
 * so the player can start playback as data arrives. 
 * 
 * Enhanced for long content (10+ hour videos):
 * - Adaptive chunk sizing based on content length and network speed
 * - Efficient seeking support for long content
 * - Only caches short content (< 30 min) to save storage
 * - Streaming-first approach for long content like YouTube Music/Spotify
 */
class ChunkedStreamManager(private val context: Context) {

    companion object {
        private const val TAG = "ChunkedStreamManager"
        private const val MAX_CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes - don't cache longer content
        private const val LONG_CONTENT_THRESHOLD_MS = 20 * 60 * 1000L // 20 minutes = long content
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private var chunkRequest = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private val _state = MutableStateFlow(ChunkDownloadState())
    val state: StateFlow<ChunkDownloadState> = _state.asStateFlow()

    private var currentTmpFile: File? = null
    private var currentFinalFile: File? = null
    private var lastAvgSpeedBps: Double = 0.0
    private var lastChunkBytes: Long = 0L
    private var allowTmpDeletion: Boolean = true
    
    // Streaming state for seek support
    private var currentStreamUrl: String? = null
    private var currentVideoId: String? = null
    private var currentDurationMs: Long = 0L
    private var bytesPerSecond: Double = 0.0

    fun stopAndCleanup(removeFinalCache: Boolean = true) {
        downloadJob?.cancel()
        downloadJob = null
        if (allowTmpDeletion) {
            currentTmpFile?.let { if (it.exists()) it.delete() }
        }
        currentTmpFile = null
        if (removeFinalCache) {
            currentFinalFile?.let { if (it.exists()) it.delete() }
        }
        currentFinalFile = null
        _state.value = ChunkDownloadState()
        currentStreamUrl = null
        currentVideoId = null
        currentDurationMs = 0L
        bytesPerSecond = 0.0
        try {
            chunkRequest.close()
        } catch (_: Exception) { }
        chunkRequest = kotlinx.coroutines.channels.Channel(kotlinx.coroutines.channels.Channel.CONFLATED)
        allowTmpDeletion = true
    }
    
    fun cleanupTmpFile() {
        if (_state.value.isComplete) {
            currentTmpFile?.let { 
                if (it.exists()) {
                    it.delete()
                }
            }
            currentTmpFile = null
        }
    }

    /**
     * Check if content is considered "long" (> 20 minutes)
     * Long content uses streaming-only mode without full caching
     */
    fun isLongContent(): Boolean = _state.value.isLongContent

    /**
     * Start downloading stream in chunks. Returns the temporary file that will be growing.
     * For long content (> 30 min), we don't cache the final file to save storage.
     */
    fun startStreaming(videoId: String, streamUrl: String, durationMs: Long, chunkSec: Int = 30): File {
        stopAndCleanup()
        
        // Store for seek support
        currentStreamUrl = streamUrl
        currentVideoId = videoId
        currentDurationMs = durationMs
        
        val isLongContent = durationMs > LONG_CONTENT_THRESHOLD_MS
        val shouldCache = durationMs <= MAX_CACHE_DURATION_MS
        
        Log.d(TAG, "Starting stream for $videoId, duration=${durationMs/1000}s, isLong=$isLongContent, willCache=$shouldCache")

        val cacheDir = context.cacheDir
        val tmp = File(cacheDir, "stream_${videoId}.tmp")
        val final = if (shouldCache) File(cacheDir, "stream_${videoId}.cache") else null
        currentTmpFile = tmp
        currentFinalFile = final

        // If final cache file already exists and is valid (only for short content)
        if (final != null && final.exists() && final.length() > 0) {
            _state.value = ChunkDownloadState(
                bufferedBytes = final.length(),
                totalBytes = final.length(),
                bufferedSeconds = (durationMs / 1000),
                isComplete = true,
                percent = 100,
                isLongContent = isLongContent
            )
            return final
        }

        if (tmp.exists()) tmp.delete()
        tmp.parentFile?.mkdirs()

        // Adaptive chunk size based on content length
        val adaptiveChunkSec = when {
            durationMs > 3600 * 1000L -> 60  // > 1 hour: 60 second chunks
            durationMs > 1800 * 1000L -> 45  // > 30 min: 45 second chunks
            durationMs > 600 * 1000L -> 30   // > 10 min: 30 second chunks
            else -> 20                         // Short content: 20 second chunks
        }

        downloadJob = scope.launch {
            try {
                var totalBytes: Long = -1L
                try {
                    val headConn = URL(streamUrl).openConnection() as HttpURLConnection
                    headConn.requestMethod = "HEAD"
                    headConn.connectTimeout = 10000
                    headConn.readTimeout = 10000
                    headConn.instanceFollowRedirects = true
                    headConn.connect()
                    val cl = headConn.getHeaderFieldLong("Content-Length", -1L)
                    if (cl > 0) totalBytes = cl
                    headConn.disconnect()
                } catch (_: Exception) { }

                val durationSec = (durationMs / 1000.0).coerceAtLeast(1.0)
                bytesPerSecond = if (totalBytes > 0) (totalBytes / durationSec) else 10000.0
                var chunkBytes = ceil(bytesPerSecond * adaptiveChunkSec).toLong().coerceAtLeast(32_000L)
                var avgSpeedBps = 0.0

                var downloaded: Long = 0L
                var chunkIndex = 0
                var shouldBreakOuter = false

                while (isActive) {
                    val startByte = downloaded
                    val endByte = if (totalBytes > 0) {
                        (startByte + chunkBytes - 1).coerceAtMost(totalBytes - 1)
                    } else {
                        startByte + chunkBytes - 1
                    }

                    val conn = URL(streamUrl).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.instanceFollowRedirects = true
                    conn.setRequestProperty("Range", "bytes=$startByte-$endByte")
                    conn.connect()

                    val code = conn.responseCode
                    if (code in 200..299 || code == 206) {
                        val input = conn.inputStream
                        RandomAccessFile(tmp, "rw").use { raf ->
                            raf.seek(startByte)
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            var chunkDownloaded: Long = 0L
                            val startTime = System.currentTimeMillis()
                            while (input.read(buffer).also { read = it } != -1) {
                                raf.write(buffer, 0, read)
                                downloaded += read
                                chunkDownloaded += read
                                val bufferedSec = if (bytesPerSecond > 0) (downloaded / bytesPerSecond).toLong() else 0L
                                val percent = if (totalBytes > 0) ((downloaded * 100 / totalBytes).toInt()) else 0
                                _state.value = ChunkDownloadState(
                                    bufferedBytes = downloaded,
                                    totalBytes = totalBytes,
                                    bufferedSeconds = bufferedSec,
                                    isComplete = (totalBytes > 0 && downloaded >= totalBytes),
                                    percent = percent,
                                    currentChunkStart = startByte,
                                    isLongContent = isLongContent
                                )
                                if (chunkDownloaded >= chunkBytes) return@use
                                if (!isActive) return@use
                            }
                            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            val speedBps = (chunkDownloaded * 1000) / elapsed
                            avgSpeedBps = if (avgSpeedBps == 0.0) speedBps.toDouble() else (avgSpeedBps * 0.75 + speedBps * 0.25)
                            
                            // For long content, use larger chunks to reduce overhead
                            val minChunkSec = if (isLongContent) 30.0 else 10.0
                            val maxChunkSec = if (isLongContent) 300.0 else 180.0 // Up to 5 min chunks for very long content
                            val suggestedChunkSec = (adaptiveChunkSec * (avgSpeedBps / 64_000.0)).coerceIn(minChunkSec, maxChunkSec)
                            val suggestedChunkBytes = (avgSpeedBps * suggestedChunkSec).toLong().coerceIn(32_000L, 20_000_000L)
                            chunkBytes = suggestedChunkBytes
                            lastAvgSpeedBps = avgSpeedBps
                            lastChunkBytes = chunkBytes
                        }

                        conn.disconnect()

                        if (totalBytes > 0 && downloaded >= totalBytes) {
                            // Only cache short content (< 30 min)
                            if (shouldCache && final != null && !final.exists()) {
                                tmp.copyTo(final, overwrite = false)
                                Log.d(TAG, "Cached complete file for short content: $videoId")
                            } else if (!shouldCache) {
                                Log.d(TAG, "Skipping cache for long content (${durationMs/60000} min): $videoId")
                            }
                            _state.value = _state.value.copy(isComplete = true, totalBytes = totalBytes, percent = 100)
                            shouldBreakOuter = true
                        }

                        if (totalBytes <= 0 && conn.contentLengthLong <= 0) {
                            if (shouldCache && final != null && !final.exists()) {
                                tmp.copyTo(final, overwrite = false)
                            }
                            _state.value = _state.value.copy(isComplete = true, totalBytes = downloaded, percent = 100)
                            shouldBreakOuter = true
                        }

                        chunkIndex++
                        delay(50)
                        if (shouldBreakOuter) break
                    } else {
                        conn.disconnect()
                        if (code == 416 || code == 403) {
                            // Fallback to full download (only for short content)
                            if (!isLongContent) {
                                val fallbackConn = URL(streamUrl).openConnection() as HttpURLConnection
                                fallbackConn.requestMethod = "GET"
                                fallbackConn.connectTimeout = 15000
                                fallbackConn.readTimeout = 60000
                                fallbackConn.connect()
                                RandomAccessFile(tmp, "rw").use { raf ->
                                    raf.seek(0)
                                    val input = fallbackConn.inputStream
                                    val buffer = ByteArray(16 * 1024)
                                    var read: Int
                                    var totalRead = 0L
                                    while (input.read(buffer).also { read = it } != -1) {
                                        raf.write(buffer, 0, read)
                                        totalRead += read
                                        downloaded += read
                                        val bufferedSec = if (bytesPerSecond > 0) (downloaded / bytesPerSecond).toLong() else 0L
                                        _state.value = ChunkDownloadState(
                                            bufferedBytes = downloaded, 
                                            totalBytes = totalBytes, 
                                            bufferedSeconds = bufferedSec, 
                                            isComplete = false,
                                            isLongContent = isLongContent
                                        )
                                        if (!isActive) return@use
                                    }
                                    fallbackConn.disconnect()
                                    if (shouldCache && final != null && !final.exists()) {
                                        tmp.copyTo(final, overwrite = false)
                                    }
                                    _state.value = _state.value.copy(isComplete = true, totalBytes = downloaded, percent = 100)
                                    shouldBreakOuter = true
                                    return@use
                                }
                            } else {
                                Log.e(TAG, "Range request failed for long content, cannot fallback: $videoId")
                                _state.value = _state.value.copy(isComplete = false)
                                break
                            }
                        } else {
                            _state.value = _state.value.copy(isComplete = false)
                            break
                        }
                    }

                    if (!isActive) break
                    try {
                        chunkRequest.receive()
                    } catch (_: CancellationException) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                // cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Streaming error for $videoId", e)
            }
        }

        return tmp
    }

    /**
     * Request to seek to a specific position in the stream.
     * For long content, this fetches the needed bytes around the target position.
     * Returns the byte offset to seek to in the file.
     */
    suspend fun seekToPosition(targetMs: Long): Long {
        if (currentStreamUrl == null || currentDurationMs <= 0) return 0L
        
        val targetBytes = if (bytesPerSecond > 0) {
            ((targetMs / 1000.0) * bytesPerSecond).toLong()
        } else {
            0L
        }
        
        val currentBuffered = _state.value.bufferedBytes
        
        // If target is within buffered range, no need to fetch more
        if (targetBytes < currentBuffered) {
            Log.d(TAG, "Seek target $targetMs ms is within buffered range")
            return targetBytes
        }
        
        // For long content, we need to fetch the chunk containing the target position
        if (_state.value.isLongContent) {
            Log.d(TAG, "Seeking in long content to $targetMs ms (byte ~$targetBytes)")
            // Request additional chunks to buffer ahead of seek position
            requestNextChunk(3)
        }
        
        return targetBytes
    }

    fun requestNextChunk(count: Int = 1) {
        repeat(count) {
            chunkRequest.trySend(Unit)
        }
    }

    fun suggestedPrefetchCount(): Int {
        // More aggressive prefetching for long content
        val basePrefetch = if (lastAvgSpeedBps <= 0.0) 1.0 else (lastAvgSpeedBps / 64_000.0)
        val multiplier = if (_state.value.isLongContent) 1.5 else 1.0
        val clamped = (basePrefetch * multiplier).coerceIn(1.0, 8.0)
        return clamped.toInt()
    }
}
