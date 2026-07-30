package com.example.cycletracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cycletracker.widget.CycleWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cycle_tracker_datastore")

data class AppState(
    val periodActiveSwitch: Boolean = false,
    val waterReminderSwitch: Boolean = true,
    val waterCount: Int = 0,
    val selectedPhase: String = "all",
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val weight: String = "",
    val height: String = "",
    val age: String = "",
    val notificationLeadTimeDays: Int = 2,
    val averageCycleLength: Int = 28,
    val geminiApiKey: String = "",
    val dailyReminderTime: String = "20:00",
    val widgetEnabled: Boolean = true,
    val waterIntervalHours: Int = 2
)

class SettingsRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val KEY_PERIOD_ACTIVE_SWITCH = booleanPreferencesKey("period_active_switch")
        val KEY_WATER_REMINDER_SWITCH = booleanPreferencesKey("water_reminder_switch")
        val KEY_WATER_COUNT = intPreferencesKey("water_count")
        val KEY_SELECTED_PHASE = stringPreferencesKey("selected_phase")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val KEY_WEIGHT = stringPreferencesKey("weight")
        val KEY_HEIGHT = stringPreferencesKey("height")
        val KEY_AGE = stringPreferencesKey("age")
        val KEY_NOTIFICATION_LEAD_TIME_DAYS = intPreferencesKey("lead_time")
        val KEY_AVERAGE_CYCLE_LENGTH = intPreferencesKey("avg_cycle_length")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        val KEY_WIDGET_ENABLED = booleanPreferencesKey("widget_enabled")
        val KEY_WATER_INTERVAL_HOURS = intPreferencesKey("water_interval_hours")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    val appStateFlow: Flow<AppState> = context.dataStore.data.map { prefs ->
        AppState(
            periodActiveSwitch = prefs[KEY_PERIOD_ACTIVE_SWITCH] ?: false,
            waterReminderSwitch = prefs[KEY_WATER_REMINDER_SWITCH] ?: true,
            waterCount = prefs[KEY_WATER_COUNT] ?: 0,
            selectedPhase = prefs[KEY_SELECTED_PHASE] ?: "all",
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            dailyReminderEnabled = prefs[KEY_DAILY_REMINDER_ENABLED] ?: true,
            weight = prefs[KEY_WEIGHT] ?: "",
            height = prefs[KEY_HEIGHT] ?: "",
            age = prefs[KEY_AGE] ?: "",
            notificationLeadTimeDays = prefs[KEY_NOTIFICATION_LEAD_TIME_DAYS] ?: 2,
            averageCycleLength = prefs[KEY_AVERAGE_CYCLE_LENGTH] ?: 28,
            geminiApiKey = prefs[KEY_GEMINI_API_KEY] ?: "",
            dailyReminderTime = prefs[KEY_DAILY_REMINDER_TIME] ?: "20:00",
            widgetEnabled = prefs[KEY_WIDGET_ENABLED] ?: true,
            waterIntervalHours = prefs[KEY_WATER_INTERVAL_HOURS] ?: 2
        )
    }

    val appState: StateFlow<AppState> = appStateFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AppState()
    )

    suspend fun getAppState(): AppState = appStateFlow.first()

    suspend fun setPeriodActiveSwitch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERIOD_ACTIVE_SWITCH] = enabled
        }
        notifyWidgetUpdate()
    }

    suspend fun setWaterReminderSwitch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WATER_REMINDER_SWITCH] = enabled
        }
        notifyWidgetUpdate()
    }

    suspend fun setWaterCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WATER_COUNT] = count.coerceAtLeast(0)
        }
        notifyWidgetUpdate()
    }

    suspend fun setSelectedPhase(phase: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PHASE] = phase
        }
        notifyWidgetUpdate()
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
        notifyWidgetUpdate()
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_REMINDER_ENABLED] = enabled
        }
        notifyWidgetUpdate()
    }

    suspend fun setWeight(weight: String) {
        context.dataStore.edit { prefs -> prefs[KEY_WEIGHT] = weight }
        notifyWidgetUpdate()
    }

    suspend fun setHeight(height: String) {
        context.dataStore.edit { prefs -> prefs[KEY_HEIGHT] = height }
        notifyWidgetUpdate()
    }

    suspend fun setAge(age: String) {
        context.dataStore.edit { prefs -> prefs[KEY_AGE] = age }
        notifyWidgetUpdate()
    }

    suspend fun setNotificationLeadTimeDays(days: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_NOTIFICATION_LEAD_TIME_DAYS] = days }
        notifyWidgetUpdate()
    }

    suspend fun setAverageCycleLength(length: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_AVERAGE_CYCLE_LENGTH] = length }
        notifyWidgetUpdate()
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_GEMINI_API_KEY] = key }
        notifyWidgetUpdate()
    }

    suspend fun setDailyReminderTime(time: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DAILY_REMINDER_TIME] = time }
        notifyWidgetUpdate()
    }

    suspend fun setWidgetEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_WIDGET_ENABLED] = enabled }
        notifyWidgetUpdate()
    }

    suspend fun setWaterIntervalHours(hours: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_WATER_INTERVAL_HOURS] = hours }
        notifyWidgetUpdate()
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    fun notifyWidgetUpdate() {
        scope.launch {
            try {
                CycleWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
