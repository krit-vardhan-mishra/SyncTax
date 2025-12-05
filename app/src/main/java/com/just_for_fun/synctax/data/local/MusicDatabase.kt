package com.just_for_fun.synctax.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.just_for_fun.synctax.data.local.dao.ListeningHistoryDao
import com.just_for_fun.synctax.data.local.dao.OnlineListeningHistoryDao
import com.just_for_fun.synctax.data.local.dao.OnlineSearchHistoryDao
import com.just_for_fun.synctax.data.local.dao.OnlineSongDao
import com.just_for_fun.synctax.data.local.dao.PlaylistDao
import com.just_for_fun.synctax.data.local.dao.PlaylistSongDao
import com.just_for_fun.synctax.data.local.dao.SongDao
import com.just_for_fun.synctax.data.local.dao.UserPreferenceDao
import com.just_for_fun.synctax.data.local.entities.ListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSearchHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.local.entities.Playlist
import com.just_for_fun.synctax.data.local.entities.PlaylistSong
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.local.entities.UserPreference

@Database(
    entities = [
        Song::class,
        ListeningHistory::class,
        UserPreference::class,
        OnlineListeningHistory::class,
        OnlineSearchHistory::class,
        Playlist::class,
        OnlineSong::class,
        PlaylistSong::class
    ],
    version = 6,  // Incremented for adding Playlist, OnlineSong, PlaylistSong
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun onlineListeningHistoryDao(): OnlineListeningHistoryDao
    abstract fun onlineSearchHistoryDao(): OnlineSearchHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun onlineSongDao(): OnlineSongDao
    abstract fun playlistSongDao(): PlaylistSongDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
