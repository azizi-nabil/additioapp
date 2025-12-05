package com.example.additioapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.additioapp.R
import com.example.additioapp.data.model.BehaviorRecordEntity
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddBehaviorDialog(
    private val studentId: Long,
    private val classId: Long,
    private val onAddBehavior: (BehaviorRecordEntity) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_behavior, null)

        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupType)
        val editCategory = view.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.editBehaviorCategory)
        val editComment = view.findViewById<EditText>(R.id.editBehaviorComment)

        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        
        val defaultPositive = setOf("Active Participation", "Helping Others", "Homework Completed",
            "Respectful Behavior", "Prepared for Class", "Leadership / Takes Initiative", "Other")
        val positiveCategories = prefs.getStringSet("pref_positive_behaviors", defaultPositive)?.toList() ?: defaultPositive.toList()

        val defaultNegative = setOf("Disturbance / Disrupting Class", "No Homework", "Late Arrival",
            "Disrespect (teacher/peers)", "Off-Task", "Using Phone", "Other")
        val negativeCategories = prefs.getStringSet("pref_negative_behaviors", defaultNegative)?.toList() ?: defaultNegative.toList()

        fun updateCategories(isPositive: Boolean) {
            val categories = if (isPositive) positiveCategories else negativeCategories
            val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
            editCategory.setAdapter(adapter)
            editCategory.setText(categories.first(), false)
        }

        // Initial setup
        updateCategories(true)

        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.chipPositive) {
                updateCategories(true)
            } else if (checkedId == R.id.chipNegative) {
                updateCategories(false)
            }
        }

        builder.setView(view)
            .setTitle("Add Behavior")
            .setPositiveButton("Add") { _, _ ->
                val selectedId = chipGroup.checkedChipId
                val points = when (selectedId) {
                    R.id.chipPositive -> 1
                    R.id.chipNegative -> -1
                    else -> 0
                }
                val comment = editComment.text.toString()
                val category = editCategory.text.toString()

                if (points != 0) {
                    val type = if (points > 0) "POSITIVE" else "NEGATIVE"
                    val record = BehaviorRecordEntity(
                        studentId = studentId,
                        classId = classId,
                        type = type,
                        category = category,
                        points = points,
                        comment = comment,
                        date = System.currentTimeMillis()
                    )
                    onAddBehavior(record)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }
}
