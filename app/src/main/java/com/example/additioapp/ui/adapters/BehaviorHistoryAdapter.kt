package com.example.additioapp.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.BehaviorRecordEntity
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BehaviorHistoryAdapter(
    private val onDeleteClick: (BehaviorRecordEntity) -> Unit
) : ListAdapter<BehaviorRecordEntity, BehaviorHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_behavior_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.textBehaviorDate)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textBehaviorCategory)
        private val commentTextView: TextView = itemView.findViewById(R.id.textBehaviorComment)
        private val pointsChip: Chip = itemView.findViewById(R.id.chipPoints)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteBehavior)
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(item: BehaviorRecordEntity, onDeleteClick: (BehaviorRecordEntity) -> Unit) {
            dateTextView.text = dateFormat.format(Date(item.date))
            categoryTextView.text = item.category

            if (item.comment.isNullOrBlank()) {
                commentTextView.visibility = View.GONE
            } else {
                commentTextView.visibility = View.VISIBLE
                commentTextView.text = item.comment
            }
            
            pointsChip.text = if (item.points > 0) "+${item.points}" else "${item.points}"
            
            if (item.points > 0) {
                pointsChip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#2E7D32")) // Green
            } else {
                pointsChip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#C62828")) // Red
            }

            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BehaviorRecordEntity>() {
        override fun areItemsTheSame(oldItem: BehaviorRecordEntity, newItem: BehaviorRecordEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BehaviorRecordEntity, newItem: BehaviorRecordEntity): Boolean {
            return oldItem == newItem
        }
    }
}
