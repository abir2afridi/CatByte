package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProjectEntity::class, ExportJobEntity::class, ProjectVersionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class CatByteDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    abstract fun exportJobDao(): ExportJobDao

    abstract fun projectVersionDao(): ProjectVersionDao

    companion object {
        private const val DATABASE_NAME = "catbyte.db"

        @Volatile
        private var instance: CatByteDatabase? = null

        // Migration from v1 to v2: Clear all projects (remove demo/seeded data)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM projects")
            }
        }

        // Migration from v2 to v3: the export queue + history table.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS export_jobs (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        resolution TEXT NOT NULL,
                        fps INTEGER NOT NULL,
                        watermark INTEGER NOT NULL,
                        bitrateMbps INTEGER NOT NULL,
                        audioBoost REAL NOT NULL,
                        state TEXT NOT NULL,
                        progress REAL NOT NULL,
                        outputPath TEXT,
                        error TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration from v3 to v4: the project time-machine table.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS project_versions (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        label TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        clipCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): CatByteDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatByteDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
