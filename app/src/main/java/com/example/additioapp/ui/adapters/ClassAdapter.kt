package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.ClassWithSummary

enum class ClassAction {
    CLICK, EDIT, ARCHIVE, UNARCHIVE, DELETE
}

class ClassAdapter(private val onAction: (ClassEntity, ClassAction) -> Unit) :
    ListAdapter<ClassWithSummary, ClassAdapter.ClassViewHolder>(ClassDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view, onAction)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClassViewHolder(itemView: View, val onAction: (ClassEntity, ClassAction) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        
        private val nameTextView: TextView = itemView.findViewById(R.id.textClassName)
        private val locationTextView: TextView = itemView.findViewById(R.id.textClassLocation)
        private val yearTextView: TextView = itemView.findViewById(R.id.textClassYear)
        private val semesterTextView: TextView = itemView.findViewById(R.id.textClassSemester)
        private val studentCountTextView: TextView = itemView.findViewById(R.id.textStudentCount)
        private val colorStrip: View = itemView.findViewById(R.id.colorStrip)
        private val btnMoreOptions: ImageView = itemView.findViewById(R.id.btnMoreOptions)

        fun bind(classSummary: ClassWithSummary) {
            val classEntity = classSummary.classEntity
            
            nameTextView.text = classEntity.name
            locationTextView.text = if (classEntity.location.isNotEmpty()) "📍 ${classEntity.location}" else "📍 No location"
            yearTextView.text = classEntity.year
            semesterTextView.text = if (classEntity.semester == "Semester 1") "S1" else "S2"
            studentCountTextView.text = classSummary.studentCount.toString()

            // Set colors
            val color = try {
                Color.parseColor(classEntity.color)
            } catch (e: Exception) {
                Color.parseColor("#2196F3") // Default blue
            }
            colorStrip.setBackgroundColor(color)
            
            // Update semester badge color based on semester
            val semesterColor = if (classEntity.semester == "Semester 1") "#9C27B0" else "#FF9800"
            semesterTextView.background.setTint(Color.parseColor(semesterColor))

            itemView.setOnClickListener { onAction(classEntity, ClassAction.CLICK) }

            btnMoreOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Edit")
                if (classEntity.isArchived) {
                    popup.menu.add("Unarchive")
                } else {
                    popup.menu.add("Archive")
                }
                popup.menu.add("Delete")

                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edit" -> onAction(classEntity, ClassAction.EDIT)
                        "Archive" -> onAction(classEntity, ClassAction.ARCHIVE)
                        "Unarchive" -> onAction(classEntity, ClassAction.UNARCHIVE)
                        "Delete" -> onAction(classEntity, ClassAction.DELETE)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    class ClassDiffCallback : DiffUtil.ItemCallback<ClassWithSummary>() {
        override fun areItemsTheSame(oldItem: ClassWithSummary, newItem: ClassWithSummary): Boolean {
            return oldItem.classEntity.id == newItem.classEntity.id
        }

        override fun areContentsTheSame(oldItem: ClassWithSummary, newItem: ClassWithSummary): Boolean {
            return oldItem == newItem
        }
    }
}
