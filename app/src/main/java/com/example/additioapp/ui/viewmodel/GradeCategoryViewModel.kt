package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.GradeCategoryEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class GradeCategoryViewModel(private val repository: AppRepository) : ViewModel() {
    fun getCategoriesForClass(classId: Long): LiveData<List<GradeCategoryEntity>> =
        repository.getGradeCategoriesForClass(classId)

    fun insert(category: GradeCategoryEntity) = viewModelScope.launch {
        repository.insertGradeCategory(category)
    }

    fun delete(category: GradeCategoryEntity) = viewModelScope.launch {
        repository.deleteGradeCategory(category)
    }
}
