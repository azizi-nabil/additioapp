package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.BehaviorHistoryAdapter
import com.example.additioapp.ui.viewmodel.BehaviorViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class StudentBehaviorBottomSheet : BottomSheetDialogFragment() {

    private var studentId: Long = -1
    private var classId: Long = -1
    private var studentName: String = ""

    private val behaviorViewModel: BehaviorViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            studentId = it.getLong(ARG_STUDENT_ID)
            classId = it.getLong(ARG_CLASS_ID)
            studentName = it.getString(ARG_STUDENT_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_student_behavior, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textStudentName = view.findViewById<TextView>(R.id.textSheetStudentName)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerBehaviorHistory)
        val fabAdd = view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddBehavior)
        val textEmpty = view.findViewById<TextView>(R.id.textEmptyBehavior)

        textStudentName.text = studentName

        val adapter = BehaviorHistoryAdapter { record ->
            behaviorViewModel.deleteBehavior(record)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        behaviorViewModel.getBehaviorForStudent(studentId).observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
            textEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (records.isNotEmpty()) View.VISIBLE else View.GONE
        }

        fabAdd.setOnClickListener {
            val dialog = AddBehaviorDialog(studentId, classId) { record ->
                behaviorViewModel.insertBehavior(record)
            }
            dialog.show(parentFragmentManager, "AddBehaviorDialog")
        }
    }

    companion object {
        private const val ARG_STUDENT_ID = "student_id"
        private const val ARG_CLASS_ID = "class_id"
        private const val ARG_STUDENT_NAME = "student_name"

        fun newInstance(studentId: Long, classId: Long, studentName: String) =
            StudentBehaviorBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_STUDENT_ID, studentId)
                    putLong(ARG_CLASS_ID, classId)
                    putString(ARG_STUDENT_NAME, studentName)
                }
            }
    }
}
