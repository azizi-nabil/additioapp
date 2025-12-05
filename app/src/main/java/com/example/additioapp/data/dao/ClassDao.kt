package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.additioapp.data.model.ClassEntity

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllClasses(): LiveData<List<ClassEntity>>

    @Query("""
        SELECT c.*, COUNT(s.id) as studentCount 
        FROM classes c 
        LEFT JOIN students s ON c.id = s.classId 
        WHERE c.isArchived = 0 
        GROUP BY c.id 
        ORDER BY c.name ASC
    """)
    fun getAllClassesWithSummary(): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>>

    @Query("SELECT DISTINCT year FROM classes ORDER BY year DESC")
    fun getDistinctYears(): LiveData<List<String>>

    @Query("SELECT * FROM classes WHERE isArchived = 0 AND semester = :semester AND year = :year ORDER BY name ASC")
    fun getClassesBySemesterAndYear(semester: String, year: String): LiveData<List<ClassEntity>>

    @Query("""
        SELECT c.*, COUNT(s.id) as studentCount 
        FROM classes c 
        LEFT JOIN students s ON c.id = s.classId 
        WHERE c.isArchived = 0 AND c.semester = :semester AND c.year = :year 
        GROUP BY c.id 
        ORDER BY c.name ASC
    """)
    fun getClassesWithSummaryBySemesterAndYear(semester: String, year: String): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>>

    @Query("SELECT * FROM classes WHERE isArchived = 1 AND year = :year ORDER BY name ASC")
    fun getArchivedClassesByYear(year: String): LiveData<List<ClassEntity>>

    @Query("""
        SELECT c.*, COUNT(s.id) as studentCount 
        FROM classes c 
        LEFT JOIN students s ON c.id = s.classId 
        WHERE c.isArchived = 1 AND c.year = :year 
        GROUP BY c.id 
        ORDER BY c.name ASC
    """)
    fun getArchivedClassesWithSummaryByYear(year: String): LiveData<List<com.example.additioapp.data.model.ClassWithSummary>>

    @Query("SELECT * FROM classes ORDER BY name ASC")
    suspend fun getAllClassesSync(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: Long): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassEntity>)

    @Update
    suspend fun updateClass(classEntity: ClassEntity)

    @Delete
    suspend fun deleteClass(classEntity: ClassEntity)
}
