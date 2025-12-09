package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.repository.AppRepository
import kotlinx.coroutines.launch

class GradeViewModel(private val repository: AppRepository) : ViewModel() {

    // Grade Items
    fun getAllGradeItems(): LiveData<List<GradeItemEntity>> {
        return repository.getAllGradeItems()
    }

    fun getGradeItemsForClass(classId: Long): LiveData<List<GradeItemEntity>> {
        return repository.getGradeItemsForClass(classId)
    }

    fun getGradeItemById(id: Long): LiveData<GradeItemEntity> {
        return repository.getGradeItemById(id)
    }

    fun insertGradeItem(item: GradeItemEntity) = viewModelScope.launch {
        repository.insertGradeItem(item)
        recalculateGrades(item.classId)
    }

    fun deleteGradeItem(item: GradeItemEntity) = viewModelScope.launch {
        repository.deleteGradeItem(item)
    }

    // Grade Records
    fun getGradesForStudent(studentId: Long): LiveData<List<GradeRecordEntity>> {
        return repository.getGradesForStudent(studentId)
    }

    fun getGradesForItem(itemId: Long): LiveData<List<GradeRecordEntity>> {
        return repository.getGradesForItem(itemId)
    }

    fun getGradeRecordsForClass(classId: Long): LiveData<List<GradeRecordEntity>> {
        return repository.getGradeRecordsForClass(classId)
    }

    fun getStudentGradeDetails(studentId: Long): LiveData<List<com.example.additioapp.data.model.StudentGradeDetail>> {
        return repository.getStudentGradeDetails(studentId)
    }

    fun insertGradeRecord(record: GradeRecordEntity) = viewModelScope.launch {
        repository.insertGradeRecord(record)
        // Trigger recalculation for the class (we need classId, but record doesn't have it directly)
        // We can get it from the item. This is expensive to do for every insert.
        // Optimization: Pass classId to this function?
        // For now, let's look up the item to get the classId.
        val items = repository.getAllGradeItems().value // This might be null or empty if not observed
        // Better: Fetch item by ID.
        // Let's assume the caller knows the classId or we fetch it.
        // Actually, let's just add a separate method `recalculateGrades(classId)` and call it from UI.
    }

    fun updateGradeRecord(record: GradeRecordEntity) = viewModelScope.launch {
        repository.updateGradeRecord(record)
    }

    fun saveGradeAndRecalculate(record: GradeRecordEntity, classId: Long) = viewModelScope.launch {
        val existing = repository.getGradesForItem(record.gradeItemId).value?.find { it.studentId == record.studentId }
        if (existing != null) {
             repository.updateGradeRecord(record.copy(id = existing.id))
        } else {
             repository.insertGradeRecord(record)
        }
        recalculateGrades(classId)
    }

    // Helper to calculate weighted average for a student in a class
    fun calculateWeightedAverage(items: List<GradeItemEntity>, records: List<GradeRecordEntity>): Float {
        if (items.isEmpty()) return 0f
        
        var totalWeightedScore = 0f
        var totalWeight = 0f
        
        val recordsMap = records.associateBy { it.gradeItemId }
        
        for (item in items) {
            // Skip calculated items for weighted average to avoid double counting?
            // Or should we include them? Usually calculated items are summaries.
            // For now, let's include them if they have a numeric score.
            
            val record = recordsMap[item.id]
            if (record != null) {
                val scorePct = (record.score / item.maxScore) * 100
                totalWeightedScore += scorePct * item.weight
                totalWeight += item.weight
            }
        }
        
        return if (totalWeight > 0) totalWeightedScore / totalWeight else 0f
    }

    fun recalculateGrades(classId: Long) = viewModelScope.launch {
        val items = repository.getGradeItemsForClassSync(classId)
        val records = repository.getGradeRecordsForClassSync(classId)
        
        val calculatedItems = items.filter { !it.formula.isNullOrEmpty() }
        if (calculatedItems.isEmpty()) return@launch

        // Fetch attendance data for the class
        val attendanceRecords = repository.getAttendanceWithTypeForClassSync(classId)
        val attendanceByStudent = attendanceRecords.groupBy { it.studentId }
        
        // Get total session counts
        val totalTD = repository.getTotalSessionCountByType(classId, "TD")
        val totalTP = repository.getTotalSessionCountByType(classId, "TP")
        val totalCours = repository.getTotalCoursSessionCount(classId)

        // Group records by student
        val recordsByStudent = records.groupBy { it.studentId }
        
        // Get all student IDs (we might need a better way to get ALL students if some have no grades)
        // For now, we only update students who have at least one grade record or if we fetch all students.
        // Let's iterate over recordsByStudent keys.
        
        recordsByStudent.forEach { (studentId, studentRecords) ->
            val studentRecordsMap = studentRecords.associateBy { it.gradeItemId }
            
            // Calculate attendance stats for this student
            val studentAttendance = attendanceByStudent[studentId] ?: emptyList()
            val absTD = studentAttendance.count { it.type == "TD" && it.status in listOf("A", "E") }
            val absTP = studentAttendance.count { it.type == "TP" && it.status in listOf("A", "E") }
            val presCours = studentAttendance.count { (it.type == "Cours" || it.type.isEmpty()) && it.status == "P" }
            
            calculatedItems.forEach { calcItem ->
                val formula = calcItem.formula
                if (formula != null) {
                    // Build variables map
                    val variables = mutableMapOf<String, Float>()
                    items.forEach { item ->
                        // Use item name as variable
                        val record = studentRecordsMap[item.id]
                        val value = record?.score ?: 0f
                        variables[item.name] = value
                    }
                    
                    // Add attendance variables
                    variables["abs-td"] = absTD.toFloat()
                    variables["abs-tp"] = absTP.toFloat()
                    variables["pres-cours"] = presCours.toFloat()
                    variables["tot-td"] = totalTD.toFloat()
                    variables["tot-tp"] = totalTP.toFloat()
                    variables["tot-c"] = totalCours.toFloat()
                    
                    val result = com.example.additioapp.util.FormulaEvaluator.evaluate(formula, variables)
                    android.util.Log.d("GradeViewModel", "Calculated grade for student $studentId item ${calcItem.name}: $result")
                    
                    // Update or Insert record
                    val existingRecord = studentRecordsMap[calcItem.id]
                    if (existingRecord != null) {
                        if (existingRecord.score != result) {
                            repository.updateGradeRecord(existingRecord.copy(score = result))
                        }
                    } else {
                        repository.insertGradeRecord(
                            GradeRecordEntity(
                                studentId = studentId,
                                gradeItemId = calcItem.id,
                                score = result
                            )
                        )
                    }
                }
            }
        }
    }
}
