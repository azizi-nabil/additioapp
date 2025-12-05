package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class StudentViewModel(private val repository: AppRepository) : ViewModel() {

    val allStudents: LiveData<List<StudentEntity>> = repository.getAllStudents()

    fun getStudentsForClass(classId: Long): LiveData<List<StudentEntity>> {
        return repository.getStudentsForClass(classId)
    }

    fun insertStudent(student: StudentEntity) = viewModelScope.launch {
        repository.insertStudent(student)
    }

    fun insertStudents(students: List<StudentEntity>) = viewModelScope.launch {
        repository.insertStudents(students)
    }

    fun updateStudent(student: StudentEntity) = viewModelScope.launch {
        repository.updateStudent(student)
    }

    fun deleteStudent(student: StudentEntity) = viewModelScope.launch {
        repository.deleteStudent(student)
    }

    suspend fun getStudentsForClassOnce(classId: Long): List<StudentEntity> {
        return repository.getStudentsForClassOnce(classId)
    }
}
