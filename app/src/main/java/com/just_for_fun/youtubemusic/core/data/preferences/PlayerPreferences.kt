package com.just_for_fun.youtubemusic.core.data.preferences

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
    }
}
