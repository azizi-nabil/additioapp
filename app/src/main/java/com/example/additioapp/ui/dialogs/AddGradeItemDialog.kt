package com.example.additioapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.additioapp.R
import com.example.additioapp.data.model.GradeItemEntity

class AddGradeItemDialog(
    private val classId: Long,
    private val gradeItem: GradeItemEntity? = null,
    private val onSave: (GradeItemEntity) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_grade_item, null)

        val editName = view.findViewById<EditText>(R.id.editGradeItemName)
        val editCategory = view.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.editGradeItemCategory)
        val editMaxScore = view.findViewById<EditText>(R.id.editGradeItemMaxScore)
        val editWeight = view.findViewById<EditText>(R.id.editGradeWeight)
        val editDate = view.findViewById<EditText>(R.id.editGradeItemDate)

        val switchCalculated = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchCalculated)
        val layoutFormulaContainer = view.findViewById<android.widget.LinearLayout>(R.id.layoutFormulaContainer)
        val editFormula = view.findViewById<EditText>(R.id.editFormula)
        val btnFormulaHelp = view.findViewById<android.widget.ImageButton>(R.id.btnFormulaHelp)

        // Setup Category Dropdown
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultCategories = setOf("Exam", "CC", "Test", "Homework", "Project", "Other")
        val categories = prefs.getStringSet("pref_grade_categories", defaultCategories)?.toList() ?: defaultCategories.toList()
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        editCategory.setAdapter(adapter)

        var selectedDate = gradeItem?.date ?: System.currentTimeMillis()
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        editDate.setText(dateFormat.format(java.util.Date(selectedDate)))

        // Pre-fill if editing
        gradeItem?.let { item ->
            editName.setText(item.name)
            editCategory.setText(item.category, false) // false to filter
            editMaxScore.setText(item.maxScore.toString())
            editWeight.setText(item.weight.toString())
            if (!item.formula.isNullOrEmpty()) {
                switchCalculated.isChecked = true
                layoutFormulaContainer.visibility = android.view.View.VISIBLE
                editFormula.setText(item.formula)
            }
        }

        switchCalculated.setOnCheckedChangeListener { _, isChecked ->
            layoutFormulaContainer.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Formula Help Button
        btnFormulaHelp.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("📐 Formula Help")
                .setMessage("""
                    |Use grade item names as variables (spaces are removed).
                    |
                    |📊 Functions:
                    |• avg(a, b, ...) - Average
                    |• max(a, b, ...) - Maximum
                    |• min(a, b, ...) - Minimum
                    |
                    |➕ Operators:
                    |• + - * / ( )
                    |
                    |📋 Attendance Variables:
                    |• abs-td - TD absences count
                    |• abs-tp - TP absences count
                    |• pres-cours - Course presences
                    |• tot-td - Total TD sessions
                    |• tot-tp - Total TP sessions
                    |• tot-c - Total Course sessions
                    |
                    |📝 Examples:
                    |• avg(Test1, Test2, Exam)
                    |• max(Quiz1, Quiz2)
                    |• CC * 0.4 + Exam * 0.6
                    |• 20 - abs-td * 2
                    |• pres-cours / tot-c * 20
                """.trimMargin())
                .setPositiveButton("OK", null)
                .show()
        }

        editDate.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedDate)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                editDate.setText(dateFormat.format(java.util.Date(selectedDate)))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        builder.setView(view)
            .setTitle(if (gradeItem == null) "Add Grade Item" else "Edit Grade Item")
            .setPositiveButton("Save") { _, _ ->
                val name = editName.text.toString()
                val category = editCategory.text.toString()
                val maxScoreStr = editMaxScore.text.toString()
                val weightStr = editWeight.text.toString()
                val isCalculated = switchCalculated.isChecked
                val formula = if (isCalculated) editFormula.text.toString() else null
                
                if (name.isNotEmpty() && maxScoreStr.isNotEmpty()) {
                    val maxScore = maxScoreStr.toFloatOrNull() ?: 100f
                    val weight = weightStr.toFloatOrNull() ?: 1.0f
                    
                    val newItem = GradeItemEntity(
                        id = gradeItem?.id ?: 0L, // Preserve ID if editing
                        classId = classId,
                        name = name,
                        category = category,
                        maxScore = maxScore,
                        weight = weight,
                        date = selectedDate,
                        formula = formula
                    )
                    onSave(newItem)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }
}
