package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.additioapp.data.model.StudentAbsenceDetail

class AbsenceReportDialog : BottomSheetDialogFragment() {

    private val attendanceViewModel: AttendanceViewModel by viewModels {
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
        return inflater.inflate(R.layout.dialog_absence_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textName = view.findViewById<TextView>(R.id.textStudentName)
        val textHeaderTD = view.findViewById<TextView>(R.id.textHeaderTD)
        val textListTD = view.findViewById<TextView>(R.id.textListTD)
        val textHeaderTP = view.findViewById<TextView>(R.id.textHeaderTP)
        val textListTP = view.findViewById<TextView>(R.id.textListTP)
        val textHeaderCours = view.findViewById<TextView>(R.id.textHeaderCours)
        val textListCours = view.findViewById<TextView>(R.id.textListCours)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        textName.text = studentName

        attendanceViewModel.getAbsencesForStudent(studentId).observe(viewLifecycleOwner) { absences ->
            val fmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            val greenColor = android.graphics.Color.parseColor("#388E3C")

            fun formatAbsenceList(list: List<StudentAbsenceDetail>): CharSequence {
                val builder = android.text.SpannableStringBuilder()
                list.forEachIndexed { index, absence ->
                    if (index > 0) builder.append("\n")
                    
                    val dateStr = fmt.format(absence.date)
                    builder.append(dateStr)
                    
                    if (absence.status == "E") {
                        val start = builder.length
                        builder.append(" (Excused)")
                        builder.setSpan(
                            android.text.style.ForegroundColorSpan(greenColor),
                            start,
                            builder.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                         builder.setSpan(
                            android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                            start,
                            builder.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                return builder
            }
            
            val tdAbsences = absences.filter { it.type == "TD" }
            val tpAbsences = absences.filter { it.type == "TP" }
            val coursAbsences = absences.filter { it.type == "Cours" }

            // TD
            textHeaderTD.text = "TD Absences (${tdAbsences.size})"
            if (tdAbsences.isNotEmpty()) {
                textListTD.text = formatAbsenceList(tdAbsences)
                textListTD.visibility = View.VISIBLE
            } else {
                textListTD.text = "None"
                textListTD.visibility = View.GONE
            }

            // TP
            textHeaderTP.text = "TP Absences (${tpAbsences.size})"
            if (tpAbsences.isNotEmpty()) {
                textListTP.text = formatAbsenceList(tpAbsences)
                textListTP.visibility = View.VISIBLE
            } else {
                textListTP.text = "None"
                textListTP.visibility = View.GONE
            }
            
            // Cours
            if (coursAbsences.isNotEmpty()) {
                textHeaderCours.visibility = View.VISIBLE
                textListCours.visibility = View.VISIBLE
                textHeaderCours.text = "Cours Absences (${coursAbsences.size})"
                textListCours.text = formatAbsenceList(coursAbsences)
            } else {
                textHeaderCours.visibility = View.GONE
                textListCours.visibility = View.GONE
            }
        }

        btnClose.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance(studentId: Long, studentName: String) = AbsenceReportDialog().apply {
            arguments = Bundle().apply {
                putLong("studentId", studentId)
                putString("studentName", studentName)
            }
        }
    }
}
