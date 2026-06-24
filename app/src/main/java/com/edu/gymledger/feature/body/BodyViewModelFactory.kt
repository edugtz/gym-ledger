package com.edu.gymledger.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.gymledger.data.repository.BodyMeasurementRepository

class BodyViewModelFactory(
    private val repository: BodyMeasurementRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BodyViewModel::class.java)) {
            return BodyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
