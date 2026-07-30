package com.example.cycletracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cycletracker.data.SettingsManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiInsightsCards(
    settingsManager: SettingsManager,
    onGetInsights: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val commonSymptoms = listOf("Cramps", "Headache", "Bloating", "Fatigue", "Acne", "Tender Breasts", "Nausea")
    val commonMoods = listOf("Happy", "Sad", "Irritable", "Anxious", "Calm", "Mood Swings")

    var flowLevel by remember { mutableStateOf("medium") }
    var painLevel by remember { mutableFloatStateOf(3f) }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var selectedMoods by remember { mutableStateOf(setOf<String>()) }
    var showKeyRequired by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Personal Healthcare AI Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Share your current feelings and get personalized advice. Make sure to add your API key in Settings.",
                fontSize = 12.sp,
                color = Color(0xFF2C1E21).copy(alpha = 0.7f),
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Flow Level
            Text("Flow Level", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21).copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light", "medium", "heavy").forEach { level ->
                    val isSelected = flowLevel == level
                    Surface(
                        onClick = { flowLevel = level },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFE53935) else Color(0xFFF3F4F6),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = level.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(vertical = 8.dp).wrapContentWidth(Alignment.CenterHorizontally),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pain Level
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cramp/Pain Level", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21).copy(alpha = 0.7f))
                Text("${painLevel.toInt()} / 10", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21).copy(alpha = 0.7f))
            }
            Slider(
                value = painLevel,
                onValueChange = { painLevel = it },
                valueRange = 0f..10f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFE53935),
                    activeTrackColor = Color(0xFFE53935),
                    inactiveTrackColor = Color(0xFFE53935).copy(alpha = 0.2f)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Symptoms
            Text("Symptoms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21).copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                commonSymptoms.forEach { item ->
                    val isSelected = selectedSymptoms.contains(item)
                    Surface(
                        onClick = { 
                            selectedSymptoms = if (isSelected) selectedSymptoms - item else selectedSymptoms + item
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFE53935) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFE53935) else Color.LightGray)
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mood
            Text("Mood", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C1E21).copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                commonMoods.forEach { item ->
                    val isSelected = selectedMoods.contains(item)
                    Surface(
                        onClick = { 
                            selectedMoods = if (isSelected) selectedMoods - item else selectedMoods + item
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFE53935) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFE53935) else Color.LightGray)
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (settingsManager.geminiApiKey.isBlank()) {
                        showKeyRequired = true
                    } else {
                        val prompt = """
                            Please provide personalized healthcare advice based on my personal attributes and current logged symptoms.
                            
                            User Profile:
                            - Weight: ${settingsManager.weight.ifBlank { "Not provided" }}
                            - Height: ${settingsManager.height.ifBlank { "Not provided" }}
                            - Age: ${settingsManager.age.ifBlank { "Not provided" }}
                            - Average Cycle Length: ${settingsManager.averageCycleLength} days
                            
                            Current Context & Symptoms:
                            - Flow Level: $flowLevel
                            - Pain/Cramp Level: ${painLevel.toInt()}/10
                            - Symptoms: ${if (selectedSymptoms.isEmpty()) "None" else selectedSymptoms.joinToString(", ")}
                            - Mood: ${if (selectedMoods.isEmpty()) "Normal" else selectedMoods.joinToString(", ")}
                            
                            Instructions:
                            Provide a summary of personalized recommendations tailored to my profile and current symptoms in maximum 5 bullet points (maximum 2 lines each).
                        """.trimIndent()
                        onGetInsights(prompt)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Insights", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showKeyRequired) {
        Dialog(onDismissRequest = { showKeyRequired = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFFF1F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFFE53935))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Setup API Key First", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To get personalized AI insights and chat with Gemini, please first set up your API key in Settings.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showKeyRequired = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                showKeyRequired = false
                                onOpenSettings()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("Go to Settings")
                        }
                    }
                }
            }
        }
    }
}
