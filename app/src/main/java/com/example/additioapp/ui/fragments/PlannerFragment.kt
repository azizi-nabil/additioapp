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
import com.example.additioapp.data.model.TeacherAbsenceEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.CalendarAdapter
import com.example.additioapp.ui.adapters.CalendarDay
import com.example.additioapp.ui.adapters.EventAdapter
import com.example.additioapp.ui.adapters.ScheduleAdapter
import com.example.additioapp.ui.adapters.TaskAdapter
import com.example.additioapp.ui.adapters.AbsenceAdapter
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    private lateinit var completedTaskAdapter: TaskAdapter
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var absenceAdapter: AbsenceAdapter
    private var classMap: Map<Long, ClassEntity> = emptyMap()

    private var currentMonth = Calendar.getInstance()
    private var selectedDate = Calendar.getInstance()
    private var classes: List<ClassEntity> = emptyList()
    private var currentTab = 0 // 0 = Events, 1 = Tasks, 2 = Schedule, 3 = Absences
    private var selectedScheduleDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    private var searchQuery = ""

    private lateinit var dayNames: List<String>
    private lateinit var fullDayNames: List<String>
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

        dayNames = listOf(
            getString(R.string.calendar_sun), getString(R.string.calendar_mon),
            getString(R.string.calendar_tue), getString(R.string.calendar_wed),
            getString(R.string.calendar_thu), getString(R.string.calendar_fri),
            getString(R.string.calendar_sat)
        )
        fullDayNames = listOf(
            getString(R.string.day_sunday_full), getString(R.string.day_monday_full),
            getString(R.string.day_tuesday_full), getString(R.string.day_wednesday_full),
            getString(R.string.day_thursday_full), getString(R.string.day_friday_full),
            getString(R.string.day_saturday_full)
        )

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val textCurrentMonth = view.findViewById<TextView>(R.id.textCurrentMonth)
        val btnPrevMonth = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val calendarSection = view.findViewById<LinearLayout>(R.id.calendarSection)
        val recyclerCalendar = view.findViewById<RecyclerView>(R.id.recyclerCalendar)
        val eventsView = view.findViewById<LinearLayout>(R.id.eventsView)
        val tasksView = view.findViewById<ScrollView>(R.id.tasksView)
        val scheduleView = view.findViewById<LinearLayout>(R.id.scheduleView)
        val recyclerEvents = view.findViewById<RecyclerView>(R.id.recyclerEvents)
        val recyclerTasks = view.findViewById<RecyclerView>(R.id.recyclerTasks)
        val recyclerSchedule = view.findViewById<RecyclerView>(R.id.recyclerSchedule)
        val textSelectedDate = view.findViewById<TextView>(R.id.textSelectedDate)
        val textNoEvents = view.findViewById<TextView>(R.id.textNoEvents)
        val textNoTasks = view.findViewById<TextView>(R.id.textNoTasks)
        val textNoSchedule = view.findViewById<TextView>(R.id.textNoSchedule)
        val textScheduleDay = view.findViewById<TextView>(R.id.textScheduleDay)
        val chipGroupDays = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupDays)
        val fabAddEvent = view.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fabAddEvent)
        val btnToggleCalendar = view.findViewById<ImageButton>(R.id.btnToggleCalendar)
        val textStatCompleted = view.findViewById<TextView>(R.id.textStatCompleted)
        val textStatThisWeek = view.findViewById<TextView>(R.id.textStatThisWeek)
        val textStatRate = view.findViewById<TextView>(R.id.textStatRate)
        val dayLabels = view.findViewById<LinearLayout>(R.id.dayLabels)
        
        // Completed tasks section
        val completedSection = view.findViewById<LinearLayout>(R.id.completedSection)
        val completedHeader = view.findViewById<LinearLayout>(R.id.completedHeader)
        val textCompletedTitle = view.findViewById<TextView>(R.id.textCompletedTitle)
        val iconExpandCompleted = view.findViewById<ImageView>(R.id.iconExpandCompleted)
        val recyclerCompletedTasks = view.findViewById<RecyclerView>(R.id.recyclerCompletedTasks)
        val btnClearCompleted = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClearCompleted)
        val editSearch = view.findViewById<TextInputEditText>(R.id.editSearch)
        
        val searchLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchLayout)
        searchLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT
        searchLayout.setEndIconOnClickListener {
            editSearch.text?.clear()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(editSearch.windowToken, 0)
            editSearch.clearFocus()
        }
        
        // Calendar visibility state (visible by default)
        var isCalendarVisible = true
        
        fun updateCalendarVisibility() {
            if (isCalendarVisible) {
                recyclerCalendar.visibility = View.VISIBLE
                dayLabels.visibility = View.VISIBLE
                btnToggleCalendar.setImageResource(R.drawable.ic_expand_less)
            } else {
                recyclerCalendar.visibility = View.GONE
                dayLabels.visibility = View.GONE
                btnToggleCalendar.setImageResource(R.drawable.ic_expand_more)
            }
        }
        
        btnToggleCalendar.setOnClickListener {
            isCalendarVisible = !isCalendarVisible
            updateCalendarVisibility()
        }

        val btnToggleFab = view.findViewById<ImageButton>(R.id.btnToggleFab)
        val plannerPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        var isFabVisibleByUser = plannerPrefs.getBoolean("pref_fab_visible_planner", true)
        
        // Helper function to update icon
        fun updateToggleFabIcon() {
            if (isFabVisibleByUser) {
                btnToggleFab.setImageResource(R.drawable.ic_visibility)
                btnToggleFab.alpha = 1.0f
            } else {
                btnToggleFab.setImageResource(R.drawable.ic_visibility_off)
                btnToggleFab.alpha = 0.6f
            }
        }
        
        // Initial state
        if (!isFabVisibleByUser) {
            fabAddEvent.hide()
        }
        updateToggleFabIcon()
        
        btnToggleFab.setOnClickListener {
            isFabVisibleByUser = !isFabVisibleByUser
            plannerPrefs.edit().putBoolean("pref_fab_visible_planner", isFabVisibleByUser).apply()
            if (isFabVisibleByUser) {
                fabAddEvent.show()
            } else {
                fabAddEvent.hide()
            }
            updateToggleFabIcon()
        }

        // Setup tabs with icons
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_calendar_today_24dp).setText("Events"))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_check_circle).setText("Tasks"))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_schedule_24dp).setText("Schedule"))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_swap_horiz).setText("Replace"))

        // Setup day chips for schedule (chipGroupDays is now used internally)
        setupDayButtons(chipGroupDays, textScheduleDay, recyclerSchedule, textNoSchedule)
        
        // Find absences view elements
        val absencesView = view.findViewById<LinearLayout>(R.id.absencesView)
        val recyclerAbsences = view.findViewById<RecyclerView>(R.id.recyclerAbsences)
        val textNoAbsences = view.findViewById<TextView>(R.id.textNoAbsences)
        val textPendingCount = view.findViewById<TextView>(R.id.textPendingCount)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                when (currentTab) {
                    0 -> {
                        calendarSection.visibility = View.VISIBLE
                        eventsView.visibility = View.VISIBLE
                        tasksView.visibility = View.GONE
                        scheduleView.visibility = View.GONE
                        absencesView.visibility = View.GONE
                        fabAddEvent.text = getString(R.string.action_add_event)
                        fabAddEvent.setIconResource(R.drawable.ic_add)
                    }
                    1 -> {
                        calendarSection.visibility = View.VISIBLE
                        eventsView.visibility = View.GONE
                        tasksView.visibility = View.VISIBLE
                        scheduleView.visibility = View.GONE
                        absencesView.visibility = View.GONE
                        loadTasks(recyclerTasks, textNoTasks)
                        loadTaskStatistics()
                        fabAddEvent.text = getString(R.string.action_add_task)
                        fabAddEvent.setIconResource(R.drawable.ic_add)
                    }
                    2 -> {
                        calendarSection.visibility = View.GONE
                        eventsView.visibility = View.GONE
                        tasksView.visibility = View.GONE
                        scheduleView.visibility = View.VISIBLE
                        absencesView.visibility = View.GONE
                        loadScheduleForDay(recyclerSchedule, textNoSchedule, textScheduleDay)
                        fabAddEvent.text = getString(R.string.action_add_schedule)
                        fabAddEvent.setIconResource(R.drawable.ic_add)
                    }
                    3 -> {
                        calendarSection.visibility = View.GONE
                        eventsView.visibility = View.GONE
                        tasksView.visibility = View.GONE
                        scheduleView.visibility = View.GONE
                        absencesView.visibility = View.VISIBLE
                        loadAbsences(recyclerAbsences, textNoAbsences, textPendingCount)
                        fabAddEvent.text = getString(R.string.action_add_absence)
                        fabAddEvent.setIconResource(R.drawable.ic_add)
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
            onDeleteClick = { event -> deleteEvent(event, recyclerEvents, textNoEvents) },
            onLongClick = { event -> showDuplicateEventDialog(event) }
        )
        recyclerEvents.adapter = eventAdapter
        recyclerEvents.layoutManager = LinearLayoutManager(requireContext())

        // Task adapter
        taskAdapter = TaskAdapter(
            onTaskChecked = { task, isChecked -> toggleTaskComplete(task, isChecked, recyclerTasks, textNoTasks) },
            onTaskClick = { task -> showAddTaskDialog(task) },
            onDeleteClick = { task -> deleteTask(task, recyclerTasks, textNoTasks) },
            onLongClick = { task -> showDuplicateTaskDialog(task) }
        )
        recyclerTasks.adapter = taskAdapter
        recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        
        // Swipe actions for tasks (left = delete, right = complete)
        val swipeHandler = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val tasks = taskAdapter.getCurrentList()
                if (position >= 0 && position < tasks.size) {
                    val task = tasks[position]
                    when (direction) {
                        androidx.recyclerview.widget.ItemTouchHelper.LEFT -> {
                            // Delete task
                            deleteTask(task, recyclerTasks, textNoTasks)
                        }
                        androidx.recyclerview.widget.ItemTouchHelper.RIGHT -> {
                            // Complete task
                            toggleTaskComplete(task, true, recyclerTasks, textNoTasks)
                        }
                    }
                }
            }
            
            override fun onChildDraw(c: android.graphics.Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                     dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint()
                val iconMargin = 32 // Margin for icon
                
                if (dX > 0) {
                    // Swipe right - Complete (green)
                    paint.color = Color.parseColor("#4CAF50")
                    c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat(), paint)
                    
                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_24dp)
                    if (icon != null) {
                        icon.setTint(Color.WHITE)
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                } else if (dX < 0) {
                    // Swipe left - Delete (red)
                    paint.color = Color.parseColor("#F44336")
                    c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                    
                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_24dp)
                    if (icon != null) {
                        icon.setTint(Color.WHITE)
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerTasks)
        
        // Completed task adapter
        completedTaskAdapter = TaskAdapter(
            onTaskChecked = { task, isChecked -> toggleTaskComplete(task, isChecked, recyclerTasks, textNoTasks) },
            onTaskClick = { task -> showAddTaskDialog(task) },
            onDeleteClick = { task -> deleteTask(task, recyclerTasks, textNoTasks) }
        )
        recyclerCompletedTasks.adapter = completedTaskAdapter
        recyclerCompletedTasks.layoutManager = LinearLayoutManager(requireContext())
        
        // Completed section expand/collapse
        var isCompletedExpanded = false
        completedHeader.setOnClickListener {
            isCompletedExpanded = !isCompletedExpanded
            recyclerCompletedTasks.visibility = if (isCompletedExpanded) View.VISIBLE else View.GONE
            btnClearCompleted.visibility = if (isCompletedExpanded) View.VISIBLE else View.GONE
            iconExpandCompleted.setImageResource(
                if (isCompletedExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
        
        // Clear completed tasks
        btnClearCompleted.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_clear_completed))
                .setMessage(getString(R.string.msg_clear_completed_confirm))
                .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            repository.clearCompletedTasks()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }

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
                3 -> showAddAbsenceDialog(null, recyclerAbsences, textNoAbsences, textPendingCount)
            }
        }
        
        // Search listener
        editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchQuery = s?.toString()?.lowercase() ?: ""
                when (currentTab) {
                    0 -> loadEventsForDate(recyclerEvents, textNoEvents)
                    1 -> loadTasks(recyclerTasks, textNoTasks)
                }
            }
        })
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
        
        // Handle eventId argument - open edit dialog for the event
        val eventId = arguments?.getLong("eventId", -1L) ?: -1L
        if (eventId > 0) {
            lifecycleScope.launch {
                val event = withContext(Dispatchers.IO) {
                    repository.getEventById(eventId)
                }
                event?.let { showAddEventDialog(it) }
            }
        }
        
        // Handle taskId argument - open edit dialog for the task
        val taskId = arguments?.getLong("taskId", -1L) ?: -1L
        if (taskId > 0) {
            lifecycleScope.launch {
                val task = withContext(Dispatchers.IO) {
                    repository.getTaskById(taskId)
                }
                task?.let { showAddTaskDialog(it) }
            }
        }
    }

    private fun setupDayButtons(chipGroup: com.google.android.material.chip.ChipGroup?, textScheduleDay: TextView, recyclerSchedule: RecyclerView, textNoSchedule: TextView) {
        // Use the passed chipGroup or find it from view
        val group = chipGroup ?: view?.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupDays) ?: return
        group.removeAllViews()
        
        for (i in 0..6) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = dayNames[i]
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        ContextCompat.getColor(requireContext(), R.color.purple_500),
                        ContextCompat.getColor(requireContext(), android.R.color.transparent)
                    )
                )
                setTextColor(android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        Color.WHITE,
                        ContextCompat.getColor(requireContext(), R.color.purple_500)
                    )
                ))
                chipStrokeWidth = 2f
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.purple_200)
                )
                chipCornerRadius = 50f
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedScheduleDay = i + 1
                        loadScheduleForDay(recyclerSchedule, textNoSchedule, textScheduleDay)
                    }
                }
            }
            group.addView(chip)
            
            // Select the current day
            if (i + 1 == selectedScheduleDay) {
                chip.isChecked = true
            }
        }
    }

    private fun updateDayButtonStyles(buttons: List<Button>) {
        // This method is no longer needed with ChipGroup
        // Keeping it for backward compatibility but it does nothing now
    }

    private fun loadScheduleItems() {
        val recyclerSchedule = view?.findViewById<RecyclerView>(R.id.recyclerSchedule)
        val textNoSchedule = view?.findViewById<TextView>(R.id.textNoSchedule)
        val textScheduleDay = view?.findViewById<TextView>(R.id.textScheduleDay)
        
        if (recyclerSchedule != null && textNoSchedule != null && textScheduleDay != null) {
            loadScheduleForDay(recyclerSchedule, textNoSchedule, textScheduleDay)
        }
    }

    private fun loadScheduleForDay(recyclerSchedule: RecyclerView, textNoSchedule: TextView, textScheduleDay: TextView) {
        val dayName = fullDayNames.getOrElse(selectedScheduleDay - 1) { "Monday" }
        textScheduleDay.text = dayName
        
        val calendarDay = selectedScheduleDay
        
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                repository.getScheduleItemsForDaySync(calendarDay)
            }
            
            if (items.isEmpty()) {
                recyclerSchedule.visibility = View.GONE
                textNoSchedule.visibility = View.VISIBLE
            } else {
                recyclerSchedule.visibility = View.VISIBLE
                textNoSchedule.visibility = View.GONE
                scheduleAdapter.submitList(items)
                
                // Fetch class names for items
                val scheduleClassNamesMap = mutableMapOf<Long, List<String>>()
                val classInfoMap = mutableMapOf<Long, Pair<String, String>>()
                    
                items.forEach { item ->
                    val classIds = withContext(Dispatchers.IO) {
                        repository.getClassIdsForScheduleItem(item.id)
                    }
                        
                    if (classIds.isNotEmpty()) {
                            val names = classIds.mapNotNull { id -> classes.find { it.id == id }?.name }
                            if (names.isNotEmpty()) {
                                scheduleClassNamesMap[item.id] = names
                            }
                    }
                        
                    // Ensure primary class info is available for color fallback
                    val primaryClass = classes.find { it.id == item.classId }
                    if (primaryClass != null) {
                        classInfoMap[item.classId] = Pair(primaryClass.name, primaryClass.color)
                    }
                }
                    
                scheduleAdapter.setClassInfo(classInfoMap)
                scheduleAdapter.setScheduleClassNames(scheduleClassNamesMap)
            }
        }
    }

    private fun deleteScheduleItem(item: ScheduleItemEntity, recyclerSchedule: RecyclerView, textNoSchedule: TextView, textScheduleDay: TextView) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_delete_schedule))
            .setMessage(getString(R.string.msg_delete_schedule_confirm))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null)
        
        // Removed validation to ensure classes list is populated. Assuming classes isn't filtered here for brevity or checked inside
        if (classes.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_create_class_first), Toast.LENGTH_LONG).show()
            return
        }

        val btnSelectClasses = dialogView.findViewById<TextView>(R.id.btnSelectClasses)
        val chipGroupClasses = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupClasses)
        val inputDay = dialogView.findViewById<AutoCompleteTextView>(R.id.inputDayOfWeek)
        val editStartTime = dialogView.findViewById<TextInputEditText>(R.id.inputScheduleStartTime)
        val editEndTime = dialogView.findViewById<TextInputEditText>(R.id.inputScheduleEndTime)
        val inputSessionType = dialogView.findViewById<AutoCompleteTextView>(R.id.inputSessionType)
        val editRoom = dialogView.findViewById<TextInputEditText>(R.id.editScheduleRoom)
        // val spinnerClasses = dialogView.findViewById<Spinner>(R.id.spinnerClasses) - Removed/Hidden

        // Multi-class selection state
        val selectedClassIds = mutableListOf<Long>()
        
        fun updateChips() {
            chipGroupClasses.removeAllViews()
            selectedClassIds.forEach { classId ->
                val classEntity = classes.find { it.id == classId }
                if (classEntity != null) {
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = classEntity.name
                        isCloseIconVisible = true
                        setOnCloseIconClickListener {
                            selectedClassIds.remove(classId)
                            updateChips()
                        }
                    }
                    chipGroupClasses.addView(chip)
                }
            }
            btnSelectClasses.text = if (selectedClassIds.isEmpty()) "Tap to select classes..." else "${selectedClassIds.size} class(es) selected"
        }
        
        btnSelectClasses.setOnClickListener {
            val classNames = classes.map { it.name }.toTypedArray()
            val checkedItems = classes.map { selectedClassIds.contains(it.id) }.toBooleanArray()
            
            AlertDialog.Builder(requireContext())
                .setTitle("Select Classes")
                .setMultiChoiceItems(classNames, checkedItems) { _, which, isChecked ->
                    val classId = classes[which].id
                    if (isChecked) {
                        if (!selectedClassIds.contains(classId)) selectedClassIds.add(classId)
                    } else {
                        selectedClassIds.remove(classId)
                    }
                }
                .setPositiveButton("Done") { _, _ -> updateChips() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Initialize state if editing
        if (existingItem != null) {
            editRoom.setText(existingItem.room)
            editStartTime.setText(existingItem.startTime)
            editEndTime.setText(existingItem.endTime)
            
            // Load associated classes
            lifecycleScope.launch {
                val classIds = withContext(Dispatchers.IO) {
                    repository.getClassIdsForScheduleItem(existingItem.id)
                }
                if (classIds.isNotEmpty()) {
                    selectedClassIds.addAll(classIds)
                } else if (existingItem.classId > 0) {
                     // Backward compatibility fallback
                    selectedClassIds.add(existingItem.classId)
                }
                updateChips()
            }
        }

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        inputDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))
        
        if (existingItem != null) {
            val dayIndex = if (existingItem.dayOfWeek > 1) existingItem.dayOfWeek - 2 else 6
            val safeIndex = dayIndex.takeIf { it in 0..6 } ?: 0
            inputDay.setText(days[safeIndex], false)
        } else {
            // Pre-select currently viewed day
            // selectedScheduleDay: 1=Sun, 2=Mon ... 7=Sat
            // days list: 0=Mon ... 6=Sun
            val defaultIndex = if (selectedScheduleDay == Calendar.SUNDAY) 6 else selectedScheduleDay - 2
            val safeDefaultIndex = defaultIndex.takeIf { it in 0..6 } ?: 0
            inputDay.setText(days[safeDefaultIndex], false)
        }

        val sessionTypes = repository.getSessionTypes()
        inputSessionType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sessionTypes))
        if (existingItem != null) {
            inputSessionType.setText(existingItem.sessionType, false)
        } else {
            inputSessionType.setText(sessionTypes[0], false)
        }

        // Time pickers setup (reuse helper method or logic)
        editStartTime.setOnClickListener { 
            showTimePicker(editStartTime) { h, m ->
                // Auto-set end time to start + 1h30
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.add(Calendar.MINUTE, 90)
                
                val endH = cal.get(Calendar.HOUR_OF_DAY)
                val endM = cal.get(Calendar.MINUTE)
                editEndTime.setText(String.format("%02d:%02d", endH, endM))
            } 
        }
        editEndTime.setOnClickListener { showTimePicker(editEndTime) }

        // Dialog creation
        AlertDialog.Builder(requireContext())
            .setTitle(if (existingItem == null) "Add to Schedule" else "Edit Schedule")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (selectedClassIds.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.toast_select_at_least_one_class), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedClassIds.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select at least one class", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Adjust day logic: UI list 0=Mon..6=Sun. Calendar: Sun=1, Mon=2...Sat=7
                val selectedDayName = inputDay.text.toString()
                val selectedDayIndex = days.indexOf(selectedDayName).coerceAtLeast(0)
                
                val calendarDay = if (selectedDayIndex == 6) Calendar.SUNDAY else selectedDayIndex + 2
                
                val primaryClassId = selectedClassIds.first()

                val sessionType = inputSessionType.text.toString()
                repository.addSessionType(sessionType) // Save if new

                val item = ScheduleItemEntity(
                    id = existingItem?.id ?: 0,
                    classId = primaryClassId, // primary class link
                    dayOfWeek = calendarDay,
                    startTime = editStartTime.text.toString(),
                    endTime = editEndTime.text.toString(),
                    room = editRoom.text.toString().trim(),
                    sessionType = sessionType
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (existingItem == null) {
                            repository.insertScheduleItemWithClasses(item, selectedClassIds)
                        } else {
                            repository.updateScheduleItemWithClasses(item, selectedClassIds)
                        }
                    }
                    loadScheduleItems()
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
        
        // Get pending tasks with due dates in this month
        val allTasks = repository.getPendingTasksSync()
        val tasksByDate = allTasks.filter { it.dueDate != null && it.dueDate in monthStart.timeInMillis..monthEnd.timeInMillis }
            .groupBy { task ->
                val taskCal = Calendar.getInstance()
                taskCal.timeInMillis = task.dueDate!!
                taskCal.set(Calendar.HOUR_OF_DAY, 0)
                taskCal.set(Calendar.MINUTE, 0)
                taskCal.set(Calendar.SECOND, 0)
                taskCal.set(Calendar.MILLISECOND, 0)
                taskCal.timeInMillis
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
            val dayTasks = tasksByDate[calendar.timeInMillis] ?: emptyList()
            
            // Combine event colors with task indicator (orange for tasks)
            val colors = mutableListOf<String>()
            
            // Add event colors (up to 2)
            dayEvents.take(2).forEach { event ->
                colors.add(event.color ?: classes.find { it.id == event.classId }?.color ?: getEventTypeColor(event.eventType))
            }
            
            // Add task indicator if there are tasks due (amber/orange color)
            if (dayTasks.isNotEmpty()) {
                colors.add("#FF9800") // Orange for tasks
            }

            days.add(CalendarDay(
                day = day,
                date = calendar.timeInMillis,
                isCurrentMonth = true,
                isToday = calendar.timeInMillis == today.timeInMillis,
                eventColors = colors.take(3) // Max 3 indicators
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
            val allEvents = withContext(Dispatchers.IO) {
                repository.getEventsForDateSync(selectedDate.timeInMillis)
            }
            // Filter by search query
            val events = if (searchQuery.isBlank()) allEvents else allEvents.filter { 
                it.title.lowercase().contains(searchQuery) 
            }
            eventAdapter.submitList(events)
            
            // Load class names for each event
            val eventClassNamesMap = mutableMapOf<Long, List<String>>()
            events.forEach { event ->
                val classIds = withContext(Dispatchers.IO) {
                    repository.getClassIdsForEvent(event.id)
                }
                if (classIds.isNotEmpty()) {
                    val classNamesList = classIds.mapNotNull { classId ->
                        classes.find { it.id == classId }?.name
                    }
                    if (classNamesList.isNotEmpty()) {
                        eventClassNamesMap[event.id] = classNamesList
                    }
                }
            }
            eventAdapter.setEventClassNames(eventClassNamesMap)
            
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
        repository.getPendingTasks().observe(viewLifecycleOwner) { allTasks ->
            // Filter by search query
            val tasks = if (searchQuery.isBlank()) allTasks else allTasks.filter { 
                it.title.lowercase().contains(searchQuery) 
            }
            taskAdapter.submitList(tasks)
            
            // Load class names for each task
            lifecycleScope.launch {
                val taskClassNamesMap = mutableMapOf<Long, List<String>>()
                tasks.forEach { task ->
                    val classIds = withContext(Dispatchers.IO) {
                        repository.getClassIdsForTask(task.id)
                    }
                    if (classIds.isNotEmpty()) {
                        val classNamesList = classIds.mapNotNull { classId ->
                            classes.find { it.id == classId }?.name
                        }
                        if (classNamesList.isNotEmpty()) {
                            taskClassNamesMap[task.id] = classNamesList
                        }
                    }
                }
                taskAdapter.setTaskClassNames(taskClassNamesMap)
            }
            
            if (tasks.isEmpty()) {
                recyclerTasks.visibility = View.GONE
                textNoTasks.visibility = View.VISIBLE
            } else {
                recyclerTasks.visibility = View.VISIBLE
                textNoTasks.visibility = View.GONE
            }
        }
        
        // Observe completed tasks
        repository.getCompletedTasks().observe(viewLifecycleOwner) { completedTasks ->
            completedTaskAdapter.submitList(completedTasks)
            
            // Load class names for completed tasks
            lifecycleScope.launch {
                val taskClassNamesMap = mutableMapOf<Long, List<String>>()
                completedTasks.forEach { task ->
                    val classIds = withContext(Dispatchers.IO) {
                        repository.getClassIdsForTask(task.id)
                    }
                    if (classIds.isNotEmpty()) {
                        val classNamesList = classIds.mapNotNull { classId ->
                            classes.find { it.id == classId }?.name
                        }
                        if (classNamesList.isNotEmpty()) {
                            taskClassNamesMap[task.id] = classNamesList
                        }
                    }
                }
                completedTaskAdapter.setTaskClassNames(taskClassNamesMap)
            }
            
            // Update completed section visibility
            val completedSection = view?.findViewById<LinearLayout>(R.id.completedSection)
            val textCompletedTitle = view?.findViewById<TextView>(R.id.textCompletedTitle)
            if (completedTasks.isEmpty()) {
                completedSection?.visibility = View.GONE
            } else {
                completedSection?.visibility = View.VISIBLE
                textCompletedTitle?.text = "✅ Completed (${completedTasks.size})"
            }
        }
    }
    
    private fun loadTaskStatistics() {
        lifecycleScope.launch {
            val completed = withContext(Dispatchers.IO) { repository.getCompletedTaskCount() }
            val total = withContext(Dispatchers.IO) { repository.getTotalTaskCount() }
            
            // Calculate week start (Monday)
            val weekCal = Calendar.getInstance()
            weekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            weekCal.set(Calendar.HOUR_OF_DAY, 0)
            weekCal.set(Calendar.MINUTE, 0)
            weekCal.set(Calendar.SECOND, 0)
            weekCal.set(Calendar.MILLISECOND, 0)
            
            val thisWeek = withContext(Dispatchers.IO) { repository.getCompletedThisWeek(weekCal.timeInMillis) }
            val rate = if (total > 0) (completed * 100 / total) else 0
            
            view?.findViewById<TextView>(R.id.textStatCompleted)?.text = completed.toString()
            view?.findViewById<TextView>(R.id.textStatThisWeek)?.text = thisWeek.toString()
            view?.findViewById<TextView>(R.id.textStatRate)?.text = "$rate%"
        }
    }

    private fun toggleTaskComplete(task: TaskEntity, completed: Boolean, recyclerTasks: RecyclerView, textNoTasks: TextView) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.setTaskCompleted(task.id, completed)
            }
            loadTaskStatistics()
        }
    }

    private fun deleteEvent(event: EventEntity, recyclerEvents: RecyclerView, textNoEvents: TextView) {
        val isRecurring = event.recurrenceType != "NONE" || event.parentEventId != null
        
        if (isRecurring) {
            val options = arrayOf("Delete this event only", "Delete all future events")
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Recurring Event")
                .setItems(options) { _, which ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            if (which == 0) {
                                // Delete this only
                                repository.deleteEvent(event)
                            } else {
                                // Delete all future
                                val seriesId = event.parentEventId ?: event.id
                                repository.deleteEventSeries(seriesId, event.date)
                            }
                        }
                        loadEventsForDate(recyclerEvents, textNoEvents)
                        updateCalendar(view?.findViewById(R.id.textCurrentMonth)!!)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
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
                    loadTaskStatistics()
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
        val editDate = dialogView.findViewById<TextInputEditText>(R.id.inputEventDate)
        val editStartTime = dialogView.findViewById<TextInputEditText>(R.id.inputStartTime)
        val editEndTime = dialogView.findViewById<TextInputEditText>(R.id.inputEndTime)
        val inputType = dialogView.findViewById<AutoCompleteTextView>(R.id.inputEventType)
        val inputRecurrence = dialogView.findViewById<AutoCompleteTextView>(R.id.inputRecurrence)
        val checkAllDay = dialogView.findViewById<CheckBox>(R.id.checkAllDay)
        val btnSelectClasses = dialogView.findViewById<TextView>(R.id.btnSelectClasses)
        val chipGroupClasses = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupClasses)

        val eventTypes = repository.getEventTypes()
        inputType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, eventTypes))
        
        val recurrenceOptions = listOf(
            getString(R.string.event_recurrence_none), getString(R.string.event_recurrence_daily),
            getString(R.string.event_recurrence_weekly), getString(R.string.event_recurrence_biweekly),
            getString(R.string.event_recurrence_monthly), getString(R.string.event_recurrence_yearly)
        )
        val recurrenceValues = listOf("NONE", "DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "YEARLY")
        inputRecurrence.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recurrenceOptions))

        // Multi-class selection state
        val selectedClassIds = mutableListOf<Long>()
        
        fun updateChips() {
            chipGroupClasses.removeAllViews()
            selectedClassIds.forEach { classId ->
                val classEntity = classes.find { it.id == classId }
                if (classEntity != null) {
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = classEntity.name
                        isCloseIconVisible = true
                        setOnCloseIconClickListener {
                            selectedClassIds.remove(classId)
                            updateChips()
                        }
                    }
                    chipGroupClasses.addView(chip)
                }
            }
            btnSelectClasses.text = if (selectedClassIds.isEmpty()) getString(R.string.schedule_select_classes) else getString(R.string.msg_classes_selected_count, selectedClassIds.size)
        }
        
        btnSelectClasses.setOnClickListener {
            val classNames = classes.map { it.name }.toTypedArray()
            val checkedItems = classes.map { selectedClassIds.contains(it.id) }.toBooleanArray()
            
            AlertDialog.Builder(requireContext())
                .setTitle("Select Classes")
                .setMultiChoiceItems(classNames, checkedItems) { _, which, isChecked ->
                    val classId = classes[which].id
                    if (isChecked) {
                        if (!selectedClassIds.contains(classId)) selectedClassIds.add(classId)
                    } else {
                        selectedClassIds.remove(classId)
                    }
                }
                .setPositiveButton("OK") { _, _ -> updateChips() }
                .setNegativeButton("Cancel", null)
                .show()
        }

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
            
            // Set simple dropdown text instead of selection index
            inputType.setText(it.eventType, false)
            
            // Initial recurrence text
            val recIndex = recurrenceValues.indexOf(it.recurrenceType).coerceAtLeast(0)
            inputRecurrence.setText(recurrenceOptions[recIndex], false)
            
            // Load existing class associations
            lifecycleScope.launch {
                val classIds = withContext(Dispatchers.IO) {
                    repository.getClassIdsForEvent(it.id)
                }
                selectedClassIds.clear()
                selectedClassIds.addAll(classIds)
                // Fallback: if no cross-ref entries but event has legacy classId
                if (selectedClassIds.isEmpty() && it.classId != null) {
                    selectedClassIds.add(it.classId)
                }
                updateChips()
            }
        } ?: run {
             // Defaults for new event
            inputType.setText(eventTypes[0], false)
            inputRecurrence.setText(recurrenceOptions[0], false)
        }

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        editDate.setText(dateFormat.format(Date(eventDate)))
        editStartTime.setText(startTime)
        editEndTime.setText(endTime)

        editDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = eventDate
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                eventDate = cal.timeInMillis
                editDate.setText(dateFormat.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        editStartTime.setOnClickListener {
            val parts = startTime.split(":")
            val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            
            showTimePicker(editStartTime) { h, m ->
                startTime = String.format("%02d:%02d", h, m)
                // editStartTime.setText(startTime) - handled by helper
                
                // Auto-set end time to start + 1h30
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.add(Calendar.MINUTE, 90)
                
                val endH = cal.get(Calendar.HOUR_OF_DAY)
                val endM = cal.get(Calendar.MINUTE)
                endTime = String.format("%02d:%02d", endH, endM)
                editEndTime.setText(endTime)
            }
        }

        editEndTime.setOnClickListener {
             val parts = endTime.split(":")
             val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 10
             val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
             
            showTimePicker(editEndTime) { h, m ->
                endTime = String.format("%02d:%02d", h, m)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingEvent == null) getString(R.string.action_add_event) else getString(R.string.dialog_edit_event))
            .setView(dialogView)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.msg_title_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Keep first classId for backward compatibility, or null if none
                val primaryClassId = selectedClassIds.firstOrNull()
                
                // Get selected recurrence value
                val selectedRecurrenceText = inputRecurrence.text.toString()
                val selectedRecIndex = recurrenceOptions.indexOf(selectedRecurrenceText)
                val selectedRecurrence = if (selectedRecIndex >= 0) recurrenceValues[selectedRecIndex] else "NONE"

                val eventType = inputType.text.toString()
                repository.addEventType(eventType) // Save if new

                val event = EventEntity(
                    id = existingEvent?.id ?: 0,
                    title = title,
                    description = editDescription.text.toString().trim(),
                    date = eventDate,
                    startTime = if (checkAllDay.isChecked) null else startTime,
                    endTime = if (checkAllDay.isChecked) null else endTime,
                    classId = primaryClassId,
                    eventType = eventType,
                    isAllDay = checkAllDay.isChecked,
                    recurrenceType = selectedRecurrence,
                    parentEventId = existingEvent?.parentEventId
                )

                val isSeries = existingEvent != null && (existingEvent.recurrenceType != "NONE" || existingEvent.parentEventId != null)

                if (isSeries) {
                    val options = arrayOf(getString(R.string.option_update_event_only), getString(R.string.option_update_future_events))
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.dialog_update_recurring)
                        .setItems(options) { _, which ->
                             lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    if (which == 0) {
                                        // Update Single
                                        // If we changed recurrence type to/from NONE on a single instance, it detaches from series?
                                        // For simplicity: Update this instance. If it was part of a series, it might stay linked or detach.
                                        // Let's detach it if user chose "This only" and changed recurrence.
                                        // But here we just update.
                                        repository.updateEventWithClasses(event, selectedClassIds)
                                    } else {
                                        // Update Future
                                        // 1. Delete future events of the OLD series from this date
                                        // 1. Delete future events of the OLD series from this date
                                        val oldSeriesId = existingEvent?.parentEventId ?: existingEvent!!.id
                                        repository.deleteEventSeries(oldSeriesId, existingEvent!!.date)
                                        
                                        // 2. Insert this event as a NEW start (or linked, but easiest is new chain if recurrence changes)
                                        // We treat it as a new series start to apply new properties cleanly
                                        val newEvent = event.copy(id = 0, parentEventId = null) 
                                        val insertedId = repository.insertEventWithClasses(newEvent, selectedClassIds)
                                        
                                        // 3. Generate instances
                                        if (newEvent.recurrenceType != "NONE") {
                                            generateRecurringInstances(newEvent.copy(id = insertedId), selectedClassIds)
                                        }
                                    }
                                }
                                loadEventsForDate(requireView().findViewById(R.id.recyclerEvents), requireView().findViewById(R.id.textNoEvents))
                                updateCalendar(requireView().findViewById(R.id.textCurrentMonth))
                             }
                        }
                        .show()
                } else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            if (existingEvent == null) {
                                val insertedId = repository.insertEventWithClasses(event, selectedClassIds)
                                // Generate recurring instances
                                if (selectedRecurrence != "NONE") {
                                    generateRecurringInstances(event.copy(id = insertedId), selectedClassIds)
                                }
                            } else {
                                repository.updateEventWithClasses(event, selectedClassIds)
                            }
                        }
                        loadEventsForDate(requireView().findViewById(R.id.recyclerEvents), requireView().findViewById(R.id.textNoEvents))
                        updateCalendar(requireView().findViewById(R.id.textCurrentMonth))
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showAddTaskDialog(existingTask: TaskEntity?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task, null)

        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.editTaskTitle)
        val editDescription = dialogView.findViewById<TextInputEditText>(R.id.editTaskDescription)
        val editDueDate = dialogView.findViewById<TextInputEditText>(R.id.inputTaskDueDate)
        val radioPriority = dialogView.findViewById<RadioGroup>(R.id.radioPriority)
        val btnSelectClasses = dialogView.findViewById<TextView>(R.id.btnSelectClasses)
        val chipGroupClasses = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupClasses)
        val btnToday = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnToday)
        val btnTomorrow = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTomorrow)
        val btnNextWeek = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNextWeek)

        // Multi-class selection state
        val selectedClassIds = mutableListOf<Long>()
        
        fun updateChips() {
            chipGroupClasses.removeAllViews()
            selectedClassIds.forEach { classId ->
                val classEntity = classes.find { it.id == classId }
                if (classEntity != null) {
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = classEntity.name
                        isCloseIconVisible = true
                        setOnCloseIconClickListener {
                            selectedClassIds.remove(classId)
                            updateChips()
                        }
                    }
                    chipGroupClasses.addView(chip)
                }
            }
            btnSelectClasses.text = if (selectedClassIds.isEmpty()) getString(R.string.schedule_select_classes) else getString(R.string.msg_classes_selected_count, selectedClassIds.size)
        }
        
        btnSelectClasses.setOnClickListener {
            val classNames = classes.map { it.name }.toTypedArray()
            val checkedItems = classes.map { selectedClassIds.contains(it.id) }.toBooleanArray()
            
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_select_classes)
                .setMultiChoiceItems(classNames, checkedItems) { _, which, isChecked ->
                    val classId = classes[which].id
                    if (isChecked) {
                        if (!selectedClassIds.contains(classId)) selectedClassIds.add(classId)
                    } else {
                        selectedClassIds.remove(classId)
                    }
                }
                .setPositiveButton("OK") { _, _ -> updateChips() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

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
            // Load existing class associations
            lifecycleScope.launch {
                val classIds = withContext(Dispatchers.IO) {
                    repository.getClassIdsForTask(it.id)
                }
                selectedClassIds.clear()
                selectedClassIds.addAll(classIds)
                // Fallback: if no cross-ref entries but task has legacy classId
                if (selectedClassIds.isEmpty() && it.classId != null) {
                    selectedClassIds.add(it.classId)
                }
                updateChips()
            }
        }

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        editDueDate.setText(if (dueDate != null) dateFormat.format(Date(dueDate!!)) else getString(R.string.label_no_due_date))

        editDueDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (dueDate != null) cal.timeInMillis = dueDate!!
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dueDate = cal.timeInMillis
                editDueDate.setText(dateFormat.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        
        // Quick date shortcuts
        btnToday.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            dueDate = cal.timeInMillis
            editDueDate.setText(dateFormat.format(cal.time))
        }
        
        btnTomorrow.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            dueDate = cal.timeInMillis
            editDueDate.setText(dateFormat.format(cal.time))
        }
        
        btnNextWeek.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            dueDate = cal.timeInMillis
            editDueDate.setText(dateFormat.format(cal.time))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (existingTask == null) getString(R.string.action_add_task) else getString(R.string.dialog_edit_task))
            .setView(dialogView)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.msg_title_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }


                val priority = when (radioPriority.checkedRadioButtonId) {
                    R.id.radioLow -> "LOW"
                    R.id.radioHigh -> "HIGH"
                    else -> "MEDIUM"
                }

                // Keep first classId for backward compatibility, or null if none
                val primaryClassId = selectedClassIds.firstOrNull()

                val task = TaskEntity(
                    id = existingTask?.id ?: 0,
                    title = title,
                    description = editDescription.text.toString().trim(),
                    dueDate = dueDate,
                    classId = primaryClassId,
                    isCompleted = existingTask?.isCompleted ?: false,
                    priority = priority,
                    createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (existingTask == null) {
                            repository.insertTaskWithClasses(task, selectedClassIds)
                        } else {
                            repository.updateTaskWithClasses(task, selectedClassIds)
                        }
                    }
                    loadTaskStatistics()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
    
    private fun showDuplicateTaskDialog(task: TaskEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_duplicate_task)
            .setMessage(getString(R.string.msg_duplicate_task_confirm, task.title))
            .setPositiveButton(R.string.action_duplicate) { _, _ ->
                lifecycleScope.launch {
                    // Get class IDs for the original task
                    val classIds = withContext(Dispatchers.IO) {
                        repository.getClassIdsForTask(task.id)
                    }
                    
                    // Create a copy with "Copy of" prefix
                    val copy = task.copy(
                        id = 0,
                        title = getString(R.string.prefix_copy_of, task.title),
                        isCompleted = false,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    withContext(Dispatchers.IO) {
                        repository.insertTaskWithClasses(copy, classIds)
                    }
                    
                    Toast.makeText(requireContext(), getString(R.string.msg_task_duplicated), Toast.LENGTH_SHORT).show()
                    loadTaskStatistics()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
    
    private suspend fun generateRecurringInstances(parentEvent: EventEntity, classIds: List<Long>) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = parentEvent.date
        
        // Generate instances for 6 months ahead
        val endCal = Calendar.getInstance()
        endCal.add(Calendar.MONTH, 6)
        val maxDate = endCal.timeInMillis
        
        val instances = mutableListOf<EventEntity>()
        
        while (true) {
            // Move to next occurrence
            when (parentEvent.recurrenceType) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "BIWEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 2)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                else -> break
            }
            
            if (cal.timeInMillis > maxDate) break
            
            val instance = parentEvent.copy(
                id = 0,
                date = cal.timeInMillis,
                parentEventId = parentEvent.id
            )
            instances.add(instance)
        }
        
        // Insert all instances
        instances.forEach { instance ->
            repository.insertEventWithClasses(instance, classIds)
        }
    }


    private fun showDuplicateEventDialog(event: EventEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_duplicate_event)
            .setMessage(getString(R.string.msg_duplicate_event_confirm, event.title))
            .setPositiveButton(R.string.action_duplicate) { _, _ ->
                lifecycleScope.launch {
                    val copy = event.copy(
                        id = 0,
                        title = getString(R.string.prefix_copy_of, event.title),
                        parentEventId = null // Detach from recurrence series if copied
                    )
                    
                    withContext(Dispatchers.IO) {
                        repository.insertEvent(copy)
                    }
                    
                    Toast.makeText(requireContext(), getString(R.string.msg_event_duplicated), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(editText: TextInputEditText, onTimeSelected: ((Int, Int) -> Unit)? = null) {
        val currentContext = context ?: return
        val parts = editText.text.toString().split(":")
        val hour = if (parts.size == 2) parts[0].toIntOrNull() ?: 9 else 9
        val minute = if (parts.size == 2) parts[1].toIntOrNull() ?: 0 else 0

        android.app.TimePickerDialog(currentContext, { _, h, m ->
            editText.setText(String.format("%02d:%02d", h, m))
            onTimeSelected?.invoke(h, m)
        }, hour, minute, true).show()
    }
    
    // ===== TEACHER ABSENCE FUNCTIONS =====
    
    private fun loadAbsences(recyclerView: RecyclerView, textNoAbsences: TextView, textPendingCount: TextView) {
        val view = requireView()
        val completedSection = view.findViewById<LinearLayout>(R.id.completedAbsencesSection)
        val completedHeader = view.findViewById<LinearLayout>(R.id.completedAbsencesHeader)
        val recyclerCompleted = view.findViewById<RecyclerView>(R.id.recyclerCompletedAbsences)
        val textCompletedTitle = view.findViewById<TextView>(R.id.textCompletedAbsencesTitle)
        val iconExpand = view.findViewById<ImageView>(R.id.iconExpandCompletedAbsences)
        
        var isCompletedExpanded = false
        
        // Toggle completed section
        completedHeader.setOnClickListener {
            isCompletedExpanded = !isCompletedExpanded
            recyclerCompleted.visibility = if (isCompletedExpanded) View.VISIBLE else View.GONE
            iconExpand.setImageResource(if (isCompletedExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
        }
        
        lifecycleScope.launch {
            // Update class map for display
            classViewModel.allClasses.observe(viewLifecycleOwner) { classList ->
                classes = classList
                classMap = classList.associateBy { it.id }
                
                // Initialize adapters if needed
                if (!::absenceAdapter.isInitialized) {
                    absenceAdapter = AbsenceAdapter(
                        classMap = classMap,
                        onScheduleClick = { absence -> showScheduleReplacementDialog(absence, recyclerView, textNoAbsences, textPendingCount) },
                        onCompleteClick = { absence -> markAbsenceCompleted(absence, recyclerView, textNoAbsences, textPendingCount) },
                        onDeleteClick = { absence -> deleteAbsence(absence, recyclerView, textNoAbsences, textPendingCount) },
                        onItemClick = { absence -> showAddAbsenceDialog(absence, recyclerView, textNoAbsences, textPendingCount) }
                    )
                    recyclerView.adapter = absenceAdapter
                    recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    
                    // Swipe gestures for pending/scheduled absences
                    val pendingSwipeCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                        0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
                    ) {
                        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
                        
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            val position = viewHolder.adapterPosition
                            if (position >= 0 && position < absenceAdapter.currentList.size) {
                                val absence = absenceAdapter.currentList[position]
                                when (direction) {
                                    androidx.recyclerview.widget.ItemTouchHelper.LEFT -> {
                                        deleteAbsence(absence, recyclerView, textNoAbsences, textPendingCount)
                                    }
                                    androidx.recyclerview.widget.ItemTouchHelper.RIGHT -> {
                                        // Complete if scheduled, schedule if pending
                                        if (absence.status == TeacherAbsenceEntity.STATUS_SCHEDULED) {
                                            markAbsenceCompleted(absence, recyclerView, textNoAbsences, textPendingCount)
                                        } else {
                                            showScheduleReplacementDialog(absence, recyclerView, textNoAbsences, textPendingCount)
                                        }
                                    }
                                }
                            }
                        }
                        
                        override fun onChildDraw(c: android.graphics.Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                                 dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                            val itemView = viewHolder.itemView
                            val paint = android.graphics.Paint()
                            val iconMargin = 32
                            
                            if (dX > 0) {
                                // Swipe right - Complete/Schedule (green)
                                paint.color = Color.parseColor("#4CAF50")
                                c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat(), paint)
                                val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                                if (icon != null) {
                                    icon.setTint(Color.WHITE)
                                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                                    val iconBottom = iconTop + icon.intrinsicHeight
                                    val iconLeft = itemView.left + iconMargin
                                    val iconRight = iconLeft + icon.intrinsicWidth
                                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                    icon.draw(c)
                                }
                            } else if (dX < 0) {
                                // Swipe left - Delete (red)
                                paint.color = Color.parseColor("#F44336")
                                c.drawRect(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                                val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                                if (icon != null) {
                                    icon.setTint(Color.WHITE)
                                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                                    val iconBottom = iconTop + icon.intrinsicHeight
                                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                                    val iconRight = itemView.right - iconMargin
                                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                    icon.draw(c)
                                }
                            }
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        }
                    }
                    androidx.recyclerview.widget.ItemTouchHelper(pendingSwipeCallback).attachToRecyclerView(recyclerView)
                    
                    // Completed adapter
                    val completedAdapter = AbsenceAdapter(
                        classMap = classMap,
                        onScheduleClick = { absence -> showScheduleReplacementDialog(absence, recyclerView, textNoAbsences, textPendingCount) },
                        onCompleteClick = { absence -> markAbsenceCompleted(absence, recyclerView, textNoAbsences, textPendingCount) },
                        onDeleteClick = { absence -> deleteAbsence(absence, recyclerView, textNoAbsences, textPendingCount) },
                        onItemClick = { absence -> showAddAbsenceDialog(absence, recyclerView, textNoAbsences, textPendingCount) }
                    )
                    recyclerCompleted.adapter = completedAdapter
                    recyclerCompleted.layoutManager = LinearLayoutManager(requireContext())
                    
                    // Swipe gestures for completed absences (swipe left to delete, right to revert to scheduled)
                    val completedSwipeCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                        0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
                    ) {
                        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
                        
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            val position = viewHolder.adapterPosition
                            if (position >= 0 && position < completedAdapter.currentList.size) {
                                val absence = completedAdapter.currentList[position]
                                when (direction) {
                                    androidx.recyclerview.widget.ItemTouchHelper.LEFT -> {
                                        deleteAbsence(absence, recyclerView, textNoAbsences, textPendingCount)
                                    }
                                    androidx.recyclerview.widget.ItemTouchHelper.RIGHT -> {
                                        // Revert to scheduled
                                        showScheduleReplacementDialog(absence, recyclerView, textNoAbsences, textPendingCount)
                                    }
                                }
                            }
                        }
                        
                        override fun onChildDraw(c: android.graphics.Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                                 dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                            val itemView = viewHolder.itemView
                            val paint = android.graphics.Paint()
                            val iconMargin = 32
                            
                            if (dX > 0) {
                                // Swipe right - Revert to scheduled (orange)
                                paint.color = Color.parseColor("#FF9800")
                                c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat(), paint)
                                val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_swap_horiz)
                                if (icon != null) {
                                    icon.setTint(Color.WHITE)
                                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                                    val iconBottom = iconTop + icon.intrinsicHeight
                                    val iconLeft = itemView.left + iconMargin
                                    val iconRight = iconLeft + icon.intrinsicWidth
                                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                    icon.draw(c)
                                }
                            } else if (dX < 0) {
                                // Swipe left - Delete (red)
                                paint.color = Color.parseColor("#F44336")
                                c.drawRect(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                                val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                                if (icon != null) {
                                    icon.setTint(Color.WHITE)
                                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                                    val iconBottom = iconTop + icon.intrinsicHeight
                                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                                    val iconRight = itemView.right - iconMargin
                                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                    icon.draw(c)
                                }
                            }
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        }
                    }
                    androidx.recyclerview.widget.ItemTouchHelper(completedSwipeCallback).attachToRecyclerView(recyclerCompleted)
                }
            }
            
            // Observe absences - split by status
            repository.allAbsences.collect { allAbsences ->
                val pendingOrScheduled = allAbsences.filter { it.status != TeacherAbsenceEntity.STATUS_COMPLETED }
                val completed = allAbsences.filter { it.status == TeacherAbsenceEntity.STATUS_COMPLETED }
                
                // Pending/Scheduled list
                if (pendingOrScheduled.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    textNoAbsences.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    textNoAbsences.visibility = View.GONE
                }
                absenceAdapter.submitList(pendingOrScheduled)
                
                // Completed list
                if (completed.isNotEmpty()) {
                    completedSection.visibility = View.VISIBLE
                    textCompletedTitle.text = getString(R.string.absence_completed_count, completed.size)
                    (recyclerCompleted.adapter as? AbsenceAdapter)?.submitList(completed)
                } else {
                    completedSection.visibility = View.GONE
                }
                
                // Update pending count
                val pendingCount = pendingOrScheduled.count { it.status == TeacherAbsenceEntity.STATUS_PENDING }
                textPendingCount.text = if (pendingCount > 0) {
                    getString(R.string.absence_pending_count, pendingCount)
                } else {
                    getString(R.string.absence_no_pending)
                }
            }
        }
    }
    
    private fun showAddAbsenceDialog(
        existingAbsence: TeacherAbsenceEntity?,
        recyclerView: RecyclerView,
        textNoAbsences: TextView,
        textPendingCount: TextView
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_absence, null)
        
        val chipGroupClasses = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupClasses)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupSessionType)
        val radioTD = dialogView.findViewById<RadioButton>(R.id.radioTD)
        val radioTP = dialogView.findViewById<RadioButton>(R.id.radioTP)
        val radioCourse = dialogView.findViewById<RadioButton>(R.id.radioCourse)
        val editAbsenceDate = dialogView.findViewById<TextInputEditText>(R.id.editAbsenceDate)
        val editReplacementDate = dialogView.findViewById<TextInputEditText>(R.id.editReplacementDate)
        val editReason = dialogView.findViewById<TextInputEditText>(R.id.editReason)
        val editNotes = dialogView.findViewById<TextInputEditText>(R.id.editNotes)
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var selectedAbsenceDate: Long? = existingAbsence?.absenceDate
        var selectedReplacementDate: Long? = existingAbsence?.replacementDate
        val selectedClassIds = mutableSetOf<Long>()
        
        // Pre-select existing classes
        existingAbsence?.getClassIdList()?.let { selectedClassIds.addAll(it) }
        
        // Populate class chips
        classes.forEach { classEntity ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = classEntity.name
                isCheckable = true
                isChecked = selectedClassIds.contains(classEntity.id)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedClassIds.add(classEntity.id)
                    } else {
                        selectedClassIds.remove(classEntity.id)
                    }
                }
            }
            chipGroupClasses.addView(chip)
        }
        
        // Set existing values
        existingAbsence?.let { absence ->
            // Set session type
            when (absence.sessionType) {
                TeacherAbsenceEntity.TYPE_TD -> radioTD.isChecked = true
                TeacherAbsenceEntity.TYPE_TP -> radioTP.isChecked = true
                else -> radioCourse.isChecked = true
            }
            
            // Set dates
            editAbsenceDate.setText(dateFormat.format(Date(absence.absenceDate)))
            absence.replacementDate?.let {
                editReplacementDate.setText(dateFormat.format(Date(it)))
            }
            editReason.setText(absence.reason ?: "")
            editNotes.setText(absence.notes ?: "")
        }
        
        // Date pickers
        editAbsenceDate.setOnClickListener {
            showDatePickerForAbsence { date ->
                selectedAbsenceDate = date
                editAbsenceDate.setText(dateFormat.format(Date(date)))
            }
        }
        
        editReplacementDate.setOnClickListener {
            showDatePickerForAbsence { date ->
                selectedReplacementDate = date
                editReplacementDate.setText(dateFormat.format(Date(date)))
            }
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (existingAbsence == null) R.string.absence_record_title else R.string.action_edit)
            .setView(dialogView)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (selectedClassIds.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.toast_select_class_first), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val absenceDate = selectedAbsenceDate ?: run {
                    Toast.makeText(requireContext(), getString(R.string.absence_date), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val sessionType = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioTD -> TeacherAbsenceEntity.TYPE_TD
                    R.id.radioTP -> TeacherAbsenceEntity.TYPE_TP
                    else -> TeacherAbsenceEntity.TYPE_COURSE
                }
                
                val status = when {
                    selectedReplacementDate != null && existingAbsence?.status == TeacherAbsenceEntity.STATUS_COMPLETED -> TeacherAbsenceEntity.STATUS_COMPLETED
                    selectedReplacementDate != null -> TeacherAbsenceEntity.STATUS_SCHEDULED
                    else -> TeacherAbsenceEntity.STATUS_PENDING
                }
                
                val absence = TeacherAbsenceEntity(
                    id = existingAbsence?.id ?: 0,
                    classIds = TeacherAbsenceEntity.createClassIdsString(selectedClassIds.toList()),
                    sessionType = sessionType,
                    absenceDate = absenceDate,
                    reason = editReason.text.toString().takeIf { it.isNotBlank() },
                    replacementDate = selectedReplacementDate,
                    status = status,
                    notes = editNotes.text.toString().takeIf { it.isNotBlank() },
                    createdAt = existingAbsence?.createdAt ?: System.currentTimeMillis()
                )
                
                lifecycleScope.launch {
                    if (existingAbsence == null) {
                        repository.insertAbsence(absence)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), getString(R.string.toast_absence_added), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        repository.updateAbsence(absence)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), getString(R.string.toast_absence_updated), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showScheduleReplacementDialog(
        absence: TeacherAbsenceEntity,
        recyclerView: RecyclerView,
        textNoAbsences: TextView,
        textPendingCount: TextView
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var selectedDate: Long? = null
        
        showDatePickerForAbsence { date ->
            selectedDate = date
            
            lifecycleScope.launch {
                repository.scheduleReplacement(absence.id, date)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.toast_replacement_scheduled), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun markAbsenceCompleted(
        absence: TeacherAbsenceEntity,
        recyclerView: RecyclerView,
        textNoAbsences: TextView,
        textPendingCount: TextView
    ) {
        lifecycleScope.launch {
            repository.markAbsenceCompleted(absence.id)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), getString(R.string.toast_marked_completed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteAbsence(
        absence: TeacherAbsenceEntity,
        recyclerView: RecyclerView,
        textNoAbsences: TextView,
        textPendingCount: TextView
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(getString(R.string.toast_absence_deleted) + "?")
            .setPositiveButton(R.string.action_delete) { dialog, which ->
                lifecycleScope.launch {
                    repository.deleteAbsence(absence)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.toast_absence_deleted), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
    
    private fun showDatePickerForAbsence(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
