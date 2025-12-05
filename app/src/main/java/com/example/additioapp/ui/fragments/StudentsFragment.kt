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
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewStudents)
        val textTotalStudents = view.findViewById<android.widget.TextView>(R.id.textTotalStudents)

        val etSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        val layoutEmptyState = view.findViewById<android.widget.LinearLayout>(R.id.layoutEmptyState)

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
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

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

        var currentStudents: List<StudentEntity> = emptyList()
        var currentAttendance: List<com.example.additioapp.data.model.AttendanceRecordWithType> = emptyList()
        var currentBehaviors: List<com.example.additioapp.data.model.BehaviorRecordEntity> = emptyList()
        var currentGrades: List<com.example.additioapp.data.model.GradeRecordEntity> = emptyList()
        var sortMode = "NAME_ASC" // NAME_ASC, NAME_DESC, ID_ASC

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
                "NAME_ASC" -> currentStudents.sortedBy { it.name }
                "NAME_DESC" -> currentStudents.sortedByDescending { it.name }
                "ID_ASC" -> currentStudents.sortedBy { it.studentId }
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
                "NAME_ASC" -> "Sort by Name (A-Z)"
                "NAME_DESC" -> "Sort by Name (Z-A)"
                "ID_ASC" -> "Sort by ID"
                else -> "Sort"
            }
            btnSort.text = sortText
        }

        btnSort.setOnClickListener {
            val options = arrayOf("Name (A-Z)", "Name (Z-A)", "ID")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sort Students By")
                .setItems(options) { _, which ->
                    sortMode = when (which) {
                        0 -> "NAME_ASC"
                        1 -> "NAME_DESC"
                        2 -> "ID_ASC"
                        else -> "NAME_ASC"
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
    }

    companion object {
        fun newInstance(classId: Long) = StudentsFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
