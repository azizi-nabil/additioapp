package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class ClassViewModel(private val repository: AppRepository) : ViewModel() {

    val allClasses: LiveData<List<ClassEntity>> = repository.allClasses
    val allClassesWithSummary: LiveData<List<com.example.additioapp.data.model.ClassWithSummary>> = repository.allClassesWithSummary
    val distinctYears: LiveData<List<String>> = repository.distinctYears

    fun getClassesBySemesterAndYear(semester: String, year: String): LiveData<List<ClassEntity>> {
        return repository.getClassesBySemesterAndYear(semester, year)
    }

    fun getClassesWithSummaryBySemesterAndYear(semester: String, year: String): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>> {
        return repository.getClassesWithSummaryBySemesterAndYear(semester, year)
    }

    fun getArchivedClassesByYear(year: String): LiveData<List<ClassEntity>> {
        return repository.getArchivedClassesByYear(year)
    }

    fun getArchivedClassesWithSummaryByYear(year: String): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>> {
        return repository.getArchivedClassesWithSummaryByYear(year)
    }

    fun insertClass(classEntity: ClassEntity) = viewModelScope.launch {
        repository.insertClass(classEntity)
    }

    fun updateClass(classEntity: ClassEntity) = viewModelScope.launch {
        repository.updateClass(classEntity)
    }

    fun deleteClass(classEntity: ClassEntity) = viewModelScope.launch {
        repository.deleteClass(classEntity)
    }
}
