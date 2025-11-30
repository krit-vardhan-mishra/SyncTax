package com.just_for_fun.synctax.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.just_for_fun.synctax.core.data.local.entities.OnlineListeningHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlineListeningHistoryDao {
    
    /**
     * Get the last 10 online listened songs ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM online_listening_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentOnlineHistory(): Flow<List<OnlineListeningHistory>>
    
    /**
     * Insert a new online listening record
     * If the table has more than 15 records, delete the oldest ones
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnlineListening(history: OnlineListeningHistory)
    
    /**
     * Delete existing record with the same videoId to prevent duplicates
     */
    @Query("DELETE FROM online_listening_history WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)
    
    /**
     * Delete oldest records to maintain only 15 records
     */
    @Query("DELETE FROM online_listening_history WHERE id NOT IN (SELECT id FROM online_listening_history ORDER BY timestamp DESC LIMIT 15)")
    suspend fun trimOldRecords()
    
    /**
     * Clear all online listening history
     */
    @Query("DELETE FROM online_listening_history")
    suspend fun clearAll()
}
