package com.example.additioapp.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.example.additioapp.R
import com.example.additioapp.data.model.StudentEntity
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportStudentsDialog(
    private val classId: Long,
    private val existingStudents: List<StudentEntity> = emptyList(),
    private val onImport: (List<StudentEntity>) -> Unit,
    private val onUpdate: ((List<StudentEntity>) -> Unit)? = null
) : DialogFragment() {

    private lateinit var textStatus: TextView
    private lateinit var btnSelectFile: Button
    private lateinit var btnImport: Button
    private lateinit var btnManualMatch: Button
    private lateinit var layoutInstructions: View
    private lateinit var layoutResult: View
    private var parsedStudents: List<StudentEntity> = emptyList()
    private var studentsToUpdate: List<StudentEntity> = emptyList()
    private var studentsToInsert: List<StudentEntity> = emptyList()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                parseCSV(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_import_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textStatus = view.findViewById(R.id.textStatus)
        btnSelectFile = view.findViewById(R.id.btnSelectFile)
        btnImport = view.findViewById(R.id.btnImport)
        layoutInstructions = view.findViewById(R.id.layoutInstructions)
        layoutResult = view.findViewById(R.id.layoutResult)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnShowFormat = view.findViewById<Button>(R.id.btnShowFormat)
        btnManualMatch = view.findViewById(R.id.btnManualMatch)

        // Initially show instructions, hide result
        layoutInstructions.visibility = View.VISIBLE
        layoutResult.visibility = View.GONE

        btnShowFormat.setOnClickListener {
            showFormatDialog()
        }

        btnSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*"))
            }
            filePickerLauncher.launch(intent)
        }

        btnImport.setOnClickListener {
            if (studentsToInsert.isNotEmpty() || studentsToUpdate.isNotEmpty()) {
                // Update existing students
                if (studentsToUpdate.isNotEmpty() && onUpdate != null) {
                    onUpdate.invoke(studentsToUpdate)
                }
                // Insert new students
                if (studentsToInsert.isNotEmpty()) {
                    onImport(studentsToInsert)
                }
                val total = studentsToInsert.size + studentsToUpdate.size
                Toast.makeText(context, getString(R.string.import_success_toast, total), Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        // Manual matching button
        btnManualMatch.setOnClickListener {
            if (parsedStudents.isNotEmpty() && existingStudents.isNotEmpty()) {
                val matchDialog = MatchStudentsDialog(
                    importedStudents = parsedStudents,
                    existingStudents = existingStudents,
                    onComplete = { toUpdate, toInsert ->
                        // Update existing students
                        toUpdate.forEach { (_, updated) ->
                            onUpdate?.invoke(listOf(updated))
                        }
                        // Insert new students
                        if (toInsert.isNotEmpty()) {
                            onImport(toInsert)
                        }
                    }
                )
                matchDialog.show(parentFragmentManager, "MatchStudentsDialog")
                dismiss()
            }
        }
    }

    private fun showFormatDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.import_guide_title))
            .setMessage(getString(R.string.import_instructions))
            .setPositiveButton(getString(R.string.import_guide_btn), null)
            .show()
    }

    private fun parseCSV(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val students = mutableListOf<StudentEntity>()
            var headerFound = false

            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine

                // Skip metadata rows (class info, etc.) - they don't start with a numeric matricule
                val parts = line.split(",", ";", "\t").map { it.trim() }
                val firstPart = (parts.getOrNull(0) ?: "").uppercase()
                
                // Detection logic: skip header or empty rows
                // Headers usually contain words like "Matricule", "Mle", "ID", "Nom", "Name"
                val isHeader = line.contains("Matricule", ignoreCase = true) || 
                              line.contains("Mle", ignoreCase = true) || 
                              line.contains("Nom", ignoreCase = true) ||
                              line.contains("ID", ignoreCase = true)
                
                if (isHeader) {
                    headerFound = true
                    return@forEachLine
                }

                // If first part is empty or too short (noise), skip
                if (firstPart.isEmpty() || firstPart.length < 2) {
                    return@forEachLine
                }

                if (parts.size >= 3) {
                    val matricule = parts[0]
                    val nomFull = parts[1]
                    val prenomFull = parts[2]

                    // Parse French/Arabic names (separated by /)
                    val nomParts = nomFull.split("/").map { it.trim() }
                    val prenomParts = prenomFull.split("/").map { it.trim() }

                    // Convert to proper case (UPPERCASE -> Title Case) for French names
                    fun String.toProperCase(): String {
                        return this.lowercase().split(" ", "-").joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }.replace(" - ", "-")
                    }

                    val lastNameFr = nomParts.getOrNull(0)?.toProperCase() ?: ""
                    val lastNameAr = nomParts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    val firstNameFr = prenomParts.getOrNull(0)?.toProperCase() ?: ""
                    val firstNameAr = prenomParts.getOrNull(1)?.takeIf { it.isNotEmpty() }

                    val student = StudentEntity(
                        classId = classId,
                        matricule = matricule,
                        firstNameFr = firstNameFr,
                        lastNameFr = lastNameFr,
                        firstNameAr = firstNameAr,
                        lastNameAr = lastNameAr,
                        name = "$lastNameFr $firstNameFr".trim(),
                        studentId = matricule
                    )
                    students.add(student)
                }
            }

            reader.close()
            parsedStudents = students
            
            // Match with existing students by Arabic name
            studentsToUpdate = mutableListOf()
            studentsToInsert = mutableListOf()
            
            students.forEach { csvStudent ->
                // Try to find a matching existing student by Arabic name
                val arabicFullName = "${csvStudent.lastNameAr ?: ""} ${csvStudent.firstNameAr ?: ""}".trim()
                val matchedStudent = if (arabicFullName.isNotEmpty()) {
                    existingStudents.find { existing ->
                        // Check 1: Match against existing Arabic name fields
                        val existingArabicName = "${existing.lastNameAr ?: ""} ${existing.firstNameAr ?: ""}".trim()
                        val matchByArabicFields = existingArabicName.isNotEmpty() && existingArabicName == arabicFullName
                        
                        // Check 2: Match against lastNameFr (where user stores full Arabic name)
                        val matchByLastNameFr = existing.lastNameFr.isNotEmpty() && 
                            (existing.lastNameFr == arabicFullName || existing.lastNameFr.contains(csvStudent.lastNameAr ?: "###"))
                        
                        matchByArabicFields || matchByLastNameFr
                    }
                } else null
                
                if (matchedStudent != null) {
                    // Update existing student with new info, preserving ID
                    val updatedStudent = matchedStudent.copy(
                        matricule = csvStudent.matricule.ifEmpty { matchedStudent.matricule },
                        firstNameFr = csvStudent.firstNameFr.ifEmpty { matchedStudent.firstNameFr },
                        lastNameFr = csvStudent.lastNameFr.ifEmpty { matchedStudent.lastNameFr },
                        firstNameAr = csvStudent.firstNameAr ?: matchedStudent.firstNameAr,
                        lastNameAr = csvStudent.lastNameAr ?: matchedStudent.lastNameAr,
                        name = "${csvStudent.lastNameFr.ifEmpty { matchedStudent.lastNameFr }} ${csvStudent.firstNameFr.ifEmpty { matchedStudent.firstNameFr }}".trim(),
                        studentId = csvStudent.matricule.ifEmpty { matchedStudent.studentId }
                    )
                    (studentsToUpdate as MutableList).add(updatedStudent)
                } else {
                    (studentsToInsert as MutableList).add(csvStudent)
                }
            }

            // Show result section, hide instructions
            layoutInstructions.visibility = View.GONE
            layoutResult.visibility = View.VISIBLE

            if (students.isNotEmpty()) {
                val updateCount = studentsToUpdate.size
                val insertCount = studentsToInsert.size
                val previewList = students.take(5).mapIndexed { i, s -> "${i+1}. ${s.displayNameFr} (${s.matricule})" }.joinToString("\n")
                
                val statusText = StringBuilder()
                statusText.append(getString(R.string.import_status_found, students.size))
                statusText.append(previewList)
                if (updateCount > 0) {
                    statusText.append("\n\n✅ Will update $updateCount existing student(s)")
                }
                if (insertCount > 0) {
                    statusText.append("\n➕ Will add $insertCount new student(s)")
                }
                
                textStatus.text = statusText.toString()
                btnImport.isEnabled = true
                // Enable manual match if there are existing students
                btnManualMatch.isEnabled = existingStudents.isNotEmpty()
            } else {
                textStatus.text = getString(R.string.import_status_empty)
                btnImport.isEnabled = false
            }
        } catch (e: Exception) {
            layoutInstructions.visibility = View.GONE
            layoutResult.visibility = View.VISIBLE
            textStatus.text = getString(R.string.import_status_error, e.message)
            btnImport.isEnabled = false
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
