package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grade_records",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GradeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["gradeItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("studentId"),
        Index("gradeItemId"),
        Index(value = ["studentId", "gradeItemId"], unique = true)
    ]
)
data class GradeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val gradeItemId: Long,
    val score: Float,
    val comment: String? = null,
    val status: String = "PRESENT" // PRESENT, ABSENT, MISSING, EXCUSED
)
