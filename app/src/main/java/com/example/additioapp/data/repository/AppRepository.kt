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
import com.example.additioapp.data.model.UnitEntity

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
    private val scheduleItemDao: ScheduleItemDao
) {
    // Classes
    val allClasses: LiveData<List<ClassEntity>> = classDao.getAllClasses()
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
    fun getAttendanceForSession(sessionId: String) = attendanceDao.getAttendanceForSession(sessionId)
    
    // One-time load without LiveData
    suspend fun getAttendanceForSessionOnce(sessionId: String): List<AttendanceRecordEntity> {
        return attendanceDao.getAttendanceForSessionOnce(sessionId)
    }
    
    suspend fun getStudentsForClassOnce(classId: Long): List<StudentEntity> {
        return studentDao.getStudentsForClassSync(classId)
    }

    suspend fun getAllAttendanceForClassOnce(classId: Long): List<AttendanceRecordEntity> {
        return attendanceDao.getAllAttendanceForClassSync(classId)
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

    // Units
    fun getUnitsForClass(classId: Long): LiveData<List<UnitEntity>> = unitDao.getUnitsForClass(classId)
    suspend fun insertUnit(unit: UnitEntity) = unitDao.insertUnit(unit)
    suspend fun deleteUnit(unit: UnitEntity) = unitDao.deleteUnit(unit)
    // Backup & Restore
    suspend fun getAllData(): com.example.additioapp.data.model.BackupData {
        return com.example.additioapp.data.model.BackupData(
            classes = classDao.getAllClassesSync(),
            students = studentDao.getAllStudentsSync(),
            sessions = sessionDao.getAllSessionsSync(),
            attendanceRecords = attendanceDao.getAllAttendanceSync(),
            gradeItems = gradeDao.getAllGradeItemsSync(),
            gradeRecords = gradeDao.getAllGradeRecordsSync(),
            behaviorRecords = behaviorDao.getAllBehaviorsSync()
        )
    }

    suspend fun restoreData(data: com.example.additioapp.data.model.BackupData) {
        // Clear existing data? Or just upsert?
        // For a true restore, we should probably clear, but that's risky if the backup is partial.
        // Given the "BackupData" structure implies a full backup, we'll upsert everything.
        // Users can clear data manually if they want a clean slate.
        
        classDao.insertClasses(data.classes)
        studentDao.insertStudents(data.students)
        sessionDao.insertSessions(data.sessions)
        attendanceDao.insertAttendanceList(data.attendanceRecords)
        gradeDao.insertGradeItems(data.gradeItems)

        
        // Sanitize grade records: Ensure status is not null (for old backups)
        val sanitizedGrades = data.gradeRecords.map { record ->
            if (record.status == null) record.copy(status = "PRESENT") else record
        }
        gradeDao.insertGradeRecords(sanitizedGrades)
        behaviorDao.insertBehaviors(data.behaviorRecords)
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

    // Schedule Items
    fun getAllScheduleItems() = scheduleItemDao.getAllScheduleItems()
    suspend fun getAllScheduleItemsSync() = scheduleItemDao.getAllScheduleItemsSync()
    fun getScheduleItemsForDay(dayOfWeek: Int) = scheduleItemDao.getScheduleItemsForDay(dayOfWeek)
    suspend fun getScheduleItemsForDaySync(dayOfWeek: Int) = scheduleItemDao.getScheduleItemsForDaySync(dayOfWeek)
    fun getScheduleItemsForClass(classId: Long) = scheduleItemDao.getScheduleItemsForClass(classId)
    suspend fun getScheduleItemById(id: Long) = scheduleItemDao.getScheduleItemById(id)
    suspend fun insertScheduleItem(item: ScheduleItemEntity) = scheduleItemDao.insertScheduleItem(item)
    suspend fun updateScheduleItem(item: ScheduleItemEntity) = scheduleItemDao.updateScheduleItem(item)
    suspend fun deleteScheduleItem(item: ScheduleItemEntity) = scheduleItemDao.deleteScheduleItem(item)
    suspend fun deleteScheduleItemById(id: Long) = scheduleItemDao.deleteScheduleItemById(id)
    suspend fun deleteScheduleItemsForClass(classId: Long) = scheduleItemDao.deleteScheduleItemsForClass(classId)
}
