package com.edu.gymledger.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.BodyMeasurementRepository
import com.edu.gymledger.domain.model.BodyMeasurement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface BodyUiEvent {
    data object SaveSucceeded : BodyUiEvent
    data class Error(val message: String) : BodyUiEvent
}

data class BodyUiState(
    val measurements: List<BodyMeasurement> = emptyList(),
    val latestMeasurement: BodyMeasurement? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class BodyViewModel(
    private val repository: BodyMeasurementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyUiState())
    val uiState: StateFlow<BodyUiState> = _uiState.asStateFlow()

    private val _events = Channel<BodyUiEvent>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { measurements ->
                _uiState.value = BodyUiState(
                    measurements = measurements,
                    latestMeasurement = measurements.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun addMeasurement(
        date: String,
        weightStr: String,
        waistStr: String = "",
        chestStr: String = "",
        armStr: String = "",
        thighStr: String = "",
        hipStr: String = "",
        notes: String = ""
    ) {
        val trimmedDate = date.trim()
        if (trimmedDate.isBlank()) {
            _events.trySend(BodyUiEvent.Error("Date is required."))
            return
        }

        val weight = parsePositiveDouble(weightStr, "Weight") ?: return

        val waist = parseOptionalDouble(waistStr)
        if (waist == null && waistStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Waist must be a valid number greater than 0."))
            return
        }

        val chest = parseOptionalDouble(chestStr)
        if (chest == null && chestStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Chest must be a valid number greater than 0."))
            return
        }

        val arm = parseOptionalDouble(armStr)
        if (arm == null && armStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Arm must be a valid number greater than 0."))
            return
        }

        val thigh = parseOptionalDouble(thighStr)
        if (thigh == null && thighStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Thigh must be a valid number greater than 0."))
            return
        }

        val hip = parseOptionalDouble(hipStr)
        if (hip == null && hipStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Hip must be a valid number greater than 0."))
            return
        }

        viewModelScope.launch {
            try {
                repository.create(
                    date = trimmedDate,
                    weight = weight,
                    waist = waist,
                    chest = chest,
                    arm = arm,
                    thigh = thigh,
                    hip = hip,
                    notes = notes.ifBlank { null }
                )
                _events.trySend(BodyUiEvent.SaveSucceeded)
            } catch (e: Exception) {
                _events.trySend(BodyUiEvent.Error(e.message ?: "Failed to save measurement."))
            }
        }
    }

    fun updateMeasurement(
        measurement: BodyMeasurement,
        date: String,
        weightStr: String,
        waistStr: String = "",
        chestStr: String = "",
        armStr: String = "",
        thighStr: String = "",
        hipStr: String = "",
        notes: String = ""
    ) {
        val trimmedDate = date.trim()
        if (trimmedDate.isBlank()) {
            _events.trySend(BodyUiEvent.Error("Date is required."))
            return
        }

        val weight = parsePositiveDouble(weightStr, "Weight") ?: return

        val waist = parseOptionalDouble(waistStr)
        if (waist == null && waistStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Waist must be a valid number greater than 0."))
            return
        }

        val chest = parseOptionalDouble(chestStr)
        if (chest == null && chestStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Chest must be a valid number greater than 0."))
            return
        }

        val arm = parseOptionalDouble(armStr)
        if (arm == null && armStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Arm must be a valid number greater than 0."))
            return
        }

        val thigh = parseOptionalDouble(thighStr)
        if (thigh == null && thighStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Thigh must be a valid number greater than 0."))
            return
        }

        val hip = parseOptionalDouble(hipStr)
        if (hip == null && hipStr.isNotBlank()) {
            _events.trySend(BodyUiEvent.Error("Hip must be a valid number greater than 0."))
            return
        }

        viewModelScope.launch {
            try {
                repository.update(
                    measurement.copy(
                        date = trimmedDate,
                        weight = weight,
                        waist = waist,
                        chest = chest,
                        arm = arm,
                        thigh = thigh,
                        hip = hip,
                        notes = notes.ifBlank { null }
                    )
                )
                _events.trySend(BodyUiEvent.SaveSucceeded)
            } catch (e: Exception) {
                _events.trySend(BodyUiEvent.Error(e.message ?: "Failed to update measurement."))
            }
        }
    }

    fun deleteMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            try {
                repository.delete(measurement)
            } catch (e: Exception) {
                _events.trySend(BodyUiEvent.Error(e.message ?: "Failed to delete measurement."))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun parsePositiveDouble(input: String, fieldName: String): Double? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isBlank()) {
            _events.trySend(BodyUiEvent.Error("$fieldName is required."))
            return null
        }
        val value = cleaned.toDoubleOrNull()
        if (value == null || value <= 0.0) {
            _events.trySend(
                BodyUiEvent.Error(
                    if (value != null) "$fieldName must be greater than 0."
                    else "Invalid $fieldName value."
                )
            )
            return null
        }
        return value
    }

    private fun parseOptionalDouble(input: String): Double? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isBlank()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        return value
    }
}
