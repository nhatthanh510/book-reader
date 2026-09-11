package com.example.docsach.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedBookEntity::class, ReadingProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedBookDao(): SavedBookDao

    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "docsach.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
