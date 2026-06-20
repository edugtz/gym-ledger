package com.edu.gymledger.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.ExerciseRepository
import com.edu.gymledger.data.repository.RoutineExerciseRepository
import com.edu.gymledger.data.repository.RoutineRepository
import com.edu.gymledger.data.repository.WorkoutRepository
import com.edu.gymledger.data.repository.WorkoutSessionExerciseRepository
import com.edu.gymledger.domain.model.Exercise
import com.edu.gymledger.domain.model.Routine
import com.edu.gymledger.domain.model.RoutineExercise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoutineDetailViewModel(
    private val routineRepository: RoutineRepository,
    private val routineExerciseRepository: RoutineExerciseRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val workoutSessionExerciseRepository: WorkoutSessionExerciseRepository
) : ViewModel() {

    data class RoutineExerciseUiItem(
        val routineExercise: RoutineExercise,
        val exercise: Exercise?
    )

    private val _routineId = MutableStateFlow<Long?>(null)

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine.asStateFlow()

    val uiItems: StateFlow<List<RoutineExerciseUiItem>> = _routineId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                routineExerciseRepository.listByRoutine(id),
                exerciseRepository.getAll()
            ) { routineExercises, exercises ->
                val exerciseMap = exercises.associateBy { it.id }
                routineExercises.map { re ->
                    RoutineExerciseUiItem(
                        routineExercise = re,
                        exercise = exerciseMap[re.exerciseId]
                    )
                }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<Exercise>> = exerciseRepository.getAll()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showExercisePicker = MutableStateFlow(false)
    val showExercisePicker: StateFlow<Boolean> = _showExercisePicker.asStateFlow()

    private val _exerciseSearchQuery = MutableStateFlow("")
    val exerciseSearchQuery: StateFlow<String> = _exerciseSearchQuery.asStateFlow()

    private val _editNoteTarget = MutableStateFlow<RoutineExerciseUiItem?>(null)
    val editNoteTarget: StateFlow<RoutineExerciseUiItem?> = _editNoteTarget.asStateFlow()

    private val _newNoteText = MutableStateFlow("")
    val newNoteText: StateFlow<String> = _newNoteText.asStateFlow()

    private val _removeTarget = MutableStateFlow<RoutineExerciseUiItem?>(null)
    val removeTarget: StateFlow<RoutineExerciseUiItem?> = _removeTarget.asStateFlow()

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()

    private val _renameName = MutableStateFlow("")
    val renameName: StateFlow<String> = _renameName.asStateFlow()

    private val _renameDescription = MutableStateFlow("")
    val renameDescription: StateFlow<String> = _renameDescription.asStateFlow()

    private var loadJob: Job? = null

    fun loadRoutine(routineId: Long) {
        if (_routineId.value == routineId) return
        _routineId.value = routineId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _routine.value = routineRepository.getById(routineId)
            } catch (e: CancellationException) {
                // Normal cancellation
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load routine"
            }
        }
    }

    fun addExercise(exerciseId: Long) {
        val routineId = _routineId.value ?: return
        viewModelScope.launch {
            try {
                routineExerciseRepository.create(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    orderNum = null,
                    notes = null
                )
                _showExercisePicker.value = false
                _exerciseSearchQuery.value = ""
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add exercise"
            }
        }
    }

    fun requestRemoveExercise(uiItem: RoutineExerciseUiItem) {
        _removeTarget.value = uiItem
    }

    fun cancelRemoveExercise() {
        _removeTarget.value = null
    }

    fun removeExercise() {
        val target = _removeTarget.value ?: return
        viewModelScope.launch {
            try {
                routineExerciseRepository.delete(target.routineExercise)
                _removeTarget.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove exercise"
            }
        }
    }

    fun requestEditNote(uiItem: RoutineExerciseUiItem) {
        _editNoteTarget.value = uiItem
        _newNoteText.value = uiItem.routineExercise.notes ?: ""
    }

    fun cancelEditNote() {
        _editNoteTarget.value = null
        _newNoteText.value = ""
    }

    fun updateNoteText(value: String) {
        _newNoteText.value = value
    }

    fun saveNote() {
        val target = _editNoteTarget.value ?: return
        val noteText = _newNoteText.value.trim().ifBlank { null }
        viewModelScope.launch {
            try {
                routineExerciseRepository.update(
                    target.routineExercise.copy(notes = noteText)
                )
                _editNoteTarget.value = null
                _newNoteText.value = ""
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save note"
            }
        }
    }

    fun toggleExercisePicker(show: Boolean) {
        _showExercisePicker.value = show
        if (!show) {
            _exerciseSearchQuery.value = ""
        }
    }

    fun updateExerciseSearchQuery(value: String) {
        _exerciseSearchQuery.value = value
    }

    fun showRenameDialog() {
        val current = _routine.value ?: return
        _renameName.value = current.name
        _renameDescription.value = current.description ?: ""
        _showRenameDialog.value = true
    }

    fun hideRenameDialog() {
        _showRenameDialog.value = false
    }

    fun updateRenameName(value: String) {
        _renameName.value = value
    }

    fun updateRenameDescription(value: String) {
        _renameDescription.value = value
    }

    fun saveRename() {
        val current = _routine.value ?: return
        val name = _renameName.value.trim()
        if (name.isBlank()) {
            _error.value = "Routine name cannot be blank"
            return
        }
        val description = _renameDescription.value.trim().ifBlank { null }
        viewModelScope.launch {
            try {
                val updated = current.copy(name = name, description = description)
                routineRepository.update(updated)
                _routine.value = updated
                _showRenameDialog.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to rename routine"
            }
        }
    }

    fun startWorkout(onWorkoutCreated: (Long) -> Unit) {
        val routine = _routine.value ?: return
        val routineId = _routineId.value ?: return
        val exercises = uiItems.value
        if (exercises.isEmpty()) {
            _error.value = "Add exercises before starting this routine."
            return
        }
        viewModelScope.launch {
            try {
                val now = Instant.now().toString()
                val session = workoutRepository.createSession(
                    routineId = routineId,
                    title = routine.name,
                    startedAt = now,
                    notes = null
                )
                for (item in exercises) {
                    workoutSessionExerciseRepository.create(
                        sessionId = session.id,
                        exerciseId = item.routineExercise.exerciseId,
                        orderNum = item.routineExercise.orderNum ?: 1,
                        notes = item.routineExercise.notes
                    )
                }
                onWorkoutCreated(session.id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start workout"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
