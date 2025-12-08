package com.example.additioapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.additioapp.AdditioApplication
import com.example.additioapp.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "ReminderWorker"
        const val WORK_NAME = "reminder_check"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderWorker started")
        
        return withContext(Dispatchers.IO) {
            try {
                val app = applicationContext as AdditioApplication
                val repository = app.repository
                
                // Get current time
                val now = Calendar.getInstance()
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                // Check for today's events
                val todayEvents = repository.getEventsForDateSync(today.timeInMillis)
                for (event in todayEvents) {
                    // Only notify for events that haven't started yet
                    if (event.startTime != null) {
                        val parts = event.startTime.split(":")
                        val eventHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val eventMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        
                        val eventTime = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, eventHour)
                            set(Calendar.MINUTE, eventMinute)
                        }
                        
                        // Notify 30 minutes before or if it's within the next hour
                        val diffMinutes = (eventTime.timeInMillis - now.timeInMillis) / (1000 * 60)
                        if (diffMinutes in 0..60) {
                            NotificationHelper.showEventNotification(
                                applicationContext,
                                "📅 ${event.title}",
                                "Starting at ${event.startTime}",
                                event.id
                            )
                        }
                    }
                }
                
                // Check for today's pending tasks
                val pendingTasks = repository.getPendingTasksSync()
                for (task in pendingTasks) {
                    if (task.dueDate != null && task.dueDate >= today.timeInMillis && task.dueDate < tomorrow.timeInMillis) {
                        NotificationHelper.showTaskNotification(
                            applicationContext,
                            "📝 Task Due Today",
                            task.title,
                            task.id
                        )
                    }
                }
                
                Log.d(TAG, "ReminderWorker completed successfully")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "ReminderWorker failed", e)
                Result.failure()
            }
        }
    }
}
