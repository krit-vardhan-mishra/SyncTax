package com.just_for_fun.synctax.core.utils

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.core.data.local.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class AudioProcessor(private val context: Context) {

    private val client = OkHttpClient()

    init {
        // Initialize Chaquopy if not already
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    /**
     * Downloads audio from YouTube using yt-dlp.
     */
    suspend fun downloadAudio(song: Song): File? = withContext(Dispatchers.IO) {
        try {
            val python = Python.getInstance()
            val ytDlp = python.getModule("yt_dlp")

            val outputDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val outputTemplate = "${song.title} - ${song.artist}.%(ext)s"

            val options = mapOf(
                "format" to "bestaudio/best",
                "extractaudio" to true,
                "audioformat" to "mp3",
                "outtmpl" to File(outputDir, outputTemplate).absolutePath,
                "noplaylist" to true,
                "quiet" to true
            )

            val query = "ytsearch:${song.title} ${song.artist}"
            val ydl = ytDlp.callAttr("YoutubeDL", options)
            ydl.callAttr("download", listOf(query))

            // Find the downloaded file
            outputDir.listFiles()?.find { it.name.startsWith(song.title) && it.extension == "mp3" }
        } catch (e: Exception) {
            Log.e("AudioProcessor", "Error downloading audio", e)
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
                null
            }
        } catch (e: IOException) {
            Log.e("AudioProcessor", "Error downloading album art", e)
            null
        }
    }

    /**
     * Embeds metadata and album art into the audio file using FFmpeg.
     * @param inputFile The downloaded audio file.
     * @param song The song metadata.
     * @return The output file with embedded data, or null if failed.
     */
    suspend fun embedMetadataAndArt(inputFile: File, song: Song): File? = withContext(Dispatchers.IO) {
        val outputFile = File(inputFile.parent, "${song.title}_${song.artist}_embedded.mp3")

        // Download album art if available
        val artFile = song.albumArtUri?.let { downloadAlbumArt(it) }

        // Build FFmpeg command
        val command = buildString {
            append("-i \"${inputFile.absolutePath}\"")
            artFile?.let { append(" -i \"${it.absolutePath}\"") }
            append(" -map 0")
            artFile?.let { append(" -map 1") }
            append(" -c copy")
            append(" -id3v2_version 3")
            append(" -metadata title=\"${song.title}\"")
            append(" -metadata artist=\"${song.artist}\"")
            song.album?.let { append(" -metadata album=\"$it\"") }
            song.genre?.let { append(" -metadata genre=\"$it\"") }
            song.releaseYear?.let { append(" -metadata date=\"$it\"") }
            artFile?.let { append(" -metadata:s:v title=\"Album cover\" -metadata:s:v comment=\"Cover (front)\"") }
            append(" \"${outputFile.absolutePath}\"")
        }

        Log.d("AudioProcessor", "FFmpeg command: $command")

        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            Log.d("AudioProcessor", "Embedding successful")
            // Clean up temp files
            artFile?.delete()
            inputFile.delete() // Remove original
            outputFile
        } else {
            Log.e("AudioProcessor", "FFmpeg failed: ${session.failStackTrace}")
            null
        }
    }

    /**
     * Full download and embed process.
     */
    suspend fun downloadAndEmbed(song: Song): File? {
        val audioFile = downloadAudio(song)
        return audioFile?.let { embedMetadataAndArt(it, song) }
    }
}