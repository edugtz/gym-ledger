package com.edu.gymledger.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.FoodReferenceRepository
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.domain.model.FoodReference
import com.edu.gymledger.domain.model.FoodReferenceCalculator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SmartFoodEntryEvent {
    data object SaveSucceeded : SmartFoodEntryEvent
    data class Error(val message: String) : SmartFoodEntryEvent
}

data class SmartFoodEntryUiState(
    val searchQuery: String = "",
    val searchResults: List<FoodReference> = emptyList(),
    val selectedReference: FoodReference? = null,
    val unitsText: String = "",
    val gramsText: String = "",
    val nameText: String = "",
    val caloriesText: String = "",
    val proteinText: String = "",
    val carbsText: String = "",
    val fatText: String = "",
    val isSaving: Boolean = false,
    val saveSucceeded: Boolean = false
)

class SmartFoodEntryViewModel(
    private val referenceRepository: FoodReferenceRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartFoodEntryUiState())
    val uiState: StateFlow<SmartFoodEntryUiState> = _uiState.asStateFlow()

    private val _events = Channel<SmartFoodEntryEvent>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    fun resetState() {
        _uiState.value = SmartFoodEntryUiState()
    }

    fun onSearchQueryChange(query: String) {
        val trimmed = query.trim()
        val results = referenceRepository.search(trimmed)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchResults = results
        )
    }

    fun selectReference(ref: FoodReference) {
        val state = _uiState.value
        val initialGrams = if (ref.gramsPerUnit != null) {
            formatDouble(ref.gramsPerUnit)
        } else {
            "100"
        }
        val initialUnits = if (ref.gramsPerUnit != null) "1" else ""

        val calculated = try {
            if (ref.gramsPerUnit != null) {
                FoodReferenceCalculator.calculateFromUnits(ref, 1.0)
            } else {
                FoodReferenceCalculator.calculateFromGrams(ref, 100.0)
            }
        } catch (_: Exception) {
            null
        }

        _uiState.value = state.copy(
            selectedReference = ref,
            searchQuery = "",
            searchResults = emptyList(),
            unitsText = initialUnits,
            gramsText = if (ref.gramsPerUnit != null) {
                formatGramsFromUnits(initialUnits, ref.gramsPerUnit)
            } else {
                initialGrams
            },
            nameText = ref.name,
            caloriesText = calculated?.calories?.toString() ?: "",
            proteinText = calculated?.let { formatMacro(it.protein) } ?: "",
            carbsText = calculated?.let { formatMacro(it.carbs) } ?: "",
            fatText = calculated?.let { formatMacro(it.fat) } ?: ""
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedReference = null,
            unitsText = "",
            gramsText = "",
            nameText = "",
            caloriesText = "",
            proteinText = "",
            carbsText = "",
            fatText = ""
        )
    }

    fun onUnitsChange(text: String) {
        val ref = _uiState.value.selectedReference ?: return
        val gpu = ref.gramsPerUnit ?: return
        val cleaned = text.trim().replace(",", ".")
        val units = cleaned.toDoubleOrNull()

        val gramsText = if (units != null && units > 0.0 && units.isFinite()) {
            formatGramsFromUnits(cleaned, gpu)
        } else {
            ""
        }

        val calculated = if (units != null && units > 0.0 && units.isFinite()) {
            try {
                FoodReferenceCalculator.calculateFromUnits(ref, units)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        _uiState.value = _uiState.value.copy(
            unitsText = text,
            gramsText = gramsText,
            caloriesText = calculated?.calories?.toString() ?: "",
            proteinText = calculated?.let { formatMacro(it.protein) } ?: "",
            carbsText = calculated?.let { formatMacro(it.carbs) } ?: "",
            fatText = calculated?.let { formatMacro(it.fat) } ?: ""
        )
    }

    fun onGramsChange(text: String) {
        val ref = _uiState.value.selectedReference ?: return
        val cleaned = text.trim().replace(",", ".")
        val grams = cleaned.toDoubleOrNull()

        val unitsText = if (ref.gramsPerUnit != null && grams != null && grams > 0.0 && grams.isFinite()) {
            formatDouble(grams / ref.gramsPerUnit)
        } else if (ref.gramsPerUnit != null) {
            ""
        } else {
            _uiState.value.unitsText
        }

        val calculated = if (grams != null && grams > 0.0 && grams.isFinite()) {
            try {
                FoodReferenceCalculator.calculateFromGrams(ref, grams)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        _uiState.value = _uiState.value.copy(
            gramsText = text,
            unitsText = unitsText,
            caloriesText = calculated?.calories?.toString() ?: "",
            proteinText = calculated?.let { formatMacro(it.protein) } ?: "",
            carbsText = calculated?.let { formatMacro(it.carbs) } ?: "",
            fatText = calculated?.let { formatMacro(it.fat) } ?: ""
        )
    }

    fun onNameChange(text: String) {
        _uiState.value = _uiState.value.copy(nameText = text)
    }

    fun onCaloriesChange(text: String) {
        _uiState.value = _uiState.value.copy(caloriesText = text)
    }

    fun onProteinChange(text: String) {
        _uiState.value = _uiState.value.copy(proteinText = text)
    }

    fun onCarbsChange(text: String) {
        _uiState.value = _uiState.value.copy(carbsText = text)
    }

    fun onFatChange(text: String) {
        _uiState.value = _uiState.value.copy(fatText = text)
    }

    fun save() {
        val state = _uiState.value
        val ref = state.selectedReference
        if (ref == null) {
            _events.trySend(SmartFoodEntryEvent.Error("Select a reference food first."))
            return
        }

        val name = state.nameText.trim()
        if (name.isBlank()) {
            _events.trySend(SmartFoodEntryEvent.Error("Food name cannot be blank."))
            return
        }

        val caloriesCleaned = state.caloriesText.trim()
        if (caloriesCleaned.isBlank()) {
            _events.trySend(SmartFoodEntryEvent.Error("Calories cannot be blank."))
            return
        }
        val calories = caloriesCleaned.toIntOrNull()
        if (calories == null || calories < 0) {
            _events.trySend(SmartFoodEntryEvent.Error("Calories must be a whole number >= 0."))
            return
        }

        val protein = parseMacro(state.proteinText, "Protein") ?: return
        val carbs = parseMacro(state.carbsText, "Carbs") ?: return
        val fat = parseMacro(state.fatText, "Fat") ?: return

        val gramsCleaned = state.gramsText.trim().replace(",", ".")
        val grams = gramsCleaned.toDoubleOrNull()
        if (grams == null || !grams.isFinite() || grams <= 0.0) {
            _events.trySend(SmartFoodEntryEvent.Error("Quantity must be greater than 0."))
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            try {
                foodRepository.create(
                    name = name,
                    caloriesPerServing = calories,
                    servingSize = grams,
                    proteinPerServing = protein,
                    carbsPerServing = carbs,
                    fatPerServing = fat
                )
                _uiState.value = _uiState.value.copy(isSaving = false, saveSucceeded = true)
                _events.trySend(SmartFoodEntryEvent.SaveSucceeded)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _events.trySend(SmartFoodEntryEvent.Error(e.message ?: "Failed to save food."))
            }
        }
    }

    fun onSaveHandled() {
        _uiState.value = _uiState.value.copy(saveSucceeded = false)
    }

    private fun parseMacro(input: String, fieldName: String): Double? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isBlank()) return 0.0
        val value = cleaned.toDoubleOrNull()
        if (value == null) {
            _events.trySend(SmartFoodEntryEvent.Error("$fieldName must be a valid number."))
            return null
        }
        if (!value.isFinite()) {
            _events.trySend(SmartFoodEntryEvent.Error("$fieldName must be a finite number."))
            return null
        }
        if (value < 0.0) {
            _events.trySend(SmartFoodEntryEvent.Error("$fieldName must be 0 or greater."))
            return null
        }
        return value
    }

    private fun formatGramsFromUnits(unitsText: String, gpu: Double): String {
        val units = unitsText.trim().replace(",", ".").toDoubleOrNull() ?: return ""
        if (!units.isFinite() || units <= 0.0) return ""
        val grams = units * gpu
        return formatDouble(grams)
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.2f".format(value).trimEnd('0').trimEnd('.')
        }
    }

    private fun formatMacro(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.1f".format(value)
        }
    }
}
