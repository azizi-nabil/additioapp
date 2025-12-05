package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("classId"), Index("dueDate")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,               // Due date timestamp
    val classId: Long? = null,               // Optional link to class
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM",         // HIGH, MEDIUM, LOW
    val createdAt: Long = System.currentTimeMillis()
)
