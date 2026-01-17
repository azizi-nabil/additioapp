package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.additioapp.data.model.BehaviorTypeEntity

@Dao
interface BehaviorTypeDao {
    @Query("SELECT * FROM behavior_types WHERE classId = :classId ORDER BY isPositive DESC, name ASC")
    fun getBehaviorTypesForClass(classId: Long): LiveData<List<BehaviorTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorType(type: BehaviorTypeEntity): Long

    @Delete
    suspend fun deleteBehaviorType(type: BehaviorTypeEntity)

    @Query("SELECT * FROM behavior_types")
    suspend fun getAllBehaviorTypesSync(): List<BehaviorTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<BehaviorTypeEntity>)
}
