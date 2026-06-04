package com.edu.gymledger.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.ExerciseRepository
import com.edu.gymledger.domain.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExercisesViewModel(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleteTarget = MutableStateFlow<Exercise?>(null)
    val deleteTarget: StateFlow<Exercise?> = _deleteTarget.asStateFlow()

    val filteredExercises: StateFlow<List<Exercise>> = MutableStateFlow(emptyList())

    init {
        viewModelScope.launch {
            repository.getAll().collect { list ->
                _exercises.value = list
                applyFilter()
            }
        }
        viewModelScope.launch {
            _searchQuery.collect {
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val list = if (query.isBlank()) {
            _exercises.value
        } else {
            _exercises.value.filter {
                it.name.lowercase().contains(query)
            }
        }
        (filteredExercises as MutableStateFlow).value = list
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addExercise(
        name: String,
        category: String?,
        primaryMuscle: String?,
        secondaryMuscles: String?,
        equipment: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                repository.create(
                    name = name,
                    category = category,
                    primaryMuscle = primaryMuscle,
                    secondaryMuscles = secondaryMuscles,
                    equipment = equipment,
                    notes = notes
                )
                _error.value = null
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            }
        }
    }

    fun updateExercise(
        exercise: Exercise,
        name: String,
        category: String?,
        primaryMuscle: String?,
        secondaryMuscles: String?,
        equipment: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                repository.update(
                    exercise.copy(
                        name = name,
                        category = category,
                        primaryMuscle = primaryMuscle,
                        secondaryMuscles = secondaryMuscles,
                        equipment = equipment,
                        notes = notes
                    )
                )
                _error.value = null
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.delete(exercise)
            _deleteTarget.value = null
        }
    }

    fun requestDelete(exercise: Exercise) {
        _deleteTarget.value = exercise
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
