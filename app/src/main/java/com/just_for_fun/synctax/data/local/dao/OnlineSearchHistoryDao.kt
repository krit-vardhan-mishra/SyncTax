package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.just_for_fun.synctax.data.local.entities.OnlineSearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlineSearchHistoryDao {
    
    /**
     * Get the last N search queries ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM online_search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<OnlineSearchHistory>>
    
    /**
     * Insert a new search query
     * Delete existing entry with same query first to move it to top
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(history: OnlineSearchHistory)
    
    /**
     * Delete existing record with the same query to prevent duplicates
     */
    @Query("DELETE FROM online_search_history WHERE query = :query")
    suspend fun deleteByQuery(query: String)
    
    /**
     * Delete a specific search history entry by ID
     */
    @Query("DELETE FROM online_search_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Delete oldest records to maintain only N records
     */
    @Query("DELETE FROM online_search_history WHERE id NOT IN (SELECT id FROM online_search_history ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimOldRecords(limit: Int = 20)
    
    /**
     * Clear all search history
     */
    @Query("DELETE FROM online_search_history")
    suspend fun clearAll()
}
