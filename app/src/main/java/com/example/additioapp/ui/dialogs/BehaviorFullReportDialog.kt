package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.BehaviorViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class BehaviorFullReportDialog : BottomSheetDialogFragment() {

    private val behaviorViewModel: BehaviorViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var studentId: Long = -1
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            studentId = it.getLong("studentId")
            studentName = it.getString("studentName", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_behavior_full_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textName = view.findViewById<TextView>(R.id.textStudentName)
        val textSummary = view.findViewById<TextView>(R.id.textBehaviorSummary)
        val layoutPositive = view.findViewById<LinearLayout>(R.id.layoutPositiveList)
        val layoutNegative = view.findViewById<LinearLayout>(R.id.layoutNegativeList)
        val textNoPositive = view.findViewById<TextView>(R.id.textNoPositive)
        val textNoNegative = view.findViewById<TextView>(R.id.textNoNegative)
        val layoutNotes = view.findViewById<LinearLayout>(R.id.layoutNotesList)
        val textNoNotes = view.findViewById<TextView>(R.id.textNoNotes)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        textName.text = studentName

        // Load behaviors
        behaviorViewModel.getBehaviorForStudent(studentId).observe(viewLifecycleOwner) { behaviors ->
            val positives = behaviors.filter { it.type == "POSITIVE" }
            val negatives = behaviors.filter { it.type == "NEGATIVE" }
            
            val positivePoints = positives.sumOf { it.points }
            val negativePoints = negatives.sumOf { it.points }
            val netScore = positivePoints - negativePoints
            
            textSummary.text = getString(R.string.behavior_summary_format, 
                positives.size, positivePoints, 
                negatives.size, negativePoints, 
                netScore)
            
            val fmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
            
            // Positive behaviors
            layoutPositive.removeAllViews()
            if (positives.isNotEmpty()) {
                textNoPositive.visibility = View.GONE
                positives.forEach { record ->
                    val itemView = layoutInflater.inflate(R.layout.item_behavior_report, layoutPositive, false)
                    
                    val imgIcon = itemView.findViewById<android.widget.ImageView>(R.id.imgBehaviorIcon)
                    val textDate = itemView.findViewById<TextView>(R.id.textBehaviorDate)
                    val textPoints = itemView.findViewById<TextView>(R.id.textBehaviorPoints)
                    val textCategory = itemView.findViewById<TextView>(R.id.textBehaviorCategory)
                    val textComment = itemView.findViewById<TextView>(R.id.textBehaviorComment)

                    textDate.text = fmt.format(record.date)
                    textCategory.visibility = if (record.category == "General") View.GONE else View.VISIBLE
                    textCategory.text = record.category
                    textComment.visibility = if (record.comment.isNullOrEmpty()) View.GONE else View.VISIBLE
                    textComment.text = record.comment
                    
                    textPoints.text = "+${record.points}"
                    textPoints.setTextColor(android.graphics.Color.parseColor("#388E3C"))
                    imgIcon.setImageResource(R.drawable.ic_thumb_up_24dp)
                    imgIcon.setColorFilter(android.graphics.Color.parseColor("#388E3C"))

                    layoutPositive.addView(itemView)
                }
            } else {
                textNoPositive.visibility = View.VISIBLE
            }
            
            // Negative behaviors
            layoutNegative.removeAllViews()
            if (negatives.isNotEmpty()) {
                textNoNegative.visibility = View.GONE
                negatives.forEach { record ->
                    val itemView = layoutInflater.inflate(R.layout.item_behavior_report, layoutNegative, false)
                    
                    val imgIcon = itemView.findViewById<android.widget.ImageView>(R.id.imgBehaviorIcon)
                    val textDate = itemView.findViewById<TextView>(R.id.textBehaviorDate)
                    val textPoints = itemView.findViewById<TextView>(R.id.textBehaviorPoints)
                    val textCategory = itemView.findViewById<TextView>(R.id.textBehaviorCategory)
                    val textComment = itemView.findViewById<TextView>(R.id.textBehaviorComment)

                    textDate.text = fmt.format(record.date)
                    textCategory.visibility = if (record.category == "General") View.GONE else View.VISIBLE
                    textCategory.text = record.category
                    textComment.visibility = if (record.comment.isNullOrEmpty()) View.GONE else View.VISIBLE
                    textComment.text = record.comment
                    
                    textPoints.text = "-${record.points}"
                    textPoints.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                    imgIcon.setImageResource(R.drawable.ic_thumb_down_24dp)
                    imgIcon.setColorFilter(android.graphics.Color.parseColor("#D32F2F"))

                    layoutNegative.addView(itemView)
                }
            } else {
                textNoNegative.visibility = View.VISIBLE
            }
        }
        
        // Load notes
        val repository = (requireActivity().application as AdditioApplication).repository
        repository.getNotesForStudent(studentId).observe(viewLifecycleOwner) { notes ->
            layoutNotes.removeAllViews()
            if (notes.isNotEmpty()) {
                textNoNotes.visibility = View.GONE
                val fmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                notes.forEach { note ->
                    val itemView = TextView(requireContext()).apply {
                        text = "• ${fmt.format(note.date)}: ${note.content}"
                        textSize = 14f
                        setTextColor(context.getColor(R.color.on_surface))
                        setPadding(0, 8, 0, 8)
                    }
                    layoutNotes.addView(itemView)
                }
            } else {
                textNoNotes.visibility = View.VISIBLE
            }
        }

        btnClose.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance(studentId: Long, studentName: String) = BehaviorFullReportDialog().apply {
            arguments = Bundle().apply {
                putLong("studentId", studentId)
                putString("studentName", studentName)
            }
        }
    }
}
