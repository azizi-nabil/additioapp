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
            // Check preferences
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(itemView.context)
            val nameLang = prefs.getString("pref_name_language", "french") ?: "french"
            val listSize = prefs.getString("pref_list_size", "normal") ?: "normal"

            // Apply list size
            val (nameSize, padding) = when (listSize) {
                "compact" -> Pair(14f, 8)
                "comfortable" -> Pair(16f, 16)
                else -> Pair(14f, 12) // normal
            }
            nameTextView.textSize = nameSize

            val paddingPx = (padding * itemView.context.resources.displayMetrics.density).toInt()
            (itemView as? com.google.android.material.card.MaterialCardView)?.setContentPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            // Name
            // NOTE: ReportItem only has studentName currently. We'll use it as is for now unless we refactor ReportItem too.
            // Since ReportAdapter appears unused, we'll just apply sizing logic.
            nameTextView.text = item.studentName
            
            attendanceProgress.progress = item.attendancePct.toInt()
            attendanceText.text = "${item.attendancePct.toInt()}%"
            
            gradeProgress.progress = item.gradePct.toInt()
            gradeText.text = "${item.gradePct.toInt()}%"
        }
    }
}
