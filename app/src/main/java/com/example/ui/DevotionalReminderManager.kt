package com.example.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import java.util.Calendar

class DevotionalReminderManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("devotional_reminder_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_ID = "daily_devotional_reminders"
        const val REMINDER_PREF_ENABLED = "daily_reminder_enabled"
        const val REMINDER_PREF_TIME = "daily_reminder_time" // "HH:mm"
        const val NOTIFICATION_ID = 4001
        const val ALARM_REQUEST_CODE = 5001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Devotional Reminders"
            val descriptionText = "Reminds you to complete your daily scripture devotion & reflection"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isReminderEnabled(): Boolean {
        return sharedPrefs.getBoolean(REMINDER_PREF_ENABLED, false)
    }

    fun getReminderTime(): String {
        return sharedPrefs.getString(REMINDER_PREF_TIME, "20:00") ?: "20:00"
    }

    fun setReminderSettings(enabled: Boolean, time: String) {
        sharedPrefs.edit()
            .putBoolean(REMINDER_PREF_ENABLED, enabled)
            .putString(REMINDER_PREF_TIME, time)
            .apply()

        if (enabled) {
            scheduleDailyAlarm(time)
        } else {
            cancelDailyAlarm()
        }
    }

    private fun scheduleDailyAlarm(timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DevotionalReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            // Use setInexactRepeating for battery efficiency and automatic daily repeating
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d("DevotionalReminder", "Successfully scheduled alarm repeating daily at $timeStr")
        } catch (e: Exception) {
            Log.e("DevotionalReminder", "Failed to schedule alarm", e)
        }
    }

    fun cancelDailyAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DevotionalReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("DevotionalReminder", "Canceled scheduled reminder alarm")
        }
    }

    fun sendReminderNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard system info icon
            .setContentTitle("Daily Devotion Reminder 🙏")
            .setContentText("You haven't completed your journal reflection today. Spend a few moments with the Word!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.d("DevotionalReminder", "Fired reminder notification to system channel")
        } catch (e: SecurityException) {
            Log.e("DevotionalReminder", "Missing POST_NOTIFICATIONS permission to fire notification", e)
        } catch (e: Exception) {
            Log.e("DevotionalReminder", "Failed to fire notification", e)
        }
    }
}
