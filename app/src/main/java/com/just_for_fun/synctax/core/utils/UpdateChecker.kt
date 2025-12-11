package com.just_for_fun.synctax.core.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.just_for_fun.synctax.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * Utility class for checking app updates from GitHub releases
 */
class UpdateChecker(private val context: Context) {

    private val client = OkHttpClient()
    private val repoOwner = "krit-vardhan-mishra"
    private val repoName = "SyncTax"

    /**
     * Data class for update information
     */
    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String?,
        val releaseNotes: String?
    )

    /**
     * Check for updates by comparing current version with latest GitHub release
     */
    suspend fun checkForUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = BuildConfig.VERSION_NAME

            // Fetch latest release from GitHub API
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch release info: ${response.code}"))
            }

            val jsonResponse = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val releaseJson = JSONObject(jsonResponse)

            val latestVersionTag = releaseJson.getString("tag_name")
            // Remove 'v-' or 'v' prefix if present (GitHub releases may use either format)
            val latestVersion = latestVersionTag.removePrefix("v-").removePrefix("v")

            // Compare versions
            val isUpdateAvailable = compareVersions(currentVersion, latestVersion) < 0

            // Find APK asset
            val assets = releaseJson.getJSONArray("assets")
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.getString("name")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val releaseNotes = if (releaseJson.has("body")) releaseJson.getString("body") else null

            val updateInfo = UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download and install the APK
     */
    fun downloadAndInstallApk(downloadUrl: String, onDownloadComplete: () -> Unit = {}) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("SyncTax Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "synctax_update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        // Register receiver for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    // Download complete, install APK
                    installApk()
                    onDownloadComplete()
                    context?.unregisterReceiver(this)
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    /**
     * Install the downloaded APK
     */
    private fun installApk() {
        val apkFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "synctax_update.apk"
        )

        if (!apkFile.exists()) return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                },
                "application/vnd.android.package-archive"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }

    /**
     * Compare two semantic version strings
     * Returns: negative if version1 < version2, 0 if equal, positive if version1 > version2
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val v1 = v1Parts.getOrElse(i) { 0 }
            val v2 = v2Parts.getOrElse(i) { 0 }

            if (v1 != v2) {
                return v1.compareTo(v2)
            }
        }

        return 0
    }
}