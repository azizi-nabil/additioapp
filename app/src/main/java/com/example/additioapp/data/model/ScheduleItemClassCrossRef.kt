package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "schedule_item_class_cross_ref",
    primaryKeys = ["scheduleItemId", "classId"],
    foreignKeys = [
        ForeignKey(
            entity = ScheduleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleItemId"), Index("classId")]
)
data class ScheduleItemClassCrossRef(
    val scheduleItemId: Long,
    val classId: Long
)
