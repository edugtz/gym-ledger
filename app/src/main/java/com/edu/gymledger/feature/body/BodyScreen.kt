package com.edu.gymledger.feature.body

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.gymledger.app.AppContainer
import com.edu.gymledger.domain.model.BodyMeasurement
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    modifier: Modifier = Modifier,
    viewModel: BodyViewModel = viewModel(
        factory = BodyViewModelFactory(AppContainer.bodyMeasurementRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var editingMeasurement by remember { mutableStateOf<BodyMeasurement?>(null) }
    var deleteTarget by remember { mutableStateOf<BodyMeasurement?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BodyUiEvent.SaveSucceeded -> {
                    showBottomSheet = false
                    editingMeasurement = null
                }
                is BodyUiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Body") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMeasurement = null
                    showBottomSheet = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log weight")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (uiState.measurements.isEmpty()) {
            EmptyBodyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onLogWeight = {
                    editingMeasurement = null
                    showBottomSheet = true
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    LatestWeightCard(measurement = uiState.latestMeasurement)
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(uiState.measurements, key = { it.id }) { measurement ->
                    HistoryCard(
                        measurement = measurement,
                        onEdit = {
                            editingMeasurement = measurement
                            showBottomSheet = true
                        },
                        onDelete = { deleteTarget = measurement }
                    )
                }

                item {
                    Spacer(Modifier.height(96.dp))
                }
            }
        }
    }

    if (showBottomSheet) {
        BodyMeasurementSheet(
            measurement = editingMeasurement,
            onDismiss = {
                showBottomSheet = false
                editingMeasurement = null
            },
            onSave = { date, weight, waist, chest, arm, thigh, hip, notes ->
                if (editingMeasurement != null) {
                    viewModel.updateMeasurement(
                        measurement = editingMeasurement!!,
                        date = date,
                        weightStr = weight,
                        waistStr = waist,
                        chestStr = chest,
                        armStr = arm,
                        thighStr = thigh,
                        hipStr = hip,
                        notes = notes
                    )
                } else {
                    viewModel.addMeasurement(
                        date = date,
                        weightStr = weight,
                        waistStr = waist,
                        chestStr = chest,
                        armStr = arm,
                        thighStr = thigh,
                        hipStr = hip,
                        notes = notes
                    )
                }
            }
        )
    }

    deleteTarget?.let { measurement ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete measurement?") },
            text = { Text("This body measurement will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMeasurement(measurement)
                        deleteTarget = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyBodyState(
    modifier: Modifier = Modifier,
    onLogWeight: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No body measurements yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Log your first weight entry to start tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogWeight) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log weight")
        }
    }
}

@Composable
private fun LatestWeightCard(measurement: BodyMeasurement?) {
    val hasWeight = measurement?.weight != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Latest weight",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            if (hasWeight) {
                Text(
                    text = "${measurement!!.weight} kg",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = measurement.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = "No weight recorded",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(
    measurement: BodyMeasurement,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (measurement.weight != null) {
                            Text(
                                text = "${measurement.weight} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "No weight",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = measurement.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            val optionalMetrics = buildList {
                measurement.waist?.let { add("Waist ${it}") }
                measurement.chest?.let { add("Chest ${it}") }
                measurement.arm?.let { add("Arm ${it}") }
                measurement.thigh?.let { add("Thigh ${it}") }
                measurement.hip?.let { add("Hip ${it}") }
            }
            if (optionalMetrics.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val visible = optionalMetrics.take(3)
                val overflow = optionalMetrics.size - visible.size
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    visible.forEach { metric ->
                        MetricChip(text = metric)
                    }
                    if (overflow > 0) {
                        MetricChip(text = "+$overflow more")
                    }
                }
            }

            if (!measurement.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = measurement.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyMeasurementSheet(
    measurement: BodyMeasurement?,
    onDismiss: () -> Unit,
    onSave: (date: String, weight: String, waist: String, chest: String, arm: String, thigh: String, hip: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val isEdit = measurement != null

    var date by rememberSaveable { mutableStateOf(measurement?.date ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var weight by rememberSaveable { mutableStateOf(measurement?.weight?.toString() ?: "") }
    var waist by rememberSaveable { mutableStateOf(measurement?.waist?.toString() ?: "") }
    var chest by rememberSaveable { mutableStateOf(measurement?.chest?.toString() ?: "") }
    var arm by rememberSaveable { mutableStateOf(measurement?.arm?.toString() ?: "") }
    var thigh by rememberSaveable { mutableStateOf(measurement?.thigh?.toString() ?: "") }
    var hip by rememberSaveable { mutableStateOf(measurement?.hip?.toString() ?: "") }
    var notes by rememberSaveable { mutableStateOf(measurement?.notes ?: "") }

    var optionalExpanded by rememberSaveable {
        mutableStateOf(
            isEdit && (measurement?.waist != null || measurement?.chest != null ||
                    measurement?.arm != null || measurement?.thigh != null || measurement?.hip != null)
        )
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
                text = if (isEdit) "Edit body measurement" else "Log body measurement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Optional measurements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { optionalExpanded = !optionalExpanded }
                ) {
                    Icon(
                        imageVector = if (optionalExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (optionalExpanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = optionalExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    OutlinedTextField(
                        value = waist,
                        onValueChange = { waist = it },
                        label = { Text("Waist (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = chest,
                        onValueChange = { chest = it },
                        label = { Text("Chest (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = arm,
                        onValueChange = { arm = it },
                        label = { Text("Arm (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = thigh,
                        onValueChange = { thigh = it },
                        label = { Text("Thigh (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hip,
                        onValueChange = { hip = it },
                        label = { Text("Hip (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
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
                androidx.compose.material3.Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSave(date, weight, waist, chest, arm, thigh, hip, notes)
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
