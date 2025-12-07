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

        // Name Language Setting
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val btnNameLanguage = view.findViewById<android.widget.LinearLayout>(R.id.btnNameLanguage)
        val textNameLanguage = view.findViewById<android.widget.TextView>(R.id.textNameLanguage)
        
        // Load current setting
        val currentLang = prefs.getString("pref_name_language", "french") ?: "french"
        textNameLanguage.text = if (currentLang == "arabic") "العربية (Arabic)" else "French"

        btnNameLanguage.setOnClickListener {
            val options = arrayOf("French", "العربية (Arabic)")
            val currentIndex = if (prefs.getString("pref_name_language", "french") == "arabic") 1 else 0
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Student Name Display")
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val newLang = if (which == 1) "arabic" else "french"
                    prefs.edit().putString("pref_name_language", newLang).apply()
                    textNameLanguage.text = options[which]
                    Toast.makeText(requireContext(), "Name display updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Sort Order Setting
        val btnSortOrder = view.findViewById<android.widget.LinearLayout>(R.id.btnSortOrder)
        val textSortOrder = view.findViewById<android.widget.TextView>(R.id.textSortOrder)
        
        val sortOptions = arrayOf("By Last Name", "By First Name", "By ID/Matricule")
        val sortValues = arrayOf("lastname", "firstname", "id")
        
        // Load current setting
        val currentSort = prefs.getString("pref_sort_order", "lastname") ?: "lastname"
        val currentSortIndex = sortValues.indexOf(currentSort).coerceAtLeast(0)
        textSortOrder.text = sortOptions[currentSortIndex]

        btnSortOrder.setOnClickListener {
            val selectedIndex = sortValues.indexOf(prefs.getString("pref_sort_order", "lastname")).coerceAtLeast(0)
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Default Sort Order")
                .setSingleChoiceItems(sortOptions, selectedIndex) { dialog, which ->
                    prefs.edit().putString("pref_sort_order", sortValues[which]).apply()
                    textSortOrder.text = sortOptions[which]
                    Toast.makeText(requireContext(), "Sort order updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // List Size Setting
        val layoutListSize = view.findViewById<android.widget.LinearLayout>(R.id.layoutListSize)
        val textListSize = view.findViewById<android.widget.TextView>(R.id.textListSize)
        
        val sizeOptions = arrayOf("Compact", "Normal", "Comfortable")
        val sizeValues = arrayOf("compact", "normal", "comfortable")
        
        // Load current setting
        val currentSize = prefs.getString("pref_list_size", "normal") ?: "normal"
        val currentSizeIndex = sizeValues.indexOf(currentSize).coerceAtLeast(0)
        textListSize.text = sizeOptions[currentSizeIndex]

        layoutListSize.setOnClickListener {
            val selectedIndex = sizeValues.indexOf(prefs.getString("pref_list_size", "normal")).coerceAtLeast(0)
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Student List Size")
                .setSingleChoiceItems(sizeOptions, selectedIndex) { dialog, which ->
                    prefs.edit().putString("pref_list_size", sizeValues[which]).apply()
                    textListSize.text = sizeOptions[which]
                    Toast.makeText(requireContext(), "List size updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        // Global Search Visibility Setting
        val switchShowGlobalSearch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchShowGlobalSearch)
        switchShowGlobalSearch.isChecked = prefs.getBoolean("pref_show_global_search", true)
        switchShowGlobalSearch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_show_global_search", isChecked).apply()
            Toast.makeText(requireContext(), if (isChecked) "Global search enabled" else "Global search hidden", Toast.LENGTH_SHORT).show()
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
