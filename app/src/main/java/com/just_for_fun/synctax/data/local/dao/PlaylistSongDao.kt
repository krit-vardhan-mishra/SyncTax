package com.just_for_fun.synctax.data.local.dao

import androidx.room.*
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.local.entities.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongDao {
    
    @Query("""
        SELECT os.* FROM online_songs os
        INNER JOIN playlist_songs ps ON os.id = ps.onlineSongId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getSongsForPlaylist(playlistId: Int): Flow<List<OnlineSong>>
    
    @Query("""
        SELECT os.* FROM online_songs os
        INNER JOIN playlist_songs ps ON os.id = ps.onlineSongId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    suspend fun getSongsForPlaylistSync(playlistId: Int): List<OnlineSong>
    
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongEntries(playlistId: Int): List<PlaylistSong>
    
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCountForPlaylist(playlistId: Int): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSong)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(playlistSongs: List<PlaylistSong>)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND onlineSongId = :songId")
    suspend fun removeFromPlaylist(playlistId: Int, songId: Int)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Int)
    
    @Query("UPDATE playlist_songs SET position = :newPosition WHERE playlistId = :playlistId AND onlineSongId = :songId")
    suspend fun updateSongPosition(playlistId: Int, songId: Int, newPosition: Int)
    
    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Int): Int?
}
