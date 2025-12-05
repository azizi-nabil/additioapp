package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity

data class StudentStats(
    val coursPresent: Int,
    val coursTotal: Int,
    val tdAbsent: Int,
    val tdExcused: Int,
    val tdTotal: Int,
    val tpAbsent: Int,
    val tpExcused: Int,
    val tpTotal: Int,
    val behaviorPositive: Int,
    val behaviorNegative: Int,
    val hasGrades: Boolean = false
)

class StudentAdapter(
    private var items: List<StudentEntity> = emptyList(),
    private var attendanceStats: Map<Long, StudentStats> = emptyMap(),
    private val onStudentClick: (StudentEntity) -> Unit,
    private val onReportClick: (StudentEntity) -> Unit = {},
    private val onGradesClick: (StudentEntity) -> Unit = {},
    private val onBehaviorClick: (StudentEntity, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private var originalItems: List<StudentEntity> = emptyList()
    private var filteredItems: List<StudentEntity> = emptyList()

    fun submitList(newItems: List<StudentEntity>, newStats: Map<Long, StudentStats> = attendanceStats) {
        items = newItems
        originalItems = newItems
        filteredItems = newItems
        attendanceStats = newStats
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            originalItems
        } else {
            originalItems.filter {
                it.name.contains(query, ignoreCase = true) || it.studentId.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(filteredItems[position], attendanceStats[filteredItems[position].id], onStudentClick, onReportClick, onGradesClick, onBehaviorClick)
    }

    override fun getItemCount(): Int = filteredItems.size

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textStudentName)
        private val idTextView: TextView = itemView.findViewById(R.id.textStudentId)
        private val orderTextView: TextView = itemView.findViewById(R.id.textStudentOrder)
        private val layoutStatsCours: View = itemView.findViewById(R.id.layoutStatsCours)
        private val statsCoursTextView: TextView = itemView.findViewById(R.id.textStatsCours)
        private val layoutStatsTD: View = itemView.findViewById(R.id.layoutStatsTD)
        private val statsTDTextView: TextView = itemView.findViewById(R.id.textStatsTD)
        private val layoutStatsTP: View = itemView.findViewById(R.id.layoutStatsTP)
        private val statsTPTextView: TextView = itemView.findViewById(R.id.textStatsTP)
        private val behaviorPosTextView: TextView = itemView.findViewById(R.id.textStudentBehaviorPos)
        private val behaviorNegTextView: TextView = itemView.findViewById(R.id.textStudentBehaviorNeg)
        private val btnReport: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnAbsenceReport)
        private val btnGrades: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnGradesReport)
        private val btnMoreOptions: android.widget.ImageView = itemView.findViewById(R.id.btnMoreOptions)

        fun bind(
            student: StudentEntity, 
            stats: StudentStats?, 
            onStudentClick: (StudentEntity) -> Unit, 
            onReportClick: (StudentEntity) -> Unit, 
            onGradesClick: (StudentEntity) -> Unit,
            onBehaviorClick: (StudentEntity, String) -> Unit
        ) {
            orderTextView.text = "${adapterPosition + 1}."
            nameTextView.text = student.name
            idTextView.text = "ID: ${student.studentId}"
            
            if (stats != null) {
                val coursPct = if (stats.coursTotal > 0) (stats.coursPresent.toFloat() / stats.coursTotal) * 100 else 0f
                val tdPct = if (stats.tdTotal > 0) (stats.tdAbsent.toFloat() / stats.tdTotal) * 100 else 0f
                val tpPct = if (stats.tpTotal > 0) (stats.tpAbsent.toFloat() / stats.tpTotal) * 100 else 0f
                
                // Cours Stats (Presence)
                if (stats.coursPresent > 0) {
                    layoutStatsCours.visibility = View.VISIBLE
                    statsCoursTextView.text = "${stats.coursPresent} (${"%.0f".format(coursPct)}%)"
                } else {
                    layoutStatsCours.visibility = View.GONE
                }

                // TD Stats - show justified in green
                if (stats.tdAbsent > 0) {
                    layoutStatsTD.visibility = View.VISIBLE
                    val tdText = if (stats.tdExcused > 0) {
                        // Format: "5:3 (25%)" with 3 in green
                        val text = "${stats.tdAbsent}:${stats.tdExcused} (${"%.0f".format(tdPct)}%)"
                        val spannable = android.text.SpannableString(text)
                        val colonIndex = text.indexOf(':')
                        val parenIndex = text.indexOf('(')
                        if (colonIndex >= 0 && parenIndex > colonIndex) {
                            spannable.setSpan(
                                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#4CAF50")),
                                colonIndex + 1,
                                parenIndex - 1,
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        spannable
                    } else {
                        "${stats.tdAbsent} (${"%.0f".format(tdPct)}%)"
                    }
                    statsTDTextView.text = tdText
                } else {
                    layoutStatsTD.visibility = View.GONE
                }

                // TP Stats - show justified in green
                if (stats.tpAbsent > 0) {
                    layoutStatsTP.visibility = View.VISIBLE
                    val tpText = if (stats.tpExcused > 0) {
                        // Format: "5:3 (25%)" with 3 in green
                        val text = "${stats.tpAbsent}:${stats.tpExcused} (${"%.0f".format(tpPct)}%)"
                        val spannable = android.text.SpannableString(text)
                        val colonIndex = text.indexOf(':')
                        val parenIndex = text.indexOf('(')
                        if (colonIndex >= 0 && parenIndex > colonIndex) {
                            spannable.setSpan(
                                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#4CAF50")),
                                colonIndex + 1,
                                parenIndex - 1,
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        spannable
                    } else {
                        "${stats.tpAbsent} (${"%.0f".format(tpPct)}%)"
                    }
                    statsTPTextView.text = tpText
                } else {
                    layoutStatsTP.visibility = View.GONE
                }
                
                if (stats.behaviorPositive != 0) {
                    behaviorPosTextView.visibility = View.VISIBLE
                    behaviorPosTextView.text = stats.behaviorPositive.toString()
                    behaviorPosTextView.setOnClickListener { onBehaviorClick(student, "POSITIVE") }
                } else {
                    behaviorPosTextView.visibility = View.GONE
                }

                if (stats.behaviorNegative != 0) {
                    behaviorNegTextView.visibility = View.VISIBLE
                    behaviorNegTextView.text = stats.behaviorNegative.toString()
                    behaviorNegTextView.setOnClickListener { onBehaviorClick(student, "NEGATIVE") }
                } else {
                    behaviorNegTextView.visibility = View.GONE
                }

                // Show report button if any absence
                if (stats.tdAbsent > 0 || stats.tpAbsent > 0) {
                    btnReport.visibility = View.VISIBLE
                    btnReport.setOnClickListener { onReportClick(student) }
                } else {
                    btnReport.visibility = View.GONE
                }

                // Show grades button if any grades
                if (stats.hasGrades) {
                    btnGrades.visibility = View.VISIBLE
                    btnGrades.setOnClickListener { onGradesClick(student) }
                } else {
                    btnGrades.visibility = View.GONE
                }
            } else {
                layoutStatsCours.visibility = View.GONE
                layoutStatsTD.visibility = View.GONE
                layoutStatsTP.visibility = View.GONE
                behaviorPosTextView.visibility = View.GONE
                behaviorNegTextView.visibility = View.GONE
                btnReport.visibility = View.GONE
                btnGrades.visibility = View.GONE
            }

            itemView.setOnClickListener { onStudentClick(student) }
            
            btnMoreOptions.setOnClickListener { view ->
                // You can implement a popup menu here if needed, or just trigger the click
                onStudentClick(student)
            }
        }
    }
}
