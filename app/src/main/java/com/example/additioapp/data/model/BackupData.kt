package com.example.additioapp.data.model

import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.data.model.SessionEntity
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.BehaviorRecordEntity

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val classes: List<ClassEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val gradeItems: List<GradeItemEntity> = emptyList(),
    val gradeRecords: List<GradeRecordEntity> = emptyList(),
    val behaviorRecords: List<BehaviorRecordEntity> = emptyList()
)
