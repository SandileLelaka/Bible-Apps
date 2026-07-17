package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
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
import com.example.data.StreakRecord
import java.text.SimpleDateFormat
import java.util.*

data class StreakDayInfo(
    val dateString: String,
    val dayLabel: String,
    val isToday: Boolean
)

@Composable
fun DailyStreakDashboard(
    viewModel: BibleStoriesViewModel,
    modifier: Modifier = Modifier
) {
    val streakRecord by viewModel.streakRecord.collectAsStateWithLifecycle()
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()

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

    // Identify which dates of the last 7 days had completed devotions
    val completedDates = remember(allStories) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        allStories
            .filter { it.state.isCompleted }
            .map { format.format(Date(it.state.lastUpdated)) }
            .toSet()
    }

    val orangeFireColor = Color(0xFFF95B24)
    val goldBronzeColor = Color(0xFFFFB300)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_streak_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Block: Icon + Streak Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Fire Icon Badge with Radiant Gradient Background
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        orangeFireColor.copy(alpha = 0.25f),
                                        orangeFireColor.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(1.2.dp, orangeFireColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Streak Tracker",
                            tint = orangeFireColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Engagement Streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your spiritual habit tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Best Streak Record Trophy Badge
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Best Streak",
                        tint = goldBronzeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Best: ${streakRecord.highestStreak}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Streak Metric Callout & Encouragement Message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val message = when {
                        streakRecord.currentStreak == 0 -> "Start your journey today by completing a daily Bible story!"
                        streakRecord.currentStreak in 1..2 -> "Great start! Keep reading daily to build a powerful divine habit."
                        streakRecord.currentStreak in 3..6 -> "You're on fire! Keep feeding your spirit daily."
                        else -> "Incredible commitment! God is doing amazing things in your life."
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Giant interactive circle showing the current consecutive day count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(orangeFireColor, goldBronzeColor)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "${streakRecord.currentStreak}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = if (streakRecord.currentStreak == 1) "DAY" else "DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
            }

            // Weekly Calendar Checkmarks Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Weekly Habit Calendar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                            modifier = Modifier.width(42.dp)
                        ) {
                            // Letter label representing the day
                            Text(
                                text = dayInfo.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (dayInfo.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (dayInfo.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Visual checkmark or hollow tracker button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (isDayCompleted) orangeFireColor.copy(alpha = 0.15f)
                                        else if (dayInfo.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        border = BorderStroke(
                                            width = if (dayInfo.isToday) 1.5.dp else 1.dp,
                                            color = if (isDayCompleted) orangeFireColor.copy(alpha = 0.6f)
                                            else if (dayInfo.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDayCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = orangeFireColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (dayInfo.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
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
