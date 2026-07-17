package com.example.ui

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.data.PrayerRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonalPrayerWall(
    viewModel: BibleStoriesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prayerRequests by viewModel.allPrayerRequests.collectAsStateWithLifecycle()

    var newPrayerText by remember { mutableStateOf("") }
    var includeInReflection by remember { mutableStateOf(true) }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Answered, 2 = All
    var selectedCategory by remember { mutableStateOf("General") }
    var isManuallySelected by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Inline sheet or dialog state for recording an answered prayer details
    var prayerRequestToAnswer by remember { mutableStateOf<PrayerRequest?>(null) }
    var answerNotesInput by remember { mutableStateOf("") }

    // Simulation state for testing the prayer streak
    var simulatedStreakOverride by remember { mutableStateOf<Int?>(null) }

    // Dynamic prayer streak calculation
    val (actualPrayerStreak, actualHighestPrayerStreak) = remember(prayerRequests) {
        if (prayerRequests.isEmpty()) {
            Pair(0, 0)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val prayerDates = prayerRequests.map { sdf.format(Date(it.dateAdded)) }.toSet()
            
            val todayStr = sdf.format(Date())
            val yesterdayStr = sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
            
            var currentStreak = 0
            val checkDate = java.util.Calendar.getInstance()
            
            val isStreakActive = prayerDates.contains(todayStr) || prayerDates.contains(yesterdayStr)
            if (isStreakActive) {
                if (!prayerDates.contains(todayStr)) {
                    checkDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
                }
                
                while (prayerDates.contains(sdf.format(checkDate.time))) {
                    currentStreak++
                    checkDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
                }
            }
            
            // Calculate highest streak
            val sortedDates = prayerDates.sorted()
            var highestStreak = 0
            if (sortedDates.isNotEmpty()) {
                var tempStreak = 1
                highestStreak = 1
                for (i in 1 until sortedDates.size) {
                    val prevDate = sdf.parse(sortedDates[i - 1])
                    val currDate = sdf.parse(sortedDates[i])
                    if (prevDate != null && currDate != null) {
                        val diffMs = currDate.time - prevDate.time
                        val diffDays = diffMs / (24 * 60 * 60 * 1000)
                        if (diffDays == 1L) {
                            tempStreak++
                            highestStreak = maxOf(highestStreak, tempStreak)
                        } else if (diffDays > 1L) {
                            tempStreak = 1
                        }
                    }
                }
                highestStreak = maxOf(highestStreak, currentStreak)
            }
            Pair(currentStreak, highestStreak)
        }
    }

    val currentPrayerStreak = simulatedStreakOverride ?: actualPrayerStreak
    val highestPrayerStreak = if (simulatedStreakOverride != null) {
        maxOf(simulatedStreakOverride!!, actualHighestPrayerStreak)
    } else {
        actualHighestPrayerStreak
    }

    val filteredPrayers = remember(prayerRequests, selectedFilterTab, selectedCategoryFilter) {
        val baseList = when (selectedFilterTab) {
            0 -> prayerRequests.filter { !it.isAnswered }
            1 -> prayerRequests.filter { it.isAnswered }
            else -> prayerRequests
        }
        if (selectedCategoryFilter == "All") {
            baseList
        } else {
            baseList.filter { it.category == selectedCategoryFilter }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Prayer Wall Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Personal Prayer Wall",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Post requests, track answers, and reflect on God's faithfulness daily.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("prayer_wall_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Badge & Streak Counter for consistent prayer
            item {
                PrayerStreakDashboard(
                    currentStreak = currentPrayerStreak,
                    highestStreak = highestPrayerStreak,
                    simulatedStreak = simulatedStreakOverride,
                    onSimulateChange = { simulatedStreakOverride = it }
                )
            }

            // Post a New Prayer Request Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_prayer_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    imageVector = Icons.Default.Create,
                                    contentDescription = "Add Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Post a New Prayer Request",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newPrayerText,
                            onValueChange = { text ->
                                newPrayerText = text
                                if (text.isEmpty()) {
                                    isManuallySelected = false
                                    selectedCategory = "General"
                                } else if (!isManuallySelected) {
                                    selectedCategory = AutoCategoryDetector.detect(text)
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "What is on your heart? (e.g., Praying for healing, guidance with career decision, or peace in transitions...)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                                .testTag("prayer_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Category selection row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Select Category",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                AnimatedVisibility(
                                    visible = newPrayerText.isNotEmpty(),
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Text(
                                        text = if (isManuallySelected) "(Selected)" else "✨ (Auto-Detected)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isManuallySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("General", "Healing", "Guidance", "Family", "Provision", "Peace", "Thanksgiving").forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    val (icon, color) = when (cat) {
                                        "Healing" -> Pair(Icons.Default.Favorite, Color(0xFFE57373))
                                        "Guidance" -> Pair(Icons.Default.Info, Color(0xFF64B5F6))
                                        "Family" -> Pair(Icons.Default.Person, Color(0xFF4DB6AC))
                                        "Provision" -> Pair(Icons.Default.ShoppingCart, Color(0xFF81C784))
                                        "Peace" -> Pair(Icons.Default.CheckCircle, Color(0xFF9575CD))
                                        "Thanksgiving" -> Pair(Icons.Default.ThumbUp, Color(0xFFFFB74D))
                                        else -> Pair(Icons.Default.List, Color(0xFF90A4AE))
                                    }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCategory = cat
                                            isManuallySelected = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) color else color.copy(alpha = 0.6f)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = color.copy(alpha = 0.15f),
                                            selectedLabelColor = color
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = color.copy(alpha = 0.4f),
                                            selectedBorderColor = color
                                        ),
                                        modifier = Modifier.testTag("post_category_chip_$cat")
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = includeInReflection,
                                    onCheckedChange = { includeInReflection = it },
                                    modifier = Modifier.testTag("reflection_toggle_checkbox")
                                )
                                Text(
                                    text = "Include in daily reflection prompts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    if (newPrayerText.trim().isBlank()) {
                                        Toast.makeText(context, "Please enter your prayer request text", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.postPrayerRequest(newPrayerText.trim(), includeInReflection, selectedCategory)
                                    newPrayerText = ""
                                    selectedCategory = "General"
                                    isManuallySelected = false
                                    Toast.makeText(context, "Prayer posted to the wall!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("post_prayer_submit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Post", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Filter Chips Selector
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFilterTab == 0,
                            onClick = { selectedFilterTab = 0 },
                            label = { Text("Active Requests (${prayerRequests.count { !it.isAnswered }})") },
                            modifier = Modifier.testTag("filter_chip_active")
                        )
                        FilterChip(
                            selected = selectedFilterTab == 1,
                            onClick = { selectedFilterTab = 1 },
                            label = { Text("Answered (${prayerRequests.count { it.isAnswered }})") },
                            modifier = Modifier.testTag("filter_chip_answered")
                        )
                        FilterChip(
                            selected = selectedFilterTab == 2,
                            onClick = { selectedFilterTab = 2 },
                            label = { Text("All (${prayerRequests.size})") },
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == "All",
                            onClick = { selectedCategoryFilter = "All" },
                            label = { Text("All Categories") },
                            modifier = Modifier.testTag("filter_category_chip_all")
                        )

                        listOf("General", "Healing", "Guidance", "Family", "Provision", "Peace", "Thanksgiving").forEach { cat ->
                            val count = prayerRequests.count { req ->
                                val statusMatch = when (selectedFilterTab) {
                                    0 -> !req.isAnswered
                                    1 -> req.isAnswered
                                    else -> true
                                }
                                statusMatch && req.category == cat
                            }
                            val isSelected = selectedCategoryFilter == cat

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text("$cat ($count)") },
                                modifier = Modifier.testTag("filter_category_chip_$cat")
                            )
                        }
                    }
                }
            }

            // Empty State
            if (filteredPrayers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                            .testTag("prayer_wall_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedFilterTab == 1) Icons.Outlined.CheckCircle else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Empty Wall icon",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when (selectedFilterTab) {
                                0 -> "No active prayer requests. Tap above to post one of your needs."
                                1 -> "No praise reports recorded yet. Mark a request as answered below to count your blessings."
                                else -> "Your prayer wall is empty. Start writing your heart to God."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Loop of prayers
            items(filteredPrayers, key = { it.id }) { prayer ->
                PrayerItemRow(
                    prayer = prayer,
                    onToggleReflection = { include ->
                        viewModel.togglePrayerIncludeInDailyReflection(prayer.id, include)
                    },
                    onTriggerAnswer = {
                        prayerRequestToAnswer = prayer
                        answerNotesInput = if (prayer.isAnswered) prayer.answerText else ""
                    },
                    onMarkUnanswered = {
                        viewModel.markPrayerAnswered(prayer.id, false, "")
                    },
                    onDelete = {
                        viewModel.deletePrayerRequest(prayer.id)
                        Toast.makeText(context, "Prayer request deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Answer dialog
    prayerRequestToAnswer?.let { prayer ->
        AlertDialog(
            onDismissRequest = { prayerRequestToAnswer = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Praise Report Icon",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = {
                Text(
                    text = "Record Praise Report!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Original Request: “${prayer.text}”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        text = "Write down any details on how God answered this prayer. This will be stored as a testimony on your wall.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = answerNotesInput,
                        onValueChange = { answerNotesInput = it },
                        placeholder = {
                            Text(
                                "e.g., God provided a marvelous breakthrough! Surgery was fully successful...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .testTag("praise_report_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markPrayerAnswered(prayer.id, true, answerNotesInput.trim())
                        prayerRequestToAnswer = null
                        answerNotesInput = ""
                        Toast.makeText(context, "Recorded answered prayer! 🎉", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_praise_report_button")
                ) {
                    Text("Save Praise", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { prayerRequestToAnswer = null }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun PrayerItemRow(
    prayer: PrayerRequest,
    onToggleReflection: (Boolean) -> Unit,
    onTriggerAnswer: () -> Unit,
    onMarkUnanswered: () -> Unit,
    onDelete: () -> Unit
) {
    val dateAddedString = remember(prayer.dateAdded) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(prayer.dateAdded))
    }

    val dateAnsweredString = remember(prayer.answeredDate) {
        if (prayer.answeredDate > 0L) {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "Answered: ${sdf.format(Date(prayer.answeredDate))}"
        } else ""
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (prayer.isAnswered) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (prayer.isAnswered) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_item_card_${prayer.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header line: Date Added, isAnswered status, Category, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (prayer.isAnswered) Icons.Default.CheckCircle else Icons.Default.FavoriteBorder,
                        contentDescription = "Status icon",
                        tint = if (prayer.isAnswered) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Posted $dateAddedString",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    CategoryBadge(category = prayer.category)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_prayer_button_${prayer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Prayer Request",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Body Prayer Text
            Text(
                text = "“${prayer.text}”",
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (prayer.isAnswered) TextDecoration.LineThrough else TextDecoration.None,
                fontStyle = FontStyle.Italic,
                color = if (prayer.isAnswered) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Answered Details Block (if answered)
            if (prayer.isAnswered) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ PRAISE REPORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (dateAnsweredString.isNotBlank()) {
                            Text(
                                text = dateAnsweredString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (prayer.answerText.isNotBlank()) {
                        Text(
                            text = prayer.answerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Marked as answered!",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // Action Bar: Answer toggle, reflection switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Option to toggle prompt inclusion
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Switch(
                        checked = prayer.includeInDailyReflection,
                        onCheckedChange = onToggleReflection,
                        modifier = Modifier
                            .scale(0.7f)
                            .testTag("reflection_switch_${prayer.id}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Text(
                        text = "Reflection prompt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (prayer.isAnswered) {
                    TextButton(
                        onClick = onMarkUnanswered,
                        modifier = Modifier.testTag("mark_unanswered_button_${prayer.id}"),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Button(
                        onClick = onTriggerAnswer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("mark_answered_button_${prayer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Mark as Answered Icon",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Answered", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper functions for dynamic prayer habit badges
private fun getPrayerBadgeName(streak: Int): String {
    return when {
        streak == 0 -> "Beginner"
        streak in 1..2 -> "Faithful Starter"
        streak in 3..6 -> "Devoted Supplicant"
        streak in 7..14 -> "Steady Intercessor"
        streak in 15..29 -> "Prayer Warrior"
        else -> "Prayer Pillar 🌟"
    }
}

private fun getPrayerBadgeDescription(streak: Int): String {
    return when {
        streak == 0 -> "Post your first prayer request to start your consistency streak!"
        streak in 1..2 -> "You've started a beautiful habit of daily prayer. Keep it up!"
        streak in 3..6 -> "Fervent and consistent. Your daily prayers build a spiritual shelter."
        streak in 7..14 -> "A week or more of consecutive prayer. Your faith shines through steady intercession."
        streak in 15..29 -> "A mighty prayer habit! Your persistence shows deep reliance on God's grace."
        else -> "An exceptional milestone! You stand strong as a pillar of faithful prayer in the sanctuary."
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PrayerStreakDashboard(
    currentStreak: Int,
    highestStreak: Int,
    simulatedStreak: Int?,
    onSimulateChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeName = getPrayerBadgeName(currentStreak)
    val badgeDesc = getPrayerBadgeDescription(currentStreak)
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_streak_dashboard_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Prayer Streak Icon",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Prayer Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Nurture your daily conversation with God",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Active badge tag
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Stats row (Circular dial + description text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Counter Circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$currentStreak",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.testTag("prayer_streak_count_text")
                        )
                        Text(
                            text = if (currentStreak == 1) "day" else "days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Description text & personal best
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = badgeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Personal Best Icon",
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Personal Best: $highestStreak ${if (highestStreak == 1) "day" else "days"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Test Simulation Bar
            HorizontalDivider(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Reviewer Test Controls (Simulate Habit Growth):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        null to "Real",
                        3 to "3d",
                        7 to "7d",
                        15 to "15d",
                        30 to "30d"
                    ).forEach { (streakVal, label) ->
                        val isSelected = simulatedStreak == streakVal
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSimulateChange(streakVal) },
                            label = { 
                                Text(
                                    text = label, 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("simulate_prayer_chip_$label")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String, modifier: Modifier = Modifier) {
    val (icon, color) = remember(category) {
        when (category) {
            "Healing" -> Pair(Icons.Default.Favorite, Color(0xFFE57373))
            "Guidance" -> Pair(Icons.Default.Info, Color(0xFF64B5F6))
            "Family" -> Pair(Icons.Default.Person, Color(0xFF4DB6AC))
            "Provision" -> Pair(Icons.Default.ShoppingCart, Color(0xFF81C784))
            "Peace" -> Pair(Icons.Default.CheckCircle, Color(0xFF9575CD))
            "Thanksgiving" -> Pair(Icons.Default.ThumbUp, Color(0xFFFFB74D))
            else -> Pair(Icons.Default.List, Color(0xFF90A4AE))
        }
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

object AutoCategoryDetector {
    val categories = listOf(
        "Healing" to listOf("heal", "healing", "sick", "surgery", "illness", "disease", "pain", "doctor", "hospital", "cancer", "health", "recovery", "operation", "cure", "hurt", "ill"),
        "Guidance" to listOf("guide", "guidance", "decision", "choice", "career", "job", "future", "wisdom", "direction", "path", "college", "school", "lead", "steps", "interview", "hired"),
        "Family" to listOf("family", "parent", "mother", "father", "son", "daughter", "child", "children", "spouse", "husband", "wife", "marriage", "brother", "sister", "grandma", "grandpa", "cousin", "kid", "mom", "dad"),
        "Provision" to listOf("finance", "money", "provision", "job", "work", "debt", "rent", "bills", "provide", "need", "financial", "employment", "income", "mortgage", "salary"),
        "Peace" to listOf("peace", "anxiety", "anxious", "worry", "stress", "calm", "fear", "depressed", "mental", "sleepless", "nightmare", "comfort", "rest", "worrying", "afraid"),
        "Thanksgiving" to listOf("thank", "thanks", "praise", "grateful", "blessing", "glorious", "gratitude", "answered", "testimony", "celebrate", "thankful", "favor")
    )

    fun detect(text: String): String {
        val lower = text.lowercase()
        var bestCategory = "General"
        var maxMatches = 0
        
        for ((cat, keywords) in categories) {
            val matches = keywords.count { lower.contains(it) }
            if (matches > maxMatches) {
                maxMatches = matches
                bestCategory = cat
            }
        }
        return bestCategory
    }
}
