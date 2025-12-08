package com.example.additioapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.additioapp.MainActivity
import com.example.additioapp.R
import com.example.additioapp.data.AppDatabase
import com.example.additioapp.data.model.EventEntity
import com.example.additioapp.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_today)
                
                // Set date
                val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                views.setTextViewText(R.id.textDate, today)
                
                // Get database
                val db = AppDatabase.getDatabase(context)
                
                // Get today's events
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfDay = calendar.timeInMillis
                
                val events = db.eventDao().getEventsInRangeSync(startOfDay, endOfDay)
                val tasks = db.taskDao().getPendingTasksSync()
                
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                
                withContext(Dispatchers.Main) {
                    // Clear all event views first
                    views.setViewVisibility(R.id.textEvent1, View.GONE)
                    views.setViewVisibility(R.id.textEvent2, View.GONE)
                    views.setViewVisibility(R.id.textEvent3, View.GONE)
                    
                    // Show events
                    if (events.isEmpty()) {
                        views.setViewVisibility(R.id.textNoEvents, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.textNoEvents, View.GONE)
                        
                        events.take(3).forEachIndexed { index, event: EventEntity ->
                            val time = event.startTime ?: ""
                            val text = if (time.isNotEmpty()) "$time - ${event.title}" else event.title
                            val viewId = when (index) {
                                0 -> R.id.textEvent1
                                1 -> R.id.textEvent2
                                else -> R.id.textEvent3
                            }
                            views.setTextViewText(viewId, text)
                            views.setViewVisibility(viewId, View.VISIBLE)
                        }
                    }
                    
                    // Clear all task views first
                    views.setViewVisibility(R.id.textTask1, View.GONE)
                    views.setViewVisibility(R.id.textTask2, View.GONE)
                    views.setViewVisibility(R.id.textTask3, View.GONE)
                    
                    // Show tasks
                    if (tasks.isEmpty()) {
                        views.setViewVisibility(R.id.textNoTasks, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.textNoTasks, View.GONE)
                        
                        tasks.take(3).forEachIndexed { index, task: TaskEntity ->
                            val priority = when (task.priority) {
                                "HIGH" -> "🔴"
                                "MEDIUM" -> "🟡"
                                else -> "🟢"
                            }
                            val text = "$priority ${task.title}"
                            val viewId = when (index) {
                                0 -> R.id.textTask1
                                1 -> R.id.textTask2
                                else -> R.id.textTask3
                            }
                            views.setTextViewText(viewId, text)
                            views.setViewVisibility(viewId, View.VISIBLE)
                        }
                    }
                    
                    // Clear all schedule views first
                    views.setViewVisibility(R.id.textSchedule1, View.GONE)
                    views.setViewVisibility(R.id.textSchedule2, View.GONE)
                    views.setViewVisibility(R.id.textSchedule3, View.GONE)
                    
                    // Get today's day of week (Calendar.SUNDAY=1, we need 0=Sunday for our DB)
                    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                    val scheduleItems = db.scheduleItemDao().getScheduleItemsForDaySync(dayOfWeek)
                    
                    // Show schedule
                    if (scheduleItems.isEmpty()) {
                        views.setViewVisibility(R.id.textNoSchedule, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.textNoSchedule, View.GONE)
                        
                        scheduleItems.take(3).forEachIndexed { index, item ->
                            val time = item.startTime
                            val info = if (item.room.isNotEmpty()) "${item.sessionType} (${item.room})" else item.sessionType
                            val text = "$time - $info"
                            val viewId = when (index) {
                                0 -> R.id.textSchedule1
                                1 -> R.id.textSchedule2
                                else -> R.id.textSchedule3
                            }
                            views.setTextViewText(viewId, text)
                            views.setViewVisibility(viewId, View.VISIBLE)
                        }
                    }
                    
                    // Open app on click (entire widget clickable)
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
