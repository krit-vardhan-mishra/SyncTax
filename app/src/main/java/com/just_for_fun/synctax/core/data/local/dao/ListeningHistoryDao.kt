package com.just_for_fun.synctax.core.data.local.dao

import androidx.room.*
import com.just_for_fun.synctax.core.data.local.entities.ListeningHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {
    @Insert
    suspend fun insertHistory(history: ListeningHistory)

    @Query("SELECT * FROM listening_history ORDER BY playTimestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<ListeningHistory>>

    @Query("SELECT * FROM listening_history WHERE songId = :songId ORDER BY playTimestamp DESC")
    fun getHistoryForSong(songId: String): Flow<List<ListeningHistory>>

    @Query("SELECT * FROM listening_history WHERE playTimestamp >= :timestamp")
    suspend fun getHistorySince(timestamp: Long): List<ListeningHistory>

    @Query("DELETE FROM listening_history WHERE playTimestamp < :timestamp")
    suspend fun deleteOldHistory(timestamp: Long)
}