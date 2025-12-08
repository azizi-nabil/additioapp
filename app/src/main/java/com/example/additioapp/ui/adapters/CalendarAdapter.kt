package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import java.util.Calendar

data class CalendarDay(
    val day: Int,
    val date: Long,        // Timestamp of start of day
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val eventColors: List<String> = emptyList() // Up to 3 colors for indicators
)

class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    private var days: List<CalendarDay> = emptyList()
    private var selectedDate: Long = 0

    fun submitList(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: Long) {
        selectedDate = date
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.dayContainer)
        private val textDay: TextView = itemView.findViewById(R.id.textDay)
        private val indicator1: View = itemView.findViewById(R.id.indicator1)
        private val indicator2: View = itemView.findViewById(R.id.indicator2)
        private val indicator3: View = itemView.findViewById(R.id.indicator3)

        fun bind(day: CalendarDay) {
            if (day.day == 0) {
                // Empty cell
                textDay.text = ""
                container.isClickable = false
                indicator1.visibility = View.GONE
                indicator2.visibility = View.GONE
                indicator3.visibility = View.GONE
                return
            }

            textDay.text = day.day.toString()
            container.isClickable = true
            container.setOnClickListener { onDayClick(day) }

            // Style based on state
            val context = itemView.context
            
            // Get theme-aware colors
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            val colorOnSurface = typedValue.data
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            val colorOnSurfaceVariant = typedValue.data
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            val colorPrimary = typedValue.data
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
            val colorPrimaryContainer = typedValue.data
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValue, true)
            val colorOnPrimaryContainer = typedValue.data
            
            when {
                day.date == selectedDate -> {
                    textDay.setTextColor(Color.WHITE)
                    // Use a rounded drawable for selected state with accent color
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorPrimary)
                    }
                    container.background = drawable
                }
                day.isToday -> {
                    textDay.setTextColor(colorOnPrimaryContainer)
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorPrimaryContainer)
                    }
                    container.background = drawable
                }
                !day.isCurrentMonth -> {
                    textDay.setTextColor(colorOnSurfaceVariant)
                    container.setBackgroundColor(Color.TRANSPARENT)
                }
                else -> {
                    textDay.setTextColor(colorOnSurface)
                    container.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            // Event indicators
            val indicators = listOf(indicator1, indicator2, indicator3)
            indicators.forEachIndexed { index, indicator ->
                if (index < day.eventColors.size) {
                    indicator.visibility = View.VISIBLE
                    (indicator.background as? GradientDrawable)?.setColor(
                        Color.parseColor(day.eventColors[index])
                    )
                } else {
                    indicator.visibility = View.GONE
                }
            }
        }
    }
}
