package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.GradeItemEntity

class GradeItemAdapter(
    private val onItemClick: (GradeItemEntity) -> Unit,
    private val onMoreClick: (GradeItemEntity, View) -> Unit
) : ListAdapter<GradeItemEntity, GradeItemAdapter.GradeItemViewHolder>(GradeItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade_item, parent, false)
        return GradeItemViewHolder(view, onItemClick, onMoreClick)
    }

    override fun onBindViewHolder(holder: GradeItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GradeItemViewHolder(
        itemView: View,
        val onItemClick: (GradeItemEntity) -> Unit,
        val onMoreClick: (GradeItemEntity, View) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textGradeItemName)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textGradeItemCategory)
        private val maxScoreTextView: TextView = itemView.findViewById(R.id.textGradeItemMaxScore)
        private val dateTextView: TextView = itemView.findViewById(R.id.textGradeItemDate)
        private val moreButton: android.widget.ImageView = itemView.findViewById(R.id.btnMoreOptions)
        private val calculatedIndicator: TextView = itemView.findViewById(R.id.textCalculatedIndicator)
        private val gradeIcon: android.widget.ImageView = itemView.findViewById(R.id.imgGradeIcon)
        private val cardView: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(item: GradeItemEntity) {
            nameTextView.text = item.name
            categoryTextView.text = item.category
            maxScoreTextView.text = "Max: ${item.maxScore}"
            
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            dateTextView.text = dateFormat.format(java.util.Date(item.date))
            
            // Show calculated indicator, swap icon and change card color if formula is not null
            val isCalculated = !item.formula.isNullOrEmpty()
            if (isCalculated) {
                calculatedIndicator.visibility = View.VISIBLE
                calculatedIndicator.text = "Calculated"
                calculatedIndicator.setTypeface(null, android.graphics.Typeface.BOLD)
                gradeIcon.setImageResource(R.drawable.ic_sigma_orange)
                gradeIcon.imageTintList = null // Remove tint for PNG
                gradeIcon.background = null // Remove circle background
                
                val softGreen = androidx.core.content.ContextCompat.getColor(itemView.context, R.color.soft_green)
                cardView.setCardBackgroundColor(softGreen)
            } else {
                calculatedIndicator.visibility = View.GONE
                gradeIcon.setImageResource(R.drawable.ic_assignment)
                
                val onPrimaryContainer = androidx.core.content.ContextCompat.getColor(itemView.context, R.color.on_primary_container)
                gradeIcon.imageTintList = android.content.res.ColorStateList.valueOf(onPrimaryContainer)
                
                gradeIcon.setBackgroundResource(R.drawable.bg_circle_primary_container)
                
                val surfaceColor = androidx.core.content.ContextCompat.getColor(itemView.context, R.color.surface)
                cardView.setCardBackgroundColor(surfaceColor)
            }
            
            itemView.setOnClickListener { onItemClick(item) }
            moreButton.setOnClickListener { onMoreClick(item, it) }
        }
    }

    class GradeItemDiffCallback : DiffUtil.ItemCallback<GradeItemEntity>() {
        override fun areItemsTheSame(oldItem: GradeItemEntity, newItem: GradeItemEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GradeItemEntity, newItem: GradeItemEntity): Boolean {
            return oldItem == newItem
        }
    }
}
