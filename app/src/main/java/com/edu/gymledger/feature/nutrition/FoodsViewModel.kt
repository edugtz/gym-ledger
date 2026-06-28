package com.edu.gymledger.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.domain.model.Food
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface FoodsUiEvent {
    data object SaveSucceeded : FoodsUiEvent
    data object DeleteSucceeded : FoodsUiEvent
    data class Error(val message: String) : FoodsUiEvent
}

data class FoodsUiState(
    val searchQuery: String = "",
    val foods: List<Food> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class FoodsViewModel(
    private val repository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodsUiState())
    val uiState: StateFlow<FoodsUiState> = _uiState.asStateFlow()

    private val _events = Channel<FoodsUiEvent>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .flatMapLatest { query ->
                    val foodsFlow: Flow<List<Food>> = if (query.isBlank()) {
                        repository.getAll()
                    } else {
                        repository.searchByName(query)
                    }
                    foodsFlow
                }
                .collect { foods ->
                    _uiState.value = _uiState.value.copy(
                        searchQuery = searchQuery.value,
                        foods = foods,
                        isLoading = false
                    )
                }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addFood(
        name: String,
        caloriesStr: String,
        servingSizeStr: String = "",
        proteinStr: String = "",
        carbsStr: String = "",
        fatStr: String = ""
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _events.trySend(FoodsUiEvent.Error("Food name is required."))
            return
        }

        val calories = parseRequiredInt(caloriesStr, "Calories") ?: return
        val servingSize = parseOptionalPositiveDouble(servingSizeStr, "Serving size")
        if (servingSize == null && servingSizeStr.isNotBlank()) {
            _events.trySend(FoodsUiEvent.Error("Serving size must be greater than 0."))
            return
        }

        val protein = parseOptionalNonNegativeDouble(proteinStr, "Protein") ?: return
        val carbs = parseOptionalNonNegativeDouble(carbsStr, "Carbs") ?: return
        val fat = parseOptionalNonNegativeDouble(fatStr, "Fat") ?: return

        viewModelScope.launch {
            try {
                repository.create(
                    name = trimmedName,
                    caloriesPerServing = calories,
                    servingSize = servingSize,
                    proteinPerServing = protein,
                    carbsPerServing = carbs,
                    fatPerServing = fat
                )
                _events.trySend(FoodsUiEvent.SaveSucceeded)
            } catch (e: Exception) {
                _events.trySend(FoodsUiEvent.Error(e.message ?: "Failed to save food."))
            }
        }
    }

    fun updateFood(
        food: Food,
        name: String,
        caloriesStr: String,
        servingSizeStr: String = "",
        proteinStr: String = "",
        carbsStr: String = "",
        fatStr: String = ""
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _events.trySend(FoodsUiEvent.Error("Food name is required."))
            return
        }

        val calories = parseRequiredInt(caloriesStr, "Calories") ?: return
        val servingSize = parseOptionalPositiveDouble(servingSizeStr, "Serving size")
        if (servingSize == null && servingSizeStr.isNotBlank()) {
            _events.trySend(FoodsUiEvent.Error("Serving size must be greater than 0."))
            return
        }

        val protein = parseOptionalNonNegativeDouble(proteinStr, "Protein") ?: return
        val carbs = parseOptionalNonNegativeDouble(carbsStr, "Carbs") ?: return
        val fat = parseOptionalNonNegativeDouble(fatStr, "Fat") ?: return

        viewModelScope.launch {
            try {
                repository.update(
                    food.copy(
                        name = trimmedName,
                        caloriesPerServing = calories,
                        servingSize = servingSize,
                        proteinPerServing = protein,
                        carbsPerServing = carbs,
                        fatPerServing = fat
                    )
                )
                _events.trySend(FoodsUiEvent.SaveSucceeded)
            } catch (e: Exception) {
                _events.trySend(FoodsUiEvent.Error(e.message ?: "Failed to update food."))
            }
        }
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch {
            try {
                repository.delete(food)
                _events.trySend(FoodsUiEvent.DeleteSucceeded)
            } catch (e: Exception) {
                _events.trySend(FoodsUiEvent.Error(e.message ?: "Failed to delete food."))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun parseRequiredInt(input: String, fieldName: String): Int? {
        val cleaned = input.trim()
        if (cleaned.isBlank()) {
            _events.trySend(FoodsUiEvent.Error("$fieldName are required."))
            return null
        }
        val value = cleaned.toIntOrNull()
        if (value == null) {
            _events.trySend(FoodsUiEvent.Error("$fieldName must be a whole number."))
            return null
        }
        if (value < 0) {
            _events.trySend(FoodsUiEvent.Error("$fieldName must be 0 or greater."))
            return null
        }
        return value
    }

    private fun parseOptionalPositiveDouble(input: String, fieldName: String): Double? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isBlank()) return null
        val value = cleaned.toDoubleOrNull()
        if (value == null || value <= 0.0 || !value.isFinite()) {
            _events.trySend(FoodsUiEvent.Error("$fieldName must be greater than 0."))
            return null
        }
        return value
    }

    private fun parseOptionalNonNegativeDouble(input: String, fieldName: String): Double? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isBlank()) return 0.0
        val value = cleaned.toDoubleOrNull()
        if (value == null || value < 0.0 || !value.isFinite()) {
            _events.trySend(FoodsUiEvent.Error("$fieldName must be 0 or greater."))
            return null
        }
        return value
    }
}
