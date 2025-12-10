package com.example.additioapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.additioapp.MainActivity
import com.example.additioapp.R
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // Refresh all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, TodayWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            // Notify data changed for all ListViews
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listSchedule)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listTasks)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listEvents)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listReplacements)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    companion object {
        const val ACTION_REFRESH = "com.example.additioapp.widget.ACTION_REFRESH"
        
        fun refreshAllWidgets(context: Context) {
            val intent = Intent(context, TodayWidgetProvider::class.java)
            intent.action = ACTION_REFRESH
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            
            // Set date
            val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            views.setTextViewText(R.id.textDate, today)
            
            // Set up Schedule ListView
            val scheduleIntent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetDataProvider.EXTRA_SECTION_TYPE, WidgetDataProvider.SECTION_SCHEDULE)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.listSchedule, scheduleIntent)
            views.setEmptyView(R.id.listSchedule, R.id.textNoSchedule)
            
            // Set up Tasks ListView
            val tasksIntent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetDataProvider.EXTRA_SECTION_TYPE, WidgetDataProvider.SECTION_TASKS)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.listTasks, tasksIntent)
            views.setEmptyView(R.id.listTasks, R.id.textNoTasks)
            
            // Set up Events ListView
            val eventsIntent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetDataProvider.EXTRA_SECTION_TYPE, WidgetDataProvider.SECTION_EVENTS)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.listEvents, eventsIntent)
            views.setEmptyView(R.id.listEvents, R.id.textNoEvents)
            
            // Set up Replacements ListView
            val replacementsIntent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetDataProvider.EXTRA_SECTION_TYPE, WidgetDataProvider.SECTION_REPLACEMENTS)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.listReplacements, replacementsIntent)
            views.setEmptyView(R.id.listReplacements, R.id.textNoReplacements)
            
            // Set up pending intent template for list item clicks (opens main app)
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.listSchedule, clickPendingIntent)
            views.setPendingIntentTemplate(R.id.listTasks, clickPendingIntent)
            views.setPendingIntentTemplate(R.id.listEvents, clickPendingIntent)
            views.setPendingIntentTemplate(R.id.listReplacements, clickPendingIntent)
            
            // Open app on header click
            views.setOnClickPendingIntent(R.id.widgetRoot, clickPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
