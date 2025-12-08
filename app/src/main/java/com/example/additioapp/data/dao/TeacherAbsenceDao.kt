package com.example.additioapp.data.dao

import androidx.room.*
import com.example.additioapp.data.model.TeacherAbsenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherAbsenceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(absence: TeacherAbsenceEntity): Long
    
    @Update
    suspend fun update(absence: TeacherAbsenceEntity)
    
    @Delete
    suspend fun delete(absence: TeacherAbsenceEntity)
    
    @Query("DELETE FROM teacher_absences WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM teacher_absences ORDER BY absenceDate DESC")
    fun getAllAbsences(): Flow<List<TeacherAbsenceEntity>>
    
    @Query("SELECT * FROM teacher_absences WHERE status = :status ORDER BY absenceDate DESC")
    fun getAbsencesByStatus(status: String): Flow<List<TeacherAbsenceEntity>>
    
    @Query("SELECT * FROM teacher_absences WHERE status = 'PENDING' ORDER BY absenceDate ASC")
    fun getPendingAbsences(): Flow<List<TeacherAbsenceEntity>>
    
    @Query("SELECT * FROM teacher_absences WHERE status = 'SCHEDULED' ORDER BY replacementDate ASC")
    fun getScheduledAbsences(): Flow<List<TeacherAbsenceEntity>>
    
    @Query("SELECT * FROM teacher_absences WHERE classId = :classId ORDER BY absenceDate DESC")
    fun getAbsencesForClass(classId: Long): Flow<List<TeacherAbsenceEntity>>
    
    @Query("SELECT * FROM teacher_absences WHERE id = :id")
    suspend fun getAbsenceById(id: Long): TeacherAbsenceEntity?
    
    @Query("UPDATE teacher_absences SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    
    @Query("UPDATE teacher_absences SET replacementDate = :replacementDate, status = 'SCHEDULED' WHERE id = :id")
    suspend fun scheduleReplacement(id: Long, replacementDate: Long)
    
    @Query("UPDATE teacher_absences SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markCompleted(id: Long)
    
    @Query("SELECT COUNT(*) FROM teacher_absences WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
    
    // For backup/restore
    @Query("SELECT * FROM teacher_absences")
    suspend fun getAllAbsencesSync(): List<TeacherAbsenceEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(absences: List<TeacherAbsenceEntity>)
    
    @Query("DELETE FROM teacher_absences")
    suspend fun deleteAll()
}
