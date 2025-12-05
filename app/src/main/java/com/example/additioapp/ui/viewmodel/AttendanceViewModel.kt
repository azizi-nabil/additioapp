package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.AttendanceSessionSummary
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class AttendanceViewModel(private val repository: AppRepository) : ViewModel() {

    fun getAttendanceForStudent(studentId: Long): LiveData<List<AttendanceRecordEntity>> {
        return repository.getAttendanceForStudent(studentId)
    }

    fun getAbsencesForStudent(studentId: Long): LiveData<List<com.example.additioapp.data.model.StudentAbsenceDetail>> {
        return repository.getAbsencesForStudent(studentId)
    }

    fun getAttendanceForSession(sessionId: String): LiveData<List<AttendanceRecordEntity>> {
        return repository.getAttendanceForSession(sessionId)
    }
    
    // One-time load without LiveData to prevent rebinding during edits
    suspend fun getAttendanceForSessionOnce(sessionId: String): List<AttendanceRecordEntity> {
        return repository.getAttendanceForSessionOnce(sessionId)
    }

    fun getAttendanceForClass(classId: Long): LiveData<List<AttendanceRecordEntity>> {
        return repository.getAttendanceForClass(classId)
    }

    fun getAttendanceWithTypeForClass(classId: Long): LiveData<List<com.example.additioapp.data.model.AttendanceRecordWithType>> {
        return repository.getAttendanceWithTypeForClass(classId)
    }

    fun getSessionSummaries(classId: Long, start: Long, end: Long): LiveData<List<AttendanceSessionSummary>> {
        return repository.getAttendanceSessionSummaries(classId, start, end)
    }

    fun insertAttendance(attendance: AttendanceRecordEntity) = viewModelScope.launch {
        repository.insertAttendance(attendance)
    }

    suspend fun setAttendance(attendance: AttendanceRecordEntity) {
        repository.setAttendance(attendance)
    }

    fun insertAttendanceList(list: List<AttendanceRecordEntity>) = viewModelScope.launch {
        repository.insertAttendanceList(list)
    }

    fun updateAttendance(attendance: AttendanceRecordEntity) = viewModelScope.launch {
        repository.updateAttendance(attendance)
    }

    suspend fun deleteSession(sessionId: String) {
        repository.deleteSession(sessionId)
    }

    suspend fun deleteAttendance(studentId: Long, sessionId: String) {
        repository.deleteAttendance(studentId, sessionId)
    }

    // Session Nature
    private val _currentSessionType = androidx.lifecycle.MutableLiveData<String>("Cours")
    val currentSessionType: LiveData<String> = _currentSessionType

    suspend fun loadSessionType(classId: Long, date: Long) {
        val session = repository.getSessionByDate(classId, date)
        _currentSessionType.postValue(session?.type ?: "Cours")
    }

    suspend fun saveSessionType(classId: Long, date: Long, type: String) {
        val existingSession = repository.getSessionByDate(classId, date)
        val session = existingSession?.copy(type = type) ?: com.example.additioapp.data.model.SessionEntity(
            classId = classId,
            date = date,
            type = type
        )
        repository.insertSession(session)
        _currentSessionType.postValue(type)
    }

    suspend fun getOldestSessionDate(classId: Long): Long? {
        return repository.getOldestSessionDate(classId)
    }

    suspend fun getAllAttendanceForClass(classId: Long): List<AttendanceRecordEntity> {
        return repository.getAllAttendanceForClassOnce(classId)
    }

    suspend fun getAttendanceWithTypeForClassSync(classId: Long): List<com.example.additioapp.data.model.AttendanceRecordWithType> {
        return repository.getAttendanceWithTypeForClassSync(classId)
    }
}
