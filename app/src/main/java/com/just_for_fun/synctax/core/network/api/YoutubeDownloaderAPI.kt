package com.just_for_fun.synctax.core.network.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class YoutubeDownloaderAPI(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Download audio synchronously - returns file directly
     * Use this in a coroutine or background thread
     */
    fun downloadAudioSync(youtubeUrl: String, outputFile: File): Result<File> {
        val json = JSONObject().apply {
            put("url", youtubeUrl)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/download-sync")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Download failed: ${response.code}"))
                }

                response.body?.byteStream()?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Result.success(outputFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start async download - returns job_id immediately
     */
    fun startDownloadAsync(youtubeUrl: String): Result<String> {
        val json = JSONObject().apply {
            put("url", youtubeUrl)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/download")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Request failed: ${response.code}"))
                }

                val jsonResponse = JSONObject(response.body?.string() ?: "")
                val jobId = jsonResponse.getString("job_id")
                Result.success(jobId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download file by filename
     */
    fun downloadFileByName(filename: String, outputFile: File): Result<File> {
        val request = Request.Builder()
            .url("$baseUrl/file/$filename")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Download failed: ${response.code}"))
                }

                response.body?.byteStream()?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Result.success(outputFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of available files
     */
    fun listFiles(): Result<List<AudioFile>> {
        val request = Request.Builder()
            .url("$baseUrl/files")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Request failed: ${response.code}"))
                }

                val jsonResponse = JSONObject(response.body?.string() ?: "")
                val filesArray = jsonResponse.getJSONArray("files")
                val files = mutableListOf<AudioFile>()

                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    files.add(
                        AudioFile(
                            name = fileObj.getString("name"),
                            sizeMb = fileObj.getDouble("size_mb"),
                            created = fileObj.getString("created"),
                            path = fileObj.getString("path")
                        )
                    )
                }

                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class AudioFile(
    val name: String,
    val sizeMb: Double,
    val created: String,
    val path: String
)


//// Example Usage in Activity/Fragment:
//class MainActivity : AppCompatActivity() {
//    private val downloader = YouTubeDownloader("http://your-server-ip:8000")
//
//    fun onDownloadButtonClick(youtubeUrl: String) {
//        // Show loading indicator
//        showLoading(true)
//
//        // Run in background thread (use Coroutines in real app)
//        lifecycleScope.launch(Dispatchers.IO) {
//            val outputFile = File(
//                getExternalFilesDir(Environment.DIRECTORY_MUSIC),
//                "downloaded_audio.m4a"
//            )
//
//            val result = downloader.downloadAudioSync(youtubeUrl, outputFile)
//
//            withContext(Dispatchers.Main) {
//                showLoading(false)
//
//                result.fold(
//                    onSuccess = { file ->
//                        Toast.makeText(this@MainActivity,
//                            "Downloaded: ${file.name}",
//                            Toast.LENGTH_LONG).show()
//                        // Play or share the file
//                    },
//                    onFailure = { error ->
//                        Toast.makeText(this@MainActivity,
//                            "Error: ${error.message}",
//                            Toast.LENGTH_LONG).show()
//                    }
//                )
//            }
//        }
//    }
//
//    private fun showLoading(show: Boolean) {
//        // Update UI
//    }
//}