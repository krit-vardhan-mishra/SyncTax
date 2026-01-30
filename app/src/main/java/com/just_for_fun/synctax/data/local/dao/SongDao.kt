package com.just_for_fun.synctax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC")
    suspend fun getAllSongsList(): List<Song>

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

    // Favorites queries
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY addedTimestamp DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("UPDATE songs SET isFavorite = NOT isFavorite WHERE id = :songId")
    suspend fun toggleFavorite(songId: String)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    // Most played songs query - joins with listening_history to get play counts
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT songId, COUNT(*) as playCount 
            FROM listening_history 
            WHERE songId NOT LIKE 'online:%'
            GROUP BY songId 
            ORDER BY playCount DESC 
            LIMIT :limit
        ) h ON s.id = h.songId
        ORDER BY h.playCount DESC
    """)
    suspend fun getMostPlayedSongs(limit: Int = 10): List<Song>

    // Album-related queries
    @Query("SELECT * FROM songs WHERE album = :albumName")
    fun getSongsByAlbum(albumName: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE album = :albumName")
    suspend fun getSongsByAlbumList(albumName: String): List<Song>

    @Query("""
        SELECT album, artist, COUNT(*) as songCount, MIN(albumArtUri) as albumArt
        FROM songs 
        WHERE album IS NOT NULL AND album != ''
        GROUP BY album, artist
        ORDER BY album ASC
    """)
    suspend fun getAlbums(): List<AlbumInfo>

    @Query("""
        SELECT album, artist, COUNT(*) as songCount, MIN(albumArtUri) as albumArt
        FROM songs 
        WHERE album IS NOT NULL AND album != ''
        GROUP BY album, artist
        ORDER BY songCount DESC
        LIMIT :limit
    """)
    suspend fun getTopAlbums(limit: Int = 10): List<AlbumInfo>
}

/**
 * Data class for album information from grouped query
 */
data class AlbumInfo(
    val album: String,
    val artist: String,
    val songCount: Int,
    val albumArt: String?
)
