package com.example.additioapp.data.repository

import androidx.lifecycle.LiveData
import com.example.additioapp.data.dao.AttendanceDao
import com.example.additioapp.data.dao.AttendanceStatusDao
import com.example.additioapp.data.dao.BehaviorDao
import com.example.additioapp.data.dao.BehaviorTypeDao
import com.example.additioapp.data.dao.ClassDao
import com.example.additioapp.data.dao.EventDao
import com.example.additioapp.data.dao.GradeCategoryDao
import com.example.additioapp.data.dao.GradeDao
import com.example.additioapp.data.dao.ScheduleItemDao
import com.example.additioapp.data.dao.SessionDao
import com.example.additioapp.data.dao.StudentDao
import com.example.additioapp.data.dao.TaskDao
import com.example.additioapp.data.dao.TeacherAbsenceDao
import com.example.additioapp.data.dao.UnitDao
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.AttendanceStatusEntity
import com.example.additioapp.data.model.BehaviorRecordEntity
import com.example.additioapp.data.model.BehaviorTypeEntity
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.EventEntity
import com.example.additioapp.data.model.GradeCategoryEntity
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.ScheduleItemEntity
import com.example.additioapp.data.model.SessionEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.data.model.TaskEntity
import com.example.additioapp.data.model.TeacherAbsenceEntity
import com.example.additioapp.data.model.UnitEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val gradeDao: GradeDao,
    private val behaviorDao: BehaviorDao,
    private val attendanceStatusDao: AttendanceStatusDao,
    private val behaviorTypeDao: BehaviorTypeDao,
    private val gradeCategoryDao: GradeCategoryDao,
    private val sessionDao: SessionDao,
    private val unitDao: UnitDao,
    private val eventDao: EventDao,
    private val taskDao: TaskDao,
    private val scheduleItemDao: ScheduleItemDao,
    private val teacherAbsenceDao: TeacherAbsenceDao,
    private val sharedPreferences: android.content.SharedPreferences
) {
    
    // Custom Types Management
    companion object {
        private const val KEY_EVENT_TYPES = "custom_event_types"
        private const val KEY_SESSION_TYPES = "custom_session_types"
        private val DEFAULT_EVENT_TYPES = setOf("OTHER", "EXAM", "MEETING", "DEADLINE")
        private val DEFAULT_SESSION_TYPES = setOf("Cours", "TP", "TD", "Exam", "Other")
    }

    fun getEventTypes(): List<String> {
        val saved = sharedPreferences.getStringSet(KEY_EVENT_TYPES, emptySet()) ?: emptySet()
        return (DEFAULT_EVENT_TYPES + saved).sorted()
    }

    fun addEventType(type: String) {
        if (type.isBlank()) return
        val current = sharedPreferences.getStringSet(KEY_EVENT_TYPES, emptySet()) ?: emptySet()
        if (!current.contains(type) && !DEFAULT_EVENT_TYPES.contains(type)) {
            sharedPreferences.edit().putStringSet(KEY_EVENT_TYPES, current + type).apply()
        }
    }

    fun getSessionTypes(): List<String> {
        val saved = sharedPreferences.getStringSet(KEY_SESSION_TYPES, emptySet()) ?: emptySet()
        return (DEFAULT_SESSION_TYPES + saved).sorted()
    }

    fun addSessionType(type: String) {
        if (type.isBlank()) return
        val current = sharedPreferences.getStringSet(KEY_SESSION_TYPES, emptySet()) ?: emptySet()
        if (!current.contains(type) && !DEFAULT_SESSION_TYPES.contains(type)) {
            sharedPreferences.edit().putStringSet(KEY_SESSION_TYPES, current + type).apply()
        }
    }
    // Classes
    val allClasses: LiveData<List<ClassEntity>> = classDao.getAllClasses()
    val allClassesIncludingArchived: LiveData<List<ClassEntity>> = classDao.getAllClassesIncludingArchived()
    val allClassesWithSummary: LiveData<List<com.example.additioapp.data.model.ClassWithSummary>> = classDao.getAllClassesWithSummary()
    val distinctYears: LiveData<List<String>> = classDao.getDistinctYears()
    
    fun getClassesBySemesterAndYear(semester: String, year: String): LiveData<List<ClassEntity>> = 
        classDao.getClassesBySemesterAndYear(semester, year)

    fun getClassesWithSummaryBySemesterAndYear(semester: String, year: String): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>> = 
        classDao.getClassesWithSummaryBySemesterAndYear(semester, year)
        
    fun getArchivedClassesByYear(year: String): LiveData<List<ClassEntity>> = 
        classDao.getArchivedClassesByYear(year)

    fun getArchivedClassesWithSummaryByYear(year: String): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>> = 
        classDao.getArchivedClassesWithSummaryByYear(year)

    suspend fun insertClass(classEntity: ClassEntity) = classDao.insertClass(classEntity)
    suspend fun insertClassAndGetId(classEntity: ClassEntity): Long = classDao.insertClass(classEntity)
    suspend fun updateClass(classEntity: ClassEntity) = classDao.updateClass(classEntity)
    suspend fun deleteClass(classEntity: ClassEntity) = classDao.deleteClass(classEntity)
    suspend fun getClassById(id: Long) = classDao.getClassById(id)

    // Students
    fun getAllStudents(): LiveData<List<StudentEntity>> = studentDao.getAllStudents()
    fun getStudentsForClass(classId: Long) = studentDao.getStudentsForClass(classId)
    suspend fun getStudentById(id: Long) = studentDao.getStudentById(id)
    suspend fun insertStudent(student: StudentEntity) = studentDao.insertStudent(student)
    suspend fun insertStudents(students: List<StudentEntity>) = studentDao.insertStudents(students)
    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)
    suspend fun deleteStudent(student: StudentEntity) = studentDao.deleteStudent(student)

    // Attendance
    fun getAttendanceForStudent(studentId: Long) = attendanceDao.getAttendanceForStudent(studentId)
    fun getAbsencesForStudent(studentId: Long) = attendanceDao.getAbsencesForStudent(studentId)
    fun getCoursPresenceForStudent(studentId: Long, classId: Long) = attendanceDao.getCoursPresenceForStudent(studentId, classId)
    suspend fun getTotalCoursSessionCount(classId: Long): Int {
        return attendanceDao.getTotalCoursSessionCount(classId)
    }

    suspend fun getTotalSessionCountByType(classId: Long, type: String): Int {
        return attendanceDao.getTotalSessionCountByType(classId, type)
    }
    fun getAttendanceForSession(sessionId: String) = attendanceDao.getAttendanceForSession(sessionId)
    
    // One-time load without LiveData
    suspend fun getAttendanceForSessionOnce(sessionId: String): List<AttendanceRecordEntity> {
        return attendanceDao.getAttendanceForSessionOnce(sessionId)
    }
    
    suspend fun countRecordsBySessionIdPattern(pattern: String): Int {
        return attendanceDao.countRecordsBySessionIdPattern(pattern)
    }
    
    suspend fun getStudentsForClassOnce(classId: Long): List<StudentEntity> {
        return studentDao.getStudentsForClassSync(classId)
    }

    suspend fun getAllAttendanceForClassOnce(classId: Long): List<AttendanceRecordEntity> {
        return attendanceDao.getAllAttendanceForClassSync(classId)
    }
    
    /**
     * Deduplicate attendance records by keeping only one record per (studentId, sessionId).
     * For duplicates, keeps the record with the highest ID (most recent).
     * Returns the number of duplicates removed.
     */
    suspend fun deduplicateAttendanceSessions(): Int {
        val allRecords = attendanceDao.getAllAttendanceSync()
        
        // Group by (studentId, sessionId) - only one record allowed per student per session
        // This allows different session types (Cours, TD, TP) on the same date
        val grouped = allRecords.groupBy { Pair(it.studentId, it.sessionId) }
        
        var removed = 0
        for ((_, records) in grouped) {
            if (records.size > 1) {
                // Keep the one with highest ID (most recent), delete others
                val sorted = records.sortedByDescending { it.id }
                val toKeep = sorted.first()
                val toDelete = sorted.drop(1)
                
                for (record in toDelete) {
                    attendanceDao.deleteById(record.id)
                    removed++
                }
                
                android.util.Log.d("AppRepository", "Deduped: kept record ${toKeep.id} for student ${toKeep.studentId} on ${toKeep.date}, removed ${toDelete.size}")
            }
        }
        
        android.util.Log.d("AppRepository", "Deduplication complete: removed $removed duplicate records")
        return removed
    }
    
    fun getAttendanceForClass(classId: Long) = attendanceDao.getAttendanceForClass(classId)
    fun getAttendanceWithTypeForClass(classId: Long) = attendanceDao.getAttendanceWithTypeForClass(classId)
    
    suspend fun getAttendanceWithTypeForClassSync(classId: Long): List<com.example.additioapp.data.model.AttendanceRecordWithType> {
        return attendanceDao.getAttendanceWithTypeForClassSync(classId)
    }
    fun getAttendanceSessionSummaries(classId: Long, start: Long, end: Long) =
        attendanceDao.getSessionSummaries(classId, start, end)
    suspend fun insertAttendance(attendance: AttendanceRecordEntity) = attendanceDao.insertAttendance(attendance)
    suspend fun setAttendance(attendance: AttendanceRecordEntity) {
        // Check if exists
        val existing = attendanceDao.getAttendanceForStudentAndSession(attendance.studentId, attendance.sessionId)
        if (existing != null) {
            attendanceDao.updateAttendance(attendance.copy(id = existing.id))
        } else {
            attendanceDao.insertAttendance(attendance)
        }
    }
    suspend fun updateAttendance(attendance: AttendanceRecordEntity) = attendanceDao.updateAttendance(attendance)
    suspend fun insertAttendanceList(list: List<AttendanceRecordEntity>) = attendanceDao.insertAttendanceList(list)
    suspend fun deleteSession(sessionId: String) = attendanceDao.deleteSession(sessionId)
    suspend fun deleteAttendance(studentId: Long, sessionId: String) = attendanceDao.deleteAttendance(studentId, sessionId)
    suspend fun updateSessionId(oldSessionId: String, newSessionId: String) = attendanceDao.updateSessionId(oldSessionId, newSessionId)

    // Grades
    fun getAllGradeItems() = gradeDao.getAllGradeItems()
    fun getGradeItemsForClass(classId: Long) = gradeDao.getGradeItemsForClass(classId)
    fun getGradeItemById(id: Long) = gradeDao.getGradeItemById(id)
    suspend fun getGradeItemsForClassSync(classId: Long) = gradeDao.getGradeItemsForClassSync(classId)
    suspend fun insertGradeItem(item: GradeItemEntity) = gradeDao.insertGradeItem(item)
    suspend fun deleteGradeItem(item: GradeItemEntity) = gradeDao.deleteGradeItem(item)

    fun getGradesForStudent(studentId: Long) = gradeDao.getGradesForStudent(studentId)
    fun getGradesForItem(itemId: Long) = gradeDao.getGradesForItem(itemId)
    fun getGradeRecordsForClass(classId: Long) = gradeDao.getGradeRecordsForClass(classId)
    fun getStudentGradeDetails(studentId: Long) = gradeDao.getStudentGradeDetails(studentId)
    suspend fun getGradeRecordsForClassSync(classId: Long) = gradeDao.getGradeRecordsForClassSync(classId)
    suspend fun insertGradeRecord(record: GradeRecordEntity) = gradeDao.insertGradeRecord(record)
    suspend fun updateGradeRecord(record: GradeRecordEntity) = gradeDao.updateGradeRecord(record)

    // Behavior
    fun getBehaviorForStudent(studentId: Long) = behaviorDao.getBehaviorForStudent(studentId)
    fun getBehaviorsForClass(classId: Long) = behaviorDao.getBehaviorsForClass(classId)
    suspend fun getBehaviorsForClassSync(classId: Long) = behaviorDao.getBehaviorsForClassSync(classId)
    suspend fun insertBehavior(behavior: BehaviorRecordEntity) = behaviorDao.insertBehavior(behavior)
    suspend fun updateBehavior(behavior: BehaviorRecordEntity) = behaviorDao.updateBehavior(behavior)
    suspend fun deleteBehavior(behavior: BehaviorRecordEntity) = behaviorDao.deleteBehavior(behavior)

    // Attendance statuses
    fun getAttendanceStatuses(): LiveData<List<AttendanceStatusEntity>> = attendanceStatusDao.getAllStatuses()
    suspend fun insertAttendanceStatus(status: AttendanceStatusEntity) = attendanceStatusDao.insertStatus(status)
    suspend fun deleteAttendanceStatus(status: AttendanceStatusEntity) = attendanceStatusDao.deleteStatus(status)
    suspend fun getAttendanceStatusCount() = attendanceStatusDao.getStatusCount()

    // Behavior types
    fun getBehaviorTypesForClass(classId: Long): LiveData<List<BehaviorTypeEntity>> =
        behaviorTypeDao.getBehaviorTypesForClass(classId)
    suspend fun insertBehaviorType(type: BehaviorTypeEntity) = behaviorTypeDao.insertBehaviorType(type)
    suspend fun deleteBehaviorType(type: BehaviorTypeEntity) = behaviorTypeDao.deleteBehaviorType(type)

    // Grade categories
    fun getGradeCategoriesForClass(classId: Long): LiveData<List<GradeCategoryEntity>> =
        gradeCategoryDao.getCategoriesForClass(classId)
    suspend fun insertGradeCategory(category: GradeCategoryEntity) = gradeCategoryDao.insertCategory(category)
    suspend fun deleteGradeCategory(category: GradeCategoryEntity) = gradeCategoryDao.deleteCategory(category)

    // Sessions
    fun getSessionsForClass(classId: Long): LiveData<List<SessionEntity>> = sessionDao.getSessionsForClass(classId)
    fun getSessionsInRange(classId: Long, start: Long, end: Long): LiveData<List<SessionEntity>> =
        sessionDao.getSessionsInRange(classId, start, end)
    suspend fun getSessionByDate(classId: Long, date: Long): SessionEntity? = sessionDao.getSessionByDate(classId, date)
    suspend fun insertSession(session: SessionEntity) = sessionDao.insertSession(session)
    suspend fun deleteSession(session: SessionEntity) = sessionDao.deleteSession(session)
    suspend fun getOldestSessionDate(classId: Long): Long? = sessionDao.getOldestSessionDate(classId)
    suspend fun getSessionCountByType(classId: Long, type: String): Int = sessionDao.getSessionCountByType(classId, type)

    // Units
    fun getUnitsForClass(classId: Long): LiveData<List<UnitEntity>> = unitDao.getUnitsForClass(classId)
    suspend fun insertUnit(unit: UnitEntity) = unitDao.insertUnit(unit)
    suspend fun deleteUnit(unit: UnitEntity) = unitDao.deleteUnit(unit)
    // Backup & Restore
    suspend fun getAllData(): com.example.additioapp.data.model.BackupData {
        return com.example.additioapp.data.model.BackupData(
            version = 2,
            timestamp = System.currentTimeMillis(),
            classes = classDao.getAllClassesSync(),
            students = studentDao.getAllStudentsSync(),
            sessions = sessionDao.getAllSessionsSync(),
            attendanceRecords = attendanceDao.getAllAttendanceSync(),
            gradeItems = gradeDao.getAllGradeItemsSync(),
            gradeRecords = gradeDao.getAllGradeRecordsSync(),
            behaviorRecords = behaviorDao.getAllBehaviorsSync(),
            
            // New v2 data
            events = eventDao.getAllEventsSync(),
            tasks = taskDao.getAllTasksSync(),
            scheduleItems = scheduleItemDao.getAllScheduleItemsSync(),
            eventClassRefs = eventDao.getAllEventClassRefs(),
            taskClassRefs = taskDao.getAllTaskClassRefs(),
            scheduleItemClassRefs = scheduleItemDao.getAllScheduleItemClassRefs()
        )
    }

    suspend fun restoreData(data: com.example.additioapp.data.model.BackupData) {
        // Comprehensive sanitization with detailed error handling for each entity type
        
        try {
            // Classes - ensure all required fields have defaults
            val sanitizedClasses = data.classes.map { classEntity ->
                classEntity.copy(
                    color = classEntity.color ?: "#2196F3",
                    semester = classEntity.semester ?: "Semester 1",
                    location = classEntity.location ?: "",
                    schedule = classEntity.schedule ?: ""
                )
            }
            android.util.Log.d("AppRepository", "Inserting ${sanitizedClasses.size} classes")
            classDao.insertClasses(sanitizedClasses)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore classes: ${e.message}", e)
            throw Exception("Classes restore failed: ${e.message}")
        }
        
        try {
            // Students - sanitize all string fields to prevent null constraint violations
            val sanitizedStudents = data.students.map { student ->
                student.copy(
                    matricule = student.matricule ?: "",
                    firstNameFr = student.firstNameFr ?: "",
                    lastNameFr = student.lastNameFr ?: "",
                    name = student.name ?: "",
                    studentId = student.studentId ?: ""
                )
            }
            android.util.Log.d("AppRepository", "Inserting ${sanitizedStudents.size} students")
            studentDao.insertStudents(sanitizedStudents)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore students: ${e.message}", e)
            throw Exception("Students restore failed: ${e.message}")
        }
        
        try {
            // Sessions - ensure type is not null
            val sanitizedSessions = data.sessions.map { session ->
                session.copy(
                    type = session.type ?: "Cours"
                )
            }
            android.util.Log.d("AppRepository", "Inserting ${sanitizedSessions.size} sessions")
            sessionDao.insertSessions(sanitizedSessions)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore sessions: ${e.message}", e)
            throw Exception("Sessions restore failed: ${e.message}")
        }
        
        try {
            // Attendance - already has defaults
            android.util.Log.d("AppRepository", "Inserting ${data.attendanceRecords.size} attendance records")
            attendanceDao.insertAttendanceList(data.attendanceRecords)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore attendance: ${e.message}", e)
            throw Exception("Attendance restore failed: ${e.message}")
        }
        
        try {
            // Grade Items - ensure all fields have defaults
            val sanitizedGradeItems = data.gradeItems.map { item ->
                item.copy(
                    category = item.category ?: "General",
                    weight = item.weight ?: 1.0f
                )
            }
            android.util.Log.d("AppRepository", "Inserting ${sanitizedGradeItems.size} grade items")
            gradeDao.insertGradeItems(sanitizedGradeItems)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore grade items: ${e.message}", e)
            throw Exception("Grade items restore failed: ${e.message}")
        }

        try {
            // Grade Records - ensure status is not null
            val sanitizedGrades = data.gradeRecords.map { record ->
                record.copy(status = record.status ?: "PRESENT")
            }
            android.util.Log.d("AppRepository", "Inserting ${sanitizedGrades.size} grade records")
            gradeDao.insertGradeRecords(sanitizedGrades)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore grade records: ${e.message}", e)
            throw Exception("Grade records restore failed: ${e.message}")
        }
        
        try {
            // Behavior Records - already has defaults
            android.util.Log.d("AppRepository", "Inserting ${data.behaviorRecords.size} behavior records")
            behaviorDao.insertBehaviors(data.behaviorRecords)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to restore behavior records: ${e.message}", e)
            throw Exception("Behavior records restore failed: ${e.message}")
        }
        
        // Restore v2 data (only if present)
        if (data.events.isNotEmpty()) {
            try {
                android.util.Log.d("AppRepository", "Inserting ${data.events.size} events")
                eventDao.insertEvents(data.events)
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Failed to restore events: ${e.message}", e)
            }
        }
        if (data.tasks.isNotEmpty()) {
            try {
                taskDao.insertTasks(data.tasks)
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Failed to restore tasks: ${e.message}", e)
            }
        }
        if (data.scheduleItems.isNotEmpty()) {
            try {
                scheduleItemDao.insertScheduleItems(data.scheduleItems)
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Failed to restore schedule items: ${e.message}", e)
            }
        }
        
        if (data.eventClassRefs.isNotEmpty()) eventDao.insertEventClassRefs(data.eventClassRefs)
        if (data.taskClassRefs.isNotEmpty()) taskDao.insertTaskClassRefs(data.taskClassRefs)
        if (data.scheduleItemClassRefs.isNotEmpty()) scheduleItemDao.insertScheduleItemClassRefs(data.scheduleItemClassRefs)
        
        android.util.Log.d("AppRepository", "Restore completed successfully")
    }

    // Events
    fun getAllEvents() = eventDao.getAllEvents()
    fun getEventsForDate(date: Long) = eventDao.getEventsForDate(date)
    suspend fun getEventsForDateSync(date: Long) = eventDao.getEventsForDateSync(date)
    fun getEventsInRange(startDate: Long, endDate: Long) = eventDao.getEventsInRange(startDate, endDate)
    suspend fun getEventsInRangeSync(startDate: Long, endDate: Long) = eventDao.getEventsInRangeSync(startDate, endDate)
    fun getEventsForClass(classId: Long) = eventDao.getEventsForClass(classId)
    suspend fun getEventById(eventId: Long) = eventDao.getEventById(eventId)
    suspend fun getDatesWithEvents(startDate: Long, endDate: Long) = eventDao.getDatesWithEvents(startDate, endDate)
    suspend fun insertEvent(event: EventEntity) = eventDao.insertEvent(event)
    suspend fun updateEvent(event: EventEntity) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: EventEntity) = eventDao.deleteEvent(event)
    suspend fun deleteEventById(eventId: Long) = eventDao.deleteEventById(eventId)
    suspend fun deleteEventSeries(seriesId: Long, fromDate: Long) = eventDao.deleteFutureEvents(seriesId, fromDate)
    
    // Event-Class cross reference
    suspend fun insertEventWithClasses(event: EventEntity, classIds: List<Long>): Long {
        val eventId = eventDao.insertEvent(event)
        if (classIds.isNotEmpty()) {
            val refs = classIds.map { com.example.additioapp.data.model.EventClassCrossRef(eventId, it) }
            eventDao.insertEventClassRefs(refs)
        }
        return eventId
    }
    
    suspend fun updateEventWithClasses(event: EventEntity, classIds: List<Long>) {
        eventDao.updateEvent(event)
        eventDao.deleteEventClassRefs(event.id)
        if (classIds.isNotEmpty()) {
            val refs = classIds.map { com.example.additioapp.data.model.EventClassCrossRef(event.id, it) }
            eventDao.insertEventClassRefs(refs)
        }
    }
    
    suspend fun getClassIdsForEvent(eventId: Long): List<Long> = eventDao.getClassIdsForEvent(eventId)

    // Tasks
    fun getPendingTasks() = taskDao.getPendingTasks()
    fun getCompletedTasks() = taskDao.getCompletedTasks()
    fun getAllTasks() = taskDao.getAllTasks()
    fun getTasksForClass(classId: Long) = taskDao.getTasksForClass(classId)
    suspend fun getTasksForDateSync(date: Long) = taskDao.getTasksForDateSync(date)
    suspend fun getTaskById(taskId: Long) = taskDao.getTaskById(taskId)
    suspend fun insertTask(task: TaskEntity) = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) = taskDao.setTaskCompleted(taskId, completed)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(taskId: Long) = taskDao.deleteTaskById(taskId)
    suspend fun clearCompletedTasks() = taskDao.clearCompletedTasks()
    
    // Task-Class cross reference
    suspend fun insertTaskWithClasses(task: TaskEntity, classIds: List<Long>): Long {
        val taskId = taskDao.insertTask(task)
        if (classIds.isNotEmpty()) {
            val refs = classIds.map { com.example.additioapp.data.model.TaskClassCrossRef(taskId, it) }
            taskDao.insertTaskClassRefs(refs)
        }
        return taskId
    }
    
    suspend fun updateTaskWithClasses(task: TaskEntity, classIds: List<Long>) {
        taskDao.updateTask(task)
        taskDao.deleteTaskClassRefs(task.id)
        if (classIds.isNotEmpty()) {
            val refs = classIds.map { com.example.additioapp.data.model.TaskClassCrossRef(task.id, it) }
            taskDao.insertTaskClassRefs(refs)
        }
    }
    
    suspend fun getClassIdsForTask(taskId: Long): List<Long> = taskDao.getClassIdsForTask(taskId)
    fun getClassIdsForTaskLive(taskId: Long) = taskDao.getClassIdsForTaskLive(taskId)

    // Schedule Items
    fun getAllScheduleItems() = scheduleItemDao.getAllScheduleItems()
    suspend fun getAllScheduleItemsSync() = scheduleItemDao.getAllScheduleItemsSync()
    
    // ScheduleItem-Class methods
    suspend fun insertScheduleItemWithClasses(item: ScheduleItemEntity, classIds: List<Long>): Long {
        val insertedId = scheduleItemDao.insertScheduleItem(item)
        
        // Insert class references
        if (classIds.isNotEmpty()) {
            val refs = classIds.map { classId -> 
                com.example.additioapp.data.model.ScheduleItemClassCrossRef(insertedId, classId) 
            }
            scheduleItemDao.insertScheduleItemClassRefs(refs)
        }
        
        return insertedId
    }
    
    suspend fun updateScheduleItemWithClasses(item: ScheduleItemEntity, classIds: List<Long>) {
        scheduleItemDao.updateScheduleItem(item)
        
        // Update class references (delete all and re-insert)
        scheduleItemDao.deleteScheduleItemClassRefs(item.id)
        if (classIds.isNotEmpty()) {
            val refs = classIds.map { classId -> 
                com.example.additioapp.data.model.ScheduleItemClassCrossRef(item.id, classId) 
            }
            scheduleItemDao.insertScheduleItemClassRefs(refs)
        }
    }
    
    suspend fun getClassIdsForScheduleItem(scheduleItemId: Long) = scheduleItemDao.getClassIdsForScheduleItem(scheduleItemId)

    fun getScheduleItemsForDay(dayOfWeek: Int) = scheduleItemDao.getScheduleItemsForDay(dayOfWeek)
    suspend fun getScheduleItemsForDaySync(dayOfWeek: Int) = scheduleItemDao.getScheduleItemsForDaySync(dayOfWeek)
    fun getScheduleItemsForClass(classId: Long) = scheduleItemDao.getScheduleItemsForClass(classId)
    suspend fun getScheduleItemById(id: Long) = scheduleItemDao.getScheduleItemById(id)
    suspend fun insertScheduleItem(item: ScheduleItemEntity) = scheduleItemDao.insertScheduleItem(item)
    suspend fun updateScheduleItem(item: ScheduleItemEntity) = scheduleItemDao.updateScheduleItem(item)
    suspend fun deleteScheduleItem(item: ScheduleItemEntity) = scheduleItemDao.deleteScheduleItem(item)
    suspend fun deleteScheduleItemById(id: Long) = scheduleItemDao.deleteScheduleItemById(id)
    suspend fun deleteScheduleItemsForClass(classId: Long) = scheduleItemDao.deleteScheduleItemsForClass(classId)
    
    // Sync methods for calendar
    suspend fun getPendingTasksSync() = taskDao.getPendingTasksSync()
    
    // Task statistics
    suspend fun getCompletedTaskCount() = taskDao.getCompletedTaskCount()
    suspend fun getTotalTaskCount() = taskDao.getTotalTaskCount()
    suspend fun getCompletedThisWeek(weekStart: Long) = taskDao.getCompletedThisWeek(weekStart)

    // Global Search
    suspend fun searchStudents(query: String): List<StudentEntity> {
        return studentDao.getAllStudentsSync().filter {
            it.displayNameFr.contains(query, ignoreCase = true) ||
            it.displayNameAr?.contains(query, ignoreCase = true) == true ||
            it.displayMatricule.contains(query, ignoreCase = true) ||
            it.email?.contains(query, ignoreCase = true) == true
        }
    }
    
    suspend fun searchClasses(query: String): List<ClassEntity> {
        return classDao.getAllClassesSync().filter {
            it.name.contains(query, ignoreCase = true) ||
            it.year.contains(query, ignoreCase = true)
        }
    }
    
    suspend fun searchEvents(query: String): List<EventEntity> {
        return eventDao.getAllEventsSync().filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }
    
    suspend fun searchTasks(query: String): List<TaskEntity> {
        return taskDao.getAllTasksSync().filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }
    
    // Report Card Generation - TODO: Complete implementation with correct DAO methods
    /*
    suspend fun generateStudentReportCard(studentId: Long, classId: Long): com.example.additioapp.data.model.StudentReportCard? {
        // Implementation commented out - needs correct DAO methods
        return null
    }
    */
    
    // ===== Teacher Absences =====
    val allAbsences: Flow<List<TeacherAbsenceEntity>> = teacherAbsenceDao.getAllAbsences()
    val pendingAbsences: Flow<List<TeacherAbsenceEntity>> = teacherAbsenceDao.getPendingAbsences()
    val scheduledAbsences: Flow<List<TeacherAbsenceEntity>> = teacherAbsenceDao.getScheduledAbsences()
    val pendingCount: Flow<Int> = teacherAbsenceDao.getPendingCount()
    
    fun getAbsencesForClass(classId: Long): Flow<List<TeacherAbsenceEntity>> = 
        teacherAbsenceDao.getAbsencesForClass(classId)
    
    fun getAbsencesByStatus(status: String): Flow<List<TeacherAbsenceEntity>> = 
        teacherAbsenceDao.getAbsencesByStatus(status)
    
    suspend fun insertAbsence(absence: TeacherAbsenceEntity): Long = 
        teacherAbsenceDao.insert(absence)
    
    suspend fun updateAbsence(absence: TeacherAbsenceEntity) = 
        teacherAbsenceDao.update(absence)
    
    suspend fun deleteAbsence(absence: TeacherAbsenceEntity) = 
        teacherAbsenceDao.delete(absence)
    
    suspend fun deleteAbsenceById(id: Long) = 
        teacherAbsenceDao.deleteById(id)
    
    suspend fun getAbsenceById(id: Long): TeacherAbsenceEntity? = 
        teacherAbsenceDao.getAbsenceById(id)
    
    suspend fun scheduleReplacement(id: Long, replacementDate: Long) = 
        teacherAbsenceDao.scheduleReplacement(id, replacementDate)
    
    suspend fun markAbsenceCompleted(id: Long) = 
        teacherAbsenceDao.markCompleted(id)
    
    suspend fun updateAbsenceStatus(id: Long, status: String) = 
        teacherAbsenceDao.updateStatus(id, status)
    
    // For backup/restore
    suspend fun getAllAbsencesSync(): List<TeacherAbsenceEntity> = 
        teacherAbsenceDao.getAllAbsencesSync()
    
    suspend fun insertAllAbsences(absences: List<TeacherAbsenceEntity>) = 
        teacherAbsenceDao.insertAll(absences)
    
    suspend fun deleteAllAbsences() = 
        teacherAbsenceDao.deleteAll()

    // Query for replacements within a date range (for Widget and Home)
    suspend fun getReplacementsForDateRangeSync(startOfDay: Long, endOfDay: Long): List<TeacherAbsenceEntity> {
        return teacherAbsenceDao.getAllAbsencesSync()
            .filter { absence ->
                absence.status != "COMPLETED" &&
                absence.replacementDate != null &&
                absence.replacementDate >= startOfDay &&
                absence.replacementDate <= endOfDay
            }
            .sortedBy { it.replacementDate }
    }
}
