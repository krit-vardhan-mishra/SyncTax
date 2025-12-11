package com.just_for_fun.synctax.core.player

import android.content.Context
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
    val percent: Int = 0
)

/**
 * Downloads a remote progressive audio stream in 30s chunks and writes to a temp file
 * so the player can start playback as data arrives. When download completes the tmp file
 * is renamed to final cached file.
 */
class ChunkedStreamManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private var chunkRequest = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private val _state = MutableStateFlow(ChunkDownloadState())
    val state: StateFlow<ChunkDownloadState> = _state.asStateFlow()

    private var currentTmpFile: File? = null
    private var currentFinalFile: File? = null
    private var lastAvgSpeedBps: Double = 0.0
    private var lastChunkBytes: Long = 0L
    private var allowTmpDeletion: Boolean = true  // Only delete tmp when safe

    fun stopAndCleanup(removeFinalCache: Boolean = true) {
        downloadJob?.cancel()
        downloadJob = null
        // Only delete tmp file if allowed (not currently being played)
        if (allowTmpDeletion) {
            currentTmpFile?.let { if (it.exists()) it.delete() }
        }
        currentTmpFile = null
        if (removeFinalCache) {
            currentFinalFile?.let { if (it.exists()) it.delete() }
        }
        currentFinalFile = null
        _state.value = ChunkDownloadState()
        try {
            chunkRequest.close()
        } catch (_: Exception) { }
        chunkRequest = kotlinx.coroutines.channels.Channel(kotlinx.coroutines.channels.Channel.CONFLATED)
        allowTmpDeletion = true  // Reset for next stream
    }
    
    /**
     * Clean up only the tmp file when it's safe (download complete and player no longer needs it).
     * This should be called when switching songs or stopping playback.
     */
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
     * Start downloading stream in chunks. Returns the temporary file that will be growing.
     * If a complete cached file already exists, it returns that instead.
     */
    fun startStreaming(videoId: String, streamUrl: String, durationMs: Long, chunkSec: Int = 30): File {
        stopAndCleanup()

        val cacheDir = context.cacheDir
        val tmp = File(cacheDir, "stream_${videoId}.tmp")
        val final = File(cacheDir, "stream_${videoId}.cache")
        currentTmpFile = tmp
        currentFinalFile = final

        // If final cache file already exists and is valid, use it directly
        if (final.exists() && final.length() > 0) {
            _state.value = ChunkDownloadState(
                bufferedBytes = final.length(),
                totalBytes = final.length(),
                bufferedSeconds = (durationMs / 1000),
                isComplete = true,
                percent = 100
            )
            // Return the cached file for playback
            return final
        }

        // ensure empty tmp file
        if (tmp.exists()) tmp.delete()
        tmp.parentFile?.mkdirs()

        downloadJob = scope.launch {
            try {
                // 1) Try HEAD to get total bytes
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
                } catch (_: Exception) {
                    // ignore
                }

                val durationSec = (durationMs / 1000.0).coerceAtLeast(1.0)
                val chunkSeconds = chunkSec

                // Estimate bytes per second if totalBytes known
                val bytesPerSec = if (totalBytes > 0) (totalBytes / durationSec) else 10000.0
                var chunkBytes = ceil(bytesPerSec * chunkSeconds).toLong().coerceAtLeast(32_000L)
                var avgSpeedBps = 0.0

                var downloaded: Long = 0L
                var chunkIndex = 0
                var shouldBreakOuter = false

                // If totalBytes unknown, we will fetch sequentially until remote ends
                while (isActive) {
                    val startByte = downloaded
                    val endByte = if (totalBytes > 0) {
                        val tentative = (startByte + chunkBytes - 1).coerceAtMost(totalBytes - 1)
                        tentative
                    } else {
                        startByte + chunkBytes - 1
                    }

                    val conn = URL(streamUrl).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.instanceFollowRedirects = true
                    // Range header
                    conn.setRequestProperty("Range", "bytes=$startByte-$endByte")
                    conn.connect()

                    val code = conn.responseCode
                    if (code in 200..299 || code == 206) {
                        val input = conn.inputStream
                        // Write to tmp file at correct position
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
                                // Update state
                                val bufferedSec = if (bytesPerSec > 0) (downloaded / bytesPerSec).toLong() else 0L
                                val percent = if (totalBytes > 0) ((downloaded * 100 / totalBytes).toInt()) else 0
                                _state.value = ChunkDownloadState(
                                    bufferedBytes = downloaded,
                                    totalBytes = totalBytes,
                                    bufferedSeconds = bufferedSec,
                                    isComplete = (totalBytes > 0 && downloaded >= totalBytes),
                                    percent = percent
                                )
                                // If we've read at least the requested chunk size, return from use to continue outer loop
                                if (chunkDownloaded >= chunkBytes) return@use
                                if (!isActive) return@use
                            }
                            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            val speedBps = (chunkDownloaded * 1000) / elapsed
                                        // adjust chunkBytes based on speed (simple heuristic)
                                        // Maintain a rolling average for download speed and scale chunk size accordingly.
                                        // Smoothing avoids wild changes when speeds fluctuate.
                                                    avgSpeedBps = if (avgSpeedBps == 0.0) speedBps.toDouble() else (avgSpeedBps * 0.75 + speedBps * 0.25)
                                        // Target chunk length in seconds should adapt to speed: faster => bigger chunks
                                        val minChunkSec = 10.0
                                        val maxChunkSec = 180.0
                                        val suggestedChunkSec = (chunkSeconds * (avgSpeedBps / 64_000.0)).coerceIn(minChunkSec, maxChunkSec)
                                        // Convert to bytes and constrain to sensible min/max boundaries
                                        val suggestedChunkBytes = (avgSpeedBps * suggestedChunkSec).toLong().coerceIn(32_000L, 10_000_000L)
                                        chunkBytes = suggestedChunkBytes
                                        lastAvgSpeedBps = avgSpeedBps
                                        lastChunkBytes = chunkBytes
                            // faster -> prefetch more next iteration
                        }

                        conn.disconnect()

                        // If we've reached totalBytes then finish
                        if (totalBytes > 0 && downloaded >= totalBytes) {
                            // DO NOT rename yet - let the tmp file remain until playback switches
                            // Copy to final cache file so it's available for future plays
                            if (!final.exists()) {
                                tmp.copyTo(final, overwrite = false)
                            }
                            _state.value = _state.value.copy(isComplete = true, totalBytes = totalBytes, percent = 100)
                            shouldBreakOuter = true
                        }

                        // If server returned less than requested and total unknown, assume EOF
                        if (totalBytes <= 0 && conn.contentLengthLong <= 0) {
                            // No more data
                            // DO NOT rename yet - copy to final cache file instead
                            if (!final.exists()) {
                                tmp.copyTo(final, overwrite = false)
                            }
                            _state.value = _state.value.copy(isComplete = true, totalBytes = downloaded, percent = 100)
                            shouldBreakOuter = true
                        }

                        chunkIndex++
                        delay(50) // small yield before next chunk
                        if (shouldBreakOuter) break
                    } else {
                        conn.disconnect()
                        // If server refused range, try full download once
                        if (code == 416 || code == 403) {
                            // fallback: full GET into tmp
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
                                    val bufferedSec = if (bytesPerSec > 0) (downloaded / bytesPerSec).toLong() else 0L
                                    _state.value = ChunkDownloadState(bufferedBytes = downloaded, totalBytes = totalBytes, bufferedSeconds = bufferedSec, isComplete = false)
                                    if (!isActive) return@use
                                }
                                fallbackConn.disconnect()
                                // Copy to final cache instead of rename to keep tmp active
                                if (!final.exists()) {
                                    tmp.copyTo(final, overwrite = false)
                                }
                                _state.value = _state.value.copy(isComplete = true, totalBytes = downloaded, percent = 100)
                                shouldBreakOuter = true
                                return@use
                            }
                        } else {
                            // other errors: stop
                            _state.value = _state.value.copy(isComplete = false)
                            break
                        }
                    }

                    // Wait for explicit request before starting the next chunk
                    if (!isActive) break
                    try {
                        chunkRequest.receive()
                    } catch (_: CancellationException) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                // cancelled - ignore
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return tmp
    }

    /** Request next chunk to be downloaded. Useful to coordinate downloads with player progress. */
    fun requestNextChunk(count: Int = 1) {
        repeat(count) {
            chunkRequest.trySend(Unit)
        }
    }

    /** Returns how many chunks to prefetch based on measured average speed. */
    fun suggestedPrefetchCount(): Int {
        // Basic heuristic: scale from 1..5 based on avg speed.
        val base = if (lastAvgSpeedBps <= 0.0) 1.0 else (lastAvgSpeedBps / 64_000.0)
        val clamped = base.coerceIn(1.0, 5.0)
        return clamped.toInt()
    }
}
