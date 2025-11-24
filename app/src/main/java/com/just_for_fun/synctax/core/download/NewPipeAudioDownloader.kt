package com.just_for_fun.synctax.core.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads YouTube audio using NewPipe's stream extraction.
 * This bypasses yt-dlp's issues with bot detection and signature decryption.
 */
object NewPipeAudioDownloader {
    private const val TAG = "NewPipeAudioDownloader"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    data class DownloadResult(
        val success: Boolean,
        val filePath: String? = null,
        val message: String = "",
        val format: String = "unknown"
    )
    
    /**
     * Download audio for a YouTube video using NewPipe.
     * @param videoId YouTube video ID
     * @param outputDir Directory to save the downloaded file
     * @param preferredFormat Preferred audio format (null for best available)
     * @return DownloadResult with success status and file path
     */
    suspend fun downloadAudio(
        videoId: String,
        outputDir: File,
        preferredFormat: String? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Starting NewPipe download for video: $videoId")
            
            // Ensure output directory exists
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // Extract video info using NewPipe
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            
            // Get video title for filename
            val title = sanitizeFilename(extractor.name)
            Log.d(TAG, "📝 Video title: $title")
            
            // Get best audio stream
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                return@withContext DownloadResult(
                    success = false,
                    message = "No audio streams available for this video"
                )
            }
            
            // Select best audio stream based on preference
            val selectedStream = selectBestAudioStream(audioStreams, preferredFormat)
            val streamUrl = selectedStream.content
            val format = selectedStream.format?.suffix ?: "audio"
            
            Log.d(TAG, "🎧 Selected stream: ${selectedStream.format?.name ?: "unknown"}")
            Log.d(TAG, "🎧 Bitrate: ${selectedStream.averageBitrate} kbps")
            Log.d(TAG, "🎧 Format: $format")
            
            // Download the stream
            val outputFile = File(outputDir, "$title.$format")
            downloadStream(streamUrl, outputFile)
            
            Log.d(TAG, "✅ Download successful: ${outputFile.absolutePath}")
            
            DownloadResult(
                success = true,
                filePath = outputFile.absolutePath,
                message = "Downloaded successfully",
                format = format
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download failed", e)
            DownloadResult(
                success = false,
                message = "Download failed: ${e.message}"
            )
        }
    }
    
    /**
     * Select the best audio stream based on preferences.
     * Priority: Opus > AAC (m4a) > WebM > others, with highest bitrate
     */
    private fun selectBestAudioStream(
        streams: List<AudioStream>,
        preferredFormat: String?
    ): AudioStream {
        return when {
            // If specific format requested, find it
            preferredFormat != null -> {
                streams.find { it.format?.suffix?.equals(preferredFormat, ignoreCase = true) == true }
                    ?: streams.maxByOrNull { it.averageBitrate }!!
            }
            // Prefer Opus for best quality/size ratio
            else -> {
                val opusStreams = streams.filter { 
                    it.format?.name?.contains("opus", ignoreCase = true) == true 
                }
                if (opusStreams.isNotEmpty()) {
                    opusStreams.maxByOrNull { it.averageBitrate }!!
                } else {
                    // Fallback to highest bitrate
                    streams.maxByOrNull { it.averageBitrate }!!
                }
            }
        }
    }
    
    /**
     * Download stream to file with progress logging.
     */
    private suspend fun downloadStream(url: String, outputFile: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            
            val contentLength = response.body?.contentLength() ?: -1
            Log.d(TAG, "📦 Content length: ${contentLength / 1024 / 1024}MB")
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    var lastLoggedPercent = 0
                    
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        
                        // Log progress every 10%
                        if (contentLength > 0) {
                            val percent = ((downloaded * 100) / contentLength).toInt()
                            if (percent >= lastLoggedPercent + 10) {
                                Log.d(TAG, "⬇️ Download progress: $percent%")
                                lastLoggedPercent = percent
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Sanitize filename to remove invalid characters.
     */
    private fun sanitizeFilename(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(100) // Limit filename length
    }
}
