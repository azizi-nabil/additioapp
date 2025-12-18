package com.example.additioapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.util.DriveBackupHelper
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class DriveBackupsAdapter(
    private var backups: List<DriveBackupHelper.DriveBackupFile> = emptyList(),
    private val onRestoreClick: (DriveBackupHelper.DriveBackupFile) -> Unit,
    private val onDeleteClick: (DriveBackupHelper.DriveBackupFile) -> Unit
) : RecyclerView.Adapter<DriveBackupsAdapter.BackupViewHolder>() {

    fun submitList(list: List<DriveBackupHelper.DriveBackupFile>) {
        backups = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drive_backup, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        holder.bind(backups[position], onRestoreClick, onDeleteClick)
    }

    override fun getItemCount(): Int = backups.size

    class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textBackupName)
        private val textDate: TextView = itemView.findViewById(R.id.textBackupDate)
        private val btnRestore: MaterialButton = itemView.findViewById(R.id.btnRestore)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            backup: DriveBackupHelper.DriveBackupFile,
            onRestoreClick: (DriveBackupHelper.DriveBackupFile) -> Unit,
            onDeleteClick: (DriveBackupHelper.DriveBackupFile) -> Unit
        ) {
            // Format name to be more readable
            val displayName = backup.name
                .replace("backup_", "")
                .replace(".json", "")
                .replace("_", " at ")
                .replace("-", "/")
            textName.text = displayName
            
            // Parse and format the date
            val formattedDate = try {
                // Drive API returns ISO 8601 format: 2024-12-18T20:30:00.000Z
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                val date = inputFormat.parse(backup.modifiedTime.take(19))
                date?.let { outputFormat.format(it) } ?: backup.modifiedTime.take(10)
            } catch (e: Exception) {
                backup.modifiedTime.take(10)
            }
            textDate.text = formattedDate

            btnRestore.setOnClickListener { onRestoreClick(backup) }
            btnDelete.setOnClickListener { onDeleteClick(backup) }
        }
    }
}
