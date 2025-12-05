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
            android.widget.Toast.makeText(requireContext(), "Exported ${currentStudents.size} students", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
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
                android.widget.Toast.makeText(requireContext(), "No students in this class", android.widget.Toast.LENGTH_SHORT).show()
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
                textSelectionCount.text = "$count selected"
            } else {
                selectionToolbar.visibility = View.GONE
                searchCard.visibility = View.VISIBLE
                fab.show()
            }
        }

        val adapter = StudentAdapter(
            onStudentClick = { student ->
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
            onReportClick = { student ->
                val dialog = com.example.additioapp.ui.dialogs.AbsenceReportDialog.newInstance(student.id, student.name)
                dialog.show(parentFragmentManager, "AbsenceReportDialog")
            },
            onGradesClick = { student ->
                val dialog = com.example.additioapp.ui.dialogs.GradesReportDialog.newInstance(student.id, student.name)
                dialog.show(parentFragmentManager, "GradesReportDialog")
            },
            onBehaviorClick = { student, type ->
                val dialog = com.example.additioapp.ui.dialogs.BehaviorReportDialog.newInstance(student.id, student.name, type)
                dialog.show(parentFragmentManager, "BehaviorReportDialog")
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
                    .setTitle("Delete Students")
                    .setMessage("Are you sure you want to delete ${selectedStudents.size} student(s)?\n\nThis will also delete their attendance, grades, and behavior records.")
                    .setPositiveButton("Delete") { _, _ ->
                        // Disable buttons to prevent double-tap
                        btnDeleteSelected.isEnabled = false
                        btnSelectAll.isEnabled = false
                        btnCancelSelection.isEnabled = false
                        (btnDeleteSelected as? com.google.android.material.button.MaterialButton)?.text = "Deleting..."
                        
                        // Delete students
                        selectedStudents.forEach { student ->
                            studentViewModel.deleteStudent(student)
                        }
                        
                        // Exit selection mode and show toast
                        adapter.exitSelectionMode()
                        android.widget.Toast.makeText(requireContext(), "${selectedStudents.size} students deleted", android.widget.Toast.LENGTH_SHORT).show()
                        
                        // Re-enable buttons (toolbar will be hidden by exitSelectionMode)
                        btnDeleteSelected.isEnabled = true
                        btnSelectAll.isEnabled = true
                        btnCancelSelection.isEnabled = true
                        (btnDeleteSelected as? com.google.android.material.button.MaterialButton)?.text = "🗑️ Delete"
                    }
                    .setNegativeButton("Cancel", null)
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
                    textTotalStudents.text = "0 Students found"
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                    textTotalStudents.text = "${adapter.itemCount} Students found"
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        var currentAttendance: List<com.example.additioapp.data.model.AttendanceRecordWithType> = emptyList()
        var currentBehaviors: List<com.example.additioapp.data.model.BehaviorRecordEntity> = emptyList()
        var currentGrades: List<com.example.additioapp.data.model.GradeRecordEntity> = emptyList()
        
        // Load default sort order from preferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultSort = when (prefs.getString("pref_sort_order", "lastname")) {
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
                textTotalStudents.text = "0 Students found"
            } else {
                recyclerView.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
                textTotalStudents.text = "${adapter.itemCount} Students found"
            }
            
            val sortText = when (sortMode) {
                "LASTNAME_ASC", "NAME_ASC" -> "Sort: Last Name (A-Z)"
                "LASTNAME_DESC", "NAME_DESC" -> "Sort: Last Name (Z-A)"
                "FIRSTNAME_ASC" -> "Sort: First Name (A-Z)"
                "FIRSTNAME_DESC" -> "Sort: First Name (Z-A)"
                "ID_ASC" -> "Sort: ID/Matricule"
                else -> "Sort"
            }
            btnSort.text = sortText
        }

        btnSort.setOnClickListener {
            val options = arrayOf("Last Name (A-Z)", "Last Name (Z-A)", "First Name (A-Z)", "First Name (Z-A)", "ID/Matricule")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sort Students By")
                .setItems(options) { _, which ->
                    sortMode = when (which) {
                        0 -> "LASTNAME_ASC"
                        1 -> "LASTNAME_DESC"
                        2 -> "FIRSTNAME_ASC"
                        3 -> "FIRSTNAME_DESC"
                        4 -> "ID_ASC"
                        else -> "LASTNAME_ASC"
                    }
                    updateUI()
                }
                .show()
        }

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            currentStudents = students
            updateUI()
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
            val options = arrayOf("Add Student", "Import from CSV", "Export to CSV")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Student Options")
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
                                android.widget.Toast.makeText(requireContext(), "No students to export", android.widget.Toast.LENGTH_SHORT).show()
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
            .setTitle("🎲 Random Student")
            .setView(dialogView)
            .setPositiveButton("Pick Again") { _, _ -> }
            .setNegativeButton("Close", null)
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
        fun newInstance(classId: Long) = StudentsFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
