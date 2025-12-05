package com.example.additioapp.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onTaskChecked: (TaskEntity, Boolean) -> Unit,
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onDeleteClick: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    private var tasks: List<TaskEntity> = emptyList()
    private var classNames: Map<Long, String> = emptyMap()

    fun submitList(newTasks: List<TaskEntity>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun setClassNames(names: Map<Long, String>) {
        classNames = names
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkTask: CheckBox = itemView.findViewById(R.id.checkTask)
        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        private val textTitle: TextView = itemView.findViewById(R.id.textTaskTitle)
        private val textDue: TextView = itemView.findViewById(R.id.textTaskDue)
        private val textClass: TextView = itemView.findViewById(R.id.textTaskClass)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTask)

        fun bind(task: TaskEntity) {
            checkTask.setOnCheckedChangeListener(null)
            checkTask.isChecked = task.isCompleted
            
            textTitle.text = task.title
            if (task.isCompleted) {
                textTitle.paintFlags = textTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                textTitle.alpha = 0.6f
            } else {
                textTitle.paintFlags = textTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                textTitle.alpha = 1.0f
            }

            // Priority color
            val priorityColor = when (task.priority) {
                "HIGH" -> "#F44336"    // Red
                "MEDIUM" -> "#FF9800"  // Orange
                "LOW" -> "#4CAF50"     // Green
                else -> "#9E9E9E"      // Gray
            }
            (priorityIndicator.background as? GradientDrawable)?.setColor(Color.parseColor(priorityColor))

            // Due date
            if (task.dueDate != null) {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                textDue.text = "Due: ${dateFormat.format(Date(task.dueDate))}"
                textDue.visibility = View.VISIBLE
                
                // Highlight overdue
                if (!task.isCompleted && task.dueDate < System.currentTimeMillis()) {
                    textDue.setTextColor(Color.parseColor("#F44336"))
                } else {
                    textDue.setTextColor(Color.parseColor("#757575"))
                }
            } else {
                textDue.visibility = View.GONE
            }

            // Class name
            if (task.classId != null && classNames.containsKey(task.classId)) {
                textClass.text = classNames[task.classId]
                textClass.visibility = View.VISIBLE
            } else {
                textClass.visibility = View.GONE
            }

            checkTask.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }
            
            itemView.setOnClickListener { onTaskClick(task) }
            btnDelete.setOnClickListener { onDeleteClick(task) }
        }
    }
}
