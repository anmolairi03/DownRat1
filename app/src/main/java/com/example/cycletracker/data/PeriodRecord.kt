package com.example.cycletracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_records")
data class PeriodRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: String, // YYYY-MM-DD
    val endDate: String? = null, // YYYY-MM-DD
    val flowLevel: String? = null, // light, medium, heavy
    val painLevel: Int = 0,
    val symptoms: String = "", // comma separated
    val mood: String = "", // comma separated
    val notes: String = ""
)
