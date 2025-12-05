package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grade_items",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId")]
)
data class GradeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val categoryId: Long? = null,
    val name: String,
    val category: String, // "Exam", "Homework", etc.
    val maxScore: Float,
    val gradingType: String = "NUMERIC", // NUMERIC, LETTER, PASS_FAIL, RUBRIC, CUSTOM_SCALE
    val weight: Float = 1.0f, // Default weight 1.0 (100% if single item, or relative weight)
    val date: Long = System.currentTimeMillis(),
    val formula: String? = null // e.g. "max([Item1], [Item2])"
)
