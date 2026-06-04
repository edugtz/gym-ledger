package com.edu.gymledger.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edu.gymledger.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormScreen(
    exercise: Exercise?,
    onSave: (
        name: String,
        category: String?,
        primaryMuscle: String?,
        secondaryMuscles: String?,
        equipment: String?,
        notes: String?
    ) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEdit = exercise != null

    var name by remember(exercise) { mutableStateOf(exercise?.name ?: "") }
    var category by remember(exercise) { mutableStateOf(exercise?.category) }
    var primaryMuscle by remember(exercise) { mutableStateOf(exercise?.primaryMuscle ?: "") }
    var secondaryMuscles by remember(exercise) { mutableStateOf(exercise?.secondaryMuscles ?: "") }
    var equipment by remember(exercise) { mutableStateOf(exercise?.equipment) }
    var notes by remember(exercise) { mutableStateOf(exercise?.notes ?: "") }

    var showMoreDetails by remember { mutableStateOf(false) }
    var presetSearch by remember { mutableStateOf("") }
    var showPresetPicker by remember(exercise) { mutableStateOf(!isEdit) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun applyPreset(preset: ExercisePreset) {
        name = preset.name
        category = preset.category
        primaryMuscle = preset.primaryMuscle ?: ""
        secondaryMuscles = preset.secondaryMuscles ?: ""
        equipment = preset.equipment
        notes = preset.notes ?: ""
        showPresetPicker = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Exercise" else "New Exercise") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.trim().isBlank()) {
                                nameError = "Exercise name is required"
                                return@IconButton
                            }
                            onSave(
                                name.trim(),
                                category?.trim()?.ifBlank { null },
                                primaryMuscle.trim().ifBlank { null },
                                secondaryMuscles.trim().ifBlank { null },
                                equipment?.trim()?.ifBlank { null },
                                notes.trim().ifBlank { null }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (showPresetPicker) {
                PresetPickerSection(
                    searchQuery = presetSearch,
                    onSearchChange = { presetSearch = it },
                    onPresetSelected = { applyPreset(it) },
                    onDismiss = { showPresetPicker = false }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("Name *") },
                isError = nameError != null,
                supportingText = {
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            CategorySelector(
                selected = category,
                onSelect = { category = it }
            )

            EquipmentSelector(
                selected = equipment,
                onSelect = { equipment = it }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMoreDetails = !showMoreDetails },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More details",
                    style = MaterialTheme.typography.titleSmall
                )
                Icon(
                    imageVector = if (showMoreDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (showMoreDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = primaryMuscle,
                        onValueChange = { primaryMuscle = it },
                        label = { Text("Primary muscle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = secondaryMuscles,
                        onValueChange = { secondaryMuscles = it },
                        label = { Text("Secondary muscles") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            if (onDelete != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Delete exercise", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete exercise?") },
            text = { Text("\"${exercise?.name}\" will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete?.invoke()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PresetPickerSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPresetSelected: (ExercisePreset) -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choose a preset",
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Search presets") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val filtered = if (searchQuery.isBlank()) {
            CommonPresets.list
        } else {
            CommonPresets.list.filter {
                it.name.lowercase().contains(searchQuery.trim().lowercase())
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtered.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryOptions.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = {
                        onSelect(if (selected == option) null else option)
                    },
                    label = { Text(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentSelector(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Equipment",
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EquipmentOptions.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = {
                        onSelect(if (selected == option) null else option)
                    },
                    label = { Text(option) }
                )
            }
        }
    }
}
