package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.di.AppModule
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.data.local.entities.*
import com.just_for_fun.synctax.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI state for playlist screen
 */
data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class UrlValidationResult(val isValid: Boolean, val platform: String?)

/**
 * UI state for playlist import
 */
data class PlaylistImportState(
    val isImporting: Boolean = false,
    val isValidating: Boolean = false,
    val isUrlValid: Boolean? = null,
    val platform: String? = null,
    val importSuccess: Boolean? = null,
    val importedPlaylist: Playlist? = null,
    val importedSongCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * UI state for playlist detail
 */
data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val songs: List<OnlineSong> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing playlists, importing from YouTube, and playlist details
 */
class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val playlistRepository = AppModule.providePlaylistRepository(application)

    // Playlist list state
    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    // Import state
    private val _importState = MutableStateFlow(PlaylistImportState())
    val importState: StateFlow<PlaylistImportState> = _importState.asStateFlow()

    // Detail state
    private val _detailState = MutableStateFlow(PlaylistDetailState())
    val detailState: StateFlow<PlaylistDetailState> = _detailState.asStateFlow()

    init {
        loadPlaylists()
    }

    /**
     * Load all playlists from database
     */
    private fun loadPlaylists() {
        viewModelScope.launch(AppDispatchers.Database) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            playlistRepository.getAllPlaylists().collectLatest { playlists ->
                _uiState.value = PlaylistUiState(
                    playlists = playlists,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Reload playlists
     */
    fun reloadPlaylists() {
        loadPlaylists()
    }

    /**
     * Validate a playlist URL
     */
    fun validateUrl(url: String) {
        if (url.isBlank()) {
            _importState.value = _importState.value.copy(
                isUrlValid = null,
                platform = null
            )
            return
        }

        viewModelScope.launch(AppDispatchers.Network) {
            _importState.value = _importState.value.copy(isValidating = true)

            val result = playlistRepository.validatePlaylistUrl(url)

            _importState.value = _importState.value.copy(
                isValidating = false,
                isUrlValid = result.isValid,
                platform = result.platform
            )
        }
    }

    suspend fun validatePlaylistUrl(url: String): UrlValidationResult {
        return when {
            url.contains("open.spotify.com/playlist/") || url.contains("spotify.link") -> {
                UrlValidationResult(isValid = true, platform = "spotify")
            }
            url.contains("youtube.com/playlist") || url.contains("music.youtube.com/playlist") -> {
                UrlValidationResult(isValid = true, platform = "youtube")
            }
            // your existing YouTube checks...
            else -> UrlValidationResult(isValid = false, platform = null)
        }
    }

    /**
     * Import a playlist from URL
     */
    fun importPlaylist(url: String) {
        if (url.isBlank()) {
            _importState.value = _importState.value.copy(
                errorMessage = "Please enter a playlist URL"
            )
            return
        }

        viewModelScope.launch(AppDispatchers.Network) {
            _importState.value = _importState.value.copy(
                isImporting = true,
                importSuccess = null,
                errorMessage = null
            )

            when (val result = playlistRepository.importPlaylist(url)) {
                is PlaylistRepository.ImportResult.Success -> {
                    _importState.value = _importState.value.copy(
                        isImporting = false,
                        importSuccess = true,
                        importedPlaylist = result.playlist,
                        importedSongCount = result.songCount,
                        errorMessage = null
                    )
                }
                is PlaylistRepository.ImportResult.Error -> {
                    _importState.value = _importState.value.copy(
                        isImporting = false,
                        importSuccess = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Reset import state for new import
     */
    fun resetImportState() {
        _importState.value = PlaylistImportState()
    }

    /**
     * Load playlist detail with songs
     */
    fun loadPlaylistDetail(playlistId: Int) {
        viewModelScope.launch(AppDispatchers.Database) {
            _detailState.value = _detailState.value.copy(isLoading = true)

            val playlist = playlistRepository.getPlaylistById(playlistId)

            if (playlist == null) {
                _detailState.value = PlaylistDetailState(
                    isLoading = false,
                    error = "Playlist not found"
                )
                return@launch
            }

            playlistRepository.getSongsForPlaylist(playlistId).collectLatest { songs ->
                _detailState.value = PlaylistDetailState(
                    playlist = playlist,
                    songs = songs,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
 * Delete a playlist
 */
    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch(AppDispatchers.Database) {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    /**
     * Clear detail state
     */
    fun clearDetailState() {
        _detailState.value = PlaylistDetailState()
    }

    /**
     * Save an album as a playlist
     *
     * @param albumName The name of the album
     * @param artistName The artist name
     * @param thumbnailUrl The album thumbnail URL
     * @param songs List of songs in the album
     * @param onResult Callback with result (true if saved, false if failed)
     */
    fun saveAlbumAsPlaylist(
        albumName: String,
        artistName: String,
        thumbnailUrl: String?,
        songs: List<Song>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(AppDispatchers.Database) {
            val result = playlistRepository.saveAlbumAsPlaylist(albumName, artistName, thumbnailUrl, songs)
            onResult(result != null)
        }
    }

    /**
     * Remove a saved album
     *
     * @param albumName The name of the album
     * @param artistName The artist name
     * @param onResult Callback with result (true if removed, false if failed)
     */
    fun unsaveAlbum(
        albumName: String,
        artistName: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(AppDispatchers.Database) {
            val result = playlistRepository.unsaveAlbum(albumName, artistName)
            onResult(result)
        }
    }

    /**
     * Check if an album is saved
     *
     * @param albumName The name of the album
     * @param artistName The artist name
     * @param onResult Callback with result (true if saved, false otherwise)
     */
    fun isAlbumSaved(
        albumName: String,
        artistName: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(AppDispatchers.Database) {
            val result = playlistRepository.isAlbumSaved(albumName, artistName)
            onResult(result)
        }
    }
    /**
     * Create a manually created playlist
     */
    fun createPlaylist(
        name: String,
        offlineSongs: List<Song>,
        onlineSongs: List<OnlineSong>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(AppDispatchers.Database) {
            val result = playlistRepository.createPlaylist(name, offlineSongs, onlineSongs)
            if (result != null) {
                loadPlaylists()
            }
            onResult(result != null)
        }
    }

    /**
     * Add a song to an existing playlist
     */
    fun addSongToPlaylist(
        playlistId: Int,
        song: Song,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(AppDispatchers.Database) {
            val result = playlistRepository.addSongToPlaylist(playlistId, song)
            onResult(result)
        }
    }
}
