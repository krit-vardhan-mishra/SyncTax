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
 * Downloads as WebM, then converts to MP3 with metadata and album art embedded.
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
        val format: String = "unknown",
        val title: String = "",
        val artist: String = "",
        val thumbnailUrl: String = ""
    )
    
    /**
     * Download audio for a YouTube video using NewPipe, then convert to MP3 with metadata.
     * @param videoId YouTube video ID
     * @param outputDir Directory to save the downloaded file
     * @param preferredFormat Preferred audio format (null for MP3 with metadata)
     * @return DownloadResult with success status and file path
     */
    suspend fun downloadAudio(
        videoId: String,
        outputDir: File,
        preferredFormat: String? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        var webmFile: File? = null
        var thumbnailFile: File? = null
        
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
            
            // Get video metadata
            val title = extractor.name
            val artist = extractor.uploaderName ?: "Unknown Artist"
            val sanitizedTitle = sanitizeFilename(title)
            val sanitizedArtist = sanitizeFilename(artist)
            
            Log.d(TAG, "📝 Video title: $title")
            Log.d(TAG, "🎤 Artist: $artist")
            
            // Get best audio stream
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                return@withContext DownloadResult(
                    success = false,
                    message = "No audio streams available for this video"
                )
            }
            
            // Select best audio stream (prefer Opus/AAC for quality)
            val selectedStream = selectBestAudioStream(audioStreams, null)
            val streamUrl = selectedStream.content
            
            Log.d(TAG, "🎧 Selected stream: ${selectedStream.format?.name ?: "unknown"}")
            Log.d(TAG, "🎧 Bitrate: ${selectedStream.averageBitrate} kbps")
            
            // Download audio stream as WebM
            val webmFileName = "${sanitizedArtist} - ${sanitizedTitle}.webm"
            webmFile = File(outputDir, webmFileName)
            downloadStream(streamUrl, webmFile)
            Log.d(TAG, "✅ WebM download complete: ${webmFile.length() / 1024 / 1024}MB")
            
            // Download thumbnail
            val thumbnailUrl = extractor.thumbnails.maxByOrNull { it.height }?.url
            if (!thumbnailUrl.isNullOrEmpty()) {
                try {
                    thumbnailFile = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}.jpg")
                    downloadStream(thumbnailUrl, thumbnailFile)
                    Log.d(TAG, "✅ Thumbnail downloaded: ${thumbnailFile.length() / 1024}KB")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to download thumbnail: ${e.message}")
                    thumbnailFile = null
                }
            }
            
            // Try to add metadata using FFmpeg (optional, won't fail if FFmpeg unavailable)
            val finalFile = if (thumbnailFile != null && thumbnailFile.exists()) {
                val webmWithMetadata = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}_final.webm")
                val metadataAdded = addMetadataToWebm(
                    webmFile,
                    webmWithMetadata,
                    thumbnailFile,
                    title,
                    artist,
                    "YouTube Audio"
                )
                
                if (metadataAdded && webmWithMetadata.exists() && webmWithMetadata.length() > 1024) {
                    // Delete original and rename
                    webmFile.delete()
                    webmWithMetadata.renameTo(webmFile)
                    thumbnailFile.delete()
                    Log.d(TAG, "✅ Metadata embedded successfully")
                } else {
                    // Keep original file even if metadata embedding failed
                    webmWithMetadata.delete()
                    thumbnailFile?.delete()
                    Log.w(TAG, "⚠️ Metadata embedding failed, keeping original WebM")
                }
                webmFile
            } else {
                Log.w(TAG, "⚠️ No thumbnail available, keeping WebM without cover art")
                webmFile
            }
            
            if (finalFile.exists() && finalFile.length() > 1024) {
                Log.d(TAG, "✅ Download successful: ${finalFile.absolutePath}")
                Log.d(TAG, "📦 Final file size: ${finalFile.length() / 1024 / 1024}MB")
                
                DownloadResult(
                    success = true,
                    filePath = finalFile.absolutePath,
                    message = "Downloaded as WebM with Opus codec",
                    format = "webm",
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl ?: ""
                )
            } else {
                return@withContext DownloadResult(
                    success = false,
                    message = "Download verification failed"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download failed", e)
            
            // Cleanup on failure
            webmFile?.delete()
            thumbnailFile?.delete()
            
            DownloadResult(
                success = false,
                message = "Download failed: ${e.message}"
            )
        }
    }
    
    /**
     * Add metadata and thumbnail to WebM file using FFmpeg.
     * This is optional - if it fails, the original WebM file is still usable.
     */
    private suspend fun addMetadataToWebm(
        inputFile: File,
        outputFile: File,
        thumbnailFile: File?,
        title: String,
        artist: String,
        album: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try using youtubedl-android FFmpeg library
            try {
                // Build command string for youtubedl-android FFmpeg
                val command = buildString {
                    append("-y -i \"${inputFile.absolutePath}\"")
                    
                    if (thumbnailFile != null && thumbnailFile.exists()) {
                        append(" -i \"${thumbnailFile.absolutePath}\"")
                        append(" -map 0 -map 1")
                        append(" -disposition:v:0 attached_pic")
                    }
                    
                    append(" -c copy") // Copy streams without re-encoding
                    append(" -metadata title=\"${escapeMetadata(title)}\"")
                    append(" -metadata artist=\"${escapeMetadata(artist)}\"")
                    append(" -metadata album=\"${escapeMetadata(album)}\"")
                    append(" \"${outputFile.absolutePath}\"")
                }
                
                Log.d(TAG, "🔧 Adding metadata with FFmpeg...")
                Log.d(TAG, "🔧 Command: ffmpeg $command")
                
                // Use reflection to call FFmpeg.execute from youtubedl-android library
                val ffmpegClass = Class.forName("com.yausername.ffmpeg.FFmpeg")
                val instanceMethod = ffmpegClass.getMethod("getInstance")
                val ffmpegInstance = instanceMethod.invoke(null)
                val executeMethod = ffmpegClass.getMethod("execute", Array<String>::class.java)
                
                // Split command into array
                val commandArray = command.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                    .map { it.replace("\"", "") }
                    .toTypedArray()
                
                executeMethod.invoke(ffmpegInstance, commandArray)
                
                // Check if output file was created successfully
                if (outputFile.exists() && outputFile.length() > 1024) {
                    Log.d(TAG, "✅ Metadata added successfully")
                    return@withContext true
                } else {
                    Log.w(TAG, "⚠️ FFmpeg output file validation failed")
                    return@withContext false
                }
                
            } catch (reflectionError: Exception) {
                Log.w(TAG, "⚠️ FFmpeg library not available: ${reflectionError.message}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Metadata embedding failed: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Escape special characters in metadata values.
     */
    private fun escapeMetadata(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
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
