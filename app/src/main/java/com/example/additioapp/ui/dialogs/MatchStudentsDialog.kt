package com.example.additioapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity
import com.google.android.material.button.MaterialButton

class MatchStudentsDialog(
    private val importedStudents: List<StudentEntity>,
    private val existingStudents: List<StudentEntity>,
    private val onComplete: (toUpdate: List<Pair<StudentEntity, StudentEntity>>, toInsert: List<StudentEntity>) -> Unit
) : DialogFragment() {

    private var currentIndex = 0
    private val matchResults = mutableMapOf<Int, StudentEntity?>() // index -> matched existing student (null = add as new)
    private var selectedExistingStudent: StudentEntity? = null
    private val usedExistingStudents = mutableSetOf<Long>() // Track already matched students

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_match_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textProgress = view.findViewById<TextView>(R.id.textProgress)
        val progressIndicator = view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressIndicator)
        val textImportedName = view.findViewById<TextView>(R.id.textImportedName)
        val textImportedDetails = view.findViewById<TextView>(R.id.textImportedDetails)
        val recyclerExisting = view.findViewById<RecyclerView>(R.id.recyclerExistingStudents)
        val btnAddAsNew = view.findViewById<MaterialButton>(R.id.btnAddAsNew)
        val btnSkip = view.findViewById<MaterialButton>(R.id.btnSkip)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)
        val btnFinish = view.findViewById<MaterialButton>(R.id.btnFinish)

        recyclerExisting.layoutManager = LinearLayoutManager(requireContext())

        fun updateUI() {
            if (currentIndex >= importedStudents.size) {
                // All done, show finish
                btnFinish.visibility = View.VISIBLE
                btnNext.visibility = View.GONE
                btnSkip.text = "Back"
                textProgress.text = "All students processed!"
                textImportedName.text = "Ready to import"
                textImportedDetails.text = "Tap Finish to complete"
                recyclerExisting.visibility = View.GONE
                btnAddAsNew.visibility = View.GONE
                return
            }

            val imported = importedStudents[currentIndex]
            textProgress.text = "${currentIndex + 1} / ${importedStudents.size}"
            
            // Update progress bar
            progressIndicator.max = importedStudents.size
            progressIndicator.progress = currentIndex + 1
            
            // Show imported student info
            val frName = "${imported.lastNameFr} ${imported.firstNameFr}".trim()
            val arName = "${imported.lastNameAr ?: ""} ${imported.firstNameAr ?: ""}".trim()
            textImportedName.text = frName.ifEmpty { arName }
            textImportedDetails.text = "ID: ${imported.matricule}" + (if (arName.isNotEmpty()) " | $arName" else "")

            // Filter out already used existing students
            val availableStudents = existingStudents.filter { it.id !in usedExistingStudents }
            
            // Update adapter
            recyclerExisting.adapter = MatchStudentAdapter(availableStudents) { selected ->
                selectedExistingStudent = selected
                btnNext.isEnabled = true // Enable Next when selection is made
            }
            recyclerExisting.visibility = View.VISIBLE
            btnAddAsNew.visibility = View.VISIBLE
            btnFinish.visibility = View.GONE
            btnNext.visibility = View.VISIBLE
            btnNext.isEnabled = false // Disabled until selection is made
            btnSkip.text = if (currentIndex > 0) "Back" else "Skip"
            selectedExistingStudent = null
        }

        fun goNext() {
            // Save current selection
            if (selectedExistingStudent != null) {
                matchResults[currentIndex] = selectedExistingStudent
                usedExistingStudents.add(selectedExistingStudent!!.id)
            } else {
                matchResults[currentIndex] = null // Add as new
            }
            currentIndex++
            updateUI()
        }

        btnAddAsNew.setOnClickListener {
            selectedExistingStudent = null
            goNext()
        }

        btnSkip.setOnClickListener {
            if (currentIndex > 0 && btnSkip.text == "Back") {
                // Go back
                val previousMatch = matchResults[currentIndex - 1]
                if (previousMatch != null) {
                    usedExistingStudents.remove(previousMatch.id)
                }
                matchResults.remove(currentIndex - 1)
                currentIndex--
                updateUI()
            } else {
                // Skip without matching
                currentIndex++
                updateUI()
            }
        }

        // Next button - confirms selection and advances
        btnNext.setOnClickListener {
            if (selectedExistingStudent != null) {
                goNext()
            }
        }

        btnFinish.setOnClickListener {
            // Process all matches
            val toUpdate = mutableListOf<Pair<StudentEntity, StudentEntity>>()
            val toInsert = mutableListOf<StudentEntity>()

            matchResults.forEach { (index, existingStudent) ->
                val imported = importedStudents[index]
                if (existingStudent != null) {
                    // Update existing student with imported data
                    val updated = existingStudent.copy(
                        matricule = imported.matricule.ifEmpty { existingStudent.matricule },
                        firstNameFr = imported.firstNameFr.ifEmpty { existingStudent.firstNameFr },
                        lastNameFr = imported.lastNameFr.ifEmpty { existingStudent.lastNameFr },
                        firstNameAr = imported.firstNameAr ?: existingStudent.firstNameAr,
                        lastNameAr = imported.lastNameAr ?: existingStudent.lastNameAr,
                        name = "${imported.lastNameFr.ifEmpty { existingStudent.lastNameFr }} ${imported.firstNameFr.ifEmpty { existingStudent.firstNameFr }}".trim(),
                        studentId = imported.matricule.ifEmpty { existingStudent.studentId }
                    )
                    toUpdate.add(Pair(existingStudent, updated))
                } else {
                    toInsert.add(imported)
                }
            }

            onComplete(toUpdate, toInsert)
            Toast.makeText(context, "Updated ${toUpdate.size}, Added ${toInsert.size} students", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        // Start with first student
        updateUI()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Inner adapter for existing students list
    inner class MatchStudentAdapter(
        private val students: List<StudentEntity>,
        private val onSelect: (StudentEntity) -> Unit
    ) : RecyclerView.Adapter<MatchStudentAdapter.ViewHolder>() {

        private var selectedPosition = -1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val radioSelect: android.widget.RadioButton = view.findViewById(R.id.radioSelect)
            val textName: TextView = view.findViewById(R.id.textStudentName)
            val textDetails: TextView = view.findViewById(R.id.textStudentDetails)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match_student, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = students[position]
            
            // Display existing student info
            val displayName = student.displayNameFr.ifEmpty { student.name }
            holder.textName.text = displayName
            holder.textDetails.text = "ID: ${student.displayMatricule}"

            holder.radioSelect.isChecked = position == selectedPosition

            holder.itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                if (oldPosition >= 0) notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onSelect(student)
            }

            holder.radioSelect.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                if (oldPosition >= 0) notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onSelect(student)
            }
        }

        override fun getItemCount() = students.size
    }
}
