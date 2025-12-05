package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.BehaviorTypeEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class BehaviorTypeViewModel(private val repository: AppRepository) : ViewModel() {
    fun getBehaviorTypesForClass(classId: Long): LiveData<List<BehaviorTypeEntity>> =
        repository.getBehaviorTypesForClass(classId)

    fun insert(type: BehaviorTypeEntity) = viewModelScope.launch {
        repository.insertBehaviorType(type)
    }

    fun delete(type: BehaviorTypeEntity) = viewModelScope.launch {
        repository.deleteBehaviorType(type)
    }
}
