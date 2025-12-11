package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.just_for_fun.synctax.data.local.entities.RecommendationCache

@Dao
interface RecommendationCacheDao {
    
    @Query("SELECT * FROM recommendations_cache WHERE cacheKey = :key AND expiresAt > :currentTime")
    suspend fun getValidCache(key: String, currentTime: Long = System.currentTimeMillis()): RecommendationCache?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: RecommendationCache)
    
    @Query("DELETE FROM recommendations_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)
    
    @Query("DELETE FROM recommendations_cache WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM recommendations_cache WHERE cacheKey = :key")
    suspend fun getCache(key: String): RecommendationCache?
}
