package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.EventEntity

class EventAdapter(
    private val onEventClick: (EventEntity) -> Unit,
    private val onDeleteClick: (EventEntity) -> Unit
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    private var events: List<EventEntity> = emptyList()
    private var classNames: Map<Long, Pair<String, String>> = emptyMap() // classId -> (name, color)

    fun submitList(newEvents: List<EventEntity>) {
        events = newEvents
        notifyDataSetChanged()
    }

    fun setClassInfo(classInfo: Map<Long, Pair<String, String>>) {
        classNames = classInfo
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorStrip: View = itemView.findViewById(R.id.colorStrip)
        private val textTitle: TextView = itemView.findViewById(R.id.textEventTitle)
        private val textTime: TextView = itemView.findViewById(R.id.textEventTime)
        private val textType: TextView = itemView.findViewById(R.id.textEventType)
        private val textClass: TextView = itemView.findViewById(R.id.textEventClass)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEvent)

        fun bind(event: EventEntity) {
            textTitle.text = event.title

            // Time
            val timeText = when {
                event.isAllDay -> "All day"
                event.startTime != null && event.endTime != null -> "${event.startTime} - ${event.endTime}"
                event.startTime != null -> event.startTime
                else -> ""
            }
            textTime.text = timeText
            textTime.visibility = if (timeText.isNotEmpty()) View.VISIBLE else View.GONE

            // Type
            textType.text = event.eventType
            textType.visibility = if (event.eventType != "OTHER") View.VISIBLE else View.GONE

            // Color
            val color = when {
                event.color != null -> event.color
                event.classId != null -> classNames[event.classId]?.second ?: "#2196F3"
                else -> getEventTypeColor(event.eventType)
            }
            (colorStrip.background as? GradientDrawable)?.setColor(Color.parseColor(color))
                ?: colorStrip.setBackgroundColor(Color.parseColor(color))

            // Class name
            if (event.classId != null && classNames.containsKey(event.classId)) {
                textClass.text = classNames[event.classId]?.first
                textClass.visibility = View.VISIBLE
            } else {
                textClass.visibility = View.GONE
            }

            itemView.setOnClickListener { onEventClick(event) }
            btnDelete.setOnClickListener { onDeleteClick(event) }
        }

        private fun getEventTypeColor(type: String): String {
            return when (type) {
                "EXAM" -> "#F44336"      // Red
                "MEETING" -> "#9C27B0"   // Purple
                "DEADLINE" -> "#FF9800"  // Orange
                else -> "#2196F3"        // Blue
            }
        }
    }
}
