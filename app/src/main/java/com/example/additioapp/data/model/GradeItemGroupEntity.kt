package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grade_item_groups",
    foreignKeys = [
        ForeignKey(
            entity = GradeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["gradeItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gradeItemId"]),
        Index(value = ["studentId"]),
        Index(value = ["gradeItemId", "studentId"], unique = true)
    ]
)
data class GradeItemGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gradeItemId: Long,
    val groupNumber: Int = 1,  // Group 1, 2, 3, etc.
    val studentId: Long
)
