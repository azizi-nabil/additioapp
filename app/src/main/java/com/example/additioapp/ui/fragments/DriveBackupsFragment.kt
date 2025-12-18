package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.adapters.DriveBackupsAdapter
import com.example.additioapp.util.DriveBackupHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch

class DriveBackupsFragment : Fragment() {

    private lateinit var driveHelper: DriveBackupHelper
    private lateinit var adapter: DriveBackupsAdapter
    private var accessToken: String? = null
    
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var textBackupCount: TextView

    private val driveAuthLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val token = driveHelper.getTokenFromResult(requireActivity(), result)
        if (token != null) {
            accessToken = token
            loadBackups()
        } else {
            Toast.makeText(requireContext(), "Authorization failed", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_drive_backups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        driveHelper = DriveBackupHelper(requireContext())
        
        // Setup toolbar
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Setup views
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        recyclerView = view.findViewById(R.id.recyclerBackups)
        textBackupCount = view.findViewById(R.id.textBackupCount)

        // Setup RecyclerView
        adapter = DriveBackupsAdapter(
            onRestoreClick = { backup -> restoreBackup(backup) },
            onDeleteClick = { backup -> confirmDelete(backup) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Setup FAB
        view.findViewById<ExtendedFloatingActionButton>(R.id.fabBackupNow).setOnClickListener {
            createNewBackup()
        }

        // Authorize and load backups
        authorize()
    }

    private fun authorize() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val token = driveHelper.authorize(requireActivity(), driveAuthLauncher)
                if (token != null) {
                    accessToken = token
                    loadBackups()
                }
                // If null, consent is needed and will be handled by driveAuthLauncher
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Authorization error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBackups() {
        val token = accessToken ?: return
        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val backups = driveHelper.listBackups(token)
                progressBar.visibility = View.GONE
                
                // Update backup count in header
                textBackupCount.text = getString(R.string.backups_synced_count, backups.size)
                
                if (backups.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(backups)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading backups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreBackup(backup: DriveBackupHelper.DriveBackupFile) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_restore_backup_title)
            .setMessage(getString(R.string.dialog_restore_backup_message, backup.name))
            .setPositiveButton(R.string.action_restore) { _, _ ->
                performRestore(backup)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun performRestore(backup: DriveBackupHelper.DriveBackupFile) {
        val token = accessToken ?: return
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val json = driveHelper.downloadBackup(token, backup.id)
                progressBar.visibility = View.GONE

                if (json == null) {
                    Toast.makeText(requireContext(), "Failed to download backup", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val repository = (requireActivity().application as AdditioApplication).repository
                val gson = GsonBuilder().create()
                val backupData = gson.fromJson(json, com.example.additioapp.data.model.BackupData::class.java)
                repository.restoreData(backupData)
                
                Toast.makeText(requireContext(), R.string.toast_restore_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Restore error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(backup: DriveBackupHelper.DriveBackupFile) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_backup_title)
            .setMessage(getString(R.string.dialog_delete_backup_message, backup.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteBackup(backup)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deleteBackup(backup: DriveBackupHelper.DriveBackupFile) {
        val token = accessToken ?: return
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                driveHelper.deleteBackupFile(token, backup.id)
                Toast.makeText(requireContext(), R.string.toast_backup_deleted, Toast.LENGTH_SHORT).show()
                loadBackups() // Refresh list
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Delete error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNewBackup() {
        val token = accessToken ?: return
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val repository = (requireActivity().application as AdditioApplication).repository
                val backupData = repository.getAllData()
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(backupData)

                val success = driveHelper.uploadBackup(token, json)
                progressBar.visibility = View.GONE

                if (success) {
                    Toast.makeText(requireContext(), R.string.toast_backup_success, Toast.LENGTH_SHORT).show()
                    loadBackups() // Refresh list
                } else {
                    Toast.makeText(requireContext(), R.string.toast_backup_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Backup error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
