package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.AttendanceStatusEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class AttendanceStatusViewModel(private val repository: AppRepository) : ViewModel() {
    val statuses: LiveData<List<AttendanceStatusEntity>> = repository.getAttendanceStatuses()

    init {
        checkAndSeedDefaults()
    }

    private fun checkAndSeedDefaults() = viewModelScope.launch {
        if (repository.getAttendanceStatusCount() == 0) {
            val defaults = listOf(
                AttendanceStatusEntity(code = "P", label = "Present", countsAsPresent = true, colorHex = "#22C55E", orderIndex = 0),
                AttendanceStatusEntity(code = "A", label = "Absent", countsAsPresent = false, colorHex = "#EF4444", orderIndex = 1),
                AttendanceStatusEntity(code = "L", label = "Late", countsAsPresent = true, colorHex = "#EAB308", orderIndex = 2),
                AttendanceStatusEntity(code = "E", label = "Excused", countsAsPresent = false, colorHex = "#6B7280", orderIndex = 3),
                AttendanceStatusEntity(code = "JA", label = "Justified Absence", countsAsPresent = false, colorHex = "#6B7280", orderIndex = 4),
                AttendanceStatusEntity(code = "JL", label = "Justified Late", countsAsPresent = true, colorHex = "#6B7280", orderIndex = 5),
                AttendanceStatusEntity(code = "X", label = "Expelled", countsAsPresent = false, colorHex = "#1F2937", orderIndex = 6)
            )
            defaults.forEach { repository.insertAttendanceStatus(it) }
        }
    }

    fun insert(status: AttendanceStatusEntity) = viewModelScope.launch {
        repository.insertAttendanceStatus(status)
    }

    fun delete(status: AttendanceStatusEntity) = viewModelScope.launch {
        repository.deleteAttendanceStatus(status)
    }
}
