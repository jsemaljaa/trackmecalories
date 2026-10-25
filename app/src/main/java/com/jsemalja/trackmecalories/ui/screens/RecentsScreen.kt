package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.normalizedForSearch
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel

/**
 * Screen for one-tap logging of saved foods: recently used, pinned favorites, and every
 * saved food (so anything you've created can be found and edited, not just the latest 30).
 */
@Composable
fun RecentsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val recentFoods by viewModel.recentFoods.collectAsState()
    val favoriteFoods by viewModel.favoriteFoods.collectAsState()
    val allFoods by viewModel.allCustomFoods.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<CustomFood?>(null) }
    var editingFood by remember { mutableStateOf<CustomFood?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(Icons.Default.History, "Saved Foods")

        Spacer(Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recent") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favorites") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("All (${allFoods.size})") })
        }

        Spacer(Modifier.height(12.dp))

        if (selectedTab == 2) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Filter your foods") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(onClick = { filter = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear filter") }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        val list = when (selectedTab) {
            0 -> recentFoods
            1 -> favoriteFoods
            else -> {
                val needle = filter.normalizedForSearch()
                if (needle.isBlank()) allFoods
                else allFoods.filter { needle in it.name.normalizedForSearch() }
            }
        }

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text(
                        when (selectedTab) {
                            0 -> "No saved foods yet"
                            1 -> "No favorites yet"
                            else -> if (filter.isBlank()) "No saved foods yet" else "Nothing matches “${filter.trim()}”"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (selectedTab == 1) "Tap the star on a saved food to pin it here" else "Scan or add foods to see them here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(list, key = { it.id }) { food ->
                    SavedFoodRow(
                        food = food,
                        onClick = { selectedFood = food },
                        onEdit = { editingFood = food },
                        onToggleFavorite = { viewModel.toggleFavorite(food) }
                    )
                }
            }
        }
    }

    selectedFood?.let { food ->
        PortionEntryDialog(
            food = food,
            onDismiss = { selectedFood = null },
            onConfirm = { amount, mealType ->
                viewModel.logCustomFood(food, amount, mealType, selectedDate.toString())
                selectedFood = null
            },
            onEdit = {
                selectedFood = null
                editingFood = food
            }
        )
    }

    editingFood?.let { food ->
        EditSavedFoodDialog(viewModel, food, onDismiss = { editingFood = null })
    }
}

@Composable
fun SavedFoodRow(
    food: CustomFood,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val refLabel = if (food.unit == "pcs") "piece" else "100 ${food.unit}"
                Text(
                    "${food.kcalPer100.toInt()} kcal per $refLabel · P ${food.proteinPer100.toInt()} · C ${food.carbsPer100.toInt()} · F ${food.fatPer100.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (food.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (food.isFavorite) "Unpin from favorites" else "Pin to favorites",
                    tint = if (food.isFavorite) Color(0xFFFFC107) else LocalContentColor.current
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Add, contentDescription = "Log")
            }
        }
    }
}
