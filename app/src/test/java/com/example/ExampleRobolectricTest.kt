package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BibleStoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bible Devotion", appName)
  }

  @Test
  fun `test passage bookmark toggling`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val dao = database.storyStateDao()
    val repository = BibleStoryRepository(dao)

    val storyId = "creation"

    // Verify initially false
    val initialStories = repository.allStoriesWithState.first()
    val creationInitial = initialStories.first { it.story.id == storyId }
    assertFalse(creationInitial.state.isPassageBookmarked)

    // Verify first toggle makes it true
    repository.togglePassageBookmark(storyId)
    val afterToggleStories = repository.allStoriesWithState.first()
    val creationAfterToggle = afterToggleStories.first { it.story.id == storyId }
    assertTrue(creationAfterToggle.state.isPassageBookmarked)

    // Verify second toggle makes it false again
    repository.togglePassageBookmark(storyId)
    val finalStories = repository.allStoriesWithState.first()
    val creationFinal = finalStories.first { it.story.id == storyId }
    assertFalse(creationFinal.state.isPassageBookmarked)
  }

  @Test
  fun `test verse of the day state and refresh`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val dao = database.storyStateDao()
    val repository = BibleStoryRepository(dao)
    val viewModel = com.example.ui.BibleStoriesViewModel(repository, context)

    val initialVerse = viewModel.verseOfTheDay.value
    assertTrue("Verse text should not be empty", initialVerse.text.isNotBlank())
    assertTrue("Verse reference should not be empty", initialVerse.reference.isNotBlank())
    assertTrue("Verse topic should not be empty", initialVerse.topic.isNotBlank())
    assertTrue("Verse summary should not be empty", initialVerse.summary.isNotBlank())

    // Test refresh chooses randomly from the full list
    viewModel.refreshVerseOfTheDay()
    val nextVerse = viewModel.verseOfTheDay.value
    assertTrue("Refreshed verse text should be valid", nextVerse.text.isNotBlank())
    assertTrue("Refreshed verse reference should be valid", nextVerse.reference.isNotBlank())
    assertTrue("Refreshed verse summary should be valid", nextVerse.summary.isNotBlank())
  }

  @Test
  fun `test prayer request addition and resolution`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val dao = database.storyStateDao()
    val repository = BibleStoryRepository(dao)

    // Clear and verify empty
    repository.clearLocalStates()
    var initialPrayers = repository.allPrayerRequests.first()
    assertTrue(initialPrayers.isEmpty())

    // Add prayer
    repository.addPrayerRequest("Please heal my sibling", true)
    var currentPrayers = repository.allPrayerRequests.first()
    assertEquals(1, currentPrayers.size)
    assertEquals("Please heal my sibling", currentPrayers[0].text)
    assertFalse(currentPrayers[0].isAnswered)
    assertTrue(currentPrayers[0].includeInDailyReflection)

    // Highlight answered status
    val addedPrayer = currentPrayers[0]
    val answeredPrayer = addedPrayer.copy(
        isAnswered = true,
        answerText = "Sibling is fully healed!"
    )
    repository.updatePrayerRequest(answeredPrayer)

    val updatedPrayers = repository.allPrayerRequests.first()
    assertEquals(1, updatedPrayers.size)
    assertTrue(updatedPrayers[0].isAnswered)
    assertEquals("Sibling is fully healed!", updatedPrayers[0].answerText)

    // Delete prayer
    repository.deletePrayerRequest(addedPrayer.id)
    val finalPrayers = repository.allPrayerRequests.first()
    assertTrue(finalPrayers.isEmpty())
  }

  @Test
  fun `test prayer category auto detection`() {
    val detector = com.example.ui.AutoCategoryDetector
    
    // Check keywords triggering correct categories
    assertEquals("Healing", detector.detect("Praying for healing and quick recovery after surgery"))
    assertEquals("Provision", detector.detect("Need financial support to pay off my debt and rent"))
    assertEquals("Family", detector.detect("Please protect my children, wife, and entire family"))
    assertEquals("Peace", detector.detect("Struggling with intense anxiety and daily stress"))
    assertEquals("Thanksgiving", detector.detect("Thank God for the incredible blessings this week!"))
    assertEquals("General", detector.detect("Just standard daily reflection"))
  }

  @Test
  fun `test 30 days streak calculation outputs exact and accurate timeline`() {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdf.format(Date())
    val yesterdayStr = sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
    val twoDaysAgoStr = sdf.format(Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000))

    // Create mock completed stories
    val mockStories = listOf(
      com.example.data.BibleStoryWithState(
        story = com.example.data.BibleStory(
          id = "story_1",
          title = "Creation Narrative",
          bibleReference = "Genesis 1:1",
          testament = "Old Testament",
          category = "Faith",
          verseHighlight = "In the beginning",
          summary = "The beginning of everything",
          keyLessons = listOf("God creates"),
          suggestedPrayer = "Praise God"
        ),
        state = com.example.data.StoryState(
          storyId = "story_1",
          isCompleted = true,
          lastUpdated = sdf.parse(twoDaysAgoStr)!!.time
        )
      ),
      com.example.data.BibleStoryWithState(
        story = com.example.data.BibleStory(
          id = "story_2",
          title = "Garden of Eden",
          bibleReference = "Genesis 2:4",
          testament = "Old Testament",
          category = "Faith",
          verseHighlight = "The Lord God took the man",
          summary = "Life in Eden",
          keyLessons = listOf("Trust and obey"),
          suggestedPrayer = "Lord, grant obedience"
        ),
        state = com.example.data.StoryState(
          storyId = "story_2",
          isCompleted = true,
          lastUpdated = sdf.parse(yesterdayStr)!!.time
        )
      )
    )

    val streakPoints = com.example.ui.calculatePast30DaysStreak(mockStories)

    // Verify exactly 30 days are mapped
    assertEquals(30, streakPoints.size)

    // Check chronological ordering: last point is today
    val todayPoint = streakPoints.last()
    assertEquals(todayStr, todayPoint.dateFull)

    // Two days ago should be marked as completed, yesterday as completed
    val yesterdayPoint = streakPoints[28] // today is 29
    val twoDaysAgoPoint = streakPoints[27]

    assertEquals(yesterdayStr, yesterdayPoint.dateFull)
    assertEquals(twoDaysAgoStr, twoDaysAgoPoint.dateFull)

    assertTrue(twoDaysAgoPoint.isCompleted)
    assertTrue(yesterdayPoint.isCompleted)
    assertFalse(todayPoint.isCompleted)

    // Streaks sequential validation
    assertTrue(twoDaysAgoPoint.streakValue >= 1)
    assertTrue(yesterdayPoint.streakValue == twoDaysAgoPoint.streakValue + 1)
    // Stays identical on today as it holds the streak from yesterday
    assertEquals(yesterdayPoint.streakValue, todayPoint.streakValue)
  }

  @Test
  fun `test devotional reminder manager settings and date completion flag`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val reminderManager = com.example.ui.DevotionalReminderManager(context)

    // Initially disabled
    assertFalse(reminderManager.isReminderEnabled())
    assertEquals("20:00", reminderManager.getReminderTime())

    // Set enabled and check
    reminderManager.setReminderSettings(true, "08:30")
    assertTrue(reminderManager.isReminderEnabled())
    assertEquals("08:30", reminderManager.getReminderTime())

    // Simulate marking reflection completed today
    val mainPrefs = context.getSharedPreferences("bible_devotional_prefs", Context.MODE_PRIVATE)
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    
    // Initially not marked completed
    assertFalse(mainPrefs.getString("last_completed_reflection_date", "") == todayStr)

    // Mark completed today
    mainPrefs.edit().putString("last_completed_reflection_date", todayStr).apply()
    assertTrue(mainPrefs.getString("last_completed_reflection_date", "") == todayStr)

    // Disable reminder and check
    reminderManager.setReminderSettings(false, "08:30")
    assertFalse(reminderManager.isReminderEnabled())
  }
}
