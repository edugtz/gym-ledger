package com.edu.gymledger.feature.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.edu.gymledger.domain.model.Food

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsScreen(
    modifier: Modifier = Modifier,
    viewModel: FoodsViewModel = viewModel(
        factory = FoodsViewModelFactory(AppContainer.foodRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var deleteTarget by remember { mutableStateOf<Food?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FoodsUiEvent.SaveSucceeded -> {
                    showBottomSheet = false
                    editingFood = null
                }
                is FoodsUiEvent.DeleteSucceeded -> {
                    deleteTarget = null
                }
                is FoodsUiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Foods") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingFood = null
                    showBottomSheet = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add food")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Saved foods for fast meal logging",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onClear = { viewModel.updateSearchQuery("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            FoodsSummary(
                count = uiState.foods.size,
                isSearching = uiState.searchQuery.isNotBlank(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.foods.isEmpty() && uiState.searchQuery.isBlank()) {
                EmptyFoodsState(
                    modifier = Modifier.fillMaxSize(),
                    onAddFood = {
                        editingFood = null
                        showBottomSheet = true
                    }
                )
            } else if (uiState.foods.isEmpty() && uiState.searchQuery.isNotBlank()) {
                SearchEmptyState(
                    query = uiState.searchQuery,
                    modifier = Modifier.fillMaxSize(),
                    onClearSearch = { viewModel.updateSearchQuery("") },
                    onAddFood = {
                        editingFood = null
                        showBottomSheet = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.foods, key = { it.id }) { food ->
                        FoodCard(
                            food = food,
                            onEdit = {
                                editingFood = food
                                showBottomSheet = true
                            },
                            onDelete = { deleteTarget = food }
                        )
                    }

                    item {
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        FoodFormSheet(
            food = editingFood,
            onDismiss = {
                showBottomSheet = false
                editingFood = null
            },
            onSave = { name, calories, servingSize, protein, carbs, fat ->
                if (editingFood != null) {
                    viewModel.updateFood(
                        food = editingFood!!,
                        name = name,
                        caloriesStr = calories,
                        servingSizeStr = servingSize,
                        proteinStr = protein,
                        carbsStr = carbs,
                        fatStr = fat
                    )
                } else {
                    viewModel.addFood(
                        name = name,
                        caloriesStr = calories,
                        servingSizeStr = servingSize,
                        proteinStr = protein,
                        carbsStr = carbs,
                        fatStr = fat
                    )
                }
            }
        )
    }

    deleteTarget?.let { food ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete food?") },
            text = { Text("\"${food.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFood(food)
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search foods") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun FoodsSummary(
    count: Int,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    val text = if (isSearching) {
        "$count food${if (count != 1) "s" else ""} found"
    } else {
        "$count food${if (count != 1) "s" else ""} saved"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun EmptyFoodsState(
    modifier: Modifier = Modifier,
    onAddFood: () -> Unit
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
            text = "No foods yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your first food to make meal logging faster.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onAddFood) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add food")
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    modifier: Modifier = Modifier,
    onClearSearch: () -> Unit,
    onAddFood: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No foods found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try a different search or add this food.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClearSearch) {
                Text("Clear search")
            }
            Button(onClick = onAddFood) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add food")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodCard(
    food: Food,
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatCaloriesAndServing(food),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MacroChip(label = "Protein", value = food.proteinPerServing)
                MacroChip(label = "Carbs", value = food.carbsPerServing)
                MacroChip(label = "Fat", value = food.fatPerServing)
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: Double) {
    val displayValue = if (value == value.toLong().toDouble()) {
        "${value.toLong()}g"
    } else {
        "${"%.1f".format(value)}g"
    }

    Text(
        text = "$label $displayValue",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun formatCaloriesAndServing(food: Food): String {
    val calories = "${food.caloriesPerServing} kcal"
    return if (food.servingSize != null) {
        val servingDisplay = if (food.servingSize == food.servingSize.toLong().toDouble()) {
            "${food.servingSize.toLong()} g"
        } else {
            "${"%.1f".format(food.servingSize)} g"
        }
        "$calories · $servingDisplay"
    } else {
        calories
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodFormSheet(
    food: Food?,
    onDismiss: () -> Unit,
    onSave: (name: String, calories: String, servingSize: String, protein: String, carbs: String, fat: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val isEdit = food != null

    var name by rememberSaveable { mutableStateOf(food?.name ?: "") }
    var calories by rememberSaveable { mutableStateOf(food?.caloriesPerServing?.toString() ?: "") }
    var servingSize by rememberSaveable { mutableStateOf(food?.servingSize?.toString() ?: "") }
    var protein by rememberSaveable { mutableStateOf(food?.proteinPerServing?.toString() ?: "") }
    var carbs by rememberSaveable { mutableStateOf(food?.carbsPerServing?.toString() ?: "") }
    var fat by rememberSaveable { mutableStateOf(food?.fatPerServing?.toString() ?: "") }

    var optionalExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(food) {
        optionalExpanded = isEdit && (food?.servingSize != null ||
                (food?.proteinPerServing ?: 0.0) > 0.0 ||
                (food?.carbsPerServing ?: 0.0) > 0.0 ||
                (food?.fatPerServing ?: 0.0) > 0.0)
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
                text = if (isEdit) "Edit food" else "Add food",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories per serving") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Optional nutrition details",
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
                        value = servingSize,
                        onValueChange = { servingSize = it },
                        label = { Text("Serving size (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }

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
                    onClick = {
                        focusManager.clearFocus()
                        onSave(name, calories, servingSize, protein, carbs, fat)
                    }
                ) {
                    Text(if (isEdit) "Save changes" else "Save food")
                }
            }
        }
    }
}
