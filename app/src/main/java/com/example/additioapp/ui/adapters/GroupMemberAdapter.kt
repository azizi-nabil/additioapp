package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity

class GroupMemberAdapter(
    private val onRemove: (StudentEntity) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.ViewHolder>() {

    private var members: List<StudentEntity> = emptyList()
    
    // Color palette for avatars
    private val avatarColors = listOf(
        "#2196F3", "#4CAF50", "#FF9800", "#9C27B0", 
        "#F44336", "#00BCD4", "#E91E63", "#3F51B5"
    )

    fun submitList(list: List<StudentEntity>) {
        members = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(members[position], position)
    }

    override fun getItemCount(): Int = members.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textInitial: TextView = itemView.findViewById(R.id.textInitial)
        private val textName: TextView = itemView.findViewById(R.id.textStudentName)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(student: StudentEntity, position: Int) {
            val displayName = student.displayNameFr ?: student.name
            textName.text = displayName
            
            // Set initial
            val initial = displayName.firstOrNull()?.uppercaseChar() ?: '?'
            textInitial.text = initial.toString()
            
            // Set avatar background color
            val colorIndex = position % avatarColors.size
            val bgDrawable = textInitial.background.mutate()
            bgDrawable.setTint(Color.parseColor(avatarColors[colorIndex]))
            textInitial.background = bgDrawable
            
            btnRemove.setOnClickListener {
                onRemove(student)
            }
        }
    }
}
