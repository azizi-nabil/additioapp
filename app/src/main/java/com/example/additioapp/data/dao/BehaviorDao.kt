package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.additioapp.data.model.BehaviorRecordEntity

@Dao
interface BehaviorDao {
    @Query("SELECT * FROM behavior_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getBehaviorForStudent(studentId: Long): LiveData<List<BehaviorRecordEntity>>

    @Query("SELECT * FROM behavior_records WHERE classId = :classId")
    fun getBehaviorsForClass(classId: Long): LiveData<List<BehaviorRecordEntity>>

    @Query("SELECT * FROM behavior_records")
    suspend fun getAllBehaviorsSync(): List<BehaviorRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehavior(behavior: BehaviorRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviors(behaviors: List<BehaviorRecordEntity>)

    @Update
    suspend fun updateBehavior(behavior: BehaviorRecordEntity)

    @Delete
    suspend fun deleteBehavior(behavior: BehaviorRecordEntity)
}
