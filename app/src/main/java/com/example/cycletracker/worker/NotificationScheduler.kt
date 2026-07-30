package com.example.cycletracker.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import com.example.cycletracker.data.PeriodRecord

object NotificationScheduler {
    fun scheduleNextPeriodReminder(
        context: Context,
        latestRecord: PeriodRecord?,
        avgCycleLength: Int,
        leadTimeDays: Int
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("period_reminder")

        if (latestRecord == null || latestRecord.endDate == null) return

        val start = LocalDate.parse(latestRecord.startDate)
        val nextStart = start.plusDays(avgCycleLength.toLong())
        val reminderDate = nextStart.minusDays(leadTimeDays.toLong())
        
        val today = LocalDate.now()
        val daysUntilReminder = ChronoUnit.DAYS.between(today, reminderDate)

        if (daysUntilReminder > 0) {
            val data = Data.Builder()
                .putString("title", "Upcoming Period")
                .putString("message", "Your period is expected to start in $leadTimeDays days.")
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(daysUntilReminder, TimeUnit.DAYS)
                .setInputData(data)
                .addTag("period_reminder")
                .build()

            workManager.enqueue(workRequest)
        }
    }

    fun cancelPeriodReminder(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("period_reminder")
    }

    fun scheduleDailyStatusReminder(context: Context, timeStr: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("daily_status_reminder")

        try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 20
                val minute = parts[1].toIntOrNull() ?: 0
                val targetTime = LocalTime.of(hour, minute)
                val now = LocalDateTime.now()
                var targetDateTime = LocalDateTime.of(LocalDate.now(), targetTime)
                if (now.isAfter(targetDateTime)) {
                    targetDateTime = targetDateTime.plusDays(1)
                }
                val initialDelayMinutes = Duration.between(now, targetDateTime).toMinutes()

                val data = Data.Builder()
                    .putString("title", "Daily Health & Hydration Check")
                    .putString("message", "Don't forget to log your water intake and daily period status!")
                    .build()

                val periodicWork = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                    .setInputData(data)
                    .addTag("daily_status_reminder")
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "daily_status_reminder_work",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleWaterIntervalReminder(context: Context, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        val clampedHours = intervalHours.coerceAtLeast(1).toLong()

        val data = Data.Builder()
            .putString("title", "💧 Drink Water Reminder")
            .putString("message", "Time to stay hydrated! Drink a glass of water now.")
            .putBoolean("is_water_reminder", true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<NotificationWorker>(clampedHours, TimeUnit.HOURS)
            .setInputData(data)
            .addTag("water_interval_reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "water_interval_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    fun cancelWaterIntervalReminder(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("water_interval_reminder")
        WorkManager.getInstance(context).cancelUniqueWork("water_interval_work")
    }

    fun cancelDailyStatusReminder(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("daily_status_reminder")
        WorkManager.getInstance(context).cancelUniqueWork("daily_status_reminder_work")
    }
}

