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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFoodEntrySheet(
    onDismiss: () -> Unit,
    viewModel: SmartFoodEntryViewModel = viewModel(
        factory = SmartFoodEntryViewModelFactory(
            referenceRepository = AppContainer.foodReferenceRepository,
            foodRepository = AppContainer.foodRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.resetState()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
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
                SmartSearchSection(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSelect = viewModel::selectReference
                )
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
