package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity

data class StudentBehaviorItem(
    val student: StudentEntity,
    val positivePoints: Int,
    val negativePoints: Int
)

class BehaviorAdapter(
    private val onItemClick: (StudentEntity) -> Unit
) : ListAdapter<StudentBehaviorItem, BehaviorAdapter.BehaviorViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BehaviorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_behavior, parent, false)
        return BehaviorViewHolder(view)
    }

    override fun onBindViewHolder(holder: BehaviorViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class BehaviorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textStudentName)
        private val textPositive: TextView = itemView.findViewById(R.id.textPositiveBehavior)
        private val textNegative: TextView = itemView.findViewById(R.id.textNegativeBehavior)
        private val textOrder: TextView = itemView.findViewById(R.id.textStudentOrder)

        fun bind(item: StudentBehaviorItem, orderNumber: Int) {
            textName.text = item.student.name
            textOrder.text = "$orderNumber."

            textPositive.text = item.positivePoints.toString()
            textNegative.text = item.negativePoints.toString()
            
            itemView.setOnClickListener { onItemClick(item.student) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<StudentBehaviorItem>() {
            override fun areItemsTheSame(oldItem: StudentBehaviorItem, newItem: StudentBehaviorItem): Boolean {
                return oldItem.student.id == newItem.student.id
            }

            override fun areContentsTheSame(oldItem: StudentBehaviorItem, newItem: StudentBehaviorItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
