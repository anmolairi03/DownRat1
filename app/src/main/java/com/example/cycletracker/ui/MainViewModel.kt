package com.example.cycletracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cycletracker.BuildConfig
import com.example.cycletracker.CycleTrackerApplication
import com.example.cycletracker.data.PeriodRecord
import com.example.cycletracker.data.SettingsManager
import com.google.ai.client.generativeai.GenerativeModel
import com.example.cycletracker.data.AppState
import com.example.cycletracker.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.glance.appwidget.updateAll

data class ChatMessage(val role: String, val text: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CycleTrackerApplication
    private val periodDao = app.database.periodDao()
    val settingsManager = SettingsManager(application)
    val repository = SettingsRepository.getInstance(application)

    val appState: StateFlow<AppState> = repository.appState

    val allRecords: StateFlow<List<PeriodRecord>> = periodDao.getAllRecords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            allRecords.collect { records ->
                val latest = records.sortedByDescending { it.startDate }.firstOrNull()
                val isOngoing = latest != null && latest.endDate == null
                if (repository.getAppState().periodActiveSwitch != isOngoing) {
                    repository.setPeriodActiveSwitch(isOngoing)
                }
            }
        }
    }

    fun togglePeriodActiveSwitch(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPeriodActiveSwitch(enabled)
            val todayStr = LocalDate.now().toString()
            val records = allRecords.value.sortedByDescending { it.startDate }
            val latestRecord = records.firstOrNull()

            if (enabled) {
                if (latestRecord == null || latestRecord.endDate != null) {
                    val newRecord = PeriodRecord(
                        startDate = todayStr,
                        endDate = null,
                        flowLevel = "medium",
                        painLevel = 2,
                        notes = ""
                    )
                    insertRecord(newRecord)
                }
            } else {
                if (latestRecord != null && latestRecord.endDate == null) {
                    val updated = latestRecord.copy(endDate = todayStr)
                    insertRecord(updated)
                }
            }
        }
    }

    fun toggleWaterReminderSwitch(enabled: Boolean) {
        viewModelScope.launch {
            repository.setWaterReminderSwitch(enabled)
            settingsManager.waterReminderEnabled = enabled
        }
    }

    fun setWaterCount(count: Int) {
        viewModelScope.launch {
            repository.setWaterCount(count)
            settingsManager.setWaterCountForToday(count)
        }
    }

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    fun updateWidget() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val glanceIdManager = androidx.glance.appwidget.GlanceAppWidgetManager(app)
                val glanceIds = glanceIdManager.getGlanceIds(com.example.cycletracker.widget.CycleWidget::class.java)
                val widget = com.example.cycletracker.widget.CycleWidget()
                glanceIds.forEach { id ->
                    widget.update(app, id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertRecord(record: PeriodRecord) {
        viewModelScope.launch {
            periodDao.insertRecord(record)
            updateNotifications()
            updateWidget()
        }
    }

    fun deleteRecord(record: PeriodRecord) {
        viewModelScope.launch {
            periodDao.deleteRecord(record)
            updateNotifications()
            updateWidget()
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            periodDao.deleteAllRecords()
            settingsManager.clearAll()
            updateNotifications()
            updateWidget()
        }
    }

    fun updateNotifications() {
        viewModelScope.launch {
            val records = allRecords.value.sortedByDescending { it.startDate }
            com.example.cycletracker.worker.NotificationScheduler.scheduleNextPeriodReminder(
                app,
                records.firstOrNull(),
                calculateCalculatedAvgCycleLength(),
                settingsManager.notificationLeadTimeDays
            )
        }
    }

    fun calculateCalculatedAvgCycleLength(): Int {
        val records = allRecords.value.sortedBy { it.startDate }
        if (records.size < 2) return settingsManager.averageCycleLength

        var totalDays = 0L
        var count = 0
        for (i in 1 until records.size) {
            val prev = LocalDate.parse(records[i - 1].startDate)
            val curr = LocalDate.parse(records[i].startDate)
            val diff = ChronoUnit.DAYS.between(prev, curr)
            if (diff in 15..60) {
                totalDays += diff
                count++
            }
        }
        if (count == 0) return settingsManager.averageCycleLength
        val avg = (totalDays / count).toInt()
        return if (avg in 20..45) avg else settingsManager.averageCycleLength
    }

    fun startChat(contextPrompt: String) {
        _chatMessages.value = emptyList()
        sendMessage(contextPrompt, isInitial = true)
    }

    private fun cleanAiText(raw: String): String {
        return raw
            .lines()
            .joinToString("\n") { line ->
                var cleaned = line.trim()
                if (cleaned.startsWith("* ") || cleaned.startsWith("- ")) {
                    cleaned = "• " + cleaned.substring(2).trim()
                }
                cleaned.replace("**", "").replace("*", "")
            }
            .trim()
    }

    fun sendMessage(text: String, isInitial: Boolean = false) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val currentMessages = _chatMessages.value.toMutableList()
            if (!isInitial) {
                currentMessages.add(ChatMessage("user", text))
                _chatMessages.value = currentMessages
            }
            
            _isLoadingAi.value = true
            
            try {
                val apiKey = settingsManager.geminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
                if (apiKey.isNotBlank()) {
                    val modelNames = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-flash-latest")
                    var lastError: Exception? = null
                    var responseText: String? = null

                    val systemPrompt = """
                        You are an empathetic, expert women's period health & wellness assistant named Your Personal Healthcare AI Assistant.
                        
                        User Personal Health Attributes:
                        - Weight: ${settingsManager.weight.ifBlank { "Not provided" }}
                        - Height: ${settingsManager.height.ifBlank { "Not provided" }}
                        - Age: ${settingsManager.age.ifBlank { "Not provided" }}
                        - Average Cycle Length: ${settingsManager.averageCycleLength} days
                        - Recorded History Entries: ${allRecords.value.size} cycle entries
                        
                        Chat History:
                        ${_chatMessages.value.joinToString("\n") { "${it.role}: ${it.text}" }}
                        
                        User Query: $text
                        
                        CRITICAL FORMATTING INSTRUCTIONS:
                        1. Do NOT use any asterisks (*) anywhere in your output. Do not use markdown bold like **text**.
                        2. Use clean plain text and unicode bullet points (•) for list items.
                        3. Provide personalized, concise health recommendations tailored to the user's profile in maximum 5 bullet points.
                        4. Maintain a warm, supportive, and professional tone.
                    """.trimIndent()

                    for (model in modelNames) {
                        try {
                            val generativeModel = GenerativeModel(
                                modelName = model,
                                apiKey = apiKey
                            )
                            val response = generativeModel.generateContent(systemPrompt)
                            if (!response.text.isNullOrBlank()) {
                                responseText = response.text
                                break
                            }
                        } catch (e: Exception) {
                            lastError = e
                        }
                    }

                    val updatedMessages = _chatMessages.value.toMutableList()
                    if (!responseText.isNullOrBlank()) {
                        updatedMessages.add(ChatMessage("model", cleanAiText(responseText)))
                    } else {
                        updatedMessages.add(ChatMessage("model", "Error connecting to AI service: ${lastError?.message ?: "Unable to fetch response"}"))
                    }
                    _chatMessages.value = updatedMessages
                } else {
                    val updatedMessages = _chatMessages.value.toMutableList()
                    updatedMessages.add(ChatMessage("model", "Please configure your Gemini API key in Settings to get personalized insights."))
                    _chatMessages.value = updatedMessages
                }
            } catch (e: Exception) {
                val updatedMessages = _chatMessages.value.toMutableList()
                updatedMessages.add(ChatMessage("model", "Error connecting to AI service: ${e.message}"))
                _chatMessages.value = updatedMessages
            } finally {
                _isLoadingAi.value = false
            }
        }
    }
}
