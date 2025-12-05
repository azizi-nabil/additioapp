package com.example.additioapp.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.StudentEntity

data class StudentGradeItem(
    val student: StudentEntity,
    var gradeRecord: GradeRecordEntity? = null
)

class GradeEntryAdapter(
    private var items: List<StudentGradeItem> = emptyList(),
    private var isCalculated: Boolean = false,
    private var maxScore: Float = 10.0f, // Default, will be updated
    private val onGradeChanged: (StudentGradeItem, Float, String) -> Unit
) : RecyclerView.Adapter<GradeEntryAdapter.GradeEntryViewHolder>() {

    init {
        setHasStableIds(true)
    }

    fun submitList(newItems: List<StudentGradeItem>) {
        val diffCallback = object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].student.id == newItems[newItemPosition].student.id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = items[oldItemPosition]
                val newItem = newItems[newItemPosition]
                return oldItem.gradeRecord?.score == newItem.gradeRecord?.score &&
                       oldItem.student.name == newItem.student.name
            }
        }
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    fun setIsCalculated(calculated: Boolean) {
        isCalculated = calculated
        notifyDataSetChanged()
    }

    fun setMaxScore(max: Float) {
        maxScore = max
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return items[position].student.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade_entry, parent, false)
        return GradeEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeEntryViewHolder, position: Int) {
        holder.bind(items[position], position, isCalculated, maxScore, onGradeChanged)
    }

    override fun getItemCount(): Int = items.size

    class GradeEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderTextView: TextView = itemView.findViewById(R.id.textStudentOrder)
        private val nameTextView: TextView = itemView.findViewById(R.id.textStudentName)
        private val scoreEditText: com.google.android.material.textfield.TextInputEditText = itemView.findViewById(R.id.editScore)
        private val scoreInputLayout: com.google.android.material.textfield.TextInputLayout = itemView.findViewById(R.id.inputLayoutScore)
        private val statusTextView: TextView = itemView.findViewById(R.id.textStatus)
        private val statusButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnStatus)
        private var textWatcher: TextWatcher? = null

        fun bind(item: StudentGradeItem, position: Int, isCalculated: Boolean, maxScore: Float, onGradeChanged: (StudentGradeItem, Float, String) -> Unit) {
            // Check preferences
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(itemView.context)
            val nameLang = prefs.getString("pref_name_language", "french") ?: "french"
            val listSize = prefs.getString("pref_list_size", "normal") ?: "normal"
            
            // Apply list size
            val nameSize = when (listSize) {
                "compact" -> 12f
                "comfortable" -> 16f
                else -> 14f // normal
            }
            nameTextView.textSize = nameSize
            
            orderTextView.text = "${position + 1}."
            // Use proper display name
            val displayName = if (nameLang == "arabic" && !item.student.displayNameAr.isNullOrEmpty()) {
                item.student.displayNameAr
            } else {
                item.student.displayNameFr
            }
            nameTextView.text = displayName

            val currentStatus = item.gradeRecord?.status ?: "PRESENT"

            // UI State based on Status
            when (currentStatus) {
                "PRESENT" -> {
                    scoreInputLayout.visibility = View.VISIBLE
                    statusTextView.visibility = View.GONE
                    scoreEditText.isEnabled = !isCalculated
                }
                else -> {
                    scoreInputLayout.visibility = View.GONE
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = currentStatus
                    
                    // Color coding for status
                    val color = when(currentStatus) {
                        "ABSENT" -> R.color.behavior_negative // Red
                        "MISSING" -> R.color.tertiary // Orange
                        "EXCUSED" -> R.color.behavior_positive // Green
                        else -> R.color.outline
                    }
                    statusTextView.setTextColor(itemView.context.getColor(color))
                }
            }

            // Remove previous listeners
            scoreEditText.onFocusChangeListener = null
            scoreEditText.setOnEditorActionListener(null)
            if (textWatcher != null) {
                scoreEditText.removeTextChangedListener(textWatcher)
            }

            // Set current score
            val currentText = scoreEditText.text.toString()
            val newText = item.gradeRecord?.score?.toString() ?: ""
            if (currentText != newText) {
                scoreEditText.setText(newText)
            }

            // Helper to update color
            fun updateColor(scoreStr: String) {
                val score = scoreStr.toFloatOrNull()
                if (score != null) {
                    val colorRes = if (score >= maxScore / 2) R.color.soft_green else R.color.soft_red
                    scoreInputLayout.boxBackgroundColor = itemView.context.getColor(colorRes)
                } else {
                    scoreInputLayout.boxBackgroundColor = itemView.context.getColor(android.R.color.transparent)
                }
            }
            
            // Initial color update
            updateColor(newText)

            // TextWatcher
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateColor(s.toString())
                }
            }
            scoreEditText.addTextChangedListener(textWatcher)

            // Save on Focus Loss
            scoreEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveGrade(item, scoreEditText.text.toString(), "PRESENT", onGradeChanged)
                }
            }

            // Save on Editor Action
            scoreEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    saveGrade(item, scoreEditText.text.toString(), "PRESENT", onGradeChanged)
                    false 
                } else {
                    false
                }
            }

            // Status Button Click
            statusButton.setOnClickListener {
                val popup = androidx.appcompat.widget.PopupMenu(itemView.context, statusButton)
                popup.menu.add("Present")
                popup.menu.add("Absent")
                popup.menu.add("Missing")
                popup.menu.add("Excused")

                popup.setOnMenuItemClickListener { menuItem ->
                    val newStatus = menuItem.title.toString().uppercase()
                    // If switching to PRESENT, keep existing score or 0. If others, score is 0.
                    val scoreStr = if (newStatus == "PRESENT") scoreEditText.text.toString() else "0"
                    
                    // Update UI immediately (optimistic)
                    if (newStatus == "PRESENT") {
                        scoreInputLayout.visibility = View.VISIBLE
                        statusTextView.visibility = View.GONE
                    } else {
                        scoreInputLayout.visibility = View.GONE
                        statusTextView.visibility = View.VISIBLE
                        statusTextView.text = newStatus
                    }

                    saveGrade(item, scoreStr, newStatus, onGradeChanged)
                    true
                }
                popup.show()
            }
        }

        private fun saveGrade(item: StudentGradeItem, text: String, status: String, onGradeChanged: (StudentGradeItem, Float, String) -> Unit) {
            val score = text.toFloatOrNull() ?: 0f
            // Trigger if score OR status changed
            if (item.gradeRecord == null || item.gradeRecord?.score != score || item.gradeRecord?.status != status) {
                onGradeChanged(item, score, status)
            }
        }
    }
}
