package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodEntry
import java.time.LocalDate

@Composable
fun PortionEntryDialog(
    food: CustomFood,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("100") }
    var mealType by remember { mutableStateOf("Lunch") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${food.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroSummary(
                    kcal = food.kcalPer100,
                    protein = food.proteinPer100,
                    carbs = food.carbsPer100,
                    fat = food.fatPer100,
                    amount = amount.toDoubleOrNull() ?: 100.0,
                    unit = food.unit
                )

                Text("Meal Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                        FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type) })
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("How many ${food.unit} did you have?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(amount.toDoubleOrNull() ?: 100.0, mealType)
            }) { Text("Log Food") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditEntryDialog(
    entry: FoodEntry, 
    onDismiss: () -> Unit, 
    onUpdate: (FoodEntry) -> Unit, 
    onCopyToDate: (LocalDate) -> Unit
) {
    var amount by remember { mutableStateOf(entry.amount.toString()) }
    
    // Calculate per 100g values for the summary
    val ratio = 100.0 / entry.amount
    val kcalPer100 = entry.kcal * ratio
    val proteinPer100 = entry.protein * ratio
    val carbsPer100 = entry.carbs * ratio
    val fatPer100 = entry.fat * ratio

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry: ${entry.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroSummary(
                    kcal = kcalPer100,
                    protein = proteinPer100,
                    carbs = carbsPer100,
                    fat = fatPer100,
                    amount = amount.toDoubleOrNull() ?: entry.amount,
                    unit = entry.unit
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("How many ${entry.unit} did you have?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Quick Copy", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onCopyToDate(LocalDate.now().minusDays(1)); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Yesterday")
                    }
                    Button(onClick = { onCopyToDate(LocalDate.now().plusDays(1)); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Tomorrow")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newAmount = amount.toDoubleOrNull() ?: entry.amount
                val updateRatio = newAmount / entry.amount
                onUpdate(entry.copy(
                    amount = newAmount,
                    kcal = (entry.kcal * updateRatio).toInt(),
                    protein = entry.protein * updateRatio,
                    carbs = entry.carbs * updateRatio,
                    fat = entry.fat * updateRatio,
                    fiber = entry.fiber * updateRatio,
                    salt = entry.salt * updateRatio
                ))
                onDismiss()
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MacroSummary(
    kcal: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    amount: Double,
    unit: String
) {
    val ratio = amount / 100.0
    val currentKcal = (kcal * ratio).toInt()
    val currentProtein = protein * ratio
    val currentCarbs = carbs * ratio
    val currentFat = fat * ratio

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$currentKcal kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("for ${amount.toInt()}$unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroItem("Protein", currentProtein, Color(0xFFE57373))
            MacroItem("Carbs", currentCarbs, Color(0xFF81C784))
            MacroItem("Fat", currentFat, Color(0xFFFFD54F))
        }
    }
}

@Composable
fun MacroItem(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text("${value.toInt()}g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CustomFoodEntryDialog(
    barcode: String? = null,
    onDismiss: () -> Unit,
    onSave: (CustomFood) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (barcode != null) "Food Not Found. Create Custom?" else "Add New Custom Food") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (barcode != null) {
                    item { Text("Barcode: $barcode", style = MaterialTheme.typography.labelSmall) }
                }
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("P/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("C/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("F/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Fib/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Salt/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: $unit") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("g", "ml", "pcs").forEach { u ->
                                DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; expanded = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val food = CustomFood(
                    barcode = barcode,
                    name = name,
                    kcalPer100 = kcal.toDoubleOrNull() ?: 0.0,
                    proteinPer100 = protein.toDoubleOrNull() ?: 0.0,
                    carbsPer100 = carbs.toDoubleOrNull() ?: 0.0,
                    fatPer100 = fat.toDoubleOrNull() ?: 0.0,
                    fiberPer100 = fiber.toDoubleOrNull() ?: 0.0,
                    saltPer100 = salt.toDoubleOrNull() ?: 0.0,
                    unit = unit
                )
                onSave(food)
            }) { Text("Save to Database") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
