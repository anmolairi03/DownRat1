package com.example.cycletracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycletracker.data.PeriodRecord
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
fun CycleCalendar(
    records: List<PeriodRecord>,
    latestRecord: PeriodRecord?,
    avgCycleLength: Int,
    activePhase: String? = null,
    onDayClick: (dateStr: String) -> Unit
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun isPeriodDay(dateStr: String): Boolean {
        val todayStr = LocalDate.now().toString()
        return records.any { r ->
            val start = r.startDate
            val end = if (r.endDate.isNullOrEmpty()) {
                if (r.startDate <= todayStr) todayStr else start
            } else {
                r.endDate
            }
            dateStr >= start && dateStr <= end
        }
    }

    fun isPredictedDay(dateStr: String): Boolean {
        if (latestRecord == null) return false
        if (isPeriodDay(dateStr)) return false
        val latestStart = LocalDate.parse(latestRecord.startDate)
        val nextStart = latestStart.plusDays(avgCycleLength.toLong())
        val nextEnd = nextStart.plusDays(4)
        val cellDate = LocalDate.parse(dateStr)
        return !cellDate.isBefore(nextStart) && !cellDate.isAfter(nextEnd)
    }

    fun getPhaseColor(dateStr: String): Color? {
        if (activePhase == null || latestRecord == null) return null
        
        val cellDate = LocalDate.parse(dateStr)
        val sDate = LocalDate.parse(latestRecord.startDate)
        
        var phaseStartDay = 1
        var phaseEndDay = 5
        
        when (activePhase) {
            "menstrual" -> {
                phaseStartDay = 1
                phaseEndDay = 5
            }
            "follicular" -> {
                phaseStartDay = 1
                phaseEndDay = maxOf(1, avgCycleLength - 15)
            }
            "ovulation" -> {
                phaseStartDay = maxOf(1, avgCycleLength - 19)
                phaseEndDay = maxOf(1, avgCycleLength - 14)
            }
            "luteal" -> {
                phaseStartDay = maxOf(1, avgCycleLength - 13)
                phaseEndDay = avgCycleLength
            }
        }
        
        val phaseStart = sDate.plusDays((phaseStartDay - 1).toLong())
        val phaseEnd = sDate.plusDays((phaseEndDay - 1).toLong())
        
        if (!cellDate.isBefore(phaseStart) && !cellDate.isAfter(phaseEnd)) {
            return when (activePhase) {
                "menstrual" -> Color(0xFFEF4444)
                "follicular" -> Color(0xFF991B1B)
                "ovulation" -> Color(0xFFA855F7)
                "luteal" -> Color(0xFFFACC15)
                else -> null
            }
        }
        return null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Month Selector
            val isMaxMonth = currentYearMonth.year > today.year || (currentYearMonth.year == today.year && currentYearMonth.monthValue >= today.monthValue + 1)
            
            var minYearMonth = YearMonth.now().minusMonths(1)
            if (records.isNotEmpty()) {
                val oldestRecordStart = LocalDate.parse(records.last().startDate)
                val oldestRecordMonth = YearMonth.of(oldestRecordStart.year, oldestRecordStart.month)
                if (oldestRecordMonth.isBefore(minYearMonth)) {
                    minYearMonth = oldestRecordMonth
                }
            }
            val isMinMonth = currentYearMonth.isBefore(minYearMonth) || currentYearMonth == minYearMonth

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    enabled = !isMinMonth
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft, 
                        contentDescription = "Previous",
                        tint = if (isMinMonth) Color.LightGray else LocalContentColor.current
                    )
                }
                Text(
                    text = "${currentYearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentYearMonth.year}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C1E21)
                )
                IconButton(
                    onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                    enabled = !isMaxMonth
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = if (isMaxMonth) Color.LightGray else LocalContentColor.current
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weekday labels
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C1E21).copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val totalGridCells = firstDayOfWeek + daysInMonth
            val rows = (totalGridCells + 6) / 7

            Column {
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - firstDayOfWeek + 1
                            if (cellIndex < firstDayOfWeek || dayNumber > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val cellDate = currentYearMonth.atDay(dayNumber)
                                val dateStr = cellDate.toString()
                                val phaseColor = getPhaseColor(dateStr)
                                val recorded = isPeriodDay(dateStr)
                                val predicted = isPredictedDay(dateStr)
                                val isToday = cellDate == today

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(
                                            color = phaseColor ?: when {
                                                recorded -> Color(0xFFE53935)
                                                predicted -> Color(0xFFE53935).copy(alpha = 0.2f)
                                                isToday -> Color(0xFFE53935).copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = if (isToday && phaseColor == null && !recorded && !predicted) 2.dp else 0.dp,
                                            color = if (isToday && phaseColor == null && !recorded && !predicted) Color(0xFFE53935) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onDayClick(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            phaseColor != null -> if (activePhase == "luteal") Color(0xFF111827) else Color.White
                                            recorded -> Color.White
                                            predicted -> Color(0xFFE53935)
                                            isToday -> Color(0xFFE53935)
                                            else -> Color(0xFF2C1E21)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFE53935), shape = RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Logged", fontSize = 11.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFE53935).copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Predicted", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
