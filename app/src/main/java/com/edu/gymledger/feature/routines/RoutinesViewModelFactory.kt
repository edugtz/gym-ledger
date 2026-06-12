package com.edu.gymledger.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.gymledger.data.repository.RoutineRepository

class RoutinesViewModelFactory(
    private val repository: RoutineRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutinesViewModel::class.java)) {
            return RoutinesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
