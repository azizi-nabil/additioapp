package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.additioapp.data.model.GradeItemGroupEntity

@Dao
interface GradeItemGroupDao {
    
    @Query("SELECT * FROM grade_item_groups WHERE gradeItemId = :gradeItemId ORDER BY groupNumber, studentId")
    fun getGroupsForGradeItem(gradeItemId: Long): LiveData<List<GradeItemGroupEntity>>
    
    @Query("SELECT * FROM grade_item_groups WHERE gradeItemId = :gradeItemId ORDER BY groupNumber, studentId")
    suspend fun getGroupsForGradeItemSync(gradeItemId: Long): List<GradeItemGroupEntity>
    
    @Query("SELECT * FROM grade_item_groups WHERE gradeItemId = :gradeItemId AND studentId = :studentId")
    suspend fun getGroupForStudent(gradeItemId: Long, studentId: Long): GradeItemGroupEntity?
    
    @Query("SELECT DISTINCT groupNumber FROM grade_item_groups WHERE gradeItemId = :gradeItemId ORDER BY groupNumber")
    suspend fun getGroupNumbersForItem(gradeItemId: Long): List<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GradeItemGroupEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<GradeItemGroupEntity>)
    
    @Delete
    suspend fun delete(entity: GradeItemGroupEntity)
    
    @Query("DELETE FROM grade_item_groups WHERE gradeItemId = :gradeItemId AND studentId = :studentId")
    suspend fun deleteByGradeItemAndStudent(gradeItemId: Long, studentId: Long)
    
    @Query("DELETE FROM grade_item_groups WHERE gradeItemId = :gradeItemId AND groupNumber = :groupNumber")
    suspend fun deleteGroup(gradeItemId: Long, groupNumber: Int)
    
    @Query("DELETE FROM grade_item_groups WHERE gradeItemId = :gradeItemId")
    suspend fun deleteAllForGradeItem(gradeItemId: Long)
    
    @Query("SELECT * FROM grade_item_groups")
    suspend fun getAllGroupsSync(): List<GradeItemGroupEntity>

    @Query("UPDATE grade_item_groups SET groupNumber = :groupNumber WHERE gradeItemId = :gradeItemId AND studentId = :studentId")
    suspend fun updateGroupNumber(gradeItemId: Long, studentId: Long, groupNumber: Int)
}
