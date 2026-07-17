package com.example.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BibleStoryWithState
import java.util.Locale

@Composable
fun AudioWaveVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val heights = listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.3f, 0.6f, 0.4f)
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(32.dp)
    ) {
        heights.forEachIndexed { index, baseHeight ->
            val animatedHeightFraction by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = baseHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + (index * 70), easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "audio_bar_$index"
                )
            } else {
                remember { mutableStateOf(0.15f) }
            }
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(animatedHeightFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequentialAudioPlayer(
    allStories: List<BibleStoryWithState>,
    onSelectStory: (BibleStoryWithState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // History queue consists of completed stories. Fall back to all stories if empty so they can test easily!
    val completedHistory = remember(allStories) {
        allStories.filter { it.state.isCompleted }
    }
    
    // Use completed history if available, otherwise fallback to all stories so user has items to listen to sequentially
    val playQueue = if (completedHistory.isNotEmpty()) completedHistory else allStories
    val isUsingFallbackEmptyHistory = completedHistory.isEmpty()

    var isTtsInitialized by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    
    var currentQueueIndex by remember { mutableStateOf(-1) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var autoAdvanceEnabled by remember { mutableStateOf(true) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Init and release TextToSpeech safely
    DisposableEffect(Unit) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                ttsInstance?.language = Locale.getDefault()
            }
        }
        val tts = TextToSpeech(context, listener)
        ttsInstance = tts

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { isSpeaking = true }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    isSpeaking = false
                    // Auto-advance sequentially if configured
                    if (autoAdvanceEnabled && playQueue.isNotEmpty()) {
                        val nextIndex = currentQueueIndex + 1
                        if (nextIndex < playQueue.size) {
                            currentQueueIndex = nextIndex
                        } else {
                            // Cycle back to beginning
                            currentQueueIndex = 0
                            Toast.makeText(context, "Completed devotional stream! Starting over.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            @Deprecated("Deprecated in Java", ReplaceWith("mainHandler.post { isSpeaking = false }"))
            override fun onError(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    isSpeaking = false
                    Toast.makeText(context, "Audio playback error: code $errorCode", Toast.LENGTH_SHORT).show()
                }
            }
        })

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Trigger TTS narration on queue index change
    LaunchedEffect(currentQueueIndex) {
        if (currentQueueIndex >= 0 && currentQueueIndex < playQueue.size && isTtsInitialized) {
            val item = playQueue[currentQueueIndex]
            val story = item.story
            ttsInstance?.let { tts ->
                tts.stop()
                val lessonsJoined = story.keyLessons.joinToString(". ")
                val speechContent = "${story.title}. From the book or scripture references of: ${story.bibleReference}. " +
                        "A reading of the highlight verse: ${story.verseHighlight}. " +
                        "Narrative Devotional text: ${story.summary}. " +
                        "Key Spiritual Lessons: $lessonsJoined. " +
                        "Let us close in devotional prayer: ${story.suggestedPrayer}"
                
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "devotional_sequential_${story.id}")
                }
                tts.speak(speechContent, TextToSpeech.QUEUE_FLUSH, params, "devotional_sequential_${story.id}")
                isSpeaking = true
            }
        } else if (currentQueueIndex == -1) {
            ttsInstance?.stop()
            isSpeaking = false
        }
    }

    // If queue is empty, do not show anything to respect clean, robust boundaries
    if (playQueue.isEmpty()) {
        return
    }

    val activeItem = if (currentQueueIndex >= 0 && currentQueueIndex < playQueue.size) playQueue[currentQueueIndex] else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(top = 8.dp)
    ) {
        // Collapsed Player Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulse Visualizer Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSpeaking) {
                        AudioWaveVisualizer(isPlaying = true)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Ready placeholder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Title details
                Column {
                    Text(
                        text = if (activeItem != null) "Listening Info: ${activeItem.story.title}" else "Devotional Audio Journey",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (activeItem != null) {
                            "${activeItem.story.bibleReference} • ${activeItem.story.category}"
                        } else if (isUsingFallbackEmptyHistory) {
                            "Listen sequentially (${playQueue.size} Devotionals)"
                        } else {
                            "From your history queue (${playQueue.size} Completed)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Play Status Control & Expand Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Collapsed Play / Stop
                IconButton(
                    onClick = {
                        if (currentQueueIndex == -1 && playQueue.isNotEmpty()) {
                            currentQueueIndex = 0
                        } else {
                            if (isSpeaking) {
                                ttsInstance?.stop()
                                isSpeaking = false
                            } else {
                                // Resume or replay current
                                val savedIndex = currentQueueIndex
                                currentQueueIndex = -1
                                currentQueueIndex = savedIndex
                            }
                        }
                    },
                    modifier = Modifier.testTag("collapsed_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Trigger play sequential",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Expand and collapse arrow
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Expand controls",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Expanded player console pane
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // Beautiful narrative reading card
                activeItem?.let { current ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectStory(current) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(
                                        text = current.story.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Item ${currentQueueIndex + 1} of ${playQueue.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = current.story.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = current.story.bibleReference,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = current.story.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Primary Playback Engine controllers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    FilledTonalIconButton(
                        onClick = {
                            if (playQueue.isNotEmpty()) {
                                if (currentQueueIndex > 0) {
                                    currentQueueIndex--
                                } else {
                                    // Move to last item
                                    currentQueueIndex = playQueue.lastIndex
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(46.dp).testTag("audio_player_prev")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous story", modifier = Modifier.size(20.dp))
                    }

                    // Large Play / Stop Core Hub
                    FilledIconButton(
                        onClick = {
                            if (currentQueueIndex == -1 && playQueue.isNotEmpty()) {
                                currentQueueIndex = 0
                            } else {
                                if (isSpeaking) {
                                    ttsInstance?.stop()
                                    isSpeaking = false
                                } else {
                                    val saved = currentQueueIndex
                                    currentQueueIndex = -1
                                    currentQueueIndex = saved
                                }
                            }
                        },
                        modifier = Modifier.size(58.dp).testTag("audio_player_play_toggle"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Play core toggle",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Next Button
                    FilledTonalIconButton(
                        onClick = {
                            if (playQueue.isNotEmpty()) {
                                val nextIndex = currentQueueIndex + 1
                                if (nextIndex < playQueue.size) {
                                    currentQueueIndex = nextIndex
                                } else {
                                    currentQueueIndex = 0
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(46.dp).testTag("audio_player_next")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next story", modifier = Modifier.size(20.dp))
                    }
                }

                // Auto-advance & Queue info summary panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (autoAdvanceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Auto-Advance Sequentially",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoAdvanceEnabled,
                        onCheckedChange = { autoAdvanceEnabled = it },
                        modifier = Modifier.testTag("auto_advance_toggle")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // Scrollable Play Queue Explorer inside player
                Text(
                    text = if (isUsingFallbackEmptyHistory) "Sequential Listening Queue (All)" else "Your Completed Devotional Journey History Playlist",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isUsingFallbackEmptyHistory) {
                    Text(
                        text = "Note: You haven't completed any devotionals yet. Mark devotionals complete to automatically build your custom history walk journey. Showing all records.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(playQueue) { index, item ->
                        val isCurrent = index == currentQueueIndex
                        val isCompleted = item.state.isCompleted
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { currentQueueIndex = index }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d", index + 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Column {
                                    Text(
                                        text = item.story.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.story.bibleReference,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed Devotional",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isCurrent && isSpeaking) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Currently speaking indicator",
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
