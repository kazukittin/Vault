package com.kazukittin.vault.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val parentId: Int,
    val isPinned: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
