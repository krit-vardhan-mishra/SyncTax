package com.just_for_fun.synctax.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "player_state",
        Context.MODE_PRIVATE
    )

    suspend fun saveCurrentSong(
        songId: String,
        position: Long,
        isPlaying: Boolean
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_CURRENT_SONG_ID, songId)
            putLong(KEY_POSITION, position)
            putBoolean(KEY_IS_PLAYING, isPlaying)
            apply()
        }
    }

    suspend fun saveCurrentPlaylist(
        songIds: List<String>,
        currentIndex: Int
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_CURRENT_PLAYLIST, songIds.joinToString(","))
            putInt(KEY_CURRENT_INDEX, currentIndex)
            apply()
        }
    }
    
    /**
     * Save online song state for restoration when app reopens
     */
    suspend fun saveOnlineSongState(
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        watchUrl: String,
        position: Long,
        isPlaying: Boolean
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_ONLINE_VIDEO_ID, videoId)
            putString(KEY_ONLINE_TITLE, title)
            putString(KEY_ONLINE_ARTIST, artist)
            putString(KEY_ONLINE_THUMBNAIL_URL, thumbnailUrl)
            putString(KEY_ONLINE_WATCH_URL, watchUrl)
            putLong(KEY_ONLINE_POSITION, position)
            putBoolean(KEY_ONLINE_IS_PLAYING, isPlaying)
            putBoolean(KEY_IS_ONLINE_SONG, true)
            apply()
        }
    }
    
    /**
     * Clear online song state when switching to offline mode
     */
    suspend fun clearOnlineSongState() = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            remove(KEY_ONLINE_VIDEO_ID)
            remove(KEY_ONLINE_TITLE)
            remove(KEY_ONLINE_ARTIST)
            remove(KEY_ONLINE_THUMBNAIL_URL)
            remove(KEY_ONLINE_WATCH_URL)
            remove(KEY_ONLINE_POSITION)
            remove(KEY_ONLINE_IS_PLAYING)
            putBoolean(KEY_IS_ONLINE_SONG, false)
            apply()
        }
    }
    
    /**
     * Check if the last played song was an online song
     */
    fun isOnlineSong(): Boolean {
        return prefs.getBoolean(KEY_IS_ONLINE_SONG, false)
    }
    
    /**
     * Get saved online song state
     */
    fun getOnlineSongState(): OnlineSongState? {
        val videoId = prefs.getString(KEY_ONLINE_VIDEO_ID, null) ?: return null
        return OnlineSongState(
            videoId = videoId,
            title = prefs.getString(KEY_ONLINE_TITLE, "") ?: "",
            artist = prefs.getString(KEY_ONLINE_ARTIST, "") ?: "",
            thumbnailUrl = prefs.getString(KEY_ONLINE_THUMBNAIL_URL, null),
            watchUrl = prefs.getString(KEY_ONLINE_WATCH_URL, "") ?: "",
            position = prefs.getLong(KEY_ONLINE_POSITION, 0L),
            isPlaying = prefs.getBoolean(KEY_ONLINE_IS_PLAYING, false)
        )
    }

    fun getCurrentSongId(): String? {
        return prefs.getString(KEY_CURRENT_SONG_ID, null)
    }

    fun getLastPosition(): Long {
        return prefs.getLong(KEY_POSITION, 0L)
    }

    fun wasPlaying(): Boolean {
        return prefs.getBoolean(KEY_IS_PLAYING, false)
    }

    fun getCurrentPlaylist(): List<String> {
        val playlistStr = prefs.getString(KEY_CURRENT_PLAYLIST, "") ?: ""
        return if (playlistStr.isEmpty()) emptyList() else playlistStr.split(",")
    }

    fun getCurrentIndex(): Int {
        return prefs.getInt(KEY_CURRENT_INDEX, 0)
    }

    fun clearPlayerState() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_CURRENT_SONG_ID = "current_song_id"
        private const val KEY_POSITION = "position"
        private const val KEY_IS_PLAYING = "is_playing"
        private const val KEY_CURRENT_PLAYLIST = "current_playlist"
        private const val KEY_CURRENT_INDEX = "current_index"
        
        // Online song state keys
        private const val KEY_IS_ONLINE_SONG = "is_online_song"
        private const val KEY_ONLINE_VIDEO_ID = "online_video_id"
        private const val KEY_ONLINE_TITLE = "online_title"
        private const val KEY_ONLINE_ARTIST = "online_artist"
        private const val KEY_ONLINE_THUMBNAIL_URL = "online_thumbnail_url"
        private const val KEY_ONLINE_WATCH_URL = "online_watch_url"
        private const val KEY_ONLINE_POSITION = "online_position"
        private const val KEY_ONLINE_IS_PLAYING = "online_is_playing"
    }
}

/**
 * Data class to hold online song state
 */
data class OnlineSongState(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val watchUrl: String,
    val position: Long,
    val isPlaying: Boolean
)
