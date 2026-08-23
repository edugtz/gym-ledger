package com.edu.gymledger.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.gymledger.app.AppContainer
import com.edu.gymledger.domain.model.FoodReference
import com.edu.gymledger.domain.model.lookup.RemoteFoodLookupResult
import com.edu.gymledger.data.repository.lookup.OnlineSearchAvailability

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFoodEntrySheet(
    onDismiss: () -> Unit,
    viewModel: SmartFoodEntryViewModel = viewModel(
        factory = SmartFoodEntryViewModelFactory(
            referenceRepository = AppContainer.foodReferenceRepository,
            foodRepository = AppContainer.foodRepository,
            remoteFoodLookupRepository = AppContainer.remoteFoodLookupRepository,
            settingsFlow = AppContainer.settingsRepository.onlineAssistanceSettings
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedContentScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelSearch()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SmartFoodEntryEvent.SaveSucceeded -> {
                    viewModel.onSaveHandled()
                    onDismiss()
                }
                is SmartFoodEntryEvent.Error -> {
                }
            }
        }
    }

    LaunchedEffect(uiState.selectedReference?.id) {
        if (uiState.selectedReference != null) {
            selectedContentScrollState.scrollTo(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.cancelSearch()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (uiState.selectedReference != null) {
                        // Selected-food flow is taller than a phone viewport once the
                        // IME is shown (edge-to-edge). imePadding() is applied before
                        // verticalScroll() so the padded viewport stays above the
                        // keyboard and the bottom actions remain reachable.
                        Modifier
                            .imePadding()
                            .verticalScroll(selectedContentScrollState)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp)
                    } else {
                        // Search states host fixed-height LazyColumn result lists and
                        // must not gain a verticalScroll parent.
                        Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp)
                    }
                )
        ) {
            Text(
                text = "Smart food entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Search common foods and calculate approximate macros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            if (uiState.selectedReference == null) {
                if (uiState.isOnlineAvailable) {
                    OnlineModeSelector(
                        onlineMode = uiState.onlineMode,
                        onToggleOnlineMode = viewModel::toggleOnlineMode
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (uiState.onlineMode && uiState.isOnlineAvailable) {
                    OnlineSearchSection(
                        query = uiState.onlineQuery,
                        results = uiState.onlineResults,
                        isLoading = uiState.isOnlineSearching,
                        isCheckingOnlineAvailability = uiState.isCheckingOnlineAvailability,
                        hasSubmittedOnlineSearch = uiState.hasSubmittedOnlineSearch,
                        error = uiState.onlineError,
                        availability = uiState.onlineAvailability,
                        minQueryLength = uiState.minQueryLength,
                        onQueryChange = viewModel::onOnlineQueryChange,
                        onSubmit = viewModel::submitOnlineSearch,
                        onSelect = viewModel::selectOnlineResult
                    )
                } else {
                    SmartSearchSection(
                        query = uiState.searchQuery,
                        results = uiState.searchResults,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSelect = viewModel::selectReference
                    )
                }
            } else {
                SmartSelectedSection(
                    reference = uiState.selectedReference!!,
                    onChangeSelection = viewModel::clearSelection
                )

                Spacer(Modifier.height(16.dp))

                SmartQuantitySection(
                    reference = uiState.selectedReference!!,
                    unitsText = uiState.unitsText,
                    gramsText = uiState.gramsText,
                    onUnitsChange = viewModel::onUnitsChange,
                    onGramsChange = viewModel::onGramsChange
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SmartNutritionSection(
                    nameText = uiState.nameText,
                    caloriesText = uiState.caloriesText,
                    proteinText = uiState.proteinText,
                    carbsText = uiState.carbsText,
                    fatText = uiState.fatText,
                    onNameChange = viewModel::onNameChange,
                    onCaloriesChange = viewModel::onCaloriesChange,
                    onProteinChange = viewModel::onProteinChange,
                    onCarbsChange = viewModel::onCarbsChange,
                    onFatChange = viewModel::onFatChange
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Saving..." else "Save as custom food")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineModeSelector(
    onlineMode: Boolean,
    onToggleOnlineMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !onlineMode,
            onClick = { onToggleOnlineMode(false) },
            label = { Text("Local reference") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = onlineMode,
            onClick = { onToggleOnlineMode(true) },
            label = { Text("Online search") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun OnlineSearchSection(
    query: String,
    results: List<RemoteFoodLookupResult>,
    isLoading: Boolean,
    isCheckingOnlineAvailability: Boolean,
    hasSubmittedOnlineSearch: Boolean,
    error: String?,
    availability: OnlineSearchAvailability,
    minQueryLength: Int,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSelect: (RemoteFoodLookupResult) -> Unit
) {
    val availabilityMessage = when (availability) {
        is OnlineSearchAvailability.NotConfigured -> "Online lookup isn't configured. Add an API key in Settings."
        is OnlineSearchAvailability.UsdaDisabled -> "Online lookup isn't available. Enable USDA in Settings."
        is OnlineSearchAvailability.SafeMode -> "Online lookup isn't available while safe mode is on."
        is OnlineSearchAvailability.InvalidEndpoint -> "The lookup endpoint URL is invalid. Check Settings."
        is OnlineSearchAvailability.RemoteDisabled ->
            if (isCheckingOnlineAvailability) null else "Online lookup is temporarily disabled."
        is OnlineSearchAvailability.Disabled -> null
        is OnlineSearchAvailability.Available -> null
    }

    if (isCheckingOnlineAvailability) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Checking online availability...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    } else if (availabilityMessage != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = availabilityMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search foods online") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmit() }
        ),
        enabled = !isLoading && availability is OnlineSearchAvailability.Available
    )

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = onSubmit,
            enabled = !isLoading &&
                query.trim().length >= minQueryLength &&
                availability is OnlineSearchAvailability.Available
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Search online")
        }
    }

    Spacer(Modifier.height(12.dp))

    if (error != null && availability is OnlineSearchAvailability.Available) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    if (results.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.externalId }) { result ->
                OnlineResultRow(
                    result = result,
                    onClick = { onSelect(result) }
                )
            }
        }
    } else if (!isLoading && query.trim().length < minQueryLength) {
        Text(
            text = "Enter at least $minQueryLength characters to search online.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 24.dp)
        )
    } else if (!isLoading && !hasSubmittedOnlineSearch) {
        Text(
            text = "Press Search online to search.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlineResultRow(
    result: RemoteFoodLookupResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${result.caloriesPer100g} kcal per 100 g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(result.source) },
                        enabled = false,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (result.isApproximate) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Approximate") },
                            enabled = false,
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                disabledLabelColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSearchSection(
    query: String,
    results: List<FoodReference>,
    onQueryChange: (String) -> Unit,
    onSelect: (FoodReference) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search foods") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )

    Spacer(Modifier.height(12.dp))

    if (query.isNotBlank() && results.isEmpty()) {
        Text(
            text = "No matching reference foods",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    } else if (results.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.id }) { ref ->
                ReferenceFoodRow(
                    reference = ref,
                    onClick = { onSelect(ref) }
                )
            }
        }
    } else {
        Text(
            text = "Start typing to search common foods.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
}

@Composable
private fun ReferenceFoodRow(
    reference: FoodReference,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reference.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${reference.caloriesPer100g} kcal per 100 g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (reference.gramsPerUnit != null && reference.unitLabel != null) {
                Text(
                    text = "≈ ${formatGramsForDisplay(reference.gramsPerUnit)} g / ${reference.unitLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartSelectedSection(
    reference: FoodReference,
    onChangeSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = reference.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onChangeSelection) {
                    Text("Change")
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(reference.sourceLabel) },
                    enabled = false,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("Approximate") },
                    enabled = false,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${reference.caloriesPer100g} kcal · P ${formatMacroDisplay(reference.proteinPer100g)} g · C ${formatMacroDisplay(reference.carbsPer100g)} g · F ${formatMacroDisplay(reference.fatPer100g)} g per 100 g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmartQuantitySection(
    reference: FoodReference,
    unitsText: String,
    gramsText: String,
    onUnitsChange: (String) -> Unit,
    onGramsChange: (String) -> Unit
) {
    Text(
        text = "Quantity",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))

    if (reference.gramsPerUnit != null && reference.unitLabel != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = unitsText,
                onValueChange = onUnitsChange,
                label = { Text("Units") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = gramsText,
                onValueChange = onGramsChange,
                label = { Text("Grams") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "1 × ${reference.unitLabel} ≈ ${formatGramsForDisplay(reference.gramsPerUnit)} g",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedTextField(
            value = gramsText,
            onValueChange = onGramsChange,
            label = { Text("Grams") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SmartNutritionSection(
    nameText: String,
    caloriesText: String,
    proteinText: String,
    carbsText: String,
    fatText: String,
    onNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit
) {
    Text(
        text = "Approximate nutrition",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Approximate values from local reference. Review before saving.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = nameText,
        onValueChange = onNameChange,
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = caloriesText,
        onValueChange = onCaloriesChange,
        label = { Text("Calories (kcal)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = proteinText,
            onValueChange = onProteinChange,
            label = { Text("Protein (g)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = carbsText,
            onValueChange = onCarbsChange,
            label = { Text("Carbs (g)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = fatText,
            onValueChange = onFatChange,
            label = { Text("Fat (g)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            )
        )
    }
}

private fun formatGramsForDisplay(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.1f".format(value).trimEnd('0').trimEnd('.')
    }
}

private fun formatMacroDisplay(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.1f".format(value)
    }
}
