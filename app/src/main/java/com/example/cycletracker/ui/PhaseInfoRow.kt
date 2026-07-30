package com.example.cycletracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycletracker.data.PeriodRecord

@Composable
fun PhaseInfoRow(
    latestRecord: PeriodRecord?,
    avgCycleLength: Int,
    activePhase: String?,
    onPhaseChange: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PhasePill(
            name = "Menstrual",
            phaseKey = "menstrual",
            icon = Icons.Default.WaterDrop,
            accentColor = Color(0xFFEF4444),
            activePhase = activePhase,
            onClick = { onPhaseChange(if (activePhase == "menstrual") null else "menstrual") },
            modifier = Modifier.weight(1f)
        )
        PhasePill(
            name = "Follicular",
            phaseKey = "follicular",
            icon = Icons.Default.Spa,
            accentColor = Color(0xFFF43F5E),
            activePhase = activePhase,
            onClick = { onPhaseChange(if (activePhase == "follicular") null else "follicular") },
            modifier = Modifier.weight(1f)
        )
        PhasePill(
            name = "Ovulation",
            phaseKey = "ovulation",
            icon = Icons.Default.LocalFlorist,
            accentColor = Color(0xFFEC4899),
            activePhase = activePhase,
            onClick = { onPhaseChange(if (activePhase == "ovulation") null else "ovulation") },
            modifier = Modifier.weight(1f)
        )
        PhasePill(
            name = "Luteal",
            phaseKey = "luteal",
            icon = Icons.Default.Bedtime,
            accentColor = Color(0xFFEAB308),
            activePhase = activePhase,
            onClick = { onPhaseChange(if (activePhase == "luteal") null else "luteal") },
            modifier = Modifier.weight(1f)
        )
    }

    val description = when (activePhase) {
        "menstrual" -> "Menstrual (Days 1-5): Your cycle starts. Rest and hydrate as estrogen and progesterone levels drop."
        "follicular" -> "Follicular (Days 1-13): Estrogen rises. You might feel a boost in energy and improved mood!"
        "ovulation" -> "Ovulation (Day 14): An egg is released. This is your peak fertile window with highest estrogen."
        "luteal" -> "Luteal (Days 15-28): Progesterone rises. You may experience PMS symptoms as your body prepares for the next cycle."
        else -> null
    }

    if (description != null) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE4E6)),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                text = description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                lineHeight = 18.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun PhasePill(
    name: String,
    phaseKey: String,
    icon: ImageVector,
    accentColor: Color,
    activePhase: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = activePhase == phaseKey
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) accentColor else Color(0xFFFFE4E6)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) accentColor else Color(0xFF4B5563)
            )
        }
    }
}
