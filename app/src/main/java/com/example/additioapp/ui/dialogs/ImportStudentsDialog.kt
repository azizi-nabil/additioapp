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
    private val onImport: (List<StudentEntity>) -> Unit
) : DialogFragment() {

    private lateinit var textStatus: TextView
    private lateinit var btnSelectFile: Button
    private lateinit var btnImport: Button
    private lateinit var layoutInstructions: View
    private lateinit var layoutResult: View
    private var parsedStudents: List<StudentEntity> = emptyList()

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
            if (parsedStudents.isNotEmpty()) {
                onImport(parsedStudents)
                Toast.makeText(context, "Imported ${parsedStudents.size} students", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun showFormatDialog() {
        val message = """
            📋 CSV Format Instructions
            
            Your CSV file should have 3 columns:
            
            Column 1: Matricule (Student ID)
            Column 2: Nom (Last Name)
            Column 3: Prénom (First Name)
            
            ─────────────────────────
            
            📝 Example (French only):
            Matricule,Nom,Prénom
            202034052475,ABDERREZAK,MAISSOUN
            191934055534,ABIDI,YASSAMINE
            
            ─────────────────────────
            
            📝 Example (French + Arabic):
            Matricule,Nom,Prénom
            202034052475,ABDERREZAK/عبد الرزاق,MAISSOUN/ميسون
            191934055534,ABIDI/عبيدي,YASSAMINE/ياسمين
            
            ─────────────────────────
            
            💡 Tips:
            • Save Excel as CSV (File → Save As → CSV)
            • Use comma (,) or semicolon (;) as separator
            • First row can be headers (will be skipped)
            • Arabic names are optional (use / to separate)
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📄 CSV Format Guide")
            .setMessage(message)
            .setPositiveButton("Got it!", null)
            .show()
    }

    private fun parseCSV(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val students = mutableListOf<StudentEntity>()
            var skippedHeader = false

            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine

                // Skip header row
                if (!skippedHeader && (line.contains("Matricule", ignoreCase = true) || 
                    line.contains("Nom", ignoreCase = true))) {
                    skippedHeader = true
                    return@forEachLine
                }

                val parts = line.split(",", ";", "\t").map { it.trim() }
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

            // Show result section, hide instructions
            layoutInstructions.visibility = View.GONE
            layoutResult.visibility = View.VISIBLE

            if (students.isNotEmpty()) {
                textStatus.text = "✅ Found ${students.size} students\n\nPreview:\n" +
                    students.take(5).mapIndexed { i, s -> "${i+1}. ${s.displayNameFr} (${s.matricule})" }.joinToString("\n")
                btnImport.isEnabled = true
            } else {
                textStatus.text = "⚠️ No students found.\n\nCheck that your CSV has:\n• Matricule, Nom, Prénom columns\n• Data rows after the header"
                btnImport.isEnabled = false
            }
        } catch (e: Exception) {
            layoutInstructions.visibility = View.GONE
            layoutResult.visibility = View.VISIBLE
            textStatus.text = "❌ Error reading file:\n${e.message}"
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
