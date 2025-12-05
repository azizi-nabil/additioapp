package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.additioapp.data.model.ScheduleItemEntity

@Dao
interface ScheduleItemDao {
    @Query("SELECT * FROM schedule_items ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllScheduleItems(): LiveData<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllScheduleItemsSync(): List<ScheduleItemEntity>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getScheduleItemsForDay(dayOfWeek: Int): LiveData<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getScheduleItemsForDaySync(dayOfWeek: Int): List<ScheduleItemEntity>

    @Query("SELECT * FROM schedule_items WHERE classId = :classId ORDER BY dayOfWeek ASC, startTime ASC")
    fun getScheduleItemsForClass(classId: Long): LiveData<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items WHERE id = :id")
    suspend fun getScheduleItemById(id: Long): ScheduleItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItem(item: ScheduleItemEntity): Long

    @Update
    suspend fun updateScheduleItem(item: ScheduleItemEntity)

    @Delete
    suspend fun deleteScheduleItem(item: ScheduleItemEntity)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteScheduleItemById(id: Long)

    @Query("DELETE FROM schedule_items WHERE classId = :classId")
    suspend fun deleteScheduleItemsForClass(classId: Long)
}
