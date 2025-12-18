package com.example.additioapp.data.model

import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.data.model.SessionEntity
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.BehaviorRecordEntity

data class BackupData(
    val version: Int = 3,
    val timestamp: Long = System.currentTimeMillis(),
    val classes: List<ClassEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val gradeItems: List<GradeItemEntity> = emptyList(),
    val gradeRecords: List<GradeRecordEntity> = emptyList(),
    val behaviorRecords: List<BehaviorRecordEntity> = emptyList(),
    
    // Version 2: Planner & Schedule
    val events: List<com.example.additioapp.data.model.EventEntity> = emptyList(),
    val tasks: List<com.example.additioapp.data.model.TaskEntity> = emptyList(),
    val scheduleItems: List<com.example.additioapp.data.model.ScheduleItemEntity> = emptyList(),
    val eventClassRefs: List<com.example.additioapp.data.model.EventClassCrossRef> = emptyList(),
    val taskClassRefs: List<com.example.additioapp.data.model.TaskClassCrossRef> = emptyList(),
    val scheduleItemClassRefs: List<com.example.additioapp.data.model.ScheduleItemClassCrossRef> = emptyList(),
    
    // Version 3: Absences, Notes, Units
    val teacherAbsences: List<com.example.additioapp.data.model.TeacherAbsenceEntity> = emptyList(),
    val studentNotes: List<com.example.additioapp.data.model.StudentNoteEntity> = emptyList(),
    val classNotes: List<com.example.additioapp.data.model.ClassNoteEntity> = emptyList(),
    val units: List<com.example.additioapp.data.model.UnitEntity> = emptyList()
)

