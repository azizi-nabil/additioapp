package com.example.additioapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.additioapp.data.repository.AppRepository
import com.example.additioapp.ui.adapters.SearchResult
import kotlinx.coroutines.launch

class GlobalSearchViewModel(private val repository: AppRepository) : ViewModel() {
    
    private val _searchResults = MutableLiveData<List<SearchResult>>()
    val searchResults: LiveData<List<SearchResult>> = _searchResults
    
    fun search(query: String) {
        viewModelScope.launch {
            val results = mutableListOf<SearchResult>()
            
            // Search students
            val students = repository.searchStudents(query)
            results.addAll(students.map { SearchResult.StudentResult(it) })
            
            // Search classes
            val classes = repository.searchClasses(query)
            results.addAll(classes.map { SearchResult.ClassResult(it) })
            
            // Search events
            val events = repository.searchEvents(query)
            results.addAll(events.map { SearchResult.EventResult(it) })
            
            // Search tasks
            val tasks = repository.searchTasks(query)
            results.addAll(tasks.map { SearchResult.TaskResult(it) })
            
            _searchResults.postValue(results)
        }
    }
}
