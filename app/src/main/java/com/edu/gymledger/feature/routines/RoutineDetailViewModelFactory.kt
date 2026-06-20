package com.edu.gymledger.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.gymledger.data.repository.ExerciseRepository
import com.edu.gymledger.data.repository.RoutineExerciseRepository
import com.edu.gymledger.data.repository.RoutineRepository
import com.edu.gymledger.data.repository.WorkoutRepository
import com.edu.gymledger.data.repository.WorkoutSessionExerciseRepository

class RoutineDetailViewModelFactory(
    private val routineRepository: RoutineRepository,
    private val routineExerciseRepository: RoutineExerciseRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val workoutSessionExerciseRepository: WorkoutSessionExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineDetailViewModel::class.java)) {
            return RoutineDetailViewModel(
                routineRepository,
                routineExerciseRepository,
                exerciseRepository,
                workoutRepository,
                workoutSessionExerciseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
