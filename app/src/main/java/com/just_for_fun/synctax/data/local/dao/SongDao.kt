package com.just_for_fun.synctax.data.local.dao

import androidx.room.*
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getSongsPaginated(limit: Int, offset: Int): List<Song>

    @Query("SELECT * FROM songs WHERE addedTimestamp < :lastTimestamp ORDER BY addedTimestamp DESC LIMIT :limit")
    suspend fun getSongsKeysetPaginated(lastTimestamp: Long, limit: Int): List<Song>

    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC LIMIT :limit")
    suspend fun getSongsChunk(limit: Int): List<Song>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("SELECT * FROM songs WHERE genre = :genre")
    fun getSongsByGenre(genre: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artist")
    fun getSongsByArtist(artist: String): Flow<List<Song>>

    @Query("DELETE FROM songs WHERE id NOT IN (:ids)")
    suspend fun deleteSongsNotIn(ids: List<String>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<String>)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}
