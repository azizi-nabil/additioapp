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

        val editName = view.findViewById<EditText>(R.id.editStudentName)
        val editId = view.findViewById<EditText>(R.id.editStudentId)

        if (studentEntity != null) {
            editName.setText(studentEntity.name)
            editId.setText(studentEntity.studentId)
        }

        val title = if (studentEntity != null) "Edit Student" else "Add New Student"
        val positiveButton = if (studentEntity != null) "Save" else "Add"

        builder.setView(view)
            .setTitle(title)
            .setPositiveButton(positiveButton) { _, _ ->
                val name = editName.text.toString()
                val studentId = editId.text.toString()

                if (name.isNotEmpty()) {
                    val updatedStudent = if (studentEntity != null) {
                        studentEntity.copy(name = name, studentId = studentId)
                    } else {
                        StudentEntity(
                            classId = classId,
                            name = name,
                            studentId = studentId
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
                    .setMessage("Are you sure you want to delete ${studentEntity.name}?")
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
