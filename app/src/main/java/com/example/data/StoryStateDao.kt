package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryStateDao {
    @Query("SELECT * FROM story_states")
    fun getAllStoryStates(): Flow<List<StoryState>>

    @Query("SELECT * FROM story_states WHERE storyId = :storyId LIMIT 1")
    fun getStoryStateFlow(storyId: String): Flow<StoryState?>

    @Query("SELECT * FROM story_states WHERE storyId = :storyId LIMIT 1")
    suspend fun getStoryState(storyId: String): StoryState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryState(state: StoryState)

    @Update
    suspend fun updateStoryState(state: StoryState)

    @Delete
    suspend fun deleteStoryState(state: StoryState)

    @Query("SELECT * FROM streak_records WHERE id = 'singleton_streak_key' LIMIT 1")
    fun getStreakRecordFlow(): Flow<StreakRecord?>

    @Query("SELECT * FROM streak_records WHERE id = 'singleton_streak_key' LIMIT 1")
    suspend fun getStreakRecord(): StreakRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakRecord(record: StreakRecord)

    @Query("SELECT * FROM story_states")
    suspend fun getStoryStatesSync(): List<StoryState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryStatesBulk(states: List<StoryState>)

    @Query("SELECT * FROM daily_reading_goals")
    suspend fun getAllDailyReadingGoalsSync(): List<DailyReadingGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReadingGoalsBulk(goals: List<DailyReadingGoal>)

    @Query("SELECT * FROM daily_reading_goals WHERE dateString = :dateString LIMIT 1")
    fun getDailyReadingGoalFlow(dateString: String): Flow<DailyReadingGoal?>

    @Query("SELECT * FROM daily_reading_goals WHERE dateString = :dateString LIMIT 1")
    suspend fun getDailyReadingGoal(dateString: String): DailyReadingGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReadingGoal(goal: DailyReadingGoal)

    @Query("DELETE FROM story_states")
    suspend fun clearStoryStates()

    @Query("DELETE FROM daily_reading_goals")
    suspend fun clearDailyReadingGoals()

    @Query("DELETE FROM streak_records")
    suspend fun clearStreakRecords()

    // --- Personal Prayer Wall Queries ---

    @Query("SELECT * FROM prayer_requests ORDER BY dateAdded DESC")
    fun getAllPrayerRequests(): Flow<List<PrayerRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRequest(request: PrayerRequest)

    @Update
    suspend fun updatePrayerRequest(request: PrayerRequest)

    @Delete
    suspend fun deletePrayerRequest(request: PrayerRequest)

    @Query("DELETE FROM prayer_requests WHERE id = :id")
    suspend fun deletePrayerRequestById(id: Long)

    @Query("DELETE FROM prayer_requests")
    suspend fun clearPrayerRequests()

    // --- Personal Devotional Journal Queries ---

    @Query("SELECT * FROM devotional_journal_entries ORDER BY dateCreated DESC")
    fun getAllDevotionalJournalEntries(): Flow<List<DevotionalJournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevotionalJournalEntry(entry: DevotionalJournalEntry): Long

    @Update
    suspend fun updateDevotionalJournalEntry(entry: DevotionalJournalEntry)

    @Delete
    suspend fun deleteDevotionalJournalEntry(entry: DevotionalJournalEntry)

    @Query("DELETE FROM devotional_journal_entries WHERE id = :id")
    suspend fun deleteDevotionalJournalEntryById(id: Long)

    @Query("DELETE FROM devotional_journal_entries")
    suspend fun clearAllDevotionalJournalEntries()
}
