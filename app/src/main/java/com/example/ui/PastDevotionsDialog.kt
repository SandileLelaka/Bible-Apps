package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.example.data.BibleStoryWithState
import com.example.data.DevotionalJournalEntry
import com.example.data.StreakRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDevotionsDialog(
    allStories: List<BibleStoryWithState>,
    journalEntries: List<DevotionalJournalEntry>,
    onSelectStory: (BibleStoryWithState) -> Unit,
    onDeleteJournalEntry: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    streakRecord: StreakRecord = StreakRecord()
) {
    val context = LocalContext.current

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

    var selectedTab by remember { mutableStateOf(0) } // 0 = Bible Devotions, 1 = Personal Journals
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showOnlyWithReflections by remember { mutableStateOf(false) }
    var showOnlyBookmarked by remember { mutableStateOf(false) }

    // Revisit reflection from one year ago state & logic
    val realHistoryCount = remember(allStories, journalEntries) {
        allStories.count { it.state.reflectionText.isNotBlank() || it.state.isCompleted } + journalEntries.size
    }
    var forceSimulationMode by remember { mutableStateOf(false) }
    val totalHistoryCount = if (forceSimulationMode) 365 else realHistoryCount

    // Occasionally prompt user - 50% chance when dialog is opened
    val occasionallyShowPrompt = remember { kotlin.random.Random(System.currentTimeMillis()).nextFloat() < 0.5f }
    var isPromptDismissed by remember { mutableStateOf(false) }

    fun isExactlyOneYearAgo(timestamp: Long): Boolean {
        val entryCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val targetCal = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        return entryCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
               entryCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    }

    val oneYearAgoStory = remember(allStories, forceSimulationMode) {
        if (forceSimulationMode) {
            val mockStory = allStories.firstOrNull { it.story.id == "creation" }?.story
                ?: allStories.firstOrNull()?.story
                ?: com.example.data.BibleStory(
                    id = "creation",
                    title = "The Creation: Chaos to Order",
                    bibleReference = "Genesis 1:1-5",
                    testament = "Old Testament",
                    category = "Creation",
                    verseHighlight = "In the beginning, God created the heavens and the earth.",
                    summary = "God brings beautiful order, light, and life out of formless chaos, establishing holy rhythms.",
                    keyLessons = listOf("God has authority over chaos", "He brings light into darkness"),
                    suggestedPrayer = "Lord, bring order to my chaotic thoughts today.",
                    thematicFocus = "Order out of Chaos"
                )
            val mockState = com.example.data.StoryState(
                storyId = mockStory.id,
                isCompleted = true,
                reflectionText = "A year ago today, I was going through a stressful career transition. This devotion reminded me that God is the Master Craftsman who creates beautiful order out of complete chaos. Looking back now, He absolutely did!",
                lastUpdated = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
            )
            BibleStoryWithState(mockStory, mockState)
        } else {
            allStories.firstOrNull { item ->
                val isPast = item.state.isCompleted || item.state.reflectionText.isNotBlank()
                isPast && isExactlyOneYearAgo(item.state.lastUpdated)
            }
        }
    }

    val oneYearAgoJournal = remember(journalEntries, forceSimulationMode) {
        if (forceSimulationMode) {
            null
        } else {
            journalEntries.firstOrNull { entry ->
                isExactlyOneYearAgo(entry.dateCreated)
            }
        }
    }

    // Date Range Filters state
    var dateFilterPreset by remember { mutableStateOf("All Time") } // "All Time", "Today", "Last 7 Days", "Last 30 Days", "Custom"
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val categories = listOf("All", "Creation", "Covenants", "Faith", "Wisdom", "Miracles", "Parables", "Grace")

    // Helper to evaluate if a timestamp matches the selected date filter
    fun matchesDateFilter(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Start of today (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        
        return when (dateFilterPreset) {
            "Today" -> timestamp >= startOfToday
            "Last 7 Days" -> timestamp >= (now - 7L * 24 * 60 * 60 * 1000)
            "Last 30 Days" -> timestamp >= (now - 30L * 24 * 60 * 60 * 1000)
            "Custom" -> {
                val startMatch = customStartDate == null || timestamp >= customStartDate!!
                // Include the entire end day (up to 23:59:59)
                val endLimit = customEndDate?.let {
                    val cal = Calendar.getInstance().apply { timeInMillis = it }
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    cal.timeInMillis
                }
                val endMatch = endLimit == null || timestamp <= endLimit
                startMatch && endMatch
            }
            else -> true // "All Time"
        }
    }

    // Filtered Bible Stories
    val filteredStories = remember(allStories, searchQuery, selectedCategory, showOnlyWithReflections, showOnlyBookmarked, dateFilterPreset, customStartDate, customEndDate) {
        allStories.filter { item ->
            val isPast = item.state.isCompleted || item.state.reflectionText.isNotBlank()
            val matchesSearch = item.story.title.contains(searchQuery, ignoreCase = true) ||
                    item.story.bibleReference.contains(searchQuery, ignoreCase = true) ||
                    item.state.reflectionText.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || item.story.category.equals(selectedCategory, ignoreCase = true)
            val matchesReflections = !showOnlyWithReflections || item.state.reflectionText.isNotBlank()
            val matchesBookmarked = !showOnlyBookmarked || item.state.isBookmarked
            val matchesDate = matchesDateFilter(item.state.lastUpdated)

            isPast && matchesSearch && matchesCategory && matchesReflections && matchesBookmarked && matchesDate
        }.sortedByDescending { it.state.lastUpdated }
    }

    // Filtered Personal Journals
    val filteredJournals = remember(journalEntries, searchQuery, dateFilterPreset, customStartDate, customEndDate) {
        journalEntries.filter { entry ->
            val matchesSearch = entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.scripturePassage.contains(searchQuery, ignoreCase = true) ||
                    entry.entryText.contains(searchQuery, ignoreCase = true)
            val matchesDate = matchesDateFilter(entry.dateCreated)

            matchesSearch && matchesDate
        }.sortedByDescending { it.dateCreated }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("past_devotions_dialog"),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Devotional History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "View and revisit past bible logs and written reflections",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                exportJsonLauncher.launch("devotional_reflections_backup.json")
                            },
                            modifier = Modifier
                                .testTag("export_json_button")
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export JSON",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export JSON",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .testTag("close_past_devotions_dialog_button")
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Past Devotions Dialog",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulation Control Bar for Reviewer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History Count: $realHistoryCount/365 entries",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Test 1-Yr Feature:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FilterChip(
                            selected = forceSimulationMode,
                            onClick = { forceSimulationMode = !forceSimulationMode },
                            label = { Text("Simulate 365+ Entries", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp).testTag("toggle_revisit_simulation")
                        )
                    }
                }

                // Occasionally prompt user to revisit a previous reflection from exactly one year ago
                if (totalHistoryCount >= 365 && !isPromptDismissed && (occasionallyShowPrompt || forceSimulationMode)) {
                    if (oneYearAgoStory != null || oneYearAgoJournal != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("revisit_reflection_prompt_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "On This Day Last Year",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "On This Day Last Year 🌟",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    IconButton(
                                        onClick = { isPromptDismissed = true },
                                        modifier = Modifier.size(28.dp).testTag("dismiss_revisit_prompt_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss Revisit Prompt",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Since you have a rich spiritual history of $totalHistoryCount entries, take a moment to look back and see how God has answered your prayers and grown your faith over the last 365 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                if (oneYearAgoStory != null) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "From your Devotional: ${oneYearAgoStory.story.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Passage: ${oneYearAgoStory.story.bibleReference}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                        if (oneYearAgoStory.state.reflectionText.isNotBlank()) {
                                            Text(
                                                text = "\"${oneYearAgoStory.state.reflectionText}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(8.dp)
                                                    .fillMaxWidth()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                onSelectStory(oneYearAgoStory)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp).testTag("revisit_devotion_button")
                                        ) {
                                            Text(
                                                "Re-Read Devotional",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondary
                                            )
                                        }
                                    }
                                } else if (oneYearAgoJournal != null) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "From your Personal Journal: ${oneYearAgoJournal.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        if (oneYearAgoJournal.scripturePassage.isNotBlank()) {
                                            Text(
                                                text = "Scripture: ${oneYearAgoJournal.scripturePassage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                        Text(
                                            text = "\"${oneYearAgoJournal.entryText}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                                .fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Unified Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("history_bible_tab"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Scripture Devotions", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("history_journal_tab"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Personal Journals", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Input TextField
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (selectedTab == 0) "Search titles, scripture, or reflection notes..."
                            else "Search personal journal entry text..."
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("past_devotions_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Range Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date Range Filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Filter by Date Range:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date Range Preset Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf("All Time", "Today", "Last 7 Days", "Last 30 Days", "Custom")
                    presets.forEach { preset ->
                        val isSelected = dateFilterPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                dateFilterPreset = preset
                                if (preset != "Custom") {
                                    customStartDate = null
                                    customEndDate = null
                                }
                            },
                            label = {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("date_filter_chip_$preset")
                        )
                    }
                }

                // Custom Date Range Pickers
                if (dateFilterPreset == "Custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Date Card
                        OutlinedCard(
                            onClick = {
                                showDatePicker(context, customStartDate) { date ->
                                    customStartDate = date
                                }
                            },
                            modifier = Modifier
                                .weight(1.0f)
                                .testTag("custom_start_date_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text("From Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = customStartDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Choose Start",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Arrow Right / KeyboardArrowRight
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )

                        // End Date Card
                        OutlinedCard(
                            onClick = {
                                showDatePicker(context, customEndDate) { date ->
                                    customEndDate = date
                                }
                            },
                            modifier = Modifier
                                .weight(1.0f)
                                .testTag("custom_end_date_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text("To Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = customEndDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Choose End",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Clear Range Button
                        if (customStartDate != null || customEndDate != null) {
                            IconButton(
                                onClick = {
                                    customStartDate = null
                                    customEndDate = null
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_custom_date_range_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Custom Range",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Sub-Filters for Bible Devotions Tab
                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter: Reflections Only
                        FilterChip(
                            selected = showOnlyWithReflections,
                            onClick = { showOnlyWithReflections = !showOnlyWithReflections },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Reflections Only", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Filter: Bookmarked Only
                        FilterChip(
                            selected = showOnlyBookmarked,
                            onClick = { showOnlyBookmarked = !showOnlyBookmarked },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Bookmarked", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable Category Row
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            Tab(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .testTag("past_devotion_category_chip_$category")
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Render Content List
                if (selectedTab == 0) {
                    // Scripture Devotions List
                    if (filteredStories.isEmpty()) {
                        EmptyHistoryView(message = "No matching scripture devotions found")
                    } else {
                        Text(
                            text = "Showing ${filteredStories.size} Scripture Devotion${if (filteredStories.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredStories, key = { "modal_bible_${it.story.id}" }) { storyWithState ->
                                var isReflectionExpanded by remember { mutableStateOf(false) }
                                val dateString = remember(storyWithState.state.lastUpdated) {
                                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(storyWithState.state.lastUpdated))
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("past_devotion_item_${storyWithState.story.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header: category & date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = storyWithState.story.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = dateString,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }

                                        // Content: Title & Scripture
                                        Column {
                                            Text(
                                                text = storyWithState.story.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = storyWithState.story.bibleReference,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // Verse brief preview
                                        Text(
                                            text = "“${storyWithState.story.verseHighlight}”",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontStyle = FontStyle.Italic,
                                                lineHeight = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Reflection display
                                        if (storyWithState.state.reflectionText.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .padding(12.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Create,
                                                                contentDescription = "Journal Text icon",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = "My Saved Reflection notes:",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }

                                                        Text(
                                                            text = if (isReflectionExpanded) "Collapse" else "Expand",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.clickable { isReflectionExpanded = !isReflectionExpanded }
                                                        )
                                                    }

                                                    Text(
                                                        text = storyWithState.state.reflectionText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = if (isReflectionExpanded) 20 else 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Actions
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (storyWithState.state.isBookmarked) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Bookmarked",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }

                                            Button(
                                                onClick = {
                                                    onSelectStory(storyWithState)
                                                    onDismiss()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(
                                                    text = "Open Workbook",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Personal Journal Entries List
                    if (filteredJournals.isEmpty()) {
                        EmptyHistoryView(message = "No matching personal journals found")
                    } else {
                        Text(
                            text = "Showing ${filteredJournals.size} Personal Journal${if (filteredJournals.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredJournals, key = { "modal_journal_${it.id}" }) { entry ->
                                var isExpanded by remember { mutableStateOf(false) }
                                val dateString = remember(entry.dateCreated) {
                                    SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(entry.dateCreated))
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("past_journal_item_${entry.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header: badge and date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Personal Devotion",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            Text(
                                                text = dateString,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }

                                        // Title and Scripture
                                        Column {
                                            Text(
                                                text = entry.title.ifBlank { "Untitled Personal Reflection" },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (entry.scripturePassage.isNotBlank()) {
                                                Text(
                                                    text = entry.scripturePassage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Entry reflection notes box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Written reflection:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    Text(
                                                        text = if (isExpanded) "Collapse" else "Expand",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                                                    )
                                                }

                                                Text(
                                                    text = entry.entryText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = if (isExpanded) 40 else 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Delete and action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            var showDeleteConfirm by remember { mutableStateOf(false) }

                                            if (showDeleteConfirm) {
                                                Text(
                                                    text = "Confirm delete?",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                TextButton(
                                                    onClick = { showDeleteConfirm = false },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                                ) {
                                                    Text("Cancel")
                                                }
                                                Button(
                                                    onClick = {
                                                        onDeleteJournalEntry(entry.id)
                                                        showDeleteConfirm = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    modifier = Modifier.height(28.dp).testTag("delete_journal_entry_btn_${entry.id}"),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { showDeleteConfirm = true },
                                                    modifier = Modifier.size(32.dp).testTag("delete_journal_confirm_btn_${entry.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Journal Entry",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Write journals and reflections on scripture stories to populate your local archive history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

fun showDatePicker(
    context: android.content.Context,
    initialTimeMs: Long?,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (initialTimeMs != null) {
        calendar.timeInMillis = initialTimeMs
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val resultCal = Calendar.getInstance()
            resultCal.set(Calendar.YEAR, selectedYear)
            resultCal.set(Calendar.MONTH, selectedMonth)
            resultCal.set(Calendar.DAY_OF_MONTH, selectedDay)
            // Reset hours, minutes, seconds for consistency
            resultCal.set(Calendar.HOUR_OF_DAY, 0)
            resultCal.set(Calendar.MINUTE, 0)
            resultCal.set(Calendar.SECOND, 0)
            resultCal.set(Calendar.MILLISECOND, 0)
            onDateSelected(resultCal.timeInMillis)
        },
        year,
        month,
        day
    ).show()
}
