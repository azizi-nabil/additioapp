package com.example.additioapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassWithSummary
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.dialogs.AddClassDialog
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel

class ClassesFragment : Fragment() {

    private val viewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var allClasses: List<ClassWithSummary> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_classes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAddInline = view.findViewById<Button>(R.id.btnAddClassInline)
        val chipGroupFilter = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFilter)
        val btnYearSelector = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnYearSelector)
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val containerClasses = view.findViewById<LinearLayout>(R.id.containerClassesList)
        val containerQuickStats = view.findViewById<LinearLayout>(R.id.containerQuickStats)

        // Stats Views
        val textTotalClasses = view.findViewById<TextView>(R.id.textTotalClasses)
        val textTotalStudents = view.findViewById<TextView>(R.id.textTotalStudents)
        val textArchivedClasses = view.findViewById<TextView>(R.id.textArchivedClasses)

        var selectedYear: String? = null
        var availableYears: List<String> = emptyList()

        // Observe all classes to update stats
        viewModel.allClasses.observe(viewLifecycleOwner) { classes ->
            val activeClasses = classes.filter { !it.isArchived }
            val archivedClasses = classes.filter { it.isArchived }
            
            textTotalClasses.text = activeClasses.size.toString()
            textArchivedClasses.text = archivedClasses.size.toString()
        }

        // Observe students for total count
        studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
            textTotalStudents.text = students.size.toString()
        }

        viewModel.distinctYears.observe(viewLifecycleOwner) { years ->
            availableYears = years.map { it.trim() }.distinct().sortedDescending()
            if (availableYears.isNotEmpty()) {
                if (selectedYear == null || !years.contains(selectedYear)) {
                    selectedYear = years.first()
                }
                btnYearSelector.text = selectedYear
                updateList(chipGroupFilter.checkedChipId, selectedYear, containerClasses, emptyState, containerQuickStats)
            } else {
                btnYearSelector.text = "No Years"
                selectedYear = null
                updateList(chipGroupFilter.checkedChipId, null, containerClasses, emptyState, containerQuickStats)
            }
        }

        btnYearSelector.setOnClickListener {
            if (availableYears.isEmpty()) return@setOnClickListener
            
            val popup = android.widget.PopupMenu(requireContext(), btnYearSelector)
            availableYears.forEach { year ->
                popup.menu.add(year)
            }
            popup.setOnMenuItemClickListener { item ->
                selectedYear = item.title.toString()
                btnYearSelector.text = selectedYear
                updateList(chipGroupFilter.checkedChipId, selectedYear, containerClasses, emptyState, containerQuickStats)
                true
            }
            popup.show()
        }

        chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            updateList(checkedId, selectedYear, containerClasses, emptyState, containerQuickStats)
        }

        val addClassAction = {
            val dialog = AddClassDialog(null) { newClass ->
                viewModel.insertClass(newClass)
            }
            dialog.show(parentFragmentManager, "AddClassDialog")
        }

        btnAddInline.setOnClickListener { addClassAction() }
    }

    private var currentLiveData: androidx.lifecycle.LiveData<List<ClassWithSummary>>? = null

    private fun updateList(
        checkedId: Int, 
        year: String?, 
        container: LinearLayout, 
        emptyState: View,
        quickStatsContainer: LinearLayout
    ) {
        currentLiveData?.removeObservers(viewLifecycleOwner)
        
        if (year == null) {
            container.removeAllViews()
            emptyState.visibility = View.VISIBLE
            renderQuickStats(quickStatsContainer, emptyList())
            return
        }
        
        val newLiveData = when (checkedId) {
            R.id.chipFilterSemester2 -> viewModel.getClassesWithSummaryBySemesterAndYear("Semester 2", year)
            R.id.chipFilterArchived -> viewModel.getArchivedClassesWithSummaryByYear(year)
            else -> viewModel.getClassesWithSummaryBySemesterAndYear("Semester 1", year)
        }
        
        newLiveData.observe(viewLifecycleOwner) { classes ->
            allClasses = classes
            renderClassList(container, classes, emptyState)
            renderQuickStats(quickStatsContainer, classes)
        }
        currentLiveData = newLiveData
    }

    private fun renderClassList(container: LinearLayout, classes: List<ClassWithSummary>, emptyState: View) {
        container.removeAllViews()
        
        if (classes.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            return
        }
        
        emptyState.visibility = View.GONE

        classes.forEach { classSummary ->
            val classEntity = classSummary.classEntity
            val row = layoutInflater.inflate(R.layout.item_home_row, container, false)

            val color = try {
                Color.parseColor(classEntity.color)
            } catch (e: Exception) {
                Color.parseColor("#2196F3")
            }

            row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(color)
            row.findViewById<TextView>(R.id.textRowTitle).text = classEntity.name
            row.findViewById<TextView>(R.id.textRowMeta).text = "${classSummary.studentCount} students • ${classEntity.location.ifEmpty { "No location" }}"
            row.findViewById<TextView>(R.id.textRowExtra).apply {
                text = if (classEntity.semester == "Semester 1") "S1" else "S2"
                visibility = View.VISIBLE
                setBackgroundColor(Color.parseColor("#E3F2FD"))
                setTextColor(Color.parseColor("#1976D2"))
            }

            row.setOnClickListener {
                val bundle = Bundle().apply {
                    putLong("classId", classEntity.id)
                }
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.classDetailFragment, bundle)
            }

            row.setOnLongClickListener {
                showClassOptions(classSummary)
                true
            }

            container.addView(row)
        }
    }

    private fun showClassOptions(classSummary: ClassWithSummary) {
        val classEntity = classSummary.classEntity
        val options = if (classEntity.isArchived) {
            arrayOf("Edit", "Unarchive", "Delete")
        } else {
            arrayOf("Edit", "Archive", "Delete")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(classEntity.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Edit" -> {
                        val dialog = AddClassDialog(classEntity) { updatedClass ->
                            viewModel.updateClass(updatedClass)
                        }
                        dialog.show(parentFragmentManager, "EditClassDialog")
                    }
                    "Archive" -> {
                        viewModel.updateClass(classEntity.copy(isArchived = true))
                    }
                    "Unarchive" -> {
                        viewModel.updateClass(classEntity.copy(isArchived = false))
                    }
                    "Delete" -> {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete Class")
                            .setMessage("Are you sure you want to delete ${classEntity.name}? This cannot be undone.")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteClass(classEntity)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun renderQuickStats(container: LinearLayout, classes: List<ClassWithSummary>) {
        container.removeAllViews()

        if (classes.isEmpty()) {
            addStatRow(container, "📊", "No data", "Select a semester with classes")
            return
        }

        val totalStudents = classes.sumOf { it.studentCount }
        val avgStudents = if (classes.isNotEmpty()) totalStudents / classes.size else 0
        val maxClass = classes.maxByOrNull { it.studentCount }
        val minClass = classes.minByOrNull { it.studentCount }

        addStatRow(container, "👥", "Total Students", totalStudents.toString())
        addStatRow(container, "📈", "Average per Class", avgStudents.toString())
        
        maxClass?.let {
            addStatRow(container, "🔝", "Largest Class", "${it.classEntity.name} (${it.studentCount})")
        }
        
        minClass?.let {
            addStatRow(container, "🔻", "Smallest Class", "${it.classEntity.name} (${it.studentCount})")
        }
    }

    private fun addStatRow(container: LinearLayout, icon: String, label: String, value: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        val iconView = TextView(requireContext()).apply {
            text = icon
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 12 }
        }

        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val valueView = TextView(requireContext()).apply {
            text = value
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#2196F3"))
        }

        row.addView(iconView)
        row.addView(labelView)
        row.addView(valueView)
        container.addView(row)
    }
}
