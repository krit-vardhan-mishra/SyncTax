package com.just_for_fun.synctax.core.chaquopy

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
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

    suspend fun downloadAudio(url: String, outputDir: String): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                val resultJson = pythonModule.callAttr("download_audio", url, outputDir)
                val result = JSONObject(resultJson.toString())

                val success = result.optBoolean("success", false)
                val message = result.optString("message", "Unknown error")
                val filePath = result.optString("file_path", "")
                val title = result.optString("title", "Unknown")
                val artist = result.optString("artist", "Unknown")
                val duration = result.optInt("duration", 0)
                val thumbnailUrl = result.optString("thumbnail_url", "")

                Log.d(TAG, "Download result: $message")

                DownloadResult(
                    success = success,
                    message = message,
                    filePath = filePath,
                    title = title,
                    artist = artist,
                    duration = duration,
                    thumbnailUrl = thumbnailUrl
                )
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
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

    suspend fun getVideoInfo(url: String): VideoInfo {
        return withContext(Dispatchers.IO) {
            try {
                val resultJson = pythonModule.callAttr("get_video_info", url)
                val result = JSONObject(resultJson.toString())

                val success = result.optBoolean("success", false)
                val message = result.optString("message", "")
                val title = result.optString("title", "Unknown")
                val artist = result.optString("artist", "Unknown")
                val duration = result.optInt("duration", 0)
                val thumbnailUrl = result.optString("thumbnail_url", "")
                val description = result.optString("description", "")

                VideoInfo(
                    success = success,
                    message = message,
                    title = title,
                    artist = artist,
                    duration = duration,
                    thumbnailUrl = thumbnailUrl,
                    description = description
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get video info", e)
                VideoInfo(
                    success = false,
                    message = "Error: ${e.message}",
                    title = "",
                    artist = "",
                    duration = 0,
                    thumbnailUrl = "",
                    description = ""
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
    val duration: Int,
    val thumbnailUrl: String,
    val description: String
)
