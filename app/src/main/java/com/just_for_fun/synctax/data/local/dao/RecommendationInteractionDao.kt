package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.just_for_fun.synctax.data.local.entities.RecommendationInteraction

@Dao
interface RecommendationInteractionDao {
    
    @Insert
    suspend fun insert(interaction: RecommendationInteraction)
    
    @Query("SELECT * FROM recommendation_interactions ORDER BY timestamp DESC")
    suspend fun getAllInteractions(): List<RecommendationInteraction>
    
    @Query("SELECT * FROM recommendation_interactions WHERE songId = :songId ORDER BY timestamp DESC")
    suspend fun getSongInteractions(songId: String): List<RecommendationInteraction>
    
    @Query("SELECT COUNT(*) FROM recommendation_interactions WHERE action = 'played' AND source = :source")
    suspend fun getPlayCountForSource(source: String): Int
    
    @Query("DELETE FROM recommendation_interactions WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldInteractions(beforeTimestamp: Long)
}
