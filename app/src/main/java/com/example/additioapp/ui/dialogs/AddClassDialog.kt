package com.example.additioapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity

class AddClassDialog(
    private val classEntity: ClassEntity? = null,
    private val onSave: (ClassEntity) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_class, null)

        val editName = view.findViewById<EditText>(R.id.editClassName)
        val editYear = view.findViewById<EditText>(R.id.editClassYear)
        val editLocation = view.findViewById<EditText>(R.id.editClassLocation)

        val chipGroupSemester = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupSemester)
        val chipSemester1 = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipSemester1)
        val chipSemester2 = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipSemester2)

        if (classEntity != null) {
            editName.setText(classEntity.name)
            editYear.setText(classEntity.year)
            editLocation.setText(classEntity.location)
            if (classEntity.semester == "Semester 2") {
                chipSemester2.isChecked = true
            } else {
                chipSemester1.isChecked = true
            }
        }

        val title = if (classEntity != null) "Edit Class" else "Add New Class"
        val positiveButton = if (classEntity != null) "Save" else "Add"

        builder.setView(view)
            .setTitle(title)
            .setPositiveButton(positiveButton) { _, _ ->
                val name = editName.text.toString()
                val year = editYear.text.toString()
                val location = editLocation.text.toString()
                val selectedSemester = if (chipSemester2.isChecked) "Semester 2" else "Semester 1"

                if (name.isNotEmpty()) {
                    val updatedClass = if (classEntity != null) {
                        classEntity.copy(name = name, year = year, location = location, semester = selectedSemester)
                    } else {
                        ClassEntity(
                            name = name,
                            year = year,
                            location = location,
                            schedule = "",
                            semester = selectedSemester
                        )
                    }
                    onSave(updatedClass)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }
}
