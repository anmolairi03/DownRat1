package com.example.cycletracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.SwitchColors
import androidx.glance.appwidget.SwitchDefaults
import com.example.cycletracker.data.AppDatabase
import com.example.cycletracker.data.PeriodRecord
import com.example.cycletracker.data.SettingsManager
import com.example.cycletracker.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class UpdateWaterActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val repository = SettingsRepository.getInstance(appContext)
        withContext(Dispatchers.IO) {
            try {
                val delta = parameters[deltaKey]
                val setCount = parameters[setCountKey]
                val appState = repository.getAppState()
                val current = appState.waterCount
                val newCount = if (setCount != null) {
                    if (setCount == current) current - 1 else setCount
                } else if (delta != null) {
                    (current + delta).coerceAtLeast(0)
                } else {
                    current
                }
                repository.setWaterCount(newCount)
                val settingsManager = SettingsManager(appContext)
                settingsManager.setWaterCountForToday(newCount)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            CycleWidget().update(appContext, glanceId)
            CycleWidget().updateAll(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val deltaKey = ActionParameters.Key<Int>("delta")
        val setCountKey = ActionParameters.Key<Int>("setCount")
    }
}

class TogglePeriodSwitchActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val repository = SettingsRepository.getInstance(appContext)
        withContext(Dispatchers.IO) {
            try {
                val currentAppState = repository.getAppState()
                val newState = !currentAppState.periodActiveSwitch
                repository.setPeriodActiveSwitch(newState)

                val db = AppDatabase.getDatabase(appContext)
                val records = db.periodDao().getAllRecordsSync()
                val latest = records.firstOrNull()
                val todayStr = LocalDate.now().toString()

                if (newState) {
                    if (latest == null || latest.endDate != null) {
                        val newRecord = PeriodRecord(
                            startDate = todayStr,
                            endDate = null,
                            flowLevel = "medium",
                            painLevel = 2,
                            notes = ""
                        )
                        db.periodDao().insertRecord(newRecord)
                    }
                } else {
                    if (latest != null && latest.endDate == null) {
                        val updated = latest.copy(endDate = todayStr)
                        db.periodDao().insertRecord(updated)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            CycleWidget().update(appContext, glanceId)
            CycleWidget().updateAll(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ToggleWaterSwitchActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val repository = SettingsRepository.getInstance(appContext)
        withContext(Dispatchers.IO) {
            try {
                val currentAppState = repository.getAppState()
                val newState = !currentAppState.waterReminderSwitch
                repository.setWaterReminderSwitch(newState)

                val sm = SettingsManager(appContext)
                sm.waterReminderEnabled = newState
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            CycleWidget().update(appContext, glanceId)
            CycleWidget().updateAll(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ToggleCycleActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val repository = SettingsRepository.getInstance(appContext)
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val records = db.periodDao().getAllRecordsSync()
                val latest = records.firstOrNull()
                val todayStr = LocalDate.now().toString()

                val newState: Boolean
                if (latest != null && latest.endDate == null) {
                    newState = false
                    val updated = latest.copy(endDate = todayStr)
                    db.periodDao().insertRecord(updated)
                } else {
                    newState = true
                    val newRecord = PeriodRecord(
                        startDate = todayStr,
                        endDate = null,
                        flowLevel = "medium",
                        painLevel = 2,
                        notes = ""
                    )
                    db.periodDao().insertRecord(newRecord)
                }
                repository.setPeriodActiveSwitch(newState)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            CycleWidget().update(appContext, glanceId)
            CycleWidget().updateAll(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class SetWidgetPhaseActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val repository = SettingsRepository.getInstance(appContext)
        withContext(Dispatchers.IO) {
            try {
                val phase = parameters[phaseKey] ?: "all"
                repository.setSelectedPhase(phase)
                val settingsManager = SettingsManager(appContext)
                settingsManager.widgetSelectedPhase = phase
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            CycleWidget().update(appContext, glanceId)
            CycleWidget().updateAll(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val phaseKey = ActionParameters.Key<String>("phase")
    }
}

private data class WidgetStateData(
    val records: List<PeriodRecord>?,
    val waterCount: Int,
    val selectedPhase: String,
    val averageCycleLength: Int,
    val periodActiveSwitch: Boolean,
    val waterReminderSwitch: Boolean
)

class CycleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val repository = SettingsRepository.getInstance(appContext)
        val stateData = withContext(Dispatchers.IO) {
            val recs = try {
                val db = AppDatabase.getDatabase(appContext)
                db.periodDao().getAllRecordsSync()
            } catch (e: Exception) {
                null
            }
            val sm = SettingsManager(appContext)
            val appState = repository.getAppState()
            WidgetStateData(
                records = recs,
                waterCount = appState.waterCount,
                selectedPhase = appState.selectedPhase,
                averageCycleLength = sm.averageCycleLength,
                periodActiveSwitch = appState.periodActiveSwitch,
                waterReminderSwitch = appState.waterReminderSwitch
            )
        }

        val records = stateData.records
        val waterCount = stateData.waterCount
        val selectedPhase = stateData.selectedPhase
        val averageCycleLength = stateData.averageCycleLength

        val latestRecord = records?.firstOrNull()

        val today = LocalDate.now()
        val todayStr = today.toString()
        val monthYearLabel = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

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
                val avgLen = averageCycleLength.toLong().coerceAtLeast(20L)

                if (isOngoing) {
                    periodDay = ChronoUnit.DAYS.between(start, today).toInt() + 1
                }

                var cyclesElapsed = 0L
                if (!start.isAfter(today)) {
                    val daysDiff = ChronoUnit.DAYS.between(start, today)
                    cyclesElapsed = daysDiff / avgLen
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

        val monday = today.with(DayOfWeek.MONDAY)
        val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

        fun getDayPhase(d: LocalDate): String {
            if (latestRecord == null) return "none"
            return try {
                val start = LocalDate.parse(latestRecord.startDate.split("T")[0])
                val avgLen = averageCycleLength.toLong().coerceAtLeast(20L)
                val diff = ChronoUnit.DAYS.between(start, d)
                val dayNum = ((diff % avgLen) + avgLen) % avgLen + 1
                when {
                    dayNum in 1..5 -> "menstrual"
                    dayNum in 1..(avgLen - 15) -> "follicular"
                    dayNum in (avgLen - 19)..(avgLen - 14) -> "ovulation"
                    else -> "luteal"
                }
            } catch (e: Exception) {
                "none"
            }
        }

        fun isDayInPeriod(d: LocalDate): Boolean {
            val dStr = d.toString()
            return records?.any { r ->
                val rStart = r.startDate.split("T")[0]
                val rEnd = r.endDate?.split("T")?.get(0) ?: todayStr
                dStr >= rStart && dStr <= rEnd
            } ?: false
        }

        fun isDayInPredictedNext(d: LocalDate): Boolean {
            if (nextPeriodStart == null || nextPeriodEnd == null) return false
            val dStr = d.toString()
            val pStartStr = nextPeriodStart.toString()
            val pEndStr = nextPeriodEnd.toString()
            return dStr >= pStartStr && dStr <= pEndStr
        }

        val targetGlasses = 8

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.White))
                    .padding(6.dp)
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xFFE11D48)))
                            .cornerRadius(6.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🩸",
                            style = TextStyle(fontSize = 11.sp)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cycle & Water Widget",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = ColorProvider(Color(0xFF2C1E21))
                            )
                        )
                        Text(
                            text = monthYearLabel,
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = ColorProvider(Color(0xFF6B7280))
                            )
                        )
                    }
                }

                // Cycle Status Banner
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xFFFFF1F2)))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = if (isOngoing) "PERIOD ONGOING - DAY $periodDay" else "NEXT PERIOD",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = ColorProvider(Color(0xFFE11D48))
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(1.dp))
                            Text(
                                text = if (isOngoing) "Active Now" else if (daysUntilNext > 0) "$daysUntilNext days" else if (daysUntilNext == 0) "Expected Today" else "${-daysUntilNext} days late",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ColorProvider(Color(0xFF2C1E21))
                                )
                            )
                        }

                        Button(
                            text = if (isOngoing) "End Cycle" else "Start Cycle",
                            onClick = actionRunCallback<ToggleCycleActionCallback>(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = ColorProvider(Color(0xFFE11D48)),
                                contentColor = ColorProvider(Color.White)
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(3.dp))

                // Week Calendar & Spaced Phase Filter Buttons
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xFFFAFAFA)))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 This Week",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = ColorProvider(Color(0xFF374151))
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "Phase: ${selectedPhase.replaceFirstChar { it.uppercase() }}",
                            style = TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFE11D48))
                            )
                        )
                    }

                    // Phase Filter Buttons with responsive single-tap phase switching
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val phases = listOf("all", "menstrual", "follicular", "ovulation", "luteal")
                        phases.forEachIndexed { idx, phase ->
                            val isSelected = selectedPhase.equals(phase, ignoreCase = true)
                            val btnBg = if (isSelected) Color(0xFFE11D48) else Color(0xFFE5E7EB)
                            val btnText = if (isSelected) Color.White else Color(0xFF374151)
                            val label = when(phase) {
                                "all" -> "All"
                                "menstrual" -> "Men"
                                "follicular" -> "Fol"
                                "ovulation" -> "Ovu"
                                else -> "Lut"
                            }

                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(btnBg))
                                    .cornerRadius(5.dp)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                    .clickable(
                                        actionRunCallback<SetWidgetPhaseActionCallback>(
                                            actionParametersOf(SetWidgetPhaseActionCallback.phaseKey to phase)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(btnText)
                                    )
                                )
                            }
                            if (idx < phases.size - 1) {
                                Spacer(modifier = GlanceModifier.width(5.dp))
                            }
                        }
                    }

                    // Weekday Calendar Days
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        weekDays.forEach { d ->
                            val dayName = d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).take(1)
                            val dayNum = d.dayOfMonth.toString()
                            val isToday = d == today
                            val inPeriod = isDayInPeriod(d)
                            val inPredicted = isDayInPredictedNext(d)
                            val dPhase = getDayPhase(d)

                            val matchesPhase = when (selectedPhase.lowercase()) {
                                "all" -> true
                                "menstrual" -> inPeriod || dPhase == "menstrual"
                                "follicular" -> dPhase == "follicular"
                                "ovulation" -> dPhase == "ovulation"
                                "luteal" -> dPhase == "luteal"
                                else -> true
                            }

                            val bgColor = if (selectedPhase.lowercase() != "all") {
                                if (matchesPhase) {
                                    when(selectedPhase.lowercase()) {
                                        "menstrual" -> Color(0xFFE11D48)
                                        "follicular" -> Color(0xFFEC4899)
                                        "ovulation" -> Color(0xFFA855F7)
                                        "luteal" -> Color(0xFFF59E0B)
                                        else -> Color(0xFFE11D48)
                                    }
                                } else {
                                    Color(0xFFF3F4F6)
                                }
                            } else {
                                when {
                                    inPeriod -> Color(0xFFE11D48)
                                    inPredicted -> Color(0xFFFFE4E6)
                                    else -> Color(0xFFF3F4F6)
                                }
                            }

                            val textColor = if (selectedPhase.lowercase() != "all") {
                                if (matchesPhase) Color.White else Color(0xFF9CA3AF)
                            } else {
                                when {
                                    inPeriod -> Color.White
                                    inPredicted -> Color(0xFF9F1239)
                                    else -> Color(0xFF374151)
                                }
                            }

                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayName,
                                    style = TextStyle(
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(Color(0xFF9CA3AF))
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(1.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(ColorProvider(bgColor))
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 2.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isToday) "[$dayNum]" else dayNum,
                                        style = TextStyle(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorProvider(textColor)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(3.dp))

                // Water Intake Tracker Widget with Water Drop Icons & 1/8, 2/8 labels
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xFFF0F9FF)))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(ColorProvider(Color(0xFF0284C7)))
                                .cornerRadius(6.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "💧",
                                style = TextStyle(fontSize = 11.sp)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Daily Hydration Tracker",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = ColorProvider(Color(0xFF0C4A6E))
                                )
                            )
                            Text(
                                text = "${waterCount * 250} ml / 2000 ml ($waterCount/8 glasses)",
                                style = TextStyle(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorProvider(Color(0xFF0284C7))
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                text = " - ",
                                onClick = actionRunCallback<UpdateWaterActionCallback>(
                                    actionParametersOf(UpdateWaterActionCallback.deltaKey to -1)
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = ColorProvider(Color.White),
                                    contentColor = ColorProvider(Color(0xFF0284C7))
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Button(
                                text = " + ",
                                onClick = actionRunCallback<UpdateWaterActionCallback>(
                                    actionParametersOf(UpdateWaterActionCallback.deltaKey to 1)
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = ColorProvider(Color(0xFF0284C7)),
                                    contentColor = ColorProvider(Color.White)
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    // Row of 8 Glasses with Water Drop icons & 1/8 2/8 labels
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        (0 until targetGlasses).forEach { idx ->
                            val glassNum = idx + 1
                            val isFilled = idx < waterCount
                            val glassBg = if (isFilled) Color(0xFF0284C7) else Color.White
                            val textColor = if (isFilled) Color.White else Color(0xFF0284C7)

                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .background(ColorProvider(glassBg))
                                    .cornerRadius(5.dp)
                                    .padding(vertical = 2.dp)
                                    .clickable(
                                        actionRunCallback<UpdateWaterActionCallback>(
                                            actionParametersOf(UpdateWaterActionCallback.setCountKey to glassNum)
                                        )
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "💧",
                                    style = TextStyle(fontSize = 9.sp)
                                )
                                Spacer(modifier = GlanceModifier.height(1.dp))
                                Text(
                                    text = "$glassNum/8",
                                    style = TextStyle(
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(textColor)
                                    )
                                )
                            }
                            if (idx < targetGlasses - 1) {
                                Spacer(modifier = GlanceModifier.width(2.dp))
                            }
                        }
                    }

                    if (waterCount >= targetGlasses) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "🎉 Hydration Goal Met!",
                            style = TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF0369A1))
                            )
                        )
                    }
                }
            }
        }
    }
}
