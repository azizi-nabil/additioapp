package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.additioapp.R

import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.FileWriter

class SettingsFragment : Fragment() {

    private lateinit var viewModel: com.example.additioapp.ui.viewmodel.SettingsViewModel

    private val backupLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewModel.backupData(it, requireContext().contentResolver)
        }
    }

    private val restoreLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.restoreData(it, requireContext().contentResolver)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = (requireActivity().application as com.example.additioapp.AdditioApplication).repository
        val factory = com.example.additioapp.ui.AdditioViewModelFactory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[com.example.additioapp.ui.viewmodel.SettingsViewModel::class.java]

        val btnBackup = view.findViewById<android.widget.LinearLayout>(R.id.btnBackupData)
        btnBackup.setOnClickListener {
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            backupLauncher.launch("teacherHub_backup_$timestamp.json")
        }

        val btnRestore = view.findViewById<android.widget.LinearLayout>(R.id.btnRestoreData)
        btnRestore.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Restore Data")
                .setMessage("Importing data will overwrite existing records. Are you sure you want to proceed?")
                .setPositiveButton("Yes") { _, _ ->
                    restoreLauncher.launch(arrayOf("application/json"))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Customization
        val btnEditPositive = view.findViewById<android.widget.LinearLayout>(R.id.btnEditPositiveBehaviors)
        btnEditPositive.setOnClickListener {
            showEditListDialog("Edit Positive Behaviors", "pref_positive_behaviors", 
                listOf("Active Participation", "Helping Others", "Homework Completed", "Respectful Behavior", "Prepared for Class", "Leadership / Takes Initiative", "Other"))
        }

        val btnEditNegative = view.findViewById<android.widget.LinearLayout>(R.id.btnEditNegativeBehaviors)
        btnEditNegative.setOnClickListener {
            showEditListDialog("Edit Negative Behaviors", "pref_negative_behaviors", 
                listOf("Disturbance / Disrupting Class", "No Homework", "Late Arrival", "Disrespect (teacher/peers)", "Off-Task", "Using Phone", "Other"))
        }

        val btnEditCategories = view.findViewById<android.widget.LinearLayout>(R.id.btnEditGradeCategories)
        btnEditCategories.setOnClickListener {
            showEditListDialog("Edit Grade Categories", "pref_grade_categories", 
                listOf("Exam", "CC", "Test", "Homework", "Project", "Other"))
        }

        viewModel.backupStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(requireContext(), "Backup Failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.restoreStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // Ideally, restart app or refresh data. For now, a toast is fine.
            }.onFailure {
                Toast.makeText(requireContext(), "Restore Failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditListDialog(title: String, prefKey: String, defaultList: List<String>) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentSet = prefs.getStringSet(prefKey, defaultList.toSet()) ?: defaultList.toSet()
        val currentList = currentSet.joinToString("\n")

        val input = android.widget.EditText(requireContext())
        input.setText(currentList)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.minLines = 5
        input.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        
        // Add padding
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString()
                val newList = newText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                prefs.edit().putStringSet(prefKey, newList).apply()
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
