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

class SettingsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _backupStatus = MutableLiveData<Result<String>>()
    val backupStatus: LiveData<Result<String>> = _backupStatus

    private val _restoreStatus = MutableLiveData<Result<String>>()
    val restoreStatus: LiveData<Result<String>> = _restoreStatus

    fun backupData(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val data = repository.getAllData()
                withContext(Dispatchers.IO) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val json = gson.toJson(data)
                    
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                }
                _backupStatus.postValue(Result.success("Backup completed successfully"))
            } catch (e: Exception) {
                _backupStatus.postValue(Result.failure(e))
            }
        }
    }

    fun restoreData(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
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
                            
                            BackupData(
                                version = version,
                                timestamp = timestamp,
                                classes = classes,
                                students = students,
                                sessions = sessions,
                                attendanceRecords = attendanceRecords,
                                gradeItems = gradeItems,
                                gradeRecords = gradeRecords,
                                behaviorRecords = behaviorRecords
                            )
                        }
                    }
                }

                if (data != null) {
                    repository.restoreData(data)
                    _restoreStatus.postValue(Result.success("Restore completed successfully"))
                } else {
                    _restoreStatus.postValue(Result.failure(Exception("Failed to read backup file")))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _restoreStatus.postValue(Result.failure(e))
            }
        }
    }
}
