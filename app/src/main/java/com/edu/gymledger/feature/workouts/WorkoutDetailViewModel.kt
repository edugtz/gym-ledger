package com.edu.gymledger.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.ExerciseRepository
import com.edu.gymledger.data.repository.WorkoutRepository
import com.edu.gymledger.data.repository.WorkoutSessionExerciseRepository
import com.edu.gymledger.domain.model.Exercise
import com.edu.gymledger.domain.model.WorkoutSession
import com.edu.gymledger.domain.model.WorkoutSet
import com.edu.gymledger.domain.model.WorkoutSessionExercise
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionExerciseRepository: WorkoutSessionExerciseRepository
) : ViewModel() {

    private val _session = MutableStateFlow<WorkoutSession?>(null)
    val session: StateFlow<WorkoutSession?> = _session.asStateFlow()

    private val _sets = MutableStateFlow<List<WorkoutSet>>(emptyList())
    val sets: StateFlow<List<WorkoutSet>> = _sets.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deleteTarget = MutableStateFlow<WorkoutSet?>(null)
    val deleteTarget: StateFlow<WorkoutSet?> = _deleteTarget.asStateFlow()

    data class WorkoutSessionExerciseUiItem(
        val sessionExercise: WorkoutSessionExercise,
        val exercise: Exercise?
    )

    private val _plannedExercises = MutableStateFlow<List<WorkoutSessionExerciseUiItem>>(emptyList())
    val plannedExercises: StateFlow<List<WorkoutSessionExerciseUiItem>> = _plannedExercises.asStateFlow()

    private var plannedExercisesJob: Job? = null

    private val _editTarget = MutableStateFlow<WorkoutSet?>(null)
    val editTarget: StateFlow<WorkoutSet?> = _editTarget.asStateFlow()

    private var currentSessionId: Long? = null

    init {
        viewModelScope.launch {
            exerciseRepository.getAll().collect { list ->
                _exercises.value = list
            }
        }
    }

    fun loadSession(sessionId: Long) {
        currentSessionId = sessionId
        viewModelScope.launch {
            try {
                val result = workoutRepository.getSessionWithSets(sessionId)
                _session.value = result?.session
                _sets.value = result?.sets ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load session"
            }
        }
        observePlannedExercises(sessionId)
    }

    private fun observePlannedExercises(sessionId: Long) {
        plannedExercisesJob?.cancel()
        plannedExercisesJob = viewModelScope.launch {
            combine(
                workoutSessionExerciseRepository.listBySession(sessionId),
                exerciseRepository.getAll()
            ) { sessionExercises, allExercises ->
                val exerciseMap = allExercises.associateBy { it.id }
                sessionExercises.map { se ->
                    WorkoutSessionExerciseUiItem(
                        sessionExercise = se,
                        exercise = exerciseMap[se.exerciseId]
                    )
                }
            }.collect { items ->
                _plannedExercises.value = items
            }
        }
    }

    private fun reload() {
        val id = currentSessionId ?: return
        viewModelScope.launch {
            try {
                val result = workoutRepository.getSessionWithSets(id)
                _session.value = result?.session
                _sets.value = result?.sets ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reload"
            }
        }
    }

    fun addSet(
        exerciseId: Long,
        reps: Int,
        weight: Double?,
        rpe: Double?,
        rir: Int?,
        notes: String?
    ) {
        val id = currentSessionId ?: return
        viewModelScope.launch {
            try {
                val currentSets = _sets.value
                val nextIndex = currentSets.maxOfOrNull { it.setIndex }?.plus(1) ?: 1
                workoutRepository.createSet(
                    sessionId = id,
                    exerciseId = exerciseId,
                    setIndex = nextIndex,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    rir = rir,
                    notes = notes
                )
                reload()
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add set"
            }
        }
    }

    fun updateSet(
        set: WorkoutSet,
        exerciseId: Long,
        reps: Int,
        weight: Double?,
        rpe: Double?,
        rir: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val updated = set.copy(
                    exerciseId = exerciseId,
                    reps = reps,
                    weight = weight,
                    rpe = rpe,
                    rir = rir,
                    notes = notes
                )
                workoutRepository.updateSet(updated)
                reload()
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update set"
            }
        }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteSet(set)
                _deleteTarget.value = null
                reload()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete set"
            }
        }
    }

    fun requestDelete(set: WorkoutSet) {
        _deleteTarget.value = set
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    fun requestEdit(set: WorkoutSet) {
        _editTarget.value = set
    }

    fun cancelEdit() {
        _editTarget.value = null
    }

    private val _preselectedExerciseId = MutableStateFlow<Long?>(null)
    val preselectedExerciseId: StateFlow<Long?> = _preselectedExerciseId.asStateFlow()

    fun preselectExercise(exerciseId: Long) {
        _preselectedExerciseId.value = exerciseId
    }

    fun clearPreselectedExercise() {
        _preselectedExerciseId.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
