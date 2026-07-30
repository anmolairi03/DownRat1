package com.example.cycletracker.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cycletracker.data.PeriodRecord
import com.example.cycletracker.data.SettingsRepository
import com.example.cycletracker.widget.CycleWidgetReceiver
import com.example.cycletracker.worker.NotificationScheduler

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WaterDrop
import androidx.glance.appwidget.updateAll
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository = SettingsRepository.getInstance(LocalContext.current),
    latestRecord: PeriodRecord? = null,
    records: List<PeriodRecord> = emptyList(),
    onResetAllData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appState by repository.appState.collectAsState()
    val scope = rememberCoroutineScope()

    var weight by remember { mutableStateOf(appState.weight) }
    var height by remember { mutableStateOf(appState.height) }
    var age by remember { mutableStateOf(appState.age) }
    var cycleLength by remember { mutableStateOf(appState.averageCycleLength.toString()) }
    var leadTime by remember { mutableStateOf(appState.notificationLeadTimeDays.toString()) }
    var apiKey by remember { mutableStateOf(appState.geminiApiKey) }

    var notificationsEnabled by remember { mutableStateOf(appState.notificationsEnabled) }
    var dailyReminderEnabled by remember { mutableStateOf(appState.dailyReminderEnabled) }
    var dailyReminderTime by remember { mutableStateOf(appState.dailyReminderTime) }
    var waterReminderEnabled by remember { mutableStateOf(appState.waterReminderSwitch) }
    var waterIntervalHours by remember { mutableStateOf(appState.waterIntervalHours.toString()) }
    var widgetEnabled by remember { mutableStateOf(appState.widgetEnabled) }

    var showApiKey by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showGuideModal by remember { mutableStateOf(false) }
    var showWidgetModal by remember { mutableStateOf(false) }

    fun updateWidgetComponentState(enabled: Boolean) {
        try {
            val componentName = ComponentName(context, CycleWidgetReceiver::class.java)
            val state = if (enabled) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                componentName,
                state,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerWidgetPin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val myProvider = ComponentName(context, CycleWidgetReceiver::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val pinnedWidgetCallbackIntent = Intent(context, CycleWidgetReceiver::class.java)
                    val successCallback = PendingIntent.getBroadcast(
                        context, 0, pinnedWidgetCallbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                } else {
                    Toast.makeText(context, "Widget pinning is not supported on this launcher. Add it manually from widgets menu.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveAllSettings() {
        val parsedCycleLen = cycleLength.toIntOrNull() ?: 28
        val parsedLeadTime = leadTime.toIntOrNull() ?: 2
        val parsedWaterInterval = waterIntervalHours.toIntOrNull() ?: 2

        scope.launch {
            repository.setWeight(weight)
            repository.setHeight(height)
            repository.setAge(age)
            repository.setAverageCycleLength(parsedCycleLen)
            repository.setNotificationLeadTimeDays(parsedLeadTime)
            repository.setGeminiApiKey(apiKey.trim())
            repository.setNotificationsEnabled(notificationsEnabled)
            repository.setWaterIntervalHours(parsedWaterInterval)
            repository.setWaterReminderSwitch(waterReminderEnabled)
            repository.setDailyReminderEnabled(dailyReminderEnabled)
            repository.setDailyReminderTime(dailyReminderTime)
            repository.setWidgetEnabled(widgetEnabled)
        }

        updateWidgetComponentState(widgetEnabled)

        if (notificationsEnabled) {
            NotificationScheduler.scheduleNextPeriodReminder(context, latestRecord, parsedCycleLen, parsedLeadTime)
        } else {
            NotificationScheduler.cancelPeriodReminder(context)
        }

        if (dailyReminderEnabled) {
            NotificationScheduler.scheduleDailyStatusReminder(context, dailyReminderTime)
        } else {
            NotificationScheduler.cancelDailyStatusReminder(context)
        }

        if (waterReminderEnabled) {
            NotificationScheduler.scheduleWaterIntervalReminder(context, parsedWaterInterval)
        } else {
            NotificationScheduler.cancelWaterIntervalReminder(context)
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.example.cycletracker.widget.CycleWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Toast.makeText(context, "Profile Settings Saved!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color(0xFF2C1E21)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2C1E21))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFF8F9))
            )
        },
        containerColor = Color(0xFFFFF8F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal Information & AI Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "PERSONAL INFORMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        letterSpacing = 0.5.sp
                    )

                    Column {
                        Text("Weight (e.g. 60 kg)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                        )
                    }

                    Column {
                        Text("Height (e.g. 165 cm)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                        )
                    }

                    Column {
                        Text("Age", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CYCLE PREFERENCES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        letterSpacing = 0.5.sp
                    )

                    Column {
                        Text("Average Cycle Length (Days)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = cycleLength,
                            onValueChange = { cycleLength = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                        )
                    }

                    Column {
                        Text("Notify days before period starts", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = leadTime,
                            onValueChange = { leadTime = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI FEATURES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        letterSpacing = 0.5.sp
                    )

                    Column {
                        Text("Gemini API Key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = { Text("AI Studio API Key", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Hide Key" else "Show Key",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                        )

                        TextButton(
                            onClick = { showGuideModal = true },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE53935))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("How to get your own Gemini API Key?", fontSize = 12.sp, color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { saveAllSettings() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Save Profile Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // APP PREFERENCES & TOGGLES Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "APP PREFERENCES & TOGGLES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        letterSpacing = 0.5.sp
                    )

                    // Cycle Notifications Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cycle Notifications", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C1E21))
                                    Text("Receive period reminders before predicted start date", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        notificationsEnabled = enabled
                                        scope.launch { repository.setNotificationsEnabled(enabled) }
                                        if (enabled) {
                                            val lead = leadTime.toIntOrNull() ?: 2
                                            NotificationScheduler.scheduleNextPeriodReminder(context, latestRecord, appState.averageCycleLength, lead)
                                        } else {
                                            NotificationScheduler.cancelPeriodReminder(context)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE53935))
                                )
                            }

                            if (notificationsEnabled) {
                                HorizontalDivider(color = Color(0xFFFFE4E6))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Days before start date:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = leadTime,
                                            onValueChange = {
                                                leadTime = it
                                                val parsed = it.toIntOrNull() ?: 2
                                                scope.launch { repository.setNotificationLeadTimeDays(parsed) }
                                                if (notificationsEnabled) {
                                                    NotificationScheduler.scheduleNextPeriodReminder(context, latestRecord, appState.averageCycleLength, parsed)
                                                }
                                            },
                                            modifier = Modifier.width(80.dp),
                                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("days", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // Daily Status Reminder Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily Status Reminder", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C1E21))
                                    Text("Reminds you if water intake (daily) or period status (if ongoing) is unlogged", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = dailyReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        dailyReminderEnabled = enabled
                                        scope.launch { repository.setDailyReminderEnabled(enabled) }
                                        if (enabled) {
                                            NotificationScheduler.scheduleDailyStatusReminder(context, dailyReminderTime)
                                        } else {
                                            NotificationScheduler.cancelDailyStatusReminder(context)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE53935))
                                )
                            }

                            if (dailyReminderEnabled) {
                                HorizontalDivider(color = Color(0xFFFFE4E6))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reminder Time:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21))
                                    OutlinedTextField(
                                        value = dailyReminderTime,
                                        onValueChange = {
                                            dailyReminderTime = it
                                            scope.launch { repository.setDailyReminderTime(it) }
                                            if (dailyReminderEnabled) {
                                                NotificationScheduler.scheduleDailyStatusReminder(context, it)
                                            }
                                        },
                                        modifier = Modifier.width(110.dp),
                                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        shape = RoundedCornerShape(8.dp),
                                         singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                                    )
                                }
                            }
                        }
                    }

                    // Water Interval Reminder Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Periodic Water Intake Reminder", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C1E21))
                                    Text("Sends periodic notifications to remind you to stay hydrated", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = waterReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        waterReminderEnabled = enabled
                                        scope.launch { repository.setWaterReminderSwitch(enabled) }
                                        val repo = com.example.cycletracker.data.SettingsRepository.getInstance(context)
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            repo.setWaterReminderSwitch(enabled)
                                        }
                                        if (enabled) {
                                            val interval = waterIntervalHours.toIntOrNull() ?: 2
                                            NotificationScheduler.scheduleWaterIntervalReminder(context, interval)
                                        } else {
                                            NotificationScheduler.cancelWaterIntervalReminder(context)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE53935))
                                )
                            }

                            if (waterReminderEnabled) {
                                HorizontalDivider(color = Color(0xFFFFE4E6))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Remind every (hours):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = waterIntervalHours,
                                            onValueChange = {
                                                waterIntervalHours = it
                                                val parsed = it.toIntOrNull() ?: 2
                                                scope.launch { repository.setWaterIntervalHours(parsed) }
                                                if (waterReminderEnabled) {
                                                    NotificationScheduler.scheduleWaterIntervalReminder(context, parsed)
                                                }
                                            },
                                            modifier = Modifier.width(80.dp),
                                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE53935))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("hours", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // Home Screen Widget Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Home Screen Widget", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C1E21))
                                    Text("Show mobile launcher widget preview & quick controls", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = widgetEnabled,
                                    onCheckedChange = { enabled ->
                                        widgetEnabled = enabled
                                        scope.launch { repository.setWidgetEnabled(enabled) }
                                        updateWidgetComponentState(enabled)
                                        if (enabled) {
                                            triggerWidgetPin()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE53935))
                                )
                            }

                            if (widgetEnabled) {
                                Button(
                                    onClick = { showWidgetModal = true },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF1F2), contentColor = Color(0xFFE53935))
                                ) {
                                    Text("Preview Home Screen Widget", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Reset All Data Danger Zone Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Data", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                    }
                    Text(
                        "This will permanently delete all recorded cycle entries, period history, personal health attributes, and preferences.",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626).copy(alpha = 0.8f)
                    )
                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        Dialog(onDismissRequest = { showConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text("Are you sure?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Are you sure you want to delete all data, including your height, weight, and cycle history? This action cannot be undone.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showConfirmDialog = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                onResetAllData()
                                showConfirmDialog = false
                                onBack()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }

    if (showGuideModal) {
        Dialog(onDismissRequest = { showGuideModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Your Free Gemini API Key", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }
                        IconButton(onClick = { showGuideModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE53935).copy(alpha = 0.1f))
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Follow these simple steps to generate your key:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)), border = BorderStroke(1.dp, Color(0xFFFFE4E6))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row {
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFE53935), CircleShape), contentAlignment = Alignment.Center) {
                                        Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Visit Google AI Studio by clicking the link below:", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.padding(start = 28.dp).height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text("Open Google AI Studio", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)), border = BorderStroke(1.dp, Color(0xFFE5E7EB))) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Box(modifier = Modifier.size(20.dp).background(Color(0xFF374151), CircleShape), contentAlignment = Alignment.Center) {
                                    Text("2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with your standard Google Account.", fontSize = 12.sp)
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)), border = BorderStroke(1.dp, Color(0xFFE5E7EB))) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Box(modifier = Modifier.size(20.dp).background(Color(0xFF374151), CircleShape), contentAlignment = Alignment.Center) {
                                    Text("3", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Click the \"Create API key\" button in AI Studio.", fontSize = 12.sp)
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)), border = BorderStroke(1.dp, Color(0xFFE5E7EB))) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Box(modifier = Modifier.size(20.dp).background(Color(0xFF374151), CircleShape), contentAlignment = Alignment.Center) {
                                    Text("4", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy the generated API Key string and paste it into the Gemini API Key input field in this Settings screen.", fontSize = 12.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE53935).copy(alpha = 0.1f))
                    Button(
                        onClick = { showGuideModal = false },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Got it!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showWidgetModal) {
        Dialog(onDismissRequest = { showWidgetModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Home Screen Widget Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2C1E21))
                        IconButton(onClick = { showWidgetModal = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }

                    var previewWaterCount by remember { mutableIntStateOf(appState.waterCount) }
                    val scope = rememberCoroutineScope()

                    fun updatePreviewWater(newCount: Int) {
                        val count = newCount.coerceAtLeast(0)
                        previewWaterCount = count
                        scope.launch { repository.setWaterCount(count) }
                    }

                    val today = remember { LocalDate.now() }
                    val todayStr = today.toString()
                    val monthYearLabel = remember { today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())) }

                    var isOngoing = false
                    var periodDay = 0
                    var daysUntilNext = 0
                    var nextPeriodStart: LocalDate? = null
                    var nextPeriodEnd: LocalDate? = null

                    if (latestRecord != null) {
                        if (latestRecord.endDate == null) {
                            isOngoing = true
                        }
                        try {
                            val start = LocalDate.parse(latestRecord.startDate.split("T")[0])
                            val avgLen = appState.averageCycleLength.toLong().coerceAtLeast(20L)

                            if (isOngoing) {
                                periodDay = ChronoUnit.DAYS.between(start, today).toInt() + 1
                            }

                            var cyclesElapsed = 0L
                            if (!start.isAfter(today)) {
                                val diffDays = ChronoUnit.DAYS.between(start, today)
                                cyclesElapsed = diffDays / avgLen
                            }
                            val currentCycleStart = start.plusDays(cyclesElapsed * avgLen)
                            val nStart = currentCycleStart.plusDays(avgLen)
                            nextPeriodStart = nStart
                            nextPeriodEnd = nStart.plusDays(4)

                            val diffNext = ChronoUnit.DAYS.between(today, nStart)
                            daysUntilNext = diffNext.toInt()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val monday = remember { today.with(DayOfWeek.MONDAY) }
                    val weekDays = remember { (0..6).map { monday.plusDays(it.toLong()) } }

                    fun isDayInPeriod(d: LocalDate): Boolean {
                        val dStr = d.toString()
                        return records.any { r ->
                            val rStart = r.startDate.split("T")[0]
                            val rEnd = r.endDate?.split("T")?.get(0) ?: todayStr
                            dStr >= rStart && dStr <= rEnd
                        }
                    }

                    fun isDayInPredictedNext(d: LocalDate): Boolean {
                        if (nextPeriodStart == null || nextPeriodEnd == null) return false
                        val dStr = d.toString()
                        val pStartStr = nextPeriodStart.toString()
                        val pEndStr = nextPeriodEnd.toString()
                        return dStr >= pStartStr && dStr <= pEndStr
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFE11D48), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Cycle & Water Widget", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2C1E21))
                                    Text(monthYearLabel, fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            // Cycle Status Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                                border = BorderStroke(1.dp, Color(0xFFFFE4E6))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            if (isOngoing) "PERIOD ONGOING - DAY $periodDay" else "NEXT PERIOD",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE11D48)
                                        )
                                        Text(
                                            if (isOngoing) "Active Now" else if (daysUntilNext > 0) "$daysUntilNext days" else if (daysUntilNext == 0) "Expected Today" else "${-daysUntilNext} days late",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2C1E21)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE11D48)
                                    ) {
                                        Text(
                                            if (isOngoing) "End Cycle" else "Start Cycle",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Week Calendar Bar
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("This Week ($monthYearLabel)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                        }
                                        Text("Mon - Sun", fontSize = 9.sp, color = Color.Gray)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        weekDays.forEach { d ->
                                            val dayName = d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).take(1)
                                            val dayNum = d.dayOfMonth.toString()
                                            val isToday = d == today
                                            val inPeriod = isDayInPeriod(d)
                                            val inPredicted = isDayInPredictedNext(d)

                                            val bg = when {
                                                inPeriod -> Color(0xFFE11D48)
                                                inPredicted -> Color(0xFFFFE4E6)
                                                else -> Color(0xFFF3F4F6)
                                            }
                                            val textCol = when {
                                                inPeriod -> Color.White
                                                inPredicted -> Color(0xFF9F1239)
                                                else -> Color(0xFF374151)
                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(dayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(bg, CircleShape)
                                                        .then(
                                                            if (isToday) Modifier.border(1.5.dp, Color(0xFFE11D48), CircleShape) else Modifier
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(dayNum, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textCol)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Water Tracker Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                                border = BorderStroke(1.dp, Color(0xFFE0F2FE))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(22.dp).background(Color(0xFF0284C7), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text("Water Intake Tracker", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0C4A6E))
                                                Text("${previewWaterCount * 250} ml / 2000 ml ($previewWaterCount/8)", fontSize = 8.sp, color = Color(0xFF0284C7))
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            IconButton(
                                                onClick = { updatePreviewWater(previewWaterCount - 1) },
                                                enabled = previewWaterCount > 0,
                                                modifier = Modifier.size(24.dp).background(Color.White, RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color(0xFF0284C7), modifier = Modifier.size(10.dp))
                                            }
                                            IconButton(
                                                onClick = { updatePreviewWater(previewWaterCount + 1) },
                                                modifier = Modifier.size(24.dp).background(Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        (0 until 8).forEach { idx ->
                                            val isFilled = idx < previewWaterCount
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(22.dp)
                                                    .background(
                                                        if (isFilled) Color(0xFF0284C7) else Color.White,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isFilled) Color(0xFF0284C7) else Color(0xFFBAE6FD),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        val nextVal = if (idx + 1 == previewWaterCount) idx else idx + 1
                                                        updatePreviewWater(nextVal)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.WaterDrop,
                                                    contentDescription = null,
                                                    tint = if (isFilled) Color.White else Color(0xFFBAE6FD),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (previewWaterCount >= 8) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🎉 Hydration Goal Met Today!", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                    }
                                }
                            }
                        }
                    }

                    Text("To place this widget on your home screen, toggle the option or tap and hold your home screen, select Widgets, and choose Cycle Tracker.", fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)

                    Button(
                        onClick = { showWidgetModal = false },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Close Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
