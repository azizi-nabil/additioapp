package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.GradeEntryAdapter
import com.example.additioapp.ui.adapters.StudentGradeItem
import com.example.additioapp.ui.viewmodel.GradeViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel

class GradeEntryFragment : Fragment() {

    private var classId: Long = -1
    private var gradeItemId: Long = -1
    private var gradeItemName: String? = null

    private val gradeViewModel: GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
            gradeItemId = it.getLong("gradeItemId")
            gradeItemName = it.getString("gradeItemName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_grade_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView = view.findViewById<TextView>(R.id.textGradeItemTitle)
        val btnSort = view.findViewById<View>(R.id.btnSort)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGradeEntry)

        titleTextView.text = gradeItemName ?: "Enter Grades"

        val adapter = GradeEntryAdapter { item, score, status ->
            val existingId = item.gradeRecord?.id ?: 0L
            val record = GradeRecordEntity(
                id = existingId,
                studentId = item.student.id,
                gradeItemId = gradeItemId,
                score = score,
                status = status
            )
            gradeViewModel.saveGradeAndRecalculate(record, classId)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        var currentSortMode = "NAME_ASC" // NAME_ASC, NAME_DESC, ID_ASC

        fun updateList(students: List<com.example.additioapp.data.model.StudentEntity>, records: List<GradeRecordEntity>) {
            val items = students.map { student ->
                val record = records.find { it.studentId == student.id }
                StudentGradeItem(student, record)
            }

            val sortedItems = when (currentSortMode) {
                "NAME_ASC" -> items.sortedBy { it.student.name }
                "NAME_DESC" -> items.sortedByDescending { it.student.name }
                "ID_ASC" -> items.sortedBy { it.student.id }
                else -> items
            }
            adapter.submitList(sortedItems)
        }

        val editSearch = view.findViewById<android.widget.EditText>(R.id.editSearchStudent)
        val editMinScore = view.findViewById<android.widget.EditText>(R.id.editMinScore)
        val editMaxScore = view.findViewById<android.widget.EditText>(R.id.editMaxScore)
        val textStudentCount = view.findViewById<TextView>(R.id.textStudentCount)

        var searchQuery = ""
        var minScore: Double? = null
        var maxScore: Double? = null

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            gradeViewModel.getGradesForItem(gradeItemId).observe(viewLifecycleOwner) { records ->
                
                fun filterAndUpdate() {
                    val filteredStudents = students.filter { student ->
                        val record = records.find { it.studentId == student.id }
                        val score = record?.score?.toDouble() ?: 0.0

                        val matchesName = student.name.contains(searchQuery, ignoreCase = true)
                        
                        val currentMin = minScore
                        val matchesMin = if (currentMin != null) score >= currentMin else true
                        
                        val currentMax = maxScore
                        val matchesMax = if (currentMax != null) score <= currentMax else true

                        matchesName && matchesMin && matchesMax
                    }
                    textStudentCount.text = "${filteredStudents.size} Students Found"
                    updateList(filteredStudents, records)
                }

                editSearch.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        searchQuery = s.toString()
                        filterAndUpdate()
                    }
                })

                editMinScore.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        minScore = s.toString().toDoubleOrNull()
                        filterAndUpdate()
                    }
                })

                editMaxScore.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        maxScore = s.toString().toDoubleOrNull()
                        filterAndUpdate()
                    }
                })

                // Initial load
                filterAndUpdate()
                
                btnSort.setOnClickListener {
                    val options = arrayOf("Name (A-Z)", "Name (Z-A)", "ID")
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Sort By")
                        .setItems(options) { _, which ->
                            currentSortMode = when (which) {
                                0 -> "NAME_ASC"
                                1 -> "NAME_DESC"
                                2 -> "ID_ASC"
                                else -> "NAME_ASC"
                            }
                            filterAndUpdate()
                        }
                        .show()
                }
            }
        }

        gradeViewModel.getGradeItemById(gradeItemId).observe(viewLifecycleOwner) { item ->
            if (item != null) {
                val isCalculated = !item.formula.isNullOrEmpty()
                adapter.setIsCalculated(isCalculated)
                adapter.setMaxScore(item.maxScore)
            }
        }
    }
}
