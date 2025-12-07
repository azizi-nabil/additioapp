package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.EventEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.data.model.TaskEntity

// Sealed class for different search result types
sealed class SearchResult {
    data class StudentResult(val student: StudentEntity) : SearchResult()
    data class ClassResult(val classEntity: ClassEntity) : SearchResult()
    data class EventResult(val event: EventEntity) : SearchResult()
    data class TaskResult(val task: TaskEntity) : SearchResult()
}

class SearchResultAdapter(
    private val onStudentClick: (StudentEntity) -> Unit,
    private val onClassClick: (ClassEntity) -> Unit,
    private val onEventClick: (EventEntity) -> Unit,
    private val onTaskClick: (TaskEntity) -> Unit
) : ListAdapter<SearchResult, RecyclerView.ViewHolder>(SearchResultDiffCallback()) {
    
    companion object {
        private const val TYPE_STUDENT = 0
        private const val TYPE_CLASS = 1
        private const val TYPE_EVENT = 2
        private const val TYPE_TASK = 3
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResult.StudentResult -> TYPE_STUDENT
            is SearchResult.ClassResult -> TYPE_CLASS
            is SearchResult.EventResult -> TYPE_EVENT
            is SearchResult.TaskResult -> TYPE_TASK
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_STUDENT -> StudentViewHolder(inflater.inflate(R.layout.item_search_result, parent, false))
            TYPE_CLASS -> ClassViewHolder(inflater.inflate(R.layout.item_search_result, parent, false))
            TYPE_EVENT -> EventViewHolder(inflater.inflate(R.layout.item_search_result, parent, false))
            TYPE_TASK -> TaskViewHolder(inflater.inflate(R.layout.item_search_result, parent, false))
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val result = getItem(position)) {
            is SearchResult.StudentResult -> (holder as StudentViewHolder).bind(result.student)
            is SearchResult.ClassResult -> (holder as ClassViewHolder).bind(result.classEntity)
            is SearchResult.EventResult -> (holder as EventViewHolder).bind(result.event)
            is SearchResult.TaskResult -> (holder as TaskViewHolder).bind(result.task)
        }
    }
    
    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: TextView = itemView.findViewById(R.id.iconType)
        private val titleView: TextView = itemView.findViewById(R.id.textTitle)
        private val subtitleView: TextView = itemView.findViewById(R.id.textSubtitle)
        
        fun bind(student: StudentEntity) {
            iconView.text = "👤"
            titleView.text = student.displayNameFr
            subtitleView.text = "Student • ${student.displayMatricule}"
            itemView.setOnClickListener { onStudentClick(student) }
        }
    }
    
    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: TextView = itemView.findViewById(R.id.iconType)
        private val titleView: TextView = itemView.findViewById(R.id.textTitle)
        private val subtitleView: TextView = itemView.findViewById(R.id.textSubtitle)
        
        fun bind(classEntity: ClassEntity) {
            iconView.text = "📚"
            titleView.text = classEntity.name
            subtitleView.text = "Class • ${classEntity.year}"
            itemView.setOnClickListener { onClassClick(classEntity) }
        }
    }
    
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: TextView = itemView.findViewById(R.id.iconType)
        private val titleView: TextView = itemView.findViewById(R.id.textTitle)
        private val subtitleView: TextView = itemView.findViewById(R.id.textSubtitle)
        
        fun bind(event: EventEntity) {
            iconView.text = "📅"
            titleView.text = event.title
            val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(event.date))
            subtitleView.text = "Event • $dateStr"
            itemView.setOnClickListener { onEventClick(event) }
        }
    }
    
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: TextView = itemView.findViewById(R.id.iconType)
        private val titleView: TextView = itemView.findViewById(R.id.textTitle)
        private val subtitleView: TextView = itemView.findViewById(R.id.textSubtitle)
        
        fun bind(task: TaskEntity) {
            iconView.text = if (task.isCompleted) "✅" else "📝"
            titleView.text = task.title
            val status = if (task.isCompleted) "Completed" else "Pending"
            subtitleView.text = "Task • $status"
            itemView.setOnClickListener { onTaskClick(task) }
        }
    }
}

class SearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
    override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
        return when {
            oldItem is SearchResult.StudentResult && newItem is SearchResult.StudentResult ->
                oldItem.student.id == newItem.student.id
            oldItem is SearchResult.ClassResult && newItem is SearchResult.ClassResult ->
                oldItem.classEntity.id == newItem.classEntity.id
            oldItem is SearchResult.EventResult && newItem is SearchResult.EventResult ->
                oldItem.event.id == newItem.event.id
            oldItem is SearchResult.TaskResult && newItem is SearchResult.TaskResult ->
                oldItem.task.id == newItem.task.id
            else -> false
        }
    }
    
    override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
        return oldItem == newItem
    }
}
