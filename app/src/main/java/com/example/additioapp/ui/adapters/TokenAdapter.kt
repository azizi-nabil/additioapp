package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R

class TokenAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<String, TokenAdapter.TokenViewHolder>(TokenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_token, parent, false)
        return TokenViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TokenViewHolder(itemView: View, val onItemClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val button: Button = itemView.findViewById(R.id.btnToken)

        fun bind(token: String) {
            button.text = token
            button.setOnClickListener { onItemClick(token) }
        }
    }

    class TokenDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }
}
