package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.EventEntity

class EventAdapter(
    private val onEventClick: (EventEntity) -> Unit,
    private val onDeleteClick: (EventEntity) -> Unit,
    private val onLongClick: (EventEntity) -> Unit = {}
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    private var events: List<EventEntity> = emptyList()
    private var classNames: Map<Long, Pair<String, String>> = emptyMap() // classId -> (name, color)
    private var eventClassNames: Map<Long, List<String>> = emptyMap()  // eventId -> list of class names

    fun submitList(newEvents: List<EventEntity>) {
        val oldList = events
        events = newEvents
        
        val diffCallback = EventDiffCallback(oldList, newEvents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setClassInfo(classInfo: Map<Long, Pair<String, String>>) {
        classNames = classInfo
        notifyDataSetChanged()
    }
    
    fun setEventClassNames(names: Map<Long, List<String>>) {
        eventClassNames = names
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
        private val textDescription: TextView = itemView.findViewById(R.id.textEventDescription)
        private val layoutLocation: View = itemView.findViewById(R.id.layoutEventLocation)
        private val textLocation: TextView = itemView.findViewById(R.id.textEventLocation)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEvent)

        fun bind(event: EventEntity) {
            textTitle.text = event.title
            
            // Recurrence Icon
            val iconRecurrence = itemView.findViewById<android.widget.ImageView>(R.id.iconRecurrence)
            if (iconRecurrence != null) {
                iconRecurrence.visibility = if (event.recurrenceType != "NONE" && event.recurrenceType != null) View.VISIBLE else View.GONE
            }

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
            
            // Description
            if (event.description.isNotEmpty()) {
                textDescription.text = event.description
                textDescription.visibility = View.VISIBLE
            } else {
                textDescription.visibility = View.GONE
            }
            
            // Location
            if (event.location.isNotEmpty()) {
                textLocation.text = event.location
                layoutLocation.visibility = View.VISIBLE
            } else {
                layoutLocation.visibility = View.GONE
            }

            // Color
            val color = when {
                event.color != null -> event.color
                event.classId != null -> classNames[event.classId]?.second ?: "#2196F3"
                else -> getEventTypeColor(event.eventType)
            }
            (colorStrip.background as? GradientDrawable)?.setColor(Color.parseColor(color))
                ?: colorStrip.setBackgroundColor(Color.parseColor(color))

            // Class names - show all classes
            val classNamesList = eventClassNames[event.id]
            if (!classNamesList.isNullOrEmpty()) {
                textClass.text = classNamesList.joinToString(", ")
                textClass.visibility = View.VISIBLE
            } else if (event.classId != null && classNames.containsKey(event.classId)) {
                // Fallback to legacy single class
                textClass.text = classNames[event.classId]?.first
                textClass.visibility = View.VISIBLE
            } else {
                textClass.visibility = View.GONE
            }

            itemView.setOnClickListener { onEventClick(event) }
            itemView.setOnLongClickListener { 
                onLongClick(event)
                true
            }
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

// DiffUtil Callback for efficient list updates
class EventDiffCallback(
    private val oldList: List<EventEntity>,
    private val newList: List<EventEntity>
) : DiffUtil.Callback() {
    
    override fun getOldListSize(): Int = oldList.size
    
    override fun getNewListSize(): Int = newList.size
    
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
    
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
