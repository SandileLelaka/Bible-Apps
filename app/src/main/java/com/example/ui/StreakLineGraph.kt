package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BibleStoryWithState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class StreakPoint(
    val index: Int,
    val dateLabel: String,   // formatted "MMM d"
    val dateFull: String,    // formatted "yyyy-MM-dd"
    val streakValue: Int,
    val isCompleted: Boolean
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StreakLineGraph(
    allStories: List<BibleStoryWithState>,
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    // 1. Reconstruct chronological daily streak values over the last 30 days
    val graphPoints = remember(allStories) {
        calculatePast30DaysStreak(allStories)
    }

    // 2. Interactive scrubbing state (track finger tap/drag index)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val activePoint = remember(graphPoints, selectedIndex) {
        selectedIndex?.let { idx -> graphPoints.getOrNull(idx) }
    }

    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_graph_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "30-Day Devotion Consistency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Daily streak progression and devotional history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // Interactive toggle key indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = primaryColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Completed Days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }

            // Interactive Info Tooltip Area
            AnimatedContent(
                targetState = activePoint,
                transitionSpec = {
                    fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                },
                label = "ScrubbingTooltip"
            ) { point ->
                if (point != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (point.isCompleted) tertiaryColor else outlineColor.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = point.dateLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = if (point.isCompleted) "🎉 Completed Today's Scripture!" else "No devotion logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${point.streakValue} Days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Text(
                                text = "Streak Metric",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Instruction Info Icon",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap or drag across the graph to inspect specific daily streak values.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // The Canvas Chart area
            val density = LocalDensity.current
            val paddingLeftPx = with(density) { 36.dp.toPx() }
            val paddingBottomPx = with(density) { 28.dp.toPx() }
            val paddingTopPx = with(density) { 16.dp.toPx() }
            val paddingRightPx = with(density) { 16.dp.toPx() }

            val maxStreakVal = remember(graphPoints) {
                graphPoints.maxOfOrNull { it.streakValue } ?: 0
            }
            val yMax = maxOf(5, maxStreakVal)

            var canvasWidth by remember { mutableStateOf(1f) }
            var canvasHeight by remember { mutableStateOf(1f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("streak_canvas_element")
                        .onSizeChanged { size ->
                            canvasWidth = size.width.toFloat()
                            canvasHeight = size.height.toFloat()
                        }
                        .pointerInput(graphPoints) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val chartWidth = canvasWidth - paddingLeftPx - paddingRightPx
                                    val xStep = if (chartWidth > 0) chartWidth / 29f else 1f
                                    val rawX = offset.x - paddingLeftPx
                                    val clampedX = rawX.coerceIn(0f, maxOf(1f, chartWidth))
                                    val index = if (xStep > 0) (clampedX / xStep).roundToInt().coerceIn(0, 29) else 0
                                    selectedIndex = index
                                }
                            )
                        }
                        .pointerInput(graphPoints) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val chartWidth = canvasWidth - paddingLeftPx - paddingRightPx
                                    val xStep = if (chartWidth > 0) chartWidth / 29f else 1f
                                    val rawX = offset.x - paddingLeftPx
                                    val clampedX = rawX.coerceIn(0f, maxOf(1f, chartWidth))
                                    val index = if (xStep > 0) (clampedX / xStep).roundToInt().coerceIn(0, 29) else 0
                                    selectedIndex = index
                                },
                                onDragEnd = {
                                    // Keep selectedIndex or let it stand. Let's let it stand so they can read details.
                                },
                                onDragCancel = {
                                    selectedIndex = null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val chartWidth = canvasWidth - paddingLeftPx - paddingRightPx
                                    val xStep = if (chartWidth > 0) chartWidth / 29f else 1f
                                    val rawX = change.position.x - paddingLeftPx
                                    val clampedX = rawX.coerceIn(0f, maxOf(1f, chartWidth))
                                    val index = if (xStep > 0) (clampedX / xStep).roundToInt().coerceIn(0, 29) else 0
                                    selectedIndex = index
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val chartWidth = width - paddingLeftPx - paddingRightPx
                    val chartHeight = height - paddingTopPx - paddingBottomPx
                    val xStep = chartWidth / 29f

                    // 1. Draw dashed gridlines and Y-axis text
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val fraction = i.toFloat() / gridSteps
                        val yVal = (fraction * yMax).roundToInt()
                        val yCoord = paddingTopPx + chartHeight - (fraction * chartHeight)
                        
                        // Dashed horizontal line
                        drawLine(
                            color = outlineColor.copy(alpha = 0.15f),
                            start = Offset(paddingLeftPx, yCoord),
                            end = Offset(width - paddingRightPx, yCoord),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Y Label Text
                        drawContext.canvas.nativeCanvas.drawText(
                            yVal.toString(),
                            paddingLeftPx - 12f,
                            yCoord + 4f,
                            android.graphics.Paint().apply {
                                color = onSurfaceColor.copy(alpha = 0.45f).toArgb()
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }

                    // 2. Generate path points
                    val linePoints = graphPoints.mapIndexed { index, p ->
                        val x = paddingLeftPx + index * xStep
                        val y = paddingTopPx + chartHeight - (p.streakValue.toFloat() / yMax) * chartHeight
                        Offset(x, y)
                    }

                    if (linePoints.isNotEmpty()) {
                        // 3. Draw gradient background fill under the trend line
                        val fillPath = Path().apply {
                            moveTo(linePoints.first().x, paddingTopPx + chartHeight)
                            linePoints.forEach { lineTo(it.x, it.y) }
                            lineTo(linePoints.last().x, paddingTopPx + chartHeight)
                            close()
                        }
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.00f)
                                ),
                                startY = paddingTopPx,
                                endY = paddingTopPx + chartHeight
                            )
                        )

                        // 4. Draw beautiful bezier-smoothed curve or thick polyline
                        // Let's draw smoothed bezier or a stylish polyline with smooth connections
                        val linePath = Path().apply {
                            moveTo(linePoints.first().x, linePoints.first().y)
                            for (i in 1 until linePoints.size) {
                                val p0 = linePoints[i - 1]
                                val p1 = linePoints[i]
                                val controlPointX1 = p0.x + (p1.x - p0.x) / 2f
                                cubicTo(
                                    controlPointX1, p0.y,
                                    controlPointX1, p1.y,
                                    p1.x, p1.y
                                )
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 5. Draw interactive / scrubbing vertical guide lines & dot
                        selectedIndex?.let { index ->
                            if (index in linePoints.indices) {
                                val point = linePoints[index]
                                val activeStreakPoint = graphPoints[index]
                                
                                // Vertical Scrub line
                                drawLine(
                                    color = secondaryColor.copy(alpha = 0.6f),
                                    start = Offset(point.x, paddingTopPx),
                                    end = Offset(point.x, paddingTopPx + chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )

                                // Outer target ring
                                drawCircle(
                                    color = secondaryColor.copy(alpha = 0.3f),
                                    radius = 12.dp.toPx(),
                                    center = point
                                )
                                // Highlight Dot
                                drawCircle(
                                    color = secondaryColor,
                                    radius = 6.dp.toPx(),
                                    center = point
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = point
                                )
                            }
                        }

                        // 6. Draw dots on completed days as subtle highlights
                        graphPoints.forEachIndexed { idx, pt ->
                            if (pt.isCompleted && idx != selectedIndex) {
                                val dotCenter = linePoints[idx]
                                // Primary glow ring
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.15f),
                                    radius = 7.dp.toPx(),
                                    center = dotCenter
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = dotCenter
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 1.8.dp.toPx(),
                                    center = dotCenter
                                )
                            }
                        }
                    }

                    // 7. Draw X-axis label markers beautifully (Start, 7 days, 14 days, 21 days, today)
                    val labelIndexes = listOf(0, 7, 14, 21, 29)
                    labelIndexes.forEach { index ->
                        if (index in graphPoints.indices) {
                            val pt = graphPoints[index]
                            val xCoord = paddingLeftPx + index * xStep
                            val labelY = paddingTopPx + chartHeight + 14.dp.toPx()
                            
                            // Tick mark line
                            drawLine(
                                color = outlineColor.copy(alpha = 0.2f),
                                start = Offset(xCoord, paddingTopPx + chartHeight),
                                end = Offset(xCoord, paddingTopPx + chartHeight + 4.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Label centered text
                            drawContext.canvas.nativeCanvas.drawText(
                                pt.dateLabel,
                                xCoord,
                                labelY,
                                android.graphics.Paint().apply {
                                    color = onSurfaceColor.copy(alpha = 0.45f).toArgb()
                                    textSize = 9.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                }
                            )
                        }
                    }
                }
            }

            // Summary Bottom Stat Row to make it deeply functional
            HorizontalDivider(color = outlineColor.copy(alpha = 0.1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        val averageStreak = remember(graphPoints) {
                            val average = graphPoints.sumOf { it.streakValue } / 30f
                            String.format(Locale.US, "%.1f", average)
                        }
                        Text(
                            text = "30d Consistency Average: $averageStreak Days",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily devotion streak average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val todayPointIdx = graphPoints.indices.last
                        selectedIndex = if (selectedIndex == todayPointIdx) null else todayPointIdx
                    }
                ) {
                    Text(
                        text = "Highlight Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Pure helper function to compute chronologically accurate daily devotional completion streaks
 * over the last 30 days. Uses a safety 60-day window fallback to calculate carryover streaks perfectly!
 */
fun calculatePast30DaysStreak(allStories: List<BibleStoryWithState>): List<StreakPoint> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFormat = SimpleDateFormat("MMM d", Locale.US)
    
    // Set of completed date strings
    val completedDatesSet = allStories
        .filter { it.state.isCompleted && it.state.lastUpdated > 0 }
        .map { sdf.format(Date(it.state.lastUpdated)) }
        .toSet()

    // Generate chronological timeline of the last 60 days to carry streaks forward correctly
    val todayCal = Calendar.getInstance()
    val last60Days = (0..59).map { offset ->
        val cal = Calendar.getInstance().apply {
            time = todayCal.time
            add(Calendar.DAY_OF_YEAR, -offset)
        }
        sdf.format(cal.time)
    }.reversed() // Chronological order: index 0 is 59 days ago, index 59 is today

    // Track state sequentially
    val dayToStreakMap = mutableMapOf<String, Int>()
    var runningStreak = 0

    last60Days.forEachIndexed { i, dayString ->
        val isCompletedToday = completedDatesSet.contains(dayString)
        if (isCompletedToday) {
            runningStreak += 1
        } else {
            // If it is today (the very last item), and they haven't completed a devotion yet,
            // we hold and carry forward yesterday's streak (gives they time to complete).
            // If it is a past day, the consecutive streak was physically broken, so resets to 0.
            if (i == last60Days.size - 1) {
                // Today: stays as yesterday's streak
            } else {
                runningStreak = 0
            }
        }
        dayToStreakMap[dayString] = runningStreak
    }

    // Now extract the past 30 days to return for the graph display (the last 30 entries of the 60 days)
    val past30DaysStrings = last60Days.takeLast(30)
    
    return past30DaysStrings.mapIndexed { index, dayString ->
        val dateFull = dayString
        val isCompleted = completedDatesSet.contains(dayString)
        val streakVal = dayToStreakMap[dayString] ?: 0
        
        // Convert yyyy-MM-dd to "MMM d" (e.g. "Jun 7")
        val dateLabel = try {
            val dateObj = sdf.parse(dayString)
            if (dateObj != null) labelFormat.format(dateObj) else dayString
        } catch (e: Exception) {
            dayString
        }

        StreakPoint(
            index = index,
            dateLabel = dateLabel,
            dateFull = dateFull,
            streakValue = streakVal,
            isCompleted = isCompleted
        )
    }
}
