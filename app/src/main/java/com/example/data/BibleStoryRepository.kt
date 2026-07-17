package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BibleStoryRepository(private val dao: StoryStateDao) {

    // Define the curated list of Bible stories with interactive content, lessons, prayers, etc.
    val staticStories = listOf(
        BibleStory(
            id = "creation",
            title = "The Creation of the World",
            bibleReference = "Genesis 1:1 - 2:3",
            testament = "Old Testament",
            category = "Creation",
            verseHighlight = "In the beginning, God created the heavens and the earth.",
            summary = "In the beginning, God brings elegant order, beauty, and Life out of darkness and formlessness. Over six magnificent days, He constructs the universe, land, plants, solar bodies, creatures, and finally humans in His own divine image, declaring His finished creation 'very good.' On the seventh day, God rests, sanctifying it as a holy rhythm for all of humanity.",
            keyLessons = listOf(
                "Divine Order: God is the sovereign source of all structure, purpose, and beauty in our lives.",
                "Inherent Dignity: Every human being is intentionally created in the image of God with sacred worth.",
                "Rhythm of Rest: Rest is not a reward for finished work, but a holy gift to restore our souls."
            ),
            suggestedPrayer = "Sovereign Creator, thank You for bringing beauty and purpose out of chaos. Help me align my life with Your holy rhythms and find peace in Your sovereign design today. Amen.",
            thematicFocus = "Order & Purpose"
        ),
        BibleStory(
            id = "noah_ark",
            title = "Noah’s Ark and the Rainbow Covenant",
            bibleReference = "Genesis 6:9 - 9:17",
            testament = "Old Testament",
            category = "Covenants",
            verseHighlight = "I have set my rainbow in the clouds, and it will be the sign of the covenant between me and the earth.",
            summary = "In a world gripped by corruption and violence, Noah stands out as an upright, faithful man of God. Instructed to build a colossal wooden ark to escape a cleansing flood, Noah obeys implicitly. Rebuilding life on dry land after the waters recede, God establishes a majestic, everlasting promise never to devastate the earth by flood again, sealing it with a radiant rainbow.",
            keyLessons = listOf(
                "Faithful Obedience: Living uprightly and obeying God's plans shields us in seasons of societal noise and storm.",
                "Mercy in Judgment: Even in times of consequence, God actively provides a safe harbor of salvation.",
                "Covenant Keeper: The rainbow stands as a timeless reminder that God never forgets His promises to His creation."
            ),
            suggestedPrayer = "Faithful Husbandman, thank You for being a shelter and shield in my times of trial. Give me the courage of Noah to build lives of steady obedience, trusting Your eternal mercy. Amen.",
            thematicFocus = "Obedience & Covenant"
        ),
        BibleStory(
            id = "abraham_faith",
            title = "Abraham’s Call and Unwavering Trust",
            bibleReference = "Genesis 12:1-9, 15:1-6",
            testament = "Old Testament",
            category = "Faith",
            verseHighlight = "Abram believed the Lord, and he credited it to him as righteousness.",
            summary = "God speaks to Abram, calling him to leave behind his familiar homeland, comforts, and kinship for a destination God would show him later. Along with the call comes a sweeping, multi-generational blessing. Despite physical barrenness and advanced age, Abraham chooses to believe God's promise of descendants as countless as the canopy of stars.",
            keyLessons = listOf(
                "Courageous Steps: True faith requires stepping into the unknown based on the character of the One who calls.",
                "Patience in Promises: God's timing is rarely quick, but His word remains unshakeable across generations.",
                "Credited Trust: Righteousness in God's eyes is built on a heart of trusting surrender, not perfect execution."
            ),
            suggestedPrayer = "God of Abraham, grant me the courage to step out onto unlit paths when You call. Help me trust Your grand perspective over my current limitations. Amen.",
            thematicFocus = "Unwavering Trust"
        ),
        BibleStory(
            id = "red_sea",
            title = "Parting of the Red Sea",
            bibleReference = "Exodus 14:1-31",
            testament = "Old Testament",
            category = "Miracles",
            verseHighlight = "The Lord will fight for you; you need only to be still.",
            summary = "Squeezed between Pharaoh’s hard-charging Egyptian war chariots and the insurmountable barriers of the Red Sea, the newly freed children of Israel panic. Moses challenges them to be still and witness God's deliverance. Raising his staff, a powerful wind sweeps back the waters, carving a miraculous path of safety and ultimate liberation.",
            keyLessons = listOf(
                "Active Stillness: When we are backed into a corner, quiet trust in God's authority is our dynamic defense.",
                "Supernatural Pathways: Our obstacles are opportunities for God to establish dry paths through deep oceans.",
                "Complete Deliverance: God will vindicate His people and finish the work of freedom He began."
            ),
            suggestedPrayer = "Mighty Deliverer, calm my racing heart and teach me the strength of being still. When pathways seem completely blocked, show me Your supernatural ways of escape. Amen."
        ),
        BibleStory(
            id = "david_goliath",
            title = "David and Goliath",
            bibleReference = "1 Samuel 17:1-51",
            testament = "Old Testament",
            category = "Faith",
            verseHighlight = "You come against me with sword and spear and javelin, but I come against you in the name of the Lord Almighty...",
            summary = "A colossal, highly trained champion soldier named Goliath terrorizes Saul's Israelite army for forty days, mocking their God. Armed only with a simple shepherd's sling, five smooth stream stones, and a fierce, resting confidence in the Lord of Heavens, simple shepherd boy David steps forward and defeats the giant, displaying that battle victory belongs to the Lord.",
            keyLessons = listOf(
                "Invisible Armor: Faith in God's authority is infinitely superior to the heavier gears and armaments of this world.",
                "Faithful History: Reflecting on how God delivered David from past wild beasts fuels his courage for the current giant.",
                "Sovereignty of Weakness: God glorifies His name by overcoming massive strongholds through small, humble instruments."
            ),
            suggestedPrayer = "Lord of Hosts, protect me from intimidation. Teach me to draw courage from past deliverances, knowing that giants are small when placed against Your infinite power. Amen."
        ),
        BibleStory(
            id = "esther_courage",
            title = "Queen Esther’s Brave Advocate",
            bibleReference = "Esther 4:1-17, 5:1-8",
            testament = "Old Testament",
            category = "Wisdom",
            verseHighlight = "And who knows but that you have come to your royal position for such a time as this?",
            summary = "With a cruel royal decree threatening the systematic execution of all Jewish residents across Persia, the exiled orphan-turned-queen Esther faces an agonizing split. Uncalled visits to the king carry immediate execution. Urged by her cousin Mordecai, Ester requests a national fast, gathers her courage, and steps forward with the legendary resolve: 'If I perish, I perish.'",
            keyLessons = listOf(
                "Divine Placement: We are placed purposely in our current jobs, stations, and communities for a sacred mission.",
                "Sacrificial Courage: True faith acts on behalf of others, even when the personal cost is high.",
                "Strategic Wisdom: Combining prayer and fasting with intelligent planning and graceful presentation completes our task."
            ),
            suggestedPrayer = "Sovereign Guide, grant me Esther's bravery to speak on behalf of the vulnerable. Let me never shrink from my purpose, trusting that You placed me in this hour for such a time as this. Amen."
        ),
        BibleStory(
            id = "daniel_lions",
            title = "Daniel and the Den of Lions",
            bibleReference = "Daniel 6:1-28",
            testament = "Old Testament",
            category = "Faith",
            verseHighlight = "My God sent his angel, and he shut the mouths of the lions. They have not hurt me, because I was found innocent in his sight.",
            summary = "Jealous government rivals deceive the Persian king into declaring an unalterable decree: for thirty days, anyone praying to any god or human other than the king must be thrown to wild lions. Knowing this, Daniel prays to God openly three times a day. Arrested and thrown to the beasts, Daniel is miraculously unscrewed by angels, proving God shields those of deep integrity.",
            keyLessons = listOf(
                "Unbroken Devotion: Our dedication to prayer should remain constant, regardless of worldly threats or peer pressure.",
                "Integrity as Armor: Solid, everyday consistency and innocent character are highly honored by the Creator.",
                "Ultimate Safety: God holds the power to calm and shut the mouths of the fiercest adversaries that threaten my peace."
            ),
            suggestedPrayer = "Incorruptible Lord, give me a heart of uncompromised devotion. When worldly pressures demand compromise, keep my eyes raised and my hands steady in trusted devotion to You. Amen."
        ),
        BibleStory(
            id = "jesus_birth",
            title = "The Humble Birth of the Savior",
            bibleReference = "Luke 2:1-20",
            testament = "New Testament",
            category = "Grace",
            verseHighlight = "Today in the town of David a Savior has been born to you; he is the Messiah, the Lord.",
            summary = "The King of Glory enters our heavy humanness under remarkably humble, obscure circumstances. Born in Bethlehem, wrapped in cheap cloths, and cradled in a coarse feeding trough because there was no room in the inn, Jesus' birth is heralded by a starry angelic choir to humble shepherds working night watch, inviting everyone to discover unconditional Grace.",
            keyLessons = listOf(
                "The Beauty of Humility: God chooses the modest, ordinary spaces of life to reveal His greatest masterpieces.",
                "Inclusion of all: The gospel's introductory invitation went to rough shepherds, showing no one is excluded from grace.",
                "Manger to Heart: Finding peace begins when we make room in our busy schedules and hearts for the Humble Savior."
            ),
            suggestedPrayer = "Gentle Savior, thank You for approaching our brokenness with humility. Cleanse the cluttered inn of my heart, and let my ordinary moments become holy ground for Your love to dwell. Amen."
        ),
        BibleStory(
            id = "sermon_mount",
            title = "The Sermon on the Mount",
            bibleReference = "Matthew 5:1-16, 7:24-29",
            testament = "New Testament",
            category = "Wisdom",
            verseHighlight = "Therefore everyone who hears these words of mine and puts them into practice is like a wise man who built his house on the rock.",
            summary = "Seated on a Galilee hillside, Jesus delivers a revolutionary, life-altering charter of God's Kingdom. He flips human concepts of blessing upside down via the Beatitudes, calling His followers to be salt and light. He wraps up with a profound invitation to construct our lives upon the bedrock of His words so we can stand firm during life's severe storms.",
            keyLessons = listOf(
                "Upside-down Blessings: True happiness is anchored in spiritual poorness, meekness, and pureness of heart.",
                "Light in the Shadows: We are called to actively illuminate suffering with deeds of kindness and peace.",
                "Rock-Solid Foundation: Sincere hearing must translate into active, daily practicing of Jesus’ teachings to remain unshakeable."
            ),
            suggestedPrayer = "Master Builder, search my foundations. Deliver me from hearing Your word without practicing it. Let my thoughts, talk, and choices be firmly grounded on the Rock of Your truth. Amen."
        ),
        BibleStory(
            id = "prodigal_son",
            title = "The Parable of the Prodigal Son",
            bibleReference = "Luke 15:11-32",
            testament = "New Testament",
            category = "Parables",
            verseHighlight = "For this son of mine was dead and is alive again; he was lost and is found.",
            summary = "A rebellious younger son demands his inheritance early, heads to a distant country, and wastes everything on empty pleasures. Hit by severe poverty, he returns home hoping to beg for a servant's work. Instead, his watching father spots him from afar, runs, falls on his neck with kisses, and celebrates his return with a rich feast of complete restoration.",
            keyLessons = listOf(
                "The Limitless Father: God's love is active and running; He watches for our return long before we reach Him.",
                "Complete Reconciliation: Sincere repentance is met with lavish robes of grace, not conditions of slavery.",
                "Healing the Self-Righteous: We must guard against the older brother's bitter envy, learning to rejoice when lost souls return."
            ),
            suggestedPrayer = "Patient Father, thank You for Your running, eager love that welcomes me home when I stagger. Keep me humble, and fill me with Your joyful grace to welcome lost brothers and sisters. Amen."
        ),
        BibleStory(
            id = "good_samaritan",
            title = "The Parable of the Good Samaritan",
            bibleReference = "Luke 10:25-37",
            testament = "New Testament",
            category = "Parables",
            verseHighlight = "But a Samaritan, as he traveled, came where the man was; and when he saw him, he took pity on him.",
            summary = "A traveler is beaten, robbed, and left bloodied along a desolate road. Respectable religious leaders walk past, ignoring his distress. It is a despised Samaritan traveling traveler who stops, wraps his wounds, transports him to safety, and deposits money for his complete recovery, defining true neighborly love.",
            keyLessons = listOf(
                "Mercy Over Titles: God values acts of immediate compassion far above clean hands or doctrinal status.",
                "Expanded Boundaries: Our neighbor is anyone in our path who has an urgent need, regardless of race, creed, or background.",
                "Costly Devotion: Genuine love demands that we sacrifice our time, energy, convenience, and resources."
            ),
            suggestedPrayer = "Merciful Teacher, forgive my busy indifference. Give me eyes that notice the broken along my daily paths and a willing spirit to incur personal costs to heal and comfort them. Amen."
        ),
        BibleStory(
            id = "resurrection",
            title = "The Triumph of the Resurrection",
            bibleReference = "Luke 24:1-12, Matthew 28:1-10",
            testament = "New Testament",
            category = "Grace",
            verseHighlight = "He is not here; he has risen! Remember how he told you...",
            summary = "Early on the third morning after the crucifixion, weeping female followers of Jesus enter the garden tomb to find the massive rock rolled back. An angel delivers a earth-shaking declaration: Christ is risen. Jesus appears alive to His disciples, forever breaking the finality of death and filling the human scope with eternal hope and power.",
            keyLessons = listOf(
                "The Final Word: Death, suffering, and sin do not possess final authority; He has fully triumphed over them.",
                "Living Hope: The Christian faith is anchored on a physical, historic, and living Savior, not dry philosophies.",
                "Resurrection Power: Same power that raised Christ from the dead lives in us to transform our daily moments of death."
            ),
            suggestedPrayer = "Risen King, thank You for conquering the grave and washing our futures with living hope. Let Your resurrection power fill my weak spots today, casting out all fear of death. Amen."
        )
    )

    // Combined flow merging static stories with user state custom data in the Room database
    val allStoriesWithState: Flow<List<BibleStoryWithState>> = dao.getAllStoryStates().map { states ->
        val stateMap = states.associateBy { it.storyId }
        staticStories.map { story ->
            val state = stateMap[story.id] ?: StoryState(storyId = story.id)
            BibleStoryWithState(story, state)
        }
    }.flowOn(Dispatchers.IO)

    val streakRecordFlow: Flow<StreakRecord> = dao.getStreakRecordFlow().map { record ->
        record ?: StreakRecord()
    }.flowOn(Dispatchers.IO)

    suspend fun toggleBookmark(storyId: String) = withContext(Dispatchers.IO) {
        val currentState = dao.getStoryState(storyId) ?: StoryState(storyId = storyId)
        val updated = currentState.copy(
            isBookmarked = !currentState.isBookmarked,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertStoryState(updated)
    }

    suspend fun updateLastViewed(storyId: String) = withContext(Dispatchers.IO) {
        val currentState = dao.getStoryState(storyId) ?: StoryState(storyId = storyId)
        val updated = currentState.copy(
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertStoryState(updated)
    }

    suspend fun togglePassageBookmark(storyId: String) = withContext(Dispatchers.IO) {
        val currentState = dao.getStoryState(storyId) ?: StoryState(storyId = storyId)
        val updated = currentState.copy(
            isPassageBookmarked = !currentState.isPassageBookmarked,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertStoryState(updated)
    }

    suspend fun toggleCompleted(storyId: String) = withContext(Dispatchers.IO) {
        val currentState = dao.getStoryState(storyId) ?: StoryState(storyId = storyId)
        val newCompleted = !currentState.isCompleted
        val updated = currentState.copy(
            isCompleted = newCompleted,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertStoryState(updated)

        if (newCompleted) {
            updateStreakOnCompletion()
        } else {
            recalculateStreak()
        }
    }

    suspend fun getStoryStateDirect(storyId: String): StoryState? = withContext(Dispatchers.IO) {
        dao.getStoryState(storyId)
    }

    suspend fun saveStoryStateDirect(state: StoryState) = withContext(Dispatchers.IO) {
        dao.insertStoryState(state)
    }

    suspend fun saveReflection(storyId: String, text: String) = withContext(Dispatchers.IO) {
        val currentState = dao.getStoryState(storyId) ?: StoryState(storyId = storyId)
        val wasCompleted = currentState.isCompleted
        val newCompleted = text.isNotBlank() || wasCompleted
        val updated = currentState.copy(
            reflectionText = text,
            isCompleted = newCompleted, // Auto-complete if they wrote a reflection
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertStoryState(updated)

        if (newCompleted && !wasCompleted) {
            updateStreakOnCompletion()
        }
    }

    suspend fun generateOrFetchAiPrompt(storyId: String, apiKey: String): AiReflectionResult = withContext(Dispatchers.IO) {
        val currentState = dao.getStoryState(storyId) ?: StoryState(storyId = storyId)
        if (!currentState.aiReflectionPrompt.isNullOrEmpty() && !currentState.aiPrayerFocus.isNullOrEmpty()) {
            return@withContext AiReflectionResult(
                prompt = currentState.aiReflectionPrompt,
                prayerFocus = currentState.aiPrayerFocus
            )
        }

        val story = staticStories.find { it.id == storyId } ?: return@withContext GeminiHelper.getFallback(storyId)
        val generated = GeminiHelper.generatePrompt(story, apiKey)

        val updated = currentState.copy(
            aiReflectionPrompt = generated.prompt,
            aiPrayerFocus = generated.prayerFocus,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertStoryState(updated)
        return@withContext generated
    }

    private fun getTodayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun getYesterdayDateString(): String {
        val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday)
    }

    private suspend fun updateStreakOnCompletion() {
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val record = dao.getStreakRecord() ?: StreakRecord()

        val lastDate = record.lastCompletedDateString
        if (lastDate == today) {
            // Already completed something today, streak stands
            return
        }

        val newStreak = if (lastDate == yesterday) {
            record.currentStreak + 1
        } else {
            1
        }
        val newHighest = maxOf(newStreak, record.highestStreak)
        dao.insertStreakRecord(
            record.copy(
                currentStreak = newStreak,
                highestStreak = newHighest,
                lastCompletedDateString = today
            )
        )
    }

    private suspend fun recalculateStreak() {
        val states = dao.getStoryStatesSync()
        val today = getTodayDateString()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val anyToday = states.any { state ->
            state.isCompleted && format.format(Date(state.lastUpdated)) == today
        }

        if (!anyToday) {
            val record = dao.getStreakRecord() ?: return
            val yesterday = getYesterdayDateString()
            val anyYesterday = states.any { state ->
                state.isCompleted && format.format(Date(state.lastUpdated)) == yesterday
            }

            if (anyYesterday) {
                val prevStreak = maxOf(0, record.currentStreak - 1)
                dao.insertStreakRecord(
                    record.copy(
                        currentStreak = prevStreak,
                        lastCompletedDateString = yesterday
                    )
                )
            } else {
                dao.insertStreakRecord(
                    record.copy(
                        currentStreak = 0,
                        lastCompletedDateString = ""
                    )
                )
            }
        }
    }

    fun getDailyReadingGoalFlow(dateString: String): Flow<DailyReadingGoal?> {
        return dao.getDailyReadingGoalFlow(dateString)
    }

    suspend fun setDailyReadingGoal(dateString: String, goalMinutes: Int) = withContext(Dispatchers.IO) {
        val current = dao.getDailyReadingGoal(dateString) ?: DailyReadingGoal(dateString = dateString)
        dao.insertDailyReadingGoal(current.copy(timeGoalMinutes = goalMinutes))
    }

    suspend fun logReadingMinutes(dateString: String, deltaMinutes: Int) = withContext(Dispatchers.IO) {
        val current = dao.getDailyReadingGoal(dateString) ?: DailyReadingGoal(dateString = dateString)
        val newDuration = maxOf(0, current.minutesAchievedToday + deltaMinutes)
        dao.insertDailyReadingGoal(current.copy(minutesAchievedToday = newDuration))
    }

    suspend fun getFullBackupData(): Triple<List<StoryState>, StreakRecord?, List<DailyReadingGoal>> = withContext(Dispatchers.IO) {
        val states = dao.getStoryStatesSync()
        val streak = dao.getStreakRecord()
        val goals = dao.getAllDailyReadingGoalsSync()
        Triple(states, streak, goals)
    }

    suspend fun restoreBackupData(
        storyStates: List<StoryState>,
        streakRecord: StreakRecord?,
        dailyGoals: List<DailyReadingGoal>
    ) = withContext(Dispatchers.IO) {
        if (storyStates.isNotEmpty()) {
            dao.insertStoryStatesBulk(storyStates)
        }
        if (streakRecord != null) {
            dao.insertStreakRecord(streakRecord)
        }
        if (dailyGoals.isNotEmpty()) {
            dao.insertDailyReadingGoalsBulk(dailyGoals)
        }
    }

    suspend fun clearLocalStates() = withContext(Dispatchers.IO) {
        dao.clearStoryStates()
        dao.clearDailyReadingGoals()
        dao.clearStreakRecords()
        dao.clearPrayerRequests()
    }

    // --- Personal Prayer Wall Repository API ---

    val allPrayerRequests: Flow<List<PrayerRequest>> = dao.getAllPrayerRequests().flowOn(Dispatchers.IO)

    suspend fun addPrayerRequest(text: String, includeInDailyReflection: Boolean, category: String = "General") = withContext(Dispatchers.IO) {
        val prayer = PrayerRequest(
            text = text,
            includeInDailyReflection = includeInDailyReflection,
            dateAdded = System.currentTimeMillis(),
            isAnswered = false,
            category = category
        )
        dao.insertPrayerRequest(prayer)
    }

    suspend fun updatePrayerAnswerStatus(id: Long, isAnswered: Boolean, answerText: String) = withContext(Dispatchers.IO) {
        // Retrieve current prayer, or update directly if we had a full query. Let's do a Flow lookup or update query.
        // Wait, since we can just update via standard DAO object if we retrieve it,
        // or we can select first:
        // Wait, does StoryStateDao have a getPrayerRequest(id: Long)? We can easily query all or write a simple update/replace.
        // To be simple and robust, let's load all or just write a specific query in DAO, or update directly.
        // Wait, since we are in the repository, we can do:
        // Let's look up the prayer request from the flow's current state.
        // Or better yet, we can add a specific direct query to StoryStateDao!
        // Wait, let's see, a direct query is extremely clean:
        // "@Query("SELECT * FROM prayer_requests WHERE id = :id LIMIT 1") suspend fun getPrayerRequest(id: Long): PrayerRequest?"
        // Let's add that to StoryStateDao as well if we need, or we can just pass the original PrayerRequest object to the update.
        // Yes! Passing the actual PrayerRequest object to update is standard and incredibly simple, requiring no extra query.
        // Let's implement that!
    }

    suspend fun updatePrayerRequest(prayer: PrayerRequest) = withContext(Dispatchers.IO) {
        dao.insertPrayerRequest(prayer) // OnConflictStrategy.REPLACE functions as update
    }

    suspend fun deletePrayerRequest(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePrayerRequestById(id)
    }

    // --- Personal Devotional Journal Repository API ---

    val allDevotionalJournalEntries: Flow<List<DevotionalJournalEntry>> = 
        dao.getAllDevotionalJournalEntries().flowOn(Dispatchers.IO)

    suspend fun addDevotionalJournalEntry(title: String, scripturePassage: String, entryText: String) = withContext(Dispatchers.IO) {
        val entry = DevotionalJournalEntry(
            title = title,
            scripturePassage = scripturePassage,
            entryText = entryText,
            dateCreated = System.currentTimeMillis()
        )
        dao.insertDevotionalJournalEntry(entry)
    }

    suspend fun updateDevotionalJournalEntry(entry: DevotionalJournalEntry) = withContext(Dispatchers.IO) {
        dao.updateDevotionalJournalEntry(entry)
    }

    suspend fun deleteDevotionalJournalEntry(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteDevotionalJournalEntryById(id)
    }

    suspend fun clearAllDevotionalJournalEntries() = withContext(Dispatchers.IO) {
        dao.clearAllDevotionalJournalEntries()
    }
}
