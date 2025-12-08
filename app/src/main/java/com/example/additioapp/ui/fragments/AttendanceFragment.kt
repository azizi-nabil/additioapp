package com.example.additioapp.ui.fragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.AttendanceRecordEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.AttendanceAdapter
import com.example.additioapp.ui.adapters.StudentAttendanceItem
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import com.example.additioapp.ui.viewmodel.AttendanceStatusViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceFragment : Fragment() {

    private var classId: Long = -1
    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val statusViewModel: AttendanceStatusViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var selectedDate: Calendar = Calendar.getInstance()
    private lateinit var adapter: AttendanceAdapter
    private var cachedStatuses: List<com.example.additioapp.data.model.AttendanceStatusEntity> = emptyList()
    private var currentStudents: List<com.example.additioapp.data.model.StudentEntity> = emptyList()
    private var summaryTextView: TextView? = null
    private var currentSessionType: String = "Cours"
    private var isSessionTypeReady = false
    private var originalSessionId: String? = null // For legacy 2-part sessionIds
    private var currentFilter: String? = null // null means "All", otherwise P/A/L/E

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
            android.util.Log.d("AttendanceFragment", "onCreate: Received classId $classId")
            val dateMillis = it.getLong("selectedDate", -1L)
            if (dateMillis != -1L) {
                selectedDate.timeInMillis = dateMillis
            }
            // If sessionId is passed, parse the session type from it
            val passedSessionId = it.getString("sessionId")
            if (!passedSessionId.isNullOrEmpty()) {
                // SessionId format can be:
                // - Legacy: classId_date (2 parts) - type needs to be loaded from DB
                // - New: classId_date_type (3 parts) - type is in the ID
                val parts = passedSessionId.split("_")
                if (parts.size >= 3) {
                    // New format: extract type from sessionId
                    currentSessionType = parts.last()
                } else {
                    // Legacy 2-part format - store for use in queries
                    originalSessionId = passedSessionId
                }
                
                // Disable auto-fill for existing sessions opened from history
                shouldAutoFill = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Force refresh when returning to fragment (e.g., after switching session types or adding attendance)
        if (currentStudents.isNotEmpty()) {
            summaryTextView?.let { refreshData(currentStudents, it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textDate = view.findViewById<TextView>(R.id.textDate)
        val btnChangeDate = view.findViewById<Button>(R.id.btnChangeDate)
        val spinnerNature = view.findViewById<android.widget.Spinner>(R.id.spinnerSessionNature)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAttendance)
        // val containerStatusButtons = view.findViewById<LinearLayout>(R.id.containerStatusButtons) // Removed
        val btnBulkPresent = view.findViewById<Button>(R.id.btnBulkPresent)
        val btnBulkAbsent = view.findViewById<Button>(R.id.btnBulkAbsent)
        val textSummary = view.findViewById<TextView>(R.id.textSummary)
        val btnClearAll = view.findViewById<Button>(R.id.btnClearAll)
        val btnLockToggle = view.findViewById<MaterialButton>(R.id.btnLockToggle)

        summaryTextView = textSummary

        updateDateDisplay(textDate)
        updateLockState(view)
        
        // Load session type from database
        val passedSessionId = arguments?.getString("sessionId")
        val isLegacySessionId = passedSessionId != null && passedSessionId.split("_").size == 2
        
        lifecycleScope.launch {
            if (passedSessionId.isNullOrEmpty()) {
                // New session - load from database
                attendanceViewModel.loadSessionType(classId, selectedDate.timeInMillis)
                currentSessionType = attendanceViewModel.currentSessionType.value ?: "Cours"
            } else if (isLegacySessionId) {
                // Legacy 2-part sessionId - load type from sessions table
                attendanceViewModel.loadSessionType(classId, selectedDate.timeInMillis)
                currentSessionType = attendanceViewModel.currentSessionType.value ?: "Cours"
            } else {
                // New 3-part sessionId - type was already parsed in onCreate
                attendanceViewModel.setSessionType(currentSessionType)
            }
            
            // Mark session type as ready and trigger refresh if students are loaded
            isSessionTypeReady = true
            if (currentStudents.isNotEmpty()) {
                summaryTextView?.let { refreshData(currentStudents, it) }
            }
        }
        
        // Setup Spinner - use raw values for data, localized strings for display
        val sessionTypeValues = listOf("Cours", "TD", "TP") // Raw values stored in DB
        val sessionTypeLabels = listOf(getString(R.string.session_type_course), getString(R.string.session_type_td), getString(R.string.session_type_tp)) // Display labels
        val spinnerAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sessionTypeLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNature.adapter = spinnerAdapter
        spinnerNature.isEnabled = !isLocked

        // Observe Session Type - match against raw values
        attendanceViewModel.currentSessionType.observe(viewLifecycleOwner) { type ->
            currentSessionType = type // Sync local variable
            val index = sessionTypeValues.indexOf(type)
            if (index >= 0 && spinnerNature.selectedItemPosition != index) {
                spinnerNature.setSelection(index)
            }
        }

        // Handle Spinner Selection - use raw values
        spinnerNature.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isLocked) return
                val selectedType = sessionTypeValues[position] // Use raw value, not label
                if (currentSessionType != selectedType) {
                    val oldSessionType = currentSessionType
                    currentSessionType = selectedType
                    lifecycleScope.launch {
                        // Build old and new session IDs
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dateStr = format.format(selectedDate.time)
                        val oldSessionId = "${classId}_${dateStr}_${oldSessionType}"
                        val newSessionId = "${classId}_${dateStr}_${selectedType}"
                        
                        // Migrate attendance records from old session ID to new session ID
                        attendanceViewModel.updateSessionId(oldSessionId, newSessionId)
                        
                        // Save the new session type
                        attendanceViewModel.saveSessionType(classId, selectedDate.timeInMillis, selectedType)
                        
                        // Reload data for the new session type
                        summaryTextView?.let { refreshData(currentStudents, it) }
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        adapter = AttendanceAdapter { student, currentStatus ->
            if (isLocked) {
                Toast.makeText(requireContext(), getString(R.string.msg_unlock_to_modify), Toast.LENGTH_SHORT).show()
                return@AttendanceAdapter
            }
            // Show bottom sheet dialog
            lifecycleScope.launch {
                val sessionId = getSessionId(selectedDate)
                val records = withContext(Dispatchers.IO) {
                    attendanceViewModel.getAttendanceForSessionOnce(sessionId)
                }
                val currentRecord = records.find { it.studentId == student.id }
                
                val dialog = com.example.additioapp.ui.dialogs.AttendanceBottomSheetDialog(
                    student = student,
                    currentRecord = currentRecord,
                    onSave = { status, comment ->
                        val record = com.example.additioapp.data.model.AttendanceRecordEntity(
                            studentId = student.id,
                            sessionId = sessionId,
                            date = selectedDate.timeInMillis, // Use selectedDate.timeInMillis for consistency
                            status = status,
                            comment = comment
                        )
                        lifecycleScope.launch {
                            attendanceViewModel.setAttendance(record)
                            // Also ensure session type is saved (idempotent)
                            val currentType = attendanceViewModel.currentSessionType.value ?: "Cours"
                            attendanceViewModel.saveSessionType(classId, selectedDate.timeInMillis, currentType)
                            
                            // Manually refresh to show update immediately (since we disabled observer)
                            summaryTextView?.let { refreshData(currentStudents, it) }
                        }
                    }
                )
                dialog.show(parentFragmentManager, "AttendanceBottomSheet")
            }
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        statusViewModel.statuses.observe(viewLifecycleOwner) { statuses ->
            cachedStatuses = statuses
            adapter.setStatuses(statuses)
            // renderStatusButtons(containerStatusButtons, statuses) // Removed to clean up UI
        }

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            android.util.Log.d("AttendanceFragment", "Observer: Received ${students.size} students for classId $classId")
            currentStudents = students
            // Only refresh if session type is ready to avoid race condition
            if (isSessionTypeReady) {
                refreshData(students, textSummary)
            }
        }

        btnBulkPresent.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showConfirmationDialog(getString(R.string.title_mark_all_present), getString(R.string.msg_mark_all_present)) {
                applyBulkStatus("P")
            }
        }

        btnBulkAbsent.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showConfirmationDialog(getString(R.string.title_mark_all_absent), getString(R.string.msg_mark_all_absent)) {
                applyBulkStatus("A")
            }
        }

        btnClearAll.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showConfirmationDialog(getString(R.string.title_clear_attendance), getString(R.string.msg_clear_attendance)) {
                clearAll()
            }
        }
        
        btnLockToggle.setOnClickListener {
            isLocked = !isLocked
            updateLockState(view)
        }

        btnChangeDate.setOnClickListener {
            showDatePicker(textDate)
        }

        // Setup filter chips
        val chipGroupFilter = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFilter)
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipFilterPresent) -> "P"
                checkedIds.contains(R.id.chipFilterAbsent) -> "A"
                checkedIds.contains(R.id.chipFilterLate) -> "L"
                checkedIds.contains(R.id.chipFilterExcused) -> "E"
                else -> null // All
            }
            // Re-apply filter to current items
            if (currentStudents.isNotEmpty()) {
                summaryTextView?.let { refreshData(currentStudents, it) }
            }
        }
    }

    private fun updateDateDisplay(textView: TextView) {
        val format = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        textView.text = format.format(selectedDate.time)
    }

    private fun getSessionId(calendar: Calendar): String {
        // For legacy 2-part sessionIds, use the original format
        originalSessionId?.let { return it }
        
        // New format: classId_date_type
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = format.format(calendar.time)
        return "${classId}_${dateStr}_$currentSessionType"
    }

    private fun showDatePicker(textView: TextView) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                shouldAutoFill = true // Reset auto-fill for new date
                updateDateDisplay(textView)
                // Clear the passed sessionId since we're now on a different date
                arguments?.remove("sessionId")
                originalSessionId = null // Clear legacy sessionId
                // Load session type from database for the new date, THEN refresh
                lifecycleScope.launch {
                    attendanceViewModel.loadSessionType(classId, selectedDate.timeInMillis)
                    // Now that type is loaded, sync the local variable
                    currentSessionType = attendanceViewModel.currentSessionType.value ?: "Cours"
                    // Now refresh with correct session type
                    summaryTextView?.let { refreshData(currentStudents, it) }
                }
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }



    private var refreshJob: kotlinx.coroutines.Job? = null

    private var shouldAutoFill = true

    private fun refreshData(students: List<com.example.additioapp.data.model.StudentEntity>, summaryText: TextView) {
        currentStudents = students
        val sessionId = getSessionId(selectedDate)
        
        // Cancel previous job to prevent race conditions
        refreshJob?.cancel()
        
        // Load attendance records ONCE - don't observe for changes
        refreshJob = lifecycleScope.launch {
            var records = withContext(Dispatchers.IO) {
                attendanceViewModel.getAttendanceForSessionOnce(sessionId)
            }
            
            android.util.Log.d("AttendanceFragment", "Loaded ${records.size} records from DB for session $sessionId")
            
            if (students.isEmpty()) {
                 android.util.Log.w("AttendanceFragment", "Warning: Student list is empty for classId $classId")
            }

            // Auto-fill "Present" if records are empty and we should auto-fill
            if (records.isEmpty() && shouldAutoFill && students.isNotEmpty()) {
                android.util.Log.d("AttendanceFragment", "Auto-filling Present for new session")
                // Apply "P" to all students
                records = students.map { student ->
                    AttendanceRecordEntity(
                        studentId = student.id,
                        sessionId = sessionId,
                        date = selectedDate.timeInMillis,
                        status = "P"
                    )
                }
                // Save to DB in background using BATCH INSERT
                val recordsToSave = records
                launch(Dispatchers.IO) {
                    attendanceViewModel.insertAttendanceList(recordsToSave)
                    // Ensure session is created with current type
                    attendanceViewModel.saveSessionType(classId, selectedDate.timeInMillis, currentSessionType)
                }
            }

            // Use HashMap for O(1) lookup instead of O(n) find
            val recordMap = records.associateBy { it.studentId }
            
            val allItems = students.map { student ->
                StudentAttendanceItem(student, recordMap[student.id])
            }
            
            // Apply filter if set
            val filteredItems = if (currentFilter != null) {
                allItems.filter { it.attendance?.status == currentFilter }
            } else {
                allItems
            }
            
            adapter.submitList(filteredItems)
            updateSummary(allItems, summaryText) // Summary shows totals, not filtered
        }
    }

    private fun updateSummary(items: List<StudentAttendanceItem>, summaryText: TextView) {
        val counts = items.groupingBy { it.attendance?.status ?: "-" }.eachCount()
        val present = counts["P"] ?: 0
        val absent = counts["A"] ?: 0
        val late = counts["L"] ?: 0
        val excused = counts["E"] ?: 0
        
        summaryText.text = getString(R.string.attendance_summary, present, absent, late, excused)
    }

    private fun applyBulkStatus(code: String) {
        val statuses = cachedStatuses
        if (statuses.none { it.code == code }) return
        val sessionId = getSessionId(selectedDate)
        
        lifecycleScope.launch {
            currentStudents.forEach { student ->
                val record = AttendanceRecordEntity(
                    studentId = student.id,
                    sessionId = sessionId,
                    date = selectedDate.timeInMillis,
                    status = code
                )
                attendanceViewModel.setAttendance(record)
            }
            summaryTextView?.let { refreshData(currentStudents, it) }
        }
    }

    private fun clearAll() {
        val sessionId = getSessionId(selectedDate)
        shouldAutoFill = false // Prevent auto-fill after clearing
        lifecycleScope.launch {
            attendanceViewModel.deleteSession(sessionId)
            summaryTextView?.let { refreshData(currentStudents, it) }
        }
    }



    private var isLocked = true

    private fun updateLockState(view: View) {
        val btnLockToggle = view.findViewById<MaterialButton>(R.id.btnLockToggle)
        val spinnerNature = view.findViewById<android.widget.Spinner>(R.id.spinnerSessionNature)
        val btnBulkPresent = view.findViewById<Button>(R.id.btnBulkPresent)
        val btnBulkAbsent = view.findViewById<Button>(R.id.btnBulkAbsent)
        val btnClearAll = view.findViewById<Button>(R.id.btnClearAll)
        
        if (isLocked) {
            btnLockToggle.setIconResource(R.drawable.ic_lock)
            spinnerNature.isEnabled = false
            btnBulkPresent.isEnabled = false
            btnBulkAbsent.isEnabled = false
            btnClearAll.isEnabled = false
            btnBulkPresent.alpha = 0.5f
            btnBulkAbsent.alpha = 0.5f
            btnClearAll.alpha = 0.5f
        } else {
            btnLockToggle.setIconResource(R.drawable.ic_lock_open)
            spinnerNature.isEnabled = true
            btnBulkPresent.isEnabled = true
            btnBulkAbsent.isEnabled = true
            btnClearAll.isEnabled = true
            btnBulkPresent.alpha = 1.0f
            btnBulkAbsent.alpha = 1.0f
            btnClearAll.alpha = 1.0f
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.action_confirm) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    companion object {
        fun newInstance(classId: Long) = AttendanceFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
