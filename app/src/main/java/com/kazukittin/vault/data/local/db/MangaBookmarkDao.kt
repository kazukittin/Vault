package com.kazukittin.vault.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MangaBookmarkDao {
    @Query("SELECT * FROM manga_bookmarks WHERE zipPath = :zipPath")
    fun getBookmark(zipPath: String): MangaBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookmark(bookmark: MangaBookmarkEntity)

    @Query("SELECT * FROM manga_bookmarks")
    fun getAllBookmarks(): List<MangaBookmarkEntity>
}
