package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.utils.UpdateUtil
import com.just_for_fun.synctax.data.model.GithubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for handling app update checks and downloads
 * Inspired by ytdlnis SettingsViewModel and UpdateUtil pattern
 */
class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val updateUtil = UpdateUtil(application)

    // App update state
    private val _appUpdateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val appUpdateState: StateFlow<AppUpdateState> = _appUpdateState.asStateFlow()

    // Library update state
    private val _libraryUpdateState = MutableStateFlow<LibraryUpdateState>(LibraryUpdateState.Idle)
    val libraryUpdateState: StateFlow<LibraryUpdateState> = _libraryUpdateState.asStateFlow()

    // All releases for changelog
    private val _allReleases = MutableStateFlow<List<GithubRelease>>(emptyList())
    val allReleases: StateFlow<List<GithubRelease>> = _allReleases.asStateFlow()

    // Download progress
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    // Read Me content
    private val _readmeContent = MutableStateFlow<String?>(null)
    val readmeContent: StateFlow<String?> = _readmeContent.asStateFlow()

    /**
     * Check for app updates
     */
    fun checkForAppUpdate() {
        viewModelScope.launch {
            _appUpdateState.value = AppUpdateState.Checking

            val result = withContext(Dispatchers.IO) {
                updateUtil.checkForAppUpdate()
            }

            _appUpdateState.value = if (result.isSuccess) {
                AppUpdateState.UpdateAvailable(result.getOrNull()!!)
            } else {
                val message = result.exceptionOrNull()?.message ?: "Unknown error"
                if (message.contains("latest version", ignoreCase = true)) {
                    AppUpdateState.UpToDate
                } else {
                    AppUpdateState.Error(message)
                }
            }

            // Record check time
            updateUtil.recordUpdateCheck()
        }
    }

    /**
     * Check for library updates (NewPipe Extractor)
     */
    fun checkForLibraryUpdates() {
        viewModelScope.launch {
            _libraryUpdateState.value = LibraryUpdateState.Checking

            val result = updateUtil.checkNewPipeUpdate()

            _libraryUpdateState.value = when (result.status) {
                UpdateUtil.LibraryUpdateStatus.UPDATE_AVAILABLE -> {
                    LibraryUpdateState.UpdateAvailable(
                        currentVersion = result.currentVersion,
                        latestVersion = result.latestVersion,
                        releaseNotes = result.releaseNotes,
                        releaseUrl = result.releaseUrl
                    )
                }
                UpdateUtil.LibraryUpdateStatus.UP_TO_DATE -> {
                    LibraryUpdateState.UpToDate(
                        currentVersion = result.currentVersion
                    )
                }
                UpdateUtil.LibraryUpdateStatus.ERROR -> {
                    LibraryUpdateState.Error(result.message)
                }
                else -> LibraryUpdateState.Idle
            }
        }
    }

    /**
     * Check for both app and library updates
     */
    fun checkAllUpdates() {
        checkForAppUpdate()
        checkForLibraryUpdates()
    }

    /**
     * Download and install app update
     */
    fun downloadAppUpdate(release: GithubRelease) {
        _appUpdateState.value = AppUpdateState.Downloading(0)

        updateUtil.downloadAndInstallAppUpdate(
            release = release,
            onProgress = { progress ->
                _downloadProgress.value = progress
                _appUpdateState.value = AppUpdateState.Downloading(progress)
            },
            onComplete = {
                _appUpdateState.value = AppUpdateState.DownloadComplete
            },
            onError = { error ->
                _appUpdateState.value = AppUpdateState.Error(error)
            }
        )
    }

    /**
     * Skip app version update
     */
    fun skipAppVersion(version: String) {
        updateUtil.skipAppVersion(version)
        _appUpdateState.value = AppUpdateState.Idle
    }

    /**
     * Skip library version update
     */
    fun skipLibraryVersion(version: String) {
        updateUtil.skipLibraryVersion(version)
        _libraryUpdateState.value = LibraryUpdateState.Idle
    }

    /**
     * Load all releases for changelog
     */
    fun loadChangelog() {
        viewModelScope.launch {
            val releases = withContext(Dispatchers.IO) {
                updateUtil.getAppReleases()
            }
            _allReleases.value = releases
        }
    }

    /**
     * Load app README
     */
    fun loadReadme() {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                updateUtil.getAppReadme()
            }
            _readmeContent.value = content ?: "Failed to load Read Me. Verify internet connection."
        }
    }

    /**
     * Reset app update state
     */
    fun resetAppUpdateState() {
        _appUpdateState.value = AppUpdateState.Idle
    }

    /**
     * Reset library update state
     */
    fun resetLibraryUpdateState() {
        _libraryUpdateState.value = LibraryUpdateState.Idle
    }

    /**
     * Check if auto-update check should run
     */
    fun shouldAutoCheck(): Boolean = updateUtil.shouldAutoCheck()

    // ==================== STATE CLASSES ====================

    sealed class AppUpdateState {
        object Idle : AppUpdateState()
        object Checking : AppUpdateState()
        object UpToDate : AppUpdateState()
        data class UpdateAvailable(val release: GithubRelease) : AppUpdateState()
        data class Downloading(val progress: Int) : AppUpdateState()
        object DownloadComplete : AppUpdateState()
        data class Error(val message: String) : AppUpdateState()
    }

    sealed class LibraryUpdateState {
        object Idle : LibraryUpdateState()
        object Checking : LibraryUpdateState()
        data class UpToDate(val currentVersion: String) : LibraryUpdateState()
        data class UpdateAvailable(
            val currentVersion: String,
            val latestVersion: String,
            val releaseNotes: String,
            val releaseUrl: String
        ) : LibraryUpdateState()
        data class Error(val message: String) : LibraryUpdateState()
    }
}
