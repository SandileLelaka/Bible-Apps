package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing daily reading goals and duration achieved today.
 */
@Entity(tableName = "daily_reading_goals")
data class DailyReadingGoal(
    @PrimaryKey val dateString: String, // format "yyyy-MM-dd"
    val timeGoalMinutes: Int = 15,
    val minutesAchievedToday: Int = 0
)

/**
 * Static schema representing a Bible story narrative, lessons, scriptures and prayers.
 */
data class BibleStory(
    val id: String,
    val title: String,
    val bibleReference: String,
    val testament: String, // "Old Testament" or "New Testament"
    val category: String,  // e.g., "Faith", "Miracles", "Parables", "Wisdom"
    val verseHighlight: String,
    val summary: String,
    val keyLessons: List<String>,
    val suggestedPrayer: String,
    val thematicFocus: String = ""
) {
    fun getEstimatedReadingTimeMinutes(): Int {
        val textToRead = buildString {
            append(title)
            append(" ")
            append(summary)
            append(" ")
            keyLessons.forEach { append(it).append(" ") }
            append(suggestedPrayer)
            append(" ")
            append(verseHighlight)
        }
        val wordCount = textToRead.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        return maxOf(1, kotlin.math.ceil(wordCount.toDouble() / 200.0).toInt())
    }
}

/**
 * Room Entity representing user interactive state (bookmarks, notes, completion status).
 */
@Entity(tableName = "story_states")
data class StoryState(
    @PrimaryKey val storyId: String,
    val isBookmarked: Boolean = false,
    val isCompleted: Boolean = false,
    val reflectionText: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isPassageBookmarked: Boolean = false,
    val aiReflectionPrompt: String = "",
    val aiPrayerFocus: String = ""
)

/**
 * Room Entity representing persistent streak records of consecutive devotional completions.
 */
@Entity(tableName = "streak_records")
data class StreakRecord(
    @PrimaryKey val id: String = "singleton_streak_key",
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val lastCompletedDateString: String = "" // formatted "yyyy-MM-dd"
)

/**
 * Room Entity representing a personal prayer request posted on the Prayer Wall.
 */
@Entity(tableName = "prayer_requests")
data class PrayerRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val isAnswered: Boolean = false,
    val includeInDailyReflection: Boolean = true,
    val answerText: String = "",
    val answeredDate: Long = 0L,
    val category: String = "General"
)

/**
 * Combined model mapped by repository to provide reactive UI state.
 */
data class BibleStoryWithState(
    val story: BibleStory,
    val state: StoryState
)

/**
 * Room Entity representing a freeform, personal devotional journal entry.
 */
@Entity(tableName = "devotional_journal_entries")
data class DevotionalJournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val scripturePassage: String = "",
    val entryText: String,
    val dateCreated: Long = System.currentTimeMillis()
)

