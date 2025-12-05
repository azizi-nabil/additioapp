package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.additioapp.data.model.EventEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC, startTime ASC")
    fun getAllEvents(): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events WHERE date = :date ORDER BY startTime ASC")
    fun getEventsForDate(date: Long): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events WHERE date = :date ORDER BY startTime ASC")
    suspend fun getEventsForDateSync(date: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getEventsInRange(startDate: Long, endDate: Long): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getEventsInRangeSync(startDate: Long, endDate: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE classId = :classId ORDER BY date ASC")
    fun getEventsForClass(classId: Long): LiveData<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @Query("SELECT DISTINCT date FROM events WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getDatesWithEvents(startDate: Long, endDate: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)
}
