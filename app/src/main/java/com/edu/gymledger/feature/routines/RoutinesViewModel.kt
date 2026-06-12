package com.edu.gymledger.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.RoutineRepository
import com.edu.gymledger.domain.model.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoutinesViewModel(
    private val repository: RoutineRepository
) : ViewModel() {

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleteTarget = MutableStateFlow<Routine?>(null)
    val deleteTarget: StateFlow<Routine?> = _deleteTarget.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _newRoutineName = MutableStateFlow("")
    val newRoutineName: StateFlow<String> = _newRoutineName.asStateFlow()

    private val _newRoutineDescription = MutableStateFlow("")
    val newRoutineDescription: StateFlow<String> = _newRoutineDescription.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { list ->
                _routines.value = list
            }
        }
    }

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
        _newRoutineName.value = ""
        _newRoutineDescription.value = ""
    }

    fun updateNewRoutineName(value: String) {
        _newRoutineName.value = value
    }

    fun updateNewRoutineDescription(value: String) {
        _newRoutineDescription.value = value
    }

    fun createRoutine() {
        val name = _newRoutineName.value.trim()
        if (name.isBlank()) {
            _error.value = "Routine name cannot be blank"
            return
        }
        val description = _newRoutineDescription.value.trim().ifBlank { null }
        viewModelScope.launch {
            try {
                repository.create(name, description)
                hideCreateDialog()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create routine"
            }
        }
    }

    fun requestDelete(routine: Routine) {
        _deleteTarget.value = routine
    }

    fun deleteRoutine() {
        val target = _deleteTarget.value ?: return
        viewModelScope.launch {
            try {
                repository.delete(target)
                _deleteTarget.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete routine"
            }
        }
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
