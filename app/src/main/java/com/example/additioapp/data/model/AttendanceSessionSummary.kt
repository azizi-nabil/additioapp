package com.example.additioapp.data.model

data class AttendanceSessionSummary(
    val sessionId: String,
    val date: Long,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val otherCount: Int,
    val totalCount: Int,
    val type: String = "Cours"
)
