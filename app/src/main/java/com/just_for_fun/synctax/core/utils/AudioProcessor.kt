package com.just_for_fun.synctax.core.utils

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.model.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
// import com.arthenica.ffmpegkit.FFmpegKit
// import com.arthenica.ffmpegkit.ReturnCode
import java.util.regex.Pattern

data class AudioFormat(
    val formatId: String,
    val quality: String,
    val bitrate: String,
    val size: String?
)

class AudioProcessor(private val context: Context) {

    private val client = OkHttpClient()

    init {
        // Initialize Chaquopy if not already
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    /**
     * Gets available formats for a YouTube URL using yt-dlp.
     */
    suspend fun getFormats(url: String): List<Format> = withContext(Dispatchers.IO) {
        try {
            Log.d("AudioProcessor", "🎵 getFormats: Starting for URL: $url")
            
            val python = Python.getInstance()
            val ytDlp = python.getModule("yt_dlp")

            // Create YoutubeDL with YouTube-specific options using iOS client (no PO token required)
            val pyDict = python.builtins.callAttr("dict")
            pyDict.callAttr("__setitem__", "quiet", false)
            pyDict.callAttr("__setitem__", "no_warnings", false)
            pyDict.callAttr("__setitem__", "nocheckcertificate", true)
            pyDict.callAttr("__setitem__", "prefer_free_formats", true)
            pyDict.callAttr("__setitem__", "allow_unplayable_formats", false)
            // Use iOS Safari user agent
            // Let yt-dlp use default clients (android_sdkless + web_safari) - they work without PO tokens
            // Only add skip parameter for YouTube
            val extractorArgs = python.builtins.callAttr("dict")
            val youtubeArgs = python.builtins.callAttr("dict")
            
            // Skip HLS and DASH streaming formats
            val skipList = python.builtins.callAttr("list")
            skipList.callAttr("append", "hls")
            skipList.callAttr("append", "dash")
            youtubeArgs.callAttr("__setitem__", "skip", skipList)
            
            extractorArgs.callAttr("__setitem__", "youtube", youtubeArgs)
            pyDict.callAttr("__setitem__", "extractor_args", extractorArgs)
            
            // Prefer audio-only formats
            pyDict.callAttr("__setitem__", "format", "bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best")

            val ydl = ytDlp.callAttr("YoutubeDL", pyDict)

            Log.d("AudioProcessor", "🎵 getFormats: Extracting info from yt-dlp...")
            
            // Extract info to get formats
            val info = ydl.callAttr("extract_info", url, false)
            val formatsList = info["formats"] as? List<Map<String, Any>> ?: emptyList()
            
            Log.d("AudioProcessor", "🎵 getFormats: Found ${formatsList.size} total formats")

            val formats = mutableListOf<Format>()

            for (fmt in formatsList) {
                try {
                    val format = Format(
                        format_id = fmt["format_id"] as? String ?: "",
                        container = fmt["ext"] as? String ?: "",
                        vcodec = fmt["vcodec"] as? String ?: "",
                        acodec = fmt["acodec"] as? String ?: "",
                        encoding = fmt["encoding"] as? String ?: "",
                        filesize = (fmt["filesize"] as? Number)?.toLong() ?: 0L,
                        format_note = fmt["format_note"] as? String ?: "",
                        fps = fmt["fps"]?.toString(),
                        asr = fmt["asr"]?.toString(),
                        url = fmt["url"] as? String,
                        lang = fmt["language"] as? String,
                        tbr = fmt["tbr"]?.toString()
                    )
                    formats.add(format)
                } catch (e: Exception) {
                    Log.w("AudioProcessor", "🎵 getFormats: Error parsing format: ${fmt["format_id"]}", e)
                }
            }

            Log.d("AudioProcessor", "🎵 getFormats: Successfully parsed ${formats.size} formats")
            return@withContext formats
        } catch (e: Exception) {
            Log.e("AudioProcessor", "🎵 getFormats: ❌ Error getting formats: ${e.message}", e)
            Log.e("AudioProcessor", "🎵 getFormats: Stack trace:", e)
            emptyList()
        }
    }

    /**
     * Gets available audio formats for a YouTube URL using yt-dlp.
     * @deprecated Use getFormats() instead for full format information
     */
    suspend fun getAudioFormats(url: String): List<AudioFormat> = withContext(Dispatchers.IO) {
        try {
            Log.d("AudioProcessor", "🎵 getAudioFormats: Starting for URL: $url")
            
            val formats = getFormats(url)
            
            Log.d("AudioProcessor", "🎵 getAudioFormats: Processing ${formats.size} formats for audio")

            val audioFormats = mutableListOf<AudioFormat>()

            for (fmt in formats) {
                // Include formats that can be converted to audio (prioritize opus-compatible)
                val abr = fmt.tbr?.toIntOrNull()
                
                Log.v("AudioProcessor", "🎵 getAudioFormats: Checking format ${fmt.format_id}: container=${fmt.container}, acodec=${fmt.acodec}, tbr=$abr")
                
                if ((fmt.container == "m4a" || fmt.container == "webm" || fmt.container == "mp4" || fmt.container == "mp3" || fmt.container == "opus") &&
                    abr != null && abr > 0 && fmt.acodec.isNotBlank() && fmt.acodec != "none") {

                    val quality = when {
                        abr >= 256 -> "High (${abr}kbps)"
                        abr >= 128 -> "Medium (${abr}kbps)"
                        else -> "Low (${abr}kbps)"
                    }
                    val size = if (fmt.filesize > 0) "${fmt.filesize / 1024 / 1024}MB" else "Unknown"
                    val audioFormat = AudioFormat(fmt.format_id, quality, "${abr}kbps", size)
                    audioFormats.add(audioFormat)
                    
                    Log.d("AudioProcessor", "🎵 getAudioFormats: Added format: $quality (${fmt.format_id})")
                }
            }

            Log.d("AudioProcessor", "🎵 getAudioFormats: Found ${audioFormats.size} audio formats before filtering")

            // Select top 3 unique qualities, sorted by bitrate descending
            val selectedFormats = audioFormats.sortedByDescending { it.bitrate.split(" ")[0].toIntOrNull() ?: 0 }
                  .distinctBy { it.quality }
                  .take(3)
            
            Log.d("AudioProcessor", "🎵 getAudioFormats: Returning ${selectedFormats.size} selected formats")
            selectedFormats.forEachIndexed { index, format ->
                Log.d("AudioProcessor", "🎵 getAudioFormats: Format $index: ${format.quality} - ${format.formatId}")
            }
            
            selectedFormats
        } catch (e: Exception) {
            Log.e("AudioProcessor", "🎵 getAudioFormats: ❌ Error: ${e.message}", e)
            Log.e("AudioProcessor", "🎵 getAudioFormats: Stack trace:", e)
            emptyList()
        }
    }

    /**
     * Downloads audio in the selected format using yt-dlp.
     */
    suspend fun downloadAudio(url: String, formatId: String, song: Song): File? = withContext(Dispatchers.IO) {
        try {
            Log.d("AudioProcessor", "🎵 downloadAudio: Starting download for '${song.title}'")
            Log.d("AudioProcessor", "🎵 downloadAudio: URL: $url")
            Log.d("AudioProcessor", "🎵 downloadAudio: Format ID: $formatId")
            
            val python = Python.getInstance()
            val ytDlp = python.getModule("yt_dlp")

            // Use external files dir for downloads
            val downloadDir = File(context.getExternalFilesDir("downloads"), "SyncTax").apply { mkdirs() }
            val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").trim()
            val safeArtist = song.artist?.replace(Regex("[^a-zA-Z0-9\\s-]"), "")?.trim() ?: "Unknown"
            val outputTemplate = "$safeTitle - $safeArtist.%(ext)s"
            
            Log.d("AudioProcessor", "🎵 downloadAudio: Download directory: ${downloadDir.absolutePath}")
            Log.d("AudioProcessor", "🎵 downloadAudio: Output template: $outputTemplate")

            // Create options as a Python dict
            val pyDict = python.builtins.callAttr("dict")
            pyDict.callAttr("__setitem__", "format", formatId)
            pyDict.callAttr("__setitem__", "extractaudio", true)
            pyDict.callAttr("__setitem__", "audioformat", "opus")
            pyDict.callAttr("__setitem__", "outtmpl", File(downloadDir, outputTemplate).absolutePath)
            pyDict.callAttr("__setitem__", "noplaylist", true)
            pyDict.callAttr("__setitem__", "quiet", false)  // Enable output for debugging

            Log.d("AudioProcessor", "🎵 downloadAudio: Starting yt-dlp download...")
            
            val ydl = ytDlp.callAttr("YoutubeDL", pyDict)
            ydl.callAttr("download", python.builtins.callAttr("list", listOf(url)))

            Log.d("AudioProcessor", "🎵 downloadAudio: yt-dlp download completed, searching for file...")

            // Find the downloaded file
            val downloadedFile = downloadDir.listFiles()?.find { it.name.startsWith(safeTitle) && (it.extension == "opus" || it.extension == "m4a") }
            
            if (downloadedFile != null) {
                Log.d("AudioProcessor", "🎵 downloadAudio: ✅ Found downloaded file: ${downloadedFile.absolutePath}")
                Log.d("AudioProcessor", "🎵 downloadAudio: File size: ${downloadedFile.length() / 1024 / 1024}MB")
            } else {
                Log.e("AudioProcessor", "🎵 downloadAudio: ❌ Downloaded file not found in ${downloadDir.absolutePath}")
                Log.d("AudioProcessor", "🎵 downloadAudio: Files in directory:")
                downloadDir.listFiles()?.forEach {
                    Log.d("AudioProcessor", "🎵 downloadAudio:   - ${it.name}")
                }
            }
            
            downloadedFile
        } catch (e: Exception) {
            Log.e("AudioProcessor", "🎵 downloadAudio: ❌ Error: ${e.message}", e)
            Log.e("AudioProcessor", "🎵 downloadAudio: Stack trace:", e)
            null
        }
    }

    /**
     * Downloads album art from the given URI to a temporary file.
     */
    private suspend fun downloadAlbumArt(uri: String): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(uri).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val tempFile = File.createTempFile("album_art", ".jpg", context.cacheDir)
                tempFile.outputStream().use { output ->
                    response.body?.byteStream()?.copyTo(output)
                }
                tempFile
            } else {
                Log.e("AudioProcessor", "Failed to download art: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e("AudioProcessor", "Error downloading album art", e)
            null
        }
    }

    /**
     * Embeds metadata and album art into the audio file using FFmpeg.
     */
    suspend fun embedMetadataAndArt(inputFile: File, song: Song): File? = withContext(Dispatchers.IO) {
        // For now, just return the input file without embedding
        // TODO: Re-enable FFmpeg embedding when dependency is resolved
        Log.d("AudioProcessor", "Embedding metadata skipped (FFmpeg not available)")
        inputFile
    }

    /**
     * Full download and embed process for selected format.
     */
    suspend fun downloadAndEmbed(url: String, formatId: String, song: Song): File? {
        Log.d("AudioProcessor", "Starting download for ${song.title}")
        val audioFile = downloadAudio(url, formatId, song)
        if (audioFile == null) {
            Log.e("AudioProcessor", "Download failed")
            return null
        }
        Log.d("AudioProcessor", "Download successful, embedding metadata")
        return embedMetadataAndArt(audioFile, song)
    }
}
