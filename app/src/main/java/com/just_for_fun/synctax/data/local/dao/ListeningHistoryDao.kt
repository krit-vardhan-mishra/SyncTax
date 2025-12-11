package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.just_for_fun.synctax.data.local.entities.ListeningHistory
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
    
    @Query("DELETE FROM listening_history WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)

    @Query("DELETE FROM listening_history WHERE songId IN (:songIds)")
    suspend fun deleteHistoryForSongs(songIds: List<String>)

    // Get most played song IDs with play counts
    @Query("""
        SELECT songId, COUNT(*) as playCount 
        FROM listening_history 
        WHERE songId NOT LIKE 'online:%'
        GROUP BY songId 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostPlayedSongIds(limit: Int = 10): List<SongPlayCountResult>

    // Update user rating for most recent play of a song
    @Query("""
        UPDATE listening_history 
        SET userRating = :rating 
        WHERE id = (
            SELECT id FROM listening_history 
            WHERE songId = :songId 
            ORDER BY playTimestamp DESC 
            LIMIT 1
        )
    """)
    suspend fun updateUserRating(songId: String, rating: Int)

    // Get all history for songs with positive ratings (for ML training)
    @Query("SELECT * FROM listening_history WHERE userRating = 2")
    suspend fun getLikedSongsHistory(): List<ListeningHistory>

    // Get all history for songs with negative ratings (for ML training)
    @Query("SELECT * FROM listening_history WHERE userRating = 1")
    suspend fun getDislikedSongsHistory(): List<ListeningHistory>
}

// Data class for most played query result
data class SongPlayCountResult(
    val songId: String,
    val playCount: Int
)
