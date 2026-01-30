package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.just_for_fun.synctax.data.local.entities.QuickPick
import com.just_for_fun.synctax.data.local.entities.QuickPickSong
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Quick Pick operations.
 * Handles both seed songs (QuickPick) and recommendation queue (QuickPickSong).
 */
@Dao
interface QuickPickDao {
    
    // ==================== Quick Pick Seeds ====================
    
    /**
     * Get Quick Pick seeds by source (offline/online), ordered by last played
     */
    @Query("SELECT * FROM quick_picks WHERE source = :source ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getQuickPicksBySource(source: String, limit: Int = 10): List<QuickPick>
    
    /**
     * Get all Quick Pick seeds, ordered by last played
     */
    @Query("SELECT * FROM quick_picks ORDER BY lastPlayedAt DESC")
    suspend fun getAllQuickPicks(): List<QuickPick>
    
    /**
     * Get Quick Pick seeds as Flow for reactive updates
     */
    @Query("SELECT * FROM quick_picks WHERE source = :source ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeQuickPicksBySource(source: String, limit: Int = 10): Flow<List<QuickPick>>
    
    /**
     * Insert or replace a Quick Pick seed
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickPick(quickPick: QuickPick)
    
    /**
     * Update an existing Quick Pick
     */
    @Update
    suspend fun updateQuickPick(quickPick: QuickPick)
    
    /**
     * Update play count and last played time for a song
     */
    @Query("UPDATE quick_picks SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE songId = :songId")
    suspend fun updatePlayCount(songId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Get a Quick Pick by song ID
     */
    @Query("SELECT * FROM quick_picks WHERE songId = :songId LIMIT 1")
    suspend fun getQuickPickBySongId(songId: String): QuickPick?
    
    /**
     * Check if any Quick Pick seeds exist for a source
     */
    @Query("SELECT COUNT(*) FROM quick_picks WHERE source = :source")
    suspend fun getQuickPickCount(source: String): Int
    
    /**
     * Delete old Quick Picks that haven't been played in a while
     */
    @Query("DELETE FROM quick_picks WHERE lastPlayedAt < :timestamp")
    suspend fun deleteOldQuickPicks(timestamp: Long)
    
    /**
     * Delete a Quick Pick by ID
     */
    @Query("DELETE FROM quick_picks WHERE id = :id")
    suspend fun deleteQuickPick(id: Long)
    
    // ==================== Quick Pick Queue ====================
    
    /**
     * Get the Quick Pick queue for a source (offline/online)
     */
    @Query("SELECT * FROM quick_pick_songs WHERE source = :source ORDER BY queuePosition ASC LIMIT :limit")
    fun observeQuickPickQueue(source: String, limit: Int = 10): Flow<List<QuickPickSong>>
    
    /**
     * Get the Quick Pick queue as a suspend function
     */
    @Query("SELECT * FROM quick_pick_songs WHERE source = :source ORDER BY queuePosition ASC LIMIT :limit")
    suspend fun getQuickPickQueue(source: String, limit: Int = 10): List<QuickPickSong>
    
    /**
     * Insert multiple Quick Pick songs into the queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickPickSongs(songs: List<QuickPickSong>)
    
    /**
     * Insert a single Quick Pick song
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickPickSong(song: QuickPickSong)
    
    /**
     * Clear the entire queue for a source
     */
    @Query("DELETE FROM quick_pick_songs WHERE source = :source")
    suspend fun clearQuickPickQueue(source: String)
    
    /**
     * Remove a specific song from the queue
     */
    @Query("DELETE FROM quick_pick_songs WHERE id = :id")
    suspend fun removeFromQueue(id: Long)
    
    /**
     * Remove a song from queue by songId
     */
    @Query("DELETE FROM quick_pick_songs WHERE songId = :songId AND source = :source")
    suspend fun removeFromQueueBySongId(songId: String, source: String)
    
    /**
     * Get the current queue size for a source
     */
    @Query("SELECT COUNT(*) FROM quick_pick_songs WHERE source = :source")
    suspend fun getQueueSize(source: String): Int
    
    /**
     * Update queue positions after removing a song
     */
    @Query("UPDATE quick_pick_songs SET queuePosition = queuePosition - 1 WHERE source = :source AND queuePosition > :position")
    suspend fun shiftQueuePositions(source: String, position: Int)
    
    /**
     * Check if a song is already in the queue
     */
    @Query("SELECT COUNT(*) FROM quick_pick_songs WHERE songId = :songId AND source = :source")
    suspend fun isSongInQueue(songId: String, source: String): Int
    
    /**
     * Get the maximum queue position for a source
     */
    @Query("SELECT MAX(queuePosition) FROM quick_pick_songs WHERE source = :source")
    suspend fun getMaxQueuePosition(source: String): Int?
}
