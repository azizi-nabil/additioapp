package com.example.additioapp.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.AtRiskAdapter
import com.example.additioapp.ui.adapters.AtRiskStudent
import com.example.additioapp.ui.adapters.RiskLevel
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

class AnalyticsFragment : Fragment() {

    private val classViewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private lateinit var atRiskAdapter: AtRiskAdapter
    private var classes: List<ClassEntity> = emptyList()
    private var selectedClassId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerClass = view.findViewById<Spinner>(R.id.spinnerClass)
        val textTotalSessions = view.findViewById<TextView>(R.id.textTotalSessions)
        val textTotalStudents = view.findViewById<TextView>(R.id.textTotalStudents)
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        val textAtRiskTitle = view.findViewById<TextView>(R.id.textAtRiskTitle)
        val recyclerAtRisk = view.findViewById<RecyclerView>(R.id.recyclerAtRisk)
        val textNoRisk = view.findViewById<TextView>(R.id.textNoRisk)
        val btnExportPdf = view.findViewById<MaterialButton>(R.id.btnExportPdf)
        val btnExportExcel = view.findViewById<MaterialButton>(R.id.btnExportExcel)

        // Setup RecyclerView
        atRiskAdapter = AtRiskAdapter { item ->
            val dialog = com.example.additioapp.ui.dialogs.AbsenceReportDialog.newInstance(item.student.id, item.student.name)
            dialog.show(parentFragmentManager, "AbsenceReportDialog")
        }
        recyclerAtRisk.adapter = atRiskAdapter
        recyclerAtRisk.layoutManager = LinearLayoutManager(requireContext())

        // Setup Pie Chart
        setupPieChart(pieChart)

        // Observe classes
        classViewModel.allClasses.observe(viewLifecycleOwner) { classList ->
            classes = classList
            val classNames = classList.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerClass.adapter = adapter

            if (classList.isNotEmpty()) {
                selectedClassId = classList[0].id
                loadAnalytics(selectedClassId, textTotalSessions, textTotalStudents, pieChart, textAtRiskTitle, recyclerAtRisk, textNoRisk)
            }
        }

        // Handle class selection
        spinnerClass.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (classes.isNotEmpty() && position < classes.size) {
                    selectedClassId = classes[position].id
                    loadAnalytics(selectedClassId, textTotalSessions, textTotalStudents, pieChart, textAtRiskTitle, recyclerAtRisk, textNoRisk)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Export buttons
        btnExportPdf.setOnClickListener {
            exportToPdf()
        }

        btnExportExcel.setOnClickListener {
            exportToExcel()
        }
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.textSize = 12f
        }
    }

    private fun loadAnalytics(
        classId: Long,
        textTotalSessions: TextView,
        textTotalStudents: TextView,
        pieChart: PieChart,
        textAtRiskTitle: TextView,
        recyclerAtRisk: RecyclerView,
        textNoRisk: TextView
    ) {
        lifecycleScope.launch {
            val students = withContext(Dispatchers.IO) {
                studentViewModel.getStudentsForClassOnce(classId)
            }
            val allRecords = withContext(Dispatchers.IO) {
                attendanceViewModel.getAllAttendanceForClass(classId)
            }
            // Get attendance WITH session type from sessions table
            val recordsWithType = withContext(Dispatchers.IO) {
                attendanceViewModel.getAttendanceWithTypeForClassSync(classId)
            }

            // Count unique sessions
            val uniqueSessions = allRecords.map { it.sessionId }.distinct().size
            textTotalSessions.text = uniqueSessions.toString()
            textTotalStudents.text = students.size.toString()

            // Calculate attendance stats from recordsWithType
            var presentCount = 0
            var absentCount = 0
            var lateCount = 0
            var excusedCount = 0

            recordsWithType.forEach { record ->
                when (record.status) {
                    "P" -> presentCount++
                    "A" -> absentCount++
                    "L" -> lateCount++
                    "E" -> excusedCount++
                }
            }

            // Update Pie Chart
            val entries = ArrayList<PieEntry>()
            if (presentCount > 0) entries.add(PieEntry(presentCount.toFloat(), "Present"))
            if (absentCount > 0) entries.add(PieEntry(absentCount.toFloat(), "Absent"))
            if (lateCount > 0) entries.add(PieEntry(lateCount.toFloat(), "Late"))
            if (excusedCount > 0) entries.add(PieEntry(excusedCount.toFloat(), "Excused"))

            if (entries.isEmpty()) {
                entries.add(PieEntry(1f, "No Data"))
            }

            val dataSet = PieDataSet(entries, "")
            dataSet.colors = listOf(
                Color.parseColor("#4CAF50"), // Green - Present
                Color.parseColor("#F44336"), // Red - Absent
                Color.parseColor("#FF9800"), // Orange - Late
                Color.parseColor("#2196F3")  // Blue - Excused
            )
            dataSet.valueTextSize = 14f
            dataSet.valueTextColor = Color.WHITE

            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(pieChart))
            pieChart.data = data
            pieChart.invalidate()

            // Calculate at-risk students
            // Rules:
            // - At Risk (⚠️): 3+ unexcused absences (status A only) in TD OR TP (independently)
            // - High Risk (🔴): 5+ total absences (A + E) in TD OR TP (independently)
            val atRiskStudents = mutableListOf<AtRiskStudent>()
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
                
                android.util.Log.d("Analytics", "Student: ${student.name}, TD: $tdUnexcused/$tdTotal, TP: $tpUnexcused/$tpTotal, HighRisk: $isHighRisk, AtRisk: $isAtRisk")

                if (isHighRisk || isAtRisk) {
                    atRiskStudents.add(AtRiskStudent(
                        student = student,
                        tdUnexcused = tdUnexcused,
                        tdTotal = tdTotal,
                        tpUnexcused = tpUnexcused,
                        tpTotal = tpTotal,
                        riskLevel = if (isHighRisk) RiskLevel.HIGH_RISK else RiskLevel.AT_RISK
                    ))
                }
            }

            // Sort: High risk first, then by total absences descending
            atRiskStudents.sortWith(compareBy<AtRiskStudent> { it.riskLevel != RiskLevel.HIGH_RISK }
                .thenByDescending { maxOf(it.tdTotal, it.tpTotal) })

            val highRiskCount = atRiskStudents.count { it.riskLevel == RiskLevel.HIGH_RISK }
            val atRiskCount = atRiskStudents.count { it.riskLevel == RiskLevel.AT_RISK }
            textAtRiskTitle.text = "🔴 High Risk: $highRiskCount | ⚠️ At Risk: $atRiskCount"
            
            if (atRiskStudents.isEmpty()) {
                recyclerAtRisk.visibility = View.GONE
                textNoRisk.visibility = View.VISIBLE
            } else {
                recyclerAtRisk.visibility = View.VISIBLE
                textNoRisk.visibility = View.GONE
                atRiskAdapter.submitList(atRiskStudents)
            }
        }
    }

    private fun exportToPdf() {
        lifecycleScope.launch {
            try {
                val className = classes.find { it.id == selectedClassId }?.name ?: "Unknown"
                val students = withContext(Dispatchers.IO) {
                    studentViewModel.getStudentsForClassOnce(selectedClassId)
                }
                val allRecords = withContext(Dispatchers.IO) {
                    attendanceViewModel.getAllAttendanceForClass(selectedClassId)
                }

                val fileName = "attendance_report_${className.replace(" ", "_")}.csv"
                val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                
                FileWriter(file).use { writer ->
                    writer.append("Student Name,TD Absences,TP Absences,Total Absences\n")
                    students.forEach { student ->
                        val studentRecords = allRecords.filter { it.studentId == student.id }
                        val tdAbsences = studentRecords.count { 
                            it.sessionId.contains("_TD") && (it.status == "A" || it.status == "E") 
                        }
                        val tpAbsences = studentRecords.count { 
                            it.sessionId.contains("_TP") && (it.status == "A" || it.status == "E") 
                        }
                        writer.append("${student.name},$tdAbsences,$tpAbsences,${tdAbsences + tpAbsences}\n")
                    }
                }

                // Share file
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Report"))

                Toast.makeText(requireContext(), "Report exported: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportToExcel() {
        lifecycleScope.launch {
            try {
                val className = classes.find { it.id == selectedClassId }?.name ?: "Unknown"
                val students = withContext(Dispatchers.IO) {
                    studentViewModel.getStudentsForClassOnce(selectedClassId)
                }
                val allRecords = withContext(Dispatchers.IO) {
                    attendanceViewModel.getAllAttendanceForClass(selectedClassId)
                }

                // Use CSV format (can be opened in Excel)
                val fileName = "attendance_report_${className.replace(" ", "_")}.csv"
                val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                
                FileWriter(file).use { writer ->
                    // Header row with BOM for Excel UTF-8 support
                    writer.append("\uFEFF") // UTF-8 BOM
                    writer.append("Student Name,TD Absences,TP Absences,Total Absences,Status\n")
                    
                    // Data rows
                    students.forEach { student ->
                        val studentRecords = allRecords.filter { it.studentId == student.id }
                        val tdAbsences = studentRecords.count { 
                            it.sessionId.contains("_TD") && (it.status == "A" || it.status == "E") 
                        }
                        val tpAbsences = studentRecords.count { 
                            it.sessionId.contains("_TP") && (it.status == "A" || it.status == "E") 
                        }
                        val total = tdAbsences + tpAbsences
                        val status = if (total >= 3) "AT RISK" else "OK"
                        
                        writer.append("\"${student.name}\",$tdAbsences,$tpAbsences,$total,$status\n")
                    }
                }

                // Share file
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Excel Report"))

                Toast.makeText(requireContext(), "Excel exported: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
