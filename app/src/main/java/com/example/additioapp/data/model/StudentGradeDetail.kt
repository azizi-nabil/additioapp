package com.example.additioapp.data.model

data class StudentGradeDetail(
    val gradeName: String,
    val score: Double,
    val maxScore: Double,
    val weight: Double,
    val date: Long,
    val isCalculated: Boolean,
    val category: String
)
