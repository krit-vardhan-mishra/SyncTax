package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.just_for_fun.synctax.data.local.entities.SongTransition
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for song transitions (Markov chain edges).
 * Used for sequence-based recommendations.
 */
@Dao
interface SongTransitionDao {

    /**
     * Get all transitions from a specific song, ordered by weight (probability)
     */
    @Query("""
        SELECT * FROM song_transitions 
        WHERE fromSongId = :songId 
        ORDER BY weight DESC
    """)
    suspend fun getTransitionsFrom(songId: String): List<SongTransition>

    /**
     * Get top transitions from a song (most likely next songs)
     */
    @Query("""
        SELECT * FROM song_transitions 
        WHERE fromSongId = :songId 
        ORDER BY weight DESC 
        LIMIT :limit
    """)
    suspend fun getTopTransitionsFrom(songId: String, limit: Int = 10): List<SongTransition>

    /**
     * Get a specific transition between two songs
     */
    @Query("SELECT * FROM song_transitions WHERE fromSongId = :fromId AND toSongId = :toId")
    suspend fun getTransition(fromId: String, toId: String): SongTransition?

    /**
     * Insert a new transition
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransition(transition: SongTransition)

    /**
     * Update an existing transition
     */
    @Update
    suspend fun updateTransition(transition: SongTransition)

    /**
     * Increment play count and update weight for a transition (positive reinforcement)
     * Weight formula: weight = weight + learning_rate (0.1 default)
     */
    @Query("""
        UPDATE song_transitions 
        SET weight = weight + :learningRate, 
            playCount = playCount + 1, 
            lastOccurred = :timestamp,
            avgCompletionRate = (avgCompletionRate * playCount + :completionRate) / (playCount + 1)
        WHERE fromSongId = :fromId AND toSongId = :toId
    """)
    suspend fun reinforceTransition(
        fromId: String, 
        toId: String, 
        learningRate: Float = 0.1f, 
        completionRate: Float = 1.0f,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Apply skip penalty to a transition (negative reinforcement)
     * Weight formula: weight = weight * decay_factor (0.5 default)
     */
    @Query("""
        UPDATE song_transitions 
        SET weight = weight * :decayFactor, 
            skipCount = skipCount + 1,
            lastOccurred = :timestamp
        WHERE fromSongId = :fromId AND toSongId = :toId
    """)
    suspend fun penalizeTransition(
        fromId: String, 
        toId: String, 
        decayFactor: Float = 0.5f,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get total weight sum from a song (for probability calculation)
     */
    @Query("SELECT COALESCE(SUM(weight), 0) FROM song_transitions WHERE fromSongId = :songId")
    suspend fun getTotalWeightFrom(songId: String): Float

    /**
     * Calculate transition probability: P(A → B) = weight(A→B) / Σ(weights from A)
     */
    @Query("""
        SELECT weight / (SELECT COALESCE(SUM(weight), 1) FROM song_transitions WHERE fromSongId = :fromId)
        FROM song_transitions 
        WHERE fromSongId = :fromId AND toSongId = :toId
    """)
    suspend fun getTransitionProbability(fromId: String, toId: String): Float?

    /**
     * Get all transitions to a specific song (for reverse analysis)
     */
    @Query("SELECT * FROM song_transitions WHERE toSongId = :songId ORDER BY weight DESC")
    suspend fun getTransitionsTo(songId: String): List<SongTransition>

    /**
     * Delete transitions involving a song (when song is deleted)
     */
    @Query("DELETE FROM song_transitions WHERE fromSongId = :songId OR toSongId = :songId")
    suspend fun deleteTransitionsForSong(songId: String)

    /**
     * Delete transitions for multiple songs
     */
    @Query("DELETE FROM song_transitions WHERE fromSongId IN (:songIds) OR toSongId IN (:songIds)")
    suspend fun deleteTransitionsForSongs(songIds: List<String>)

    /**
     * Prune weak transitions (cleanup old/unused edges)
     * Removes transitions with very low weight that haven't been used recently
     */
    @Query("""
        DELETE FROM song_transitions 
        WHERE weight < :minWeight 
        AND lastOccurred < :beforeTimestamp
    """)
    suspend fun pruneWeakTransitions(minWeight: Float = 0.1f, beforeTimestamp: Long)

    /**
     * Apply time decay to all transitions (optional periodic decay)
     * Formula: weight = weight * e^(-λ * days_since_last)
     */
    @Query("""
        UPDATE song_transitions 
        SET weight = weight * :decayMultiplier
        WHERE lastOccurred < :beforeTimestamp
    """)
    suspend fun applyTimeDecay(decayMultiplier: Float, beforeTimestamp: Long)

    /**
     * Get total number of transitions
     */
    @Query("SELECT COUNT(*) FROM song_transitions")
    suspend fun getTransitionCount(): Int

    /**
     * Get strongly connected songs (high transition weight both ways)
     */
    @Query("""
        SELECT t1.* FROM song_transitions t1
        INNER JOIN song_transitions t2 
        ON t1.fromSongId = t2.toSongId AND t1.toSongId = t2.fromSongId
        WHERE t1.weight > :minWeight AND t2.weight > :minWeight
        ORDER BY (t1.weight + t2.weight) DESC
        LIMIT :limit
    """)
    suspend fun getStronglyConnectedTransitions(minWeight: Float = 0.5f, limit: Int = 50): List<SongTransition>

    /**
     * Get flow to observe transition updates
     */
    @Query("SELECT * FROM song_transitions ORDER BY lastOccurred DESC LIMIT :limit")
    fun observeRecentTransitions(limit: Int = 100): Flow<List<SongTransition>>
}
