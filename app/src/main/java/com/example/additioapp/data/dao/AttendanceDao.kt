package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.AttendanceSessionSummary
import com.example.additioapp.data.model.DuplicateInfo

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): LiveData<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceSync(): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY id DESC")
    fun getAttendanceForSession(sessionId: String): LiveData<List<AttendanceRecordEntity>>
    
    // One-time load without LiveData
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY id DESC")
    suspend fun getAttendanceForSessionOnce(sessionId: String): List<AttendanceRecordEntity>

    @Query("SELECT ar.* FROM attendance_records ar INNER JOIN students s ON ar.studentId = s.id WHERE s.classId = :classId")
    fun getAttendanceForClass(classId: Long): LiveData<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(attendanceList: List<AttendanceRecordEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId AND studentId = :studentId")
    suspend fun deleteAttendanceForStudent(sessionId: String, studentId: Long)

    @Transaction
    suspend fun replaceAttendance(attendance: AttendanceRecordEntity) {
        deleteAttendanceForStudent(attendance.sessionId, attendance.studentId)
        insertAttendance(attendance)
    }
    
    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("UPDATE attendance_records SET sessionId = :newSessionId WHERE sessionId = :oldSessionId")
    suspend fun updateSessionId(oldSessionId: String, newSessionId: String)

    @Query("DELETE FROM attendance_records WHERE studentId = :studentId AND sessionId = :sessionId")
    suspend fun deleteAttendance(studentId: Long, sessionId: String)
    
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND sessionId = :sessionId LIMIT 1")
    suspend fun getAttendanceForStudentAndSession(studentId: Long, sessionId: String): AttendanceRecordEntity?
    
    // Find duplicate sessions by date (same classId, date, type pattern)
    @Query("""
        SELECT sessionId, date, COUNT(*) as cnt FROM attendance_records 
        WHERE sessionId LIKE :pattern
        GROUP BY date
        HAVING COUNT(DISTINCT sessionId) > 1
    """)
    suspend fun findDuplicateDatePatterns(pattern: String): List<DuplicateInfo>
    
    // Get all unique sessionIds for a date pattern
    @Query("SELECT DISTINCT sessionId FROM attendance_records WHERE sessionId LIKE :pattern")
    suspend fun getSessionIdsForPattern(pattern: String): List<String>
    
    // Delete records by id
    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT 
            ar.sessionId AS sessionId,
            MIN(ar.date) AS date,
            SUM(CASE WHEN ar.status = 'P' THEN 1 ELSE 0 END) AS presentCount,
            SUM(CASE WHEN ar.status = 'A' THEN 1 ELSE 0 END) AS absentCount,
            SUM(CASE WHEN ar.status = 'L' THEN 1 ELSE 0 END) AS lateCount,
            SUM(CASE WHEN ar.status = 'E' THEN 1 ELSE 0 END) AS excusedCount,
            COUNT(*) AS totalCount,
            COALESCE(ses.type, 'Cours') AS type
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE s.classId = :classId
          AND ar.date BETWEEN :startDate AND :endDate
        GROUP BY ar.sessionId
        ORDER BY date DESC
    """)
    fun getSessionSummaries(
        classId: Long,
        startDate: Long,
        endDate: Long
    ): LiveData<List<AttendanceSessionSummary>>

    @Query("""
        SELECT 
            ar.studentId,
            ar.status,
            COALESCE(ses.type, 'Cours') AS type
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE s.classId = :classId
    """)
    fun getAttendanceWithTypeForClass(classId: Long): LiveData<List<com.example.additioapp.data.model.AttendanceRecordWithType>>

    @Query("""
        SELECT 
            ar.studentId,
            ar.status,
            COALESCE(ses.type, 'Cours') AS type
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE s.classId = :classId
    """)
    suspend fun getAttendanceWithTypeForClassSync(classId: Long): List<com.example.additioapp.data.model.AttendanceRecordWithType>

    @Query("""
        SELECT 
            ar.date,
            COALESCE(ses.type, CASE 
                WHEN ar.sessionId LIKE '%_TD' THEN 'TD'
                WHEN ar.sessionId LIKE '%_TP' THEN 'TP'
                ELSE 'Cours' 
            END) AS type,
            ar.status
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE ar.studentId = :studentId AND ar.status IN ('A', 'E')
        ORDER BY ar.date DESC
    """)
    fun getAbsencesForStudent(studentId: Long): LiveData<List<com.example.additioapp.data.model.StudentAbsenceDetail>>

    @Query("""
        SELECT 
            ar.date,
            COALESCE(ses.type, 'Cours') AS type,
            ar.status
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE ar.studentId = :studentId AND s.classId = :classId AND ar.status = 'P' 
        AND (
            (ses.type = 'Cours' OR ses.type IS NULL)
            AND ar.sessionId NOT LIKE '%_TD'
            AND ar.sessionId NOT LIKE '%_TP'
        )
        ORDER BY ar.date DESC
    """)
    fun getCoursPresenceForStudent(studentId: Long, classId: Long): LiveData<List<com.example.additioapp.data.model.StudentAbsenceDetail>>

    @Query("""
        SELECT ar.* 
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        WHERE s.classId = :classId
    """)
    suspend fun getAllAttendanceForClassSync(classId: Long): List<AttendanceRecordEntity>

    @Query("""
        SELECT COUNT(DISTINCT ar.date)
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE s.classId = :classId 
        AND (
            (ses.type = 'Cours' OR ses.type IS NULL) 
            AND ar.sessionId NOT LIKE '%_TD' 
            AND ar.sessionId NOT LIKE '%_TP'
        )
    """)
    suspend fun getTotalCoursSessionCount(classId: Long): Int

    @Query("""
        SELECT COUNT(DISTINCT ar.date)
        FROM attendance_records ar
        INNER JOIN students s ON ar.studentId = s.id
        LEFT JOIN sessions ses ON ses.classId = s.classId AND ses.date = ar.date
        WHERE s.classId = :classId 
        AND (
            ses.type = :type 
            OR (ses.type IS NULL AND ar.sessionId LIKE '%_' || :type)
        )
    """)
    suspend fun getTotalSessionCountByType(classId: Long, type: String): Int
    
    // Check for any records matching a sessionId pattern (for duplicate detection)
    @Query("SELECT COUNT(*) FROM attendance_records WHERE sessionId LIKE :sessionIdPattern")
    suspend fun countRecordsBySessionIdPattern(sessionIdPattern: String): Int
}
