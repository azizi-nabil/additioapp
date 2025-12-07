package com.example.additioapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.EventEntity
import com.example.additioapp.data.model.ScheduleItemEntity
import com.example.additioapp.data.model.TaskEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private val classViewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private val repository by lazy {
        (requireActivity().application as AdditioApplication).repository
    }

    private var classLookup: Map<Long, ClassEntity> = emptyMap()
    
    // Expansion state for lists
    private var isTasksExpanded = false
    private var isEventsExpanded = false
    private var isClassesExpanded = false
    
    // Store data for re-rendering
    private var cachedTasks: List<TaskEntity> = emptyList()
    private var cachedEvents: List<EventEntity> = emptyList()
    private var cachedScheduleItems: List<ScheduleItemEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textGreeting = view.findViewById<TextView>(R.id.textGreeting)
        val textClasses = view.findViewById<TextView>(R.id.textTotalClasses)
        val textStudents = view.findViewById<TextView>(R.id.textTotalStudents)
        val textPendingTasks = view.findViewById<TextView>(R.id.textPendingTasks)
        val textDate = view.findViewById<TextView>(R.id.textDate)
        val textAcademicYear = view.findViewById<TextView>(R.id.textAcademicYear)

        val actionAttendance = view.findViewById<View>(R.id.actionAttendance)
        val actionPlanner = view.findViewById<View>(R.id.actionPlanner)
        val actionGradebook = view.findViewById<View>(R.id.actionGradebook)
        val actionReports = view.findViewById<View>(R.id.actionReports)

        val containerTodayClasses = view.findViewById<LinearLayout>(R.id.containerTodayClasses)
        val containerUpcomingEvents = view.findViewById<LinearLayout>(R.id.containerUpcomingEvents)
        val containerPendingTasks = view.findViewById<LinearLayout>(R.id.containerPendingTasks)

        val btnViewSchedule = view.findViewById<TextView>(R.id.btnViewSchedule)
        val btnViewEvents = view.findViewById<TextView>(R.id.btnViewEvents)
        val btnViewTasks = view.findViewById<TextView>(R.id.btnViewTasks)

        // Set Greeting based on time of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        textGreeting.text = when {
            hour < 12 -> "Good Morning ☀️"
            hour < 17 -> "Good Afternoon 🌤️"
            else -> "Good Evening 🌙"
        }

        // Set Date
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        textDate.text = dateFormat.format(Date())

        // Set Academic Year
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val academicYear = if (currentMonth >= Calendar.SEPTEMBER) {
            "$currentYear-${currentYear + 1}"
        } else {
            "${currentYear - 1}-$currentYear"
        }
        textAcademicYear.text = academicYear

        // Store students locally to update counts when classes change
        var allStudentsList: List<com.example.additioapp.data.model.StudentEntity> = emptyList()

        fun updateStudentCount() {
            if (classLookup.isEmpty()) {
                textStudents.text = "0"
                return
            }
            val currentYearClassIds = classLookup.values
                .filter { it.year == academicYear }
                .map { it.id }
                .toSet()
            val filteredStudents = allStudentsList.filter { currentYearClassIds.contains(it.classId) }
            textStudents.text = filteredStudents.size.toString()
        }

        classViewModel.allClasses.observe(viewLifecycleOwner) { classes ->
            classLookup = classes.associateBy { it.id }
            val filteredClasses = classes.filter { it.year == academicYear }
            textClasses.text = filteredClasses.size.toString()
            updateStudentCount()
            loadTodayClasses(containerTodayClasses)
            loadUpcomingEvents(containerUpcomingEvents)
        }

        studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
            allStudentsList = students
            updateStudentCount()
        }

        // Observe pending tasks count
        repository.getPendingTasks().observe(viewLifecycleOwner) { tasks ->
            textPendingTasks.text = tasks.size.toString()
            loadPendingTasks(containerPendingTasks, tasks)
        }

        // Navigation actions
        actionAttendance.setOnClickListener { findNavController().navigate(R.id.classesFragment) }
        actionPlanner.setOnClickListener { findNavController().navigate(R.id.plannerFragment) }
        actionGradebook.setOnClickListener { findNavController().navigate(R.id.gradebookFragment) }
        actionReports.setOnClickListener { findNavController().navigate(R.id.reportsFragment) }

        view.findViewById<View>(R.id.cardClasses).setOnClickListener {
            findNavController().navigate(R.id.classesFragment)
        }
        view.findViewById<View>(R.id.cardStudents).setOnClickListener {
            findNavController().navigate(R.id.classesFragment)
        }
        view.findViewById<View>(R.id.cardTasks).setOnClickListener {
            val bundle = Bundle().apply { putInt("tabIndex", 1) } // Tasks tab
            findNavController().navigate(R.id.plannerFragment, bundle)
        }

        btnViewSchedule.setOnClickListener {
            val bundle = Bundle().apply { putInt("tabIndex", 2) } // Schedule tab
            findNavController().navigate(R.id.plannerFragment, bundle)
        }
        btnViewEvents.setOnClickListener {
            findNavController().navigate(R.id.plannerFragment) // Events tab (default 0)
        }
        btnViewTasks.setOnClickListener {
            val bundle = Bundle().apply { putInt("tabIndex", 1) } // Tasks tab
            findNavController().navigate(R.id.plannerFragment, bundle)
        }
    }

    private fun loadTodayClasses(container: LinearLayout) {
        container.removeAllViews()
        val today = Calendar.getInstance()
        val dayOfWeek = today.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...

        lifecycleScope.launch {
            val scheduleItems = withContext(Dispatchers.IO) {
                repository.getScheduleItemsForDaySync(dayOfWeek)
            }

            cachedScheduleItems = scheduleItems
            renderScheduleItems(container, scheduleItems)
        }
    }
    
    private fun renderScheduleItems(container: LinearLayout, scheduleItems: List<ScheduleItemEntity>) {
        container.removeAllViews()
        
        if (scheduleItems.isEmpty()) {
            addEmptyState(container, "No classes scheduled for today", "🎉")
            return
        }
        
        val itemsToShow = if (isClassesExpanded) scheduleItems else scheduleItems.take(4)
        itemsToShow.forEach { item ->
            addScheduleItemRow(container, item)
        }

        if (scheduleItems.size > 4) {
            if (isClassesExpanded) {
                addCollapseIndicator(container, "classes") {
                    isClassesExpanded = false
                    renderScheduleItems(container, scheduleItems)
                }
            } else {
                addExpandIndicator(container, scheduleItems.size - 4, "more classes") {
                    isClassesExpanded = true
                    renderScheduleItems(container, scheduleItems)
                }
            }
        }
    }

    private fun addScheduleItemRow(container: LinearLayout, item: ScheduleItemEntity) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        val classInfo = classLookup[item.classId]
        
        row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(
            try { Color.parseColor(classInfo?.color ?: "#2196F3") } catch (e: Exception) { Color.parseColor("#2196F3") }
        )
        row.findViewById<TextView>(R.id.textRowTitle).text = classInfo?.name ?: "Class"
        row.findViewById<TextView>(R.id.textRowMeta).text = "${item.startTime} - ${item.endTime} • ${item.sessionType}"
        
        val roomView = row.findViewById<TextView>(R.id.textRowExtra)
        if (item.room.isNotEmpty()) {
            roomView.text = "📍 ${item.room}"
            roomView.visibility = View.VISIBLE
        } else {
            roomView.visibility = View.GONE
        }
        
        // Click to go to schedule
        row.setOnClickListener {
            val bundle = Bundle().apply { 
                putInt("tabIndex", 2) // Schedule tab
            }
            findNavController().navigate(R.id.plannerFragment, bundle)
        }
        
        container.addView(row)
    }

    private fun loadUpcomingEvents(container: LinearLayout) {
        container.removeAllViews()
        
        lifecycleScope.launch {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val endDate = today.clone() as Calendar
            endDate.add(Calendar.DAY_OF_MONTH, 7)

            val events = withContext(Dispatchers.IO) {
                repository.getEventsInRangeSync(today.timeInMillis, endDate.timeInMillis)
            }

            cachedEvents = events
            renderEvents(container, events)
        }
    }
    
    private fun renderEvents(container: LinearLayout, events: List<EventEntity>) {
        container.removeAllViews()
        
        if (events.isEmpty()) {
            addEmptyState(container, "No upcoming events this week", "📭")
            return
        }

        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val eventsToShow = if (isEventsExpanded) events else events.take(4)
        eventsToShow.forEach { event ->
            addEventRow(container, event, dateFormat)
        }

        if (events.size > 4) {
            if (isEventsExpanded) {
                addCollapseIndicator(container, "events") {
                    isEventsExpanded = false
                    renderEvents(container, events)
                }
            } else {
                addExpandIndicator(container, events.size - 4, "more events") {
                    isEventsExpanded = true
                    renderEvents(container, events)
                }
            }
        }
    }

    private fun addEventRow(container: LinearLayout, event: EventEntity, dateFormat: SimpleDateFormat) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        
        val eventColor = event.color ?: classLookup[event.classId]?.color ?: when (event.eventType) {
            "EXAM" -> "#F44336"
            "MEETING" -> "#9C27B0"
            "DEADLINE" -> "#FF9800"
            else -> "#2196F3"
        }
        
        row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(
            try { Color.parseColor(eventColor) } catch (e: Exception) { Color.parseColor("#2196F3") }
        )
        row.findViewById<TextView>(R.id.textRowTitle).text = event.title
        
        val timeStr = if (event.isAllDay) "All day" else event.startTime ?: ""
        row.findViewById<TextView>(R.id.textRowMeta).text = "${dateFormat.format(Date(event.date))} • $timeStr"
        
        val typeView = row.findViewById<TextView>(R.id.textRowExtra)
        typeView.text = event.eventType
        typeView.visibility = View.VISIBLE
        
        // Click to go to events
        row.setOnClickListener {
            val bundle = Bundle().apply { 
                putInt("tabIndex", 0) // Events tab
            }
            findNavController().navigate(R.id.plannerFragment, bundle)
        }
        
        container.addView(row)
    }
    private fun loadPendingTasks(container: LinearLayout, tasks: List<TaskEntity>) {
        cachedTasks = tasks
        renderTasks(container, tasks)
    }
    
    private fun renderTasks(container: LinearLayout, tasks: List<TaskEntity>) {
        container.removeAllViews()

        if (tasks.isEmpty()) {
            addEmptyState(container, "All tasks completed! 🎉", "✅")
            return
        }

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        // Sort by due date - nearest first
        val sortedTasks = tasks.sortedBy { it.dueDate }
        val tasksToShow = if (isTasksExpanded) sortedTasks else sortedTasks.take(4)
        tasksToShow.forEach { task ->
            addTaskRow(container, task, dateFormat)
        }

        if (sortedTasks.size > 4) {
            if (isTasksExpanded) {
                addCollapseIndicator(container, "tasks") {
                    isTasksExpanded = false
                    renderTasks(container, tasks)
                }
            } else {
                addExpandIndicator(container, sortedTasks.size - 4, "more tasks") {
                    isTasksExpanded = true
                    renderTasks(container, tasks)
                }
            }
        }
    }

    private fun addTaskRow(container: LinearLayout, task: TaskEntity, dateFormat: SimpleDateFormat) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        
        val priorityColor = when (task.priority) {
            "HIGH" -> "#F44336"
            "MEDIUM" -> "#FF9800"
            "LOW" -> "#4CAF50"
            else -> "#9E9E9E"
        }
        
        row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(Color.parseColor(priorityColor))
        row.findViewById<TextView>(R.id.textRowTitle).text = task.title
        
        val dueStr = if (task.dueDate != null) {
            val isOverdue = task.dueDate < System.currentTimeMillis()
            if (isOverdue) "⚠️ Overdue" else "Due ${dateFormat.format(Date(task.dueDate))}"
        } else "No due date"
        
        val classStr = classLookup[task.classId]?.name ?: ""
        row.findViewById<TextView>(R.id.textRowMeta).text = if (classStr.isNotEmpty()) "$dueStr • $classStr" else dueStr
        
        val priorityView = row.findViewById<TextView>(R.id.textRowExtra)
        priorityView.text = task.priority
        priorityView.visibility = View.VISIBLE
        
        // Click to go to tasks
        row.setOnClickListener {
            val bundle = Bundle().apply { 
                putInt("tabIndex", 1) // Tasks tab
            }
            findNavController().navigate(R.id.plannerFragment, bundle)
        }
        
        container.addView(row)
    }

    private fun addEmptyState(container: LinearLayout, message: String, emoji: String) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowTitle).text = "$emoji $message"
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        container.addView(row)
    }

    private fun addMoreIndicator(container: LinearLayout, count: Int, label: String) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
        row.findViewById<TextView>(R.id.textRowTitle).apply {
            text = "+$count $label"
            setTextColor(Color.parseColor("#2196F3"))
        }
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        
        // Add click handler to navigate to Planner
        row.setOnClickListener {
            val tabIndex = when {
                label.contains("events") -> 0
                label.contains("tasks") -> 1
                else -> 0
            }
            val bundle = Bundle().apply {
                putInt("tabIndex", tabIndex)
            }
            findNavController().navigate(R.id.plannerFragment, bundle)
        }
        
        container.addView(row)
    }
    
    private fun addExpandIndicator(container: LinearLayout, count: Int, label: String, onClick: () -> Unit) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
        row.findViewById<TextView>(R.id.textRowTitle).apply {
            text = "+$count $label"
            setTextColor(Color.parseColor("#2196F3"))
        }
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        row.setOnClickListener { onClick() }
        container.addView(row)
    }
    
    private fun addCollapseIndicator(container: LinearLayout, label: String, onClick: () -> Unit) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
        row.findViewById<TextView>(R.id.textRowTitle).apply {
            text = "Show less $label ▲"
            setTextColor(Color.parseColor("#757575"))
        }
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        row.setOnClickListener { onClick() }
        container.addView(row)
    }
}
