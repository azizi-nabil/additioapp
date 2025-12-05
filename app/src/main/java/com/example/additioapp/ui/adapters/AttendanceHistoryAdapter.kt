package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.AttendanceSessionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceHistoryAdapter(
    private var items: List<AttendanceSessionSummary> = emptyList(),
    private val onItemClick: (Long) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<AttendanceHistoryAdapter.HistoryViewHolder>() {

    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    fun submitList(list: List<AttendanceSessionSummary>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_session, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position], dateFormat, onItemClick, onDeleteClick)
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.textSessionDate)
        private val textPercent: TextView = itemView.findViewById(R.id.textSessionPercent)
        private val chipP: TextView = itemView.findViewById(R.id.chipPresent)
        private val chipA: TextView = itemView.findViewById(R.id.chipAbsent)
        private val chipL: TextView = itemView.findViewById(R.id.chipLate)

        private val textType: TextView = itemView.findViewById(R.id.textSessionType)
        private val textTotals: TextView = itemView.findViewById(R.id.textTotals)
        private val btnDelete: android.widget.ImageButton = itemView.findViewById(R.id.btnDeleteSession)

        fun bind(
            item: AttendanceSessionSummary, 
            formatter: SimpleDateFormat,
            onItemClick: (Long) -> Unit,
            onDeleteClick: (String) -> Unit
        ) {
            textDate.text = formatter.format(Date(item.date))
            textType.text = item.type
            val attended = item.presentCount + item.lateCount
            val percent = if (item.totalCount > 0) (attended * 100 / item.totalCount) else 0
            textPercent.text = "$percent%"
            chipP.text = "P:${item.presentCount}"
            chipA.text = "A:${item.absentCount}"
            chipL.text = "L:${item.lateCount}"
            textTotals.text = "${item.totalCount} Students"

            itemView.setOnClickListener { onItemClick(item.date) }
            btnDelete.setOnClickListener { onDeleteClick(item.sessionId) }
        }
    }
}
