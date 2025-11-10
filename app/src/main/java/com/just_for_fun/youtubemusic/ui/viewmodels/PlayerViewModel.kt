package com.just_for_fun.youtubemusic.ui.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.core.data.preferences.PlayerPreferences
import com.just_for_fun.youtubemusic.core.data.repository.MusicRepository
import com.just_for_fun.youtubemusic.core.player.MusicPlayer
import com.just_for_fun.youtubemusic.core.player.PlaybackCollector
import com.just_for_fun.youtubemusic.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val player = MusicPlayer(application)
    private val playbackCollector = PlaybackCollector(repository, player)
    private val playerPreferences = PlayerPreferences(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentPlaylist: List<Song> = emptyList()
    private var currentIndex: Int = 0
    private var playHistory: MutableList<Song> = mutableListOf()

    // Service binding
    private var musicService: MusicService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true
            // Update notification with current state
            updateNotification()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isServiceBound = false
        }
    }

    // Broadcast receiver for service actions
    private val serviceActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.ACTION_PLAY -> togglePlayPause()
                MusicService.ACTION_PAUSE -> togglePlayPause()
                MusicService.ACTION_NEXT -> next()
                MusicService.ACTION_PREVIOUS -> previous()
                MusicService.ACTION_STOP -> {
                    player.pause()
                    updateNotification()
                }
                MusicService.ACTION_SEEK_TO -> {
                    val position = intent.getLongExtra("position", 0L)
                    seekTo(position)
                }
            }
        }
    }

    init {
        // Bind to music service
        val intent = Intent(application, MusicService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        application.startService(intent)

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(MusicService.ACTION_PLAY)
            addAction(MusicService.ACTION_PAUSE)
            addAction(MusicService.ACTION_NEXT)
            addAction(MusicService.ACTION_PREVIOUS)
            addAction(MusicService.ACTION_STOP)
            addAction(MusicService.ACTION_SEEK_TO)
        }
        application.registerReceiver(serviceActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        viewModelScope.launch {
            player.playerState.collect { playerState ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering,
                    duration = playerState.duration
                )

                // Update notification when playback state changes
                updateNotification()

                // Handle song end
                if (playerState.isEnded) {
                    onSongEnded()
                }
            }
        }

        // Also collect position updates from player
        viewModelScope.launch {
            player.currentPosition.collect { position ->
                _uiState.value = _uiState.value.copy(
                    position = position
                )

                // Update notification periodically (every second)
                if (position % 1000 < 100) {
                    updateNotification()
                }

                // Save current state periodically
                _uiState.value.currentSong?.let { song ->
                    if (position % 5000 < 100) { // Save every ~5 seconds
                        playerPreferences.saveCurrentSong(
                            songId = song.id,
                            position = position,
                            isPlaying = _uiState.value.isPlaying
                        )
                    }
                }
            }
        }

        // Collect volume updates from player
        viewModelScope.launch {
            player.volume.collect { volume ->
                _uiState.value = _uiState.value.copy(volume = volume)
            }
        }

        // Restore last playing song
        viewModelScope.launch {
            restoreLastSong()
        }
    }

    private suspend fun restoreLastSong() {
        val lastSongId = playerPreferences.getCurrentSongId() ?: return
        val lastPosition = playerPreferences.getLastPosition()
        val playlistIds = playerPreferences.getCurrentPlaylist()
        val savedIndex = playerPreferences.getCurrentIndex()

        try {
            val song = repository.getSongById(lastSongId)
            if (song != null) {
                // Restore playlist if available
                if (playlistIds.isNotEmpty()) {
                    val playlist = playlistIds.mapNotNull { id -> repository.getSongById(id) }
                    if (playlist.isNotEmpty()) {
                        currentPlaylist = playlist
                        currentIndex = savedIndex.coerceIn(0, playlist.size - 1)
                        // Ensure the current song is at the current index
                        if (currentIndex < playlist.size && playlist[currentIndex].id == song.id) {
                            // Good
                        } else {
                            // Find the song in playlist
                            val songIndex = playlist.indexOf(song)
                            if (songIndex != -1) {
                                currentIndex = songIndex
                            } else {
                                // Add it to playlist
                                currentPlaylist = playlist + song
                                currentIndex = playlist.size
                            }
                        }
                    } else {
                        currentPlaylist = listOf(song)
                        currentIndex = 0
                    }
                } else {
                    currentPlaylist = listOf(song)
                    currentIndex = 0
                }

                player.prepare(song.filePath, song.id)
                player.seekTo(lastPosition)

                _uiState.value = _uiState.value.copy(
                    currentSong = song,
                    position = lastPosition,
                    isPlaying = false // Don't auto-play on restore
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePlaylistState() {
        viewModelScope.launch {
            playerPreferences.saveCurrentPlaylist(
                songIds = currentPlaylist.map { it.id },
                currentIndex = currentIndex
            )
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        viewModelScope.launch {
            // Stop current song
            if (_uiState.value.currentSong != null) {
                playbackCollector.stopCollecting(skipped = true)
                // Add current song to history
                _uiState.value.currentSong?.let { playHistory.add(it) }
            }

            currentPlaylist = playlist
            currentIndex = playlist.indexOf(song)

            player.prepare(song.filePath, song.id)
            player.play()

            playbackCollector.startCollecting(song.id)

            _uiState.value = _uiState.value.copy(
                currentSong = song,
                isPlaying = true
            )

            // Update notification with new song
            updateNotification()

            // Save the new song state
            playerPreferences.saveCurrentSong(
                songId = song.id,
                position = 0L,
                isPlaying = true
            )

            // Save the playlist
            savePlaylistState()
        }
    }

    fun getUpcomingQueue(): List<Song> {
        if (currentPlaylist.isEmpty()) return emptyList()
        return if (currentIndex < currentPlaylist.size - 1) {
            currentPlaylist.subList(currentIndex + 1, currentPlaylist.size)
        } else {
            emptyList()
        }
    }

    fun getPlayHistory(): List<Song> {
        return playHistory.toList()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            player.pause()
            musicService?.updatePlaybackState(
                _uiState.value.currentSong,
                false,
                _uiState.value.position,
                _uiState.value.duration
            )
        } else {
            player.play()

            _uiState.value.currentSong?.let { song ->
                playbackCollector.startCollecting(song.id)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(position = positionMs)
    }

    fun next() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = true)
            val shuffle = _uiState.value.shuffleEnabled
            if (currentPlaylist.isEmpty()) return@launch

            if (shuffle) {
                // Pick a random index different from currentIndex if possible
                val indices = currentPlaylist.indices.toList()
                val candidates = if (indices.size > 1) indices - currentIndex else indices
                currentIndex = candidates.random()
                playSong(currentPlaylist[currentIndex], currentPlaylist)
            } else {
                if (currentIndex < currentPlaylist.size - 1) {
                    currentIndex++
                    val nextSong = currentPlaylist[currentIndex]
                    playSong(nextSong, currentPlaylist)
                }
            }
        }
    }

    fun previous() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = true)
            val shuffle = _uiState.value.shuffleEnabled
            if (currentPlaylist.isEmpty()) return@launch

            if (shuffle) {
                val indices = currentPlaylist.indices.toList()
                val candidates = if (indices.size > 1) indices - currentIndex else indices
                currentIndex = candidates.random()
                playSong(currentPlaylist[currentIndex], currentPlaylist)
            } else {
                if (currentIndex > 0) {
                    currentIndex--
                    val previousSong = currentPlaylist[currentIndex]
                    playSong(previousSong, currentPlaylist)
                }
            }
        }
    }

    private fun onSongEnded() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = false)
            val shuffle = _uiState.value.shuffleEnabled
            val repeat = _uiState.value.repeatEnabled
            if (currentPlaylist.isEmpty()) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
                return@launch
            }

            if (repeat) {
                // Repeat current song
                _uiState.value.currentSong?.let { song ->
                    playSong(song, currentPlaylist)
                }
            } else if (shuffle) {
                val indices = currentPlaylist.indices.toList()
                val candidates = if (indices.size > 1) indices - currentIndex else indices
                currentIndex = candidates.random()
                playSong(currentPlaylist[currentIndex], currentPlaylist)
            } else {
                // Auto-play next song in order
                if (currentIndex < currentPlaylist.size - 1) {
                    currentIndex++
                    val nextSong = currentPlaylist[currentIndex]
                    playSong(nextSong, currentPlaylist)
                } else {
                    _uiState.value = _uiState.value.copy(isPlaying = false)
                }
            }
        }
    }

    fun toggleShuffle() {
        val enabled = !_uiState.value.shuffleEnabled
        _uiState.value = _uiState.value.copy(shuffleEnabled = enabled)

        // When enabling shuffle, fetch all songs from device, shuffle them and start playing
        if (enabled) {
            viewModelScope.launch {
                // Get all songs from the device (first emission only)
                val allSongs = repository.getAllSongs().first()
                if (allSongs.isNotEmpty()) {
                    // Shuffle all songs
                    val shuffledPlaylist = allSongs.shuffled()
                    currentPlaylist = shuffledPlaylist

                    // Start playing from the first song in the shuffled list
                    currentIndex = 0
                    playSong(shuffledPlaylist[currentIndex], shuffledPlaylist)
                }
            }
        } else {
            // When disabling shuffle, keep current playlist but reset index to current song
            _uiState.value.currentSong?.let { currentSong ->
                currentIndex = currentPlaylist.indexOf(currentSong)
                savePlaylistState()
            }
        }
    }

    fun toggleRepeat() {
        val enabled = !_uiState.value.repeatEnabled
        _uiState.value = _uiState.value.copy(repeatEnabled = enabled)
    }

    fun shufflePlay(playlist: List<Song>) {
        viewModelScope.launch {
            if (playlist.isEmpty()) return@launch
            currentPlaylist = playlist
            val indices = playlist.indices
            currentIndex = if (indices.isEmpty()) 0 else indices.random()
            _uiState.value = _uiState.value.copy(shuffleEnabled = true)
            playSong(playlist[currentIndex], playlist)
        }
    }

    fun removeFromQueue(song: Song) {
        viewModelScope.launch {
            val songIndex = currentPlaylist.indexOf(song)
            if (songIndex != -1) {
                // If removing current song, stop playback
                if (songIndex == currentIndex) {
                    player.pause()
                    playbackCollector.stopCollecting(skipped = true)
                    _uiState.value = _uiState.value.copy(
                        currentSong = null,
                        isPlaying = false
                    )
                } else if (songIndex < currentIndex) {
                    // If removing song before current index, adjust current index
                    currentIndex--
                }

                // Remove from playlist
                currentPlaylist = currentPlaylist.toMutableList().apply { removeAt(songIndex) }
                savePlaylistState()
            }
        }
    }

    fun placeNext(song: Song) {
        viewModelScope.launch {
            // Remove song from current position if it's already in the queue
            val currentPosition = currentPlaylist.indexOf(song)
            currentPlaylist = currentPlaylist.toMutableList().apply {
                if (currentPosition != -1) {
                    removeAt(currentPosition)
                    // Adjust current index if necessary
                    if (currentPosition < currentIndex) {
                        currentIndex--
                    } else if (currentPosition == currentIndex) {
                        // If we're moving the current song, keep it as current
                        currentIndex = 0
                    }
                }
            }

            // Insert song at position after current song
            val insertPosition = currentIndex + 1
            currentPlaylist = currentPlaylist.toMutableList().apply {
                add(insertPosition, song)
            }
            savePlaylistState()
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            if (fromIndex in currentPlaylist.indices && toIndex in currentPlaylist.indices) {
                val mutablePlaylist = currentPlaylist.toMutableList()
                val song = mutablePlaylist.removeAt(fromIndex)
                mutablePlaylist.add(toIndex, song)

                // Update current index if necessary
                when {
                    fromIndex == currentIndex -> currentIndex = toIndex
                    fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex--
                    fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex++
                }

                currentPlaylist = mutablePlaylist
                savePlaylistState()
            }
        }
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume)
    }

    fun getVolume(): Float {
        return player.getVolume()
    }

    private fun updateNotification() {
        musicService?.updatePlaybackState(
            _uiState.value.currentSong,
            _uiState.value.isPlaying,
            _uiState.value.position,
            _uiState.value.duration
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(serviceActionReceiver)
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
        viewModelScope.launch {
            playbackCollector.stopCollecting()
        }
        player.release()
    }
}

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val volume: Float = 1.0f
)