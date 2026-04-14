package com.kazukittin.vault.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FolderEntity::class,
        WorkEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistWorkEntity::class,
        DlSiteCacheEntity::class,
        MangaBookmarkEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun audioDao(): AudioDao
    abstract fun dlSiteCacheDao(): DlSiteCacheDao
    abstract fun mangaBookmarkDao(): MangaBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS manga_bookmarks (
                        zipPath TEXT PRIMARY KEY NOT NULL,
                        page INTEGER NOT NULL,
                        totalPages INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS dlsite_cache (
                        rjCode TEXT PRIMARY KEY NOT NULL,
                        workName TEXT,
                        makerId TEXT,
                        makerName TEXT,
                        imageUrl TEXT,
                        genres TEXT,
                        workType TEXT,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
