package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.just_for_fun.synctax.data.local.entities.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: Int): Playlist?
    
    @Query("SELECT * FROM playlists WHERE playlistUrl = :url")
    suspend fun getPlaylistByUrl(url: String): Playlist?
    
    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Query("UPDATE playlists SET songCount = :count, updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun updateSongCount(playlistId: Int, count: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Int)
    
    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()
}
