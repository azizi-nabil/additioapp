package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.GradeViewModel
import com.example.additioapp.data.model.StudentGradeDetail
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

class GradesReportDialog : BottomSheetDialogFragment() {

    private val gradeViewModel: GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var studentId: Long = -1
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            studentId = it.getLong("studentId")
            studentName = it.getString("studentName", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_grades_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textName = view.findViewById<TextView>(R.id.textStudentName)
        val layoutList = view.findViewById<LinearLayout>(R.id.layoutGradesList)
        val textNoGrades = view.findViewById<TextView>(R.id.textNoGrades)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        textName.text = studentName

        gradeViewModel.getStudentGradeDetails(studentId).observe(viewLifecycleOwner) { grades ->
            layoutList.removeAllViews()
            layoutList.addView(textNoGrades) // Keep the "No grades" view

            if (grades.isEmpty()) {
                textNoGrades.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.textOverallGrade).text = "-- / 20"
            } else {
                textNoGrades.visibility = View.GONE
                val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val decimalFormat = java.text.DecimalFormat("#.##")

                // Find calculated item for Continuous calendar (CC)
                val calculatedItem = grades.find { it.isCalculated }
                
                // Find Exam item
                val examItem = grades.find { it.category.trim().equals("Exam", ignoreCase = true) }

                // Filter out CC and Exam from the list
                val listItems = grades.filter { !it.isCalculated && !it.category.trim().equals("Exam", ignoreCase = true) }

                listItems.forEach { grade ->
                    // Inflate Item
                    val itemView = layoutInflater.inflate(R.layout.item_grade_report, layoutList, false)
                    
                    itemView.findViewById<TextView>(R.id.textGradeName).text = grade.gradeName
                    itemView.findViewById<TextView>(R.id.textGradeDate).text = fmt.format(grade.date)
                    itemView.findViewById<TextView>(R.id.textGradeScore).text = decimalFormat.format(grade.score)
                    itemView.findViewById<TextView>(R.id.textGradeMax).text = " / ${decimalFormat.format(grade.maxScore)}"
                    itemView.findViewById<TextView>(R.id.textGradeWeight).text = "Weight: ${decimalFormat.format(grade.weight)}"

                    layoutList.addView(itemView)
                }

                // Set CC Header Score
                if (calculatedItem != null) {
                    view.findViewById<TextView>(R.id.textOverallGrade).text = "${decimalFormat.format(calculatedItem.score)} / ${decimalFormat.format(calculatedItem.maxScore)}"
                } else {
                    view.findViewById<TextView>(R.id.textOverallGrade).text = "-- / 20"
                }

                // Set Exam Header Score
                if (examItem != null) {
                    view.findViewById<TextView>(R.id.textExamGrade).text = "${decimalFormat.format(examItem.score)} / ${decimalFormat.format(examItem.maxScore)}"
                } else {
                    view.findViewById<TextView>(R.id.textExamGrade).text = "-- / 20"
                }
            }
        }

        btnClose.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance(studentId: Long, studentName: String) = GradesReportDialog().apply {
            arguments = Bundle().apply {
                putLong("studentId", studentId)
                putString("studentName", studentName)
            }
        }
    }
}
