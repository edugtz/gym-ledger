package com.edu.gymledger.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.gymledger.data.repository.FoodReferenceRepository
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.data.repository.lookup.RemoteFoodLookupRepository
import kotlinx.coroutines.flow.Flow

class SmartFoodEntryViewModelFactory(
    private val referenceRepository: FoodReferenceRepository,
    private val foodRepository: FoodRepository,
    private val remoteFoodLookupRepository: RemoteFoodLookupRepository,
    private val settingsFlow: Flow<OnlineAssistanceSettings>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartFoodEntryViewModel::class.java)) {
            return SmartFoodEntryViewModel(
                referenceRepository,
                foodRepository,
                remoteFoodLookupRepository,
                settingsFlow
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
