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
import com.example.additioapp.data.model.ScheduleItemEntity

class ScheduleAdapter(
    private val onItemClick: (ScheduleItemEntity) -> Unit,
    private val onDeleteClick: (ScheduleItemEntity) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var items: List<ScheduleItemEntity> = emptyList()
    private var classInfo: Map<Long, Pair<String, String>> = emptyMap() // classId -> (name, color)

    fun submitList(newItems: List<ScheduleItemEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setClassInfo(info: Map<Long, Pair<String, String>>) {
        classInfo = info
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorStrip: View = itemView.findViewById(R.id.colorStrip)
        private val textClassName: TextView = itemView.findViewById(R.id.textClassName)
        private val textTime: TextView = itemView.findViewById(R.id.textScheduleTime)
        private val textSessionType: TextView = itemView.findViewById(R.id.textSessionType)
        private val textRoom: TextView = itemView.findViewById(R.id.textRoom)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSchedule)

        fun bind(item: ScheduleItemEntity) {
            val info = classInfo[item.classId]
            textClassName.text = info?.first ?: "Unknown Class"
            
            // Color
            val color = info?.second ?: "#2196F3"
            (colorStrip.background as? GradientDrawable)?.setColor(Color.parseColor(color))
                ?: colorStrip.setBackgroundColor(Color.parseColor(color))

            // Time
            textTime.text = "${item.startTime} - ${item.endTime}"

            // Session type
            textSessionType.text = item.sessionType

            // Room
            if (item.room.isNotEmpty()) {
                textRoom.text = "📍 ${item.room}"
                textRoom.visibility = View.VISIBLE
            } else {
                textRoom.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
