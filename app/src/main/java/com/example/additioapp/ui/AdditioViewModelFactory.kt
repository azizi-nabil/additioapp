package com.example.additioapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.additioapp.data.repository.AppRepository

class AdditioViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.ClassViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.ClassViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.StudentViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.AttendanceViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.GradeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.GradeViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.BehaviorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.BehaviorViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.AttendanceStatusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.AttendanceStatusViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.BehaviorTypeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.BehaviorTypeViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.GradeCategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.GradeCategoryViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.SessionViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.additioapp.ui.viewmodel.SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.additioapp.ui.viewmodel.SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
