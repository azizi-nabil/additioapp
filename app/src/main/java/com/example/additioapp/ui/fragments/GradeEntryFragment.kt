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
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var currentStudents: List<com.example.additioapp.data.model.StudentEntity> = emptyList()
    private var gradeAdapter: GradeEntryAdapter? = null

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
        val btnSort = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSort)
        val btnFilter = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilter)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGradeEntry)
        val editSearch = view.findViewById<EditText>(R.id.editSearchStudent)
        val textStudentCount = view.findViewById<TextView>(R.id.textStudentCount)

        titleTextView.text = gradeItemName ?: "Enter Grades"

        gradeAdapter = GradeEntryAdapter(
            onGradeChanged = { item, score, status ->
                val existingId = item.gradeRecord?.id ?: 0L
                // score of -1 means "blank/no grade" - stored as -1 in DB
                val record = GradeRecordEntity(
                    id = existingId,
                    studentId = item.student.id,
                    gradeItemId = gradeItemId,
                    score = score,
                    status = status
                )
                gradeViewModel.saveGradeAndRecalculate(record, classId)
            },
            onAbsenceReport = { item ->
                val dialog = com.example.additioapp.ui.dialogs.AbsenceReportDialog.newInstance(
                    item.student.id, 
                    item.student.displayNameFr, 
                    classId
                )
                dialog.show(parentFragmentManager, "AbsenceReportDialog")
            },
            onBehaviorReport = { item ->
                val dialog = com.example.additioapp.ui.dialogs.BehaviorFullReportDialog.newInstance(
                    item.student.id, 
                    item.student.displayNameFr
                )
                dialog.show(parentFragmentManager, "BehaviorFullReportDialog")
            },
            onGroupGrade = { clickedItem ->
                showGroupMembersBottomSheet(clickedItem.student)
            }
        )

        val editFilterMin = view.findViewById<EditText>(R.id.editFilterMin)
        val editFilterMax = view.findViewById<EditText>(R.id.editFilterMax)
        val btnClearFilter = view.findViewById<View>(R.id.btnClearFilter)

        recyclerView.adapter = gradeAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Load default sort order from preferences
        val sortPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val nameField = sortPrefs.getString("pref_sort_order", "lastname") ?: "lastname"
        var currentSortMode = if (nameField == "firstname") "FIRSTNAME_ASC" else "NAME_ASC"
        var currentFilter = "ALL" // ALL, GRADED, NOT_GRADED, NOT_PRESENT
        var searchQuery = ""
        var filterMin: Float? = null
        var filterMax: Float? = null
        
        val filterOptions = arrayOf("All", "Graded", "Not Graded", "Not Present")
        val filterValues = arrayOf("ALL", "GRADED", "NOT_GRADED", "NOT_PRESENT")

        fun updateList(students: List<com.example.additioapp.data.model.StudentEntity>, records: List<GradeRecordEntity>) {
            val items = students.map { student ->
                val record = records.find { it.studentId == student.id }
                StudentGradeItem(student, record)
            }
            
            // Filter by search, grade status, and min/max range
            val filteredItems = items.filter { item ->
                val matchesSearch = item.student.name.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (currentFilter) {
                    "GRADED" -> item.gradeRecord?.score != null && item.gradeRecord!!.score >= 0
                    "NOT_GRADED" -> item.gradeRecord?.score == null || item.gradeRecord!!.score < 0
                    "NOT_PRESENT" -> item.gradeRecord?.status in listOf("ABSENT", "MISSING", "EXCUSED")
                    else -> true
                }
                
                // Min/Max range filter (only for valid scores >= 0)
                val score = item.gradeRecord?.score
                val matchesRange = if (score == null || score < 0) {
                    // No grade - include if no range filter is set
                    filterMin == null && filterMax == null
                } else {
                    val minOk = filterMin == null || score >= filterMin!!
                    val maxOk = filterMax == null || score <= filterMax!!
                    minOk && maxOk
                }
                
                matchesSearch && matchesFilter && matchesRange
            }

            val sortedItems = when (currentSortMode) {
                "NAME_ASC" -> filteredItems.sortedBy { it.student.lastNameFr.ifEmpty { it.student.name } }
                "NAME_DESC" -> filteredItems.sortedByDescending { it.student.lastNameFr.ifEmpty { it.student.name } }
                "FIRSTNAME_ASC" -> filteredItems.sortedBy { it.student.firstNameFr }
                "FIRSTNAME_DESC" -> filteredItems.sortedByDescending { it.student.firstNameFr }
                "ID_ASC" -> filteredItems.sortedBy { it.student.id }
                "SCORE_ASC" -> filteredItems.sortedBy { it.gradeRecord?.score ?: Float.MAX_VALUE }
                "SCORE_DESC" -> filteredItems.sortedByDescending { it.gradeRecord?.score ?: Float.MIN_VALUE }
                else -> filteredItems
            }
            
            textStudentCount.text = "${sortedItems.size} Students"
            gradeAdapter?.submitList(sortedItems)
        }

        studentViewModel.getStudentsForClass(classId).observe(viewLifecycleOwner) { students ->
            currentStudents = students // Store for group grading
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
                
                // Filter button - shows dialog
                btnFilter.setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Filter By")
                        .setItems(filterOptions) { _, which ->
                            currentFilter = filterValues[which]
                            btnFilter.text = filterOptions[which]
                            filterAndUpdate()
                        }
                        .show()
                }

                // Min filter text change
                editFilterMin.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        filterMin = s.toString().toFloatOrNull()
                        filterAndUpdate()
                    }
                })

                // Max filter text change
                editFilterMax.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        filterMax = s.toString().toFloatOrNull()
                        filterAndUpdate()
                    }
                })

                // Clear min/max filter button
                btnClearFilter.setOnClickListener {
                    editFilterMin.text?.clear()
                    editFilterMax.text?.clear()
                    filterMin = null
                    filterMax = null
                    filterAndUpdate()
                }

                // Initial load
                filterAndUpdate()
                
                btnSort.setOnClickListener {
                    val options = arrayOf(
                        getString(R.string.sort_az),
                        getString(R.string.sort_za),
                        getString(R.string.sort_id_matricule_option),
                        getString(R.string.sort_score_low_high),
                        getString(R.string.sort_score_high_low)
                    )
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.dialog_sort_students_by))
                        .setItems(options) { _, which ->
                            currentSortMode = when (which) {
                                0 -> if (nameField == "firstname") "FIRSTNAME_ASC" else "NAME_ASC"
                                1 -> if (nameField == "firstname") "FIRSTNAME_DESC" else "NAME_DESC"
                                2 -> "ID_ASC"
                                3 -> "SCORE_ASC"
                                4 -> "SCORE_DESC"
                                else -> if (nameField == "firstname") "FIRSTNAME_ASC" else "NAME_ASC"
                            }
                            // Update button text to show current sort
                            btnSort.text = options[which]
                            filterAndUpdate()
                        }
                        .show()
                }
            }
        }

        gradeViewModel.getGradeItemById(gradeItemId).observe(viewLifecycleOwner) { item ->
            if (item != null) {
                val isCalculated = !item.formula.isNullOrEmpty()
                gradeAdapter?.setIsCalculated(isCalculated)
                gradeAdapter?.setMaxScore(item.maxScore)
            }
        }
        
        // Load saved groups from database and highlight them
        val repository = (requireActivity().application as AdditioApplication).repository
        repository.getGroupsForGradeItem(gradeItemId).observe(viewLifecycleOwner) { groups ->
            val groupMap = groups.associate { it.studentId to it.groupNumber }
            gradeAdapter?.setStudentGroups(groupMap)
        }
    }
    
    private fun showGroupMembersBottomSheet(initialStudent: com.example.additioapp.data.model.StudentEntity) {
        val repository = (requireActivity().application as AdditioApplication).repository
        
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_group_members, null)
        
        val recyclerMembers = sheetView.findViewById<RecyclerView>(R.id.recyclerGroupMembers)
        val emptyState = sheetView.findViewById<View>(R.id.emptyState)
        val btnAddMember = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddMember)
        val btnNewGroup = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNewGroup)
        val btnDeleteGroup = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteGroup)
        val tabLayout = sheetView.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutGroups)
        
        recyclerMembers.layoutManager = LinearLayoutManager(requireContext())
        
        var currentGroupNumber = 1
        var allGroups: List<com.example.additioapp.data.model.GradeItemGroupEntity> = emptyList()

        val memberAdapter = com.example.additioapp.ui.adapters.GroupMemberAdapter { studentToRemove ->
            lifecycleScope.launch {
                repository.deleteStudentFromGradeItemGroup(gradeItemId, studentToRemove.id)
            }
        }
        recyclerMembers.adapter = memberAdapter

        fun updateMemberList() {
            val membersInCurrentGroup = allGroups.filter { it.groupNumber == currentGroupNumber }
            val memberIds = membersInCurrentGroup.map { it.studentId }.toSet()
            val members = currentStudents.filter { it.id in memberIds }
            memberAdapter.submitList(members)
            
            if (members.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerMembers.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerMembers.visibility = View.VISIBLE
            }
        }

        fun updateTabs(groups: List<com.example.additioapp.data.model.GradeItemGroupEntity>) {
            val groupNumbers = (groups.map { it.groupNumber } + 1 + currentGroupNumber).distinct().sorted()
            
            // Remember selected tab if possible
            val currentTabGroup = currentGroupNumber
            
            tabLayout.removeAllTabs()
            groupNumbers.forEach { num ->
                val tab = tabLayout.newTab().setText("Group $num")
                tab.tag = num
                tabLayout.addTab(tab)
                if (num == currentTabGroup) {
                    tab.select()
                }
            }
            
            if (tabLayout.selectedTabPosition == -1 && tabLayout.tabCount > 0) {
                tabLayout.getTabAt(0)?.select()
            }
        }

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentGroupNumber = tab?.tag as? Int ?: 1
                updateMemberList()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        repository.getGroupsForGradeItem(gradeItemId).observe(viewLifecycleOwner) { groups ->
            allGroups = groups
            updateTabs(groups)
            updateMemberList()
        }
        
        // Switch to student's group if they are already in one
        lifecycleScope.launch {
            val existingInAnyGroup = repository.getGroupForStudent(gradeItemId, initialStudent.id)
            if (existingInAnyGroup != null) {
                withContext(Dispatchers.Main) {
                    currentGroupNumber = existingInAnyGroup.groupNumber
                    // Tabs will be updated by observer
                }
            }
        }

        btnNewGroup.setOnClickListener {
            lifecycleScope.launch {
                val existingGroups = repository.getGroupsForGradeItemSync(gradeItemId)
                val maxGroup = existingGroups.maxByOrNull { it.groupNumber }?.groupNumber ?: 0
                val nextGroup = maxGroup + 1
                
                withContext(Dispatchers.Main) {
                    currentGroupNumber = nextGroup
                    // Force rebuild tabs to include the new group if it wasn't there
                    updateTabs(allGroups)
                    
                    // Explicitly select the tab for nextGroup
                    for (i in 0 until tabLayout.tabCount) {
                        val tab = tabLayout.getTabAt(i)
                        if (tab?.tag == nextGroup) {
                            tab.select()
                            break
                        }
                    }
                }
            }
        }
        
        btnAddMember.setOnClickListener {
            lifecycleScope.launch {
                val currentGroups = repository.getGroupsForGradeItemSync(gradeItemId)
                val allMemberIds = currentGroups.map { it.studentId }.toSet()
                val availableStudents = currentStudents.filter { it.id !in allMemberIds }
                
                if (availableStudents.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        com.google.android.material.snackbar.Snackbar.make(
                            sheetView, 
                            "All students are already assigned to groups", 
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
                
                val names = availableStudents.map { it.displayNameFr ?: it.name }.toTypedArray()
                val selectedItems = BooleanArray(availableStudents.size)
                
                withContext(Dispatchers.Main) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Add to Group $currentGroupNumber")
                        .setMultiChoiceItems(names, selectedItems) { _, which, isChecked ->
                            selectedItems[which] = isChecked
                        }
                        .setPositiveButton(R.string.action_save) { _, _ ->
                            lifecycleScope.launch {
                                val toAdd = availableStudents.filterIndexed { index, _ -> selectedItems[index] }
                                val entities = toAdd.map {
                                    com.example.additioapp.data.model.GradeItemGroupEntity(
                                        gradeItemId = gradeItemId,
                                        groupNumber = currentGroupNumber,
                                        studentId = it.id
                                    )
                                }
                                repository.insertGradeItemGroups(entities)
                            }
                        }
                        .setNegativeButton(R.string.action_cancel, null)
                        .show()
                }
            }
        }
        
        btnDeleteGroup.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Group $currentGroupNumber?")
                .setMessage("This will remove all members from this group.")
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteGroup(gradeItemId, currentGroupNumber)
                        withContext(Dispatchers.Main) {
                            // After deletion, if it's not group 1, switch back to previous group or group 1
                            if (currentGroupNumber > 1) {
                                currentGroupNumber = 1
                                updateTabs(allGroups)
                            }
                        }
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
        
        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }
}
