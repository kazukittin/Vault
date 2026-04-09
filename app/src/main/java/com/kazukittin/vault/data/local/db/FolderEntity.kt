package com.kazukittin.vault.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String,
    val isPinned: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
