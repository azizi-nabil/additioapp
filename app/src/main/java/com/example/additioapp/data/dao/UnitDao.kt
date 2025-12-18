package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.additioapp.data.model.UnitEntity

@Dao
interface UnitDao {
    @Query("SELECT * FROM units WHERE classId = :classId ORDER BY startDate")
    fun getUnitsForClass(classId: Long): LiveData<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Delete
    suspend fun deleteUnit(unit: UnitEntity)
    
    // Backup/Restore
    @Query("SELECT * FROM units")
    suspend fun getAllUnitsSync(): List<UnitEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>)
}
