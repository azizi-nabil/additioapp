package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("classId"), Index("date")]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long,                          // Date as timestamp (start of day)
    val startTime: String? = null,           // "09:00" format
    val endTime: String? = null,             // "10:30" format
    val classId: Long? = null,               // Optional link to class
    val eventType: String = "OTHER",         // EXAM, MEETING, DEADLINE, OTHER
    val reminderMinutes: Int? = null,        // 15, 30, 60, 1440 (1 day), null = no reminder
    val isAllDay: Boolean = false,
    val color: String? = null,               // Custom color, or use class color if linked
    val recurrenceType: String = "NONE",     // NONE, DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY
    val recurrenceEndDate: Long? = null,     // End date for recurrence (null = forever)
    val parentEventId: Long? = null          // Links to original event for recurring instances
)
