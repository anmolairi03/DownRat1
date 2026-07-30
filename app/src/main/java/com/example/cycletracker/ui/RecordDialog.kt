package com.example.cycletracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cycletracker.data.PeriodRecord
import java.time.LocalDate

@Composable
fun RecordDialog(
    initialDateStr: String,
    existingRecord: PeriodRecord?,
    records: List<PeriodRecord> = emptyList(),
    onSave: (record: PeriodRecord) -> Unit,
    onDelete: ((record: PeriodRecord) -> Unit)?,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(existingRecord?.startDate ?: initialDateStr) }
    var isOngoing by remember { mutableStateOf(existingRecord?.endDate == null) }
    val flowLevel = existingRecord?.flowLevel ?: "medium"
    var endDate by remember {
        mutableStateOf(
            existingRecord?.endDate ?: LocalDate.parse(initialDateStr).plusDays(4).toString()
        )
    }

    val currentMonthPrefix = remember { LocalDate.now().toString().substring(0, 7) }
    val isCurrentMonth = startDate.startsWith(currentMonthPrefix)

    val targetMonthPrefix = if (startDate.length >= 7) startDate.substring(0, 7) else ""
    val hasMonthConflict = remember(startDate, records, existingRecord) {
        if (targetMonthPrefix.length == 7) {
            records.any { r ->
                r.id != (existingRecord?.id ?: 0) && r.startDate.startsWith(targetMonthPrefix)
            }
        } else false
    }

    LaunchedEffect(isCurrentMonth) {
        if (!isCurrentMonth) {
            isOngoing = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingRecord != null) "Edit Period Record" else "Log Period Record",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C1E21)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Start Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOngoing && isCurrentMonth,
                        onCheckedChange = { if (isCurrentMonth) isOngoing = it },
                        enabled = isCurrentMonth,
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE53935))
                    )
                    Text(
                        text = "Period is currently ongoing",
                        fontSize = 13.sp,
                        color = if (isCurrentMonth) Color(0xFF2C1E21) else Color.Gray
                    )
                }

                if (!isCurrentMonth) {
                    Text(
                        text = "Ongoing status can only be set for the current month.",
                        fontSize = 11.sp,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (!isOngoing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("End Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (hasMonthConflict) {
                    Text(
                        text = "Only one period start date allowed per month. A record already exists for $targetMonthPrefix.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (existingRecord != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(existingRecord)
                                onDismiss()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                    Button(
                        onClick = {
                            if (!hasMonthConflict) {
                                val record = PeriodRecord(
                                    id = existingRecord?.id ?: 0,
                                    startDate = startDate,
                                    endDate = if (isOngoing) null else endDate,
                                    flowLevel = flowLevel,
                                    painLevel = existingRecord?.painLevel ?: 2,
                                    notes = existingRecord?.notes ?: ""
                                )
                                onSave(record)
                                onDismiss()
                            }
                        },
                        enabled = !hasMonthConflict,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Save Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
