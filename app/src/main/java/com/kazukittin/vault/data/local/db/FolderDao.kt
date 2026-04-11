package com.kazukittin.vault.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isPinned = 1")
    fun getPinnedFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFolders(folders: List<FolderEntity>) // suspendを外す一時的な回避

    @Query("UPDATE folders SET category = :category WHERE id = :folderId")
    fun updateCategory(folderId: String, category: String?)

    @Query("DELETE FROM folders")
    fun clearAll() // suspendを外す一時的な回避
}
