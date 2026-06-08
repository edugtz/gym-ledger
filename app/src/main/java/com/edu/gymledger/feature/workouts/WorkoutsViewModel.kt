package com.edu.gymledger.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.WorkoutRepository
import com.edu.gymledger.domain.model.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class WorkoutsViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleteTarget = MutableStateFlow<WorkoutSession?>(null)
    val deleteTarget: StateFlow<WorkoutSession?> = _deleteTarget.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllSessions().collect { list ->
                _sessions.value = list
            }
        }
    }

    fun createWorkout(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val created = repository.createSession(
                    routineId = null,
                    title = "Workout",
                    startedAt = Instant.now().toString(),
                    notes = null
                )
                repository.updateSession(created.copy(title = "Workout #${created.id}"))
                onCreated(created.id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create workout"
            }
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            try {
                repository.deleteSession(session)
                _deleteTarget.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete workout"
            }
        }
    }

    fun requestDelete(session: WorkoutSession) {
        _deleteTarget.value = session
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
