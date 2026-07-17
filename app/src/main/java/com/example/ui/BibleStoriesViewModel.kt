package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BibleStoryRepository
import com.example.data.BibleStoryWithState
import com.example.data.StreakRecord
import com.example.data.DailyReadingGoal
import com.example.data.AiReflectionResult
import com.example.data.PrayerRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AiPromptState {
    object Idle : AiPromptState
    object Loading : AiPromptState
    data class Success(val prompt: String, val prayerFocus: String) : AiPromptState
    data class Error(val message: String) : AiPromptState
}

sealed interface AiStoryGenerationState {
    object Idle : AiStoryGenerationState
    object Loading : AiStoryGenerationState
    data class Success(val story: BibleStoryWithState) : AiStoryGenerationState
    data class Error(val message: String) : AiStoryGenerationState
}

sealed interface CloudSyncState {
    object Idle : CloudSyncState
    object Syncing : CloudSyncState
    data class Success(
        val message: String,
        val timestamp: Long,
        val stats: String
    ) : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}

data class UpliftingVerse(
    val reference: String,
    val text: String,
    val topic: String,
    val summary: String = ""
)

class BibleStoriesViewModel(
    private val repository: BibleStoryRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("bible_devotional_prefs", android.content.Context.MODE_PRIVATE)

    val isCloudSyncEnabled = MutableStateFlow(sharedPrefs.getBoolean("cloud_sync_enabled", true))

    fun setCloudSyncEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
        isCloudSyncEnabled.value = enabled
        if (enabled) {
            triggerCloudSync()
        }
    }

    private val _cloudSyncState = MutableStateFlow<CloudSyncState>(
        if (sharedPrefs.getLong("last_synced_time", 0L) > 0L) {
            CloudSyncState.Success(
                message = "Synced",
                timestamp = sharedPrefs.getLong("last_synced_time", 0L),
                stats = sharedPrefs.getString("last_synced_stats", "All data backed up") ?: "Ready"
            )
        } else {
            CloudSyncState.Idle
        }
    )
    val cloudSyncState: StateFlow<CloudSyncState> = _cloudSyncState.asStateFlow()

    fun triggerCloudSync() {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _cloudSyncState.value = CloudSyncState.Error("Sync failed: Offline Mode active.")
                return@launch
            }
            _cloudSyncState.value = CloudSyncState.Syncing
            try {
                kotlinx.coroutines.delay(1200)
                val (states, streak, goals) = repository.getFullBackupData()

                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()

                val payloadMap = mapOf(
                    "storyStates" to states,
                    "streakRecord" to streak,
                    "dailyReadingGoals" to goals
                )

                val adapter = moshi.adapter(Map::class.java)
                val jsonPayload = adapter.toJson(payloadMap)

                sharedPrefs.edit()
                    .putString("cloud_simulation_storage", jsonPayload)
                    .putLong("last_synced_time", System.currentTimeMillis())
                    .putString("last_synced_stats", "Backup: ${states.size} stories, ${goals.size} logs synced")
                    .apply()

                _cloudSyncState.value = CloudSyncState.Success(
                    message = "Synced with Cloud",
                    timestamp = System.currentTimeMillis(),
                    stats = "${states.count { it.reflectionText.isNotBlank() }} journal notes, ${states.count { it.isPassageBookmarked || it.isBookmarked }} bookmarks saved"
                )
            } catch (e: Exception) {
                _cloudSyncState.value = CloudSyncState.Error("Sync failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            if (isOfflineMode.value) {
                _cloudSyncState.value = CloudSyncState.Error("Restore failed: Offline Mode active.")
                return@launch
            }
            _cloudSyncState.value = CloudSyncState.Syncing
            try {
                kotlinx.coroutines.delay(1200)
                val jsonPayload = sharedPrefs.getString("cloud_simulation_storage", null)
                if (jsonPayload.isNullOrEmpty()) {
                    _cloudSyncState.value = CloudSyncState.Error("No backup data found on cloud server.")
                    return@launch
                }

                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()

                val adapter = moshi.adapter(Map::class.java)
                val payloadMap = adapter.fromJson(jsonPayload) ?: throw Exception("Invalid backup format")

                val statesJson = moshi.adapter(List::class.java).toJson(payloadMap["storyStates"] as List<*>)
                val storyStates: List<com.example.data.StoryState> = moshi.adapter<List<com.example.data.StoryState>>(
                    com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.StoryState::class.java)
                ).fromJson(statesJson) ?: emptyList()

                val streakJson = moshi.adapter(Map::class.java).toJson(payloadMap["streakRecord"] as Map<*, *>)
                val streakRecord = moshi.adapter(com.example.data.StreakRecord::class.java).fromJson(streakJson)

                val goalsJson = moshi.adapter(List::class.java).toJson(payloadMap["dailyReadingGoals"] as List<*>)
                val dailyReadingGoals: List<com.example.data.DailyReadingGoal> = moshi.adapter<List<com.example.data.DailyReadingGoal>>(
                    com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.DailyReadingGoal::class.java)
                ).fromJson(goalsJson) ?: emptyList()

                repository.restoreBackupData(storyStates, streakRecord, dailyReadingGoals)

                _cloudSyncState.value = CloudSyncState.Success(
                    message = "Restored from Cloud",
                    timestamp = System.currentTimeMillis(),
                    stats = "Successfully reloaded ${storyStates.size} records & reading goals"
                )
            } catch (e: Exception) {
                _cloudSyncState.value = CloudSyncState.Error("Restore failed: ${e.message ?: "Deserialization error"}")
            }
        }
    }

    fun simulateLocalReset() {
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncState.Syncing
            kotlinx.coroutines.delay(800)
            repository.clearLocalStates()
            _cloudSyncState.value = CloudSyncState.Idle
        }
    }

    private fun autoSyncIfNeeded() {
        if (isCloudSyncEnabled.value) {
            triggerCloudSync()
        }
    }

    private fun getTodayDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }

    val todayReadingGoal: StateFlow<DailyReadingGoal> = repository.getDailyReadingGoalFlow(getTodayDateString())
        .map { it ?: DailyReadingGoal(dateString = getTodayDateString()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyReadingGoal(dateString = getTodayDateString())
        )

    fun setDailyReadingGoal(minutes: Int) {
        viewModelScope.launch {
            repository.setDailyReadingGoal(getTodayDateString(), minutes)
        }
    }

    fun logReadingMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.logReadingMinutes(getTodayDateString(), minutes)
        }
    }

    // Filter flows
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val showOnlyBookmarked = MutableStateFlow(false)
    val showOnlyCompleted = MutableStateFlow(false)

    // Track Simulated Offline status
    val isOfflineMode = MutableStateFlow(false)

    val upliftingVerses = listOf(
        UpliftingVerse(
            "Philippians 4:13",
            "I can do all things through Christ who strengthens me.",
            "Strength",
            "A declaration of dependency on Christ's empowerment, reminding us that we can endure and overcome any trial through the supernatural strength He provides."
        ),
        UpliftingVerse(
            "Psalm 23:1",
            "The Lord is my shepherd; I shall not want.",
            "Comfort",
            "A beautiful metaphor highlighting God's loving guidance, provision, and absolute care, assuring us we will never lack anything essential."
        ),
        UpliftingVerse(
            "Proverbs 3:5-6",
            "Trust in the Lord with all your heart, and do not lean on your own understanding.",
            "Trust",
            "An instruction to place absolute, heartfelt reliance on God rather than our own finite understanding, promising that He will direct our paths when we acknowledge His sovereignty."
        ),
        UpliftingVerse(
            "Romans 8:28",
            "And we know that for those who love God all things work together for good.",
            "Hope",
            "This verse reassures believers that God orchestrates all circumstances of our lives to ultimately serve a greater, divine purpose of good."
        ),
        UpliftingVerse(
            "Isaiah 40:31",
            "But they who wait for the Lord shall renew their strength; they shall mount up with wings like eagles.",
            "Endurance",
            "A beautiful promise of renewal and strength for those who patiently wait upon the Lord, indicating that divine endurance allows us to rise above exhaustion."
        ),
        UpliftingVerse(
            "Joshua 1:9",
            "Be strong and courageous. Do not be frightened, and do not be dismayed, for the Lord your God is with you.",
            "Courage",
            "God's powerful command to Joshua, and to all of us, to be courageous and strong, anchored in the unwavering promise that His protective presence is always with us."
        ),
        UpliftingVerse(
            "Jeremiah 29:11",
            "For I know the plans I have for you, declares the Lord, plans for welfare and not for evil.",
            "Promise",
            "A profound promise that God's intentions for His people are filled with hope, security, and a beautiful future, even in times of transition or challenge."
        ),
        UpliftingVerse(
            "Matthew 6:34",
            "Therefore do not be anxious about tomorrow, for tomorrow will be anxious for itself.",
            "Peace",
            "Jesus' practical teaching advising against future-oriented worry, urging us to remain grounded in today and trust God with the future."
        ),
        UpliftingVerse(
            "Romans 12:12",
            "Rejoice in hope, be patient in tribulation, be constant in prayer.",
            "Faithfulness",
            "A concise roadmap for Christian living, calling us to find joy in future hope, show endurance under pressure, and maintain a vibrant prayer life."
        ),
        UpliftingVerse(
            "Psalm 121:1-2",
            "I lift up my eyes to the hills. From where does my help come? My help comes from the Lord.",
            "Help",
            "A declaration of trust in God as our ultimate protector and helper, who made heaven and earth and is always vigilant in watching over us."
        ),
        UpliftingVerse(
            "John 14:27",
            "Peace I leave with you; my peace I give to you. Let not your hearts be troubled.",
            "Peace",
            "A tender parting gift from Jesus offering His supernatural, deep peace that guards our minds against the anxieties of the world."
        ),
        UpliftingVerse(
            "2 Timothy 1:7",
            "For God gave us a spirit not of fear but of power and love and self-control.",
            "Courage",
            "A reminder that timidity and fear do not come from God, but instead, His Holy Spirit equips us with power, unconditional love, and sound judgment."
        ),
        UpliftingVerse(
            "Galatians 6:9",
            "And let us not grow weary of doing good, for in due season we will reap.",
            "Perseverance",
            "An encouraging call to persevere in doing what is right and serving others, trusting that a harvest of blessings will come at the perfect time."
        ),
        UpliftingVerse(
            "Psalm 46:1",
            "God is our refuge and strength, a very present help in trouble.",
            "Refuge",
            "A comforting declaration of God's role as our ultimate shelter and source of strength, who is instantly accessible in times of trouble."
        ),
        UpliftingVerse(
            "Romans 15:13",
            "May the God of hope fill you with all joy and peace in believing.",
            "Joy",
            "A beautiful apostolic blessing praying for believers to be filled with absolute joy and peace as they trust in the God of hope."
        ),
        UpliftingVerse(
            "Matthew 11:28",
            "Come to me, all who labor and are heavy laden, and I will give you rest.",
            "Rest",
            "A gentle, loving invitation from Jesus to all who feel overwhelmed or exhausted, promising to give them deep, spiritual rest."
        ),
        UpliftingVerse(
            "1 Corinthians 16:14",
            "Let all that you do be done in love.",
            "Love",
            "A simple yet all-encompassing instruction reminding us that love must be the ultimate motivation behind every action, word, and relationship."
        ),
        UpliftingVerse(
            "1 John 4:18",
            "There is no fear in love, but perfect love casts out fear.",
            "Love",
            "An assurance that growing in understanding God's perfect love frees us from the torment of fear and judgment."
        )
    )

    private val _verseOfTheDay = MutableStateFlow<UpliftingVerse>(
        upliftingVerses[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % upliftingVerses.size]
    )
    val verseOfTheDay: StateFlow<UpliftingVerse> = _verseOfTheDay.asStateFlow()

    private val _isVerseLoading = MutableStateFlow(false)
    val isVerseLoading: StateFlow<Boolean> = _isVerseLoading.asStateFlow()

    fun fetchVerseOfTheDayFromGemini() {
        if (isOfflineMode.value) return
        viewModelScope.launch {
            _isVerseLoading.value = true
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val result = com.example.data.GeminiHelper.generateVerseOfTheDay(apiKey)
                _verseOfTheDay.value = UpliftingVerse(
                    reference = result.reference,
                    text = result.text,
                    topic = result.topic,
                    summary = result.summary
                )
            } catch (e: Exception) {
                // Keep the current local fallback
            } finally {
                _isVerseLoading.value = false
            }
        }
    }

    fun refreshVerseOfTheDay() {
        if (!isOfflineMode.value) {
            fetchVerseOfTheDayFromGemini()
        } else {
            var nextVerse: UpliftingVerse
            do {
                nextVerse = upliftingVerses.random()
            } while (upliftingVerses.size > 1 && nextVerse == _verseOfTheDay.value)
            _verseOfTheDay.value = nextVerse
        }
    }


    // --- Personal Prayer Wall VM Methods ---

    val allPrayerRequests: StateFlow<List<PrayerRequest>> = repository.allPrayerRequests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun postPrayerRequest(text: String, includeInDailyReflection: Boolean, category: String = "General") {
        viewModelScope.launch {
            repository.addPrayerRequest(text, includeInDailyReflection, category)
        }
    }

    fun markPrayerAnswered(id: Long, isAnswered: Boolean, answerText: String) {
        viewModelScope.launch {
            val list = allPrayerRequests.value
            val prayer = list.find { it.id == id }
            if (prayer != null) {
                val updated = prayer.copy(
                    isAnswered = isAnswered,
                    answerText = answerText,
                    answeredDate = if (isAnswered) System.currentTimeMillis() else 0L
                )
                repository.updatePrayerRequest(updated)
            }
        }
    }

    fun togglePrayerIncludeInDailyReflection(id: Long, include: Boolean) {
        viewModelScope.launch {
            val list = allPrayerRequests.value
            val prayer = list.find { it.id == id }
            if (prayer != null) {
                val updated = prayer.copy(includeInDailyReflection = include)
                repository.updatePrayerRequest(updated)
            }
        }
    }

    fun deletePrayerRequest(id: Long) {
        viewModelScope.launch {
            repository.deletePrayerRequest(id)
        }
    }

    // --- Personal Devotional Journal VM Methods ---

    val allDevotionalJournalEntries: StateFlow<List<com.example.data.DevotionalJournalEntry>> = 
        repository.allDevotionalJournalEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun markReflectionCompletedToday() {
        try {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            sharedPrefs.edit()
                .putString("last_completed_reflection_date", todayStr)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("localStorage", "Failed to mark reflection as completed today", e)
        }
    }

    fun addDevotionalJournalEntry(title: String, scripturePassage: String, entryText: String) {
        viewModelScope.launch {
            repository.addDevotionalJournalEntry(title, scripturePassage, entryText)
            markReflectionCompletedToday()
        }
    }

    fun updateDevotionalJournalEntry(entry: com.example.data.DevotionalJournalEntry) {
        viewModelScope.launch {
            repository.updateDevotionalJournalEntry(entry)
        }
    }

    fun deleteDevotionalJournalEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteDevotionalJournalEntry(id)
        }
    }

    fun clearAllDevotionalJournalEntries() {
        viewModelScope.launch {
            repository.clearAllDevotionalJournalEntries()
        }
    }

    // Flow that filters and maps the most recently viewed devotional from current Room states
    val mostRecentDevotional: StateFlow<BibleStoryWithState?> = repository.allStoriesWithState.map { stories ->
        stories.filter { it.state.lastUpdated > 0 }
            .maxByOrNull { it.state.lastUpdated }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Search History for quick recall limits to latest 5 unique entries
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // Currently viewed detailed Bible Story (bottom sheet / full pane)
    private val _activeStory = MutableStateFlow<BibleStoryWithState?>(null)
    val activeStory: StateFlow<BibleStoryWithState?> = _activeStory.asStateFlow()

    // State of AI Reflection Prompt generation
    private val _aiReflectionPromptState = MutableStateFlow<AiPromptState>(AiPromptState.Idle)
    val aiReflectionPromptState: StateFlow<AiPromptState> = _aiReflectionPromptState.asStateFlow()

    // State of AI Dynamic Story generation
    private val _aiStoryGenerationState = MutableStateFlow<AiStoryGenerationState>(AiStoryGenerationState.Idle)
    val aiStoryGenerationState: StateFlow<AiStoryGenerationState> = _aiStoryGenerationState.asStateFlow()

    // Expose all stories state flow for overall progress tracking
    val allStories: StateFlow<List<BibleStoryWithState>> = repository.allStoriesWithState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Expose persistent streak details for daily devotional completions
    val streakRecord: StateFlow<StreakRecord> = repository.streakRecordFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakRecord()
    )

    // Combine parameters to dynamically compile matching stories
    val filteredStories: StateFlow<List<BibleStoryWithState>> = combine(
        repository.allStoriesWithState,
        searchQuery,
        selectedCategory,
        showOnlyBookmarked,
        showOnlyCompleted
    ) { stories, query, category, onlyBookmarked, onlyCompleted ->
        stories.filter { item ->
            val story = item.story
            val state = item.state

            val matchesQuery = query.isBlank() ||
                    story.title.contains(query, ignoreCase = true) ||
                    story.summary.contains(query, ignoreCase = true) ||
                    story.bibleReference.contains(query, ignoreCase = true) ||
                    story.verseHighlight.contains(query, ignoreCase = true) ||
                    story.keyLessons.any { it.contains(query, ignoreCase = true) }

            val matchesCategory = category == "All" || story.category.equals(category, ignoreCase = true)
            val matchesBookmark = !onlyBookmarked || state.isBookmarked
            val matchesCompleted = !onlyCompleted || state.isCompleted

            matchesQuery && matchesCategory && matchesBookmark && matchesCompleted
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Featured Daily Devotional Focus shifts automatically based on the day of the year
    val dailyDevotional: StateFlow<BibleStoryWithState?> = repository.allStoriesWithState.map { stories ->
        if (stories.isEmpty()) return@map null
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % stories.size
        stories[index]
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Synchronize active story detailed modal state when DB contents undergo outer mutations
    init {
        fetchVerseOfTheDayFromGemini()
        loadCachedDailyStoryIfExists()
        viewModelScope.launch {
            combine(repository.allStoriesWithState, _activeStory) { stories, active ->
                if (active == null) null
                else stories.find { it.story.id == active.story.id }
            }.collect { updatedActive ->
                _activeStory.value = updatedActive
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: String) {
        selectedCategory.value = category
    }

    fun toggleBookmarkFilter() {
        showOnlyBookmarked.value = !showOnlyBookmarked.value
    }

    fun toggleCompletedFilter() {
        showOnlyCompleted.value = !showOnlyCompleted.value
    }

    fun setStoryActive(story: BibleStoryWithState?) {
        _activeStory.value = story
        if (story != null) {
            // Update lastOpened is excellent for tracking Most Recent Devotional offline!
            viewModelScope.launch {
                repository.updateLastViewed(story.story.id)
            }
            val state = story.state
            if (!state.aiReflectionPrompt.isNullOrEmpty() && !state.aiPrayerFocus.isNullOrEmpty()) {
                _aiReflectionPromptState.value = AiPromptState.Success(
                    state.aiReflectionPrompt,
                    state.aiPrayerFocus
                )
            } else {
                _aiReflectionPromptState.value = AiPromptState.Idle
            }
        } else {
            _aiReflectionPromptState.value = AiPromptState.Idle
        }
    }

    fun commitSearchToHistory(query: String) {
        if (query.isBlank()) return
        val currentList = _searchHistory.value.toMutableList()
        currentList.remove(query) // remove duplicates
        currentList.add(0, query) // prepend to front
        if (currentList.size > 5) {
            currentList.removeAt(currentList.lastIndex)
        }
        _searchHistory.value = currentList
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun toggleBookmark(storyId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(storyId)
            autoSyncIfNeeded()
        }
    }

    fun togglePassageBookmark(storyId: String) {
        viewModelScope.launch {
            repository.togglePassageBookmark(storyId)
            autoSyncIfNeeded()
        }
    }

    fun toggleCompleted(storyId: String) {
        viewModelScope.launch {
            repository.toggleCompleted(storyId)
            autoSyncIfNeeded()
        }
    }

    fun saveReflectionNotes(storyId: String, notes: String) {
        viewModelScope.launch {
            // Persist locally in the robust Room SQLite database (the Android industry standard local storage)
            repository.saveReflection(storyId, notes)
            
            // Also store in SharedPreferences as a key-value pair to explicitly fulfill local storage needs
            try {
                sharedPrefs.edit()
                    .putString("localStorage_reflection_$storyId", notes)
                    .apply()
            } catch (e: Exception) {
                android.util.Log.e("localStorage", "Failed to sync reflection to SharedPreferences", e)
            }
            
            markReflectionCompletedToday()
            autoSyncIfNeeded()
        }
    }

    fun loadOrGenerateAiPrompt(storyId: String) {
        viewModelScope.launch {
            _aiReflectionPromptState.value = AiPromptState.Loading
            try {
                if (isOfflineMode.value) {
                    // Fetch existing offline database state
                    val state = allStories.value.find { it.story.id == storyId }?.state
                    if (state != null && !state.aiReflectionPrompt.isNullOrEmpty() && !state.aiPrayerFocus.isNullOrEmpty()) {
                        _aiReflectionPromptState.value = AiPromptState.Success(
                            state.aiReflectionPrompt,
                            state.aiPrayerFocus
                        )
                    } else {
                        throw Exception("Offline Mode Active: Live AI generations are paused. Core scripture narratives, lessons, and standard prayers are fully available offline.")
                    }
                } else {
                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                    val result = repository.generateOrFetchAiPrompt(storyId, apiKey)
                    _aiReflectionPromptState.value = AiPromptState.Success(result.prompt, result.prayerFocus)
                }
            } catch (e: Exception) {
                _aiReflectionPromptState.value = AiPromptState.Error(e.message ?: "Failed to generate AI content.")
            }
        }
    }

    private fun loadCachedDailyStoryIfExists() {
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val cachedJson = sharedPrefs.getString("cached_daily_story_$dateString", null)
        if (!cachedJson.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    val moshi = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val adapter = moshi.adapter(com.example.data.BibleStory::class.java)
                    val story = adapter.fromJson(cachedJson)
                    if (story != null) {
                        val state = repository.getStoryStateDirect("generated_daily_$dateString")
                            ?: com.example.data.StoryState(storyId = "generated_daily_$dateString")
                        _aiStoryGenerationState.value = AiStoryGenerationState.Success(
                            BibleStoryWithState(story, state)
                        )
                    }
                } catch (e: Exception) {
                    // Ignore decoding error
                }
            }
        }
    }

    fun generateDailyAiStory() {
        viewModelScope.launch {
            _aiStoryGenerationState.value = AiStoryGenerationState.Loading
            try {
                if (isOfflineMode.value) {
                    val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    
                    // 1. Try to load today's cached story first
                    val todayCached = sharedPrefs.getString("cached_daily_story_$dateString", null)
                    if (!todayCached.isNullOrEmpty()) {
                        val moshi = com.squareup.moshi.Moshi.Builder()
                            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                        val adapter = moshi.adapter(com.example.data.BibleStory::class.java)
                        val story = adapter.fromJson(todayCached)
                        if (story != null) {
                            val state = repository.getStoryStateDirect("generated_daily_$dateString")
                                ?: com.example.data.StoryState(storyId = "generated_daily_$dateString")
                            val storyWithState = BibleStoryWithState(story, state)
                            _aiStoryGenerationState.value = AiStoryGenerationState.Success(storyWithState)
                            setStoryActive(storyWithState)
                            return@launch
                        }
                    }

                    // 2. Try to find the most recently cached daily story from any previous day
                    val allKeys = sharedPrefs.all.keys.filter { it.startsWith("cached_daily_story_") }
                    val sortedKeys = allKeys.sortedDescending() // newest date first
                    var foundPrevious = false
                    for (key in sortedKeys) {
                        val cachedJson = sharedPrefs.getString(key, null)
                        if (!cachedJson.isNullOrEmpty()) {
                            val moshi = com.squareup.moshi.Moshi.Builder()
                                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                .build()
                            val adapter = moshi.adapter(com.example.data.BibleStory::class.java)
                            val story = adapter.fromJson(cachedJson)
                            if (story != null) {
                                val state = repository.getStoryStateDirect(story.id)
                                    ?: com.example.data.StoryState(storyId = story.id)
                                val storyWithState = BibleStoryWithState(story, state)
                                _aiStoryGenerationState.value = AiStoryGenerationState.Success(storyWithState)
                                setStoryActive(storyWithState)
                                foundPrevious = true
                                break
                            }
                        }
                    }

                    if (foundPrevious) return@launch

                    // 3. Fall back to today's static featured story as a curated offline fallback
                    val staticStoryItem = dailyDevotional.value
                    if (staticStoryItem != null) {
                        _aiStoryGenerationState.value = AiStoryGenerationState.Success(staticStoryItem)
                        setStoryActive(staticStoryItem)
                        return@launch
                    }

                    throw Exception("No cached devotions found offline. Check network connection.")
                } else {
                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                    val result = com.example.data.GeminiHelper.generateDailyBibleStory(apiKey)
                    val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val dynamicStory = com.example.data.BibleStory(
                        id = "generated_daily_$dateString",
                        title = result.title,
                        bibleReference = result.bibleReference,
                        testament = result.testament,
                        category = result.category,
                        verseHighlight = result.verseHighlight,
                        summary = result.summary,
                        keyLessons = result.keyLessons,
                        suggestedPrayer = result.suggestedPrayer,
                        thematicFocus = result.thematicFocus
                    )

                    val currentState = repository.getStoryStateDirect("generated_daily_$dateString") 
                        ?: com.example.data.StoryState(storyId = "generated_daily_$dateString")
                    val updatedState = currentState.copy(
                        aiReflectionPrompt = result.reflectionPrompt,
                        aiPrayerFocus = result.prayerFocus,
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.saveStoryStateDirect(updatedState)

                    // Cache the dynamic story object to SharedPreferences
                    try {
                        val moshi = com.squareup.moshi.Moshi.Builder()
                            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                        val adapter = moshi.adapter(com.example.data.BibleStory::class.java)
                        val storyJson = adapter.toJson(dynamicStory)
                        sharedPrefs.edit()
                            .putString("cached_daily_story_$dateString", storyJson)
                            .apply()
                    } catch (ex: Exception) {
                        // ignore caching error
                    }

                    val storyWithState = BibleStoryWithState(dynamicStory, updatedState)
                    _aiStoryGenerationState.value = AiStoryGenerationState.Success(storyWithState)
                    
                    // Launch immediately inside the detailed workbook visual modal
                    setStoryActive(storyWithState)
                }
            } catch (e: Exception) {
                _aiStoryGenerationState.value = AiStoryGenerationState.Error(e.message ?: "Failed to generate dynamic Bible story.")
            }
        }
    }

    fun resetAiStoryGeneration() {
        _aiStoryGenerationState.value = AiStoryGenerationState.Idle
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        sharedPrefs.edit().remove("cached_daily_story_$dateString").apply()
    }

    // Factory Provider
    class Factory(
        private val repo: BibleStoryRepository,
        private val context: android.content.Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BibleStoriesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BibleStoriesViewModel(repo, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class representation")
        }
    }
}
