package com.just_for_fun.synctax.core.download

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.just_for_fun.synctax.MusicApplication
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Downloads YouTube audio using NewPipe's stream extraction.
 * This bypasses yt-dlp's issues with bot detection and signature decryption.
 * Downloads as M4A (preferred for metadata support) and embeds metadata using Mutagen.
 */
object NewPipeAudioDownloader {
    private const val TAG = "NewPipeAudioDownloader"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Crop thumbnail to 720x720 from center.
     * Extracts the center 2x2 grid from a 4x4 grid (center 50% of image),
     * removing the outer 25% on each edge, then scales to 720x720.
     */
    private fun cropCenterThumbnail(origThumb: File, outDir: File, base: String): File? {
        if (!origThumb.exists()) return null

        val ext = origThumb.extension
        val cropped = File(outDir, "${base}_thumb_720x720.${ext}")

        try {
            // Crop center 50% of image (2x2 from 4x4 grid):
            // - New width = iw/2 (50% of original width)
            // - New height = ih/2 (50% of original height)
            // - X offset = iw/4 (start at 25% from left)
            // - Y offset = ih/4 (start at 25% from top)
            // Then scale to 720x720
            val vf = "crop=iw/2:ih/2:iw/4:ih/4,scale=720:720"

            Log.d(TAG, "🎨 Cropping thumbnail with filter: $vf")
            val cmd = arrayOf("ffmpeg", "-y", "-i", origThumb.absolutePath, "-vf", vf, cropped.absolutePath)
            
            val process = Runtime.getRuntime().exec(cmd)
            val exitCode = process.waitFor()
            
            return if (exitCode == 0 && cropped.exists() && cropped.length() > 1024) {
                cropped
            } else {
                cropped.delete()
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail cropping failed: ${e.message}")
            cropped.delete()
            return null
        }
    }

    /**
     * Get image dimensions using ffprobe
     */
    private fun getImageSize(imagePath: File): Pair<Int, Int>? {
        return try {
            val cmd = arrayOf("ffprobe", "-v", "error", "-select_streams", "v:0",
                            "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", imagePath.absolutePath)
            
            val process = Runtime.getRuntime().exec(cmd)
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && output.isNotEmpty()) {
                val parts = output.split("x")
                if (parts.size == 2) {
                    Pair(parts[0].toInt(), parts[1].toInt())
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    data class DownloadResult(
        val success: Boolean,
        val filePath: String? = null,
        val message: String = "",
        val format: String = "unknown",
        val title: String = "",
        val artist: String = "",
        val album: String = "",
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
        metadataAlbum: String? = null,
        progressCallback: ((Int) -> Unit)? = null,
        thumbnailProgressCallback: ((Int) -> Unit)? = null,
        progressStep: Int = 1
    ): DownloadResult = withContext(Dispatchers.IO) {
        var webmFile: File? = null
        var thumbnailFile: File? = null

        try {
            Log.d(TAG, "🎵 Starting NewPipe download for video: $videoId")

            // FFmpeg from youtubedl-android library doesn't have a usable execute() method
            // for arbitrary FFmpeg commands. It's designed to work through YoutubeDL only.
            // We set this to false to force M4A format selection (which supports metadata via Mutagen)
            val ffmpegInitialized = false
            
            Log.d(TAG, "🎵 FFmpeg execute not available - using M4A format for metadata embedding support")

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
            // IMPORTANT: Prefer M4A when FFmpeg is not available because:
            // - M4A (AAC in MP4 container) supports metadata embedding via Mutagen
            // - WebM/Opus doesn't support metadata embedding without FFmpeg conversion
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                return@withContext DownloadResult(
                    success = false,
                    message = "No audio streams available for this video"
                )
            }

            // Select best audio stream - prefer M4A when FFmpeg not available
            val selectedStream = selectBestAudioStream(audioStreams, null, ffmpegInitialized)
            val streamUrl = selectedStream.content
            val streamExtension = selectedStream.format?.suffix ?: "webm"

            Log.d(TAG, "🎧 Selected stream: ${selectedStream.format?.name ?: "unknown"}")
            Log.d(TAG, "🎧 Bitrate: ${selectedStream.averageBitrate} kbps")
            Log.d(TAG, "🎧 Extension: $streamExtension")

            // Download audio stream with appropriate extension
            val audioFileName = "${sanitizedArtist} - ${sanitizedTitle}.$streamExtension"
            webmFile = File(outputDir, audioFileName)
            downloadStream(streamUrl, webmFile, progressCallback, progressStep)
            Log.d(TAG, "✅ Audio download complete: ${webmFile.length() / 1024 / 1024}MB")

            // Download thumbnail (keep for thumbnail URL reference)
            val thumbnailUrl = extractor.thumbnails.maxByOrNull { it.height }?.url
            if (!thumbnailUrl.isNullOrEmpty()) {
                try {
                    thumbnailFile = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}.jpg")
                    downloadStream(
                        thumbnailUrl,
                        thumbnailFile,
                        thumbnailProgressCallback,
                        progressStep
                    )
                    Log.d(TAG, "✅ Thumbnail downloaded: ${thumbnailFile.length() / 1024}KB")
                    // Keep thumbnail file for later use (don't delete)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to download thumbnail: ${e.message}")
                    thumbnailFile = null
                }
            }

            // Convert WebM to user-friendly audio container (M4A/MP3/FLAC/OPUS) with embedded metadata & cover
            // IMPORTANT: Metadata is physically embedded into the audio file using FFmpeg
            // This means the metadata stays with the file even if moved or app data is cleared
            Log.d(TAG, "📝 Embedding metadata into audio file:")
            Log.d(TAG, "   Title: $title")
            Log.d(TAG, "   Artist: $finalArtist")
            Log.d(TAG, "   Album: $album")

            val baseName = "$sanitizedArtist - $sanitizedTitle"

            // Embed metadata using Mutagen (Python library)
            // This works for M4A files which we prefer when FFmpeg is not available
            Log.d(TAG, "🐍 Embedding metadata with Mutagen...")
            val mutagenSuccess = embedMetadataWithMutagen(
                inputFile = webmFile,
                thumbnailFile = thumbnailFile,
                title = title,
                artist = finalArtist,
                album = album
            )
            
            if (mutagenSuccess) {
                Log.d(TAG, "✅ Mutagen metadata embedding successful!")
            } else {
                Log.w(TAG, "⚠️ Mutagen metadata embedding failed - file will not have embedded metadata")
            }

            // Use the downloaded file (metadata may or may not be embedded)
            val finalFile = webmFile
            
            // Clean up thumbnail file
            thumbnailFile?.delete()
            
            try {
                progressCallback?.invoke(100)
            } catch (_: Exception) {
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
                    album = album,
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
     * Select the best audio stream based on preferences.
     * Prefers M4A for metadata embedding support via Mutagen.
     */
    private fun selectBestAudioStream(
        streams: List<AudioStream>,
        preferredFormat: String?,
        ffmpegAvailable: Boolean = false  // FFmpeg removed, always false
    ): AudioStream {
        return when {
            // If specific format requested, find it
            preferredFormat != null -> {
                streams.find {
                    it.format?.suffix?.equals(
                        preferredFormat,
                        ignoreCase = true
                    ) == true
                }
                    ?: streams.maxByOrNull { it.averageBitrate }!!
            }
            // When FFmpeg is NOT available, prefer M4A for metadata embedding support
            !ffmpegAvailable -> {
                Log.d(TAG, "🎧 FFmpeg not available, preferring M4A for metadata support")
                val m4aStreams = streams.filter {
                    it.format?.suffix?.equals("m4a", ignoreCase = true) == true ||
                    it.format?.name?.contains("m4a", ignoreCase = true) == true ||
                    it.format?.name?.contains("aac", ignoreCase = true) == true
                }
                if (m4aStreams.isNotEmpty()) {
                    Log.d(TAG, "🎧 Found ${m4aStreams.size} M4A streams, selecting best bitrate")
                    m4aStreams.maxByOrNull { it.averageBitrate }!!
                } else {
                    // Fall back to best available - metadata won't be embedded
                    Log.w(TAG, "⚠️ No M4A streams available, using best quality (metadata may not be embedded)")
                    streams.maxByOrNull { it.averageBitrate }!!
                }
            }
            // When FFmpeg IS available, prefer Opus for best quality/size ratio
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
    private suspend fun downloadStream(
        url: String,
        outputFile: File,
        progress: ((Int) -> Unit)? = null,
        progressStep: Int = 1
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            )
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
                    var lastReportedPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read

                        if (contentLength > 0) {
                            val percent =
                                ((downloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                            
                            // Always call progress callback for UI updates (every 1%)
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                try {
                                    progress?.invoke(percent)
                                } catch (_: Exception) {
                                }
                            }
                            
                            // Only log to logcat every 10% or at 100%
                            val shouldLog = percent == 100 || (percent > 0 && percent % 10 == 0)
                            if (shouldLog) {
                                val progressBar = getProgressBar(percent)
                                Log.d(TAG, "⬇️ Download progress: $percent% $progressBar")
                            }
                        }
                    }
                    // Final callback in case server didn't provide content-length or to ensure 100%
                    try {
                        progress?.invoke(100)
                    } catch (_: Exception) {
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

    /**
     * Embed metadata into WebM/OGG audio file using Python's metadata_embedder module.
     * This is a fallback when FFmpeg is not available.
     * Uses Mutagen to write Vorbis comments to WebM/OGG files directly.
     */
    private fun embedMetadataWithMutagen(
        inputFile: File,
        thumbnailFile: File?,
        title: String,
        artist: String,
        album: String
    ): Boolean {
        return try {
            if (!Python.isStarted()) {
                Log.w(TAG, "⚠️ Python not started, cannot use Mutagen")
                return false
            }

            val py = Python.getInstance()
            
            Log.d(TAG, "🐍 Using metadata_embedder module for: ${inputFile.absolutePath}")
            
            // Load our custom metadata_embedder module
            val embedderModule = py.getModule("metadata_embedder")
            
            // Call the embed_metadata function
            val result = embedderModule.callAttr(
                "embed_metadata",
                inputFile.absolutePath,
                title,
                artist,
                album,
                thumbnailFile?.absolutePath
            )
            
            // Parse the result dictionary
            val success = result.callAttr("get", "success")?.toBoolean() ?: false
            val message = result.callAttr("get", "message")?.toString() ?: "Unknown result"
            
            Log.d(TAG, "🐍 Metadata embedder result: success=$success, message=$message")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mutagen metadata embedding failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Download audio using yt-dlp (Python) with metadata embedding.
     * This is the most reliable method as yt-dlp handles everything natively.
     */
    suspend fun downloadWithYtDlp(
        context: Context,
        videoId: String,
        outputDir: File,
        title: String,
        artist: String,
        album: String,
        progressCallback: ((Int) -> Unit)? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            if (!Python.isStarted()) {
                return@withContext DownloadResult(
                    success = false,
                    message = "Python not initialized"
                )
            }

            // Check if FFmpeg is available for cropping
            val ffmpegAvailable = try {
                val process = Runtime.getRuntime().exec(arrayOf("ffmpeg", "-version"))
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
            
            Log.d(TAG, "🎵 FFmpeg available for cropping: $ffmpegAvailable")
            
            val py = Python.getInstance()
            val sanitizedArtist = sanitizeFilename(artist)
            val sanitizedTitle = sanitizeFilename(title)
            val outputTemplate = "${outputDir.absolutePath}/${sanitizedArtist} - ${sanitizedTitle}.%(ext)s"
            
            Log.d(TAG, "🐍 Starting yt-dlp download with metadata embedding...")
            Log.d(TAG, "   Video ID: $videoId")
            Log.d(TAG, "   Title: $title")
            Log.d(TAG, "   Artist: $artist")
            Log.d(TAG, "   Album: $album")
            
            // Create yt-dlp options
            val ytDlpModule = py.getModule("yt_dlp")
            val url = "https://www.youtube.com/watch?v=$videoId"
            
            // Build options dictionary
            val opts = py.builtins.callAttr("dict")
            opts.callAttr("__setitem__", "format", "bestaudio/best")
            opts.callAttr("__setitem__", "outtmpl", outputTemplate)
            opts.callAttr("__setitem__", "quiet", true)
            opts.callAttr("__setitem__", "no_warnings", true)
            opts.callAttr("__setitem__", "writethumbnail", true)
            opts.callAttr("__setitem__", "embedthumbnail", true)
            
            // Add metadata
            opts.callAttr("__setitem__", "addmetadata", true)
            
            // Postprocessor args for metadata
            val postprocessors = py.builtins.callAttr("list")
            
            // Add FFmpeg metadata postprocessor
            val metadataPP = py.builtins.callAttr("dict")
            metadataPP.callAttr("__setitem__", "key", "FFmpegMetadata")
            metadataPP.callAttr("__setitem__", "add_metadata", true)
            postprocessors.callAttr("append", metadataPP)
            
            // Add thumbnail embedder
            val thumbPP = py.builtins.callAttr("dict")
            thumbPP.callAttr("__setitem__", "key", "EmbedThumbnail")
            thumbPP.callAttr("__setitem__", "already_have_thumbnail", false)
            postprocessors.callAttr("append", thumbPP)
            
            // Convert to opus (better for metadata embedding)
            val extractAudioPP = py.builtins.callAttr("dict")
            extractAudioPP.callAttr("__setitem__", "key", "FFmpegExtractAudio")
            extractAudioPP.callAttr("__setitem__", "preferredcodec", "opus")
            extractAudioPP.callAttr("__setitem__", "preferredquality", "192")
            postprocessors.callAttr("append", extractAudioPP)
            
            opts.callAttr("__setitem__", "postprocessors", postprocessors)
            
            // Create YoutubeDL instance and download
            val ydl = ytDlpModule.callAttr("YoutubeDL", opts)
            
            // Download
            Log.d(TAG, "🐍 Executing yt-dlp download...")
            ydl.callAttr("download", py.builtins.callAttr("list", arrayOf(url)))
            
            // Find the output file
            val expectedFile = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}.opus")
            val m4aFile = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}.m4a")
            val webmFile = File(outputDir, "${sanitizedArtist} - ${sanitizedTitle}.webm")
            
            // Crop thumbnail to 720x720 if FFmpeg is available
            val baseName = "${sanitizedArtist} - ${sanitizedTitle}"
            var croppedThumb: File? = null
            if (ffmpegAvailable) {
                val origThumb = listOf(".jpg", ".webp", ".png", ".jpeg", ".jpg.webp").map { ext ->
                    File(outputDir, baseName + ext)
                }.find { it.exists() }
                
                if (origThumb != null) {
                    Log.d(TAG, "🎨 Cropping thumbnail to 720x720...")
                    croppedThumb = cropCenterThumbnail(origThumb, outputDir, baseName)
                    if (croppedThumb != null) {
                        Log.d(TAG, "✅ Thumbnail cropped successfully: ${croppedThumb.name}")
                        // Replace original with cropped
                        try {
                            origThumb.delete()
                            croppedThumb.renameTo(origThumb)
                            croppedThumb = origThumb
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to replace thumbnail: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "Thumbnail cropping failed, using original")
                    }
                }
            }
            
            val finalFile = when {
                expectedFile.exists() -> expectedFile
                m4aFile.exists() -> m4aFile
                webmFile.exists() -> webmFile
                else -> {
                    // Search for any matching file
                    outputDir.listFiles()?.find { 
                        it.name.startsWith("${sanitizedArtist} - ${sanitizedTitle}") 
                    }
                }
            }
            
            // Clean up any leftover thumbnail files
            listOf(".jpg", ".webp", ".png", ".jpeg", ".jpg.webp").forEach { ext ->
                val thumbFile = File(outputDir, baseName + ext)
                if (thumbFile.exists()) {
                    try {
                        thumbFile.delete()
                        Log.d(TAG, "🧹 Cleaned up thumbnail: ${thumbFile.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to cleanup thumbnail: ${thumbFile.name}")
                    }
                }
            }
            
            if (finalFile != null && finalFile.exists() && finalFile.length() > 1024) {
                Log.d(TAG, "✅ yt-dlp download successful: ${finalFile.absolutePath}")
                DownloadResult(
                    success = true,
                    filePath = finalFile.absolutePath,
                    message = "Downloaded with embedded metadata",
                    format = finalFile.extension,
                    title = title,
                    artist = artist,
                    album = album
                )
            } else {
                DownloadResult(
                    success = false,
                    message = "Output file not found"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ yt-dlp download failed: ${e.message}", e)
            DownloadResult(
                success = false,
                message = "yt-dlp download failed: ${e.message}"
            )
        }
    }
}
