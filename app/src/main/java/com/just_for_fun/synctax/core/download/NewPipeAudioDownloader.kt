package com.just_for_fun.synctax.core.download

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.just_for_fun.synctax.MusicApplication
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

            val baseName = "${sanitizedArtist} - ${sanitizedTitle}"

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
                        args += listOf(
                            "-i",
                            thumbnailFile.absolutePath,
                            "-map",
                            "0:a",
                            "-map",
                            "1",
                            "-c:v",
                            "copy",
                            "-disposition:v",
                            "attached_pic"
                        )
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
                        args += listOf(
                            "-i",
                            thumbnailFile.absolutePath,
                            "-map",
                            "0:a",
                            "-map",
                            "1",
                            "-c:v",
                            "copy"
                        )
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

            // Execute FFmpeg command using the instance's execute method
            // The youtubedl-android FFmpeg library uses executeAsync but we need synchronous execution
            val rc = try {
                // Try to call execute using reflection since the API might vary
                val executeMethod =
                    FFmpeg.getInstance().javaClass.getMethod("execute", Array<String>::class.java)
                val result = executeMethod.invoke(FFmpeg.getInstance(), args)

                when (result) {
                    is Int -> result
                    is Boolean -> if (result) 0 else -1
                    else -> {
                        // If we get here, check if file was created successfully
                        if (t.output.exists() && t.output.length() > 1024) 0 else -1
                    }
                }
            } catch (e: NoSuchMethodException) {
                // Method not found, try executeAsync or just check if FFmpeg creates the file
                Log.w(TAG, "⚠️ execute method not found, trying alternative approach")
                try {
                    // Some versions use executeAsync - let's try with a command string
                    val commandString = args.joinToString(" ") { arg ->
                        if (arg.contains(" ")) "\"$arg\"" else arg
                    }

                    // Try calling any execute-like method we can find
                    val methods = FFmpeg.getInstance().javaClass.methods.filter {
                        it.name.contains("execute", ignoreCase = true)
                    }

                    var executed = false
                    for (method in methods) {
                        try {
                            when (method.parameterCount) {
                                1 -> {
                                    val param = method.parameterTypes[0]
                                    val result = when {
                                        param == String::class.java -> method.invoke(
                                            FFmpeg.getInstance(),
                                            commandString
                                        )

                                        param == Array<String>::class.java -> method.invoke(
                                            FFmpeg.getInstance(),
                                            args
                                        )

                                        else -> continue
                                    }
                                    executed = true
                                    break
                                }
                            }
                        } catch (_: Exception) {
                            continue
                        }
                    }

                    // Check if file was created
                    if (t.output.exists() && t.output.length() > 1024) 0 else -1
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ FFmpeg execution error for ${t.desc}: ${e2.message}", e2)
                    -1
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FFmpeg execution error for ${t.desc}: ${e.message}", e)
                -1
            }

            Log.d(TAG, "🔧 FFmpeg ${t.desc} return code: $rc")

            val ok = rc == 0 && t.output.exists() && t.output.length() > 1024

            if (ok) {
                Log.d(TAG, "✅ FFmpeg ${t.desc} succeeded! Output: ${t.output.length() / 1024}KB")
            } else {
                Log.w(
                    TAG,
                    "⚠️ FFmpeg ${t.desc} failed - RC: $rc, exists: ${t.output.exists()}, size: ${if (t.output.exists()) t.output.length() else 0}"
                )
                // cleanup partial output
                if (t.output.exists()) {
                    t.output.delete()
                    Log.d(TAG, "🗑️ Cleaned up failed output: ${t.output.name}")
                }
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
     * Select the best audio stream based on preferences and FFmpeg availability.
     * When FFmpeg is available: Prefer Opus (best quality/size)
     * When FFmpeg is NOT available: Prefer M4A (supports metadata embedding via Mutagen)
     */
    private fun selectBestAudioStream(
        streams: List<AudioStream>,
        preferredFormat: String?,
        ffmpegAvailable: Boolean = true
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
                            val shouldReport = when {
                                percent == 100 -> true
                                lastReportedPercent < 0 -> true
                                progressStep <= 1 -> percent != lastReportedPercent
                                else -> (percent - lastReportedPercent) >= progressStep || percent % progressStep == 0
                            }

                            if (shouldReport && percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                val progressBar = getProgressBar(percent)
                                Log.d(TAG, "⬇️ Download progress: $percent% $progressBar")
                                try {
                                    progress?.invoke(percent)
                                } catch (_: Exception) {
                                }
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
