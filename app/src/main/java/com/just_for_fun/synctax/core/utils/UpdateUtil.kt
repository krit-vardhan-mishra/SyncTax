package com.just_for_fun.synctax.core.utils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.just_for_fun.synctax.BuildConfig
import com.just_for_fun.synctax.data.model.GithubRelease
import com.just_for_fun.synctax.data.model.GithubReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Comprehensive update utility for both app and library updates
 * Inspired by ytdlnis UpdateUtil implementation
 */
class UpdateUtil(private val context: Context) {

    private val tag = "UpdateUtil"
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        // GitHub API URLs
        private const val APP_RELEASES_URL = "https://api.github.com/repos/krit-vardhan-mishra/SyncTax/releases"
        private const val NEWPIPE_RELEASES_URL = "https://api.github.com/repos/TeamNewPipe/NewPipeExtractor/releases"

        // Preference keys
        const val PREF_AUTO_UPDATE_APP = "auto_update_app"
        const val PREF_CHECK_UPDATE_ON_START = "check_update_on_start"
        const val PREF_AUTO_UPDATE_LIBRARIES = "auto_update_libraries"
        const val PREF_SKIP_APP_VERSIONS = "skip_app_versions"
        const val PREF_SKIP_LIBRARY_VERSIONS = "skip_library_versions"
        const val PREF_INCLUDE_BETA = "include_beta_updates"
        const val PREF_LAST_UPDATE_CHECK = "last_update_check"
        const val PREF_NEWPIPE_VERSION = "newpipe_version"

        // Singleton state flags
        @Volatile
        var isUpdatingApp = false
            private set
        
        @Volatile
        var isUpdatingLibrary = false
            private set
    }

    /**
     * Extension function to convert version tag to comparable number
     */
    private fun String.tagNameToVersionNumber(): Int {
        return this.replace("-beta", "")
            .replace("-alpha", "")
            .replace("v", "")
            .replace(".", "")
            .padEnd(10, '0')
            .toIntOrNull() ?: 0
    }

    // ==================== APP UPDATE METHODS ====================

    /**
     * Check for new app version from GitHub releases
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun checkForAppUpdate(): Result<GithubRelease> {
        try {
            val skippedVersions = sharedPreferences.getString(PREF_SKIP_APP_VERSIONS, "")
                ?.split(",")
                ?.distinct()
                ?.toMutableList() ?: mutableListOf()

            val releases = getGithubReleases(APP_RELEASES_URL)
            if (releases.isEmpty()) {
                return Result.failure(Exception("Network error: Unable to fetch releases"))
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val currentVerNumber = currentVersion.tagNameToVersionNumber()

            val includeBeta = sharedPreferences.getBoolean(PREF_INCLUDE_BETA, false)
            var isInLatest = true

            // Find appropriate release
            var release: GithubRelease = if (includeBeta) {
                releases.firstOrNull { it.tagName.contains("beta", true) || it.tagName.contains("alpha", true) }
                    ?: releases.first()
            } else {
                releases.first { !it.tagName.contains("beta", true) && !it.tagName.contains("alpha", true) && !it.prerelease }
            }

            if (includeBeta) {
                // If user wants beta, check if stable is newer
                val stableRelease = releases.firstOrNull { 
                    !it.tagName.contains("beta", true) && 
                    !it.tagName.contains("alpha", true) && 
                    !it.prerelease 
                }

                if (stableRelease != null) {
                    val betaVerNumber = release.tagName.removePrefix("v").tagNameToVersionNumber()
                    val stableVerNumber = stableRelease.tagName.removePrefix("v").tagNameToVersionNumber()

                    // If stable is newer than beta, use stable
                    if (stableVerNumber > betaVerNumber) {
                        release = stableRelease
                        isInLatest = currentVerNumber >= stableVerNumber
                    } else {
                        isInLatest = currentVerNumber >= betaVerNumber
                    }
                }
            } else {
                val releaseVerNumber = release.tagName.removePrefix("v").removePrefix("v-").tagNameToVersionNumber()
                
                // If current version is beta but user disabled beta updates, allow downgrade to stable
                isInLatest = if (currentVersion.contains("beta", true) || currentVersion.contains("alpha", true)) {
                    false // Always show update option to move to stable
                } else {
                    currentVerNumber >= releaseVerNumber
                }
            }

            // Check if user skipped this version
            if (skippedVersions.contains(release.tagName)) {
                isInLatest = true
            }

            if (isInLatest) {
                return Result.failure(Exception("You are using the latest version"))
            }

            return Result.success(release)
        } catch (e: Exception) {
            Log.e(tag, "Error checking for app update", e)
            return Result.failure(e)
        }
    }

    /**
     * Get all GitHub releases for the app
     */
    fun getAppReleases(): List<GithubRelease> {
        return getGithubReleases(APP_RELEASES_URL)
    }

    /**
     * Download and install app update
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadAndInstallAppUpdate(
        release: GithubRelease,
        onProgress: ((Int) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (isUpdatingApp) {
            onError?.invoke("Update already in progress")
            return
        }

        isUpdatingApp = true

        try {
            // Find APK asset matching device architecture
            val apkAsset = findApkAsset(release.assets)
            if (apkAsset == null) {
                isUpdatingApp = false
                onError?.invoke("Could not find compatible APK for your device")
                return
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Ensure downloads directory exists
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs()

            val downloadRequest = DownloadManager.Request(Uri.parse(apkAsset.browserDownloadUrl))
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle("SyncTax Update - ${release.tagName}")
                .setDescription("Downloading update...")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkAsset.name)

            val downloadId = downloadManager.enqueue(downloadRequest)

            // Register broadcast receiver for download completion
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        ctx?.unregisterReceiver(this)
                        isUpdatingApp = false

                        // Open the APK for installation
                        val apkFile = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            apkAsset.name
                        )
                        openApkForInstallation(apkFile)
                        onComplete?.invoke()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            isUpdatingApp = false
            Log.e(tag, "Error downloading app update", e)
            onError?.invoke(e.message ?: "Unknown error")
        }
    }

    /**
     * Find APK asset matching device architecture
     */
    private fun findApkAsset(assets: List<GithubReleaseAsset>): GithubReleaseAsset? {
        val deviceAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        // First try to find architecture-specific APK
        var asset = assets.firstOrNull { 
            it.name.endsWith(".apk", ignoreCase = true) && 
            it.name.contains(deviceAbi, ignoreCase = true) 
        }

        // Fall back to universal APK
        if (asset == null) {
            asset = assets.firstOrNull { 
                it.name.endsWith(".apk", ignoreCase = true) && 
                (it.name.contains("universal", ignoreCase = true) || 
                 !it.name.contains("arm", ignoreCase = true) && 
                 !it.name.contains("x86", ignoreCase = true))
            }
        }

        // Last resort: any APK
        if (asset == null) {
            asset = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        }

        return asset
    }

    /**
     * Open APK file for installation
     */
    private fun openApkForInstallation(apkFile: File) {
        if (!apkFile.exists()) {
            Log.e(tag, "APK file does not exist: ${apkFile.absolutePath}")
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }

    /**
     * Skip a specific app version
     */
    fun skipAppVersion(version: String) {
        val skippedVersions = sharedPreferences.getString(PREF_SKIP_APP_VERSIONS, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toMutableList() ?: mutableListOf()

        if (!skippedVersions.contains(version)) {
            skippedVersions.add(version)
            sharedPreferences.edit()
                .putString(PREF_SKIP_APP_VERSIONS, skippedVersions.joinToString(","))
                .apply()
        }
    }

    // ==================== LIBRARY UPDATE METHODS ====================

    /**
     * Data class for library update response
     */
    data class LibraryUpdateResponse(
        val status: LibraryUpdateStatus,
        val message: String = "",
        val currentVersion: String = "",
        val latestVersion: String = "",
        val releaseNotes: String = "",
        val releaseUrl: String = ""
    )

    enum class LibraryUpdateStatus {
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        CHECKING,
        ERROR
    }

    /**
     * Check for NewPipe Extractor updates
     */
    suspend fun checkNewPipeUpdate(): LibraryUpdateResponse = withContext(Dispatchers.IO) {
        try {
            val releases = getGithubReleases(NEWPIPE_RELEASES_URL)
            if (releases.isEmpty()) {
                return@withContext LibraryUpdateResponse(
                    status = LibraryUpdateStatus.ERROR,
                    message = "Network error: Unable to fetch releases"
                )
            }

            val latestRelease = releases.first { !it.prerelease && !it.draft }
            val latestVersion = latestRelease.tagName.removePrefix("v")
            val currentVersion = getCurrentNewPipeVersion()

            val needsUpdate = compareVersions(currentVersion, latestVersion) < 0

            if (needsUpdate) {
                LibraryUpdateResponse(
                    status = LibraryUpdateStatus.UPDATE_AVAILABLE,
                    message = "Update available: $latestVersion",
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    releaseNotes = latestRelease.body,
                    releaseUrl = latestRelease.htmlUrl
                )
            } else {
                LibraryUpdateResponse(
                    status = LibraryUpdateStatus.UP_TO_DATE,
                    message = "NewPipe Extractor is up to date",
                    currentVersion = currentVersion,
                    latestVersion = latestVersion
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking NewPipe update", e)
            LibraryUpdateResponse(
                status = LibraryUpdateStatus.ERROR,
                message = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Get current NewPipe Extractor version from build config
     * This should be updated when the library is updated in libs.versions.toml
     */
    private fun getCurrentNewPipeVersion(): String {
        return "0.25.1" // Matches libs.versions.toml
    }

    /**
     * Skip a specific library version
     */
    fun skipLibraryVersion(version: String) {
        val skippedVersions = sharedPreferences.getString(PREF_SKIP_LIBRARY_VERSIONS, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toMutableList() ?: mutableListOf()

        if (!skippedVersions.contains(version)) {
            skippedVersions.add(version)
            sharedPreferences.edit()
                .putString(PREF_SKIP_LIBRARY_VERSIONS, skippedVersions.joinToString(","))
                .apply()
        }
    }

    // ==================== COMMON UTILITY METHODS ====================

    /**
     * Fetch GitHub releases from API
     */
    private fun getGithubReleases(apiUrl: String): List<GithubRelease> {
        var releases = emptyList<GithubRelease>()
        var connection: HttpURLConnection? = null

        try {
            val url = URL(apiUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "SyncTax-Android/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode < 300) {
                val typeToken = object : TypeToken<List<GithubRelease>>() {}.type
                releases = Gson().fromJson(InputStreamReader(connection.inputStream), typeToken)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching GitHub releases from $apiUrl", e)
        } finally {
            connection?.disconnect()
        }

        return releases
    }

    /**
     * Compare semantic version strings
     * Returns: negative if v1 < v2, 0 if equal, positive if v1 > v2
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
     * Record the last update check time
     */
    fun recordUpdateCheck() {
        sharedPreferences.edit()
            .putLong(PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get time since last update check in hours
     */
    fun getHoursSinceLastCheck(): Long {
        val lastCheck = sharedPreferences.getLong(PREF_LAST_UPDATE_CHECK, 0)
        if (lastCheck == 0L) return Long.MAX_VALUE
        return (System.currentTimeMillis() - lastCheck) / (1000 * 60 * 60)
    }

    /**
     * Check if should auto-check for updates (not more than once per 24 hours)
     */
    fun shouldAutoCheck(): Boolean {
        val autoCheck = sharedPreferences.getBoolean(PREF_CHECK_UPDATE_ON_START, true)
        return autoCheck && getHoursSinceLastCheck() >= 24
    }
}
