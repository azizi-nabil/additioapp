package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.additioapp.data.model.StudentAbsenceDetail
import kotlinx.coroutines.launch

class AbsenceReportDialog : BottomSheetDialogFragment() {

    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var studentId: Long = -1
    private var studentName: String = ""
    private var classId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            studentId = it.getLong("studentId")
            studentName = it.getString("studentName", "")
            classId = it.getLong("classId", -1)
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
        // Cours card removed per user request
        val cardCoursPresence = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardCoursPresence)
        val textHeaderCoursPresence = view.findViewById<TextView>(R.id.textHeaderCoursPresence)
        val textListCoursPresence = view.findViewById<TextView>(R.id.textListCoursPresence)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        textName.text = studentName

        // Load session totals for TD, TP, and Cours
        lifecycleScope.launch {
            val totalTD = if (classId > 0) attendanceViewModel.getTotalSessionCountByType(classId, "TD") else 0
            val totalTP = if (classId > 0) attendanceViewModel.getTotalSessionCountByType(classId, "TP") else 0
            val totalCours = if (classId > 0) attendanceViewModel.getTotalCoursSessionCount(classId) else 0
            
            attendanceViewModel.getAbsencesForStudent(studentId).observe(viewLifecycleOwner) { absences ->
                val fmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                val greenColor = android.graphics.Color.parseColor("#388E3C")

                fun formatAbsenceList(list: List<StudentAbsenceDetail>): CharSequence {
                    val builder = android.text.SpannableStringBuilder()
                    list.forEachIndexed { index, absence ->
                        if (index > 0) builder.append("\n")
                        
                        val dateStr = fmt.format(absence.date)
                        builder.append(dateStr)
                        
                        if (absence.status == "E") {
                            val start = builder.length
                            builder.append(getString(R.string.absence_excused_suffix))
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

                // TD with total
                val tdHeader = if (totalTD > 0) {
                    getString(R.string.label_td_absences_total, tdAbsences.size, totalTD)
                } else {
                    getString(R.string.label_td_absences, tdAbsences.size)
                }
                textHeaderTD.text = tdHeader
                if (tdAbsences.isNotEmpty()) {
                    textListTD.text = formatAbsenceList(tdAbsences)
                    textListTD.visibility = View.VISIBLE
                } else {
                    textListTD.text = getString(R.string.label_none)
                    textListTD.visibility = View.GONE
                }

                // TP with total
                val tpHeader = if (totalTP > 0) {
                    getString(R.string.label_tp_absences_total, tpAbsences.size, totalTP)
                } else {
                    getString(R.string.label_tp_absences, tpAbsences.size)
                }
                textHeaderTP.text = tpHeader
                if (tpAbsences.isNotEmpty()) {
                    textListTP.text = formatAbsenceList(tpAbsences)
                    textListTP.visibility = View.VISIBLE
                } else {
                    textListTP.text = getString(R.string.label_none)
                    textListTP.visibility = View.GONE
                }
                
                // Cours absences card removed per user request
            }
            
            // Observe Cours presence separately
            attendanceViewModel.getCoursPresenceForStudent(studentId, classId).observe(viewLifecycleOwner) { presences ->
                val fmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                
                fun formatDateList(list: List<StudentAbsenceDetail>): String {
                    return list.joinToString("\n") { fmt.format(it.date) }
                }
                
                if (presences.isNotEmpty() || totalCours > 0) {
                    cardCoursPresence.visibility = View.VISIBLE
                    textHeaderCoursPresence.text = getString(R.string.label_cours_presence, presences.size, totalCours)
                    if (presences.isNotEmpty()) {
                        textListCoursPresence.text = formatDateList(presences)
                        textListCoursPresence.visibility = View.VISIBLE
                    } else {
                        textListCoursPresence.visibility = View.GONE
                    }
                } else {
                    cardCoursPresence.visibility = View.GONE
                }
            }
        }

        btnClose.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance(studentId: Long, studentName: String, classId: Long = -1) = AbsenceReportDialog().apply {
            arguments = Bundle().apply {
                putLong("studentId", studentId)
                putString("studentName", studentName)
                putLong("classId", classId)
            }
        }
    }
}
