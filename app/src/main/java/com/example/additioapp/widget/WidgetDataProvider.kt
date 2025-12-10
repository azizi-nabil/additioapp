package com.example.additioapp.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.additioapp.R
import com.example.additioapp.data.AppDatabase
import com.example.additioapp.data.model.*
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class WidgetDataProvider(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        const val EXTRA_SECTION_TYPE = "section_type"
        const val SECTION_SCHEDULE = "schedule"
        const val SECTION_EVENTS = "events"
        const val SECTION_TASKS = "tasks"
        const val SECTION_REPLACEMENTS = "replacements"
    }

    private var items: List<WidgetItem> = emptyList()
    private val sectionType: String = intent.getStringExtra(EXTRA_SECTION_TYPE) ?: SECTION_SCHEDULE

    // Wrapper class for different item types
    data class WidgetItem(
        val icon: String,
        val title: String,
        val subtitle: String,
        val color: String = "#666666"
    )

    override fun onCreate() {
        // Initial data load happens in onDataSetChanged
    }

    override fun onDataSetChanged() {
        items = runBlocking {
            loadItemsForSection()
        }
    }

    private suspend fun loadItemsForSection(): List<WidgetItem> {
        val db = AppDatabase.getDatabase(context)
        
        return when (sectionType) {
            SECTION_SCHEDULE -> loadScheduleItems(db)
            SECTION_EVENTS -> loadEventItems(db)
            SECTION_TASKS -> loadTaskItems(db)
            SECTION_REPLACEMENTS -> loadReplacementItems(db)
            else -> emptyList()
        }
    }

    private suspend fun loadScheduleItems(db: AppDatabase): List<WidgetItem> {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val scheduleItems = db.scheduleItemDao().getScheduleItemsForDaySync(dayOfWeek)
        val allClasses = db.classDao().getAllClassesSync()
        val classMap = allClasses.associateBy { it.id }

        return scheduleItems.map { item ->
            val className = classMap[item.classId]?.name ?: ""
            val sessionInfo = if (item.room.isNotEmpty()) "${item.sessionType} (${item.room})" else item.sessionType
            WidgetItem(
                icon = "📚",
                title = if (className.isNotEmpty()) className else sessionInfo,
                subtitle = "${item.startTime} - ${item.endTime}" + if (className.isNotEmpty()) " • $sessionInfo" else "",
                color = classMap[item.classId]?.color ?: "#2196F3"
            )
        }
    }

    private suspend fun loadEventItems(db: AppDatabase): List<WidgetItem> {
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
        
        return events.map { event ->
            val time = event.startTime ?: ""
            WidgetItem(
                icon = "📅",
                title = event.title,
                subtitle = if (time.isNotEmpty()) time else "All day",
                color = event.color ?: "#4CAF50"
            )
        }
    }

    private suspend fun loadTaskItems(db: AppDatabase): List<WidgetItem> {
        val tasks = db.taskDao().getPendingTasksSync()
        val allClasses = db.classDao().getAllClassesSync()
        val classMap = allClasses.associateBy { it.id }
        val taskClassRefs = db.taskDao().getAllTaskClassRefs()
        val taskClassMap = taskClassRefs.groupBy { it.taskId }.mapValues { entry -> 
            entry.value.map { it.classId } 
        }
        
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        
        return tasks.map { task ->
            val priorityEmoji = when (task.priority) {
                "HIGH" -> "🔴"
                "MEDIUM" -> "🟡"
                else -> "🟢"
            }
            val classIds = taskClassMap[task.id] ?: task.classId?.let { listOf(it) } ?: emptyList()
            val classNames = classIds.mapNotNull { classMap[it]?.name }.joinToString(", ")
            val dueStr = task.dueDate?.let { dateFormat.format(Date(it)) } ?: ""
            
            WidgetItem(
                icon = priorityEmoji,
                title = task.title,
                subtitle = listOf(dueStr, classNames).filter { it.isNotEmpty() }.joinToString(" • "),
                color = when (task.priority) {
                    "HIGH" -> "#F44336"
                    "MEDIUM" -> "#FF9800"
                    else -> "#4CAF50"
                }
            )
        }
    }

    private suspend fun loadReplacementItems(db: AppDatabase): List<WidgetItem> {
        val allClasses = db.classDao().getAllClassesSync()
        val classMap = allClasses.associateBy { it.id }
        
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

        val allAbsences = db.teacherAbsenceDao().getAllAbsencesSync()
        // Filter by replacement date (today's replacements) or absence date
        val todayAbsences = allAbsences.filter { absence ->
            (absence.replacementDate != null && absence.replacementDate in startOfDay..endOfDay) ||
            (absence.absenceDate in startOfDay..endOfDay)
        }
        
        return todayAbsences.map { absence ->
            val classIds = absence.getClassIdList()
            val classNames = classIds.mapNotNull { classMap[it]?.name }.take(2).joinToString(", ")
            val isScheduled = absence.status == TeacherAbsenceEntity.STATUS_SCHEDULED || 
                              absence.status == TeacherAbsenceEntity.STATUS_COMPLETED
            val statusEmoji = if (isScheduled) "✅" else "⚠️"
            val title = if (isScheduled) "Replacement" else "Absence"
            val roomInfo = if (absence.room?.isNotEmpty() == true) " in ${absence.room}" else ""
            
            WidgetItem(
                icon = statusEmoji,
                title = "$title: $classNames",
                subtitle = "${absence.sessionType}$roomInfo",
                color = if (isScheduled) "#4CAF50" else "#FF9800"
            )
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= items.size) {
            return RemoteViews(context.packageName, R.layout.item_widget_row)
        }
        
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.item_widget_row)
        
        views.setTextViewText(R.id.textWidgetIcon, item.icon)
        views.setTextViewText(R.id.textWidgetTitle, item.title)
        views.setTextViewText(R.id.textWidgetSubtitle, item.subtitle)
        
        // Set fill-in intent for click handling
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widgetRowRoot, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
