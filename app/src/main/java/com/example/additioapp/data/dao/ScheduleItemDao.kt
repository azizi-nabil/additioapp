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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItems(items: List<ScheduleItemEntity>)

    @Update
    suspend fun updateScheduleItem(item: ScheduleItemEntity)

    @Delete
    suspend fun deleteScheduleItem(item: ScheduleItemEntity)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteScheduleItemById(id: Long)

    @Query("DELETE FROM schedule_items WHERE classId = :classId")
    suspend fun deleteScheduleItemsForClass(classId: Long)
    
    // ScheduleItem-Class cross reference methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItemClassRef(ref: com.example.additioapp.data.model.ScheduleItemClassCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItemClassRefs(refs: List<com.example.additioapp.data.model.ScheduleItemClassCrossRef>)
    
    @Query("DELETE FROM schedule_item_class_cross_ref WHERE scheduleItemId = :scheduleItemId")
    suspend fun deleteScheduleItemClassRefs(scheduleItemId: Long)
    
    @Query("SELECT classId FROM schedule_item_class_cross_ref WHERE scheduleItemId = :scheduleItemId")
    suspend fun getClassIdsForScheduleItem(scheduleItemId: Long): List<Long>
    
    @Query("SELECT classId FROM schedule_item_class_cross_ref WHERE scheduleItemId = :scheduleItemId")
    fun getClassIdsForScheduleItemLive(scheduleItemId: Long): LiveData<List<Long>>

    // Backup & Restore
    @Query("SELECT * FROM schedule_item_class_cross_ref")
    suspend fun getAllScheduleItemClassRefs(): List<com.example.additioapp.data.model.ScheduleItemClassCrossRef>
}
