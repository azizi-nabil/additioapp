package com.example.additioapp.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.BackupData
import com.example.additioapp.data.repository.AppRepository
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import android.util.Log

class SettingsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _backupStatus = MutableLiveData<Result<String>>()
    val backupStatus: LiveData<Result<String>> = _backupStatus

    private val _restoreStatus = MutableLiveData<Result<String>>()
    val restoreStatus: LiveData<Result<String>> = _restoreStatus

    fun backupData(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val data = repository.getAllData()
                withContext(Dispatchers.IO) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val json = gson.toJson(data)
                    
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                }
                _backupStatus.postValue(Result.success(context.getString(com.example.additioapp.R.string.backup_success)))
            } catch (e: Exception) {
                _backupStatus.postValue(Result.failure(e))
            }
        }
    }

    fun restoreData(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream).use { reader ->
                            // Read JSON as string first
                            val jsonString = reader.readText()
                            
                            // Parse with lenient settings
                            val gson = GsonBuilder()
                                .setLenient()
                                .create()
                            
                            // Parse manually to handle missing fields
                            val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
                            
                            val version = jsonObject.get("version")?.asInt ?: 1
                            val timestamp = jsonObject.get("timestamp")?.asLong ?: System.currentTimeMillis()
                            
                            // Parse classes with default color
                            val classesArray = jsonObject.getAsJsonArray("classes") ?: com.google.gson.JsonArray()
                            val classes = classesArray.map { classJson ->
                                val obj = classJson.asJsonObject
                                com.example.additioapp.data.model.ClassEntity(
                                    id = obj.get("id")?.asLong ?: 0,
                                    name = obj.get("name")?.asString ?: "",
                                    year = obj.get("year")?.asString ?: "",
                                    location = obj.get("location")?.asString ?: "",
                                    schedule = obj.get("schedule")?.asString ?: "",
                                    semester = obj.get("semester")?.asString ?: "Semester 1",
                                    isArchived = obj.get("isArchived")?.asBoolean ?: false,
                                    color = obj.get("color")?.asString ?: "#2196F3"
                                )
                            }
                            
                            // Parse other entities normally
                            val students = jsonObject.getAsJsonArray("students")?.map { 
                                gson.fromJson(it, com.example.additioapp.data.model.StudentEntity::class.java) 
                            } ?: emptyList()
                            
                            val sessions = jsonObject.getAsJsonArray("sessions")?.map { 
                                gson.fromJson(it, com.example.additioapp.data.model.SessionEntity::class.java) 
                            } ?: emptyList()
                            
                            val attendanceRecords = jsonObject.getAsJsonArray("attendanceRecords")?.map { 
                                gson.fromJson(it, com.example.additioapp.data.model.AttendanceRecordEntity::class.java) 
                            } ?: emptyList()
                            
                            val gradeItems = jsonObject.getAsJsonArray("gradeItems")?.map { 
                                gson.fromJson(it, com.example.additioapp.data.model.GradeItemEntity::class.java) 
                            } ?: emptyList()
                            
                            val gradeRecords = jsonObject.getAsJsonArray("gradeRecords")?.map { 
                                gson.fromJson(it, com.example.additioapp.data.model.GradeRecordEntity::class.java) 
                            } ?: emptyList()
                            
                            val behaviorRecords = jsonObject.getAsJsonArray("behaviorRecords")?.map { 
                                gson.fromJson(it, com.example.additioapp.data.model.BehaviorRecordEntity::class.java) 
                            } ?: emptyList()
                            
                            // Parse v2 planner data (events, tasks, scheduleItems, and cross-refs)
                            val events = jsonObject.getAsJsonArray("events")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.EventEntity::class.java)
                            } ?: emptyList()
                            
                            val tasks = jsonObject.getAsJsonArray("tasks")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.TaskEntity::class.java)
                            } ?: emptyList()
                            
                            val scheduleItems = jsonObject.getAsJsonArray("scheduleItems")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.ScheduleItemEntity::class.java)
                            } ?: emptyList()
                            
                            val eventClassRefs = jsonObject.getAsJsonArray("eventClassRefs")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.EventClassCrossRef::class.java)
                            } ?: emptyList()
                            
                            val taskClassRefs = jsonObject.getAsJsonArray("taskClassRefs")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.TaskClassCrossRef::class.java)
                            } ?: emptyList()
                            
                            val scheduleItemClassRefs = jsonObject.getAsJsonArray("scheduleItemClassRefs")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.ScheduleItemClassCrossRef::class.java)
                            } ?: emptyList()
                            
                            // Parse v3 data (absences, notes, units) with defaults for older backups
                            val teacherAbsences = jsonObject.getAsJsonArray("teacherAbsences")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.TeacherAbsenceEntity::class.java)
                            } ?: emptyList()
                            
                            val studentNotes = jsonObject.getAsJsonArray("studentNotes")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.StudentNoteEntity::class.java)
                            } ?: emptyList()
                            
                            val classNotes = jsonObject.getAsJsonArray("classNotes")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.ClassNoteEntity::class.java)
                            } ?: emptyList()
                            
                            val units = jsonObject.getAsJsonArray("units")?.map {
                                gson.fromJson(it, com.example.additioapp.data.model.UnitEntity::class.java)
                            } ?: emptyList()
                            
                            BackupData(
                                version = version,
                                timestamp = timestamp,
                                classes = classes,
                                students = students,
                                sessions = sessions,
                                attendanceRecords = attendanceRecords,
                                gradeItems = gradeItems,
                                gradeRecords = gradeRecords,
                                behaviorRecords = behaviorRecords,
                                events = events,
                                tasks = tasks,
                                scheduleItems = scheduleItems,
                                eventClassRefs = eventClassRefs,
                                taskClassRefs = taskClassRefs,
                                scheduleItemClassRefs = scheduleItemClassRefs,
                                teacherAbsences = teacherAbsences,
                                studentNotes = studentNotes,
                                classNotes = classNotes,
                                units = units
                            )
                        }
                    }
                }

                if (data != null) {
                    // Log what we're about to restore
                    Log.d("SettingsVM", "Restoring: ${data.classes.size} classes, ${data.students.size} students, ${data.sessions.size} sessions")
                    try {
                        repository.restoreData(data)
                        _restoreStatus.postValue(Result.success(context.getString(com.example.additioapp.R.string.restore_success)))
                    } catch (e: Exception) {
                        // Detailed error logging for repository operation
                        Log.e("SettingsVM", "Restore failed during repository operation: ${e.message}", e)
                        _restoreStatus.postValue(Result.failure(Exception(context.getString(com.example.additioapp.R.string.restore_error_generic, e.message?.take(100) ?: "Unknown error"))))
                    }
                } else {
                    Log.e("SettingsVM", "Restore failed: Could not read backup file or file was empty.")
                    _restoreStatus.postValue(Result.failure(Exception(context.getString(com.example.additioapp.R.string.restore_error_file))))
                }
            } catch (e: Exception) {
                // Detailed error logging for file read/parsing
                Log.e("SettingsVM", "File read or parsing failed: ${e.message}", e)
                _restoreStatus.postValue(Result.failure(Exception(context.getString(com.example.additioapp.R.string.restore_error_generic, e.message?.take(100) ?: "File read error"))))
            }
        }
    }
}
