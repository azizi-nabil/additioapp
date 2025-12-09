package com.example.additioapp.data.model

/**
 * Data class for duplicate session detection query results
 */
data class DuplicateInfo(
    val sessionId: String,
    val date: Long,
    val cnt: Int
)
