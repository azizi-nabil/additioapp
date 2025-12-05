package com.example.additioapp.data.model

data class AttendanceRecordWithType(
    val studentId: Long,
    val status: String,
    val type: String // Cours, TD, TP
)
