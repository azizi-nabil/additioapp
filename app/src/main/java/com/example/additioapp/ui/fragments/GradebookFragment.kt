package com.example.additioapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.ClassViewModel
import com.example.additioapp.ui.viewmodel.GradeViewModel
import com.example.additioapp.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class GradebookFragment : Fragment() {

    private val classViewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val studentViewModel: StudentViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }
    private val gradeViewModel: GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    private val repository by lazy {
        (requireActivity().application as AdditioApplication).repository
    }

    private var classes: List<ClassEntity> = emptyList()
    private var selectedClass: ClassEntity? = null
    private var students: List<StudentEntity> = emptyList()
    private var gradeItems: List<GradeItemEntity> = emptyList()
    private var gradeRecords: List<GradeRecordEntity> = emptyList()

    private val formatter = DecimalFormat("#.#")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gradebook, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerClass = view.findViewById<Spinner>(R.id.spinnerGradebookClass)
        val textClassAverage = view.findViewById<TextView>(R.id.textClassAverage)
        val textGradeItemsCount = view.findViewById<TextView>(R.id.textGradeItemsCount)
        val textStudentsGraded = view.findViewById<TextView>(R.id.textStudentsGraded)
        val containerDistribution = view.findViewById<LinearLayout>(R.id.containerGradeDistribution)
        val containerStudentGrades = view.findViewById<LinearLayout>(R.id.containerStudentGrades)
        val containerAssessments = view.findViewById<LinearLayout>(R.id.containerAssessments)
        val btnAddGradeItem = view.findViewById<Button>(R.id.btnAddGradeItem)

        btnAddGradeItem.setOnClickListener {
            selectedClass?.let {
                // Navigate to class detail for adding grade item
                findNavController().navigate(R.id.classesFragment)
            } ?: Toast.makeText(requireContext(), "Select a class first", Toast.LENGTH_SHORT).show()
        }

        classViewModel.allClasses.observe(viewLifecycleOwner) { classList ->
            classes = classList
            val classNames = classList.map { it.name }
            spinnerClass.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classNames)
        }

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (classes.isNotEmpty()) {
                    selectedClass = classes[position]
                    loadClassData(selectedClass!!, textClassAverage, textGradeItemsCount, textStudentsGraded,
                        containerDistribution, containerStudentGrades, containerAssessments)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadClassData(
        classEntity: ClassEntity,
        textClassAverage: TextView,
        textGradeItemsCount: TextView,
        textStudentsGraded: TextView,
        containerDistribution: LinearLayout,
        containerStudentGrades: LinearLayout,
        containerAssessments: LinearLayout
    ) {
        studentViewModel.getStudentsForClass(classEntity.id).observe(viewLifecycleOwner) { s ->
            students = s
            textStudentsGraded.text = s.size.toString()
            updateUI(textClassAverage, containerDistribution, containerStudentGrades)
        }

        gradeViewModel.getGradeItemsForClass(classEntity.id).observe(viewLifecycleOwner) { items ->
            gradeItems = items
            textGradeItemsCount.text = items.size.toString()
            renderAssessments(containerAssessments, items)
            updateUI(textClassAverage, containerDistribution, containerStudentGrades)
        }

        gradeViewModel.getGradeRecordsForClass(classEntity.id).observe(viewLifecycleOwner) { records ->
            gradeRecords = records
            updateUI(textClassAverage, containerDistribution, containerStudentGrades)
        }
    }

    private fun updateUI(
        textClassAverage: TextView,
        containerDistribution: LinearLayout,
        containerStudentGrades: LinearLayout
    ) {
        if (students.isEmpty()) {
            textClassAverage.text = "--"
            containerDistribution.removeAllViews()
            addEmptyRow(containerDistribution, "No students in this class")
            containerStudentGrades.removeAllViews()
            addEmptyRow(containerStudentGrades, "Add students to see grades")
            return
        }

        // Calculate student averages
        val studentAverages = students.map { student ->
            val studentRecords = gradeRecords.filter { it.studentId == student.id }
            val avg = gradeViewModel.calculateWeightedAverage(gradeItems, studentRecords)
            Pair(student, avg)
        }

        // Class average
        val validAverages = studentAverages.filter { it.second >= 0 }
        val classAvg = if (validAverages.isNotEmpty()) {
            validAverages.map { it.second }.average().toFloat()
        } else -1f

        textClassAverage.text = if (classAvg >= 0) "${formatter.format(classAvg)}%" else "--"

        // Grade distribution
        renderGradeDistribution(containerDistribution, studentAverages)

        // Student grades
        renderStudentGrades(containerStudentGrades, studentAverages)
    }

    private fun renderGradeDistribution(container: LinearLayout, studentAverages: List<Pair<StudentEntity, Float>>) {
        container.removeAllViews()

        val grades = mapOf(
            "A (90-100%)" to Pair("#4CAF50", 90f..100f),
            "B (80-89%)" to Pair("#8BC34A", 80f..89.99f),
            "C (70-79%)" to Pair("#FFC107", 70f..79.99f),
            "D (60-69%)" to Pair("#FF9800", 60f..69.99f),
            "F (<60%)" to Pair("#F44336", 0f..59.99f)
        )

        val validAverages = studentAverages.filter { it.second >= 0 }
        val total = validAverages.size.coerceAtLeast(1)

        grades.forEach { (label, info) ->
            val count = validAverages.count { it.second in info.second }
            val pct = (count.toFloat() / total) * 100
            addDistributionRow(container, label, count, pct, info.first)
        }
    }

    private fun addDistributionRow(container: LinearLayout, label: String, count: Int, pct: Float, color: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        val colorView = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(12, 12).apply { marginEnd = 8 }
            setBackgroundColor(Color.parseColor(color))
        }

        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val countView = TextView(requireContext()).apply {
            text = "$count (${formatter.format(pct)}%)"
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
        }

        row.addView(colorView)
        row.addView(labelView)
        row.addView(countView)
        container.addView(row)
    }

    private fun renderStudentGrades(container: LinearLayout, studentAverages: List<Pair<StudentEntity, Float>>, showAll: Boolean = false) {
        container.removeAllViews()

        val sorted = studentAverages.sortedByDescending { it.second }

        if (sorted.isEmpty()) {
            addEmptyRow(container, "No students to display")
            return
        }

        val displayList = if (showAll) sorted else sorted.take(10)
        
        displayList.forEachIndexed { index, (student, avg) ->
            val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
            
            val gradeColor = when {
                avg >= 90 -> "#4CAF50"
                avg >= 80 -> "#8BC34A"
                avg >= 70 -> "#FFC107"
                avg >= 60 -> "#FF9800"
                avg >= 0 -> "#F44336"
                else -> "#9E9E9E"
            }

            row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(Color.parseColor(gradeColor))
            row.findViewById<TextView>(R.id.textRowTitle).text = "${index + 1}. ${student.name}"
            row.findViewById<TextView>(R.id.textRowMeta).text = if (avg >= 0) "${formatter.format(avg)}%" else "No grades"
            row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE

            container.addView(row)
        }

        if (!showAll && sorted.size > 10) {
            val moreRow = layoutInflater.inflate(R.layout.item_home_row, container, false)
            moreRow.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
            moreRow.findViewById<TextView>(R.id.textRowTitle).apply {
                text = "+${sorted.size - 10} more students"
                setTextColor(Color.parseColor("#2196F3"))
            }
            moreRow.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
            moreRow.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
            moreRow.setOnClickListener {
                renderStudentGrades(container, studentAverages, showAll = true)
            }
            container.addView(moreRow)
        } else if (showAll && sorted.size > 10) {
            // Add "Show less" option
            val lessRow = layoutInflater.inflate(R.layout.item_home_row, container, false)
            lessRow.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
            lessRow.findViewById<TextView>(R.id.textRowTitle).apply {
                text = "Show less"
                setTextColor(Color.parseColor("#2196F3"))
            }
            lessRow.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
            lessRow.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
            lessRow.setOnClickListener {
                renderStudentGrades(container, studentAverages, showAll = false)
            }
            container.addView(lessRow)
        }
    }

    private fun renderAssessments(container: LinearLayout, items: List<GradeItemEntity>) {
        container.removeAllViews()

        if (items.isEmpty()) {
            addEmptyRow(container, "No assessments yet")
            return
        }

        items.forEach { item ->
            val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
            row.findViewById<View>(R.id.colorIndicator).setBackgroundColor(Color.parseColor("#2196F3"))
            row.findViewById<TextView>(R.id.textRowTitle).text = item.name
            row.findViewById<TextView>(R.id.textRowMeta).text = "Max: ${item.maxScore} • Weight: ${formatter.format(item.weight)}%"
            row.findViewById<TextView>(R.id.textRowExtra).apply {
                text = item.category ?: "General"
                visibility = View.VISIBLE
            }
            container.addView(row)
        }
    }

    private fun addEmptyRow(container: LinearLayout, message: String) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowTitle).text = message
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        container.addView(row)
    }

    private fun addMoreRow(container: LinearLayout, count: Int, label: String) {
        val row = layoutInflater.inflate(R.layout.item_home_row, container, false)
        row.findViewById<View>(R.id.colorIndicator).visibility = View.INVISIBLE
        row.findViewById<TextView>(R.id.textRowTitle).apply {
            text = "+$count $label"
            setTextColor(Color.parseColor("#2196F3"))
        }
        row.findViewById<TextView>(R.id.textRowMeta).visibility = View.GONE
        row.findViewById<TextView>(R.id.textRowExtra).visibility = View.GONE
        container.addView(row)
    }
}
