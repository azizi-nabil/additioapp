package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_items",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId"), Index("dayOfWeek")]
)
data class ScheduleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,                       // Link to class (required)
    val dayOfWeek: Int,                      // 1 = Sunday, 2 = Monday, ..., 7 = Saturday
    val startTime: String,                   // "09:00" format
    val endTime: String,                     // "10:30" format
    val room: String = "",                   // Room/location
    val sessionType: String = "Cours"        // Cours, TD, TP
)
