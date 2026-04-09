package com.kazukittin.vault.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FolderEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
