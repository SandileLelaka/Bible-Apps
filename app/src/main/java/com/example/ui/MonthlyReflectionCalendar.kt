package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BibleStoryWithState
import com.example.data.DevotionalJournalEntry
import java.text.SimpleDateFormat
import java.util.*

enum class ReflectionType {
    STORY, JOURNAL
}

data class ReflectionInfo(
    val title: String,
    val reference: String,
    val text: String,
    val type: ReflectionType,
    val timestamp: Long
)

data class CalendarDayInfo(
    val dayOfMonth: Int,
    val isPlaceholder: Boolean,
    val dateString: String,
    val isToday: Boolean = false
)

@Composable
fun MonthlyReflectionCalendar(
    viewModel: BibleStoriesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()
    val journalEntries by viewModel.allDevotionalJournalEntries.collectAsStateWithLifecycle()

    // Keep track of the year and month being viewed on the calendar
    var calendarYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-based

    // Reflections mapped by date ("yyyy-MM-dd")
    val reflectionsByDate = remember(allStories, journalEntries) {
        val map = mutableMapOf<String, MutableList<ReflectionInfo>>()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // 1. Process Bible Story Reflections
        allStories.forEach { item ->
            if (item.state.reflectionText.isNotBlank()) {
                val dateStr = format.format(Date(item.state.lastUpdated))
                val list = map.getOrPut(dateStr) { mutableListOf() }
                list.add(
                    ReflectionInfo(
                        title = item.story.title,
                        reference = item.story.bibleReference,
                        text = item.state.reflectionText,
                        type = ReflectionType.STORY,
                        timestamp = item.state.lastUpdated
                    )
                )
            }
        }

        // 2. Process Personal Devotional Journal Entries
        journalEntries.forEach { entry ->
            if (entry.entryText.isNotBlank()) {
                val dateStr = format.format(Date(entry.dateCreated))
                val list = map.getOrPut(dateStr) { mutableListOf() }
                list.add(
                    ReflectionInfo(
                        title = entry.title.ifBlank { "Personal Devotion" },
                        reference = entry.scripturePassage,
                        text = entry.entryText,
                        type = ReflectionType.JOURNAL,
                        timestamp = entry.dateCreated
                    )
                )
            }
        }

        map
    }

    // Days list for the selected month grid
    val calendarDays = remember(calendarYear, calendarMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Sunday is 1, so offset is (firstDayOfWeek - 1)
        val offset = firstDayOfWeek - 1

        val list = mutableListOf<CalendarDayInfo>()

        // Add empty placeholder days
        for (i in 0 until offset) {
            list.add(CalendarDayInfo(dayOfMonth = 0, isPlaceholder = true, dateString = ""))
        }

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance()
        val checkCal = Calendar.getInstance()

        for (day in 1..totalDays) {
            checkCal.set(calendarYear, calendarMonth, day)
            val dateStr = format.format(checkCal.time)
            val isToday = checkCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    checkCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                    checkCal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH)

            list.add(
                CalendarDayInfo(
                    dayOfMonth = day,
                    isPlaceholder = false,
                    dateString = dateStr,
                    isToday = isToday
                )
            )
        }
        list
    }

    // State for viewing reflection detail dialog
    var selectedDayReflections by remember { mutableStateOf<Pair<String, List<ReflectionInfo>>?>(null) }

    val monthName = remember(calendarMonth) {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, calendarMonth) }
        SimpleDateFormat("MMMM", Locale.US).format(cal.time)
    }

    // Calculate count of completed reflections in the currently selected month
    val reflectionsThisMonthCount = remember(calendarDays, reflectionsByDate) {
        calendarDays.filter { !it.isPlaceholder && reflectionsByDate.containsKey(it.dateString) }.size
    }

    val orangeCoralColor = Color(0xFFFF5722)
    val sunnyYellowColor = Color(0xFFFFC107)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_reflection_calendar_card"),
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
            // Header with icon and Title
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
                            contentDescription = "Reflection Calendar",
                            tint = orangeCoralColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Reflection Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track your daily journal reflections",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Month Selector Navigation Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (calendarMonth == 0) {
                                calendarMonth = 11
                                calendarYear -= 1
                            } else {
                                calendarMonth -= 1
                            }
                        },
                        modifier = Modifier.size(32.dp).testTag("calendar_prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$monthName $calendarYear",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (calendarMonth == 11) {
                                calendarMonth = 0
                                calendarYear += 1
                            } else {
                                calendarMonth += 1
                            }
                        },
                        modifier = Modifier.size(32.dp).testTag("calendar_next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Month Progress Highlight Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed days logo",
                        tint = orangeCoralColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (reflectionsThisMonthCount > 0) {
                            "Completed $reflectionsThisMonthCount reflection${if (reflectionsThisMonthCount == 1) "" else "s"} in $monthName!"
                        } else {
                            "No reflections recorded yet for $monthName"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (reflectionsThisMonthCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(orangeCoralColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Calendar Grid Row headers
            val daysOfWeekHeader = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeekHeader.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Calendar Days Grid Loop
            val weeks = calendarDays.chunked(7)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { dayInfo ->
                            if (dayInfo.isPlaceholder) {
                                Spacer(modifier = Modifier.size(36.dp))
                            } else {
                                val reflections = reflectionsByDate[dayInfo.dateString] ?: emptyList()
                                val isDayCompleted = reflections.isNotEmpty()

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isDayCompleted) {
                                                Brush.verticalGradient(
                                                    colors = listOf(orangeCoralColor, sunnyYellowColor.copy(alpha = 0.85f))
                                                )
                                            } else if (dayInfo.isToday) {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                    )
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.01f)
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            border = BorderStroke(
                                                width = if (dayInfo.isToday) 1.5.dp else 1.dp,
                                                color = if (isDayCompleted) orangeCoralColor.copy(alpha = 0.8f)
                                                else if (dayInfo.isToday) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            if (isDayCompleted) {
                                                selectedDayReflections = Pair(dayInfo.dateString, reflections)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "No reflections recorded on ${dayInfo.dateString}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .testTag("calendar_day_${dayInfo.dayOfMonth}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayInfo.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (dayInfo.isToday || isDayCompleted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDayCompleted) Color.White
                                            else if (dayInfo.isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        if (isDayCompleted) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Helpful Guide note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Help Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Tap highlighted days to read that day's reflections",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }

    // Day Reflections Details Dialog
    selectedDayReflections?.let { (dateStr, reflections) ->
        DayReflectionsDialog(
            dateString = dateStr,
            reflections = reflections,
            onDismiss = { selectedDayReflections = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayReflectionsDialog(
    dateString: String,
    reflections: List<ReflectionInfo>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("day_reflections_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Journal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Reflections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Reflections List
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        reflections.forEachIndexed { index, reflection ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val badgeColor = if (reflection.type == ReflectionType.STORY) {
                                        Color(0xFF4CAF50) // Green for stories
                                    } else {
                                        Color(0xFF00BCD4) // Teal for custom journal entries
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (reflection.type == ReflectionType.STORY) "STORY" else "JOURNAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            fontSize = 9.sp
                                        )
                                    }

                                    if (reflection.reference.isNotBlank()) {
                                        Text(
                                            text = reflection.reference,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = reflection.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "“${reflection.text}”",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_dismiss_btn")
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
