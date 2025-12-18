package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.additioapp.data.model.ClassNoteEntity

@Dao
interface ClassNoteDao {
    
    @Query("SELECT * FROM class_notes WHERE classId = :classId ORDER BY date DESC")
    fun getNotesForClass(classId: Long): LiveData<List<ClassNoteEntity>>
    
    @Query("SELECT * FROM class_notes WHERE classId = :classId ORDER BY date DESC")
    suspend fun getNotesForClassSync(classId: Long): List<ClassNoteEntity>
    
    @Query("SELECT COUNT(*) FROM class_notes WHERE classId = :classId")
    suspend fun getNotesCountForClass(classId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: ClassNoteEntity): Long
    
    @Update
    suspend fun update(note: ClassNoteEntity)
    
    @Delete
    suspend fun delete(note: ClassNoteEntity)
    
    @Query("DELETE FROM class_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
}
