package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R

enum class RiskLevel {
    AT_RISK,    // 3+ unexcused absences in TD or TP
    HIGH_RISK   // 5+ total absences (including excused) in TD or TP
}

data class AtRiskStudent(
    val student: com.example.additioapp.data.model.StudentEntity,
    val tdUnexcused: Int,      // TD absences (status A only)
    val tdTotal: Int,          // TD absences (status A + E)
    val tpUnexcused: Int,      // TP absences (status A only)
    val tpTotal: Int,          // TP absences (status A + E)
    val riskLevel: RiskLevel
)

class AtRiskAdapter(
    private val onStudentClick: (AtRiskStudent) -> Unit = {}
) : RecyclerView.Adapter<AtRiskAdapter.ViewHolder>() {

    private var items: List<AtRiskStudent> = emptyList()

    fun submitList(newItems: List<AtRiskStudent>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_at_risk_student, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = items[position]
        holder.bind(student)
        holder.itemView.setOnClickListener { onStudentClick(student) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textStudentName)
        private val textDetails: TextView = itemView.findViewById(R.id.textAbsenceDetails)
        private val textTotal: TextView = itemView.findViewById(R.id.textTotalAbsences)

        fun bind(student: AtRiskStudent) {
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
            textName.textSize = nameSize

            val paddingPx = (padding * itemView.context.resources.displayMetrics.density).toInt()
            (itemView as? com.google.android.material.card.MaterialCardView)?.setContentPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            // Name language
            val displayName = if (nameLang == "arabic" && !student.student.displayNameAr.isNullOrEmpty()) {
                student.student.displayNameAr
            } else {
                student.student.displayNameFr
            }
            textName.text = displayName
            
            // Show TD:unexcused/total, TP:unexcused/total
            val details = buildString {
                if (student.tdTotal > 0) {
                    append("TD: ${student.tdUnexcused}/${student.tdTotal}")
                }
                if (student.tpTotal > 0) {
                    if (isNotEmpty()) append(", ")
                    append("TP: ${student.tpUnexcused}/${student.tpTotal}")
                }
            }
            textDetails.text = details
            
            // Show risk level indicator
            when (student.riskLevel) {
                RiskLevel.HIGH_RISK -> {
                    textTotal.text = "🔴"
                    textTotal.setTextColor(Color.parseColor("#D32F2F"))
                }
                RiskLevel.AT_RISK -> {
                    textTotal.text = "⚠️"
                    textTotal.setTextColor(Color.parseColor("#FF9800"))
                }
            }
        }
    }
}
