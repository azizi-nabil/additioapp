package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teacher_absences",
    indices = [Index("status"), Index("absenceDate")]
)
data class TeacherAbsenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classIds: String,                  // Comma-separated class IDs (e.g., "1,2,3")
    val sessionType: String,               // "TD", "TP", "COURSE"
    val absenceDate: Long,                 // Original session date missed
    val reason: String? = null,            // Optional reason
    val replacementDate: Long? = null,     // Scheduled replacement date
    val status: String = "PENDING",        // "PENDING", "SCHEDULED", "COMPLETED"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Helper function to get list of class IDs
    fun getClassIdList(): List<Long> = classIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    
    companion object {
        const val TYPE_TD = "TD"
        const val TYPE_TP = "TP"
        const val TYPE_COURSE = "COURSE"
        
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SCHEDULED = "SCHEDULED"
        const val STATUS_COMPLETED = "COMPLETED"
        
        // Helper to create comma-separated string from list
        fun createClassIdsString(ids: List<Long>): String = ids.joinToString(",")
    }
}
