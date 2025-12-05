package com.example.additioapp

import android.app.Application
import com.example.additioapp.data.AppDatabase
import com.example.additioapp.data.repository.AppRepository

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
            database.scheduleItemDao()
        ) 
    }
}
