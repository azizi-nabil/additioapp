package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.SessionEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class SessionViewModel(private val repository: AppRepository) : ViewModel() {
    fun getSessionsForClass(classId: Long): LiveData<List<SessionEntity>> =
        repository.getSessionsForClass(classId)

    fun getSessionsInRange(classId: Long, start: Long, end: Long): LiveData<List<SessionEntity>> =
        repository.getSessionsInRange(classId, start, end)

    fun insert(session: SessionEntity) = viewModelScope.launch {
        repository.insertSession(session)
    }

    fun delete(session: SessionEntity) = viewModelScope.launch {
        repository.deleteSession(session)
    }
}
