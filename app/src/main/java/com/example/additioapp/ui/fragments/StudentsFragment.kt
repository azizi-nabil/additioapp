package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.StudentAdapter
import com.example.additioapp.ui.dialogs.AddStudentDialog
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class StudentsFragment : Fragment() {

    private var classId: Long = -1
    private var highlightStudentId: Long = -1L
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private val behaviorViewModel: com.example.additioapp.ui.viewmodel.BehaviorViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val gradeViewModel: com.example.additioapp.ui.viewmodel.GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var currentStudents: List<StudentEntity> = emptyList()

    private val exportLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { exportToCsv(it) }
    }

    private fun exportToCsv(uri: android.net.Uri) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.bufferedWriter()
                // Write header
                writer.write("Matricule,Nom,Prénom,Notes")
                writer.newLine()
                // Write student data
                currentStudents.forEach { student ->
                    val lastName = if (!student.lastNameAr.isNullOrEmpty()) {
                        "${student.lastNameFr}/${student.lastNameAr}"
                    } else {
                        student.lastNameFr
                    }
                    val firstName = if (!student.firstNameAr.isNullOrEmpty()) {
                        "${student.firstNameFr}/${student.firstNameAr}"
                    } else {
                        student.firstNameFr
                    }
                    val notes = student.notes?.replace(",", ";")?.replace("\n", " ") ?: ""
                    writer.write("${student.displayMatricule},$lastName,$firstName,$notes")
                    writer.newLine()
                }
                writer.flush()
            }
            android.widget.Toast.makeText(requireContext(), getString(R.string.toast_export_success, currentStudents.size), android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), getString(R.string.toast_export_failed, e.message), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
            highlightStudentId = it.getLong("studentId", -1L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddStudent)
        val btnSort = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSort)
        val btnRandomPicker = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRandomPicker)
        val btnToggleFab = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnToggleFab)
        
        // Load FAB visibility preference
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        var isFabVisibleByUser = prefs.getBoolean("pref_fab_visible_students", true)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewStudents)
        val textTotalStudents = view.findViewById<android.widget.TextView>(R.id.textTotalStudents)

        val etSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        val layoutEmptyState = view.findViewById<android.widget.LinearLayout>(R.id.layoutEmptyState)
        val searchCard = view.findViewById<View>(R.id.searchCard)

        // Random picker
        btnRandomPicker.setOnClickListener {
            if (currentStudents.isNotEmpty()) {
                showRandomStudentDialog()
            } else {
                android.widget.Toast.makeText(requireContext(), getString(R.string.toast_no_students), android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Selection mode views
        val selectionToolbar = view.findViewById<View>(R.id.selectionToolbar)
        val textSelectionCount = view.findViewById<android.widget.TextView>(R.id.textSelectionCount)
        val btnCancelSelection = view.findViewById<View>(R.id.btnCancelSelection)
        val btnSelectAll = view.findViewById<View>(R.id.btnSelectAll)
        val btnDeleteSelected = view.findViewById<View>(R.id.btnDeleteSelected)

        // Function to update selection UI
        fun updateSelectionUI(count: Int) {
            if (count > 0) {
                selectionToolbar.visibility = View.VISIBLE
                searchCard.visibility = View.GONE
                fab.hide()
                textSelectionCount.text = getString(R.string.students_selected, count)
            } else {
                selectionToolbar.visibility = View.GONE
                searchCard.visibility = View.VISIBLE
                if (isFabVisibleByUser) fab.show()
            }
        }
        
        // Initial state
        if (!isFabVisibleByUser) {
            fab.hide()
            btnToggleFab.alpha = 0.5f
        }

        btnToggleFab.setOnClickListener {
            isFabVisibleByUser = !isFabVisibleByUser
            prefs.edit().putBoolean("pref_fab_visible_students", isFabVisibleByUser).apply()
            if (isFabVisibleByUser) {
                fab.show()
                btnToggleFab.alpha = 1.0f
            } else {
                fab.hide()
                btnToggleFab.alpha = 0.5f
            }
        }

        val adapter = StudentAdapter(
            onStudentClick = { student ->
                // Card click - no action, use menu for actions
            },
            onEditClick = { student ->
                val dialog = AddStudentDialog(
                    classId = classId,
                    studentEntity = student,
                    onSave = { updatedStudent ->
                        studentViewModel.updateStudent(updatedStudent)
                    },
                    onDelete = { studentToDelete ->
                        studentViewModel.deleteStudent(studentToDelete)
                    }
                )
                dialog.show(parentFragmentManager, "EditStudentDialog")
            },
            onDeleteClick = { student ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_delete_student)
                    .setMessage(getString(R.string.msg_delete_student_confirm, student.name))
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        studentViewModel.deleteStudent(student)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            },
            onReportClick = { student ->
                val dialog = com.example.additioapp.ui.dialogs.AbsenceReportDialog.newInstance(student.id, student.name, classId)
                dialog.show(parentFragmentManager, "AbsenceReportDialog")
            },
            onGradesClick = { student ->
                val dialog = com.example.additioapp.ui.dialogs.GradesReportDialog.newInstance(student.id, student.name)
                dialog.show(parentFragmentManager, "GradesReportDialog")
            },
            onNotesClick = { student ->
                val dialog = com.example.additioapp.ui.dialogs.StudentNotesDialog.newInstance(student.id, student.name)
                dialog.show(parentFragmentManager, "StudentNotesDialog")
            },
            onBehaviorClick = { student, type ->
                val dialog = com.example.additioapp.ui.dialogs.BehaviorReportDialog.newInstance(student.id, student.name, type)
                dialog.show(parentFragmentManager, "BehaviorReportDialog")
            },
            onBehaviorFullReportClick = { student ->
                val dialog = com.example.additioapp.ui.dialogs.BehaviorFullReportDialog.newInstance(student.id, student.name)
                dialog.show(parentFragmentManager, "BehaviorFullReportDialog")
            },
            onSelectionChanged = { count ->
                updateSelectionUI(count)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Selection mode buttons
        btnCancelSelection.setOnClickListener {
            adapter.exitSelectionMode()
        }

        btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        btnDeleteSelected.setOnClickListener {
            val selectedStudents = adapter.getSelectedStudents()
            if (selectedStudents.isNotEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_delete_students)
                    .setMessage(getString(R.string.msg_delete_students_confirm, selectedStudents.size))
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        // Disable buttons to prevent double-tap
                        btnDeleteSelected.isEnabled = false
                        btnSelectAll.isEnabled = false
                        btnCancelSelection.isEnabled = false
                        (btnDeleteSelected as? com.google.android.material.button.MaterialButton)?.text = getString(R.string.msg_deleting)
                        
                        // Delete students
                        selectedStudents.forEach { student ->
                            studentViewModel.deleteStudent(student)
                        }
                        
                        // Exit selection mode and show toast
                        adapter.exitSelectionMode()
                        android.widget.Toast.makeText(requireContext(), getString(R.string.msg_students_found, selectedStudents.size).replace("found", "deleted"), android.widget.Toast.LENGTH_SHORT).show()
                        
                        // Re-enable buttons (toolbar will be hidden by exitSelectionMode)
                        btnDeleteSelected.isEnabled = true
                        btnSelectAll.isEnabled = true
                        btnCancelSelection.isEnabled = true
                        (btnDeleteSelected as? com.google.android.material.button.MaterialButton)?.text = getString(R.string.students_delete_btn)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        }

        // Search Listener
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
                
                if (adapter.itemCount == 0) {
                    recyclerView.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                    textTotalStudents.text = getString(R.string.msg_no_students_found)
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                    textTotalStudents.text = getString(R.string.msg_students_found, adapter.itemCount)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        var currentAttendance: List<com.example.additioapp.data.model.AttendanceRecordWithType> = emptyList()
        var currentBehaviors: List<com.example.additioapp.data.model.BehaviorRecordEntity> = emptyList()
        var currentGrades: List<com.example.additioapp.data.model.GradeRecordEntity> = emptyList()
        
        // Load default sort order from preferences
        val sortPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultSort = when (sortPrefs.getString("pref_sort_order", "lastname")) {
            "firstname" -> "FIRSTNAME_ASC"
            "id" -> "ID_ASC"
            else -> "LASTNAME_ASC"
        }
        var sortMode = defaultSort

        fun updateUI() {
            val stats = mutableMapOf<Long, com.example.additioapp.ui.adapters.StudentStats>()
            
            // Group all records by student
            val attendanceByStudent = currentAttendance.groupBy { it.studentId }
            val behaviorByStudent = currentBehaviors.groupBy { it.studentId }
            val gradesByStudent = currentGrades.groupBy { it.studentId }
            
            currentStudents.forEach { student ->
                val studentId = student.id
                val attRecords = attendanceByStudent[studentId] ?: emptyList()
                val behRecords = behaviorByStudent[studentId] ?: emptyList()
                val gradeRecords = gradesByStudent[studentId] ?: emptyList()

                // Filter for Cours (Presence)
                val coursRecords = attRecords.filter { it.type == "Cours" }
                val coursTotal = coursRecords.size
                val coursPresent = coursRecords.count { it.status == "P" }

                // Filter for TD
                val tdRecords = attRecords.filter { it.type == "TD" }
                val tdTotal = tdRecords.size
                val tdExcused = tdRecords.count { it.status == "E" }
                val tdAbsent = tdRecords.count { it.status == "A" || it.status == "E" }

                // Filter for TP
                val tpRecords = attRecords.filter { it.type == "TP" }
                val tpTotal = tpRecords.size
                val tpExcused = tpRecords.count { it.status == "E" }
                val tpAbsent = tpRecords.count { it.status == "A" || it.status == "E" }
                
                // Behavior
                val posPoints = behRecords.filter { it.type == "POSITIVE" }.sumOf { it.points }
                val negPoints = behRecords.filter { it.type == "NEGATIVE" }.sumOf { it.points }

                stats[studentId] = com.example.additioapp.ui.adapters.StudentStats(
                    coursPresent = coursPresent,
                    coursTotal = coursTotal,
                    tdAbsent = tdAbsent,
                    tdExcused = tdExcused,
                    tdTotal = tdTotal,
                    tpAbsent = tpAbsent,
                    tpExcused = tpExcused,
                    tpTotal = tpTotal,
                    behaviorPositive = posPoints,
                    behaviorNegative = negPoints,
                    hasGrades = gradeRecords.isNotEmpty()
                )
            }

            val sortedList = when (sortMode) {
                "LASTNAME_ASC" -> currentStudents.sortedBy { it.lastNameFr.ifEmpty { it.name } }
                "LASTNAME_DESC" -> currentStudents.sortedByDescending { it.lastNameFr.ifEmpty { it.name } }
                "FIRSTNAME_ASC" -> currentStudents.sortedBy { it.firstNameFr }
                "FIRSTNAME_DESC" -> currentStudents.sortedByDescending { it.firstNameFr }
                "ID_ASC" -> currentStudents.sortedBy { it.displayMatricule }
                // Legacy support
                "NAME_ASC" -> currentStudents.sortedBy { it.lastNameFr.ifEmpty { it.name } }
                "NAME_DESC" -> currentStudents.sortedByDescending { it.lastNameFr.ifEmpty { it.name } }
                else -> currentStudents
            }

            adapter.submitList(sortedList, stats)
            
            // Re-apply filter if search text exists
            val query = etSearch.text.toString()
            if (query.isNotEmpty()) {
                adapter.filter(query)
            }
            
            if (adapter.itemCount == 0) {
                recyclerView.visibility = View.GONE
                layoutEmptyState.visibility = View.VISIBLE
                textTotalStudents.text = getString(R.string.msg_no_students_found)
            } else {
                recyclerView.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
                textTotalStudents.text = getString(R.string.msg_students_found, adapter.itemCount)
            }
            
            val sortText = when (sortMode) {
                "LASTNAME_ASC", "NAME_ASC", "FIRSTNAME_ASC" -> getString(R.string.sort_display_az)
                "LASTNAME_DESC", "NAME_DESC", "FIRSTNAME_DESC" -> getString(R.string.sort_display_za)
                "ID_ASC" -> getString(R.string.sort_display_id)
                else -> getString(R.string.sort_default)
            }
            btnSort.text = sortText
        }

        btnSort.setOnClickListener {
            // Get name preference from settings
            val nameField = sortPrefs.getString("pref_sort_order", "lastname") ?: "lastname"
            
            val options = arrayOf(
                getString(R.string.sort_az),
                getString(R.string.sort_za),
                getString(R.string.sort_id_matricule_option)
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_sort_students_by))
                .setItems(options) { _, which ->
                    sortMode = when (which) {
                        0 -> if (nameField == "firstname") "FIRSTNAME_ASC" else "LASTNAME_ASC"
                        1 -> if (nameField == "firstname") "FIRSTNAME_DESC" else "LASTNAME_DESC"
                        2 -> "ID_ASC"
                        else -> if (nameField == "firstname") "FIRSTNAME_ASC" else "LASTNAME_ASC"
                    }
                    updateUI()
                }
                .show()
        }

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            currentStudents = students
            updateUI()
            
            // Scroll to highlighted student if navigated from search
            if (highlightStudentId != -1L) {
                val position = students.indexOfFirst { it.id == highlightStudentId }
                if (position != -1) {
                    recyclerView.post {
                        recyclerView.scrollToPosition(position)
                        // Apply search filter for the student name
                        val student = students.find { it.id == highlightStudentId }
                        student?.let {
                            etSearch.setText(it.displayNameFr)
                        }
                    }
                }
                highlightStudentId = -1L // Reset after scrolling
            }
        }

        attendanceViewModel.getAttendanceWithTypeForClass(classId).observe(viewLifecycleOwner) { records ->
            currentAttendance = records
            updateUI()
        }
        
        behaviorViewModel.getBehaviorsForClass(classId).observe(viewLifecycleOwner) { records ->
            currentBehaviors = records
            updateUI()
        }

        gradeViewModel.getGradeRecordsForClass(classId).observe(viewLifecycleOwner) { records ->
            currentGrades = records
            updateUI()
        }

        fab.setOnClickListener {
            val dialog = AddStudentDialog(
                classId = classId,
                studentEntity = null,
                onSave = { student ->
                    studentViewModel.insertStudent(student)
                }
            )
            dialog.show(parentFragmentManager, "AddStudentDialog")
        }

        // Long press to show import/export options
        fab.setOnLongClickListener {
            val options = arrayOf(getString(R.string.action_add_student), getString(R.string.action_import_from_csv), getString(R.string.action_export_to_csv))
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_student_options))
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val dialog = AddStudentDialog(
                                classId = classId,
                                studentEntity = null,
                                onSave = { student ->
                                    studentViewModel.insertStudent(student)
                                }
                            )
                            dialog.show(parentFragmentManager, "AddStudentDialog")
                        }
                        1 -> {
                            val importDialog = com.example.additioapp.ui.dialogs.ImportStudentsDialog(
                                classId = classId,
                                onImport = { students ->
                                    students.forEach { studentViewModel.insertStudent(it) }
                                }
                            )
                            importDialog.show(parentFragmentManager, "ImportStudentsDialog")
                        }
                        2 -> {
                            if (currentStudents.isNotEmpty()) {
                                exportLauncher.launch("students_export.csv")
                            } else {
                                android.widget.Toast.makeText(requireContext(), getString(R.string.toast_no_students_to_export), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .show()
            true
        }
    }

    private fun showRandomStudentDialog() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val nameLang = prefs.getString("pref_name_language", "french") ?: "french"
        
        fun getDisplayName(student: StudentEntity): String {
            return if (nameLang == "arabic" && !student.displayNameAr.isNullOrEmpty()) {
                student.displayNameAr!!
            } else {
                student.displayNameFr
            }
        }

        val dialogView = android.view.LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)
        
        val textView = dialogView.findViewById<android.widget.TextView>(android.R.id.text1)
        textView.gravity = android.view.Gravity.CENTER
        textView.setPadding(48, 64, 48, 64)
        
        val randomStudent = currentStudents.random()
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_random_student_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_pick_again)) { _, _ -> }
            .setNegativeButton(getString(R.string.action_close), null)
            .create()

        dialog.show()

        // Animate through several names before landing on the selected one
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var count = 0
        val maxIterations = 15
        val shuffleRunnable = object : Runnable {
            override fun run() {
                if (count < maxIterations) {
                    val tempStudent = currentStudents.random()
                    textView.text = getDisplayName(tempStudent)
                    textView.textSize = 20f
                    count++
                    handler.postDelayed(this, 50L + (count * 10L))
                } else {
                    // Final selection
                    textView.text = getDisplayName(randomStudent)
                    textView.textSize = 28f
                    textView.setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
        }
        handler.post(shuffleRunnable)

        // Override positive button to pick again without closing
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            count = 0
            val newRandomStudent = currentStudents.random()
            val newShuffleRunnable = object : Runnable {
                override fun run() {
                    if (count < maxIterations) {
                        val tempStudent = currentStudents.random()
                        textView.text = getDisplayName(tempStudent)
                        textView.textSize = 20f
                        textView.setTypeface(null, android.graphics.Typeface.NORMAL)
                        count++
                        handler.postDelayed(this, 50L + (count * 10L))
                    } else {
                        textView.text = getDisplayName(newRandomStudent)
                        textView.textSize = 28f
                        textView.setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                }
            }
            handler.post(newShuffleRunnable)
        }
    }

    companion object {
        fun newInstance(classId: Long, studentId: Long = -1L) = StudentsFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
                putLong("studentId", studentId)
            }
        }
    }
}
