package com.example.cycletracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val TIPS = listOf(
    "Apply a warm heating pad to lower abdomen to relax uterine muscles and reduce cramping naturally.",
    "Stay hydrated with warm herbal teas like chamomile or peppermint to relieve bloating.",
    "Gentle movement or light stretching like yoga can help release endorphins to relieve pain.",
    "Incorporate magnesium-rich foods such as dark chocolate, leafy greens, and nuts into your daily routine."
)

@Composable
fun ReliefTipsBanner() {
    var tipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            tipIndex = (tipIndex + 1) % TIPS.size
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935).copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Period Relief & Wellness Tips",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = TIPS[tipIndex],
                fontSize = 13.sp,
                color = Color(0xFF2C1E21).copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
        }
    }
}
