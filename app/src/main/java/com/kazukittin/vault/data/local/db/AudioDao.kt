package com.kazukittin.vault.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {

    // Works
    @Query("SELECT * FROM works ORDER BY addedAt DESC")
    fun getAllWorks(): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteWorks(): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works WHERE folderPath = :path")
    fun getWork(path: String): Flow<WorkEntity?>

    @Query("SELECT * FROM works WHERE rjCode IS NOT NULL")
    fun getWorksWithRjCode(): List<WorkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWork(work: WorkEntity)

    @Update
    fun updateWork(work: WorkEntity)

    @Delete
    fun deleteWork(work: WorkEntity)

    // Tracks
    @Query("SELECT * FROM tracks WHERE workFolderPath = :workPath ORDER BY `order` ASC")
    fun getTracksForWork(workPath: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTracks(tracks: List<TrackEntity>)

    // Playlists
    @Query("SELECT * FROM audio_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylist(playlist: PlaylistEntity)

    @Delete
    fun deletePlaylist(playlist: PlaylistEntity)

    // Playlist Works
    @Query("""
        SELECT w.* FROM works w
        INNER JOIN playlist_works pw ON w.folderPath = pw.workFolderPath
        WHERE pw.playlistId = :playlistId
        ORDER BY pw.`order` ASC
    """)
    fun getWorksForPlaylist(playlistId: String): Flow<List<WorkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addWorkToPlaylist(playlistWork: PlaylistWorkEntity)

    @Query("DELETE FROM playlist_works WHERE playlistId = :playlistId AND workFolderPath = :workPath")
    fun removeWorkFromPlaylist(playlistId: String, workPath: String)

    // Search & Filter
    @Query("""
        SELECT * FROM works 
        WHERE title LIKE '%' || :query || '%' 
        OR circle LIKE '%' || :query || '%' 
        OR cv LIKE '%' || :query || '%' 
        OR tags LIKE '%' || :query || '%'
    """)
    fun searchWorks(query: String): Flow<List<WorkEntity>>
}
