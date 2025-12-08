package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.additioapp.data.model.SessionEntity

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE classId = :classId ORDER BY date ASC")
    fun getSessionsForClass(classId: Long): LiveData<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessionsSync(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE classId = :classId AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getSessionsInRange(classId: Long, start: Long, end: Long): LiveData<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE classId = :classId AND date = :date LIMIT 1")
    suspend fun getSessionByDate(classId: Long, date: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("SELECT MIN(date) FROM sessions WHERE classId = :classId")
    suspend fun getOldestSessionDate(classId: Long): Long?
    
    @Query("SELECT COUNT(*) FROM sessions WHERE classId = :classId AND type = :type")
    suspend fun getSessionCountByType(classId: Long, type: String): Int
}
