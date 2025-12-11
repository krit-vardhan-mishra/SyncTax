package com.just_for_fun.synctax.data.preferences

import android.content.Context
import android.content.SharedPreferences

class WidgetPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "widget_state",
        Context.MODE_PRIVATE
    )

    fun savePlaybackState(
        songTitle: String?,
        songArtist: String?,
        songAlbum: String?,
        albumArtUri: String?,
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        shuffleOn: Boolean
    ) {
        prefs.edit().apply {
            putString(KEY_SONG_TITLE, songTitle ?: "")
            putString(KEY_SONG_ARTIST, songArtist ?: "")
            putString(KEY_SONG_ALBUM, songAlbum ?: "")
            putString(KEY_ALBUM_ART_URI, albumArtUri ?: "")
            putBoolean(KEY_IS_PLAYING, isPlaying)
            putLong(KEY_POSITION, position)
            putLong(KEY_DURATION, duration)
            putBoolean(KEY_SHUFFLE_ON, shuffleOn)
            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            apply()
        }
    }

    fun getSongTitle(): String = prefs.getString(KEY_SONG_TITLE, "") ?: ""
    fun getSongArtist(): String = prefs.getString(KEY_SONG_ARTIST, "") ?: ""
    fun getSongAlbum(): String = prefs.getString(KEY_SONG_ALBUM, "") ?: ""
    fun getAlbumArtUri(): String? = prefs.getString(KEY_ALBUM_ART_URI, null)
    fun isPlaying(): Boolean = prefs.getBoolean(KEY_IS_PLAYING, false)
    fun getPosition(): Long = prefs.getLong(KEY_POSITION, 0L)
    fun getDuration(): Long = prefs.getLong(KEY_DURATION, 0L)
    fun isShuffleOn(): Boolean = prefs.getBoolean(KEY_SHUFFLE_ON, false)
    fun getLastUpdate(): Long = prefs.getLong(KEY_LAST_UPDATE, 0L)

    fun clearState() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SONG_TITLE = "song_title"
        private const val KEY_SONG_ARTIST = "song_artist"
        private const val KEY_SONG_ALBUM = "song_album"
        private const val KEY_ALBUM_ART_URI = "album_art_uri"
        private const val KEY_IS_PLAYING = "is_playing"
        private const val KEY_POSITION = "position"
        private const val KEY_DURATION = "duration"
        private const val KEY_SHUFFLE_ON = "shuffle_on"
        private const val KEY_LAST_UPDATE = "last_update"
    }
}
