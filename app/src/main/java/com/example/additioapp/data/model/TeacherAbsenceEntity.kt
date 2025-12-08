package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teacher_absences",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId"), Index("status"), Index("absenceDate")]
)
data class TeacherAbsenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,                    // Linked class
    val sessionType: String,              // "TD", "TP", "COURSE"
    val absenceDate: Long,                // Original session date missed
    val reason: String? = null,           // Optional reason
    val replacementDate: Long? = null,    // Scheduled replacement date
    val status: String = "PENDING",       // "PENDING", "SCHEDULED", "COMPLETED"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_TD = "TD"
        const val TYPE_TP = "TP"
        const val TYPE_COURSE = "COURSE"
        
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SCHEDULED = "SCHEDULED"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
