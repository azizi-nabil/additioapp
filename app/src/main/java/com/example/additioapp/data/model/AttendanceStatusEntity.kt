package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_statuses")
data class AttendanceStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g., P, A, L, E, JA, JL
    val label: String,
    val countsAsPresent: Boolean = false,
    val colorHex: String? = null,
    val orderIndex: Int = 0
)
