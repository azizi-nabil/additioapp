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
    private var dateHasOtherRecords = false // True if records exist for different type on same date

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
                if (isLocked && !isNewSession) return
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
                        
                        // Check if new session type already has records
                        val newTypeRecords = withContext(Dispatchers.IO) {
                            attendanceViewModel.getAttendanceForSessionOnce(newSessionId)
                        }
                        
                        // Clear originalSessionId to use the new type's sessionId
                        originalSessionId = null
                        
                        if (newTypeRecords.isEmpty()) {
                            // New type has no records - enable auto-fill for new session
                            android.util.Log.d("AttendanceFragment", "Spinner: switching to NEW type $selectedType (no records)")
                            shouldAutoFill = true
                            unsavedRecords.clear()
                            // Don't migrate - keep old records under old sessionId
                        } else {
                            // New type has records - load those (don't migrate from old type)
                            android.util.Log.d("AttendanceFragment", "Spinner: switching to EXISTING type $selectedType (${newTypeRecords.size} records)")
                            shouldAutoFill = false
                        }
                        
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
            if (isLocked && !isNewSession) {
                Toast.makeText(requireContext(), getString(R.string.msg_unlock_to_modify), Toast.LENGTH_SHORT).show()
                return@AttendanceAdapter
            }
            // Show bottom sheet dialog
            lifecycleScope.launch {
                val sessionId = getSessionId(selectedDate)
                
                // Get current record (either from DB or unsavedRecords)
                val currentRecord = if (isNewSession) {
                    unsavedRecords[student.id]
                } else {
                    withContext(Dispatchers.IO) {
                        attendanceViewModel.getAttendanceForSessionOnce(sessionId)
                    }.find { it.studentId == student.id }
                }
                
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
                        
                        if (isNewSession) {
                            // Update local state ONLY
                            unsavedRecords[student.id] = record
                            hasChanges = true
                            summaryTextView?.let { refreshData(currentStudents, it) }
                        } else {
                            // Live Update
                            lifecycleScope.launch {
                                attendanceViewModel.setAttendance(record)
                                // Also ensure session type is saved (idempotent)
                                val currentType = attendanceViewModel.currentSessionType.value ?: "Cours"
                                attendanceViewModel.saveSessionType(classId, selectedDate.timeInMillis, currentType)
                                
                                // Manually refresh to show update immediately (since we disabled observer)
                                summaryTextView?.let { refreshData(currentStudents, it) }
                            }
                        }
                    }
                )
                dialog.show(parentFragmentManager, "AttendanceBottomSheet")
            }
        }

        // Handle Back Navigation
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isNewSession) {
                    val dateFormat = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault())
                    val dateStr = dateFormat.format(selectedDate.time)
                    
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Save Attendance?")
                        .setMessage("Save attendance for $dateStr?")
                        .setPositiveButton("Save") { _, _ ->
                            // Enforce current date and sessionID (Defense against stale unsavedRecords)
                            val finalSessionId = getSessionId(selectedDate)
                            val finalDate = selectedDate.timeInMillis
                            val recordsToSave = unsavedRecords.values.map { 
                                it.copy(sessionId = finalSessionId, date = finalDate) 
                            }
                            
                            // Save all unsaved records
                            lifecycleScope.launch {
                                // Check if session with EXACT sessionId already exists (same date + type)
                                android.util.Log.d("AttendanceFragment", "SAVE CHECK: finalSessionId=$finalSessionId")
                                val existingRecords = withContext(Dispatchers.IO) {
                                    attendanceViewModel.getAttendanceForSessionOnce(finalSessionId)
                                }
                                android.util.Log.d("AttendanceFragment", "SAVE CHECK: existingRecords.size=${existingRecords.size}")
                                
                                if (existingRecords.isNotEmpty()) {
                                    // Exact same session already exists - block save
                                    android.util.Log.w("AttendanceFragment", "SAVE BLOCKED: Session $finalSessionId has ${existingRecords.size} records")
                                    android.widget.Toast.makeText(requireContext(), 
                                        "A $currentSessionType session already exists for this date!",
                                        android.widget.Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                
                                // Save session type first, then batch insert records (on IO thread, awaited)
                                withContext(Dispatchers.IO) {
                                    attendanceViewModel.saveSessionType(classId, finalDate, currentSessionType)
                                    attendanceViewModel.insertAttendanceListSync(recordsToSave)
                                }
                                
                                android.widget.Toast.makeText(requireContext(), "Attendance Saved", android.widget.Toast.LENGTH_SHORT).show()
                                
                                // Disable interception and navigate back
                                isEnabled = false
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        }
                        .setNegativeButton("Discard") { _, _ ->
                            // Discard changes and exit
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        .setNeutralButton("Cancel", null)
                        .show()
                } else {
                    // No new changes or existing session -> proceed normally
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        statusViewModel.statuses.observe(viewLifecycleOwner) { statuses ->
            cachedStatuses = statuses
            adapter.setStatuses(statuses)
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
            if (isLocked && !isNewSession) return@setOnClickListener
            showConfirmationDialog(getString(R.string.title_mark_all_present), getString(R.string.msg_mark_all_present)) {
                applyBulkStatus("P")
            }
        }

        btnBulkAbsent.setOnClickListener {
            if (isLocked && !isNewSession) return@setOnClickListener
            showConfirmationDialog(getString(R.string.title_mark_all_absent), getString(R.string.msg_mark_all_absent)) {
                applyBulkStatus("A")
            }
        }

        btnClearAll.setOnClickListener {
            if (isLocked && !isNewSession) return@setOnClickListener
            showConfirmationDialog(getString(R.string.title_clear_attendance), getString(R.string.msg_clear_attendance)) {
                clearAll()
            }
        }
        
        btnLockToggle.setOnClickListener {
            isLocked = !isLocked
            updateLockState(view)
        }

        btnChangeDate.setOnClickListener {
            if (isNewSession && hasChanges) {
                val dateFormat = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault())
                val dateStr = dateFormat.format(selectedDate.time)
                
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Unsaved Changes")
                    .setMessage("Save attendance for $dateStr before switching?")
                    .setPositiveButton("Save") { _, _ ->
                         // Enforce current date and sessionID
                         val finalSessionId = getSessionId(selectedDate)
                         val finalDate = selectedDate.timeInMillis
                         val recordsToSave = unsavedRecords.values.map { 
                             it.copy(sessionId = finalSessionId, date = finalDate) 
                         }

                         lifecycleScope.launch {
                            // Check if session with EXACT sessionId already exists (same date + type)
                            val existingRecords = withContext(Dispatchers.IO) {
                                attendanceViewModel.getAttendanceForSessionOnce(finalSessionId)
                            }
                            
                            if (existingRecords.isNotEmpty()) {
                                // Exact same session already exists - block save
                                android.widget.Toast.makeText(requireContext(), 
                                    "A $currentSessionType session already exists for this date!", 
                                    android.widget.Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            
                            // Save on IO thread, awaited
                            withContext(Dispatchers.IO) {
                                attendanceViewModel.saveSessionType(classId, finalDate, currentSessionType)
                                attendanceViewModel.insertAttendanceListSync(recordsToSave)
                            }
                            android.widget.Toast.makeText(requireContext(), "Saved", android.widget.Toast.LENGTH_SHORT).show()
                            showDatePicker(textDate)
                        }
                    }
                    .setNegativeButton("Discard") { _, _ ->
                        showDatePicker(textDate)
                    }
                    .setNeutralButton("Cancel", null)
                    .show()
            } else {
                showDatePicker(textDate)
            }
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
                // Clear the passed sessionId since we're now on a different date
                arguments?.remove("sessionId")
                originalSessionId = null // Clear legacy sessionId
                
                // Clear local unsaved state to prevent carry-over from previous date
                unsavedRecords.clear()
                hasChanges = false
                isNewSession = false // Reset new session flag until refreshData determines strictly

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
    
    // New Session State
    private var isNewSession = false
    private val unsavedRecords = mutableMapOf<Long, AttendanceRecordEntity>()
    private var hasChanges = false

    private fun refreshData(students: List<com.example.additioapp.data.model.StudentEntity>, summaryText: TextView) {
        currentStudents = students
        val sessionId = getSessionId(selectedDate)
        
        // Cancel previous job to prevent race conditions
        refreshJob?.cancel()
        
        refreshJob = lifecycleScope.launch {
            // If we are already in new session mode, use local data unless force refresh is needed?
            // Actually, we should check DB first to see if session really exists.
            
            var records = withContext(Dispatchers.IO) {
                attendanceViewModel.getAttendanceForSessionOnce(sessionId)
            }
            
            android.util.Log.d("AttendanceFragment", "Loaded ${records.size} records from DB for session $sessionId")
            
            // Also check if ANY records exist for this date (any type) to prevent false "new session"
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dateStr = dateFormat.format(selectedDate.time)
            val anyRecordsForDate = withContext(Dispatchers.IO) {
                attendanceViewModel.countRecordsByDatePattern(classId, dateStr)
            }
            android.util.Log.d("AttendanceFragment", "Total records for date $dateStr: $anyRecordsForDate")
            
            if (records.isEmpty() && shouldAutoFill && students.isNotEmpty()) {
                // NEW SESSION MODE - no records exist for this exact sessionId (class + date + type)
                // This allows different types on the same date (Cours + TD on Dec 9)
                isNewSession = true
                dateHasOtherRecords = false
                android.util.Log.d("AttendanceFragment", "New Session detected for sessionId=$sessionId (records empty, auto-fill enabled)")
                
                // Initialize unsavedRecords if empty (first load)
                if (unsavedRecords.isEmpty()) {
                     students.forEach { student ->
                        unsavedRecords[student.id] = AttendanceRecordEntity(
                            studentId = student.id,
                            sessionId = sessionId,
                            date = selectedDate.timeInMillis,
                            status = "P"
                        )
                    }
                    hasChanges = false // Auto-load doesn't count as user modification for Date Switch warning
                }
                
                // Use unsavedRecords for display
                // Note: records variable is local, we can re-assign it for display purposes
                records = unsavedRecords.values.toList()
                
            } else {
                // EXISTING SESSION - records loaded for current sessionId
                isNewSession = false
                dateHasOtherRecords = false
                unsavedRecords.clear()
                hasChanges = false
            }
            
            // Update lock state based on new session status
            withContext(Dispatchers.Main) {
                this@AttendanceFragment.view?.let { updateLockState(it) }
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
        
        if (isNewSession) {
            // Update local state ONLY
            currentStudents.forEach { student ->
                unsavedRecords[student.id] = AttendanceRecordEntity(
                    studentId = student.id,
                    sessionId = sessionId,
                    date = selectedDate.timeInMillis,
                    status = code
                )
            }
            hasChanges = true
            summaryTextView?.let { refreshData(currentStudents, it) }
        } else {
            // Block if records exist for different type on same date
            if (dateHasOtherRecords) {
                android.widget.Toast.makeText(requireContext(), 
                    "Cannot save: a session already exists for this date.", 
                    android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // Live Update
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
    }

    private fun clearAll() {
        val sessionId = getSessionId(selectedDate)
        shouldAutoFill = false // Prevent auto-fill after clearing
        
        if (isNewSession) {
            unsavedRecords.clear()
            hasChanges = true // Cleared state is a change
            summaryTextView?.let { refreshData(currentStudents, it) }
        } else {
            lifecycleScope.launch {
                attendanceViewModel.deleteSession(sessionId)
                summaryTextView?.let { refreshData(currentStudents, it) }
            }
        }
    }

    private var isLocked = true

    private fun updateLockState(view: View) {
        val btnLockToggle = view.findViewById<MaterialButton>(R.id.btnLockToggle)
        val spinnerNature = view.findViewById<android.widget.Spinner>(R.id.spinnerSessionNature)
        val btnBulkPresent = view.findViewById<Button>(R.id.btnBulkPresent)
        val btnBulkAbsent = view.findViewById<Button>(R.id.btnBulkAbsent)
        val btnClearAll = view.findViewById<Button>(R.id.btnClearAll)
        
        // Update lock toggle icon
        if (isLocked) {
            btnLockToggle.setIconResource(R.drawable.ic_lock)
        } else {
            btnLockToggle.setIconResource(R.drawable.ic_lock_open)
        }

        // Enable controls if unlocked OR if it's a new session
        val enableControls = !isLocked || isNewSession

        spinnerNature.isEnabled = enableControls
        btnBulkPresent.isEnabled = enableControls
        btnBulkAbsent.isEnabled = enableControls
        btnClearAll.isEnabled = enableControls
        
        val alpha = if (enableControls) 1.0f else 0.5f
        btnBulkPresent.alpha = alpha
        btnBulkAbsent.alpha = alpha
        btnClearAll.alpha = alpha
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

