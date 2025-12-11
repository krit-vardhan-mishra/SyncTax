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
import com.just_for_fun.synctax.data.local.dao.RecommendationCacheDao
import com.just_for_fun.synctax.data.local.dao.RecommendationInteractionDao
import com.just_for_fun.synctax.data.local.dao.SongDao
import com.just_for_fun.synctax.data.local.dao.UserPreferenceDao
import com.just_for_fun.synctax.data.local.entities.ListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSearchHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.local.entities.Playlist
import com.just_for_fun.synctax.data.local.entities.PlaylistSong
import com.just_for_fun.synctax.data.local.entities.RecommendationCache
import com.just_for_fun.synctax.data.local.entities.RecommendationInteraction
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
        PlaylistSong::class,
        RecommendationCache::class,
        RecommendationInteraction::class
    ],
    version = 8,  // Incremented for isFavorite and userRating fields
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
    abstract fun recommendationCacheDao(): RecommendationCacheDao
    abstract fun recommendationInteractionDao(): RecommendationInteractionDao

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
