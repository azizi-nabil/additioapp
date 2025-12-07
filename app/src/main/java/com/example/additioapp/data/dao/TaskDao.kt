package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.additioapp.data.model.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    fun getPendingTasks(): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY dueDate DESC LIMIT 20")
    fun getCompletedTasks(): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueDate ASC")
    fun getAllTasks(): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE classId = :classId ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksForClass(classId: Long): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isCompleted = 0 ORDER BY priority DESC")
    suspend fun getTasksForDateSync(date: Long): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    suspend fun getPendingTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun clearCompletedTasks()
    
    // Task-Class cross reference methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskClassRef(ref: com.example.additioapp.data.model.TaskClassCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskClassRefs(refs: List<com.example.additioapp.data.model.TaskClassCrossRef>)
    
    @Query("DELETE FROM task_class_cross_ref WHERE taskId = :taskId")
    suspend fun deleteTaskClassRefs(taskId: Long)
    
    @Query("SELECT classId FROM task_class_cross_ref WHERE taskId = :taskId")
    suspend fun getClassIdsForTask(taskId: Long): List<Long>
    
    @Query("SELECT classId FROM task_class_cross_ref WHERE taskId = :taskId")
    fun getClassIdsForTaskLive(taskId: Long): LiveData<List<Long>>
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTaskCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND createdAt >= :weekStart")
    suspend fun getCompletedThisWeek(weekStart: Long): Int

    // Backup & Restore
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM task_class_cross_ref")
    suspend fun getAllTaskClassRefs(): List<com.example.additioapp.data.model.TaskClassCrossRef>
}
