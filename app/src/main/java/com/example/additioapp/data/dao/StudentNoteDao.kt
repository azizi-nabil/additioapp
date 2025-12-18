package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.additioapp.data.model.StudentNoteEntity

@Dao
interface StudentNoteDao {
    
    @Query("SELECT * FROM student_notes WHERE studentId = :studentId ORDER BY date DESC")
    fun getNotesForStudent(studentId: Long): LiveData<List<StudentNoteEntity>>
    
    @Query("SELECT * FROM student_notes WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getNotesForStudentSync(studentId: Long): List<StudentNoteEntity>
    
    @Query("SELECT COUNT(*) FROM student_notes WHERE studentId = :studentId")
    suspend fun getNotesCountForStudent(studentId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: StudentNoteEntity): Long
    
    @Update
    suspend fun update(note: StudentNoteEntity)
    
    @Delete
    suspend fun delete(note: StudentNoteEntity)
    
    @Query("DELETE FROM student_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
    
    // Backup/Restore
    @Query("SELECT * FROM student_notes")
    suspend fun getAllNotesSync(): List<StudentNoteEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<StudentNoteEntity>)
}
