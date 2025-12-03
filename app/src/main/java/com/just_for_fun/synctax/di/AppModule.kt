package com.just_for_fun.synctax.di

import android.content.Context
import com.just_for_fun.synctax.core.chaquopy.ChaquopyMusicAnalyzer
import com.just_for_fun.synctax.core.data.local.MusicDatabase
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import com.just_for_fun.synctax.core.data.repository.PlaylistRepository
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.player.MusicPlayer

/**
 * Simple dependency injection module
 * For a production app, consider using Hilt or Koin
 */
object AppModule {

    private var musicDatabase: MusicDatabase? = null
    private var musicRepository: MusicRepository? = null
    private var playlistRepository: PlaylistRepository? = null
    private var recommendationManager: MusicRecommendationManager? = null
    private var musicPlayer: MusicPlayer? = null
    private var chaquopyAnalyzer: ChaquopyMusicAnalyzer? = null

    fun provideDatabase(context: Context): MusicDatabase {
        return musicDatabase ?: synchronized(this) {
            musicDatabase ?: MusicDatabase.getDatabase(context).also {
                musicDatabase = it
            }
        }
    }

    fun provideRepository(context: Context): MusicRepository {
        return musicRepository ?: synchronized(this) {
            musicRepository ?: MusicRepository(context).also {
                musicRepository = it
            }
        }
    }

    fun providePlaylistRepository(context: Context): PlaylistRepository {
        return playlistRepository ?: synchronized(this) {
            playlistRepository ?: PlaylistRepository(context).also {
                playlistRepository = it
            }
        }
    }

    fun provideRecommendationManager(context: Context): MusicRecommendationManager {
        return recommendationManager ?: synchronized(this) {
            recommendationManager ?: MusicRecommendationManager(context).also {
                recommendationManager = it
            }
        }
    }

    fun provideMusicPlayer(context: Context): MusicPlayer {
        return musicPlayer ?: synchronized(this) {
            musicPlayer ?: MusicPlayer(context).also {
                musicPlayer = it
            }
        }
    }

    fun provideChaquopyAnalyzer(context: Context): ChaquopyMusicAnalyzer {
        return chaquopyAnalyzer ?: synchronized(this) {
            chaquopyAnalyzer ?: ChaquopyMusicAnalyzer.getInstance(context).also {
                chaquopyAnalyzer = it
            }
        }
    }
}