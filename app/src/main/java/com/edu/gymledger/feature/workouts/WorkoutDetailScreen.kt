package com.edu.gymledger.feature.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.gymledger.app.AppContainer
import com.edu.gymledger.domain.model.Exercise
import com.edu.gymledger.domain.model.WorkoutSet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    sessionId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = viewModel(
        factory = WorkoutDetailViewModelFactory(
            AppContainer.workoutRepository,
            AppContainer.exerciseRepository
        )
    )
) {
    val session by viewModel.session.collectAsState()
    val sets by viewModel.sets.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val error by viewModel.error.collectAsState()
    val deleteTarget by viewModel.deleteTarget.collectAsState()
    val editTarget by viewModel.editTarget.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionId) {
        sessionId?.let { viewModel.loadSession(it) }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Workout Detail") },
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add set")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                session == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                sets.isEmpty() -> {
                    EmptySetsState(
                        onAddSet = { showAddDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "header") {
                            SessionHeader(session = session!!)
                        }
                        items(sets, key = { it.id }) { set ->
                            SetCard(
                                set = set,
                                exerciseName = exercises.find { it.id == set.exerciseId }?.name ?: "Unknown",
                                onEdit = { viewModel.requestEdit(set) },
                                onDelete = { viewModel.requestDelete(set) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editTarget != null) {
        AddEditSetSheet(
            set = editTarget,
            exercises = exercises,
            onDismiss = {
                showAddDialog = false
                viewModel.cancelEdit()
            },
            onSave = { exerciseId, reps, weight, rpe, rir, notes ->
                if (editTarget != null) {
                    viewModel.updateSet(editTarget!!, exerciseId, reps, weight, rpe, rir, notes)
                } else {
                    viewModel.addSet(exerciseId, reps, weight, rpe, rir, notes)
                }
                showAddDialog = false
                viewModel.cancelEdit()
            }
        )
    }

    deleteTarget?.let { set ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete set?") },
            text = { Text("Set ${set.setIndex} will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSet(set) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptySetsState(
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
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
            text = "No sets yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first set to start logging this workout.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onAddSet) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add set")
        }
    }
}

@Composable
private fun SessionHeader(session: com.edu.gymledger.domain.model.WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatInstant(session.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SetCard(
    set: WorkoutSet,
    exerciseName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Set ${set.setIndex}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit set",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete set",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${set.reps} reps",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (set.weight != null) {
                    Text(
                        text = "\u00D7 ${formatWeight(set.weight)} kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                val rpeRir = buildString {
                    if (set.rpe != null) {
                        append("RPE ${formatRpe(set.rpe!!)}")
                    }
                    if (set.rir != null) {
                        if (isNotEmpty()) append(" \u00B7 ")
                        append("RIR ${set.rir}")
                    }
                }
                if (rpeRir.isNotEmpty()) {
                    Text(
                        text = rpeRir,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!set.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = set.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSetSheet(
    set: WorkoutSet?,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (exerciseId: Long, reps: Int, weight: Double?, rpe: Double?, rir: Int?, notes: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = set != null

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseExpanded by remember { mutableStateOf(false) }
    var repsText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var rpeText by remember { mutableStateOf("") }
    var rirText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var repsError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var rpeError by remember { mutableStateOf<String?>(null) }
    var rirError by remember { mutableStateOf<String?>(null) }
    var exerciseError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(set) {
        if (set != null) {
            selectedExercise = exercises.find { it.id == set.exerciseId }
            repsText = set.reps.toString()
            weightText = set.weight?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
            rpeText = set.rpe?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
            rirText = set.rir?.toString() ?: ""
            notesText = set.notes ?: ""
        }
    }

    fun validate(): Boolean {
        var valid = true

        if (selectedExercise == null) {
            exerciseError = "Select an exercise"
            valid = false
        } else {
            exerciseError = null
        }

        val reps = repsText.toIntOrNull()
        if (reps == null || reps < 1) {
            repsError = "Must be at least 1"
            valid = false
        } else {
            repsError = null
        }

        val weight = parseSafeDecimal(weightText)
        if (weightText.isNotBlank() && weight == null) {
            weightError = "Invalid number"
            valid = false
        } else if (weight != null && weight < 0.0) {
            weightError = "Cannot be negative"
            valid = false
        } else {
            weightError = null
        }

        val rpe = parseSafeDecimal(rpeText)
        if (rpeText.isNotBlank() && rpe == null) {
            rpeError = "Invalid number"
            valid = false
        } else if (rpe != null && (rpe < 1.0 || rpe > 10.0)) {
            rpeError = "Must be 1.0\u201310.0"
            valid = false
        } else {
            rpeError = null
        }

        val rir = if (rirText.isBlank()) null else rirText.toIntOrNull()
        if (rirText.isNotBlank() && rir == null) {
            rirError = "Invalid number"
            valid = false
        } else if (rir != null && (rir < 0 || rir > 10)) {
            rirError = "Must be 0\u201310"
            valid = false
        } else {
            rirError = null
        }

        return valid
    }

    fun save() {
        if (!validate()) return
        val exercise = selectedExercise ?: return
        val reps = repsText.toIntOrNull() ?: return
        val weight = parseSafeDecimal(weightText)
        val rpe = parseSafeDecimal(rpeText)
        val rir = if (rirText.isBlank()) null else rirText.toIntOrNull()
        val notes = notesText.trim().ifBlank { null }
        onSave(exercise.id, reps, weight, rpe, rir, notes)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isEditing) "Edit Set" else "Add Set",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Exercise selector
            OutlinedTextField(
                value = selectedExercise?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Exercise") },
                placeholder = {
                    if (exercises.isEmpty()) {
                        Text("No exercises \u2014 create one first")
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { exerciseExpanded = !exerciseExpanded }) {
                        Icon(Icons.Default.ArrowDropDown, "Select exercise")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = exerciseError != null,
                singleLine = true
            )
            if (exerciseError != null) {
                Text(
                    text = exerciseError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (exerciseExpanded) {
                if (exercises.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "No exercises available",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Go to Workouts \u2192 Exercises to create one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        exercises.forEach { exercise ->
                            val isSelected = exercise.id == selectedExercise?.id
                            Card(
                                onClick = {
                                    selectedExercise = exercise
                                    exerciseExpanded = false
                                    exerciseError = null
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2x2 grid: Reps | Weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = repsText,
                    onValueChange = {
                        repsText = it.filter { c -> c.isDigit() }
                        repsError = null
                    },
                    label = { Text("Reps *") },
                    placeholder = { Text("e.g. 10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = repsError != null,
                    singleLine = true
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = filterDecimal(it)
                        weightError = null
                    },
                    label = { Text("Weight (kg)") },
                    placeholder = { Text("optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = weightError != null,
                    singleLine = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (repsError != null) {
                    Text(
                        text = repsError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (weightError != null) {
                    Text(
                        text = weightError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // 2x2 grid: RPE | RIR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rpeText,
                    onValueChange = {
                        rpeText = filterDecimal(it)
                        rpeError = null
                    },
                    label = { Text("RPE") },
                    placeholder = { Text("1.0\u201310.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = rpeError != null,
                    singleLine = true
                )
                OutlinedTextField(
                    value = rirText,
                    onValueChange = {
                        rirText = it.filter { c -> c.isDigit() }
                        rirError = null
                    },
                    label = { Text("RIR") },
                    placeholder = { Text("0\u201310") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = rirError != null,
                    singleLine = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (rpeError != null) {
                    Text(
                        text = rpeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (rirError != null) {
                    Text(
                        text = rirError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Notes
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes") },
                placeholder = { Text("optional") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Add Set",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

private fun filterDecimal(input: String): String {
    val normalized = input.replace(',', '.')
    return buildString {
        var dotSeen = false
        for (c in normalized) {
            if (c == '.') {
                if (!dotSeen) {
                    dotSeen = true
                    append(c)
                }
            } else if (c.isDigit()) {
                append(c)
            }
        }
    }
}

private fun parseSafeDecimal(text: String): Double? {
    return text.replace(',', '.').ifBlank { null }?.toDoubleOrNull()
}

private fun formatWeight(weight: Double): String {
    return if (weight == weight.toLong().toDouble()) {
        weight.toLong().toString()
    } else {
        weight.toString()
    }
}

private fun formatRpe(rpe: Double): String {
    return if (rpe == rpe.toLong().toDouble()) {
        rpe.toLong().toString()
    } else {
        rpe.toString()
    }
}

private fun formatInstant(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoString
    }
}
