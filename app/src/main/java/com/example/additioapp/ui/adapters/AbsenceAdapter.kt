package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.TeacherAbsenceEntity
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class AbsenceAdapter(
    private val classMap: Map<Long, ClassEntity>,
    private val onScheduleClick: (TeacherAbsenceEntity) -> Unit,
    private val onCompleteClick: (TeacherAbsenceEntity) -> Unit,
    private val onDeleteClick: (TeacherAbsenceEntity) -> Unit,
    private val onItemClick: (TeacherAbsenceEntity) -> Unit
) : ListAdapter<TeacherAbsenceEntity, AbsenceAdapter.AbsenceViewHolder>(AbsenceDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_absence, parent, false)
        return AbsenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AbsenceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AbsenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val textClassName: TextView = itemView.findViewById(R.id.textClassName)
        private val chipSessionType: Chip = itemView.findViewById(R.id.chipSessionType)
        private val textAbsenceDate: TextView = itemView.findViewById(R.id.textAbsenceDate)
        private val layoutReplacement: LinearLayout = itemView.findViewById(R.id.layoutReplacement)

        private val textReplacementDate: TextView = itemView.findViewById(R.id.textReplacementDate)
        val textReplacementRoom: TextView = itemView.findViewById(R.id.textReplacementRoom) // Expose internal
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val btnSchedule: ImageButton = itemView.findViewById(R.id.btnSchedule)
        private val btnComplete: ImageButton = itemView.findViewById(R.id.btnComplete)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(absence: TeacherAbsenceEntity) {
            val context = itemView.context
            
            // Get class names for all class IDs
            val classNames = absence.getClassIdList().mapNotNull { classId ->
                classMap[classId]?.name
            }.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Unknown Class"
            
            textClassName.text = classNames
            chipSessionType.text = absence.sessionType
            textAbsenceDate.text = context.getString(
                R.string.absence_absent_on, 
                dateFormat.format(Date(absence.absenceDate))
            )

            // Replacement date
            if (absence.replacementDate != null) {
                layoutReplacement.visibility = View.VISIBLE
                textReplacementDate.text = context.getString(
                    R.string.absence_replacement_on,
                    dateFormat.format(Date(absence.replacementDate))
                )
                
                // Room display
                if (!absence.room.isNullOrEmpty()) {
                    textReplacementRoom.text = "(${absence.room})"
                    textReplacementRoom.visibility = View.VISIBLE
                } else {
                    textReplacementRoom.visibility = View.GONE
                }
            } else {
                layoutReplacement.visibility = View.GONE
            }

            // Status styling
            when (absence.status) {
                TeacherAbsenceEntity.STATUS_PENDING -> {
                    textStatus.text = context.getString(R.string.absence_status_pending)
                    textStatus.setBackgroundResource(R.drawable.bg_status_pending)
                    statusIndicator.setBackgroundColor(Color.parseColor("#FF9800"))
                    btnSchedule.visibility = View.VISIBLE
                    btnComplete.visibility = View.GONE
                }
                TeacherAbsenceEntity.STATUS_SCHEDULED -> {
                    textStatus.text = context.getString(R.string.absence_status_scheduled)
                    textStatus.setBackgroundColor(Color.parseColor("#2196F3"))
                    statusIndicator.setBackgroundColor(Color.parseColor("#2196F3"))
                    btnSchedule.visibility = View.GONE
                    btnComplete.visibility = View.VISIBLE
                }
                TeacherAbsenceEntity.STATUS_COMPLETED -> {
                    textStatus.text = context.getString(R.string.absence_status_completed)
                    textStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                    statusIndicator.setBackgroundColor(Color.parseColor("#4CAF50"))
                    btnSchedule.visibility = View.VISIBLE  // Allow reverting to scheduled
                    btnComplete.visibility = View.GONE
                }
            }

            // Click listeners
            btnSchedule.setOnClickListener { onScheduleClick(absence) }
            btnComplete.setOnClickListener { onCompleteClick(absence) }
            btnDelete.setOnClickListener { onDeleteClick(absence) }
            itemView.setOnClickListener { onItemClick(absence) }
        }
    }

    fun updateClassMap(newClassMap: Map<Long, ClassEntity>) {
        // This forces a redraw with updated class info
        notifyDataSetChanged()
    }

    class AbsenceDiffCallback : DiffUtil.ItemCallback<TeacherAbsenceEntity>() {
        override fun areItemsTheSame(oldItem: TeacherAbsenceEntity, newItem: TeacherAbsenceEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TeacherAbsenceEntity, newItem: TeacherAbsenceEntity): Boolean {
            return oldItem == newItem
        }
    }
}
