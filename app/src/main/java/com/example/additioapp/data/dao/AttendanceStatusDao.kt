package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.additioapp.data.model.AttendanceStatusEntity

@Dao
interface AttendanceStatusDao {
    @Query("SELECT * FROM attendance_statuses ORDER BY orderIndex ASC, id ASC")
    fun getAllStatuses(): LiveData<List<AttendanceStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: AttendanceStatusEntity): Long

    @Delete
    suspend fun deleteStatus(status: AttendanceStatusEntity)

    @Query("SELECT COUNT(*) FROM attendance_statuses")
    suspend fun getStatusCount(): Int

    @Query("SELECT * FROM attendance_statuses ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllStatusesSync(): List<AttendanceStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<AttendanceStatusEntity>)
}
