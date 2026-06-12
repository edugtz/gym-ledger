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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.edu.gymledger.domain.model.Routine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onNavigateToRoutineDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = viewModel(
        factory = RoutinesViewModelFactory(AppContainer.routineRepository)
    )
) {
    val routines by viewModel.routines.collectAsState()
    val error by viewModel.error.collectAsState()
    val deleteTarget by viewModel.deleteTarget.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val newRoutineName by viewModel.newRoutineName.collectAsState()
    val newRoutineDescription by viewModel.newRoutineDescription.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create routine"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (routines.isEmpty()) {
                EmptyRoutinesState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(routines, key = { it.id }) { routine ->
                        RoutineCard(
                            routine = routine,
                            onClick = { onNavigateToRoutineDetail(routine.id) },
                            onDelete = { viewModel.requestDelete(routine) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateDialog() },
            title = { Text("Create routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newRoutineName,
                        onValueChange = viewModel::updateNewRoutineName,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRoutineDescription,
                        onValueChange = viewModel::updateNewRoutineDescription,
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createRoutine() }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCreateDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteTarget?.let { routine ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete routine?") },
            text = {
                Text(
                    "This removes the routine and its exercise list. Your exercise catalog will not be deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRoutine() }) {
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
private fun EmptyRoutinesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
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
            text = "No routines yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create a routine to organize your exercises into workout plans.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineCard(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete routine",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            routine.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
