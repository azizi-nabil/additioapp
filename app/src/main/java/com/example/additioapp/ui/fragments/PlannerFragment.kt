package com.example.additioapp.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.EventEntity
import com.example.additioapp.data.model.ScheduleItemEntity
import com.example.additioapp.data.model.TaskEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.CalendarAdapter
import com.example.additioapp.ui.adapters.CalendarDay
import com.example.additioapp.ui.adapters.EventAdapter
import com.example.additioapp.ui.adapters.ScheduleAdapter
import com.example.additioapp.ui.adapters.TaskAdapter
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PlannerFragment : Fragment() {

    private val classViewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private val repository by lazy {
        (requireActivity().application as AdditioApplication).repository
    }

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventAdapter: EventAdapter
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var scheduleAdapter: ScheduleAdapter

    private var currentMonth = Calendar.getInstance()
    private var selectedDate = Calendar.getInstance()
    private var classes: List<ClassEntity> = emptyList()
    private var currentTab = 0 // 0 = Events, 1 = Tasks, 2 = Schedule
    private var selectedScheduleDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...

    private val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val fullDayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private var dayButtons: List<Button> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_planner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val textCurrentMonth = view.findViewById<TextView>(R.id.textCurrentMonth)
        val btnPrevMonth = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val calendarSection = view.findViewById<LinearLayout>(R.id.calendarSection)
        val recyclerCalendar = view.findViewById<RecyclerView>(R.id.recyclerCalendar)
        val eventsView = view.findViewById<LinearLayout>(R.id.eventsView)
        val tasksView = view.findViewById<LinearLayout>(R.id.tasksView)
        val scheduleView = view.findViewById<LinearLayout>(R.id.scheduleView)
        val recyclerEvents = view.findViewById<RecyclerView>(R.id.recyclerEvents)
        val recyclerTasks = view.findViewById<RecyclerView>(R.id.recyclerTasks)
        val recyclerSchedule = view.findViewById<RecyclerView>(R.id.recyclerSchedule)
        val textSelectedDate = view.findViewById<TextView>(R.id.textSelectedDate)
        val textNoEvents = view.findViewById<TextView>(R.id.textNoEvents)
        val textNoTasks = view.findViewById<TextView>(R.id.textNoTasks)
        val textNoSchedule = view.findViewById<TextView>(R.id.textNoSchedule)
        val textScheduleDay = view.findViewById<TextView>(R.id.textScheduleDay)
        val dayButtonsContainer = view.findViewById<LinearLayout>(R.id.dayButtonsContainer)
        val fabAddEvent = view.findViewById<FloatingActionButton>(R.id.fabAddEvent)

        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("📅 Events"))
        tabLayout.addTab(tabLayout.newTab().setText("📝 Tasks"))
        tabLayout.addTab(tabLayout.newTab().setText("🗓️ Schedule"))

        // Setup day buttons for schedule
        setupDayButtons(dayButtonsContainer, textScheduleDay, recyclerSchedule, textNoSchedule)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                when (currentTab) {
                    0 -> {
                        calendarSection.visibility = View.VISIBLE
                        eventsView.visibility = View.VISIBLE
                        tasksView.visibility = View.GONE
                        scheduleView.visibility = View.GONE
                    }
                    1 -> {
                        calendarSection.visibility = View.VISIBLE
                        eventsView.visibility = View.GONE
                        tasksView.visibility = View.VISIBLE
                        scheduleView.visibility = View.GONE
                        loadTasks(recyclerTasks, textNoTasks)
                    }
                    2 -> {
                        calendarSection.visibility = View.GONE
                        eventsView.visibility = View.GONE
                        tasksView.visibility = View.GONE
                        scheduleView.visibility = View.VISIBLE
                        loadScheduleForDay(recyclerSchedule, textNoSchedule, textScheduleDay)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Set selected date to today
        selectedDate.set(Calendar.HOUR_OF_DAY, 0)
        selectedDate.set(Calendar.MINUTE, 0)
        selectedDate.set(Calendar.SECOND, 0)
        selectedDate.set(Calendar.MILLISECOND, 0)

        // Calendar adapter
        calendarAdapter = CalendarAdapter { day ->
            selectedDate.timeInMillis = day.date
            calendarAdapter.setSelectedDate(day.date)
            updateSelectedDateLabel(textSelectedDate)
            loadEventsForDate(recyclerEvents, textNoEvents)
        }
        recyclerCalendar.adapter = calendarAdapter
        recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)

        // Event adapter
        eventAdapter = EventAdapter(
            onEventClick = { event -> showAddEventDialog(event) },
            onDeleteClick = { event -> deleteEvent(event, recyclerEvents, textNoEvents) }
        )
        recyclerEvents.adapter = eventAdapter
        recyclerEvents.layoutManager = LinearLayoutManager(requireContext())

        // Task adapter
        taskAdapter = TaskAdapter(
            onTaskChecked = { task, isChecked -> toggleTaskComplete(task, isChecked, recyclerTasks, textNoTasks) },
            onTaskClick = { task -> showAddTaskDialog(task) },
            onDeleteClick = { task -> deleteTask(task, recyclerTasks, textNoTasks) }
        )
        recyclerTasks.adapter = taskAdapter
        recyclerTasks.layoutManager = LinearLayoutManager(requireContext())

        // Schedule adapter
        scheduleAdapter = ScheduleAdapter(
            onItemClick = { item -> showAddScheduleDialog(item) },
            onDeleteClick = { item -> deleteScheduleItem(item, recyclerSchedule, textNoSchedule, textScheduleDay) }
        )
        recyclerSchedule.adapter = scheduleAdapter
        recyclerSchedule.layoutManager = LinearLayoutManager(requireContext())

        // Month navigation
        btnPrevMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateCalendar(textCurrentMonth)
        }
        btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateCalendar(textCurrentMonth)
        }

        // FAB - add based on current tab
        fabAddEvent.setOnClickListener {
            when (currentTab) {
                0 -> showAddEventDialog(null)
                1 -> showAddTaskDialog(null)
                2 -> showAddScheduleDialog(null)
            }
        }

        // Observe classes
        classViewModel.allClasses.observe(viewLifecycleOwner) { classList ->
            classes = classList
            val classInfo = classList.associate { it.id to Pair(it.name, it.color) }
            eventAdapter.setClassInfo(classInfo)
            taskAdapter.setClassNames(classList.associate { it.id to it.name })
            scheduleAdapter.setClassInfo(classInfo)
            updateCalendar(textCurrentMonth)
        }

        // Initial load
        updateCalendar(textCurrentMonth)
        updateSelectedDateLabel(textSelectedDate)
        loadEventsForDate(recyclerEvents, textNoEvents)

        // Handle tabIndex argument from navigation
        val tabIndex = arguments?.getInt("tabIndex", 0) ?: 0
        if (tabIndex in 0..2) {
            tabLayout.getTabAt(tabIndex)?.select()
        }
    }

    private fun setupDayButtons(container: LinearLayout, textScheduleDay: TextView, recyclerSchedule: RecyclerView, textNoSchedule: TextView) {
        val buttons = mutableListOf<Button>()
        for (i in 0..6) {
            val btn = Button(requireContext()).apply {
                text = dayNames[i]
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
                setOnClickListener {
                    selectedScheduleDay = i + 1 // 1=Sun, 2=Mon...
                    updateDayButtonStyles(buttons)
                    loadScheduleForDay(recyclerSchedule, textNoSchedule, textScheduleDay)
                }
            }
            buttons.add(btn)
            container.addView(btn)
        }
        dayButtons = buttons
        updateDayButtonStyles(buttons)
    }

    private fun updateDayButtonStyles(buttons: List<Button>) {
        buttons.forEachIndexed { index, btn ->
            if (index + 1 == selectedScheduleDay) {
                btn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
                btn.setTextColor(Color.WHITE)
                btn.setTypeface(null, Typeface.BOLD)
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
                btn.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun loadScheduleForDay(recyclerSchedule: RecyclerView, textNoSchedule: TextView, textScheduleDay: TextView) {
        textScheduleDay.text = "📅 ${fullDayNames[selectedScheduleDay - 1]}"
        
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                repository.getScheduleItemsForDaySync(selectedScheduleDay)
            }
            scheduleAdapter.submitList(items)
            
            if (items.isEmpty()) {
                recyclerSchedule.visibility = View.GONE
                textNoSchedule.visibility = View.VISIBLE
            } else {
                recyclerSchedule.visibility = View.VISIBLE
                textNoSchedule.visibility = View.GONE
            }
        }
    }

    private fun deleteScheduleItem(item: ScheduleItemEntity, recyclerSchedule: RecyclerView, textNoSchedule: TextView, textScheduleDay: TextView) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Remove this class from the schedule?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.deleteScheduleItem(item)
                    }
                    loadScheduleForDay(recyclerSchedule, textNoSchedule, textScheduleDay)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddScheduleDialog(existingItem: ScheduleItemEntity?) {
        if (classes.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a class first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_schedule, null)

        val spinnerClass = dialogView.findViewById<Spinner>(R.id.spinnerScheduleClass)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinnerDayOfWeek)
        val textStartTime = dialogView.findViewById<TextView>(R.id.textScheduleStartTime)
        val textEndTime = dialogView.findViewById<TextView>(R.id.textScheduleEndTime)
        val spinnerSessionType = dialogView.findViewById<Spinner>(R.id.spinnerSessionType)
        val editRoom = dialogView.findViewById<TextInputEditText>(R.id.editScheduleRoom)

        // Class spinner
        val classNames = classes.map { it.name }
        spinnerClass.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classNames)

        // Day spinner
        spinnerDay.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, fullDayNames)

        // Session type spinner
        val sessionTypes = listOf("Cours", "TD", "TP", "Exam", "Other")
        spinnerSessionType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sessionTypes)

        var startTime = "09:00"
        var endTime = "10:30"

        existingItem?.let {
            val classIndex = classes.indexOfFirst { c -> c.id == it.classId }
            if (classIndex >= 0) spinnerClass.setSelection(classIndex)
            spinnerDay.setSelection(it.dayOfWeek - 1)
            startTime = it.startTime
            endTime = it.endTime
            spinnerSessionType.setSelection(sessionTypes.indexOf(it.sessionType).coerceAtLeast(0))
            editRoom.setText(it.room)
        } ?: run {
            spinnerDay.setSelection(selectedScheduleDay - 1)
        }

        textStartTime.text = startTime
        textEndTime.text = endTime

        textStartTime.setOnClickListener {
            val parts = startTime.split(":")
            TimePickerDialog(requireContext(), { _, hour, minute ->
                startTime = String.format("%02d:%02d", hour, minute)
                textStartTime.text = startTime
                // Auto-set end time to 1h30m after start
                val endHour = (hour + 1 + (minute + 30) / 60) % 24
                val endMinute = (minute + 30) % 60
                endTime = String.format("%02d:%02d", endHour, endMinute)
                textEndTime.text = endTime
            }, parts[0].toInt(), parts[1].toInt(), true).show()
        }

        textEndTime.setOnClickListener {
            val parts = endTime.split(":")
            TimePickerDialog(requireContext(), { _, hour, minute ->
                endTime = String.format("%02d:%02d", hour, minute)
                textEndTime.text = endTime
            }, parts[0].toInt(), parts[1].toInt(), true).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingItem == null) "Add Class Schedule" else "Edit Class Schedule")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedClass = classes[spinnerClass.selectedItemPosition]
                
                val item = ScheduleItemEntity(
                    id = existingItem?.id ?: 0,
                    classId = selectedClass.id,
                    dayOfWeek = spinnerDay.selectedItemPosition + 1,
                    startTime = startTime,
                    endTime = endTime,
                    room = editRoom.text.toString().trim(),
                    sessionType = spinnerSessionType.selectedItem.toString()
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (existingItem == null) {
                            repository.insertScheduleItem(item)
                        } else {
                            repository.updateScheduleItem(item)
                        }
                    }
                    loadScheduleForDay(
                        requireView().findViewById(R.id.recyclerSchedule),
                        requireView().findViewById(R.id.textNoSchedule),
                        requireView().findViewById(R.id.textScheduleDay)
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCalendar(textCurrentMonth: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        textCurrentMonth.text = dateFormat.format(currentMonth.time)

        lifecycleScope.launch {
            val days = generateCalendarDays()
            calendarAdapter.submitList(days)
            calendarAdapter.setSelectedDate(selectedDate.timeInMillis)
        }
    }

    private suspend fun generateCalendarDays(): List<CalendarDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<CalendarDay>()
        val calendar = currentMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val monthStart = calendar.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        monthStart.set(Calendar.HOUR_OF_DAY, 0)
        monthStart.set(Calendar.MINUTE, 0)
        monthStart.set(Calendar.SECOND, 0)
        monthStart.set(Calendar.MILLISECOND, 0)

        val monthEnd = monthStart.clone() as Calendar
        monthEnd.add(Calendar.MONTH, 1)
        monthEnd.add(Calendar.DAY_OF_MONTH, -1)
        monthEnd.set(Calendar.HOUR_OF_DAY, 23)
        monthEnd.set(Calendar.MINUTE, 59)

        val events = repository.getEventsInRangeSync(monthStart.timeInMillis, monthEnd.timeInMillis)
        val eventsByDate = events.groupBy { event ->
            val eventCal = Calendar.getInstance()
            eventCal.timeInMillis = event.date
            eventCal.set(Calendar.HOUR_OF_DAY, 0)
            eventCal.set(Calendar.MINUTE, 0)
            eventCal.set(Calendar.SECOND, 0)
            eventCal.set(Calendar.MILLISECOND, 0)
            eventCal.timeInMillis
        }

        // First day of week adjustment (Sunday = 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val offset = firstDayOfWeek - 1

        repeat(offset) {
            days.add(CalendarDay(0, 0, false, false))
        }

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val dayEvents = eventsByDate[calendar.timeInMillis] ?: emptyList()
            val colors = dayEvents.take(3).map { event ->
                event.color ?: classes.find { it.id == event.classId }?.color ?: getEventTypeColor(event.eventType)
            }

            days.add(CalendarDay(
                day = day,
                date = calendar.timeInMillis,
                isCurrentMonth = true,
                isToday = calendar.timeInMillis == today.timeInMillis,
                eventColors = colors
            ))
        }

        days
    }

    private fun getEventTypeColor(type: String): String {
        return when (type) {
            "EXAM" -> "#F44336"
            "MEETING" -> "#9C27B0"
            "DEADLINE" -> "#FF9800"
            else -> "#2196F3"
        }
    }

    private fun updateSelectedDateLabel(textView: TextView) {
        val format = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        textView.text = format.format(selectedDate.time)
    }

    private fun loadEventsForDate(recyclerEvents: RecyclerView, textNoEvents: TextView) {
        lifecycleScope.launch {
            val events = withContext(Dispatchers.IO) {
                repository.getEventsForDateSync(selectedDate.timeInMillis)
            }
            eventAdapter.submitList(events)
            
            if (events.isEmpty()) {
                recyclerEvents.visibility = View.GONE
                textNoEvents.visibility = View.VISIBLE
            } else {
                recyclerEvents.visibility = View.VISIBLE
                textNoEvents.visibility = View.GONE
            }
        }
    }

    private fun loadTasks(recyclerTasks: RecyclerView, textNoTasks: TextView) {
        repository.getPendingTasks().observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
            if (tasks.isEmpty()) {
                recyclerTasks.visibility = View.GONE
                textNoTasks.visibility = View.VISIBLE
            } else {
                recyclerTasks.visibility = View.VISIBLE
                textNoTasks.visibility = View.GONE
            }
        }
    }

    private fun toggleTaskComplete(task: TaskEntity, completed: Boolean, recyclerTasks: RecyclerView, textNoTasks: TextView) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.setTaskCompleted(task.id, completed)
            }
        }
    }

    private fun deleteEvent(event: EventEntity, recyclerEvents: RecyclerView, textNoEvents: TextView) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Delete \"${event.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.deleteEvent(event)
                    }
                    loadEventsForDate(recyclerEvents, textNoEvents)
                    updateCalendar(view?.findViewById(R.id.textCurrentMonth)!!)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: TaskEntity, recyclerTasks: RecyclerView, textNoTasks: TextView) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.deleteTask(task)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddEventDialog(existingEvent: EventEntity?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_event, null)

        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.editEventTitle)
        val editDescription = dialogView.findViewById<TextInputEditText>(R.id.editEventDescription)
        val textDate = dialogView.findViewById<TextView>(R.id.textEventDate)
        val textStartTime = dialogView.findViewById<TextView>(R.id.textStartTime)
        val textEndTime = dialogView.findViewById<TextView>(R.id.textEndTime)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerEventType)
        val spinnerClass = dialogView.findViewById<Spinner>(R.id.spinnerClass)
        val checkAllDay = dialogView.findViewById<CheckBox>(R.id.checkAllDay)

        val eventTypes = listOf("OTHER", "EXAM", "MEETING", "DEADLINE")
        spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, eventTypes)

        val classOptions = listOf("No class") + classes.map { it.name }
        spinnerClass.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classOptions)

        var eventDate = selectedDate.timeInMillis
        var startTime = "09:00"
        var endTime = "10:00"

        existingEvent?.let {
            editTitle.setText(it.title)
            editDescription.setText(it.description)
            eventDate = it.date
            startTime = it.startTime ?: "09:00"
            endTime = it.endTime ?: "10:00"
            checkAllDay.isChecked = it.isAllDay
            spinnerType.setSelection(eventTypes.indexOf(it.eventType).coerceAtLeast(0))
            val classIndex = classes.indexOfFirst { c -> c.id == it.classId }
            spinnerClass.setSelection(if (classIndex >= 0) classIndex + 1 else 0)
        }

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        textDate.text = dateFormat.format(Date(eventDate))
        textStartTime.text = startTime
        textEndTime.text = endTime

        textDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = eventDate
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                eventDate = cal.timeInMillis
                textDate.text = dateFormat.format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        textStartTime.setOnClickListener {
            val parts = startTime.split(":")
            TimePickerDialog(requireContext(), { _, hour, minute ->
                startTime = String.format("%02d:%02d", hour, minute)
                textStartTime.text = startTime
            }, parts[0].toInt(), parts[1].toInt(), true).show()
        }

        textEndTime.setOnClickListener {
            val parts = endTime.split(":")
            TimePickerDialog(requireContext(), { _, hour, minute ->
                endTime = String.format("%02d:%02d", hour, minute)
                textEndTime.text = endTime
            }, parts[0].toInt(), parts[1].toInt(), true).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingEvent == null) "Add Event" else "Edit Event")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val classId = if (spinnerClass.selectedItemPosition > 0) {
                    classes[spinnerClass.selectedItemPosition - 1].id
                } else null

                val event = EventEntity(
                    id = existingEvent?.id ?: 0,
                    title = title,
                    description = editDescription.text.toString().trim(),
                    date = eventDate,
                    startTime = if (checkAllDay.isChecked) null else startTime,
                    endTime = if (checkAllDay.isChecked) null else endTime,
                    classId = classId,
                    eventType = spinnerType.selectedItem.toString(),
                    isAllDay = checkAllDay.isChecked
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (existingEvent == null) {
                            repository.insertEvent(event)
                        } else {
                            repository.updateEvent(event)
                        }
                    }
                    loadEventsForDate(
                        requireView().findViewById(R.id.recyclerEvents),
                        requireView().findViewById(R.id.textNoEvents)
                    )
                    updateCalendar(requireView().findViewById(R.id.textCurrentMonth))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTaskDialog(existingTask: TaskEntity?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task, null)

        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.editTaskTitle)
        val editDescription = dialogView.findViewById<TextInputEditText>(R.id.editTaskDescription)
        val textDueDate = dialogView.findViewById<TextView>(R.id.textTaskDueDate)
        val radioPriority = dialogView.findViewById<RadioGroup>(R.id.radioPriority)
        val spinnerClass = dialogView.findViewById<Spinner>(R.id.spinnerTaskClass)

        val classOptions = listOf("No class") + classes.map { it.name }
        spinnerClass.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classOptions)

        var dueDate: Long? = null

        existingTask?.let {
            editTitle.setText(it.title)
            editDescription.setText(it.description)
            dueDate = it.dueDate
            when (it.priority) {
                "LOW" -> radioPriority.check(R.id.radioLow)
                "HIGH" -> radioPriority.check(R.id.radioHigh)
                else -> radioPriority.check(R.id.radioMedium)
            }
            val classIndex = classes.indexOfFirst { c -> c.id == it.classId }
            spinnerClass.setSelection(if (classIndex >= 0) classIndex + 1 else 0)
        }

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        textDueDate.text = if (dueDate != null) dateFormat.format(Date(dueDate!!)) else "No due date"

        textDueDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (dueDate != null) cal.timeInMillis = dueDate!!
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dueDate = cal.timeInMillis
                textDueDate.text = dateFormat.format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingTask == null) "Add Task" else "Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val priority = when (radioPriority.checkedRadioButtonId) {
                    R.id.radioLow -> "LOW"
                    R.id.radioHigh -> "HIGH"
                    else -> "MEDIUM"
                }

                val classId = if (spinnerClass.selectedItemPosition > 0) {
                    classes[spinnerClass.selectedItemPosition - 1].id
                } else null

                val task = TaskEntity(
                    id = existingTask?.id ?: 0,
                    title = title,
                    description = editDescription.text.toString().trim(),
                    dueDate = dueDate,
                    classId = classId,
                    isCompleted = existingTask?.isCompleted ?: false,
                    priority = priority,
                    createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (existingTask == null) {
                            repository.insertTask(task)
                        } else {
                            repository.updateTask(task)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
