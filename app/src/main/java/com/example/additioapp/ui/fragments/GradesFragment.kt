package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.GradeItemAdapter
import com.example.additioapp.ui.dialogs.AddGradeItemDialog
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.ui.viewmodel.GradeViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class GradesFragment : Fragment() {

    private var classId: Long = -1
    private val viewModel: GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private var currentGradeItems: List<GradeItemEntity> = emptyList()

    private val exportLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { exportGradesToCsv(it) }
    }

    private fun exportGradesToCsv(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val repository = (requireActivity().application as AdditioApplication).repository
                
                // Fetch all necessary data
                val students = withContext(Dispatchers.IO) { repository.getStudentsForClassOnce(classId) }
                // Sort by date ASC so the latest is last
                val items = withContext(Dispatchers.IO) { repository.getGradeItemsForClassSync(classId).sortedBy { it.date } }
                val records = withContext(Dispatchers.IO) { repository.getGradeRecordsForClassSync(classId) }

                if (students.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(requireContext(), getString(R.string.toast_no_students), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val writer = outputStream.bufferedWriter(Charsets.UTF_8)
                        
                        // Write UTF-8 BOM for Excel
                        outputStream.write(0xEF)
                        outputStream.write(0xBB)
                        outputStream.write(0xBF)
                        
                        // Header: Matricule, Nom, Prénom, [Items...]
                        val header = mutableListOf("Matricule", "Nom", "Prénom")
                        items.forEach { header.add(it.name) }
                        writer.write(header.joinToString(","))
                        writer.newLine()

                        // Data rows
                        val recordsMap = records.groupBy { it.studentId to it.gradeItemId }
                        
                        students.forEach { student ->
                            val row = mutableListOf<String>()
                            row.add(student.displayMatricule)
                            
                            // Use displayNameFr which combines lastNameFr + firstNameFr
                            // The user stores Arabic names in the French fields
                            val lastName = if (student.lastNameFr.isNotEmpty()) {
                                if (!student.lastNameAr.isNullOrEmpty()) "${student.lastNameFr}/${student.lastNameAr}" else student.lastNameFr
                            } else {
                                student.name.ifEmpty { "N/A" }
                            }
                            
                            val firstName = if (student.firstNameFr.isNotEmpty()) {
                                if (!student.firstNameAr.isNullOrEmpty()) "${student.firstNameFr}/${student.firstNameAr}" else student.firstNameFr
                            } else {
                                ""
                            }
                            
                            // Sanitize for CSV: Replace comma with space, remove newlines
                            fun sanitize(s: String): String = s.replace(",", " ").replace("\n", " ").trim()
                            
                            row.add(sanitize(lastName))
                            row.add(sanitize(firstName))

                            items.forEach { item ->
                                val record = recordsMap[student.id to item.id]?.firstOrNull()
                                val cellValue = if (record != null) {
                                    when (record.status) {
                                        "ABSENT" -> "Absent"
                                        "EXCUSED" -> "Excused"
                                        "MISSING" -> "Missing"
                                        else -> {
                                            // PRESENT
                                            if (record.score >= 0) {
                                                String.format(java.util.Locale.US, "%.2f", record.score).trimEnd('0').trimEnd('.')
                                            } else {
                                                ""
                                            }
                                        }
                                    }
                                } else {
                                    ""
                                }
                                row.add(cellValue)
                            }
                            
                            writer.write(row.joinToString(","))
                            writer.newLine()
                        }
                        writer.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(requireContext(), getString(R.string.excel_export_success, uri.path), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(requireContext(), getString(R.string.excel_export_failed) + ": ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
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
        return inflater.inflate(R.layout.fragment_grades, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGradeItems)
        val emptyStateLayout = view.findViewById<LinearLayout>(R.id.emptyStateLayout)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddGradeItem)
        val btnToggleFab = view.findViewById<android.widget.ImageButton>(R.id.btnToggleFab)

        val btnExportCsv = view.findViewById<android.widget.ImageButton>(R.id.btnExportCsv)
        val textAssessmentCount = view.findViewById<android.widget.TextView>(R.id.textAssessmentCount)
        
        // FAB toggle functionality
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        var isFabVisible = prefs.getBoolean("pref_fab_visible_grades", true)
        
        fun updateFabVisibility() {
            if (isFabVisible) {
                fab.show()
                btnToggleFab.setImageResource(R.drawable.ic_visibility)
                btnToggleFab.contentDescription = getString(R.string.action_toggle_fab_hide)
            } else {
                fab.hide()
                btnToggleFab.setImageResource(R.drawable.ic_visibility_off)
                btnToggleFab.contentDescription = getString(R.string.action_toggle_fab_show)
            }
        }
        updateFabVisibility()
        
        btnToggleFab.setOnClickListener {
            isFabVisible = !isFabVisible
            prefs.edit().putBoolean("pref_fab_visible_grades", isFabVisible).apply()
            updateFabVisibility()
        }

        val adapter = GradeItemAdapter(
            onItemClick = { gradeItem ->
                val bundle = Bundle().apply {
                    putLong("gradeItemId", gradeItem.id)
                    putLong("classId", classId) // Pass classId for context if needed
                    putString("gradeItemName", gradeItem.name)
                }
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.gradeEntryFragment, bundle)
            },
            onMoreClick = { gradeItem, anchor ->
                val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
                val menuView = layoutInflater.inflate(R.layout.bottom_sheet_grade_item_menu, null)
                
                menuView.findViewById<android.widget.TextView>(R.id.textMenuTitle).text = gradeItem.name
                
                menuView.findViewById<android.view.View>(R.id.menuEdit).setOnClickListener {
                    bottomSheet.dismiss()
                    val otherNames = currentGradeItems.filter { it.id != gradeItem.id }.map { it.name }
                    val dialog = AddGradeItemDialog(classId, gradeItem, otherNames) { updatedItem ->
                        viewModel.insertGradeItem(updatedItem)
                    }
                    dialog.show(parentFragmentManager, "EditGradeItemDialog")
                }
                
                menuView.findViewById<android.view.View>(R.id.menuDuplicate).setOnClickListener {
                    bottomSheet.dismiss()
                    showDuplicateDialog(gradeItem)
                }
                
                menuView.findViewById<android.view.View>(R.id.menuDelete).setOnClickListener {
                    bottomSheet.dismiss()
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.dialog_delete_grade_item))
                        .setMessage(getString(R.string.msg_delete_grade_confirm, gradeItem.name))
                        .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                            viewModel.deleteGradeItem(gradeItem)
                        }
                        .setNegativeButton(getString(R.string.action_cancel), null)
                        .show()
                }
                
                bottomSheet.setContentView(menuView)
                bottomSheet.show()
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.getGradeItemsForClass(classId).observe(viewLifecycleOwner) { items ->
            currentGradeItems = items
            // Update Text
            textAssessmentCount.text = getString(R.string.header_assessments_subtitle, items.size)
            
            // Handle Empty State
            if (items.isEmpty()) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.submitList(items)
            }
        }

        fab.setOnClickListener {
            val names = currentGradeItems.map { it.name }
            val dialog = AddGradeItemDialog(classId, null, names) { newItem ->
                val itemWithClassId = newItem.copy(classId = classId)
                viewModel.insertGradeItem(itemWithClassId)
            }
            dialog.show(parentFragmentManager, "AddGradeItemDialog")
        }

        btnExportCsv.setOnClickListener {
            if (currentGradeItems.isNotEmpty()) {
                lifecycleScope.launch {
                    val repository = (requireActivity().application as AdditioApplication).repository
                    val classEntity = withContext(Dispatchers.IO) { repository.getClassById(classId) }
                    val className = classEntity?.name?.replace(" ", "_") ?: "Export"
                    exportLauncher.launch("Grades_${className}.csv")
                }
            } else {
                android.widget.Toast.makeText(requireContext(), getString(R.string.grades_empty), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDuplicateDialog(gradeItem: com.example.additioapp.data.model.GradeItemEntity) {
        val classViewModel: com.example.additioapp.ui.viewmodel.ClassViewModel by viewModels {
            AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
        }
        
        classViewModel.allClasses.observe(viewLifecycleOwner) { classes ->
            // Remove observer after first result to avoid duplicates
            classViewModel.allClasses.removeObservers(viewLifecycleOwner)
            
            // Filter out current class
            val otherClasses = classes.filter { it.id != classId }
            
            if (otherClasses.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), 
                    getString(R.string.msg_no_other_classes), 
                    android.widget.Toast.LENGTH_SHORT).show()
                return@observe
            }
            
            val classNames = otherClasses.map { it.name }.toTypedArray()
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_duplicate_to_class))
                .setItems(classNames) { _, which ->
                    val targetClass = otherClasses[which]
                    
                    // Create duplicated item with new classId and id=0 for auto-generate
                    val duplicatedItem = gradeItem.copy(
                        id = 0,
                        classId = targetClass.id
                    )
                    
                    viewModel.insertGradeItem(duplicatedItem)
                    
                    android.widget.Toast.makeText(requireContext(), 
                        getString(R.string.msg_duplicated_to, targetClass.name), 
                        android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }
    }

    companion object {
        fun newInstance(classId: Long) = GradesFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
