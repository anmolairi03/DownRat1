package com.example.cycletracker.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cycle_tracker_prefs", Context.MODE_PRIVATE)

    var weight: String
        get() = prefs.getString("weight", "") ?: ""
        set(value) = prefs.edit().putString("weight", value).apply()

    var height: String
        get() = prefs.getString("height", "") ?: ""
        set(value) = prefs.edit().putString("height", value).apply()

    var age: String
        get() = prefs.getString("age", "") ?: ""
        set(value) = prefs.edit().putString("age", value).apply()

    var notificationLeadTimeDays: Int
        get() = prefs.getInt("lead_time", 2)
        set(value) = prefs.edit().putInt("lead_time", value).apply()

    var averageCycleLength: Int
        get() = prefs.getInt("avg_cycle_length", 28)
        set(value) = prefs.edit().putInt("avg_cycle_length", value).apply()

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean("daily_reminder_enabled", true)
        set(value) = prefs.edit().putBoolean("daily_reminder_enabled", value).apply()

    var dailyReminderTime: String
        get() = prefs.getString("daily_reminder_time", "20:00") ?: "20:00"
        set(value) = prefs.edit().putString("daily_reminder_time", value).apply()

    var widgetEnabled: Boolean
        get() = prefs.getBoolean("widget_enabled", true)
        set(value) = prefs.edit().putBoolean("widget_enabled", value).apply()

    var waterIntervalHours: Int
        get() = prefs.getInt("water_interval_hours", 2)
        set(value) = prefs.edit().putInt("water_interval_hours", value).apply()

    var waterReminderEnabled: Boolean
        get() = prefs.getBoolean("water_reminder_enabled", true)
        set(value) = prefs.edit().putBoolean("water_reminder_enabled", value).apply()

    var widgetSelectedPhase: String
        get() = prefs.getString("widget_selected_phase", "all") ?: "all"
        set(value) { prefs.edit().putString("widget_selected_phase", value).commit() }

    fun getWaterCountForToday(): Int {
        val todayStr = java.time.LocalDate.now().toString()
        return prefs.getInt("water_$todayStr", 0)
    }

    fun getWaterCountFlow(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            val todayStr = java.time.LocalDate.now().toString()
            if (key == "water_$todayStr") {
                trySend(getWaterCountForToday())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getWaterCountForToday())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun setWaterCountForToday(count: Int) {
        val todayStr = java.time.LocalDate.now().toString()
        prefs.edit().putInt("water_$todayStr", count).commit()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
