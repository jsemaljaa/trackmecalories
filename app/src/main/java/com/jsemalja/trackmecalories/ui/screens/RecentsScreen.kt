package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel

/**
 * Screen for one-tap logging of saved foods, split into recently-used and pinned favorites
 * so favorites don't get buried once Recents fills up.
 */
@Composable
fun RecentsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val recentFoods by viewModel.recentFoods.collectAsState()
    val favoriteFoods by viewModel.favoriteFoods.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedFood by remember { mutableStateOf<CustomFood?>(null) }
    var editingFood by remember { mutableStateOf<CustomFood?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(Icons.Default.History, "Saved Foods")

        Spacer(Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recent") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favorites") })
        }

        Spacer(Modifier.height(16.dp))

        val list = if (selectedTab == 0) recentFoods else favoriteFoods

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text(
                        if (selectedTab == 0) "No saved foods yet" else "No favorites yet",
                        color = Color.Gray
                    )
                    Text(
                        if (selectedTab == 0) "Scan or add foods to see them here" else "Tap the star on a saved food to pin it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        )
    }

    editingFood?.let { food ->
        EditCustomFoodDialog(
            food = food,
            onDismiss = { editingFood = null },
            onSave = { updated ->
                viewModel.updateCustomFood(updated)
                editingFood = null
            }
        )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.Bold)
                val refLabel = if (food.unit == "pcs") "pc" else "100${food.unit}"
                Text("${food.kcalPer100.toInt()} kcal per $refLabel", style = MaterialTheme.typography.bodySmall)
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
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}
