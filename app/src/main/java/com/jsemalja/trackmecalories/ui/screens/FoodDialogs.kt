package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${food.kcalPer100.toInt()} kcal per 100${food.unit}", style = MaterialTheme.typography.bodySmall)
                
                Text("Meal Type", style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                        FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type) })
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount consumed (${food.unit})") },
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
