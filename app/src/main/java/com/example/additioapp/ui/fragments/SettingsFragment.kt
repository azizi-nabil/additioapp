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
            viewModel.backupData(it, requireContext())
        }
    }

    private val restoreLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.restoreData(it, requireContext())
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
                .setTitle(getString(R.string.dialog_restore_data_title))
                .setMessage(getString(R.string.msg_restore_data_confirm))
                .setPositiveButton(getString(R.string.action_yes)) { _, _ ->
                    restoreLauncher.launch(arrayOf("application/json"))
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }

        // Language selector
        val btnLanguage = view.findViewById<android.widget.LinearLayout>(R.id.btnLanguage)
        val textCurrentLanguage = view.findViewById<android.widget.TextView>(R.id.textCurrentLanguage)
        
        // Update current language display
        val currentLang = com.example.additioapp.util.LocaleHelper.getLanguage(requireContext())
        textCurrentLanguage.text = com.example.additioapp.util.LocaleHelper.getLanguageDisplayName(currentLang)
        
        btnLanguage.setOnClickListener {
            val languages = com.example.additioapp.util.LocaleHelper.getSupportedLanguages()
            val languageNames = com.example.additioapp.util.LocaleHelper.getSupportedLanguageNames()
            val currentIndex = languages.indexOf(currentLang).coerceAtLeast(0)
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_select_language))
                .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                    val selectedLang = languages[which]
                    com.example.additioapp.util.LocaleHelper.setLocale(requireContext(), selectedLang)
                    dialog.dismiss()
                    
                    // Restart activity to apply language change
                    requireActivity().recreate()
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }

        // Version Display
        val textAppVersion = view.findViewById<android.widget.TextView>(R.id.textAppVersion)
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            textAppVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: Exception) {
            textAppVersion.text = getString(R.string.settings_version)
        }
        
        // Theme Selector
        val btnTheme = view.findViewById<android.widget.LinearLayout>(R.id.btnTheme)
        val textCurrentTheme = view.findViewById<android.widget.TextView>(R.id.textCurrentTheme)
        val themePrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentThemeMode = themePrefs.getInt("pref_theme", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        textCurrentTheme.text = when (currentThemeMode) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_theme_light)
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_theme_dark)
            else -> getString(R.string.settings_theme_system)
        }
        
        btnTheme.setOnClickListener {
            val options = arrayOf(
                getString(R.string.settings_theme_system),
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark)
            )
            val modes = arrayOf(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            )
            val currentIndex = modes.indexOf(currentThemeMode).coerceAtLeast(0)
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.settings_theme_title))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val selectedMode = modes[which]
                    themePrefs.edit().putInt("pref_theme", selectedMode).apply()
                    // Also save to additio_prefs for Application startup
                    requireContext().getSharedPreferences("additio_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putInt("pref_theme", selectedMode).apply()
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(selectedMode)
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }
        
        // Notification Settings
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val switchNotifications = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchNotifications)
        val btnReminderTime = view.findViewById<android.widget.LinearLayout>(R.id.btnReminderTime)
        val textReminderTime = view.findViewById<android.widget.TextView>(R.id.textReminderTime)
        
        // Load notification preferences
        switchNotifications.isChecked = prefs.getBoolean("pref_notifications_enabled", true)
        val reminderMinutes = prefs.getInt("pref_reminder_minutes", 60)
        textReminderTime.text = when (reminderMinutes) {
            30 -> getString(R.string.settings_reminder_30min)
            60 -> getString(R.string.settings_reminder_1hour)
            120 -> getString(R.string.settings_reminder_2hours)
            1440 -> getString(R.string.settings_reminder_1day)
            else -> getString(R.string.settings_reminder_1hour)
        }
        
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_notifications_enabled", isChecked).apply()
            Toast.makeText(requireContext(), 
                if (isChecked) getString(R.string.toast_notifications_enabled) else getString(R.string.toast_notifications_disabled), 
                Toast.LENGTH_SHORT).show()
        }
        
        btnReminderTime.setOnClickListener {
            val options = arrayOf(
                getString(R.string.settings_reminder_30min),
                getString(R.string.settings_reminder_1hour),
                getString(R.string.settings_reminder_2hours),
                getString(R.string.settings_reminder_1day)
            )
            val values = arrayOf(30, 60, 120, 1440)
            val currentIndex = values.indexOf(prefs.getInt("pref_reminder_minutes", 60)).coerceAtLeast(0)
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.settings_reminder_time))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    prefs.edit().putInt("pref_reminder_minutes", values[which]).apply()
                    textReminderTime.text = options[which]
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }

        // Customization
        val btnEditPositive = view.findViewById<android.widget.LinearLayout>(R.id.btnEditPositiveBehaviors)
        btnEditPositive.setOnClickListener {
            showEditListDialog(getString(R.string.dialog_edit_positive_behaviors), "pref_positive_behaviors", 
                listOf("Active Participation", "Helping Others", "Homework Completed", "Respectful Behavior", "Prepared for Class", "Leadership / Takes Initiative", "Other"))
        }

        val btnEditNegative = view.findViewById<android.widget.LinearLayout>(R.id.btnEditNegativeBehaviors)
        btnEditNegative.setOnClickListener {
            showEditListDialog(getString(R.string.dialog_edit_negative_behaviors), "pref_negative_behaviors", 
                listOf("Disturbance / Disrupting Class", "No Homework", "Late Arrival", "Disrespect (teacher/peers)", "Off-Task", "Using Phone", "Other"))
        }

        val btnEditCategories = view.findViewById<android.widget.LinearLayout>(R.id.btnEditGradeCategories)
        btnEditCategories.setOnClickListener {
            showEditListDialog(getString(R.string.dialog_edit_grade_categories), "pref_grade_categories", 
                listOf("Exam", "CC", "Test", "Homework", "Project", "Other"))
        }

        viewModel.backupStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.toast_backup_failed, it.message), Toast.LENGTH_LONG).show()
            }
        }

        viewModel.restoreStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // Ideally, restart app or refresh data. For now, a toast is fine.
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.toast_restore_failed, it.message), Toast.LENGTH_LONG).show()
            }
        }

        // Name Language Setting
        val btnNameLanguage = view.findViewById<android.widget.LinearLayout>(R.id.btnNameLanguage)
        val textNameLanguage = view.findViewById<android.widget.TextView>(R.id.textNameLanguage)
        
        // Load current setting
        val currentNameLang = prefs.getString("pref_name_language", "french") ?: "french"
        textNameLanguage.text = if (currentNameLang == "arabic") getString(R.string.lang_arabic) else getString(R.string.lang_french)

        btnNameLanguage.setOnClickListener {
            val options = arrayOf(getString(R.string.lang_french), getString(R.string.lang_arabic))
            val currentIndex = if (prefs.getString("pref_name_language", "french") == "arabic") 1 else 0
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_student_name_display))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val newLang = if (which == 1) "arabic" else "french"
                    prefs.edit().putString("pref_name_language", newLang).apply()
                    textNameLanguage.text = options[which]
                    Toast.makeText(requireContext(), getString(R.string.toast_name_display_updated), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), getString(R.string.toast_sort_order_updated), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), getString(R.string.toast_list_size_updated), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), if (isChecked) getString(R.string.toast_global_search_enabled) else getString(R.string.toast_global_search_hidden), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
