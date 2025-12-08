package com.example.additioapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.additioapp.data.AppDatabase
import com.example.additioapp.data.repository.AppRepository
import com.example.additioapp.util.NotificationHelper
import com.example.additioapp.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class AdditioApplication : Application() {
    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        AppRepository(
            database.classDao(),
            database.studentDao(),
            database.attendanceDao(),
            database.gradeDao(),
            database.behaviorDao(),
            database.attendanceStatusDao(),
            database.behaviorTypeDao(),
            database.gradeCategoryDao(),
            database.sessionDao(),
            database.unitDao(),
            database.eventDao(),
            database.taskDao(),
            database.scheduleItemDao(),
            getSharedPreferences("additio_prefs", MODE_PRIVATE)
        ) 
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme
        val prefs = getSharedPreferences("additio_prefs", Context.MODE_PRIVATE)
        val themeMode = prefs.getInt("pref_theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)
        
        // Schedule reminder worker (runs every hour)
        val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )
    }
}
