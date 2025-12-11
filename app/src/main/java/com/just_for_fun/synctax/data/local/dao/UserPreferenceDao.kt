package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.just_for_fun.synctax.data.local.entities.UserPreference
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
    
    @Query("DELETE FROM user_preferences WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)

    @Query("DELETE FROM user_preferences WHERE songId IN (:songIds)")
    suspend fun deletePreferencesForSongs(songIds: List<String>)
}
