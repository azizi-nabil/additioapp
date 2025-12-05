package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.AttendanceStatusEntity
import com.example.additioapp.data.model.StudentEntity

// Helper data class to combine Student and their Attendance status
data class StudentAttendanceItem(
    val student: StudentEntity,
    var attendance: AttendanceRecordEntity? = null
)

class AttendanceAdapter(
    private var items: List<StudentAttendanceItem> = emptyList(),
    private val onStatusChanged: (StudentEntity, String) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    private var statusCycle: List<AttendanceStatusEntity> = emptyList()

    fun setStatuses(statuses: List<AttendanceStatusEntity>) {
        statusCycle = statuses
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<StudentAttendanceItem>) {
        // Only refresh if this is a NEW list (e.g., date changed)
        // Don't refresh for in-place updates (e.g., user tapping)
        val isSameRef = items === newItems
        android.util.Log.d("AttendanceAdapter", "submitList called. Same ref? $isSameRef. Will notify? ${!isSameRef}")
        
        // DISABLED: This was causing rebinding after taps
        // The initial binding happens via setStatuses()
        items = newItems
        if (!isSameRef) {
            android.util.Log.d("AttendanceAdapter", "Calling notifyDataSetChanged()")
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(items[position], statusCycle, onStatusChanged)
    }

    override fun getItemCount(): Int = items.size

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textStudentName)
        private val idTextView: TextView = itemView.findViewById(R.id.textStudentId)
        private val commentTextView: TextView = itemView.findViewById(R.id.textComment)
        private val badgeTextView: TextView = itemView.findViewById(R.id.textStatusBadge)

        fun bind(
            item: StudentAttendanceItem,
            statuses: List<AttendanceStatusEntity>,
            onStatusChanged: (StudentEntity, String) -> Unit
        ) {
            // Check preferences
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(itemView.context)
            val nameLang = prefs.getString("pref_name_language", "french") ?: "french"
            val listSize = prefs.getString("pref_list_size", "normal") ?: "normal"
            
            // Apply list size
            val (nameSize, idSize) = when (listSize) {
                "compact" -> Pair(12f, 11f)
                "comfortable" -> Pair(16f, 14f)
                else -> Pair(14f, 12f) // normal
            }
            nameTextView.textSize = nameSize
            idTextView.textSize = idSize
            
            // Use proper display name
            val displayName = if (nameLang == "arabic" && !item.student.displayNameAr.isNullOrEmpty()) {
                item.student.displayNameAr
            } else {
                item.student.displayNameFr
            }
            nameTextView.text = displayName
            idTextView.text = "ID: ${item.student.displayMatricule}"
            
            val comment = item.attendance?.comment
            if (!comment.isNullOrBlank()) {
                commentTextView.text = comment
                commentTextView.visibility = View.VISIBLE
            } else {
                commentTextView.visibility = View.GONE
            }

            val currentIndex = statuses.indexOfFirst { it.code == item.attendance?.status }
            val nextIndex = if (currentIndex >= 0) currentIndex else 0
            val currentStatus = statuses.getOrNull(nextIndex)
            applyStatus(currentStatus)

            val clickListener = View.OnClickListener {
                // Instead of cycling, just invoke callback to open dialog
                onStatusChanged(item.student, item.attendance?.status ?: "")
            }

            itemView.setOnClickListener(clickListener)
            badgeTextView.setOnClickListener(clickListener)
        }

        private fun applyStatus(status: AttendanceStatusEntity?) {
            // Log what we are applying
            android.util.Log.d("AttendanceAdapter", "Applying status: ${status?.code} Color: ${status?.colorHex} to ${nameTextView.text}")

            if (status == null) {
                badgeTextView.text = "-"
                androidx.core.view.ViewCompat.setBackgroundTintList(
                    badgeTextView, 
                    android.content.res.ColorStateList.valueOf(0xFF9E9E9E.toInt())
                )
                badgeTextView.setTextColor(Color.WHITE)
                badgeTextView.tag = null
                return
            }
            
            badgeTextView.text = status.code
            badgeTextView.tag = status.code
            val color = status.colorHex?.let { parseColorSafe(it) } ?: 0xFF9E9E9E.toInt()
            
            androidx.core.view.ViewCompat.setBackgroundTintList(
                badgeTextView, 
                android.content.res.ColorStateList.valueOf(color)
            )
            badgeTextView.setTextColor(Color.WHITE)
            badgeTextView.invalidate()
        }

        private fun parseColorSafe(colorString: String): Int {
            return try {
                android.graphics.Color.parseColor(colorString)
            } catch (e: Exception) {
                0xFF9E9E9E.toInt()
            }
        }
    }
}
