package com.just_for_fun.synctax.core.download

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
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
     * @param context Android Context (required for FFmpeg initialization)
     * @param videoId YouTube video ID
     * @param outputDir Directory to save the downloaded file
     * @param preferredFormat Preferred audio format (null for MP3 with metadata)
     * @param metadataTitle Optional title from yt-dlp (overrides NewPipe title)
     * @param metadataArtist Optional artist from yt-dlp (overrides NewPipe artist)
     * @param metadataAlbum Optional album from yt-dlp
     * @return DownloadResult with success status and file path
     */
    suspend fun downloadAudio(
        context: Context,
        videoId: String,
        outputDir: File,
        preferredFormat: String? = null,
        metadataTitle: String? = null,
        metadataArtist: String? = null,
        metadataAlbum: String? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        var webmFile: File? = null
        var thumbnailFile: File? = null
        
        try {
            Log.d(TAG, "🎵 Starting NewPipe download for video: $videoId")
            
            // Initialize FFmpeg and YoutubeDL if needed
            try {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to initialize FFmpeg/YoutubeDL: ${e.message}")
            }
            
            // Ensure output directory exists
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // Extract video info using NewPipe
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            
            // Get video metadata
            val rawTitle = extractor.name
            val rawArtist = extractor.uploaderName ?: "Unknown Artist"
            
            // Clean up artist name (remove " - Topic", " - Official", etc.)
            val artist = rawArtist
                .replace(Regex(" - Topic$", RegexOption.IGNORE_CASE), "")
                .replace(Regex(" - Official$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("VEVO$", RegexOption.IGNORE_CASE), "")
                .trim()
            
            // Use yt-dlp metadata if provided (priority), otherwise use cleaned NewPipe metadata
            val title = metadataTitle?.takeIf { it.isNotEmpty() } ?: rawTitle
            val finalArtist = metadataArtist?.takeIf { it.isNotEmpty() } ?: artist
            val album = metadataAlbum?.takeIf { it.isNotEmpty() } ?: "YouTube Audio"
            val sanitizedTitle = sanitizeFilename(title)
            val sanitizedArtist = sanitizeFilename(finalArtist)
            
            Log.d(TAG, "📝 Video title: $title")
            Log.d(TAG, "🎤 Raw artist: $rawArtist")
            Log.d(TAG, "🎤 Cleaned artist: $artist")
            Log.d(TAG, "🎤 Final artist (from yt-dlp): $finalArtist")
            Log.d(TAG, "💿 Album (from yt-dlp): $album")
            
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
            
            // Download thumbnail (keep for thumbnail URL reference)
            val thumbnailUrl = extractor.thumbnails.maxByOrNull { it.height }?.url
            if (!thumbnailUrl.isNullOrEmpty()) {
                try {
                    thumbnailFile = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}.jpg")
                    downloadStream(thumbnailUrl, thumbnailFile)
                    Log.d(TAG, "✅ Thumbnail downloaded: ${thumbnailFile.length() / 1024}KB")
                    // Keep thumbnail file for later use (don't delete)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to download thumbnail: ${e.message}")
                    thumbnailFile = null
                }
            }
            
            // Convert WebM to user-friendly audio container (M4A/MP3/FLAC/OPUS) with embedded metadata & cover
            val baseName = "${sanitizedArtist} - ${sanitizedTitle}"
            val convertedFile = convertAudioWithMetadata(
                context = context,
                inputFile = webmFile,
                thumbnailFile = thumbnailFile,
                title = title,
                artist = finalArtist,
                album = album,
                outDir = outputDir,
                baseName = baseName
            )

            val finalFile = if (convertedFile != null && convertedFile.exists() && convertedFile.length() > 1024) {
                Log.d(TAG, "✅ Converted and embedded successfully: ${convertedFile.absolutePath}")
                // Cleanup temp files
                webmFile.delete()
                thumbnailFile?.delete()
                convertedFile
            } else {
                Log.w(TAG, "⚠️ Conversion/embedding failed, keeping original WebM: ${webmFile.absolutePath}")
                webmFile
            }
            
            if (finalFile.exists() && finalFile.length() > 1024) {
                Log.d(TAG, "✅ Download successful: ${finalFile.absolutePath}")
                Log.d(TAG, "📦 Final file size: ${finalFile.length() / 1024 / 1024}MB")
                
                DownloadResult(
                    success = true,
                    filePath = finalFile.absolutePath,
                    message = "Downloaded with embedded metadata and cover",
                    format = finalFile.extension.ifEmpty { "webm" },
                    title = title,
                    artist = finalArtist,
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
    private suspend fun convertAudioWithMetadata(
        context: Context,
        inputFile: File,
        thumbnailFile: File?,
        title: String,
        artist: String,
        album: String,
        outDir: File,
        baseName: String
    ): File? = withContext(Dispatchers.IO) {
        val tries = listOf(
            // M4A (AAC) with cover art
            ConversionTry(
                desc = ".m4a (AAC + cover)",
                output = File(outDir, "$baseName.m4a"),
                build = {
                    val args = mutableListOf(
                        "-y", "-i", inputFile.absolutePath
                    )
                    if (thumbnailFile != null && thumbnailFile.exists()) {
                        args += listOf("-i", thumbnailFile.absolutePath, "-map", "0:a", "-map", "1", "-c:v", "copy", "-disposition:v", "attached_pic")
                    }
                    args += listOf(
                        "-c:a", "aac", "-b:a", "192k",
                        "-metadata", "title=${escapeMetadata(title)}",
                        "-metadata", "artist=${escapeMetadata(artist)}",
                        "-metadata", "album=${escapeMetadata(album)}",
                        "-map_metadata", "-1",
                        it.output.absolutePath
                    )
                    args.toTypedArray()
                }
            ),
            // MP3 (best compatibility)
            ConversionTry(
                desc = ".mp3 (with cover)",
                output = File(outDir, "$baseName.mp3"),
                build = {
                    val args = mutableListOf(
                        "-y", "-i", inputFile.absolutePath
                    )
                    if (thumbnailFile != null && thumbnailFile.exists()) {
                        args += listOf("-i", thumbnailFile.absolutePath, "-map", "0:a", "-map", "1")
                    }
                    args += listOf(
                        "-c:a", "libmp3lame", "-b:a", "192k",
                        "-metadata", "title=${escapeMetadata(title)}",
                        "-metadata", "artist=${escapeMetadata(artist)}",
                        "-metadata", "album=${escapeMetadata(album)}",
                        "-map_metadata", "-1",
                        it.output.absolutePath
                    )
                    args.toTypedArray()
                }
            ),
            // FLAC
            ConversionTry(
                desc = ".flac (with cover)",
                output = File(outDir, "$baseName.flac"),
                build = {
                    val args = mutableListOf(
                        "-y", "-i", inputFile.absolutePath
                    )
                    if (thumbnailFile != null && thumbnailFile.exists()) {
                        args += listOf("-i", thumbnailFile.absolutePath, "-map", "0:a", "-map", "1", "-c:v", "copy")
                    }
                    args += listOf(
                        "-c:a", "flac",
                        "-metadata", "title=${escapeMetadata(title)}",
                        "-metadata", "artist=${escapeMetadata(artist)}",
                        "-metadata", "album=${escapeMetadata(album)}",
                        "-map_metadata", "-1",
                        it.output.absolutePath
                    )
                    args.toTypedArray()
                }
            ),
            // OPUS (no cover support)
            ConversionTry(
                desc = ".opus (no cover)",
                output = File(outDir, "$baseName.opus"),
                build = {
                    arrayOf(
                        "-y", "-i", inputFile.absolutePath,
                        "-c:a", "libopus", "-b:a", "128k",
                        "-metadata", "title=${escapeMetadata(title)}",
                        "-metadata", "artist=${escapeMetadata(artist)}",
                        "-metadata", "album=${escapeMetadata(album)}",
                        "-map_metadata", "-1",
                        it.output.absolutePath
                    )
                }
            ),
            // Fallback: m4a direct copy with metadata only
            ConversionTry(
                desc = ".m4a (direct copy fallback)",
                output = File(outDir, "${baseName}_simple.m4a"),
                build = {
                    arrayOf(
                        "-y", "-i", inputFile.absolutePath,
                        "-c", "copy",
                        "-metadata", "title=${escapeMetadata(title)}",
                        "-metadata", "artist=${escapeMetadata(artist)}",
                        "-metadata", "album=${escapeMetadata(album)}",
                        it.output.absolutePath
                    )
                }
            )
        )

        for (t in tries) {
            val ok = runFfmpeg(context, t)
            if (ok) return@withContext t.output
        }
        null
    }

    private data class ConversionTry(
        val desc: String,
        val output: File,
        val build: (ConversionTry) -> Array<String>
    )

    private fun runFfmpeg(context: Context, t: ConversionTry): Boolean {
        try {
            val args = t.build(t)
            Log.d(TAG, "🔧 FFmpeg try ${t.desc}: ${args.joinToString(" ")}")
            
            // Use direct FFmpeg call instead of reflection
            val ffmpeg = FFmpeg.getInstance()
            // Ensure initialized (safe to call multiple times)
            try { ffmpeg.init(context) } catch (e: Exception) { /* ignore if already initialized */ }
            
            val rc = ffmpeg.execute(args)
            
            val ok = rc == 0 && t.output.exists() && t.output.length() > 1024
            if (!ok) {
                // cleanup partial output
                if (t.output.exists()) t.output.delete()
            }
            return ok
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ FFmpeg ${t.desc} failed: ${e.message}")
            if (t.output.exists()) t.output.delete()
            return false
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
     * Parse command string into array, handling quoted strings properly.
     * FFmpeg expects arguments like: ["-y", "-i", "input.webm", "-i", "thumb.jpg", ...]
     */
    private fun parseCommand(command: String): Array<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < command.length) {
            val char = command[i]
            
            when {
                char == '"' && (i == 0 || command[i - 1] != '\\') -> {
                    inQuotes = !inQuotes
                }
                char == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        
        return result.toTypedArray()
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
                        
                        // Log progress every 10% with green color indicators
                        if (contentLength > 0) {
                            val percent = ((downloaded * 100) / contentLength).toInt()
                            if (percent >= lastLoggedPercent + 10) {
                                val progressBar = getProgressBar(percent)
                                Log.d(TAG, "⬇️ Download progress: $percent% $progressBar")
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
    
    /**
     * Generate a green progress bar based on percentage.
     */
    private fun getProgressBar(percent: Int): String {
        val filled = percent / 10 // Each block represents 10%
        val empty = 10 - filled
        val greenBlock = "🟩" // Green square
        val grayBlock = "⬜" // Gray square
        return greenBlock.repeat(filled) + grayBlock.repeat(empty)
    }
}
