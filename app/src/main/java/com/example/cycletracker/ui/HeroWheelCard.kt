package com.example.cycletracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycletracker.data.PeriodRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.example.cycletracker.data.SettingsRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun HeroWheelCard(
    latestRecord: PeriodRecord?,
    avgCycleLength: Int,
    onLogPeriodClick: () -> Unit
) {
    val today = LocalDate.now()
    val startDate = latestRecord?.startDate?.let { LocalDate.parse(it) }

    var daysUntilNext = 0
    var nextPeriodStart: LocalDate? = null
    var nextPeriodEnd: LocalDate? = null
    var ovulationDate: LocalDate? = null
    var fertileStart: LocalDate? = null
    var fertileEnd: LocalDate? = null
    var currentCycleDay = 0
    val isOngoing = latestRecord != null && latestRecord.endDate == null
    var periodDay = 0

    if (startDate != null) {
        val daysPassedTotal = ChronoUnit.DAYS.between(startDate, today)
        val cyclesElapsed = if (daysPassedTotal >= 0) (daysPassedTotal / avgCycleLength).toInt() else 0
        
        val currentCycleStart = startDate.plusDays((cyclesElapsed * avgCycleLength).toLong())
        nextPeriodStart = currentCycleStart.plusDays(avgCycleLength.toLong())
        nextPeriodEnd = nextPeriodStart.plusDays(4)
        
        val currentOvulation = currentCycleStart.plusDays((avgCycleLength - 14).toLong())
        if (currentOvulation.plusDays(1) < today) {
            ovulationDate = nextPeriodStart.plusDays((avgCycleLength - 14).toLong())
        } else {
            ovulationDate = currentOvulation
        }
        
        fertileStart = ovulationDate.minusDays(5)
        fertileEnd = ovulationDate.plusDays(1)

        val diffTimeNext = ChronoUnit.DAYS.between(today, nextPeriodStart)
        daysUntilNext = if (diffTimeNext > 0) diffTimeNext.toInt() else 0
        
        val currentDiff = ChronoUnit.DAYS.between(currentCycleStart, today)
        currentCycleDay = currentDiff.toInt() + 1
        
        if (isOngoing) {
            periodDay = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
        }
    }

    val clamped = daysUntilNext.coerceAtLeast(0)
    val progressPercent = if (avgCycleLength > 0) {
        (100f - (clamped.toFloat() / avgCycleLength.toFloat()) * 100f).coerceIn(0f, 100f) / 100f
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(durationMillis = 1000),
        label = "progressAnimation"
    )

    fun formatRange(start: LocalDate?, end: LocalDate?): String {
        if (start == null || end == null) return "---"
        val startMonth = start.format(DateTimeFormatter.ofPattern("MMM"))
        val endMonth = end.format(DateTimeFormatter.ofPattern("MMM"))
        return if (startMonth == endMonth) {
            "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${end.format(DateTimeFormatter.ofPattern("d"))}"
        } else {
            "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${end.format(DateTimeFormatter.ofPattern("MMM d"))}"
        }
    }

    fun formatDate(date: LocalDate?): String {
        if (date == null) return "---"
        return date.format(DateTimeFormatter.ofPattern("MMM d"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFF1F2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (isOngoing) "PERIOD ONGOING" else "NEXT PERIOD IN",
                        color = Color(0xFFE11D48),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (isOngoing) "Day $periodDay" else if (latestRecord != null) "${daysUntilNext.coerceAtLeast(0)}" else "?",
                            fontSize = if (isOngoing) 40.sp else 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2C1E21),
                            lineHeight = if (isOngoing) 40.sp else 64.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (!isOngoing) {
                            Text(
                                text = "days",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
                
                // Semi-circle progress visualization
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(60.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        drawArc(
                            color = Color(0xFFFEE2E2),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFFE11D48),
                            startAngle = 180f,
                            sweepAngle = animatedProgress * 180f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = if (latestRecord != null) {
                            if (isOngoing) "PERIOD DAY $periodDay" else "DAY $currentCycleDay"
                        } else "NO DATA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fertile Window
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF1F2).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE4E6))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "FERTILE\nWINDOW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48),
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRange(fertileStart, fertileEnd),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C1E21),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Ovulation
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFAF5FF).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3E8FF))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "OVULATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA855F7),
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDate(ovulationDate),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C1E21),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Next Period
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "NEXT\nPERIOD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRange(nextPeriodStart, nextPeriodEnd),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C1E21),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogPeriodClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
            ) {
                Text(
                    text = if (latestRecord != null && latestRecord.endDate == null) "End Current Period" else "Log Period Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
