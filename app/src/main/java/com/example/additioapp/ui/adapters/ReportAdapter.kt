package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R

data class ReportItem(
    val studentName: String,
    val attendancePct: Float,
    val gradePct: Float
)

class ReportAdapter(
    private var items: List<ReportItem> = emptyList()
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    fun submitList(newItems: List<ReportItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textStudentName)
        private val attendanceProgress: ProgressBar = itemView.findViewById(R.id.progressAttendance)
        private val attendanceText: TextView = itemView.findViewById(R.id.textAttendancePct)
        private val gradeProgress: ProgressBar = itemView.findViewById(R.id.progressGrade)
        private val gradeText: TextView = itemView.findViewById(R.id.textGradePct)

        fun bind(item: ReportItem) {
            nameTextView.text = item.studentName
            
            attendanceProgress.progress = item.attendancePct.toInt()
            attendanceText.text = "${item.attendancePct.toInt()}%"
            
            gradeProgress.progress = item.gradePct.toInt()
            gradeText.text = "${item.gradePct.toInt()}%"
        }
    }
}
