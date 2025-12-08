package com.example.additioapp.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.additioapp.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

class ReplacementAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val absenceId = intent.getLongExtra(EXTRA_ABSENCE_ID, -1)
        val className = intent.getStringExtra(EXTRA_CLASS_NAME) ?: "Class"
        val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE) ?: ""
        val replacementDate = intent.getLongExtra(EXTRA_REPLACEMENT_DATE, 0)
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(replacementDate))
        
        val title = context.getString(com.example.additioapp.R.string.notification_replacement_title)
        val message = context.getString(
            com.example.additioapp.R.string.notification_replacement_message,
            className,
            sessionType,
            dateStr
        )
        
        NotificationHelper.showReplacementNotification(context, title, message, absenceId)
    }
    
    companion object {
        const val EXTRA_ABSENCE_ID = "absence_id"
        const val EXTRA_CLASS_NAME = "class_name"
        const val EXTRA_SESSION_TYPE = "session_type"
        const val EXTRA_REPLACEMENT_DATE = "replacement_date"
        
        /**
         * Schedule a notification for the morning of the replacement date
         */
        fun scheduleNotification(
            context: Context,
            absenceId: Long,
            className: String,
            sessionType: String,
            replacementDate: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, ReplacementAlarmReceiver::class.java).apply {
                putExtra(EXTRA_ABSENCE_ID, absenceId)
                putExtra(EXTRA_CLASS_NAME, className)
                putExtra(EXTRA_SESSION_TYPE, sessionType)
                putExtra(EXTRA_REPLACEMENT_DATE, replacementDate)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                absenceId.toInt() + 30000, // Unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule for 8:00 AM on the replacement date
            val calendar = Calendar.getInstance().apply {
                timeInMillis = replacementDate
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            
            // Only schedule if it's in the future
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        // Fallback to inexact alarm
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
        
        /**
         * Cancel a scheduled notification
         */
        fun cancelNotification(context: Context, absenceId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, ReplacementAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                absenceId.toInt() + 30000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
        }
    }
}
