package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel

/**
 * Screen displaying frequently logged foods for one-tap entry.
 */
@Composable
fun RecentsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val recentFoods by viewModel.recentFoods.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var selectedFood by remember { mutableStateOf<CustomFood?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Recent Foods", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (recentFoods.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("No saved foods yet", color = Color.Gray)
                    Text("Scan or add foods to see them here", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentFoods) { food ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedFood = food },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, fontWeight = FontWeight.Bold)
                                Text("${food.kcalPer100.toInt()} kcal per 100${food.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        }
    }

    selectedFood?.let { food ->
        PortionEntryDialog(
            food = food,
            onDismiss = { selectedFood = null },
            onConfirm = { amount, mealType ->
                val ratio = amount / 100.0
                viewModel.addEntry(FoodEntry(
                    name = food.name,
                    kcal = (food.kcalPer100 * ratio).toInt(),
                    protein = food.proteinPer100 * ratio,
                    carbs = food.carbsPer100 * ratio,
                    fat = food.fatPer100 * ratio,
                    fiber = food.fiberPer100 * ratio,
                    salt = food.saltPer100 * ratio,
                    amount = amount,
                    unit = food.unit,
                    mealType = mealType,
                    date = selectedDate.toString()
                ))
                selectedFood = null
            }
        )
    }
}
