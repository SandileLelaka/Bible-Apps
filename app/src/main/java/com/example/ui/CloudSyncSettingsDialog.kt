package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncSettingsDialog(
    viewModel: BibleStoriesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSyncEnabled by viewModel.isCloudSyncEnabled.collectAsStateWithLifecycle()
    val syncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    val reminderManager = remember { DevotionalReminderManager(context) }
    var reminderEnabled by remember { mutableStateOf(reminderManager.isReminderEnabled()) }
    var reminderTime by remember { mutableStateOf(reminderManager.getReminderTime()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderEnabled = true
            reminderManager.setReminderSettings(true, reminderTime)
            Toast.makeText(context, "Daily devotion reminder enabled! 🙏", Toast.LENGTH_SHORT).show()
        } else {
            reminderEnabled = false
            reminderManager.setReminderSettings(false, reminderTime)
            Toast.makeText(context, "Notification permission is required for reminders.", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("cloud_sync_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Cloud Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cloud Backup & Sync",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Preserve and restore your journals",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sync_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog"
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Toggle Feature Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Cloud Syncing",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Unifies journals and bookmarks instantly across local and cloud environments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSyncEnabled,
                        onCheckedChange = { viewModel.setCloudSyncEnabled(it) },
                        modifier = Modifier.testTag("auto_sync_switch")
                    )
                }

                // Sync Status Board
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (val state = syncState) {
                                is CloudSyncState.Idle -> {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Idle Status",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Sync is Idle",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Launch a sync manually to test cloud communication.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                is CloudSyncState.Syncing -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Synchronizing...",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Transmitting journal entries & targets...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                is CloudSyncState.Success -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success Status",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Successfully Synced",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = state.stats,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Last backup: ${formatSyncTime(state.timestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                is CloudSyncState.Error -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error Status",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Synchronization Suspended",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        // Sync button actions row
                        if (syncState !is CloudSyncState.Syncing) {
                            Button(
                                onClick = {
                                    viewModel.triggerCloudSync()
                                    Toast.makeText(context, "Cloud sync initiated!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trigger_sync_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync icon",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Sync Database Now", fontSize = 14.sp)
                            }
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                // Daily Devotional Reminder Notification Settings
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("devotional_reminder_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications Bell Icon",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Daily Reminder",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Remind me to do today's devotion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPermission) {
                                                reminderEnabled = true
                                                reminderManager.setReminderSettings(true, reminderTime)
                                                Toast.makeText(context, "Reminder notification scheduled at $reminderTime", Toast.LENGTH_SHORT).show()
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        } else {
                                            reminderEnabled = true
                                            reminderManager.setReminderSettings(true, reminderTime)
                                            Toast.makeText(context, "Reminder notification scheduled at $reminderTime", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        reminderEnabled = false
                                        reminderManager.cancelDailyAlarm()
                                        reminderManager.setReminderSettings(false, reminderTime)
                                        Toast.makeText(context, "Reminder notifications disabled", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("devotional_reminder_switch")
                            )
                        }

                        AnimatedVisibility(
                            visible = reminderEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Selected time display
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Reminder scheduled for:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    val parts = reminderTime.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    val isPm = hour >= 12
                                    val displayHour = when {
                                        hour == 0 -> 12
                                        hour > 12 -> hour - 12
                                        else -> hour
                                    }
                                    val displayMin = String.format("%02d", minute)
                                    val amPmStr = if (isPm) "PM" else "AM"

                                    Text(
                                        text = "$displayHour:$displayMin $amPmStr",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("reminder_time_display")
                                    )
                                }

                                // Quick presets chips
                                Text(
                                    text = "Quick Presets:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presets = listOf(
                                        "08:00" to "Morning",
                                        "12:00" to "Noon",
                                        "18:00" to "Evening",
                                        "21:00" to "Night"
                                    )
                                    presets.forEach { (timeStr, label) ->
                                        val isSelected = reminderTime == timeStr
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                reminderTime = timeStr
                                                reminderManager.setReminderSettings(true, timeStr)
                                                Toast.makeText(context, "Reminder updated to $timeStr", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text("$label ($timeStr)") },
                                            modifier = Modifier.testTag("reminder_preset_$timeStr")
                                        )
                                    }
                                }

                                // Custom Hour/Minute adjusters
                                Text(
                                    text = "Adjust Reminder Time:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val parts = reminderTime.split(":")
                                    val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                    val currentMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                    // Hour Adjuster
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Hr: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(
                                            onClick = {
                                                val newHour = (currentHour + 23) % 24
                                                val newTime = String.format("%02d:%02d", newHour, currentMin)
                                                reminderTime = newTime
                                                reminderManager.setReminderSettings(true, newTime)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("reminder_hour_dec")
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(
                                            text = String.format("%02d", currentHour),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                val newHour = (currentHour + 1) % 24
                                                val newTime = String.format("%02d:%02d", newHour, currentMin)
                                                reminderTime = newTime
                                                reminderManager.setReminderSettings(true, newTime)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("reminder_hour_inc")
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    // Minute Adjuster
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Min: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(
                                            onClick = {
                                                val newMin = (currentMin + 55) % 60
                                                val newTime = String.format("%02d:%02d", currentHour, newMin)
                                                reminderTime = newTime
                                                reminderManager.setReminderSettings(true, newTime)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("reminder_min_dec")
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(
                                            text = String.format("%02d", currentMin),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                val newMin = (currentMin + 5) % 60
                                                val newTime = String.format("%02d:%02d", currentHour, newMin)
                                                reminderTime = newTime
                                                reminderManager.setReminderSettings(true, newTime)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("reminder_min_inc")
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Immediate Test Button
                                Button(
                                    onClick = {
                                        // Trigger an immediate test notification
                                        reminderManager.sendReminderNotification()
                                        Toast.makeText(context, "Immediate reminder notification sent! Check your notification drawer.", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("send_test_reminder_notification_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Test Notification Icon",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Test Local Notification System", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Verification & Telemetry Tools Sandbox
                Text(
                    text = "Cloud Verification Sandbox",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Simulate clearing memory or changing physical devices to test exact restoration integrity:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.simulateLocalReset()
                            Toast.makeText(context, "Local DB Wiped. Simulate clear!", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_local_db_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Trash Icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Reset Local", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.restoreFromCloud()
                            Toast.makeText(context, "Cloud sync data restored!", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_cloud_db_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Cloud Download",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Pull Restore", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatSyncTime(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    return when {
         diff < 60000 -> "Just now"
         diff < 3600000 -> "${diff / 60000} mins ago"
         else -> java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}
