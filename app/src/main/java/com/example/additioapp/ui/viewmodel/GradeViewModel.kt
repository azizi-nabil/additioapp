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

        // Topologically sort calculated items by dependencies
        val sortedCalculated = topologicalSortByDependencies(calculatedItems, items)

        // Fetch ALL students in the class
        val students = repository.getStudentsForClassOnce(classId)
        if (students.isEmpty()) return@launch

        // Fetch attendance data for the class
        val attendanceRecords = repository.getAttendanceWithTypeForClassSync(classId)
        val attendanceByStudent = attendanceRecords.groupBy { it.studentId }
        
        // Fetch behavior data for the class
        val behaviorRecords = repository.getBehaviorsForClassSync(classId)
        val behaviorByStudent = behaviorRecords.groupBy { it.studentId }
        
        // Get total session counts
        val totalTD = repository.getTotalSessionCountByType(classId, "TD")
        val totalTP = repository.getTotalSessionCountByType(classId, "TP")
        val totalCours = repository.getTotalCoursSessionCount(classId)

        // Group records by student
        val recordsByStudent = records.groupBy { it.studentId }
        
        // Iterate over ALL students, not just those with records
        students.forEach { student ->
            val studentId = student.id
            val studentGroupId = student.groupId
            val studentRecords = recordsByStudent[studentId] ?: emptyList()
            val studentRecordsMap = studentRecords.associateBy { it.gradeItemId }
            
            // Filter items to only include those matching student's groupId (or null groupId for shared items)
            val relevantItems = items.filter { item ->
                item.groupId == null || item.groupId == studentGroupId
            }
            
            
            // Calculate attendance stats for this student
            val studentAttendance = attendanceByStudent[studentId] ?: emptyList()
            val absTD = studentAttendance.count { it.type == "TD" && (it.status == "A" || it.status == "E") }
            val absTP = studentAttendance.count { it.type == "TP" && (it.status == "A" || it.status == "E") }
            val justTD = studentAttendance.count { it.type == "TD" && it.status == "E" }
            val justTP = studentAttendance.count { it.type == "TP" && it.status == "E" }
            val presCours = studentAttendance.count { (it.type == "Cours" || it.type.isEmpty()) && it.status == "P" }
            
            // Calculate behavior stats for this student
            val studentBehavior = behaviorByStudent[studentId] ?: emptyList()
            val posCount = studentBehavior.count { it.type == "POSITIVE" }
            val negCount = studentBehavior.count { it.type == "NEGATIVE" }
            
            // Build initial variables map from relevant items the student has records for
            // Only use items matching student's groupId
            val variables = mutableMapOf<String, Float>()
            relevantItems.forEach { item ->
                val record = studentRecordsMap[item.id]
                if (record != null) {
                    val trimmedName = item.name.trim()
                    if (!variables.containsKey(trimmedName)) {
                        variables[trimmedName] = record.score
                    }
                }
            }
            
            // Add attendance variables
            variables["abs_td"] = absTD.toFloat()
            variables["abs_tp"] = absTP.toFloat()
            variables["just_td"] = justTD.toFloat()
            variables["just_tp"] = justTP.toFloat()
            variables["pres_c"] = presCours.toFloat()
            variables["tot_td"] = totalTD.toFloat()
            variables["tot_tp"] = totalTP.toFloat()
            variables["tot_c"] = totalCours.toFloat()
            
            // Add behavior variables
            variables["pos"] = posCount.toFloat()
            variables["neg"] = negCount.toFloat()
            
            // Process calculated items in dependency order, only for items matching student's group
            val relevantCalculated = sortedCalculated.filter { it.groupId == null || it.groupId == studentGroupId }
            relevantCalculated.forEach { calcItem ->
                val formula = calcItem.formula
                if (formula != null) {
                    var result = com.example.additioapp.util.FormulaEvaluator.evaluate(formula, variables)
                    
                    // Safeguard against NaN or Infinity which would crash the DB
                    if (result.isNaN() || result.isInfinite()) {
                        android.util.Log.w("GradeViewModel", "Formula result was NaN/Infinite for student $studentId item ${calcItem.name}, defaulting to 0")
                        result = 0f
                    }
                    
                    // Update variables map with trimmed name so dependent formulas see the fresh value
                    variables[calcItem.name.trim()] = result
                    
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
    
    /**
     * Topologically sort calculated items so items that depend on others come after their dependencies.
     * Uses Kahn's algorithm.
     */
    private fun topologicalSortByDependencies(
        calculatedItems: List<GradeItemEntity>,
        allItems: List<GradeItemEntity>
    ): List<GradeItemEntity> {
        
        // Build dependency graph: for each calculated item, find which other calculated items it depends on
        val dependencies = mutableMapOf<Long, MutableSet<Long>>() // itemId -> set of itemIds it depends on
        val dependents = mutableMapOf<Long, MutableSet<Long>>()   // itemId -> set of itemIds that depend on it
        
        calculatedItems.forEach { item ->
            dependencies[item.id] = mutableSetOf()
            dependents[item.id] = mutableSetOf()
        }
        
        calculatedItems.forEach { item ->
            val formula = item.formula?.replace(" ", "")?.lowercase() ?: ""
            // Check if this formula references any other calculated item
            calculatedItems.forEach { other ->
                if (other.id != item.id) {
                    val otherNameNormalized = other.name.replace(" ", "").lowercase()
                    // Simple check: does the formula contain this item name?
                    // Use word boundary-like check to avoid false positives
                    val regex = Regex("\\b${Regex.escape(otherNameNormalized)}\\b", RegexOption.IGNORE_CASE)
                    if (regex.containsMatchIn(formula)) {
                        // item depends on other
                        dependencies[item.id]?.add(other.id)
                        dependents[other.id]?.add(item.id)
                    }
                }
            }
        }
        
        // Kahn's algorithm for topological sort
        val inDegree = mutableMapOf<Long, Int>()
        calculatedItems.forEach { item ->
            inDegree[item.id] = dependencies[item.id]?.size ?: 0
        }
        
        val queue = ArrayDeque<GradeItemEntity>()
        calculatedItems.filter { inDegree[it.id] == 0 }.forEach { queue.add(it) }
        
        val sorted = mutableListOf<GradeItemEntity>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            sorted.add(current)
            
            dependents[current.id]?.forEach { depId ->
                inDegree[depId] = (inDegree[depId] ?: 1) - 1
                if (inDegree[depId] == 0) {
                    calculatedItems.find { it.id == depId }?.let { queue.add(it) }
                }
            }
        }
        
        return sorted
    }
}
