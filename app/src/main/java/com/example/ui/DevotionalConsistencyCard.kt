package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BibleStoryWithState
import com.example.data.DevotionalJournalEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DevotionalConsistencyCard(
    viewModel: BibleStoriesViewModel,
    modifier: Modifier = Modifier
) {
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()
    val journalEntries by viewModel.allDevotionalJournalEntries.collectAsStateWithLifecycle()

    // Calculate current streak of devotional entries
    val currentStreak = remember(allStories, journalEntries) {
        calculateDevotionalStreak(allStories, journalEntries)
    }

    // Calculate personal best streak of devotional entries
    val bestStreak = remember(allStories, journalEntries) {
        calculateBestDevotionalStreak(allStories, journalEntries)
    }

    // Dynamically retrieve the last 7 calendar days in chronological order (6 days ago to today)
    val last7Days = remember {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US) // e.g., Mon, Tue
        val list = mutableListOf<StreakDayInfo>()
        val calendar = Calendar.getInstance()
        
        // Rewind to 6 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val dateStr = format.format(calendar.time)
            val dayLabel = dayFormat.format(calendar.time).first().toString() // single letter Mon -> M
            list.add(
                StreakDayInfo(
                    dateString = dateStr,
                    dayLabel = dayLabel,
                    isToday = i == 6
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Identify which dates of the last 7 days had completed devotional entries (reflections or journal entries)
    val completedDates = remember(allStories, journalEntries) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val set = mutableSetOf<String>()

        allStories.forEach { item ->
            if (item.state.isCompleted || item.state.reflectionText.isNotBlank()) {
                if (item.state.lastUpdated > 0) {
                    set.add(format.format(Date(item.state.lastUpdated)))
                }
            }
        }

        journalEntries.forEach { entry ->
            set.add(format.format(Date(entry.dateCreated)))
        }

        set
    }

    val orangeCoralColor = Color(0xFFFF5722)
    val sunnyYellowColor = Color(0xFFFFC107)
    val deepGreenSuccess = Color(0xFF4CAF50)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("devotional_consistency_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon and Best Streak trophy
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
                            .size(38.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        orangeCoralColor.copy(alpha = 0.25f),
                                        orangeCoralColor.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, orangeCoralColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Consistency Tracker",
                            tint = orangeCoralColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Journaling Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Completing daily reflections & notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Best Streak Trophy Badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trophy",
                            tint = sunnyYellowColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Best: $bestStreak Days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Streak number callout & Encouragement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val encouragement = when {
                        currentStreak == 0 -> "Write your first devotional or reflection entry today to start a streak!"
                        currentStreak in 1..2 -> "Awesome start! Every entry anchors your thoughts in His truth."
                        currentStreak in 3..5 -> "Fabulous devotion! Recording your walk of faith builds deep spiritual strength."
                        else -> "Outstanding commitment! You're nurturing a powerful, consistent holy habit."
                    }
                    Text(
                        text = if (currentStreak > 0) "$currentStreak Day Streak!" else "Start a Habit!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = encouragement,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Giant Coral Fire Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(orangeCoralColor, sunnyYellowColor)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "$currentStreak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = if (currentStreak == 1) "DAY" else "DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
            }

            // 7-Day Mini Calendar habit tracker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Weekly Reflection Calendar",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    last7Days.forEach { dayInfo ->
                        val isDayCompleted = completedDates.contains(dayInfo.dateString)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.width(38.dp)
                        ) {
                            Text(
                                text = dayInfo.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (dayInfo.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (dayInfo.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (isDayCompleted) orangeCoralColor.copy(alpha = 0.12f)
                                        else if (dayInfo.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        border = BorderStroke(
                                            width = if (dayInfo.isToday) 1.5.dp else 1.dp,
                                            color = if (isDayCompleted) orangeCoralColor.copy(alpha = 0.5f)
                                            else if (dayInfo.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDayCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed Devotional",
                                        tint = orangeCoralColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = if (dayInfo.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
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

// Utility Streak Calculation Functions
fun calculateDevotionalStreak(
    allStories: List<BibleStoryWithState>,
    journalEntries: List<DevotionalJournalEntry>
): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val completedDates = mutableSetOf<String>()
    
    // Add dates for completed stories or stories with reflections
    allStories.forEach { item ->
        if (item.state.isCompleted || item.state.reflectionText.isNotBlank()) {
            if (item.state.lastUpdated > 0) {
                completedDates.add(sdf.format(Date(item.state.lastUpdated)))
            }
        }
    }
    
    // Add dates for personal journal entries
    journalEntries.forEach { entry ->
        completedDates.add(sdf.format(Date(entry.dateCreated)))
    }
    
    if (completedDates.isEmpty()) return 0
    
    // Chronologically find the consecutive streak starting from today or yesterday back in time
    val todayStr = sdf.format(Date())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)
    
    var currentStreak = 0
    var checkDateStr = ""
    
    if (completedDates.contains(todayStr)) {
        currentStreak = 1
        checkDateStr = todayStr
    } else if (completedDates.contains(yesterdayStr)) {
        currentStreak = 1
        checkDateStr = yesterdayStr
    } else {
        return 0
    }
    
    // Now loop back in time
    val searchCal = Calendar.getInstance()
    if (checkDateStr == todayStr) {
        // start searching from yesterday backwards
        searchCal.add(Calendar.DAY_OF_YEAR, -1)
    } else {
        // start searching from 2 days ago backwards
        searchCal.add(Calendar.DAY_OF_YEAR, -2)
    }
    
    while (true) {
        val prevDateStr = sdf.format(searchCal.time)
        if (completedDates.contains(prevDateStr)) {
            currentStreak++
            searchCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    
    return currentStreak
}

fun calculateBestDevotionalStreak(
    allStories: List<BibleStoryWithState>,
    journalEntries: List<DevotionalJournalEntry>
): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val completedDates = mutableSetOf<String>()
    
    allStories.forEach { item ->
        if (item.state.isCompleted || item.state.reflectionText.isNotBlank()) {
            if (item.state.lastUpdated > 0) {
                completedDates.add(sdf.format(Date(item.state.lastUpdated)))
            }
        }
    }
    
    journalEntries.forEach { entry ->
        completedDates.add(sdf.format(Date(entry.dateCreated)))
    }
    
    if (completedDates.isEmpty()) return 0
    
    // Convert to parsed dates and sort them
    val sortedDates = completedDates.mapNotNull { 
        try { sdf.parse(it) } catch(e: Exception) { null }
    }.sorted()
    
    if (sortedDates.isEmpty()) return 0
    
    var maxStreak = 1
    var currentStreak = 1
    
    val cal = Calendar.getInstance()
    for (i in 1 until sortedDates.size) {
        cal.time = sortedDates[i - 1]
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val expectedNextDay = sdf.format(cal.time)
        val actualNextDay = sdf.format(sortedDates[i])
        
        if (expectedNextDay == actualNextDay) {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else if (sdf.format(sortedDates[i - 1]) != actualNextDay) {
            // Gap detected
            currentStreak = 1
        }
    }
    
    return maxOf(maxStreak, currentStreak)
}
