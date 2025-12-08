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
        // Load default sort order from preferences
        val sortPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val nameField = sortPrefs.getString("pref_sort_order", "lastname") ?: "lastname"
        var sortMode = if (nameField == "firstname") "FIRSTNAME_ASC" else "NAME_ASC"
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
                "NAME_ASC" -> filteredItems.sortedBy { it.student.lastNameFr.ifEmpty { it.student.name } }
                "NAME_DESC" -> filteredItems.sortedByDescending { it.student.lastNameFr.ifEmpty { it.student.name } }
                "FIRSTNAME_ASC" -> filteredItems.sortedBy { it.student.firstNameFr }
                "FIRSTNAME_DESC" -> filteredItems.sortedByDescending { it.student.firstNameFr }
                "ID_ASC" -> filteredItems.sortedBy { it.student.id }
                else -> filteredItems
            }

            adapter.submitList(sortedItems)
            textTotalStudents.text = getString(R.string.msg_student_count_simple, sortedItems.size)

            val sortText = when (sortMode) {
                "NAME_ASC", "FIRSTNAME_ASC" -> getString(R.string.sort_display_az)
                "NAME_DESC", "FIRSTNAME_DESC" -> getString(R.string.sort_display_za)
                "ID_ASC" -> getString(R.string.sort_display_id)
                else -> getString(R.string.sort_default)
            }
            btnSort.text = sortText

            val filterText = when (filterMode) {
                "POSITIVE" -> getString(R.string.filter_pos)
                "NEGATIVE" -> getString(R.string.filter_neg)
                else -> getString(R.string.filter_all)
            }
            btnFilter.text = filterText
        }

        btnSort.setOnClickListener {
            val options = arrayOf(
                getString(R.string.sort_az),
                getString(R.string.sort_za),
                getString(R.string.sort_id_matricule_option)
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_sort_students_by))
                .setItems(options) { _, which ->
                    sortMode = when (which) {
                        0 -> if (nameField == "firstname") "FIRSTNAME_ASC" else "NAME_ASC"
                        1 -> if (nameField == "firstname") "FIRSTNAME_DESC" else "NAME_DESC"
                        2 -> "ID_ASC"
                        else -> if (nameField == "firstname") "FIRSTNAME_ASC" else "NAME_ASC"
                    }
                    updateList()
                }
                .show()
        }

        btnFilter.setOnClickListener {
            val options = arrayOf(getString(R.string.filter_all_students_option), getString(R.string.filter_pos_option), getString(R.string.filter_neg_option))
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_filter_behavior_title))
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
