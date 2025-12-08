package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
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
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

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
        val editSearch = view.findViewById<EditText>(R.id.editSearchStudent)
        val textStudentCount = view.findViewById<TextView>(R.id.textStudentCount)
        
        // FAB and toggle
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddGrade)
        val btnToggleFab = view.findViewById<ImageButton>(R.id.btnToggleFab)
        
        // Filter chips
        val chipAll = view.findViewById<Chip>(R.id.chipAll)
        val chipGraded = view.findViewById<Chip>(R.id.chipGraded)
        val chipNotGraded = view.findViewById<Chip>(R.id.chipNotGraded)

        titleTextView.text = gradeItemName ?: "Enter Grades"
        
        // FAB toggle functionality
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var isFabVisible = prefs.getBoolean("pref_fab_visible_grade_entry", true)
        
        fun updateFabVisibility() {
            if (isFabVisible) {
                fab.show()
                btnToggleFab.setImageResource(R.drawable.ic_visibility)
                btnToggleFab.alpha = 1.0f
            } else {
                fab.hide()
                btnToggleFab.setImageResource(R.drawable.ic_visibility_off)
                btnToggleFab.alpha = 0.6f
            }
        }
        updateFabVisibility()
        
        btnToggleFab.setOnClickListener {
            isFabVisible = !isFabVisible
            prefs.edit().putBoolean("pref_fab_visible_grade_entry", isFabVisible).apply()
            updateFabVisibility()
        }
        
        // FAB action - scroll to top for now
        fab.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

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

        var currentSortMode = "NAME_ASC"
        var currentFilter = "ALL" // ALL, GRADED, NOT_GRADED
        var searchQuery = ""

        fun updateList(students: List<com.example.additioapp.data.model.StudentEntity>, records: List<GradeRecordEntity>) {
            val items = students.map { student ->
                val record = records.find { it.studentId == student.id }
                StudentGradeItem(student, record)
            }
            
            // Filter by search and grade status
            val filteredItems = items.filter { item ->
                val matchesSearch = item.student.name.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (currentFilter) {
                    "GRADED" -> item.gradeRecord?.score != null
                    "NOT_GRADED" -> item.gradeRecord?.score == null
                    else -> true
                }
                matchesSearch && matchesFilter
            }

            val sortedItems = when (currentSortMode) {
                "NAME_ASC" -> filteredItems.sortedBy { it.student.name }
                "NAME_DESC" -> filteredItems.sortedByDescending { it.student.name }
                "ID_ASC" -> filteredItems.sortedBy { it.student.id }
                else -> filteredItems
            }
            
            textStudentCount.text = "${sortedItems.size} Students"
            adapter.submitList(sortedItems)
        }

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            gradeViewModel.getGradesForItem(gradeItemId).observe(viewLifecycleOwner) { records ->
                
                fun filterAndUpdate() {
                    updateList(students, records)
                }

                editSearch.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        searchQuery = s.toString()
                        filterAndUpdate()
                    }
                })
                
                // Chip filter listeners
                chipAll.setOnClickListener {
                    currentFilter = "ALL"
                    filterAndUpdate()
                }
                chipGraded.setOnClickListener {
                    currentFilter = "GRADED"
                    filterAndUpdate()
                }
                chipNotGraded.setOnClickListener {
                    currentFilter = "NOT_GRADED"
                    filterAndUpdate()
                }

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
