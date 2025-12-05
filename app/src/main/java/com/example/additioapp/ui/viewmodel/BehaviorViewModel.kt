package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.BehaviorRecordEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class BehaviorViewModel(private val repository: AppRepository) : ViewModel() {

    fun getBehaviorForStudent(studentId: Long): LiveData<List<BehaviorRecordEntity>> {
        return repository.getBehaviorForStudent(studentId)
    }

    fun getBehaviorsForClass(classId: Long): LiveData<List<BehaviorRecordEntity>> {
        return repository.getBehaviorsForClass(classId)
    }

    fun insertBehavior(behavior: BehaviorRecordEntity) = viewModelScope.launch {
        repository.insertBehavior(behavior)
    }

    fun updateBehavior(behavior: BehaviorRecordEntity) = viewModelScope.launch {
        repository.updateBehavior(behavior)
    }

    fun deleteBehavior(behavior: BehaviorRecordEntity) = viewModelScope.launch {
        repository.deleteBehavior(behavior)
    }
}
