package com.example.additioapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity

class AddStudentDialog(
    private val classId: Long,
    private val studentEntity: StudentEntity? = null,
    private val onSave: (StudentEntity) -> Unit,
    private val onDelete: ((StudentEntity) -> Unit)? = null
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_student, null)

        val editMatricule = view.findViewById<EditText>(R.id.editMatricule)
        val editLastNameFr = view.findViewById<EditText>(R.id.editLastNameFr)
        val editFirstNameFr = view.findViewById<EditText>(R.id.editFirstNameFr)
        val editLastNameAr = view.findViewById<EditText>(R.id.editLastNameAr)
        val editFirstNameAr = view.findViewById<EditText>(R.id.editFirstNameAr)
        val editNotes = view.findViewById<EditText>(R.id.editNotes)

        if (studentEntity != null) {
            editMatricule.setText(studentEntity.displayMatricule)
            editLastNameFr.setText(studentEntity.lastNameFr.ifEmpty { studentEntity.name })
            editFirstNameFr.setText(studentEntity.firstNameFr)
            editLastNameAr.setText(studentEntity.lastNameAr ?: "")
            editFirstNameAr.setText(studentEntity.firstNameAr ?: "")
            editNotes.setText(studentEntity.notes ?: "")
        }

        val title = if (studentEntity != null) "Edit Student" else "Add New Student"
        val positiveButton = if (studentEntity != null) "Save" else "Add"

        builder.setView(view)
            .setTitle(title)
            .setPositiveButton(positiveButton) { _, _ ->
                val matricule = editMatricule.text.toString().trim()
                val lastNameFr = editLastNameFr.text.toString().trim()
                val firstNameFr = editFirstNameFr.text.toString().trim()
                val lastNameAr = editLastNameAr.text.toString().trim().ifEmpty { null }
                val firstNameAr = editFirstNameAr.text.toString().trim().ifEmpty { null }
                val notes = editNotes.text.toString().trim().ifEmpty { null }

                // At least last name is required
                if (lastNameFr.isNotEmpty() || firstNameFr.isNotEmpty()) {
                    // Compute legacy name for backward compatibility
                    val legacyName = "$lastNameFr $firstNameFr".trim()
                    
                    val updatedStudent = if (studentEntity != null) {
                        studentEntity.copy(
                            matricule = matricule,
                            firstNameFr = firstNameFr,
                            lastNameFr = lastNameFr,
                            firstNameAr = firstNameAr,
                            lastNameAr = lastNameAr,
                            name = legacyName,
                            studentId = matricule,
                            notes = notes
                        )
                    } else {
                        StudentEntity(
                            classId = classId,
                            matricule = matricule,
                            firstNameFr = firstNameFr,
                            lastNameFr = lastNameFr,
                            firstNameAr = firstNameAr,
                            lastNameAr = lastNameAr,
                            name = legacyName,
                            studentId = matricule,
                            notes = notes
                        )
                    }
                    onSave(updatedStudent)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        if (studentEntity != null && onDelete != null) {
            builder.setNeutralButton("Delete") { _, _ ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Student")
                    .setMessage("Are you sure you want to delete ${studentEntity.displayNameFr}?")
                    .setPositiveButton("Delete") { _, _ ->
                        onDelete.invoke(studentEntity)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        return builder.create()
    }
}
