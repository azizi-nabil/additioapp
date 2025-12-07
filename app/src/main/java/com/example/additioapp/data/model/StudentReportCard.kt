package com.example.additioapp.data.model

data class StudentReportCard(
    val student: StudentEntity,
    val classInfo: ClassEntity,
    val gradesSummary: GradesSummary,
    val attendanceSummary: AttendanceSummary,
    val behaviorSummary: BehaviorSummary,
    val generatedDate: Long = System.currentTimeMillis()
)

data class GradesSummary(
    val gradesByCategory: Map<String, CategoryGrades>,
    val overallAverage: Double,
    val totalPoints: Double,
    val maxPoints: Double
)

data class CategoryGrades(
    val categoryName: String,
    val average: Double,
    val weight: Double,
    val grades: List<GradeDetail>
)

data class GradeDetail(
    val itemName: String,
    val score: Double,
    val maxScore: Double,
    val date: Long
)

data class AttendanceSummary(
    val totalSessions: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val attendanceRate: Double,
    val absentDates: List<Long>
)

data class BehaviorSummary(
    val totalPoints: Int,
    val positiveCount: Int,
    val negativeCount: Int,
    val recentBehaviors: List<BehaviorDetail>
)

data class BehaviorDetail(
    val typeName: String,
    val points: Int,
    val date: Long,
    val notes: String?
)
