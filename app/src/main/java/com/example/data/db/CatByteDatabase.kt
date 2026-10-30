package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CatByteDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    companion object {
        private const val DATABASE_NAME = "catbyte.db"

        @Volatile
        private var instance: CatByteDatabase? = null

        fun getInstance(context: Context): CatByteDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatByteDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
