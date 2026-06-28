package com.edu.gymledger.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.gymledger.data.repository.FoodReferenceRepository
import com.edu.gymledger.data.repository.FoodRepository

class SmartFoodEntryViewModelFactory(
    private val referenceRepository: FoodReferenceRepository,
    private val foodRepository: FoodRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartFoodEntryViewModel::class.java)) {
            return SmartFoodEntryViewModel(referenceRepository, foodRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
