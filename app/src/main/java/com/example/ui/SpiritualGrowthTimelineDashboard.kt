package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BibleStoryWithState
import com.example.data.StreakRecord
import com.example.data.DevotionalJournalEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// Helper function to format spiritual reflections as beautiful JSON
fun generateReflectionsJson(
    allStories: List<BibleStoryWithState>,
    streakRecord: StreakRecord,
    journalEntries: List<DevotionalJournalEntry>
): String {
    val completedWithReflections = allStories.filter { it.state.isCompleted || it.state.reflectionText.isNotBlank() }
    
    val root = JSONObject().apply {
        put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        put("totalCompletedDevotionals", allStories.count { it.state.isCompleted })
        put("totalWrittenJournalEntries", allStories.count { it.state.reflectionText.isNotBlank() })
        put("totalPersonalDevotionalJournals", journalEntries.size)
        
        val streakObj = JSONObject().apply {
            put("currentStreak", streakRecord.currentStreak)
            put("highestStreak", streakRecord.highestStreak)
            put("lastCompletedDateString", streakRecord.lastCompletedDateString)
        }
        put("streakTracker", streakObj)
        
        val listArray = JSONArray()
        completedWithReflections.forEach { storyWithState ->
            val storyObj = JSONObject().apply {
                put("id", storyWithState.story.id)
                put("title", storyWithState.story.title)
                put("bibleReference", storyWithState.story.bibleReference)
                put("category", storyWithState.story.category)
                put("isCompleted", storyWithState.state.isCompleted)
                put("lastUpdatedTimestamp", storyWithState.state.lastUpdated)
                put("reflectionText", storyWithState.state.reflectionText)
                put("aiReflectionPrompt", storyWithState.state.aiReflectionPrompt)
            }
            listArray.put(storyObj)
        }
        put("reflections", listArray)

        val journalArray = JSONArray()
        journalEntries.forEach { entry ->
            val journalObj = JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("scripturePassage", entry.scripturePassage)
                put("entryText", entry.entryText)
                put("dateCreated", entry.dateCreated)
            }
            journalArray.put(journalObj)
        }
        put("personalJournals", journalArray)
    }
    return root.toString(4) // pretty-print with 4 spaces
}

// Helper function to format spiritual reflections as beautiful text
fun generateReflectionsText(
    allStories: List<BibleStoryWithState>,
    streakRecord: StreakRecord,
    journalEntries: List<DevotionalJournalEntry>
): String {
    val sdf = SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.US)
    val currentDate = sdf.format(Date())
    
    val sb = java.lang.StringBuilder()
    sb.append("==================================================\n")
    sb.append("        MY SPIRITUAL JOURNAL REFLECTIONS          \n")
    sb.append("==================================================\n")
    sb.append("Generated on: ").append(currentDate).append("\n")
    sb.append("Total Devotionals Read: ").append(allStories.count { it.state.isCompleted }).append("\n")
    sb.append("Written Reflections: ").append(allStories.count { it.state.reflectionText.isNotBlank() }).append("\n")
    sb.append("Personal Journal Entries: ").append(journalEntries.size).append("\n")
    sb.append("Current Streak: ").append(streakRecord.currentStreak).append(" Days\n")
    sb.append("Highest Streak: ").append(streakRecord.highestStreak).append(" Days\n")
    sb.append("==================================================\n\n")
    
    sb.append("--- PERSONAL JOURNAL ENTRIES ---\n\n")
    if (journalEntries.isEmpty()) {
        sb.append("No personal journal entries written yet.\n\n")
    } else {
        journalEntries.forEachIndexed { index, entry ->
            val num = index + 1
            val dateFormed = sdf.format(Date(entry.dateCreated))
            sb.append(num).append(". ").append(entry.title.uppercase(Locale.US)).append("\n")
            if (entry.scripturePassage.isNotBlank()) {
                sb.append("   Scripture Passage: ").append(entry.scripturePassage).append("\n")
            }
            sb.append("   Date Created: ").append(dateFormed).append("\n")
            sb.append("   Reflection & Prayer:\n")
            sb.append("   \"").append(entry.entryText).append("\"\n\n")
        }
    }

    sb.append("--- BIBLE STORY DEVOTIONAL WORKBOOK ---\n\n")
    val activeItems = allStories.filter { it.state.isCompleted || it.state.reflectionText.isNotBlank() }
        .sortedBy { it.state.lastUpdated }
        
    if (activeItems.isEmpty()) {
        sb.append("You haven't recorded any story reflections yet! Start reading to build your timeline.\n")
    } else {
        activeItems.forEachIndexed { index, item ->
            val num = index + 1
            val dateFormed = sdf.format(Date(item.state.lastUpdated))
            sb.append(num).append(". ").append(item.story.title.uppercase(Locale.US)).append("\n")
            sb.append("   Bible Verse: ").append(item.story.bibleReference).append("\n")
            sb.append("   Category: ").append(item.story.category).append("\n")
            sb.append("   Date Updated: ").append(dateFormed).append("\n")
            sb.append("   Status: ").append(if (item.state.isCompleted) "Completed" else "In Progress").append("\n")
            
            if (item.state.reflectionText.isNotBlank()) {
                sb.append("   Personal Reflection:\n")
                sb.append("   \"").append(item.state.reflectionText).append("\"\n")
            } else {
                sb.append("   Status: Read and marked complete with no custom written entry.\n")
            }
            
            if (!item.state.aiReflectionPrompt.isNullOrBlank()) {
                sb.append("   AI Companion Guidance Prompt Used:\n")
                sb.append("   \"").append(item.state.aiReflectionPrompt).append("\"\n")
            }
            
            sb.append("\n--------------------------------------------------\n\n")
        }
    }
    
    sb.append("Keep walking in faith! Let the word guide your path.\n")
    sb.append("==================================================\n")
    return sb.toString()
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SpiritualGrowthTimelineDashboard(
    viewModel: BibleStoriesViewModel,
    onNavigateToDevotionals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()
    val streakRecord by viewModel.streakRecord.collectAsStateWithLifecycle()
    val todayReadingGoal by viewModel.todayReadingGoal.collectAsStateWithLifecycle()
    val journalEntries by viewModel.allDevotionalJournalEntries.collectAsStateWithLifecycle()

    val onSetDailyReadingGoal: (Int) -> Unit = { mins -> viewModel.setDailyReadingGoal(mins) }
    val onLogReadingMinutes: (Int) -> Unit = { mins -> viewModel.logReadingMinutes(mins) }
    val onStoryClick: (BibleStoryWithState) -> Unit = { story -> viewModel.setStoryActive(story) }

    var showOnlyWithReflections by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPastDevotionsDialog by remember { mutableStateOf(false) }
    var activeJournalForEdit by remember { mutableStateOf<DevotionalJournalEntry?>(null) }
    var showAddJournalDialog by remember { mutableStateOf(false) }
    var showEditJournalDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Set up standard Document Creators for exporting text & JSON files offline
    val exportTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                val textContent = generateReflectionsText(allStories, streakRecord, journalEntries)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(textContent.toByteArray())
                }
                Toast.makeText(context, "Journal reflections text file saved!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export text failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = generateReflectionsJson(allStories, streakRecord, journalEntries)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonContent.toByteArray())
                }
                Toast.makeText(context, "Journal reflections JSON backup saved! 🚀", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export JSON failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Dynamic stats computations based on real database state
    val totalCompleted = allStories.count { it.state.isCompleted }
    val totalWithReflections = allStories.count { it.state.reflectionText.isNotBlank() }
    val currentStreak = streakRecord.currentStreak
    val highestStreak = streakRecord.highestStreak

    // Calculate dominant spiritual theme/focus
    val completedStories = allStories.filter { it.state.isCompleted || it.state.reflectionText.isNotBlank() }
    val dominantTheme = completedStories
        .groupBy { it.story.category }
        .maxByOrNull { it.value.size }?.key ?: "Awaiting Reflection"

    // Calculate dynamic milestone rank
    val milestoneTitle = when {
        totalCompleted == 0 -> "Faith Seeker"
        totalCompleted in 1..2 -> "Seed Bearer"
        totalCompleted in 3..5 -> "Prayer Warrior"
        totalCompleted in 6..10 -> "Wisdom Companion"
        else -> "Spiritual Beacon"
    }

    val milestoneDescription = when {
        totalCompleted == 0 -> "Begin your journey by reading your first daily Bible devotional."
        totalCompleted in 1..2 -> "You are planting seeds of faith. Consistency will water them."
        totalCompleted in 3..5 -> "Defending and strengthening your heart with daily scripture."
        totalCompleted in 6..10 -> "Developing theological depth and rich scriptural insight."
        else -> "Shining the light of historical scripture in all that you do."
    }

    val milestoneIcon = when {
        totalCompleted == 0 -> Icons.Default.Star
        totalCompleted in 1..2 -> Icons.Default.Info
        totalCompleted in 3..5 -> Icons.Default.CheckCircle
        totalCompleted in 6..10 -> Icons.Default.Favorite
        else -> Icons.Default.CheckCircle
    }

    // Prepare list of items for timeline. Sort descending by last updated timestamp
    val timelineItems = remember(allStories, showOnlyWithReflections) {
        val filtered = if (showOnlyWithReflections) {
            allStories.filter { it.state.reflectionText.isNotBlank() }
        } else {
            allStories.filter { it.state.isCompleted || it.state.reflectionText.isNotBlank() }
        }
        filtered.sortedByDescending { it.state.lastUpdated }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Intro header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spiritual Growth",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Export Outlined Button
                    OutlinedButton(
                        onClick = {
                            val totalEntries = allStories.count { it.state.isCompleted || it.state.reflectionText.isNotBlank() }
                            if (totalEntries == 0) {
                                Toast.makeText(context, "No completed stories or reflections to export yet!", Toast.LENGTH_SHORT).show()
                            } else {
                                showExportDialog = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_reflections_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Offline Reflections File",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A detailed visual record of your steps in faith, reflection metrics, and lessons learned from the scriptures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )
            }
        }

        // Check and render modern structured export format dialog
        if (showExportDialog) {
            item {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, 
                                contentDescription = "Export Icon", 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Export Journal Reflections",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Preserve your prayers and devotional learnings offline. Choose your preferred file download format below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Plain Text Card Option
                                Card(
                                    onClick = {
                                        showExportDialog = false
                                        exportTextLauncher.launch("spiritual_reflections.txt")
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .testTag("export_text_format_card"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Text document",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Plain Text (.txt)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Structural JSON Card Option
                                Card(
                                    onClick = {
                                        showExportDialog = false
                                        exportJsonLauncher.launch("spiritual_reflections.json")
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .testTag("export_json_format_card"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "JSON document",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "JSON Data (.json)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(
                            onClick = { showExportDialog = false },
                            modifier = Modifier.testTag("dismiss_export_dialog_button")
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        if (showPastDevotionsDialog) {
            item {
                PastDevotionsDialog(
                    allStories = allStories,
                    journalEntries = journalEntries,
                    onSelectStory = onStoryClick,
                    onDeleteJournalEntry = { id -> viewModel.deleteDevotionalJournalEntry(id) },
                    onDismiss = { showPastDevotionsDialog = false },
                    streakRecord = streakRecord
                )
            }
        }

        if (showAddJournalDialog) {
            item {
                DevotionalJournalEntryDialog(
                    entry = null,
                    onSave = { title, scripture, text ->
                        viewModel.addDevotionalJournalEntry(title, scripture, text)
                        showAddJournalDialog = false
                    },
                    onDismiss = { showAddJournalDialog = false }
                )
            }
        }

        if (showEditJournalDialog && activeJournalForEdit != null) {
            item {
                DevotionalJournalEntryDialog(
                    entry = activeJournalForEdit,
                    onSave = { title, scripture, text ->
                        activeJournalForEdit?.let {
                            viewModel.updateDevotionalJournalEntry(
                                it.copy(title = title, scripturePassage = scripture, entryText = text)
                            )
                        }
                        showEditJournalDialog = false
                        activeJournalForEdit = null
                    },
                    onDismiss = {
                        showEditJournalDialog = false
                        activeJournalForEdit = null
                    }
                )
            }
        }

        // Daily Reading Goal & Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_reading_goal_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header: Goal Title & Goal Target Slider/Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Bible Reading Goal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Daily Scripture Reading Goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Nourish your spirit with focused daily study",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Progress details
                    val achieved = todayReadingGoal.minutesAchievedToday
                    val goal = todayReadingGoal.timeGoalMinutes
                    val progressFraction = if (goal > 0) achieved.toFloat() / goal.toFloat() else 0f
                    val progressPercent = (progressFraction * 100).coerceIn(0f, 100f).toInt()

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$achieved / $goal Mins Today",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (progressPercent >= 100) "Goal Succeeded! 🎉 Beautiful work today." else "Keep going to build your habit!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$progressPercent%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progressPercent >= 100) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // The Linear Progress Bar
                            LinearProgressIndicator(
                                progress = progressFraction.coerceIn(0f, 1f),
                                color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .testTag("reading_goal_progress_bar")
                            )
                        }
                    }

                    // Adjustment controls & quick logger action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section: ADJUST TARGET GOAL
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Target Goal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { if (goal > 5) onSetDailyReadingGoal(goal - 5) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("reading_goal_decrease_button")
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease goal", modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text(
                                        text = "${goal}m",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    FilledIconButton(
                                        onClick = { if (goal < 120) onSetDailyReadingGoal(goal + 5) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("reading_goal_increase_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase goal", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Section: MANUAL LOGGER
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Log Reading Mins",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { if (achieved > 0) onLogReadingMinutes(-5) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .weight(1f)
                                            .testTag("log_reading_time_decrease_button")
                                    ) {
                                        Text("-5m", style = MaterialTheme.typography.labelMedium)
                                    }

                                    FilledTonalButton(
                                        onClick = { onLogReadingMinutes(5) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .weight(1f)
                                            .testTag("log_reading_time_increase_button")
                                    ) {
                                        Text("+5m", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // A ticking Session timer focus module to track real-time study
                    var timerRunning by remember { mutableStateOf(false) }
                    var elapsedSeconds by remember { mutableStateOf(0) }

                    LaunchedEffect(timerRunning) {
                        if (timerRunning) {
                            while (timerRunning) {
                                kotlinx.coroutines.delay(1000)
                                elapsedSeconds += 1
                                if (elapsedSeconds >= 60) {
                                    onLogReadingMinutes(1)
                                    elapsedSeconds = 0
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (timerRunning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                val minutesFormatted = elapsedSeconds / 60
                                val secondsFormatted = elapsedSeconds % 60
                                Text(
                                    text = if (timerRunning) "Active Session: ${String.format(Locale.US, "%02d:%02d", minutesFormatted, secondsFormatted)}" else "Start Focused Reading Timer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (timerRunning) "Keeps screen active. Automatically commits every minute!" else "Read at your own pace while the app tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (timerRunning) {
                                    // Stop & commit any remaining seconds
                                    if (elapsedSeconds > 10) {
                                        onLogReadingMinutes(1)
                                        Toast.makeText(context, "Logged 1 minute focus time!", Toast.LENGTH_SHORT).show()
                                    }
                                    timerRunning = false
                                    elapsedSeconds = 0
                                } else {
                                    timerRunning = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (timerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("focus_session_timer_button")
                        ) {
                            Text(
                                text = if (timerRunning) "Pause" else "Start",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Today's Devotional Completion Progress Card
        item {
            val dailyStory = remember(allStories) {
                if (allStories.isEmpty()) null
                else {
                    val calendar = Calendar.getInstance()
                    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                    val index = dayOfYear % allStories.size
                    allStories[index]
                }
            }

            if (dailyStory != null) {
                val progress = when {
                    dailyStory.state.isCompleted || dailyStory.state.reflectionText.isNotBlank() -> 1f
                    dailyStory.state.lastUpdated > 0 -> 0.5f
                    else -> 0f
                }
                val progressPercent = (progress * 100).toInt()
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_devotion_completion_card")
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { onStoryClick(dailyStory) }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Today's Devotional Completion",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Today's Devotional Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = dailyStory.story.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progressPercent >= 100) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        progressPercent >= 100 -> "Devotion Complete! Journal Saved 🎉"
                                        progressPercent >= 50 -> "Scripture Reading Completed 📖"
                                        else -> "Pending Daily Reflection ✍️"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .testTag("today_devotional_progress_bar")
                            )
                        }
                    }
                }
            }
        }

        // Stats Grid Cards Area
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Main Level Progress Row Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = milestoneIcon,
                                contentDescription = "Milestone Rank ID",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Milestone: $milestoneTitle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${totalCompleted}/${allStories.size} Done",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = milestoneDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Grid stats details
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    // Total reflections count
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_reflections_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = totalWithReflections.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Written Journal Reflections",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Devotions Streak Record
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_streak_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$currentStreak Days",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current Devotion Streak (PB: $highestStreak)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Dominant Theme Focused
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_theme_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = dominantTheme,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Dominant Heart Theme Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // 30-Day Devotional Completion Streak Line Graph Card
        item {
            StreakLineGraph(
                allStories = allStories,
                currentStreak = streakRecord.currentStreak,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Calendar Devotional Reminder Schedule section
        item {
            var selectedHour by remember { mutableStateOf(8) }
            var selectedMinute by remember { mutableStateOf(0) }
            var isPmSelection by remember { mutableStateOf(false) } // False = AM, True = PM

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_reminder_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row with Icon & Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar Icon",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Daily Devotional Calendar Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Set and export a recurring daily reminder to your local calendar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Configuration Selector area (Hour, Minute, AM/PM)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hour Controller
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Hour",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { 
                                        selectedHour = if (selectedHour == 1) 12 else selectedHour - 1 
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.size(28.dp).testTag("reminder_hour_dec")
                                ) {
                                    Text(
                                        text = "−",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%02d", selectedHour),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.testTag("reminder_hour_value")
                                )
                                FilledIconButton(
                                    onClick = { 
                                        selectedHour = if (selectedHour == 12) 1 else selectedHour + 1 
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.size(28.dp).testTag("reminder_hour_inc")
                                ) {
                                    Text(
                                        text = "+",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        // Divider dots ":"
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        // Minute Controller
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Minute",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { 
                                        selectedMinute = if (selectedMinute <= 0) 55 else selectedMinute - 5 
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.size(28.dp).testTag("reminder_minute_dec")
                                ) {
                                    Text(
                                        text = "−",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%02d", selectedMinute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.testTag("reminder_minute_value")
                                )
                                FilledIconButton(
                                    onClick = { 
                                        selectedMinute = if (selectedMinute >= 55) 0 else selectedMinute + 5 
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.size(28.dp).testTag("reminder_minute_inc")
                                ) {
                                    Text(
                                        text = "+",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        // AM/PM Selector Row
                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Period",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (!isPmSelection) MaterialTheme.colorScheme.secondary 
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .clickable { isPmSelection = false }
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AM",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPmSelection) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isPmSelection) MaterialTheme.colorScheme.secondary 
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .clickable { isPmSelection = true }
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PM",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPmSelection) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Launch Export Calendar Button
                    Button(
                        onClick = {
                            try {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR, if (selectedHour == 12) 0 else selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    set(Calendar.AM_PM, if (isPmSelection) Calendar.PM else Calendar.AM)

                                    if (timeInMillis < System.currentTimeMillis()) {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                }
                                val eventStart = cal.timeInMillis
                                val eventEnd = eventStart + 20 * 60 * 1000 // 20 minutes duration

                                val calendarIntent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    data = android.provider.CalendarContract.Events.CONTENT_URI
                                    putExtra(android.provider.CalendarContract.Events.TITLE, "Daily Bible Devotional 📖")
                                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Daily time committed to reading Bible Story devotionals, reflecting, and praying.")
                                    putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventStart)
                                    putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, eventEnd)
                                    putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=DAILY;INTERVAL=1")
                                    putExtra(android.provider.CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                                }
                                context.startActivity(calendarIntent)
                                Toast.makeText(context, "Exporting to your calendar...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to launch calendar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_calendar_schedule_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Export Schedule to Calendar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Devotional Journal Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Devotional Journal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = { showAddJournalDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_journal_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Journal Entry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Write Entry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DevotionalConsistencyCard(viewModel = viewModel)

                if (journalEntries.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Your Devotional Journal is empty",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Record custom freeform journal entries, prayers, or personal Bible study notes to track your walks of faith.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // List recent journal entries
                    journalEntries.forEach { entry ->
                        JournalEntryRowItem(
                            entry = entry,
                            onEdit = {
                                activeJournalForEdit = entry
                                showEditJournalDialog = true
                            },
                            onDelete = {
                                viewModel.deleteDevotionalJournalEntry(entry.id)
                            }
                        )
                    }
                }
            }
        }

        // Timeline Filter Headers
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spiritual Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Button to open Full Past Devotions Archive Modal
                    TextButton(
                        onClick = { showPastDevotionsDialog = true },
                        modifier = Modifier.testTag("open_past_devotions_archive_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "View Archive",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "View Archive",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Journey milestone tracking feed:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    FilterChip(
                        selected = showOnlyWithReflections,
                        onClick = { showOnlyWithReflections = !showOnlyWithReflections },
                        label = { Text("Journal reflections only", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Timeline Feed items
        if (timelineItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Empty Timeline",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (showOnlyWithReflections) "No journal reflections saved." else "Timeline is currently blank.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showOnlyWithReflections) {
                                "Write personal insights in any Bible Story devotional workbook to record your first reflection."
                            } else {
                                "Complete your first scripture reading or save written devotions to generate your vertical spiritual walk record."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Button(
                            onClick = onNavigateToDevotionals,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_timeline_cta")
                        ) {
                            Text("Embark on Today's Devotional", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(timelineItems, key = { "timeline_${it.story.id}" }) { item ->
                TimelineRowItem(
                    item = item,
                    onItemClick = { onStoryClick(item) }
                )
            }
        }
    }
}

@Composable
fun TimelineRowItem(
    item: BibleStoryWithState,
    onItemClick: () -> Unit
) {
    // Elegant Date formatting
    val sdf = remember { SimpleDateFormat("MMMM d, yyyy", Locale.US) }
    val timeSdf = remember { SimpleDateFormat("h:mm a", Locale.US) }
    val formattedDate = remember(item.state.lastUpdated) {
        sdf.format(Date(item.state.lastUpdated))
    }
    val formattedTime = remember(item.state.lastUpdated) {
        timeSdf.format(Date(item.state.lastUpdated))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("timeline_item_${item.story.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Line & Circle Bullet decoration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            // Anchor Circle
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .border(
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    )
                    .background(color = MaterialTheme.colorScheme.background, shape = CircleShape)
            )

            // Connection line below
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(130.dp)
                    .background(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }

        // Timeline content card bubble
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            // Header: Date and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.story.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bible story references
            Text(
                text = item.story.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = item.story.bibleReference,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Personal Reflection
            if (item.state.reflectionText.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = "“${item.state.reflectionText}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Scripture read and marked complete.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // AI Reflector association tag
            if (!item.state.aiReflectionPrompt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "AI Prompt",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Reflected with AI Guidance Prompt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Revisit Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onItemClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Revisit Devotional",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalEntryRowItem(
    entry: com.example.data.DevotionalJournalEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.US) }
    val formattedDate = remember(entry.dateCreated) {
        sdf.format(Date(entry.dateCreated))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_journal_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_journal_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Entry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (entry.scripturePassage.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Scripture Focus",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = entry.scripturePassage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Text(
                text = entry.entryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionalJournalEntryDialog(
    entry: com.example.data.DevotionalJournalEntry?,
    onSave: (title: String, scripture: String, text: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(entry?.title ?: "") }
    var scripturePassage by remember { mutableStateOf(entry?.scripturePassage ?: "") }
    var entryText by remember { mutableStateOf(entry?.entryText ?: "") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (entry == null) "Write Devotional Journal" else "Edit Journal Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Reflect on God's word, write your prayers, thoughts, and lessons learned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Trusting in Difficulty") },
                    modifier = Modifier.fillMaxWidth().testTag("journal_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = scripturePassage,
                    onValueChange = { scripturePassage = it },
                    label = { Text("Scripture Passage (Optional)") },
                    placeholder = { Text("e.g., Romans 8:28") },
                    modifier = Modifier.fillMaxWidth().testTag("journal_scripture_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = entryText,
                    onValueChange = { entryText = it; showError = false },
                    label = { Text("Reflection & Prayer") },
                    placeholder = { Text("Write down what's on your heart...") },
                    modifier = Modifier.fillMaxWidth().height(160.dp).testTag("journal_text_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 8
                )

                if (showError) {
                    Text(
                        text = "Please enter both a title and reflection text.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || entryText.isBlank()) {
                        showError = true
                    } else {
                        onSave(title, scripturePassage, entryText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("journal_save_button")
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("journal_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
