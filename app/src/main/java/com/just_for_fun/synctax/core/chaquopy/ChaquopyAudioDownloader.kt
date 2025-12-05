package com.just_for_fun.synctax.core.chaquopy

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.data.model.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Wrapper over the Python audio downloader (Chaquopy)
 * Downloads audio from YouTube and other platforms using yt-dlp
 */
class ChaquopyAudioDownloader private constructor(context: Context) {

    private val pythonModule: com.chaquo.python.PyObject

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val python = Python.getInstance()
        pythonModule = python.getModule("audio_downloader")
        Log.d(TAG, "Chaquopy Audio Downloader initialized")
    }

    suspend fun downloadAudio(
        url: String,
        outputDir: String,
        formatId: String? = null,
        poTokenData: String? = null  // Deprecated, kept for API compatibility
    ): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎵 Starting download for URL: $url")
                Log.d(TAG, "🎵 Output directory: $outputDir")

                val resultJson = if (formatId != null) {
                    pythonModule.callAttr(
                        "download_audio",
                        url,
                        outputDir,
                        false,
                        formatId,
                        null  // No PO Token
                    )
                } else {
                    pythonModule.callAttr(
                        "download_audio",
                        url,
                        outputDir,
                        false,
                        null,
                        null  // No PO Token
                    )
                }

                Log.d(TAG, "🎵 Python download_audio returned JSON: ${resultJson.toString()}")

                val result = JSONObject(resultJson.toString())

                val success = result.optBoolean("success", false)
                val message = result.optString("message", "Unknown error")
                val filePath = result.optString("file_path", "")
                // val title = result.optString("title", "Unknown")
                // val artist = result.optString("artist", "Unknown")
                // val duration = result.optInt("duration", 0)
                // val thumbnailUrl = result.optString("thumbnail_url", "")
                val format = result.optString("format", "unknown")
                val ffmpegAvailable = result.optBoolean("ffmpeg_available", false)

                Log.d(TAG, "🎵 Download result - Success: $success")
                Log.d(TAG, "🎵 Download result - Message: $message")
                Log.d(TAG, "🎵 Download result - File path: $filePath")
                Log.d(TAG, "🎵 Download result - Format: $format")
                Log.d(TAG, "🎵 Download result - FFmpeg available: $ffmpegAvailable")

                DownloadResult(
                    success = success,
                    message = message,
                    filePath = filePath,
                    title = "",  // Metadata disabled
                    artist = "",  // Metadata disabled
                    duration = 0,  // Metadata disabled
                    thumbnailUrl = ""  // Metadata disabled
                )
            } catch (e: Exception) {
                Log.e(TAG, "🎵 ❌ Download failed: ${e.message}", e)
                Log.e(TAG, "🎵 Stack trace:", e)
                DownloadResult(
                    success = false,
                    message = "Download error: ${e.message}",
                    filePath = "",
                    title = "",
                    artist = "",
                    duration = 0,
                    thumbnailUrl = ""
                )
            }
        }
    }

    suspend fun getVideoInfo(url: String, poTokenData: String? = null): VideoInfo {
        return withContext(Dispatchers.IO) {
            try {
                val resultJson = pythonModule.callAttr("get_video_info", url, null)  // No PO Token
                val result = JSONObject(resultJson.toString())

                val success = result.optBoolean("success", false)
                val message = result.optString("message", "")
                val title = result.optString("title", "Unknown")
                val artist = result.optString("artist", "Unknown")
                val album = result.optString("album", "YouTube Audio")
                val duration = result.optInt("duration", 0)
                val thumbnailUrl = result.optString("thumbnail_url", "")
                val description = result.optString("description", "")

                // Parse formats with all available metadata
                val formatsArray = result.optJSONArray("formats")
                val formats = mutableListOf<com.just_for_fun.synctax.data.model.Format>()
                if (formatsArray != null) {
                    for (i in 0 until formatsArray.length()) {
                        val formatJson = formatsArray.getJSONObject(i)
                        val format = com.just_for_fun.synctax.data.model.Format(
                            format_id = formatJson.optString("format_id", ""),
                            container = formatJson.optString(
                                "container",
                                formatJson.optString("ext", "")
                            ),
                            vcodec = formatJson.optString("vcodec", ""),
                            acodec = formatJson.optString("acodec", ""),
                            encoding = formatJson.optString("encoding", ""),
                            filesize = formatJson.optLong("filesize", 0),
                            format_note = formatJson.optString("format_note", ""),
                            fps = formatJson.optString("fps", ""),
                            asr = formatJson.optString("asr", ""),
                            url = formatJson.optString("url", ""),
                            lang = formatJson.optString("lang", ""),
                            tbr = formatJson.optString("tbr", "")
                        )
                        formats.add(format)
                    }
                }

                VideoInfo(
                    success = success,
                    message = message,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    thumbnailUrl = thumbnailUrl,
                    description = description,
                    formats = formats
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get video info", e)
                VideoInfo(
                    success = false,
                    message = "Error: ${e.message}",
                    title = "",
                    artist = "",
                    album = "YouTube Audio",
                    duration = 0,
                    thumbnailUrl = "",
                    description = "",
                    formats = emptyList()
                )
            }
        }
    }

    companion object {
        private const val TAG = "ChaquopyAudioDownloader"

        @Volatile
        private var INSTANCE: ChaquopyAudioDownloader? = null

        fun getInstance(context: Context): ChaquopyAudioDownloader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChaquopyAudioDownloader(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

data class DownloadResult(
    val success: Boolean,
    val message: String,
    val filePath: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val thumbnailUrl: String
)

data class VideoInfo(
    val success: Boolean,
    val message: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val thumbnailUrl: String,
    val description: String,
    val formats: List<Format> = emptyList()
)
