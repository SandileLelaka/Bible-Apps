package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StoryState::class, StreakRecord::class, DailyReadingGoal::class, PrayerRequest::class, DevotionalJournalEntry::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyStateDao(): StoryStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bible_devotional_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
