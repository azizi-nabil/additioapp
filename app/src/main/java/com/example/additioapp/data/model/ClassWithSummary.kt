package com.example.additioapp.data.model

import androidx.room.Embedded

data class ClassWithSummary(
    @Embedded val classEntity: ClassEntity,
    val studentCount: Int
)
