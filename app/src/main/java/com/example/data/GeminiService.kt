package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

/**
 * Data class to hold the parsed AI generated daily story and lesson.
 */
@JsonClass(generateAdapter = true)
data class GeminiDailyStoryResult(
    val title: String,
    val bibleReference: String,
    val testament: String,
    val category: String,
    val verseHighlight: String,
    val summary: String,
    val keyLessons: List<String>,
    val suggestedPrayer: String,
    val reflectionPrompt: String,
    val prayerFocus: String,
    val thematicFocus: String = ""
)

/**
 * Data class to hold the parsed AI generated reflections.
 */
data class AiReflectionResult(
    val prompt: String,
    val prayerFocus: String
)

object GeminiHelper {
    /**
     * Fallback maps containing curated, deep theological prompts for all standard story IDs in the repository.
     */
    private val fallbackPrompts = mapOf(
        "creation" to AiReflectionResult(
            prompt = "How can you align your personal rhythms of work and rest with God's perfect creation pattern this week?",
            prayerFocus = "Thank God for the beauty of order in your life and ask for strength to practice holy rest."
        ),
        "noah_ark" to AiReflectionResult(
            prompt = "Where in your life is God calling you to step out in silent obedience, even when the surrounding culture says otherwise?",
            prayerFocus = "Trusting in God's promises and finding gratitude for being preserved in difficult seasons."
        ),
        "abraham_faith" to AiReflectionResult(
            prompt = "What is an 'unknown path' in your life right now where you need to trust God's voice over your own fears?",
            prayerFocus = "Surrendering your need to know every single future step, choosing instead to believe in God's faithfulness."
        ),
        "red_sea" to AiReflectionResult(
            prompt = "Is there a blocked path in your life where you need to cease striving and allow God to fight for you?",
            prayerFocus = "Asking for the peace of active stillness, believing that God opens dry paths in deep oceans."
        ),
        "david_goliath" to AiReflectionResult(
            prompt = "What giant of intimidation or anxiety are you facing today, and what past deliverances can you remember to find courage?",
            prayerFocus = "Drawing courage from God's identity, acknowledging that worldly weapons are nothing compared to His authority."
        ),
        "esther_courage" to AiReflectionResult(
            prompt = "In what way has God placed you in your current family, job, or circle of influence 'for such a time as this'?",
            prayerFocus = "Inviting courage to speak or stand up on behalf of the vulnerable, seeking strategic wisdom through prayer."
        ),
        "daniel_lions" to AiReflectionResult(
            prompt = "How can you keep your daily devotional devotion consistent, even when distractions or secular pressures mount?",
            prayerFocus = "Praying for uncompromised integrity and protection against adversaries that seek to divide you from Him."
        ),
        "jesus_birth" to AiReflectionResult(
            prompt = "If Jesus was born in a humble manger because there was no room in the inn, is there a cluttered area in your heart today where you need to make room for Him?",
            prayerFocus = "Welcoming Jesus into the ordinary, overlooked spaces of your life with gratitude."
        ),
        "sermon_mount" to AiReflectionResult(
            prompt = "In what practical way can you take a teaching of Jesus that you heard recently and put it into steady action today?",
            prayerFocus = "Building your life on the rock-solid foundation of practicing His word, not just listening to it."
        ),
        "prodigal_son" to AiReflectionResult(
            prompt = "Are you currently feeling like the prodigal son needing to return to the Father, or the older brother struggling with bitterness toward someone else's grace?",
            prayerFocus = "Rejoicing in running, eager love and asking for a compassionate heart that welcomes the lost."
        ),
        "good_samaritan" to AiReflectionResult(
            prompt = "Who is a 'neighbor' along your path today who has an urgent need, and what personal cost are you willing to incur to help them?",
            prayerFocus = "Forgiving busy indifference in your schedule and praying for notices of compassion for those left beaten by life's roads."
        ),
        "resurrection" to AiReflectionResult(
            prompt = "What transition of 'death' or hopelessness in your career, relationships, or health needs to be touched by the living resurrection power of Christ?",
            prayerFocus = "Declaring triumph over suffering and anxiety, praise God for the living hope of the empty tomb."
        )
    )

    fun getFallback(storyId: String): AiReflectionResult {
        return fallbackPrompts[storyId] ?: AiReflectionResult(
            prompt = "Take a moment to quiet your mind. What is one specific lesson from today's story that speaks directly to your circumstances?",
            prayerFocus = "Ask God for wisdom and guidance to live out his word in your home and workplace."
        )
    }

    /**
     * Fallback for generating a full dynamic story if API key is blank or request fails.
     */
    fun getFallbackDailyStory(): GeminiDailyStoryResult {
        return GeminiDailyStoryResult(
            title = "The Walk on the Water",
            bibleReference = "Matthew 14:22-33",
            testament = "New Testament",
            category = "Faith",
            verseHighlight = "But immediately Jesus spoke to them, saying, 'Take heart; it is I. Do not be afraid.'",
            summary = "After feeding the five thousand, Jesus sent his disciples ahead of him in a boat while he stayed behind to pray. In the middle of the night, a fierce storm arose. Jesus walked out to them on the water. Terrified, they thought he was a ghost, but he comforted them. Peter, in a burst of faith, asked to walk to Jesus on the water. He succeeded initially but sank when he saw the wind and became afraid. Jesus caught him, saying 'O you of little faith, why dId you doubt?'",
            keyLessons = listOf(
                "Faith focuses on Jesus rather than the surrounding circumstances or storms.",
                "Doubt creeps in when we look away from Jesus' authority and presence.",
                "Jesus is always close enough to reach out and catch us when we sink."
            ),
            suggestedPrayer = "Lord, grant me the courage to step out of the boat of my own comfort zones. Keep my eyes fixed solely on You rather than the terrifying waves of anxiety or doubt surrounding me.",
            reflectionPrompt = "What is the 'boat' or area of safety that God is calling you to step out of to meet Him in faith today?",
            prayerFocus = "Surrendering your fears of the storms in your life and keeping your focus on Jesus.",
            thematicFocus = "Trust & Courage"
        )
    }

    /**
     * Dynamically generates a complete new daily Bible story & lesson using the Gemini API.
     */
    suspend fun generateDailyBibleStory(apiKey: String): GeminiDailyStoryResult {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackDailyStory()
        }

        val promptStr = """
            You are a spiritual devotion assistant. Generate a highly engaging, deep, and complete daily Bible story, lesson, and reflection guide.
            Ensure the content contains:
            - A captivating title.
            - A precise Scripture Reference.
            - The testament ("Old Testament" or "New Testament").
            - A fitting category (e.g., "Faith", "Miracles", "Parables", "Wisdom", "Hope", "Love").
            - A specific thematic focus for the day, such as 'Forgiveness', 'Gratitude', 'Humility', 'Grace' or 'Hope' (thematicFocus).
            - A powerful, highlighted key verse (verseHighlight).
            - A clear, narrative summary (summary) of the scripture passage (4-6 sentences).
            - Exactly three key theological lessons (keyLessons) of 1-4 sentences each.
            - A comforting, aligned suggested prayer (suggestedPrayer).
            - A highly thought-provoking reflection prompt or question (reflectionPrompt) for journaling (1-2 sentences).
            - A brief, specific prayer focus or short prayer topic (prayerFocus) of 1 sentence.
            
            Return a raw JSON object containing exactly these keys:
            - "title": string
            - "bibleReference": string
            - "testament": string
            - "category": string
            - "thematicFocus": string
            - "verseHighlight": string
            - "summary": string
            - "keyLessons": array of strings (exactly 3 items)
            - "suggestedPrayer": string
            - "reflectionPrompt": string
            - "prayerFocus": string
            
            Respond with ONLY valid JSON. Do not write any markdown backticks, 'json' headers, or conversational introductions.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = promptStr)
                    )
                )
            )
        )

        return try {
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                val cleanedJson = jsonText.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                
                val jsonObj = JSONObject(cleanedJson)
                val lessonsArray = jsonObj.optJSONArray("keyLessons")
                val lessonsList = mutableListOf<String>()
                if (lessonsArray != null) {
                    for (i in 0 until lessonsArray.length()) {
                        lessonsList.add(lessonsArray.getString(i))
                    }
                }
                if (lessonsList.isEmpty()) {
                    lessonsList.addAll(getFallbackDailyStory().keyLessons)
                }

                GeminiDailyStoryResult(
                    title = jsonObj.optString("title", getFallbackDailyStory().title),
                    bibleReference = jsonObj.optString("bibleReference", getFallbackDailyStory().bibleReference),
                    testament = jsonObj.optString("testament", getFallbackDailyStory().testament),
                    category = jsonObj.optString("category", getFallbackDailyStory().category),
                    verseHighlight = jsonObj.optString("verseHighlight", getFallbackDailyStory().verseHighlight),
                    summary = jsonObj.optString("summary", getFallbackDailyStory().summary),
                    keyLessons = lessonsList,
                    suggestedPrayer = jsonObj.optString("suggestedPrayer", getFallbackDailyStory().suggestedPrayer),
                    reflectionPrompt = jsonObj.optString("reflectionPrompt", getFallbackDailyStory().reflectionPrompt),
                    prayerFocus = jsonObj.optString("prayerFocus", getFallbackDailyStory().prayerFocus),
                    thematicFocus = jsonObj.optString("thematicFocus", getFallbackDailyStory().thematicFocus)
                )
            } else {
                getFallbackDailyStory()
            }
        } catch (e: Exception) {
            getFallbackDailyStory()
        }
    }

    /**
     * Generates a unique reflection prompt and prayer focus calling the Gemini REST API.
     * Extracts values from returned JSON.
     */
    suspend fun generatePrompt(story: BibleStory, apiKey: String): AiReflectionResult {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallback(story.id)
        }

        val promptStr = """
            You are a spiritual devotion assistant. Generate a unique reflection prompt and prayer focus based on the Bible story:
            Title: ${story.title}
            Scripture Reference: ${story.bibleReference}
            Summary: ${story.summary}
            Key Lessons: ${story.keyLessons.joinToString(", ")}
            
            Return a raw JSON object containing exactly two keys:
            - "prompt": A highly thought-provoking reflection prompt or question (1-2 sentences) directly tied to the story's themes.
            - "prayerFocus": A brief, specific prayer focus or short prayer topic (1 sentence).
            
            Respond with ONLY valid JSON. Do not write any markdown backticks, 'json' headers, or conversational introductions.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = promptStr)
                    )
                )
            )
        )

        return try {
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                // Parse manually using standard org.json.JSONObject (guaranteed built-in on Android, extremely safe!)
                val cleanedJson = jsonText.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                
                val jsonObj = JSONObject(cleanedJson)
                AiReflectionResult(
                    prompt = jsonObj.optString("prompt", getFallback(story.id).prompt),
                    prayerFocus = jsonObj.optString("prayerFocus", getFallback(story.id).prayerFocus)
                )
            } else {
                getFallback(story.id)
            }
        } catch (e: Exception) {
            getFallback(story.id)
        }
    }

    private val fallbackUpliftingVerses = listOf(
        GeminiVerseResult(
            reference = "Romans 8:28",
            text = "And we know that for those who love God all things work together for good, for those who are called according to his purpose.",
            topic = "Purpose",
            summary = "This verse reassures believers that God orchestrates all circumstances of our lives—both blessings and challenges—to ultimately serve a greater, divine purpose of good for those who love Him."
        ),
        GeminiVerseResult(
            reference = "Philippians 4:13",
            text = "I can do all things through him who strengthens me.",
            topic = "Strength",
            summary = "A declaration of dependency on Christ's empowerment, reminding us that we can endure and overcome any trial, task, or circumstance through the supernatural strength He provides."
        ),
        GeminiVerseResult(
            reference = "Joshua 1:9",
            text = "Have I not commanded you? Be strong and courageous. Do not be frightened, and do not be dismayed, for the Lord your God is with you wherever you go.",
            topic = "Courage",
            summary = "God's powerful command to Joshua, and to all of us, to be courageous and strong, anchored in the unwavering promise that His guiding and protective presence is always with us."
        ),
        GeminiVerseResult(
            reference = "Proverbs 3:5-6",
            text = "Trust in the Lord with all your heart, and do not lean on your own understanding. In all your ways acknowledge him, and he will make straight your paths.",
            topic = "Trust",
            summary = "An instruction to place absolute, heartfelt reliance on God rather than our own finite understanding, promising that He will direct our paths when we acknowledge His sovereignty in everything."
        ),
        GeminiVerseResult(
            reference = "Isaiah 40:31",
            text = "But they who wait for the Lord shall renew their strength; they shall mount up with wings like eagles; they shall run and not be weary; they shall walk and not faint.",
            topic = "Hope",
            summary = "A beautiful promise of renewal and strength for those who patiently wait upon the Lord, indicating that divine endurance allows us to rise above exhaustion and soar spiritually."
        )
    )

    /**
     * Generates a highly inspiring, theme-appropriate Verse of the Day using the Gemini API.
     */
    suspend fun generateVerseOfTheDay(apiKey: String): GeminiVerseResult {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val fallback = fallbackUpliftingVerses.random()
            return fallback
        }

        val promptStr = """
            You are a spiritual devotion assistant. Generate a highly inspiring and beautiful "Verse of the Day" from the Holy Bible that brings comfort, hope, courage, or wisdom.
            Provide the Scripture reference, the exact text of the verse, a single-word topic summarizing its theme (e.g., "Strength", "Comfort", "Hope", "Peace", "Love", "Faith", "Joy"), and a brief summary or reflection (summary) explaining the verse's meaning and practical application in 1-3 clear sentences.
            
            Return a raw JSON object containing exactly these keys:
            - "reference": string (e.g. "Romans 8:28")
            - "text": string (the exact scripture text)
            - "topic": string (the single-word theme category)
            - "summary": string (a short summary or reflection of the verse's meaning and application)
            
            Respond with ONLY valid JSON. Do not write any markdown backticks, 'json' headers, or conversational introductions.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = promptStr)
                    )
                )
            )
        )

        return try {
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                val cleanedJson = jsonText.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                
                val jsonObj = JSONObject(cleanedJson)
                val referenceVal = jsonObj.optString("reference", "Romans 8:28")
                val textVal = jsonObj.optString("text", "And we know that for those who love God all things work together for good.")
                val topicVal = jsonObj.optString("topic", "Hope")
                val summaryVal = jsonObj.optString("summary", "God works in all things for the good of those who love him.")
                
                GeminiVerseResult(
                    reference = referenceVal,
                    text = textVal,
                    topic = topicVal,
                    summary = summaryVal
                )
            } else {
                fallbackUpliftingVerses.random()
            }
        } catch (e: Exception) {
            fallbackUpliftingVerses.random()
        }
    }
}

/**
 * Data class to hold the parsed Verse of the Day.
 */
@JsonClass(generateAdapter = true)
data class GeminiVerseResult(
    val reference: String,
    val text: String,
    val topic: String,
    val summary: String = ""
)

