package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classes",
    indices = [Index("year"), Index("isArchived")]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val year: String,
    val location: String,
    val schedule: String, // JSON or simple string for now
    val semester: String = "Semester 1",
    val isArchived: Boolean = false,
    val color: String = "#2196F3" // Default blue color for class
)
