package com.example.additioapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId"), Index("matricule")]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val matricule: String = "",              // Matricule number (e.g., "202034052475")
    val firstNameFr: String = "",            // First name in French
    val lastNameFr: String = "",             // Last name in French
    val firstNameAr: String? = null,         // First name in Arabic (optional)
    val lastNameAr: String? = null,          // Last name in Arabic (optional)
    val name: String = "",                   // Legacy field for compatibility (computed: lastNameFr + firstNameFr)
    val studentId: String = "",              // Legacy field, now use matricule
    val email: String? = null,
    val photoPath: String? = null,
    val notes: String? = null                // Private teacher notes
) {
    // Computed display names
    @Ignore
    val displayNameFr: String = if (lastNameFr.isNotEmpty() || firstNameFr.isNotEmpty()) {
        "$lastNameFr $firstNameFr".trim()
    } else {
        name // Fallback to legacy name
    }
    
    @Ignore
    val displayNameAr: String? = if (!lastNameAr.isNullOrEmpty() || !firstNameAr.isNullOrEmpty()) {
        "${lastNameAr ?: ""} ${firstNameAr ?: ""}".trim()
    } else null
    
    @Ignore
    val displayMatricule: String = matricule.ifEmpty { studentId }
    
    @Ignore
    val hasNotes: Boolean = !notes.isNullOrBlank()
}
