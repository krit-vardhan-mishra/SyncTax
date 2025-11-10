package com.just_for_fun.youtubemusic.core.data.local.dao

import androidx.room.*
import com.just_for_fun.youtubemusic.core.data.local.entities.UserPreference
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences ORDER BY likeScore DESC LIMIT :limit")
    fun getTopPreferences(limit: Int = 50): Flow<List<UserPreference>>

    @Query("SELECT * FROM user_preferences WHERE songId = :songId")
    suspend fun getPreference(songId: String): UserPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)

    @Update
    suspend fun updatePreference(preference: UserPreference)

    @Query("UPDATE user_preferences SET playCount = playCount + 1, lastPlayed = :timestamp WHERE songId = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long)

    @Query("UPDATE user_preferences SET skipCount = skipCount + 1 WHERE songId = :songId")
    suspend fun incrementSkipCount(songId: String)
}