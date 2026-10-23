package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel

/**
 * Screen for searching foods from the Open Food Facts database.
 */
@Composable
fun SearchScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var selectedFoodInfo by remember { mutableStateOf<FoodInfo?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                viewModel.searchFoods(it)
            },
            label = { Text("Search foods (e.g. eggs, milk)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        if (isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (searchResults.isEmpty() && query.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { info ->
                    FoodInfoRow(info = info, onClick = { selectedFoodInfo = info })
                }
            }
        }
    }

    selectedFoodInfo?.let { info ->
        val food = CustomFood(
            barcode = info.barcode,
            name = info.name,
            kcalPer100 = info.kcalPer100,
            proteinPer100 = info.proteinPer100,
            carbsPer100 = info.carbsPer100,
            fatPer100 = info.fatPer100,
            fiberPer100 = info.fiberPer100,
            saltPer100 = info.saltPer100,
            unit = info.unit
        )
        PortionEntryDialog(
            food = food,
            onDismiss = { selectedFoodInfo = null },
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
                // Save to custom foods so it appears in Recents
                viewModel.saveCustomFood(food)
                selectedFoodInfo = null
            }
        )
    }
}

@Composable
fun FoodInfoRow(info: FoodInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(info.name, fontWeight = FontWeight.Bold)
                Text("${info.kcalPer100.toInt()} kcal per 100${info.unit}", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}
