package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlineSongDao {
    
    @Query("SELECT * FROM online_songs ORDER BY addedAt DESC")
    fun getAllOnlineSongs(): Flow<List<OnlineSong>>
    
    @Query("SELECT * FROM online_songs WHERE id = :id")
    suspend fun getOnlineSongById(id: Int): OnlineSong?
    
    @Query("SELECT * FROM online_songs WHERE videoId = :videoId")
    suspend fun getOnlineSongByVideoId(videoId: String): OnlineSong?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOnlineSong(song: OnlineSong): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOnlineSongs(songs: List<OnlineSong>): List<Long>
    
    @Update
    suspend fun updateOnlineSong(song: OnlineSong)
    
    @Delete
    suspend fun deleteOnlineSong(song: OnlineSong)
    
    @Query("DELETE FROM online_songs WHERE id = :id")
    suspend fun deleteOnlineSongById(id: Int)
    
    @Query("UPDATE online_songs SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSavedStatus(id: Int, isSaved: Boolean)
    
    @Query("UPDATE online_songs SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun updateDownloadedStatus(id: Int, isDownloaded: Boolean)
    
    @Query("UPDATE online_songs SET isPlayed = :isPlayed WHERE videoId = :videoId")
    suspend fun updatePlayedStatus(videoId: String, isPlayed: Boolean)
    
    @Query("UPDATE online_songs SET isFullyPlayed = :isFullyPlayed WHERE videoId = :videoId")
    suspend fun updateFullyPlayedStatus(videoId: String, isFullyPlayed: Boolean)
    
    @Query("SELECT * FROM online_songs WHERE isPlayed = 1 ORDER BY addedAt DESC")
    fun getPlayedSongs(): Flow<List<OnlineSong>>
    
    @Query("SELECT * FROM online_songs WHERE isFullyPlayed = 1 ORDER BY addedAt DESC")
    fun getFullyPlayedSongs(): Flow<List<OnlineSong>>
    
    @Query("SELECT * FROM online_songs WHERE isSaved = 1")
    fun getSavedSongs(): Flow<List<OnlineSong>>
    
    @Query("SELECT * FROM online_songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<OnlineSong>>
    
    /**
     * Get or insert a song by videoId
     * Returns the existing song's id if found, otherwise inserts and returns new id
     */
    @Transaction
    suspend fun getOrInsertByVideoId(song: OnlineSong): Int {
        val existing = getOnlineSongByVideoId(song.videoId)
        return if (existing != null) {
            existing.id
        } else {
            insertOnlineSong(song).toInt()
        }
    }
}
