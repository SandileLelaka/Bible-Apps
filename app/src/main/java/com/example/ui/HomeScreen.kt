package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BibleStoryWithState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BibleStoriesViewModel,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.filteredStories.collectAsStateWithLifecycle()
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()
    val journalEntries by viewModel.allDevotionalJournalEntries.collectAsStateWithLifecycle()
    val dailyStory by viewModel.dailyDevotional.collectAsStateWithLifecycle()
    val activeStory by viewModel.activeStory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val onlyBookmarked by viewModel.showOnlyBookmarked.collectAsStateWithLifecycle()
    val onlyCompleted by viewModel.showOnlyCompleted.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val streakRecord by viewModel.streakRecord.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val mostRecentStory by viewModel.mostRecentDevotional.collectAsStateWithLifecycle()
    val todayReadingGoal by viewModel.todayReadingGoal.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var drawerTabSelected by remember { mutableStateOf(0) } // 0 = History, 1 = Favorite Passages
    var activeNavigationTab by remember { mutableStateOf(0) } // 0 = Devotionals, 1 = Spiritual Growth Dashboard
    var verseToShare by remember { mutableStateOf<BibleStoryWithState?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPastDevotionsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var globalTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isGlobalTtsInitialized by remember { mutableStateOf(false) }
    var currentlySpeakingStoryId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                isGlobalTtsInitialized = true
                globalTts?.language = java.util.Locale.getDefault()
            }
        }
        val tts = TextToSpeech(context, listener)
        globalTts = tts

        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    currentlySpeakingStoryId = utteranceId
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (currentlySpeakingStoryId == utteranceId) {
                        currentlySpeakingStoryId = null
                    }
                }
            }

            @Deprecated("Deprecated in Java", ReplaceWith("mainHandler.post { if (currentlySpeakingStoryId == utteranceId) currentlySpeakingStoryId = null }"))
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (currentlySpeakingStoryId == utteranceId) {
                        currentlySpeakingStoryId = null
                    }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    if (currentlySpeakingStoryId == utteranceId) {
                        currentlySpeakingStoryId = null
                    }
                }
            }
        })

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val toggleGlobalSpeech: (BibleStoryWithState) -> Unit = { item ->
        globalTts?.let { tts ->
            val story = item.story
            if (currentlySpeakingStoryId == story.id) {
                tts.stop()
                currentlySpeakingStoryId = null
            } else {
                val storyContent = "${story.title}. Bible Reference: ${story.bibleReference}. Highlight verse: ${story.verseHighlight}. Narrative summary: ${story.summary}. Key lessons: ${story.keyLessons.joinToString(". ")}. Suggested prayer: ${story.suggestedPrayer}"
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, story.id)
                }
                tts.speak(storyContent, TextToSpeech.QUEUE_FLUSH, params, story.id)
                currentlySpeakingStoryId = story.id
            }
        }
    }

    // Categorization options
    val categories = listOf("All", "Creation", "Covenants", "Faith", "Wisdom", "Miracles", "Parables", "Grace")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("history_drawer_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (drawerTabSelected == 0) Icons.Default.DateRange else Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (drawerTabSelected == 0) "Reading History" else "Favorite Passages",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (drawerTabSelected == 0) "Revisit past devotions" else "Saved favorite scriptures",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            modifier = Modifier.testTag("close_history_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close drawer"
                            )
                        }
                    }

                    // Tab Selector Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { drawerTabSelected = 0 },
                            modifier = Modifier.weight(1f).testTag("drawer_history_tab"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (drawerTabSelected == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (drawerTabSelected == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("History", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { drawerTabSelected = 1 },
                            modifier = Modifier.weight(1f).testTag("drawer_favorites_tab"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (drawerTabSelected == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (drawerTabSelected == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Passages", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // Drawer Contents - Tab 0: History
                    if (drawerTabSelected == 0) {
                        var drawerSubTab by remember { mutableStateOf(0) } // 0 = Scriptures, 1 = Journals
                        var drawerSearchQuery by remember { mutableStateOf("") }

                        // Sub Tab Selector
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { drawerSubTab = 0 },
                                modifier = Modifier.weight(1f).testTag("drawer_sub_scriptures_tab"),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (drawerSubTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (drawerSubTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Bible Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = { drawerSubTab = 1 },
                                modifier = Modifier.weight(1f).testTag("drawer_sub_journals_tab"),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (drawerSubTab == 1) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    contentColor = if (drawerSubTab == 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Journals", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Search Field
                        OutlinedTextField(
                            value = drawerSearchQuery,
                            onValueChange = { drawerSearchQuery = it },
                            placeholder = { Text("Search logs...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (drawerSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { drawerSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("drawer_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )

                        if (drawerSubTab == 0) {
                            val completedStories = allStories.filter { it.state.isCompleted || it.state.reflectionText.isNotBlank() }
                                .filter {
                                    it.story.title.contains(drawerSearchQuery, ignoreCase = true) ||
                                    it.story.bibleReference.contains(drawerSearchQuery, ignoreCase = true) ||
                                    it.state.reflectionText.contains(drawerSearchQuery, ignoreCase = true)
                                }
                                .sortedByDescending { it.state.lastUpdated }

                            if (completedStories.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                                        Text("No matching logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    items(completedStories, key = { "drawer_history_${it.story.id}" }) { storyWithState ->
                                        var isReflectionExpanded by remember { mutableStateOf(false) }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("drawer_history_item_${storyWithState.story.id}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = storyWithState.story.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = storyWithState.story.bibleReference,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                if (storyWithState.state.reflectionText.isNotBlank()) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("My Reflection:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                            Text(
                                                                text = if (isReflectionExpanded) "Less" else "More",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.clickable { isReflectionExpanded = !isReflectionExpanded }
                                                            )
                                                        }
                                                        Text(
                                                            text = storyWithState.state.reflectionText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontSize = 11.sp,
                                                            maxLines = if (isReflectionExpanded) 15 else 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val completeDate = remember(storyWithState.state.lastUpdated) {
                                                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                                            .format(java.util.Date(storyWithState.state.lastUpdated))
                                                    }
                                                    Text(completeDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                    
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.setStoryActive(storyWithState)
                                                            coroutineScope.launch { drawerState.close() }
                                                        },
                                                        modifier = Modifier.height(24.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Revisit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val filteredJournals = journalEntries.filter {
                                    it.title.contains(drawerSearchQuery, ignoreCase = true) ||
                                    it.scripturePassage.contains(drawerSearchQuery, ignoreCase = true) ||
                                    it.entryText.contains(drawerSearchQuery, ignoreCase = true)
                                }
                                .sortedByDescending { it.dateCreated }

                            if (filteredJournals.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                                        Text("No matching journals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    items(filteredJournals, key = { "drawer_journal_${it.id}" }) { entry ->
                                        var isJournalExpanded by remember { mutableStateOf(false) }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("drawer_journal_item_${entry.id}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = entry.title.ifBlank { "Personal Devotion" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (entry.scripturePassage.isNotBlank()) {
                                                    Text(
                                                        text = entry.scripturePassage,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                                        .padding(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Reflection notes:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                        Text(
                                                            text = if (isJournalExpanded) "Less" else "More",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.clickable { isJournalExpanded = !isJournalExpanded }
                                                        )
                                                    }
                                                    Text(
                                                        text = entry.entryText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        maxLines = if (isJournalExpanded) 20 else 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val journalDate = remember(entry.dateCreated) {
                                                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                                            .format(java.util.Date(entry.dateCreated))
                                                    }
                                                    Text(journalDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                    
                                                    IconButton(
                                                        onClick = { viewModel.deleteDevotionalJournalEntry(entry.id) },
                                                        modifier = Modifier.size(24.dp).testTag("drawer_delete_journal_btn_${entry.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Journal",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(14.dp)
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

                    // Drawer Contents - Tab 1: Favorites List of specific passages
                    if (drawerTabSelected == 1) {
                        val favoriteStories = allStories.filter { it.state.isPassageBookmarked }
                        if (favoriteStories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "No saved passages yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Open any scripture story, locate the key scripture passage, and tap the heart icon to save it here for fast access.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "${favoriteStories.size} Favorite Passage" + if (favoriteStories.size == 1) "" else "s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(favoriteStories, key = { "drawer_favorite_${it.story.id}" }) { storyWithState ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setStoryActive(storyWithState)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .testTag("favorite_passage_item_${storyWithState.story.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = storyWithState.story.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                IconButton(
                                                    onClick = {
                                                        viewModel.togglePassageBookmark(storyWithState.story.id)
                                                    },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .testTag("remove_passage_bookmark_${storyWithState.story.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "Remove from Favorites",
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "“${storyWithState.story.verseHighlight}”",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = storyWithState.story.bibleReference,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                IconButton(
                                                    onClick = { verseToShare = storyWithState },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .testTag("share_passage_drawer_${storyWithState.story.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Share Verse",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
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
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier.testTag("open_history_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Devotional History"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Bible Devotion",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Daily reflections & scripture stories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(
                            onClick = { showPastDevotionsDialog = true },
                            modifier = Modifier.testTag("open_past_devotions_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Open Past Devotions Archive"
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Open Settings & Cloud Sync"
                            )
                        }

                        // Quick stats display
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Saved state count",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stories.count { it.state.isBookmarked }.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(14.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed count",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stories.count { it.state.isCompleted }.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeNavigationTab == 0,
                        onClick = { activeNavigationTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Devotionals") },
                        label = { Text("Devotionals") },
                        modifier = Modifier.testTag("nav_devotionals_tab")
                    )
                    NavigationBarItem(
                        selected = activeNavigationTab == 1,
                        onClick = { activeNavigationTab = 1 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Growth Dashboard") },
                        label = { Text("Spiritual Growth") },
                        modifier = Modifier.testTag("nav_growth_tab")
                    )
                    NavigationBarItem(
                        selected = activeNavigationTab == 2,
                        onClick = { activeNavigationTab = 2 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Prayer Wall") },
                        label = { Text("Prayer Wall") },
                        modifier = Modifier.testTag("nav_prayer_wall_tab")
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isOfflineMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Offline Cache Mode Enabled",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Working Offline — Reading from local SQLite cache vault",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeNavigationTab) {
                        0 -> {
                    // Main content body
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    // Uplifting Verse of the Day Tracker Section
                    item {
                        VerseOfTheDayCard(
                            viewModel = viewModel
                        )
                    }

                    // Persistent Reading Progress Tracker & Today's Devotional Completion Checkbox
                    item {
                        ReadingProgressTrackerCard(
                            completedCount = allStories.count { it.state.isCompleted },
                            totalCount = allStories.size,
                            dailyStory = dailyStory,
                            streakRecord = streakRecord,
                            onToggleCompleted = { storyId -> viewModel.toggleCompleted(storyId) },
                            onDailyStoryClick = { story -> viewModel.setStoryActive(story) }
                        )
                    }

                    // Dynamic Gamified Daily Streak & Active Calendar Habit Tracker
                    item {
                        DailyStreakDashboard(
                            viewModel = viewModel
                        )
                    }

                    // Journaling Consistency & Quiet Time Habit Tracker
                    item {
                        DevotionalConsistencyCard(
                            viewModel = viewModel
                        )
                    }

                    // Visual Calendar Month Grid Tracker highlighting days with reflections
                    item {
                        MonthlyReflectionCalendar(
                            viewModel = viewModel
                        )
                    }

                    // Offline Cache & SQLite Storage Management Controller
                    item {
                        OfflineCacheManager(
                            mostRecentStory = mostRecentStory,
                            allStories = allStories,
                            isOfflineMode = isOfflineMode,
                            onToggleOfflineMode = { viewModel.isOfflineMode.value = it },
                            onSelectStory = { story -> viewModel.setStoryActive(story) }
                        )
                    }

                    // Gemini On-Demand AI Daily Devotional Section
                    item {
                        GeminiDailyStoryCard(
                            viewModel = viewModel
                        )
                    }

                    // Item 1: Daily Featured Devotional (if available and no searches or filters active)
                    if (searchQuery.isBlank() && selectedCategory == "All" && !onlyBookmarked && !onlyCompleted) {
                        dailyStory?.let { item ->
                            if (item.story.thematicFocus.isNotBlank()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "TODAY'S THEMATIC FOCUS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = item.story.thematicFocus,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                DailyFeatureCard(
                                    item = item,
                                    onClick = { viewModel.setStoryActive(item) },
                                    onShareVerse = { verseToShare = it },
                                    isSpeaking = currentlySpeakingStoryId == item.story.id,
                                    onToggleSpeech = { toggleGlobalSpeech(item) },
                                    isPassageBookmarked = item.state.isPassageBookmarked,
                                    onTogglePassageBookmark = { viewModel.togglePassageBookmark(item.story.id) },
                                    allStories = allStories
                                )
                            }
                            item {
                                DailyStoryReflectionFormCard(
                                    item = item,
                                    onSaveReflection = { notes -> viewModel.saveReflectionNotes(item.story.id, notes) },
                                    onToggleCompleted = { viewModel.toggleCompleted(item.story.id) }
                                )
                            }
                        }
                    }

                    // Item 2: Search and filtering block
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input"),
                                placeholder = {
                                    Text(
                                        "Search by keyword, bible verse, lesson...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.commitSearchToHistory(searchQuery)
                                        focusManager.clearFocus()
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Search History suggestions if input is blank but history exists
                            if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .clickable { viewModel.clearSearchHistory() }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    searchHistory.forEach { query ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { viewModel.updateSearchQuery(query) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = query,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontal Category select strip
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { category ->
                                    val isSelected = selectedCategory == category
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = { Text(category) },
                                        modifier = Modifier.testTag("category_chip_$category"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.background,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outline,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            // Supplementary Filter Badges: Bookmarks and Completed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = onlyBookmarked,
                                    onClick = { viewModel.toggleBookmarkFilter() },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (onlyBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Bookmarked")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                FilterChip(
                                    selected = onlyCompleted,
                                    onClick = { viewModel.toggleCompletedFilter() },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Reflected/Completed")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Item 3: Header for the list results
                    item {
                        val resultText = if (searchQuery.isNotBlank() || selectedCategory != "All" || onlyBookmarked || onlyCompleted) {
                            "Found ${stories.size} match" + if (stories.size != 1) "es" else ""
                        } else {
                            "Scripture Devotionals"
                        }
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Item 4: Results Feed
                    if (stories.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != "All" || onlyBookmarked || onlyCompleted,
                                onReset = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.selectCategory("All")
                                    if (onlyBookmarked) viewModel.toggleBookmarkFilter()
                                    if (onlyCompleted) viewModel.toggleCompletedFilter()
                                }
                            )
                        }
                    } else {
                        items(stories, key = { it.story.id }) { storyWithState ->
                            StoryFeedItem(
                                item = storyWithState,
                                onClick = { viewModel.setStoryActive(storyWithState) },
                                onToggleBookmark = { viewModel.toggleBookmark(storyWithState.story.id) },
                                onToggleCompleted = { viewModel.toggleCompleted(storyWithState.story.id) },
                                onShareVerse = { verseToShare = it },
                                isSpeaking = currentlySpeakingStoryId == storyWithState.story.id,
                                onToggleSpeech = { toggleGlobalSpeech(storyWithState) }
                            )
                        }
                    }
                }
                        }
                        1 -> {
                            SpiritualGrowthTimelineDashboard(
                                viewModel = viewModel,
                                onNavigateToDevotionals = { activeNavigationTab = 0 },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        2 -> {
                            PersonalPrayerWall(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
        
        SequentialAudioPlayer(
            allStories = allStories,
            onSelectStory = { story -> viewModel.setStoryActive(story) }
        )
    }
}
}

    // Modal dialog for active reading/reflecting workbook
    activeStory?.let { item ->
        val aiPromptState by viewModel.aiReflectionPromptState.collectAsStateWithLifecycle()
        DetailWorkbookDialog(
            viewModel = viewModel,
            item = item,
            aiPromptState = aiPromptState,
            onGenerateAiPrompt = { viewModel.loadOrGenerateAiPrompt(item.story.id) },
            onDismiss = { viewModel.setStoryActive(null) },
            onToggleBookmark = { viewModel.toggleBookmark(item.story.id) },
            onToggleCompleted = { viewModel.toggleCompleted(item.story.id) },
            onTogglePassageBookmark = { viewModel.togglePassageBookmark(item.story.id) },
            onSaveReflection = { text -> viewModel.saveReflectionNotes(item.story.id, text) },
            onShareVerse = { verseToShare = it }
        )
    }

    verseToShare?.let { item ->
        VerseShareDialog(
            item = item,
            onDismiss = { verseToShare = null }
        )
    }

    if (showSettingsDialog) {
        CloudSyncSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPastDevotionsDialog) {
        PastDevotionsDialog(
            allStories = allStories,
            journalEntries = journalEntries,
            onSelectStory = { story -> viewModel.setStoryActive(story) },
            onDeleteJournalEntry = { id -> viewModel.deleteDevotionalJournalEntry(id) },
            onDismiss = { showPastDevotionsDialog = false },
            streakRecord = streakRecord
        )
    }
}

@Composable
fun ReadingProgressTrackerCard(
    completedCount: Int,
    totalCount: Int,
    dailyStory: BibleStoryWithState?,
    streakRecord: com.example.data.StreakRecord,
    onToggleCompleted: (String) -> Unit,
    onDailyStoryClick: (BibleStoryWithState) -> Unit
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val percentage = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading_progress_tracker_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Your Reading Journey",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Growing in faith day by day (Best: ${streakRecord.highestStreak})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Streak Badge on the right side
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (streakRecord.currentStreak > 0) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                        .border(
                            1.dp,
                            if (streakRecord.currentStreak > 0) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (streakRecord.currentStreak > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Day Streak",
                        tint = if (streakRecord.currentStreak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${streakRecord.currentStreak} Day Streak",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (streakRecord.currentStreak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Progress bar and details
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedCount of $totalCount completed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }

            // Divider and today's challenge
            if (dailyStory != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox container with minimum touch size
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = dailyStory.state.isCompleted,
                            onCheckedChange = { onToggleCompleted(dailyStory.story.id) },
                            modifier = Modifier.testTag("daily_completion_checkbox"),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.tertiary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                    }

                    // Clickable Text area
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDailyStoryClick(dailyStory) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TODAY'S DEVOTIONAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    val currentFormattedDate = remember {
                                        java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.US).format(java.util.Date())
                                    }
                                    Text(
                                        text = currentFormattedDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                
                                val progressValue = when {
                                    dailyStory.state.isCompleted || dailyStory.state.reflectionText.isNotBlank() -> 1f
                                    dailyStory.state.lastUpdated > 0 -> 0.5f
                                    else -> 0f
                                }
                                val percentage = (progressValue * 100).toInt()
                                Text(
                                    text = "$percentage% Complete",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progressValue == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            Text(
                                text = dailyStory.story.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            val progressValue = when {
                                dailyStory.state.isCompleted || dailyStory.state.reflectionText.isNotBlank() -> 1f
                                dailyStory.state.lastUpdated > 0 -> 0.5f
                                else -> 0f
                            }
                            
                            LinearProgressIndicator(
                                progress = { progressValue },
                                color = if (progressValue == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .testTag("today_devotion_progress_bar")
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Read narrative",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun extractBookName(reference: String): String {
    val trimmed = reference.trim()
    val parts = trimmed.split(" ")
    if (parts.isEmpty()) return ""
    if (parts[0].all { it.isDigit() } && parts.size > 1) {
        return parts[0] + " " + parts[1]
    }
    return parts[0]
}

private fun getBookCompletionStats(bookName: String, allStories: List<BibleStoryWithState>): Pair<Int, Int> {
    if (bookName.isBlank() || allStories.isEmpty()) return Pair(0, 0)
    val storiesInBook = allStories.filter { extractBookName(it.story.bibleReference).equals(bookName, ignoreCase = true) }
    if (storiesInBook.isEmpty()) return Pair(0, 0)
    val completedCount = storiesInBook.count { it.state.isCompleted }
    return Pair(completedCount, storiesInBook.size)
}

@Composable
fun DailyFeatureCard(
    item: BibleStoryWithState,
    onClick: () -> Unit,
    onShareVerse: (BibleStoryWithState) -> Unit,
    isSpeaking: Boolean = false,
    onToggleSpeech: () -> Unit = {},
    isPassageBookmarked: Boolean = false,
    onTogglePassageBookmark: () -> Unit = {},
    allStories: List<BibleStoryWithState> = emptyList()
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )
    var fontScale by remember { mutableStateOf(1.0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradientBrush)
                .padding(24.dp),
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
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DAILY FEATURED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val readTime = item.story.getEstimatedReadingTimeMinutes()
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("estimated_reading_time_daily")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$readTime min read",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Font Size Toggle Button
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .clickable {
                                fontScale = when (fontScale) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    else -> 1.0f
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("font_size_toggle_daily")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "A⁺",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when (fontScale) {
                                    1.0f -> "100%"
                                    1.25f -> "125%"
                                    else -> "150%"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (item.state.isBookmarked) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    if (item.state.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Read today",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = item.story.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = MaterialTheme.typography.displaySmall.fontSize * fontScale
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = item.story.bibleReference,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * fontScale
                ),
                color = Color.White.copy(alpha = 0.85f),
                fontStyle = FontStyle.Italic
            )

            // Book Completion Progress Bar
            val bookName = remember(item.story.bibleReference) { extractBookName(item.story.bibleReference) }
            val (completedCount, totalCount) = remember(bookName, allStories) {
                getBookCompletionStats(bookName, allStories)
            }
            if (totalCount > 0) {
                val progress = completedCount.toFloat() / totalCount
                val progressAnim by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                    label = "book_progress"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("book_progress_container"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Book Progress: $bookName",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize * fontScale
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "$completedCount of $totalCount read (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = MaterialTheme.typography.labelMedium.fontSize * fontScale
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.testTag("book_progress_percentage_text")
                        )
                    }

                    LinearProgressIndicator(
                        progress = progressAnim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("book_progress_bar"),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "“${item.story.verseHighlight}”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontScale
                ),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic
            )

            // Prayer Prompt Generator section
            var prayerSeed by remember(item.story.id) { mutableStateOf(0) }
            val themeStr = item.story.thematicFocus.ifBlank { item.story.category }
            val prayerPrompt = remember(themeStr, prayerSeed) {
                PrayerPromptGenerator.generatePrompt(themeStr, prayerSeed)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Daily Prayer Focus",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { prayerSeed++ },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("btn_refresh_prayer_featured_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Prayer Prompt",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Text(
                    text = prayerPrompt,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * fontScale
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    fontStyle = FontStyle.Normal
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShareVerse(item) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .size(40.dp)
                            .testTag("share_passage_featured_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Featured Verse",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleSpeech,
                        modifier = Modifier
                            .background(
                                color = if (isSpeaking) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(40.dp)
                            .testTag("speak_passage_featured_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop Narration" else "Read Story Aloud",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePassageBookmark,
                        modifier = Modifier
                            .background(
                                color = if (isPassageBookmarked) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(40.dp)
                            .testTag("bookmark_passage_featured_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = if (isPassageBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isPassageBookmarked) "Remove Verse from Bookmarks" else "Bookmark Specific Verse",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reflect Now",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryFeedItem(
    item: BibleStoryWithState,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleCompleted: () -> Unit,
    onShareVerse: (BibleStoryWithState) -> Unit,
    isSpeaking: Boolean = false,
    onToggleSpeech: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("story_card_${item.story.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Category Tag
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.story.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Testament Tag
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.story.testament,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Reading time Tag
                    val readTime = item.story.getEstimatedReadingTimeMinutes()
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("estimated_reading_time_${item.story.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$readTime min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Interactive icons quick triggers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleSpeech,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("speak_button_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop Narration" else "Read Story Aloud",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("bookmark_button_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = if (item.state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (item.state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("completed_checkbox_${item.story.id}")
                    ) {
                        Icon(
                            imageVector = if (item.state.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                            contentDescription = "Mark reflective completion",
                            tint = if (item.state.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Story title & coordinates
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.story.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.story.bibleReference,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontStyle = FontStyle.Italic
                )
            }

            // Summary teaser
            Text(
                text = item.story.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Highlighter verse box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "“${item.story.verseHighlight}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onShareVerse(item) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("share_passage_feed_${item.story.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Verse",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Small Completion banner indicator
            if (item.state.reflectionText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Personal Reflection Written",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStatePanel(
    hasActiveFilters: Boolean,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "No stories found",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "No Devotionals Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (hasActiveFilters) {
                "We couldn't find any scripture stories that match your filters. Try clearing some selections or typing other bible keywords."
            } else {
                "There are no Bible stories loaded in this view."
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        if (hasActiveFilters) {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset Filters")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailWorkbookDialog(
    viewModel: BibleStoriesViewModel,
    item: BibleStoryWithState,
    aiPromptState: AiPromptState,
    onGenerateAiPrompt: () -> Unit,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleCompleted: () -> Unit,
    onTogglePassageBookmark: () -> Unit,
    onSaveReflection: (String) -> Unit,
    onShareVerse: (BibleStoryWithState) -> Unit
) {
    var notesInput by remember(item.story.id) { mutableStateOf(item.state.reflectionText) }
    var fontSizeFactor by remember { mutableStateOf(1.0f) }
    val prayerRequests by viewModel.allPrayerRequests.collectAsStateWithLifecycle()
    val reflectionPrayers = remember(prayerRequests) {
        prayerRequests.filter { !it.isAnswered && it.includeInDailyReflection }
    }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(item.story.id) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { t ->
                    t.language = java.util.Locale.getDefault()
                    isTtsInitialized = true
                }
            }
        }
        val ttsInstance = TextToSpeech(context, listener)
        tts = ttsInstance

        ttsInstance.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java", ReplaceWith("isSpeaking = false"))
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
            }
        })

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header tool bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close workbook",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Scripture Devotional",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (item.state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Bookmark",
                                tint = if (item.state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(onClick = onToggleCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Toggle completion",
                                tint = if (item.state.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Devotional Journey Progress Bar
                val devotionProgress = when {
                    item.state.isCompleted || item.state.reflectionText.isNotBlank() -> 1f
                    else -> 0.5f // Actively processing/reading
                }
                val devotionProgressText = when {
                    item.state.isCompleted || item.state.reflectionText.isNotBlank() -> "Devotion Complete! Journal Saved 🎉"
                    else -> "Currently Reading Scripture • Reflection Pending"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = devotionProgressText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (devotionProgress == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${(devotionProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (devotionProgress == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { devotionProgress },
                        color = if (devotionProgress == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("dialog_devotion_progress_bar")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)

                // Font Size & Speech Narration Control Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Font adjustments
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Text Size: ${(fontSizeFactor * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { if (fontSizeFactor > 0.8f) fontSizeFactor -= 0.1f },
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("font_size_decrease"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("A-", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { fontSizeFactor = 1.0f },
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("font_size_reset"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = { if (fontSizeFactor < 1.8f) fontSizeFactor += 0.1f },
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("font_size_increase"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("A+", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Right: Text-To-Speech Narration button
                    FilledTonalButton(
                        onClick = {
                            tts?.let { instance ->
                                if (isSpeaking) {
                                    instance.stop()
                                    isSpeaking = false
                                } else {
                                    val story = item.story
                                    val lessonsStr = story.keyLessons.joinToString(". ")
                                    val textToSpeak = "${story.title}. Bible Reference: ${story.bibleReference}. Key passage: ${story.verseHighlight}. Narrative: ${story.summary} Key lessons learned: $lessonsStr. Concluding Prayer: ${story.suggestedPrayer}"
                                    val params = android.os.Bundle().apply {
                                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "BibleStorySpeech")
                                    }
                                    instance.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "BibleStorySpeech")
                                    isSpeaking = true
                                }
                            }
                        },
                        enabled = isTtsInitialized,
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("narrate_story_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = if (isSpeaking) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop Narration" else "Narrate Bible Story",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSpeaking) "Stop" else "Narrate",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)

                // Scrollable workbook contents
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Category & Testament strip
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.story.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.story.testament,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Main titles
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = item.story.title,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = item.story.bibleReference,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Key devotional verse card focus
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Key Scripture Passage",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onShareVerse(item) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("share_passage_detail_${item.story.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share Verse",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = onTogglePassageBookmark,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("passage_bookmark_${item.story.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (item.state.isPassageBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Save Passage to Favorites",
                                                tint = if (item.state.isPassageBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "“${item.story.verseHighlight}”",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = (16 * fontSizeFactor).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = (24 * fontSizeFactor).sp
                                )
                            }
                        }
                    }

                    // Comprehensive Narrative summary
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "The Narrative Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = item.story.summary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = (16 * fontSizeFactor).sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                lineHeight = (26 * fontSizeFactor).sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }

                    // Devotional Key Lessons
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Key Devotional Lessons",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            item.story.keyLessons.forEach { lesson ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "✦",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = (16 * fontSizeFactor).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val parts = lesson.split(":", limit = 2)
                                    if (parts.size == 2) {
                                        Text(
                                            text = buildString {
                                                append(parts[0].trim())
                                                append(": ")
                                                append(parts[1].trim())
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = (16 * fontSizeFactor).sp,
                                            lineHeight = (24 * fontSizeFactor).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = lesson,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = (16 * fontSizeFactor).sp,
                                            lineHeight = (24 * fontSizeFactor).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Devotional Prayer
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Suggested Devotional Prayer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.story.suggestedPrayer,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = (16 * fontSizeFactor).sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                lineHeight = (24 * fontSizeFactor).sp
                            )
                        }
                    }

                    // AI-powered daily reflection prompt generator card
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "AI Prompt",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "AI reflection prompt generator",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Gemini 3.5",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            when (aiPromptState) {
                                is AiPromptState.Idle -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Let AI generate a scripture-based devotion focus and custom reflection prompt tailored for your devotion today.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                        Button(
                                            onClick = onGenerateAiPrompt,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary,
                                                contentColor = MaterialTheme.colorScheme.onTertiary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("generate_ai_prompt_button")
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Launch AI Reflection Generator",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                                is AiPromptState.Loading -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.tertiary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Igniting AI light...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                                is AiPromptState.Success -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "UNIQUE REFLECTION FOCUS",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                text = aiPromptState.prompt,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontSize = (16 * fontSizeFactor).sp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                lineHeight = (24 * fontSizeFactor).sp
                                            )
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "TODAY'S DEVOTIONAL PRAYER FOCUS",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = aiPromptState.prayerFocus,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = (15 * fontSizeFactor).sp,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                                lineHeight = (22 * fontSizeFactor).sp
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = onGenerateAiPrompt,
                                                modifier = Modifier.testTag("regenerate_ai_prompt_button")
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.tertiary
                                                    )
                                                    Text(
                                                        text = "Regenerate Reflection",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                is AiPromptState.Error -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = aiPromptState.message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = onGenerateAiPrompt,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Active Prayers for Daily Reflection Prompts
                    if (reflectionPrayers.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Your Active Prayers for Reflection",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Contemplate how today's scripture narrative connects to these personal petitions on your heart. You can also quickly mark them as answered here!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    reflectionPrayers.forEach { prayer ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "“${prayer.text}”",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.markPrayerAnswered(prayer.id, true, "Answered during devotion!")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                    contentColor = MaterialTheme.colorScheme.tertiary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Mark Answered",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Answered", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Personal interactive journaling reflection workbook
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Personal Notes & Devotional Reflections",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Write down what this story teaches you, your personal prayers, or action items for your daily walk.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                placeholder = {
                                    Text(
                                        "Take a moment to reflect and write your journal entry here...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                maxLines = 8
                            )

                            Button(
                                onClick = {
                                    onSaveReflection(notesInput)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("dialog_reflection_save_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Save Journal Reflection",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
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
