package com.example.additioapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.additioapp.data.model.GradeCategoryEntity

@Dao
interface GradeCategoryDao {
    @Query("SELECT * FROM grade_categories WHERE classId = :classId ORDER BY id ASC")
    fun getCategoriesForClass(classId: Long): LiveData<List<GradeCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: GradeCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: GradeCategoryEntity)

    @Query("SELECT * FROM grade_categories")
    suspend fun getAllCategoriesSync(): List<GradeCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<GradeCategoryEntity>)
}
