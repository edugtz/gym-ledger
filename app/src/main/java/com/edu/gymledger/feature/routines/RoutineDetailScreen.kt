package com.edu.gymledger.feature.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.gymledger.app.AppContainer
import com.edu.gymledger.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RoutineDetailViewModel = viewModel(
        factory = RoutineDetailViewModelFactory(
            AppContainer.routineRepository,
            AppContainer.routineExerciseRepository,
            AppContainer.exerciseRepository,
            AppContainer.workoutRepository,
            AppContainer.workoutSessionExerciseRepository
        )
    )
) {
    val routine by viewModel.routine.collectAsState()
    val uiItems by viewModel.uiItems.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val error by viewModel.error.collectAsState()
    val showExercisePicker by viewModel.showExercisePicker.collectAsState()
    val exerciseSearchQuery by viewModel.exerciseSearchQuery.collectAsState()
    val editNoteTarget by viewModel.editNoteTarget.collectAsState()
    val newNoteText by viewModel.newNoteText.collectAsState()
    val removeTarget by viewModel.removeTarget.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val renameName by viewModel.renameName.collectAsState()
    val renameDescription by viewModel.renameDescription.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(routineId) {
        routineId?.let { viewModel.loadRoutine(it) }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showExercisePicker) {
        if (showExercisePicker) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(routine?.name ?: "Routine")
                },
                actions = {
                    if (routine != null) {
                        TextButton(
                            onClick = { viewModel.startWorkout { sessionId -> onNavigateToWorkoutDetail(sessionId) } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start workout")
                        }
                        IconButton(onClick = { viewModel.showRenameDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleExercisePicker(true) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add exercise"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            routine?.let { r ->
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = r.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    r.description?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiItems.isEmpty()) {
                EmptyRoutineExercisesState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiItems, key = { it.routineExercise.id }) { item ->
                        RoutineExerciseCard(
                            uiItem = item,
                            onEditNote = { viewModel.requestEditNote(item) },
                            onRemove = { viewModel.requestRemoveExercise(item) }
                        )
                    }
                }
            }
        }
    }

    if (showExercisePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleExercisePicker(false) },
            sheetState = sheetState
        ) {
            ExercisePickerSheet(
                allExercises = allExercises,
                addedExerciseIds = uiItems.map { it.routineExercise.exerciseId }.toSet(),
                searchQuery = exerciseSearchQuery,
                onSearchQueryChange = viewModel::updateExerciseSearchQuery,
                onAddExercise = { exerciseId -> viewModel.addExercise(exerciseId) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    editNoteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::cancelEditNote,
            title = { Text("Edit notes") },
            text = {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = viewModel::updateNoteText,
                    label = { Text("Notes") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveNote) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelEditNote) {
                    Text("Cancel")
                }
            }
        )
    }

    removeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoveExercise,
            title = { Text("Remove exercise?") },
            text = {
                Text(
                    "This removes the exercise from this routine only. Your exercise catalog will not be deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::removeExercise) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemoveExercise) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideRenameDialog,
            title = { Text("Rename routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = viewModel::updateRenameName,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = renameDescription,
                        onValueChange = viewModel::updateRenameDescription,
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveRename) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideRenameDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyRoutineExercisesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No exercises added",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add exercises from your catalog to build this routine.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineExerciseCard(
    uiItem: RoutineDetailViewModel.RoutineExerciseUiItem,
    onEditNote: () -> Unit,
    onRemove: () -> Unit
) {
    val exercise = uiItem.exercise
    val name = exercise?.name ?: "Unknown exercise"
    val category = exercise?.category
    val primaryMuscle = exercise?.primaryMuscle

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEditNote) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit notes"
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove exercise",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            val subtitle = buildList {
                category?.let { add(it) }
                primaryMuscle?.let { add(it) }
            }.joinToString(" / ")

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiItem.routineExercise.notes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ExercisePickerSheet(
    allExercises: List<Exercise>,
    addedExerciseIds: Set<Long>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddExercise: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val available = allExercises.filter { it.id !in addedExerciseIds }
    val filtered = if (searchQuery.isBlank()) {
        available
    } else {
        val query = searchQuery.trim().lowercase()
        available.filter { exercise ->
            exercise.name.lowercase().contains(query) ||
            exercise.category?.lowercase()?.contains(query) == true ||
            exercise.primaryMuscle?.lowercase()?.contains(query) == true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Add exercise",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search exercises") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (available.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "All exercises are already in this routine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (filtered.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No exercises match your search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    ExercisePickerRow(
                        exercise = exercise,
                        onAdd = { onAddExercise(exercise.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerRow(
    exercise: Exercise,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildList {
                    exercise.category?.let { add(it) }
                    exercise.primaryMuscle?.let { add(it) }
                }.joinToString(" / ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onAdd) {
                Text("Add")
            }
        }
    }
}
