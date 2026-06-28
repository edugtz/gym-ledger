package com.edu.gymledger.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.gymledger.app.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(AppContainer.settingsRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.onlineAssistance

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OnlineAssistanceSection(
                settings = settings,
                showEndpointHelper = uiState.showEndpointHelper,
                showApiKeyHelper = uiState.showApiKeyHelper,
                onOnlineLookupEnabledChange = viewModel::updateOnlineFoodLookupEnabled,
                onEndpointChange = viewModel::updateFoodLookupEndpoint,
                onApiKeyChange = viewModel::updateFoodLookupApiKey,
                onUsdaEnabledChange = viewModel::updateUsdaEnabled,
                onOpenFoodFactsEnabledChange = viewModel::updateOpenFoodFactsEnabled,
                onSafeModeEnabledChange = viewModel::updateSafeModeEnabled
            )
        }
    }
}

@Composable
private fun OnlineAssistanceSection(
    settings: com.edu.gymledger.data.repository.OnlineAssistanceSettings,
    showEndpointHelper: Boolean,
    showApiKeyHelper: Boolean,
    onOnlineLookupEnabledChange: (Boolean) -> Unit,
    onEndpointChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onUsdaEnabledChange: (Boolean) -> Unit,
    onOpenFoodFactsEnabledChange: (Boolean) -> Unit,
    onSafeModeEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Online Assistance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Online lookup is optional. Saved foods and manual entry still work offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingToggle(
            label = "Enable online food lookup",
            checked = settings.onlineFoodLookupEnabled,
            onCheckedChange = onOnlineLookupEnabledChange
        )

        if (showEndpointHelper) {
            HelperText("Enter an endpoint URL to enable online lookup.")
        }

        OutlinedTextField(
            value = settings.foodLookupEndpoint,
            onValueChange = onEndpointChange,
            label = { Text("Lookup endpoint") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            supportingText = {
                Text(
                    text = "Leave blank to use default.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        if (showApiKeyHelper) {
            HelperText("Enter an API key if your endpoint requires one.")
        }

        OutlinedTextField(
            value = settings.foodLookupApiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Personal API key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = {
                Text(
                    text = "Stored locally on this device only.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        SettingToggle(
            label = "Use USDA generic foods",
            checked = settings.usdaEnabled,
            onCheckedChange = onUsdaEnabledChange
        )

        SettingToggle(
            label = "Use Open Food Facts products",
            checked = settings.openFoodFactsEnabled,
            onCheckedChange = onOpenFoodFactsEnabledChange
        )

        SettingToggle(
            label = "Safe mode",
            checked = settings.safeModeEnabled,
            onCheckedChange = onSafeModeEnabledChange
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun HelperText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(start = 4.dp)
    )
}
