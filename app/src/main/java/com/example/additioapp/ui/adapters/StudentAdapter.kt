package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity

data class StudentStats(
    val coursPresent: Int,
    val coursTotal: Int,
    val tdAbsent: Int,
    val tdExcused: Int,
    val tdTotal: Int,
    val tpAbsent: Int,
    val tpExcused: Int,
    val tpTotal: Int,
    val behaviorPositive: Int,
    val behaviorNegative: Int,
    val hasGrades: Boolean = false
)

class StudentAdapter(
    private var items: List<StudentEntity> = emptyList(),
    private var attendanceStats: Map<Long, StudentStats> = emptyMap(),
    private val onStudentClick: (StudentEntity) -> Unit,
    private val onEditClick: (StudentEntity) -> Unit = {},
    private val onDeleteClick: (StudentEntity) -> Unit = {},
    private val onReportClick: (StudentEntity) -> Unit = {},
    private val onGradesClick: (StudentEntity) -> Unit = {},
    private val onNotesClick: (StudentEntity) -> Unit = {},
    private val onBehaviorClick: (StudentEntity, String) -> Unit = { _, _ -> },
    private val onSelectionChanged: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private var originalItems: List<StudentEntity> = emptyList()
    private var filteredItems: List<StudentEntity> = emptyList()
    
    // Selection mode
    var isSelectionMode = false
        private set
    private val selectedItems = mutableSetOf<Long>()

    fun submitList(newItems: List<StudentEntity>, newStats: Map<Long, StudentStats> = attendanceStats) {
        val oldList = filteredItems
        items = newItems
        originalItems = newItems
        filteredItems = newItems
        attendanceStats = newStats
        
        val diffCallback = StudentDiffCallback(oldList, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    fun filter(query: String) {
        val oldList = filteredItems
        filteredItems = if (query.isEmpty()) {
            originalItems
        } else {
            originalItems.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.studentId.contains(query, ignoreCase = true) ||
                it.matricule.contains(query, ignoreCase = true)
            }
        }
        val diffCallback = StudentDiffCallback(oldList, filteredItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    // Selection mode methods
    fun enterSelectionMode(initialStudentId: Long? = null) {
        isSelectionMode = true
        selectedItems.clear()
        initialStudentId?.let { selectedItems.add(it) }
        onSelectionChanged?.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        onSelectionChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun toggleSelection(studentId: Long) {
        if (selectedItems.contains(studentId)) {
            selectedItems.remove(studentId)
        } else {
            selectedItems.add(studentId)
        }
        onSelectionChanged?.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        filteredItems.forEach { selectedItems.add(it.id) }
        onSelectionChanged?.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun getSelectedStudents(): List<StudentEntity> {
        return filteredItems.filter { selectedItems.contains(it.id) }
    }

    fun getSelectedCount(): Int = selectedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = filteredItems[position]
        val isSelected = selectedItems.contains(student.id)
        holder.bind(
            student, 
            attendanceStats[student.id], 
            isSelectionMode,
            isSelected,
            onStudentClick,
            onEditClick,
            onDeleteClick,
            onReportClick, 
            onGradesClick,
            onNotesClick,
            onBehaviorClick,
            { enterSelectionMode(student.id) },
            { toggleSelection(student.id) }
        )
    }

    override fun getItemCount(): Int = filteredItems.size

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textStudentName)
        private val idTextView: TextView = itemView.findViewById(R.id.textStudentId)
        private val orderTextView: TextView = itemView.findViewById(R.id.textStudentOrder)
        private val checkBox: CheckBox? = itemView.findViewById(R.id.checkboxSelect)
        private val iconNotes: TextView? = itemView.findViewById(R.id.iconNotes)
        private val layoutStatsCours: View = itemView.findViewById(R.id.layoutStatsCours)
        private val statsCoursTextView: TextView = itemView.findViewById(R.id.textStatsCours)
        private val layoutStatsTD: View = itemView.findViewById(R.id.layoutStatsTD)
        private val statsTDTextView: TextView = itemView.findViewById(R.id.textStatsTD)
        private val layoutStatsTP: View = itemView.findViewById(R.id.layoutStatsTP)
        private val statsTPTextView: TextView = itemView.findViewById(R.id.textStatsTP)
        private val layoutBehaviorPos: View = itemView.findViewById(R.id.layoutBehaviorPos)
        private val behaviorPosTextView: TextView = itemView.findViewById(R.id.textStudentBehaviorPos)
        private val layoutBehaviorNeg: View = itemView.findViewById(R.id.layoutBehaviorNeg)
        private val behaviorNegTextView: TextView = itemView.findViewById(R.id.textStudentBehaviorNeg)
        private val btnMoreOptions: android.widget.ImageView = itemView.findViewById(R.id.btnMoreOptions)
        private val layoutRow1: View = itemView.findViewById(R.id.layoutRow1)
        private val layoutRow2: View = itemView.findViewById(R.id.layoutRow2)

        fun bind(
            student: StudentEntity, 
            stats: StudentStats?,
            isSelectionMode: Boolean,
            isSelected: Boolean,
            onStudentClick: (StudentEntity) -> Unit,
            onEditClick: (StudentEntity) -> Unit,
            onDeleteClick: (StudentEntity) -> Unit,
            onReportClick: (StudentEntity) -> Unit, 
            onGradesClick: (StudentEntity) -> Unit,
            onNotesClick: (StudentEntity) -> Unit,
            onBehaviorClick: (StudentEntity, String) -> Unit,
            onLongPress: () -> Unit,
            onToggleSelection: () -> Unit
        ) {
            orderTextView.text = "${adapterPosition + 1}."
            
            // Check preferences
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(itemView.context)
            val nameLang = prefs.getString("pref_name_language", "french") ?: "french"
            val listSize = prefs.getString("pref_list_size", "normal") ?: "normal"
            
            // Apply list size
            val (nameSize, idSize, padding) = when (listSize) {
                "compact" -> Triple(12f, 11f, 8)
                "comfortable" -> Triple(16f, 14f, 16)
                else -> Triple(14f, 12f, 12) // normal
            }
            nameTextView.textSize = nameSize
            idTextView.textSize = idSize
            val paddingPx = (padding * itemView.context.resources.displayMetrics.density).toInt()
            (itemView as? com.google.android.material.card.MaterialCardView)?.setContentPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            
            val displayName = if (nameLang == "arabic" && !student.displayNameAr.isNullOrEmpty()) {
                student.displayNameAr
            } else {
                student.displayNameFr
            }
            nameTextView.text = displayName
            idTextView.text = itemView.context.getString(R.string.student_id_format, student.displayMatricule)
            
            // Show notes indicator if student has notes
            iconNotes?.visibility = if (student.hasNotes) View.VISIBLE else View.GONE

            // Selection mode UI
            if (isSelectionMode) {
                checkBox?.visibility = View.VISIBLE
                checkBox?.isChecked = isSelected
                orderTextView.visibility = View.GONE
                
                // Use soft red for selected items (indicates dangerous delete action)
                val cardView = itemView as? com.google.android.material.card.MaterialCardView
                if (isSelected) {
                    cardView?.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")) // Soft red
                    cardView?.strokeColor = android.graphics.Color.parseColor("#EF5350") // Red border
                    cardView?.strokeWidth = 2
                } else {
                    // Use theme-aware color for dark mode support
                    val typedValue = android.util.TypedValue()
                    itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
                    cardView?.setCardBackgroundColor(typedValue.data)
                    cardView?.strokeWidth = 0
                }
                
                itemView.setOnClickListener { onToggleSelection() }
                checkBox?.setOnClickListener { onToggleSelection() }
            } else {
                checkBox?.visibility = View.GONE
                orderTextView.visibility = View.VISIBLE
                
                // Reset card styling
                // Reset card styling with theme-aware color
                val cardView = itemView as? com.google.android.material.card.MaterialCardView
                val typedValue = android.util.TypedValue()
                itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
                cardView?.setCardBackgroundColor(typedValue.data)
                cardView?.strokeWidth = 0
                
                itemView.setOnClickListener { onStudentClick(student) }
                itemView.setOnLongClickListener { 
                    onLongPress()
                    true
                }
            }
            
            if (stats != null) {
                val coursPct = if (stats.coursTotal > 0) (stats.coursPresent.toFloat() / stats.coursTotal) * 100 else 0f
                val tdPct = if (stats.tdTotal > 0) (stats.tdAbsent.toFloat() / stats.tdTotal) * 100 else 0f
                val tpPct = if (stats.tpTotal > 0) (stats.tpAbsent.toFloat() / stats.tpTotal) * 100 else 0f
                
                // Cours Stats (Presence)
                if (stats.coursPresent > 0) {
                    layoutStatsCours.visibility = View.VISIBLE
                    statsCoursTextView.text = "${stats.coursPresent} (${"%.0f".format(coursPct)}%)"
                } else {
                    layoutStatsCours.visibility = View.GONE
                }

                // TD Stats - show justified in green
                if (stats.tdAbsent > 0) {
                    layoutStatsTD.visibility = View.VISIBLE
                    val tdText = if (stats.tdExcused > 0) {
                        val text = "${stats.tdAbsent}:${stats.tdExcused} (${"%.0f".format(tdPct)}%)"
                        val spannable = android.text.SpannableString(text)
                        val colonIndex = text.indexOf(':')
                        val parenIndex = text.indexOf('(')
                        if (colonIndex >= 0 && parenIndex > colonIndex) {
                            spannable.setSpan(
                                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#4CAF50")),
                                colonIndex + 1,
                                parenIndex - 1,
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        spannable
                    } else {
                        "${stats.tdAbsent} (${"%.0f".format(tdPct)}%)"
                    }
                    statsTDTextView.text = tdText
                } else {
                    layoutStatsTD.visibility = View.GONE
                }

                // TP Stats - show justified in green
                if (stats.tpAbsent > 0) {
                    layoutStatsTP.visibility = View.VISIBLE
                    val tpText = if (stats.tpExcused > 0) {
                        val text = "${stats.tpAbsent}:${stats.tpExcused} (${"%.0f".format(tpPct)}%)"
                        val spannable = android.text.SpannableString(text)
                        val colonIndex = text.indexOf(':')
                        val parenIndex = text.indexOf('(')
                        if (colonIndex >= 0 && parenIndex > colonIndex) {
                            spannable.setSpan(
                                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#4CAF50")),
                                colonIndex + 1,
                                parenIndex - 1,
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        spannable
                    } else {
                        "${stats.tpAbsent} (${"%.0f".format(tpPct)}%)"
                    }
                    statsTPTextView.text = tpText
                } else {
                    layoutStatsTP.visibility = View.GONE
                }
                
                if (stats.behaviorPositive != 0) {
                    layoutBehaviorPos.visibility = View.VISIBLE
                    behaviorPosTextView.text = stats.behaviorPositive.toString()
                    layoutBehaviorPos.setOnClickListener { onBehaviorClick(student, "POSITIVE") }
                } else {
                    layoutBehaviorPos.visibility = View.GONE
                }

                if (stats.behaviorNegative != 0) {
                    layoutBehaviorNeg.visibility = View.VISIBLE
                    behaviorNegTextView.text = kotlin.math.abs(stats.behaviorNegative).toString()
                    layoutBehaviorNeg.setOnClickListener { onBehaviorClick(student, "NEGATIVE") }
                } else {
                    layoutBehaviorNeg.visibility = View.GONE
                }
            } else {
                layoutStatsCours.visibility = View.GONE
                layoutStatsTD.visibility = View.GONE
                layoutStatsTP.visibility = View.GONE
                layoutBehaviorPos.visibility = View.GONE
                layoutBehaviorNeg.visibility = View.GONE
            }

            // Hide empty rows to reduce visual clutter
            val row1HasContent = layoutStatsTD.visibility == View.VISIBLE || layoutStatsTP.visibility == View.VISIBLE
            layoutRow1.visibility = if (row1HasContent) View.VISIBLE else View.GONE
            
            val row2HasContent = layoutStatsCours.visibility == View.VISIBLE || 
                                 layoutBehaviorPos.visibility == View.VISIBLE || 
                                 layoutBehaviorNeg.visibility == View.VISIBLE
            layoutRow2.visibility = if (row2HasContent) View.VISIBLE else View.GONE
            
            // Modern bottom sheet menu on more options button
            btnMoreOptions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            btnMoreOptions.setOnClickListener { _ ->
                val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(itemView.context)
                val menuView = android.view.LayoutInflater.from(itemView.context)
                    .inflate(com.example.additioapp.R.layout.bottom_sheet_student_menu, null)
                
                // Set student name in header
                menuView.findViewById<android.widget.TextView>(com.example.additioapp.R.id.textMenuTitle).text = student.name
                
                // Set click listeners
                menuView.findViewById<View>(com.example.additioapp.R.id.menuEdit).setOnClickListener {
                    bottomSheet.dismiss()
                    onEditClick(student)
                }
                menuView.findViewById<View>(com.example.additioapp.R.id.menuGrades).setOnClickListener {
                    bottomSheet.dismiss()
                    onGradesClick(student)
                }
                menuView.findViewById<View>(com.example.additioapp.R.id.menuReport).setOnClickListener {
                    bottomSheet.dismiss()
                    onReportClick(student)
                }
                menuView.findViewById<View>(com.example.additioapp.R.id.menuNotes).setOnClickListener {
                    bottomSheet.dismiss()
                    onNotesClick(student)
                }
                menuView.findViewById<View>(com.example.additioapp.R.id.menuDelete).setOnClickListener {
                    bottomSheet.dismiss()
                    onDeleteClick(student)
                }
                
                bottomSheet.setContentView(menuView)
                bottomSheet.show()
            }
        }
    }
}

// DiffUtil Callback for efficient list updates
class StudentDiffCallback(
    private val oldList: List<StudentEntity>,
    private val newList: List<StudentEntity>
) : DiffUtil.Callback() {
    
    override fun getOldListSize(): Int = oldList.size
    
    override fun getNewListSize(): Int = newList.size
    
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
    
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }
}
