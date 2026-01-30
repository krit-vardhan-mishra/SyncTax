package com.just_for_fun.synctax.core.utils

import android.content.Context
import android.util.Log
import com.just_for_fun.synctax.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class to check for library updates and handle version compatibility
 */
object LibraryUpdateChecker {

    private const val TAG = "LibraryUpdateChecker"
    private const val NEWPIPE_GITHUB_API = "https://api.github.com/repos/TeamNewPipe/NewPipeExtractor/releases/latest"

    /**
     * Check if NewPipeExtractor has a newer version available
     */
    suspend fun checkForNewPipeUpdate(): LibraryUpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(NEWPIPE_GITHUB_API)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "SyncTax-Android/${BuildConfig.VERSION_NAME}")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)

                    val latestVersion = jsonResponse.getString("tag_name").removePrefix("v")
                    val currentVersion = getCurrentNewPipeVersion()

                    Log.d(TAG, "Current NewPipe version: $currentVersion, Latest: $latestVersion")

                    val needsUpdate = compareVersions(currentVersion, latestVersion) < 0

                    connection.disconnect()

                    return@withContext LibraryUpdateResult(
                        needsUpdate = needsUpdate,
                        currentVersion = currentVersion,
                        latestVersion = latestVersion,
                        releaseNotes = jsonResponse.optString("body", ""),
                        releaseUrl = jsonResponse.optString("html_url", "")
                    )
                } else {
                    Log.w(TAG, "Failed to check NewPipe updates: HTTP $responseCode")
                    connection.disconnect()
                    return@withContext LibraryUpdateResult(
                        needsUpdate = false,
                        error = "Failed to check updates: HTTP $responseCode"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking for NewPipe updates", e)
                return@withContext LibraryUpdateResult(
                    needsUpdate = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Get current NewPipeExtractor version from build config
     */
    private fun getCurrentNewPipeVersion(): String {
        // This would be set in build.gradle.kts as a buildConfigField
        // For now, we'll hardcode it based on our libs.versions.toml
        return "0.25.1" // Update this when you update the library
    }

    /**
     * Compare version strings (semantic versioning)
     * Returns: -1 if v1 < v2, 0 if equal, 1 if v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val part1 = parts1.getOrElse(i) { 0 }
            val part2 = parts2.getOrElse(i) { 0 }

            when {
                part1 < part2 -> return -1
                part1 > part2 -> return 1
            }
        }

        return 0
    }

    /**
     * Check if a stream error indicates NewPipeExtractor needs updating
     */
    fun isNewPipeVersionError(error: Throwable?): Boolean {
        if (error == null) return false

        val errorMessage = error.message ?: ""
        return errorMessage.contains("page needs to be reloaded", ignoreCase = true) ||
               errorMessage.contains("ContentNotAvailableException", ignoreCase = true) ||
               errorMessage.contains("signature", ignoreCase = true) ||
               errorMessage.contains("cipher", ignoreCase = true)
    }
}

data class LibraryUpdateResult(
    val needsUpdate: Boolean,
    val currentVersion: String = "",
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val releaseUrl: String = "",
    val error: String? = null
)