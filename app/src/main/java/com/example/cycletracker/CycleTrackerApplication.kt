package com.example.cycletracker

import android.app.Application
import com.example.cycletracker.data.AppDatabase

class CycleTrackerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
