package com.kazukittin.vault.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DlSiteCacheDao {
    @Query("SELECT * FROM dlsite_cache WHERE rjCode = :rjCode")
    fun getCache(rjCode: String): DlSiteCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCache(cache: DlSiteCacheEntity)
}
