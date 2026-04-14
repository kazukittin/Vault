package com.kazukittin.vault.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_bookmarks")
data class MangaBookmarkEntity(
    @PrimaryKey val zipPath: String,
    val page: Int,
    val totalPages: Int,
    val savedAt: Long = System.currentTimeMillis()
)
