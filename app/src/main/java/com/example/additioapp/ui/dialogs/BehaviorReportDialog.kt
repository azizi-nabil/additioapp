package com.example.additioapp.ui.dialogs
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.BehaviorViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

class BehaviorReportDialog : BottomSheetDialogFragment() {

    private val behaviorViewModel: BehaviorViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private var studentId: Long = -1
    private var studentName: String = ""
    private var behaviorType: String = "" // "POSITIVE" or "NEGATIVE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            studentId = it.getLong("studentId")
            studentName = it.getString("studentName", "")
            behaviorType = it.getString("behaviorType", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_behavior_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textName = view.findViewById<TextView>(R.id.textStudentName)
        val textTitle = view.findViewById<TextView>(R.id.textReportTitle)
        val layoutList = view.findViewById<LinearLayout>(R.id.layoutBehaviorList)
        val textNoBehaviors = view.findViewById<TextView>(R.id.textNoBehaviors)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        textName.text = studentName
        textTitle.text = if (behaviorType == "POSITIVE") getString(R.string.behavior_positive_title) else getString(R.string.behavior_negative_title)
        
        val titleColor = if (behaviorType == "POSITIVE") 
            android.graphics.Color.parseColor("#388E3C") 
        else 
            android.graphics.Color.parseColor("#D32F2F")
        textTitle.setTextColor(titleColor)

        behaviorViewModel.getBehaviorForStudent(studentId).observe(viewLifecycleOwner) { behaviors ->
            val filteredBehaviors = behaviors.filter { it.type == behaviorType }
            
            layoutList.removeAllViews()
            layoutList.addView(textNoBehaviors)

            if (filteredBehaviors.isNotEmpty()) {
                textNoBehaviors.visibility = View.GONE
                val fmt = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                
                filteredBehaviors.forEach { record ->
                    val itemView = layoutInflater.inflate(R.layout.item_behavior_report, layoutList, false)
                    
                    val imgIcon = itemView.findViewById<android.widget.ImageView>(R.id.imgBehaviorIcon)
                    val textDate = itemView.findViewById<TextView>(R.id.textBehaviorDate)
                    val textPoints = itemView.findViewById<TextView>(R.id.textBehaviorPoints)
                    val textCategory = itemView.findViewById<TextView>(R.id.textBehaviorCategory)
                    val textComment = itemView.findViewById<TextView>(R.id.textBehaviorComment)

                    textDate.text = fmt.format(record.date)
                    
                    if (record.category == "General") {
                        textCategory.visibility = View.GONE
                    } else {
                        textCategory.visibility = View.VISIBLE
                        textCategory.text = record.category
                    }
                    
                    if (record.comment.isNullOrEmpty()) {
                        textComment.visibility = View.GONE
                    } else {
                        textComment.visibility = View.VISIBLE
                        textComment.text = record.comment
                    }

                    if (record.type == "POSITIVE") {
                        textPoints.text = "+${record.points}"
                        textPoints.setTextColor(android.graphics.Color.parseColor("#388E3C")) // Green
                        imgIcon.setImageResource(R.drawable.ic_thumb_up_24dp)
                        imgIcon.setColorFilter(android.graphics.Color.parseColor("#388E3C"))
                    } else {
                        textPoints.text = "-${record.points}"
                        textPoints.setTextColor(android.graphics.Color.parseColor("#D32F2F")) // Red
                        imgIcon.setImageResource(R.drawable.ic_thumb_down_24dp)
                        imgIcon.setColorFilter(android.graphics.Color.parseColor("#D32F2F"))
                    }

                    layoutList.addView(itemView)
                }
            } else {
                textNoBehaviors.visibility = View.VISIBLE
            }
        }

        btnClose.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance(studentId: Long, studentName: String, behaviorType: String) = BehaviorReportDialog().apply {
            arguments = Bundle().apply {
                putLong("studentId", studentId)
                putString("studentName", studentName)
                putString("behaviorType", behaviorType)
            }
        }
    }
}
