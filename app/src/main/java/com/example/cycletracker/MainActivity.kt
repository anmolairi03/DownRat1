package com.example.cycletracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycletracker.data.PeriodRecord
import com.example.cycletracker.ui.*
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFFE53935),
                    background = Color(0xFFFFF8F9),
                    surface = Color.White
                )
            ) {
                var currentTab by remember { mutableStateOf("home") }
                val records by viewModel.allRecords.collectAsState()
                val chatMessages by viewModel.chatMessages.collectAsState()
                val isLoadingAi by viewModel.isLoadingAi.collectAsState()

                var selectedDateForModal by remember { mutableStateOf<String?>(null) }
                var editingRecord by remember { mutableStateOf<PeriodRecord?>(null) }
                var showConflictDialog by remember { mutableStateOf(false) }
                var showChatModal by remember { mutableStateOf(false) }
                var activePhase by remember { mutableStateOf<String?>(null) }

                val sortedRecords = remember(records) {
                    records.sortedByDescending { it.startDate }
                }
                val latestRecord = sortedRecords.firstOrNull()
                val avgCycleLength = viewModel.calculateCalculatedAvgCycleLength()

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == "home",
                                onClick = { currentTab = "home" },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
                                label = { Text("Overview", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFE53935),
                                    selectedTextColor = Color(0xFFE53935),
                                    indicatorColor = Color(0xFFFFE4E6)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == "history",
                                onClick = { currentTab = "history" },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFE53935),
                                    selectedTextColor = Color(0xFFE53935),
                                    indicatorColor = Color(0xFFFFE4E6)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == "ai",
                                onClick = { currentTab = "ai" },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Care") },
                                label = { Text("AI Care", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFE53935),
                                    selectedTextColor = Color(0xFFE53935),
                                    indicatorColor = Color(0xFFFFE4E6)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == "profile",
                                onClick = { currentTab = "profile" },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFE53935),
                                    selectedTextColor = Color(0xFFE53935),
                                    indicatorColor = Color(0xFFFFE4E6)
                                )
                            )
                        }
                    },
                    containerColor = Color(0xFFFFF8F9)
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentTab) {
                            "home" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    CycleHeader()
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Spacer(modifier = Modifier.height(4.dp))

                                        DailyReminderBanner(
                                            latestRecord = latestRecord,
                                            onOpenLogModal = { dateStr ->
                                                editingRecord = null
                                                selectedDateForModal = dateStr
                                            }
                                        )

                                        HeroWheelCard(
                                            latestRecord = latestRecord,
                                            avgCycleLength = avgCycleLength,
                                            onLogPeriodClick = {
                                                val todayStr = LocalDate.now().toString()

                                                if (latestRecord != null && latestRecord.endDate == null) {
                                                    val endDateToSet = if (todayStr >= latestRecord.startDate) todayStr else latestRecord.startDate
                                                    val updated = latestRecord.copy(endDate = endDateToSet)
                                                    viewModel.insertRecord(updated)
                                                    return@HeroWheelCard
                                                }

                                                val currYearMonth = todayStr.substring(0, 7)
                                                val hasLoggedThisMonth = records.any { it.startDate.startsWith(currYearMonth) }

                                                if (hasLoggedThisMonth) {
                                                    showConflictDialog = true
                                                } else {
                                                    selectedDateForModal = todayStr
                                                    editingRecord = null
                                                }
                                            }
                                        )

                                        PhaseInfoRow(
                                            latestRecord = latestRecord,
                                            avgCycleLength = avgCycleLength,
                                            activePhase = activePhase,
                                            onPhaseChange = { activePhase = it }
                                        )

                                        WaterIntakeWidget()

                                        DailyTips()

                                        Text(
                                            text = "Cycle Calendar",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2C1E21)
                                        )

                                         CycleCalendar(
                                            records = records,
                                            latestRecord = latestRecord,
                                            avgCycleLength = avgCycleLength,
                                            activePhase = activePhase,
                                            onDayClick = { dateStr ->
                                                val todayStr = LocalDate.now().toString()
                                                val found = records.find { r ->
                                                    val start = r.startDate
                                                    val end = r.endDate ?: todayStr
                                                    dateStr >= start && dateStr <= end
                                                }
                                                if (found != null) {
                                                    editingRecord = found
                                                    selectedDateForModal = found.startDate
                                                } else {
                                                    if (dateStr <= todayStr) {
                                                        val monthPrefix = dateStr.substring(0, 7)
                                                        val existingMonthRecord = records.find { it.startDate.startsWith(monthPrefix) }
                                                        if (existingMonthRecord != null) {
                                                            editingRecord = existingMonthRecord
                                                            selectedDateForModal = existingMonthRecord.startDate
                                                            showConflictDialog = true
                                                        } else {
                                                            editingRecord = null
                                                            selectedDateForModal = dateStr
                                                        }
                                                    }
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }

                            "history" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Cycle History",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2C1E21),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    if (sortedRecords.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No periods logged yet.",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(sortedRecords) { record ->
                                                Card(
                                                    onClick = {
                                                        editingRecord = record
                                                        selectedDateForModal = record.startDate
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE4E6))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                         Column {
                                                            Text(
                                                                text = "${record.startDate} ${if (record.endDate != null) "to ${record.endDate}" else "(Ongoing)"}",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF2C1E21)
                                                            )
                                                            if (record.notes.isNotBlank()) {
                                                                Text(
                                                                    text = record.notes,
                                                                    fontSize = 12.sp,
                                                                    color = Color.Gray,
                                                                    modifier = Modifier.padding(top = 2.dp)
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

                            "ai" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    AiInsightsCards(
                                        onGetInsights = { question ->
                                            viewModel.startChat(question)
                                            showChatModal = true
                                        },
                                        onOpenSettings = { currentTab = "profile" }
                                    )
                                }
                            }

                            "profile" -> {
                                SettingsScreen(
                                    latestRecord = latestRecord,
                                    records = records,
                                    onResetAllData = { viewModel.resetAllData() },
                                    onBack = { currentTab = "home" }
                                )
                            }
                        }
                    }
                }

                if (showChatModal) {
                    ChatModal(
                        messages = chatMessages,
                        isLoading = isLoadingAi,
                        onSendMessage = { viewModel.sendMessage(it) },
                        onClose = { showChatModal = false }
                    )
                }

                if (selectedDateForModal != null) {
                    RecordDialog(
                        initialDateStr = selectedDateForModal!!,
                        existingRecord = editingRecord,
                        records = records,
                        onSave = { record -> viewModel.insertRecord(record) },
                        onDelete = { record -> viewModel.deleteRecord(record) },
                        onDismiss = {
                            selectedDateForModal = null
                            editingRecord = null
                        }
                    )
                }

                if (showConflictDialog) {
                    AlertDialog(
                        onDismissRequest = { showConflictDialog = false },
                        title = { Text("Period Already Recorded", fontWeight = FontWeight.Bold) },
                        text = { Text("You already have a period entry recorded for this month. Would you like to edit your existing record or delete it and start a new cycle?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showConflictDialog = false
                                    if (latestRecord != null) {
                                        viewModel.deleteRecord(latestRecord)
                                    }
                                    selectedDateForModal = LocalDate.now().toString()
                                    editingRecord = null
                                }
                            ) {
                                Text("Delete & Start New", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showConflictDialog = false
                                    if (latestRecord != null) {
                                        editingRecord = latestRecord
                                        selectedDateForModal = latestRecord.startDate
                                    }
                                }
                            ) {
                                Text("Edit Current")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun String.capitalize(): String =
    this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
