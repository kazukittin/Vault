package com.kazukittin.vault.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey val folderPath: String,
    val rjCode: String?,
    val title: String,
    val circle: String,
    val coverUrl: String?,
    val cv: String,
    val tags: String,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["folderPath"],
            childColumns = ["workFolderPath"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TrackEntity(
    @PrimaryKey val path: String,
    val workFolderPath: String,
    val title: String,
    val order: Int,
    val durationMs: Long
)

@Entity(tableName = "audio_playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String, // UUID
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_works",
    primaryKeys = ["playlistId", "workFolderPath"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["folderPath"],
            childColumns = ["workFolderPath"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistWorkEntity(
    val playlistId: String,
    val workFolderPath: String,
    val order: Int
)
