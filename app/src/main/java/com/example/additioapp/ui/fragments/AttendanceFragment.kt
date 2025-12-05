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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
            android.util.Log.d("AttendanceFragment", "onCreate: Received classId $classId")
            val dateMillis = it.getLong("selectedDate", -1L)
            if (dateMillis != -1L) {
                selectedDate.timeInMillis = dateMillis
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
        
        // Setup Spinner
        val natureOptions = listOf("Cours", "TD", "TP")
        val spinnerAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, natureOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNature.adapter = spinnerAdapter
        spinnerNature.isEnabled = !isLocked

        // Observe Session Type
        attendanceViewModel.currentSessionType.observe(viewLifecycleOwner) { type ->
            currentSessionType = type // Sync local variable
            val index = natureOptions.indexOf(type)
            if (index >= 0 && spinnerNature.selectedItemPosition != index) {
                spinnerNature.setSelection(index)
            }
        }

        // Handle Spinner Selection
        spinnerNature.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isLocked) return
                val selectedType = natureOptions[position]
                if (currentSessionType != selectedType) {
                    currentSessionType = selectedType
                    lifecycleScope.launch {
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
                Toast.makeText(requireContext(), "Unlock to modify attendance", Toast.LENGTH_SHORT).show()
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
            refreshData(students, textSummary)
        }

        btnBulkPresent.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showConfirmationDialog("Mark All Present", "Are you sure you want to mark all students as Present?") {
                applyBulkStatus("P")
            }
        }

        btnBulkAbsent.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showConfirmationDialog("Mark All Absent", "Are you sure you want to mark all students as Absent?") {
                applyBulkStatus("A")
            }
        }

        btnClearAll.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showConfirmationDialog("Clear Attendance", "Are you sure you want to clear all attendance records for this session? This cannot be undone.") {
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
    }

    private fun updateDateDisplay(textView: TextView) {
        val format = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        textView.text = format.format(selectedDate.time)
    }

    private fun getSessionId(calendar: Calendar): String {
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
                // Trigger re-fetch (simplistic approach: re-observe happens automatically if we used a LiveData for date)
                // Since we are manually observing inside the student observer, we might need to force refresh.
                // A better way is to have a "currentSessionId" LiveData in ViewModel.
                // For this MVP, let's just recreate the observer chain or notify change.
                // Actually, the simplest way here is to just reload the fragment or use a proper ViewModel trigger.
                // Let's create a method to refresh data.
                summaryTextView?.let { refreshData(currentStudents, it) }
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
            // Load Session Type (sync ViewModel if needed, but we rely on local variable)
            // attendanceViewModel.loadSessionType(classId, selectedDate.timeInMillis) 

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
                // Save to DB in background
                val recordsToSave = records
                launch {
                    recordsToSave.forEach { attendanceViewModel.setAttendance(it) }
                    // Ensure session is created with current type
                    attendanceViewModel.saveSessionType(classId, selectedDate.timeInMillis, currentSessionType)
                }
            }

            val items = students.map { student ->
                val record = records.find { it.studentId == student.id }
                StudentAttendanceItem(student, record)
            }
            adapter.submitList(items)
            updateSummary(items, summaryText)
        }
    }

    private fun updateSummary(items: List<StudentAttendanceItem>, summaryText: TextView) {
        val counts = items.groupingBy { it.attendance?.status ?: "-" }.eachCount()
        val present = counts["P"] ?: 0
        val absent = counts["A"] ?: 0
        val late = counts["L"] ?: 0
        
        // Debug Log
        android.util.Log.d("AttendanceSummary", "Counts: $counts")
        
        summaryText.text = "Present: $present | Absent: $absent | Late: $late"
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
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
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
