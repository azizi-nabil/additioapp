package com.example.additioapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.example.additioapp.ui.viewmodel.GradeViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class ReportsFragment : Fragment() {

    private val classViewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val gradeViewModel: GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var classes: List<ClassEntity> = emptyList()
    private var selectedClass: ClassEntity? = null
    private var students: List<StudentEntity> = emptyList()
    private var attendance: List<AttendanceRecordEntity> = emptyList()
    private var gradeItems: List<GradeItemEntity> = emptyList()
    private var gradeRecords: List<GradeRecordEntity> = emptyList()
    
    private var atRiskJob: Job? = null

    private val formatter = DecimalFormat("#.#")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerClass = view.findViewById<Spinner>(R.id.spinnerReportClass)
        val textAttendanceRate = view.findViewById<TextView>(R.id.textAttendanceRate)
        val textAverageGrade = view.findViewById<TextView>(R.id.textAverageGrade)
        val textTotalStudents = view.findViewById<TextView>(R.id.textTotalStudentsReport)
        val containerPerformance = view.findViewById<LinearLayout>(R.id.containerPerformanceSummary)
        val containerRankings = view.findViewById<LinearLayout>(R.id.containerStudentRankings)
        val containerAtRisk = view.findViewById<LinearLayout>(R.id.containerAtRiskStudents)

        classViewModel.allClasses.observe(viewLifecycleOwner) { classList ->
            classes = classList
            val classNames = classList.map { it.name }
            spinnerClass.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classNames)
        }

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (classes.isNotEmpty()) {
                    selectedClass = classes[position]
                    loadClassReport(selectedClass!!, textAttendanceRate, textAverageGrade, textTotalStudents,
                        containerPerformance, containerRankings, containerAtRisk)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadClassReport(
        classEntity: ClassEntity,
        textAttendanceRate: TextView,
        textAverageGrade: TextView,
        textTotalStudents: TextView,
        containerPerformance: LinearLayout,
        containerRankings: LinearLayout,
        containerAtRisk: LinearLayout
    ) {
        studentViewModel.getStudentsForClass(classEntity.id).observe(viewLifecycleOwner) { s ->
            students = s
            textTotalStudents.text = s.size.toString()
            updateReportUI(classEntity.id, textAttendanceRate, textAverageGrade, containerPerformance, containerRankings, containerAtRisk)
        }

        attendanceViewModel.getAttendanceForClass(classEntity.id).observe(viewLifecycleOwner) { a ->
            attendance = a
            updateReportUI(classEntity.id, textAttendanceRate, textAverageGrade, containerPerformance, containerRankings, containerAtRisk)
        }

        gradeViewModel.getGradeItemsForClass(classEntity.id).observe(viewLifecycleOwner) { items ->
            gradeItems = items
            updateReportUI(classEntity.id, textAttendanceRate, textAverageGrade, containerPerformance, containerRankings, containerAtRisk)
        }

        gradeViewModel.getGradeRecordsForClass(classEntity.id).observe(viewLifecycleOwner) { records ->
            gradeRecords = records
            updateReportUI(classEntity.id, textAttendanceRate, textAverageGrade, containerPerformance, containerRankings, containerAtRisk)
        }
    }

    private fun updateReportUI(
        classId: Long,
        textAttendanceRate: TextView,
        textAverageGrade: TextView,
        containerPerformance: LinearLayout,
        containerRankings: LinearLayout,
        containerAtRisk: LinearLayout
    ) {
        if (students.isEmpty()) {
            textAttendanceRate.text = "--"
            textAverageGrade.text = "--"
            containerPerformance.removeAllViews()
            addEmptyRow(containerPerformance, "No students in this class")
            containerRankings.removeAllViews()
            containerAtRisk.removeAllViews()
            return
        }

        // Calculate student stats
        data class StudentStats(
            val student: StudentEntity,
            val attendancePct: Float,
            val gradePct: Float
        )

        val studentStats = students.map { student ->
            // Attendance
            val studentAttendance = attendance.filter { it.studentId == student.id }
            val totalSessions = studentAttendance.size
            val presentOrLate = studentAttendance.count { it.status == "P" || it.status == "L" || it.status == "JL" }
            val attendancePct = if (totalSessions > 0) (presentOrLate.toFloat() / totalSessions) * 100 else -1f

            // Grades
            val studentRecords = gradeRecords.filter { it.studentId == student.id }
            val gradePct = gradeViewModel.calculateWeightedAverage(gradeItems, studentRecords)

            StudentStats(student, attendancePct, gradePct)
        }

        // Class averages
        val validAttendance = studentStats.filter { it.attendancePct >= 0 }
        val avgAttendance = if (validAttendance.isNotEmpty()) {
            validAttendance.map { it.attendancePct }.average().toFloat()
        } else -1f

        val validGrades = studentStats.filter { it.gradePct >= 0 }
        val avgGrade = if (validGrades.isNotEmpty()) {
            validGrades.map { it.gradePct }.average().toFloat()
        } else -1f

        textAttendanceRate.text = if (avgAttendance >= 0) "${formatter.format(avgAttendance)}%" else "--"
        textAverageGrade.text = if (avgGrade >= 0) "${formatter.format(avgGrade)}%" else "--"

        // Performance Summary
        renderPerformanceSummary(containerPerformance, studentStats)

        // Rankings
        renderStudentRankings(containerRankings, studentStats)

        // At-risk students - use same rules as Analytics (TD/TP absences)
        renderAtRiskStudents(classId, containerAtRisk)
    }

    private fun renderPerformanceSummary(container: LinearLayout, studentStats: List<Any>) {
        container.removeAllViews()

        @Suppress("UNCHECKED_CAST")
        val stats = studentStats as List<*>

        // Performance categories
        val excellent = stats.count { 
            val s = it as? StudentStats
            s != null && s.gradePct >= 80 && s.attendancePct >= 80 
        }
        val good = stats.count { 
            val s = it as? StudentStats
            s != null && s.gradePct >= 60 && s.gradePct < 80 && s.attendancePct >= 70 
        }
        val needsImprovement = stats.count { 
            val s = it as? StudentStats
            s != null && (s.gradePct < 60 || s.attendancePct < 70) && s.gradePct >= 0 
        }
        val noData = stats.count {
            val s = it as? StudentStats
            s != null && s.gradePct < 0
        }

        addPerformanceRow(container, "🌟 Excellent", excellent, "#4CAF50")
        addPerformanceRow(container, "👍 Good", good, "#2196F3")
        addPerformanceRow(container, "📈 Needs Improvement", needsImprovement, "#FF9800")
        if (noData > 0) {
            addPerformanceRow(container, "❓ No Data", noData, "#9E9E9E")
        }
    }

    private data class StudentStats(
        val student: StudentEntity,
        val attendancePct: Float,
        val gradePct: Float
    )

    private fun addPerformanceRow(container: LinearLayout, label: String, count: Int, color: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val countView = TextView(requireContext()).apply {
            text = count.toString()
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor(color))
        }

        row.addView(labelView)
        row.addView(countView)
        container.addView(row)
    }

    private fun renderStudentRankings(container: LinearLayout, stats: List<Any>) {
        container.removeAllViews()

        @Suppress("UNCHECKED_CAST")
        val studentStats = (stats as List<*>).filterIsInstance<StudentStats>()

        // Sort by combined score (average of attendance and grade)
        val ranked = studentStats
            .filter { it.gradePct >= 0 || it.attendancePct >= 0 }
            .sortedByDescending { 
                val g = if (it.gradePct >= 0) it.gradePct else 0f
                val a = if (it.attendancePct >= 0) it.attendancePct else 0f
                (g + a) / 2
            }

        if (ranked.isEmpty()) {
            addEmptyRow(container, "No ranking data available")
            return
        }

        ranked.take(5).forEachIndexed { index, stat ->
            val row = layoutInflater.inflate(R.layout.item_home_row, container, false)

            val medal = when (index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${index + 1}."
            }

            row.findViewById<View>(R.id.colorIndicator).visibility = View.GONE
            row.findViewById<TextView>(R.id.textRowTitle).text = "$medal ${stat.student.name}"

            val gradeTxt = if (stat.gradePct >= 0) "${formatter.format(stat.gradePct)}%" else "N/A"
            val attTxt = if (stat.attendancePct >= 0) "${formatter.format(stat.attendancePct)}%" else "N/A"
            row.findViewById<TextView>(R.id.textRowMeta).text = "Grade: $gradeTxt • Attendance: $attTxt"

            row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE

            container.addView(row)
        }
    }

    // Data class for at-risk calculation
    private data class AtRiskInfo(
        val student: StudentEntity,
        val tdUnexcused: Int,
        val tdTotal: Int,
        val tpUnexcused: Int,
        val tpTotal: Int,
        val isHighRisk: Boolean,
        val isAtRisk: Boolean
    )

    private fun renderAtRiskStudents(classId: Long, container: LinearLayout) {
        container.removeAllViews()

        // Cancel previous job to prevent duplicates
        atRiskJob?.cancel()
        
        // Use coroutine to fetch attendance with type
        atRiskJob = lifecycleScope.launch {
            val recordsWithType = withContext(Dispatchers.IO) {
                attendanceViewModel.getAttendanceWithTypeForClassSync(classId)
            }

            // Calculate at-risk using same rules as Analytics:
            // - At Risk (⚠️): 3+ unexcused absences (status A only) in TD OR TP
            // - High Risk (🔴): 5+ total absences (A + E) in TD OR TP
            val atRiskStudents = mutableListOf<AtRiskInfo>()
            
            students.forEach { student ->
                val studentRecords = recordsWithType.filter { it.studentId == student.id }
                
                // TD absences
                val tdUnexcused = studentRecords.count { it.type == "TD" && it.status == "A" }
                val tdTotal = studentRecords.count { it.type == "TD" && (it.status == "A" || it.status == "E") }
                
                // TP absences
                val tpUnexcused = studentRecords.count { it.type == "TP" && it.status == "A" }
                val tpTotal = studentRecords.count { it.type == "TP" && (it.status == "A" || it.status == "E") }
                
                // Check risk levels (TD and TP are independent)
                val isHighRisk = tdTotal >= 5 || tpTotal >= 5
                val isAtRisk = tdUnexcused >= 3 || tpUnexcused >= 3
                
                if (isHighRisk || isAtRisk) {
                    atRiskStudents.add(AtRiskInfo(
                        student = student,
                        tdUnexcused = tdUnexcused,
                        tdTotal = tdTotal,
                        tpUnexcused = tpUnexcused,
                        tpTotal = tpTotal,
                        isHighRisk = isHighRisk,
                        isAtRisk = isAtRisk
                    ))
                }
            }

            // Sort: High risk first, then by total absences descending
            atRiskStudents.sortWith(compareBy<AtRiskInfo> { !it.isHighRisk }
                .thenByDescending { maxOf(it.tdTotal, it.tpTotal) })

            // Update UI on main thread
            if (atRiskStudents.isEmpty()) {
                val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
                row.findViewById<View>(R.id.colorIndicator).visibility = View.GONE
                row.findViewById<TextView>(R.id.textRowTitle).text = "✅ All students performing well!"
                row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
                row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
                container.addView(row)
                return@launch
            }

            // Show counts header
            val highRiskCount = atRiskStudents.count { it.isHighRisk }
            val atRiskCount = atRiskStudents.count { it.isAtRisk && !it.isHighRisk }
            
            val headerRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            }
            val headerText = TextView(requireContext()).apply {
                text = "🔴 High Risk: $highRiskCount | ⚠️ At Risk: $atRiskCount"
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
            }
            headerRow.addView(headerText)
            container.addView(headerRow)

            atRiskStudents.take(5).forEach { info ->
                val row = layoutInflater.inflate(R.layout.item_home_row, container, false)

                val indicatorColor = if (info.isHighRisk) "#D32F2F" else "#FF9800"
                row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(Color.parseColor(indicatorColor))
                row.findViewById<TextView>(R.id.textRowTitle).text = info.student.name

                // Show TD:unexcused/total, TP:unexcused/total
                val details = buildString {
                    if (info.tdTotal > 0) {
                        append("TD: ${info.tdUnexcused}/${info.tdTotal}")
                    }
                    if (info.tpTotal > 0) {
                        if (isNotEmpty()) append(", ")
                        append("TP: ${info.tpUnexcused}/${info.tpTotal}")
                    }
                }
                row.findViewById<TextView>(R.id.textRowMeta).text = details

                row.findViewById<TextView>(R.id.textRowExtra).apply {
                    text = if (info.isHighRisk) "🔴" else "⚠️"
                    visibility = View.VISIBLE
                }

                // Click to show absence report
                row.setOnClickListener {
                    val dialog = com.example.additioapp.ui.dialogs.AbsenceReportDialog.newInstance(info.student.id, info.student.name, classId)
                    dialog.show(parentFragmentManager, "AbsenceReportDialog")
                }

                container.addView(row)
            }

            if (atRiskStudents.size > 5) {
                val moreRow = layoutInflater.inflate(R.layout.item_home_row, container, false)
                moreRow.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
                moreRow.findViewById<TextView>(R.id.textRowTitle).apply {
                    text = "+${atRiskStudents.size - 5} more students"
                    setTextColor(Color.parseColor("#F44336"))
                }
                moreRow.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
                moreRow.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
                
                // Make clickable to expand
                moreRow.setOnClickListener {
                    // Remove the "more" row
                    container.removeView(moreRow)
                    
                    // Add remaining students
                    atRiskStudents.drop(5).forEach { info ->
                        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)

                        val indicatorColor = if (info.isHighRisk) "#D32F2F" else "#FF9800"
                        row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(Color.parseColor(indicatorColor))
                        row.findViewById<TextView>(R.id.textRowTitle).text = info.student.name

                        val details = buildString {
                            if (info.tdTotal > 0) {
                                append("TD: ${info.tdUnexcused}/${info.tdTotal}")
                            }
                            if (info.tpTotal > 0) {
                                if (isNotEmpty()) append(", ")
                                append("TP: ${info.tpUnexcused}/${info.tpTotal}")
                            }
                        }
                        row.findViewById<TextView>(R.id.textRowMeta).text = details

                        row.findViewById<TextView>(R.id.textRowExtra).apply {
                            text = if (info.isHighRisk) "🔴" else "⚠️"
                            visibility = View.VISIBLE
                        }

                        // Click to show absence report
                        row.setOnClickListener {
                            val dialog = com.example.additioapp.ui.dialogs.AbsenceReportDialog.newInstance(info.student.id, info.student.name, classId)
                            dialog.show(parentFragmentManager, "AbsenceReportDialog")
                        }

                        container.addView(row)
                    }
                }
                
                container.addView(moreRow)
            }
        }
    }

    private fun addEmptyRow(container: LinearLayout, message: String) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowTitle).text = message
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        container.addView(row)
    }
}
