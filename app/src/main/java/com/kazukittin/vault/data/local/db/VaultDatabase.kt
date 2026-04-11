package com.kazukittin.vault.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FolderEntity::class,
        WorkEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistWorkEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun audioDao(): AudioDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
