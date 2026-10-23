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
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.toCustomFood
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
        ScreenHeader(Icons.Default.Search, "Search Foods")
        Spacer(Modifier.height(16.dp))

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
        val food = info.toCustomFood()
        PortionEntryDialog(
            food = food,
            onDismiss = { selectedFoodInfo = null },
            onConfirm = { amount, mealType ->
                viewModel.logCustomFood(food, amount, mealType, selectedDate.toString())
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
