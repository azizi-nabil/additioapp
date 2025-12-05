package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.additioapp.R
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.StudentEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class AttendanceBottomSheetDialog(
    private val student: StudentEntity,
    private val currentRecord: AttendanceRecordEntity?,
    private val onSave: (String, String?) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textName = view.findViewById<TextView>(R.id.textStudentName)
        val textId = view.findViewById<TextView>(R.id.textStudentId)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupStatus)
        val inputComment = view.findViewById<TextInputEditText>(R.id.inputComment)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        textName.text = student.name
        textId.text = "ID: ${student.studentId}"
        inputComment.setText(currentRecord?.comment)

        // Pre-select status
        val status = currentRecord?.status ?: "P" // Default to Present
        when (status) {
            "P" -> view.findViewById<Chip>(R.id.chipPresent).isChecked = true
            "A" -> view.findViewById<Chip>(R.id.chipAbsent).isChecked = true
            "L" -> view.findViewById<Chip>(R.id.chipLate).isChecked = true
            "E" -> view.findViewById<Chip>(R.id.chipExcused).isChecked = true
        }

        btnSave.setOnClickListener {
            val selectedChipId = chipGroup.checkedChipId
            if (selectedChipId != View.NO_ID) {
                val selectedStatus = when (selectedChipId) {
                    R.id.chipPresent -> "P"
                    R.id.chipAbsent -> "A"
                    R.id.chipLate -> "L"
                    R.id.chipExcused -> "E"
                    else -> "P"
                }
                val comment = inputComment.text.toString().takeIf { it.isNotBlank() }
                onSave(selectedStatus, comment)
                dismiss()
            }
        }
    }
}
