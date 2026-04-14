package com.kazukittin.vault.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dlsite_cache")
data class DlSiteCacheEntity(
    @PrimaryKey val rjCode: String,
    val workName: String?,
    val makerId: String?,
    val makerName: String?,
    val imageUrl: String?,
    val genres: String?,    // カンマ区切り
    val workType: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
