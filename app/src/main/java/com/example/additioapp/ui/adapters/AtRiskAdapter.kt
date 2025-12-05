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
    val studentId: Long,
    val name: String,
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
            textName.text = student.name
            
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
