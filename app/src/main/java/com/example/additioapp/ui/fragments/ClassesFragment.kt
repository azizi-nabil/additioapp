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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
        val btnYearSelectorContainer = view.findViewById<View>(R.id.btnYearSelectorContainer)
        val textYearSelector = view.findViewById<TextView>(R.id.textYearSelector)
        val chipGroupFilter = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFilter)
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val containerClasses = view.findViewById<LinearLayout>(R.id.containerClassesList)
        val containerQuickStats = view.findViewById<LinearLayout>(R.id.containerQuickStats)
        val textEmptyTitle = view.findViewById<TextView>(R.id.textEmptyTitle)
        val textEmptySubtitle = view.findViewById<TextView>(R.id.textEmptySubtitle)

        // Stats Views
        val textTotalClasses = view.findViewById<TextView>(R.id.textTotalClasses)
        val textTotalStudents = view.findViewById<TextView>(R.id.textTotalStudents)
        val textArchivedClasses = view.findViewById<TextView>(R.id.textArchivedClasses)

        var selectedYear: String? = null
        var availableYears: List<String> = emptyList()
        var allClassesList: List<com.example.additioapp.data.model.ClassEntity> = emptyList()

        // Function to update stats based on selected year
        fun updateStats() {
            val yearClasses = if (selectedYear != null) {
                allClassesList.filter { it.year == selectedYear }
            } else {
                allClassesList
            }
            val activeClasses = yearClasses.filter { !it.isArchived }
            val archivedClasses = yearClasses.filter { it.isArchived }
            
            textTotalClasses.text = activeClasses.size.toString()
            textArchivedClasses.text = archivedClasses.size.toString()
        }

        // Observe all classes (including archived) to update stats
        viewModel.allClassesIncludingArchived.observe(viewLifecycleOwner) { classes ->
            allClassesList = classes
            updateStats()
        }

        // Observe students for total count
        studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
            textTotalStudents.text = students.size.toString()
        }

        // Update empty state and Add button based on filter
        fun updateEmptyStateAndAddButton(checkedId: Int) {
            val isArchive = checkedId == R.id.chipFilterArchived
            btnAddInline.visibility = if (isArchive) View.GONE else View.VISIBLE
            
            if (isArchive) {
                textEmptyTitle.text = "No archived classes"
                textEmptySubtitle.text = "Long-press a class to archive it"
            } else {
                textEmptyTitle.text = "No classes yet"
                textEmptySubtitle.text = "Tap + Add to create your first class"
            }
        }

        viewModel.distinctYears.observe(viewLifecycleOwner) { years ->
            availableYears = years.map { it.trim() }.distinct().sortedDescending()
            if (availableYears.isNotEmpty()) {
                if (selectedYear == null || !years.contains(selectedYear)) {
                    selectedYear = years.first()
                }
                textYearSelector.text = selectedYear
                updateStats()
                updateEmptyStateAndAddButton(chipGroupFilter.checkedChipId)
                updateList(chipGroupFilter.checkedChipId, selectedYear, containerClasses, emptyState, containerQuickStats)
            } else {
                textYearSelector.text = "No Years"
                selectedYear = null
                updateStats()
                updateList(chipGroupFilter.checkedChipId, null, containerClasses, emptyState, containerQuickStats)
            }
        }

        btnYearSelectorContainer.setOnClickListener {
            if (availableYears.isEmpty()) return@setOnClickListener
            
            val popup = android.widget.PopupMenu(requireContext(), btnYearSelectorContainer)
            availableYears.forEach { year ->
                popup.menu.add(year)
            }
            popup.setOnMenuItemClickListener { item ->
                selectedYear = item.title.toString()
                textYearSelector.text = selectedYear
                updateStats()
                updateList(chipGroupFilter.checkedChipId, selectedYear, containerClasses, emptyState, containerQuickStats)
                true
            }
            popup.show()
        }

        chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            updateEmptyStateAndAddButton(checkedId)
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
            val row = layoutInflater.inflate(R.layout.item_class, container, false) // Use item_class

            val color = try {
                Color.parseColor(classEntity.color)
            } catch (e: Exception) {
                Color.parseColor("#2196F3")
            }

            // Bind data to item_class views
            row.findViewById<View>(R.id.colorStrip).setBackgroundColor(color)
            row.findViewById<TextView>(R.id.textClassName).text = classEntity.name
            
            // Location
            val locationView = row.findViewById<TextView>(R.id.textClassLocation)
            if (classEntity.location.isNotEmpty()) {
                locationView.text = "📍 ${classEntity.location}"
                locationView.visibility = View.VISIBLE
            } else {
                locationView.visibility = View.GONE
            }

            // Year Tag
            val yearView = row.findViewById<TextView>(R.id.textClassYear)
            yearView.text = classEntity.year
            yearView.visibility = if (classEntity.year.isNotEmpty()) View.VISIBLE else View.GONE

            // Semester Tag
            val semesterView = row.findViewById<TextView>(R.id.textClassSemester)
            semesterView.text = if (classEntity.semester == "Semester 1") "S1" else "S2"
            semesterView.background.setTint(if (classEntity.semester == "Semester 1") Color.parseColor("#2196F3") else Color.parseColor("#9C27B0"))

            // Student Count
            row.findViewById<TextView>(R.id.textStudentCount).text = classSummary.studentCount.toString()

            // Notes Icon
            val iconHasNotes = row.findViewById<View>(R.id.iconHasNotes)
            if (classSummary.noteCount > 0) {
                iconHasNotes.visibility = View.VISIBLE
                iconHasNotes.setOnClickListener {
                    val dialog = com.example.additioapp.ui.dialogs.ClassNotesDialog.newInstance(classEntity.id, classEntity.name)
                    dialog.show(parentFragmentManager, "ClassNotesDialog")
                }
            } else {
                iconHasNotes.visibility = View.GONE
            }

            // Click listener on the CardView itself
            row.setOnClickListener {
                val bundle = Bundle().apply {
                    putLong("classId", classEntity.id)
                }
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.classDetailFragment, bundle)
            }

            // Menu Options Click
            row.findViewById<View>(R.id.btnMoreOptions).setOnClickListener {
                showClassOptions(classSummary)
            }
            
            // Long click on card also shows options
            row.setOnLongClickListener {
                showClassOptions(classSummary)
                true
            }

            container.addView(row)
        }
    }

    private fun showClassOptions(classSummary: ClassWithSummary) {
        val classEntity = classSummary.classEntity
        val isArchived = classEntity.isArchived
        
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val menuView = layoutInflater.inflate(R.layout.bottom_sheet_class_menu, null)
        
        // Set class name in header
        menuView.findViewById<android.widget.TextView>(R.id.textMenuTitle).text = classEntity.name
        
        // Update archive/unarchive icon and text based on state
        val iconArchive = menuView.findViewById<android.widget.ImageView>(R.id.iconArchive)
        val textArchive = menuView.findViewById<android.widget.TextView>(R.id.textArchive)
        if (isArchived) {
            iconArchive.setImageResource(R.drawable.ic_unarchive)
            textArchive.text = getString(R.string.action_unarchive)
        } else {
            iconArchive.setImageResource(R.drawable.ic_archive)
            textArchive.text = getString(R.string.action_archive)
        }
        
        // Set click listeners
        menuView.findViewById<android.view.View>(R.id.menuEdit).setOnClickListener {
            bottomSheet.dismiss()
            val dialog = AddClassDialog(classEntity) { updatedClass ->
                viewModel.updateClass(updatedClass)
            }
            dialog.show(parentFragmentManager, "EditClassDialog")
        }
        
        menuView.findViewById<android.view.View>(R.id.menuDuplicate).setOnClickListener {
            bottomSheet.dismiss()
            duplicateClass(classEntity)
        }
        
        menuView.findViewById<android.view.View>(R.id.menuNotes).setOnClickListener {
            bottomSheet.dismiss()
            val dialog = com.example.additioapp.ui.dialogs.ClassNotesDialog.newInstance(classEntity.id, classEntity.name)
            dialog.show(parentFragmentManager, "ClassNotesDialog")
        }
        
        menuView.findViewById<android.view.View>(R.id.menuArchive).setOnClickListener {
            bottomSheet.dismiss()
            viewModel.updateClass(classEntity.copy(isArchived = !isArchived))
        }
        
        menuView.findViewById<android.view.View>(R.id.menuDelete).setOnClickListener {
            bottomSheet.dismiss()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to delete ${classEntity.name}? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteClass(classEntity)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        bottomSheet.setContentView(menuView)
        bottomSheet.show()
    }

    private fun duplicateClass(classEntity: com.example.additioapp.data.model.ClassEntity) {
        lifecycleScope.launch {
            // Create new class with "(Copy)" suffix
            val newClass = classEntity.copy(
                id = 0,
                name = "${classEntity.name} (Copy)"
            )
            
            // Insert new class and get its ID
            val newClassId = viewModel.insertClassAndGetId(newClass)
            
            // Get all students from original class
            val students = studentViewModel.getStudentsForClassOnce(classEntity.id)
            
            // Copy students to new class
            students.forEach { student ->
                val newStudent = student.copy(
                    id = 0,
                    classId = newClassId
                )
                studentViewModel.insertStudent(newStudent)
            }
            
            android.widget.Toast.makeText(
                requireContext(),
                "Duplicated \"${classEntity.name}\" with ${students.size} students",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
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
