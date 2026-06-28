package com.edu.gymledger.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val onlineAssistance: OnlineAssistanceSettings = OnlineAssistanceSettings(),
    val showEndpointHelper: Boolean = false,
    val showApiKeyHelper: Boolean = false
)

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.onlineAssistanceSettings
        .map { settings ->
            SettingsUiState(
                onlineAssistance = settings,
                showEndpointHelper = settings.onlineFoodLookupEnabled && settings.foodLookupEndpoint.isBlank(),
                showApiKeyHelper = settings.onlineFoodLookupEnabled && settings.foodLookupApiKey.isBlank()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun updateOnlineFoodLookupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateOnlineFoodLookupEnabled(enabled)
        }
    }

    fun updateFoodLookupEndpoint(endpoint: String) {
        viewModelScope.launch {
            repository.updateFoodLookupEndpoint(endpoint)
        }
    }

    fun updateFoodLookupApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.updateFoodLookupApiKey(apiKey)
        }
    }

    fun updateUsdaEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUsdaEnabled(enabled)
        }
    }

    fun updateOpenFoodFactsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateOpenFoodFactsEnabled(enabled)
        }
    }

    fun updateSafeModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSafeModeEnabled(enabled)
        }
    }
}
