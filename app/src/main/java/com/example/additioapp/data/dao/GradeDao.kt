package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity

@Dao
interface GradeDao {
    // Grade Items
    @Query("SELECT * FROM grade_items ORDER BY date DESC")
    fun getAllGradeItems(): LiveData<List<GradeItemEntity>>

    @Query("SELECT * FROM grade_items")
    suspend fun getAllGradeItemsSync(): List<GradeItemEntity>

    @Query("SELECT * FROM grade_items WHERE classId = :classId ORDER BY date DESC")
    fun getGradeItemsForClass(classId: Long): LiveData<List<GradeItemEntity>>

    @Query("SELECT * FROM grade_items WHERE id = :id")
    fun getGradeItemById(id: Long): LiveData<GradeItemEntity>

    @Query("SELECT * FROM grade_items WHERE classId = :classId ORDER BY date DESC")
    suspend fun getGradeItemsForClassSync(classId: Long): List<GradeItemEntity>

    @Insert
    suspend fun insertGradeItem(gradeItem: GradeItemEntity): Long

    @Update
    suspend fun updateGradeItem(gradeItem: GradeItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeItems(gradeItems: List<GradeItemEntity>)

    @Delete
    suspend fun deleteGradeItem(gradeItem: GradeItemEntity)

    // Grade Records
    @Query("SELECT * FROM grade_records WHERE studentId = :studentId")
    fun getGradesForStudent(studentId: Long): LiveData<List<GradeRecordEntity>>

    @Query("SELECT * FROM grade_records WHERE gradeItemId = :itemId")
    fun getGradesForItem(itemId: Long): LiveData<List<GradeRecordEntity>>

    @Query("SELECT gr.* FROM grade_records gr INNER JOIN grade_items gi ON gr.gradeItemId = gi.id WHERE gi.classId = :classId")
    fun getGradeRecordsForClass(classId: Long): LiveData<List<GradeRecordEntity>>

    @Query("SELECT * FROM grade_records")
    suspend fun getAllGradeRecordsSync(): List<GradeRecordEntity>

    @Query("SELECT gr.* FROM grade_records gr INNER JOIN grade_items gi ON gr.gradeItemId = gi.id WHERE gi.classId = :classId")
    suspend fun getGradeRecordsForClassSync(classId: Long): List<GradeRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeRecord(record: GradeRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeRecords(records: List<GradeRecordEntity>)

    @Update
    suspend fun updateGradeRecord(gradeRecord: GradeRecordEntity)

    @Query("""
        SELECT 
            gi.name as gradeName,
            gr.score,
            gi.maxScore,
            gi.weight,
            gi.date,
            (gi.formula IS NOT NULL) as isCalculated,
            gi.category
        FROM grade_records gr
        INNER JOIN grade_items gi ON gr.gradeItemId = gi.id
        WHERE gr.studentId = :studentId
        GROUP BY gi.id
        ORDER BY gi.date DESC
    """)
    fun getStudentGradeDetails(studentId: Long): LiveData<List<com.example.additioapp.data.model.StudentGradeDetail>>
}
