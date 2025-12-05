package com.example.additioapp.data.model

data class StudentAbsenceDetail(
    val date: Long,
    val type: String, // Cours, TD, TP
    val status: String // 'A' or 'E'
)
