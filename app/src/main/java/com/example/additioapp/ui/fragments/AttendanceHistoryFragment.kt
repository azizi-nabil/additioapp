package com.example.additioapp.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.AttendanceSessionSummary
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.AttendanceHistoryAdapter
import com.example.additioapp.ui.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.io.File
import java.io.FileWriter
import androidx.core.content.FileProvider

class AttendanceHistoryFragment : Fragment() {

    private val attendanceViewModel: AttendanceViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var classId: Long = -1
    private val calendar = Calendar.getInstance()
    private var startDate: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
    private var endDate: Calendar = Calendar.getInstance().apply {
        // Set to end of day to include all sessions created today
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    private lateinit var adapter: AttendanceHistoryAdapter
    private var currentSummaries: List<AttendanceSessionSummary> = emptyList()
    private var sortDesc = true
    private var currentFilter: String? = null
    private var filteredSummaries: List<AttendanceSessionSummary> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment (e.g., after adding attendance)
        if (::adapter.isInitialized) {
            loadSummaries()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val containerStartDate = view.findViewById<View>(R.id.containerStartDate)
        val containerEndDate = view.findViewById<View>(R.id.containerEndDate)
        val textStart = view.findViewById<TextView>(R.id.textStartDate)
        val textEnd = view.findViewById<TextView>(R.id.textEndDate)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerHistory)
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFilters)
        val btnSortHistory = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSortHistory)
        val btnExport = view.findViewById<Button>(R.id.btnExportCsv)
        val btnShare = view.findViewById<Button>(R.id.btnShareCsv)
        val btnNewSession = view.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.btnNewSession)

        btnNewSession.setOnClickListener {
            // Navigate to AttendanceFragment for today
            val bundle = Bundle().apply {
                putLong("classId", classId)
                putLong("selectedDate", System.currentTimeMillis())
            }
            findNavController().navigate(R.id.attendanceFragment, bundle)
        }
        
        adapter = AttendanceHistoryAdapter(
            onItemClick = { sessionId, date ->
                // Navigate to AttendanceFragment with selected date and sessionId
                val bundle = Bundle().apply {
                    putLong("classId", classId)
                    putLong("selectedDate", date)
                    putString("sessionId", sessionId)
                }
                findNavController().navigate(R.id.attendanceFragment, bundle)
            },
            onDeleteClick = { sessionId ->
                // Show confirmation dialog
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete this attendance session? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            attendanceViewModel.deleteSession(sessionId)
                            Toast.makeText(requireContext(), getString(R.string.toast_session_deleted), Toast.LENGTH_SHORT).show()
                            loadSummaries()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        updateDateLabels(textStart, textEnd)
        
        // Fetch oldest session date to set as start date
        lifecycleScope.launch {
            val oldestDate = attendanceViewModel.getOldestSessionDate(classId)
            if (oldestDate != null) {
                startDate.timeInMillis = oldestDate
                updateDateLabels(textStart, textEnd)
            }
            loadSummaries()
        }

        containerStartDate.setOnClickListener { pickDate(startDate) { updateDateLabels(textStart, textEnd); loadSummaries() } }
        containerEndDate.setOnClickListener { pickDate(endDate) { updateDateLabels(textStart, textEnd); loadSummaries() } }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentFilter = "All" // Default fallback
            } else {
                val chipId = checkedIds[0]
                currentFilter = when (chipId) {
                    R.id.chipFilterCours -> "Cours"
                    R.id.chipFilterTD -> "TD"
                    R.id.chipFilterTP -> "TP"
                    else -> "All"
                }
            }
            applyFilter(currentFilter)
        }

        btnSortHistory.setOnClickListener {
            sortDesc = !sortDesc
            btnSortHistory.text = if (sortDesc) "Newest" else "Oldest"
            // Rotate icon 180 degrees - MaterialButton icon rotation isn't directly exposed as a property for animation easily without ObjectAnimator on the drawable
            // For now, we'll just keep the text toggle which is clear enough.
            // If we really want icon rotation, we'd need to animate the drawable or use a different approach.
            // Given the user request was just position, let's ensure it works first.
            
            applyFilter(currentFilter)
        }

        btnExport.setOnClickListener { exportCsv() }
        btnShare.setOnClickListener { shareCsv() }
    }

    private fun pickDate(target: Calendar, onPicked: () -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                target.set(year, month, dayOfMonth)
                onPicked()
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabels(textStart: TextView, textEnd: TextView) {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        textStart.text = fmt.format(startDate.time)
        textEnd.text = fmt.format(endDate.time)
    }

    private fun loadSummaries() {
        attendanceViewModel.getSessionSummaries(classId, startDate.timeInMillis, endDate.timeInMillis)
            .observe(viewLifecycleOwner) { summaries ->
                currentSummaries = summaries
                applyFilter(currentFilter)
            }
    }

    private fun applyFilter(filter: String?) {
        val filtered = when (filter) {
            "Cours" -> currentSummaries.filter { it.type == "Cours" }
            "TD" -> currentSummaries.filter { it.type == "TD" }
            "TP" -> currentSummaries.filter { it.type == "TP" }
            else -> currentSummaries
        }
        val sorted = if (sortDesc) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
        filteredSummaries = sorted
        adapter.submitList(sorted)
        
        updateOverallStats(sorted)
    }

    private fun updateOverallStats(summaries: List<AttendanceSessionSummary>) {
        val totalSessions = summaries.size
        view?.findViewById<TextView>(R.id.textTotalSessions)?.text = totalSessions.toString()

        if (totalSessions > 0) {
            var totalAttended = 0
            var totalPossible = 0
            summaries.forEach { 
                totalAttended += (it.presentCount + it.lateCount)
                totalPossible += it.totalCount
            }
            val avg = if (totalPossible > 0) (totalAttended * 100f / totalPossible) else 0f
            view?.findViewById<TextView>(R.id.textAvgAttendance)?.text = String.format(Locale.getDefault(), "%.1f%%", avg)
        } else {
            view?.findViewById<TextView>(R.id.textAvgAttendance)?.text = "0%"
        }
    }

    private fun exportCsv() {
        if (filteredSummaries.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_no_attendance_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = "attendance_${fmt.format(startDate.time)}_${fmt.format(endDate.time)}.csv"
        val file = File(requireContext().filesDir, fileName)
        try {
            FileWriter(file).use { writer ->
                                writer.appendLine("sessionId,date,present,absent,late,excused,total,attendancePercent")
                filteredSummaries.forEach { s ->
                    val attended = s.presentCount + s.lateCount
                    val pct = if (s.totalCount > 0) (attended * 100f / s.totalCount) else 0f
                    writer.appendLine(
                        listOf(
                            s.sessionId,
                            fmt.format(s.date),
                            s.presentCount,
                            s.absentCount,
                            s.lateCount,
                            s.excusedCount,
                            s.totalCount,
                            String.format(Locale.getDefault(), "%.1f", pct)
                        ).joinToString(",")
                    )
                }
            }
            Toast.makeText(requireContext(), getString(R.string.toast_exported_to, file.absolutePath), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCsv() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = "attendance_${fmt.format(startDate.time)}_${fmt.format(endDate.time)}_share.csv"
        val file = File(requireContext().cacheDir, fileName)
        if (filteredSummaries.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_no_attendance_to_share), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            FileWriter(file).use { writer ->
                                writer.appendLine("sessionId,date,present,absent,late,excused,total,attendancePercent")
                filteredSummaries.forEach { s ->
                    val attended = s.presentCount + s.lateCount
                    val pct = if (s.totalCount > 0) (attended * 100f / s.totalCount) else 0f
                    writer.appendLine(
                        listOf(
                            s.sessionId,
                            fmt.format(s.date),
                            s.presentCount,
                            s.absentCount,
                            s.lateCount,
                            s.excusedCount,
                            s.totalCount,
                            String.format(Locale.getDefault(), "%.1f", pct)
                        ).joinToString(",")
                    )
                }
            }
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share attendance CSV"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.toast_share_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        fun newInstance(classId: Long) = AttendanceHistoryFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
