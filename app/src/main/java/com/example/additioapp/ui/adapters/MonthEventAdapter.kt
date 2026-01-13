package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.EventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MonthEventItem {
    data class Header(val date: Long, val dateString: String) : MonthEventItem()
    data class Event(val event: EventEntity) : MonthEventItem()
}

class MonthEventAdapter(
    private val onEventClick: (EventEntity) -> Unit,
    private val classNames: Map<Long, Pair<String, String>> = emptyMap()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<MonthEventItem> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_EVENT = 1
    }

    fun submitGroupedEvents(events: List<EventEntity>) {
        val grouped = events
            .sortedBy { it.date }
            .groupBy { it.date }
        
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val newItems = mutableListOf<MonthEventItem>()
        
        grouped.forEach { (date, dayEvents) ->
            newItems.add(MonthEventItem.Header(date, dateFormat.format(Date(date))))
            dayEvents.sortedBy { it.startTime ?: "00:00" }.forEach { event ->
                newItems.add(MonthEventItem.Event(event))
            }
        }
        
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MonthEventItem.Header -> TYPE_HEADER
            is MonthEventItem.Event -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_event_month, parent, false)
                view.findViewById<View>(R.id.eventItemContainer).visibility = View.GONE
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_event_month, parent, false)
                view.findViewById<View>(R.id.textDateHeader).visibility = View.GONE
                EventViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MonthEventItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MonthEventItem.Event -> (holder as EventViewHolder).bind(item.event)
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textHeader: TextView = itemView.findViewById(R.id.textDateHeader)

        fun bind(header: MonthEventItem.Header) {
            textHeader.text = header.dateString
            textHeader.visibility = View.VISIBLE
        }
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.eventItemContainer)
        private val colorDot: View = itemView.findViewById(R.id.colorDot)
        private val textTitle: TextView = itemView.findViewById(R.id.textEventTitle)
        private val textDetails: TextView = itemView.findViewById(R.id.textEventDetails)
        private val textTime: TextView = itemView.findViewById(R.id.textEventTime)

        fun bind(event: EventEntity) {
            container.visibility = View.VISIBLE
            textTitle.text = event.title

            // Time
            val timeText = when {
                event.isAllDay -> "All day"
                event.startTime != null -> event.startTime
                else -> ""
            }
            textTime.text = timeText

            // Details (location or description)
            val details = when {
                event.location.isNotEmpty() -> event.location
                event.description.isNotEmpty() -> event.description
                else -> null
            }
            if (details != null) {
                textDetails.text = details
                textDetails.visibility = View.VISIBLE
                // Show location icon if it's a location
                if (event.location.isNotEmpty()) {
                    val locationIcon = androidx.core.content.ContextCompat.getDrawable(
                        itemView.context, R.drawable.ic_location
                    )
                    locationIcon?.setBounds(0, 0, 36, 36)
                    val colorOnSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(
                        itemView, com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                    locationIcon?.setTint(colorOnSurfaceVariant)
                    textDetails.setCompoundDrawables(locationIcon, null, null, null)
                    textDetails.compoundDrawablePadding = 8
                } else {
                    textDetails.setCompoundDrawables(null, null, null, null)
                }
            } else {
                textDetails.visibility = View.GONE
                textDetails.setCompoundDrawables(null, null, null, null)
            }

            // Color
            val color = when {
                event.color != null -> event.color
                event.classId != null -> classNames[event.classId]?.second ?: "#2196F3"
                else -> getEventTypeColor(event.eventType)
            }
            (colorDot.background as? GradientDrawable)?.setColor(Color.parseColor(color))
                ?: colorDot.setBackgroundColor(Color.parseColor(color))

            container.setOnClickListener { onEventClick(event) }
        }

        private fun getEventTypeColor(type: String): String {
            return when (type) {
                "EXAM" -> "#F44336"
                "MEETING" -> "#9C27B0"
                "DEADLINE" -> "#FF9800"
                else -> "#2196F3"
            }
        }
    }
}
