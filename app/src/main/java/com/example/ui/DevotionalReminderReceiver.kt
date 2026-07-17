package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevotionalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DevotionalReminder", "Received broadcast trigger for reminder")

        val manager = DevotionalReminderManager(context)
        
        // 1. Double check if reminder is still enabled
        if (!manager.isReminderEnabled()) {
            Log.d("DevotionalReminder", "Reminder is disabled, skipping notification")
            return
        }

        // 2. Check if reflection was completed today
        val mainPrefs = context.getSharedPreferences("bible_devotional_prefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastCompletedDate = mainPrefs.getString("last_completed_reflection_date", "")

        Log.d("DevotionalReminder", "Checking completion: Today=$todayStr, LastCompleted=$lastCompletedDate")

        if (lastCompletedDate == todayStr) {
            Log.d("DevotionalReminder", "Reflection completed today! Skipping reminder.")
        } else {
            Log.d("DevotionalReminder", "Reflection not completed today. Firing notification.")
            manager.sendReminderNotification()
        }
    }
}
