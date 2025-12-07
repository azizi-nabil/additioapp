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
import com.example.additioapp.ui.adapters.BehaviorAdapter
import com.example.additioapp.ui.adapters.StudentBehaviorItem
import com.example.additioapp.ui.viewmodel.BehaviorViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel

class BehaviorFragment : Fragment() {

    private var classId: Long = -1
    private val behaviorViewModel: BehaviorViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
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
        return inflater.inflate(R.layout.fragment_behavior, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewBehavior)
        val btnSort = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSort)
        val btnFilter = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilter)
        val textTotalStudents = view.findViewById<android.widget.TextView>(R.id.textTotalStudents)
        val editSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editSearchStudent)

        val adapter = BehaviorAdapter { student ->
            val bottomSheet = com.example.additioapp.ui.dialogs.StudentBehaviorBottomSheet.newInstance(student.id, classId, student.name)
            bottomSheet.show(parentFragmentManager, "StudentBehaviorBottomSheet")
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        var currentStudents: List<com.example.additioapp.data.model.StudentEntity> = emptyList()
        var currentBehaviors: List<com.example.additioapp.data.model.BehaviorRecordEntity> = emptyList()
        var sortMode = "NAME_ASC" // NAME_ASC, NAME_DESC, ID_ASC
        var filterMode = "ALL" // ALL, POSITIVE, NEGATIVE
        var searchQuery = ""

        fun updateList() {
            val filteredStudents = currentStudents.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }

            val items = filteredStudents.map { student ->
                val studentBehaviors = currentBehaviors.filter { it.studentId == student.id }
                val positive = studentBehaviors.filter { it.points > 0 }.sumOf { it.points }
                val negative = studentBehaviors.filter { it.points < 0 }.sumOf { it.points }
                StudentBehaviorItem(student, positive, negative)
            }

            // Apply behavior filter
            val filteredItems = when (filterMode) {
                "POSITIVE" -> items.filter { it.positivePoints != 0 }
                "NEGATIVE" -> items.filter { it.negativePoints != 0 }
                else -> items // ALL
            }

            val sortedItems = when (sortMode) {
                "NAME_ASC" -> filteredItems.sortedBy { it.student.name }
                "NAME_DESC" -> filteredItems.sortedByDescending { it.student.name }
                "ID_ASC" -> filteredItems.sortedBy { it.student.id }
                else -> filteredItems
            }

            adapter.submitList(sortedItems)
            textTotalStudents.text = "${sortedItems.size} Students"

            val sortText = when (sortMode) {
                "NAME_ASC" -> "Sort by Name (A-Z)"
                "NAME_DESC" -> "Sort by Name (Z-A)"
                "ID_ASC" -> "Sort by ID"
                else -> "Sort"
            }
            btnSort.text = sortText

            val filterText = when (filterMode) {
                "POSITIVE" -> "Pos"
                "NEGATIVE" -> "Neg"
                else -> "All"
            }
            btnFilter.text = filterText
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
                    updateList()
                }
                .show()
        }

        btnFilter.setOnClickListener {
            val options = arrayOf("All Students", "Positive Behavior (≠0)", "Negative Behavior (≠0)")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Filter Students By Behavior")
                .setItems(options) { _, which ->
                    filterMode = when (which) {
                        0 -> "ALL"
                        1 -> "POSITIVE"
                        2 -> "NEGATIVE"
                        else -> "ALL"
                    }
                    updateList()
                }
                .show()
        }

        editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchQuery = s.toString()
                updateList()
            }
        })

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            currentStudents = students
            updateList()
        }

        behaviorViewModel.getBehaviorsForClass(classId).observe(viewLifecycleOwner) { behaviors ->
            currentBehaviors = behaviors
            updateList()
        }
    }

    companion object {
        fun newInstance(classId: Long) = BehaviorFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
