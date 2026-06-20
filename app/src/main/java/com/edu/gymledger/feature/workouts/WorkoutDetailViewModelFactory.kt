package com.edu.gymledger.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.gymledger.data.repository.ExerciseRepository
import com.edu.gymledger.data.repository.WorkoutRepository
import com.edu.gymledger.data.repository.WorkoutSessionExerciseRepository

class WorkoutDetailViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionExerciseRepository: WorkoutSessionExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
            return WorkoutDetailViewModel(workoutRepository, exerciseRepository, workoutSessionExerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
