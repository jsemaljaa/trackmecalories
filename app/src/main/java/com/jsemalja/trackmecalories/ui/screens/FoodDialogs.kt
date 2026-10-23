package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import java.time.LocalDate

@Composable
fun PortionEntryDialog(
    food: CustomFood,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf(food.referenceAmount.toString()) }
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
                    amount = amount.toDoubleOrNull() ?: food.referenceAmount,
                    referenceAmount = food.referenceAmount,
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
                onConfirm(amount.toDoubleOrNull() ?: food.referenceAmount, mealType)
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

    // Back out the per-reference-amount values for the live summary preview.
    val ratio = entry.referenceAmount / entry.amount
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
                    referenceAmount = entry.referenceAmount,
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
    unit: String,
    referenceAmount: Double = 100.0
) {
    val ratio = amount / referenceAmount
    val currentKcal = (kcal * ratio).toInt()
    val currentProtein = protein * ratio
    val currentCarbs = carbs * ratio
    val currentFat = fat * ratio

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
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
            MacroItem("Protein", currentProtein, MacroProtein)
            MacroItem("Carbs", currentCarbs, MacroCarbs)
            MacroItem("Fat", currentFat, MacroFat)
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

/** "100g"/"100ml" for weight/volume units, or "1 pc" for piece-counted foods. */
private fun referenceLabel(unit: String): String = if (unit == "pcs") "1 pc" else "100$unit"

private fun referenceAmountFor(unit: String): Double = if (unit == "pcs") 1.0 else 100.0

@Composable
private fun UnitDropdown(unit: String, onUnitChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: $unit") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("g", "ml", "pcs").forEach { u ->
                DropdownMenuItem(text = { Text(u) }, onClick = { onUnitChange(u); expanded = false })
            }
        }
    }
}

@Composable
fun CustomFoodEntryDialog(
    barcode: String? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (CustomFood) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    val refLabel = referenceLabel(unit)

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
                        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("P/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("C/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("F/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Fib/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Salt/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item { UnitDropdown(unit = unit) { unit = it } }
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
                    unit = unit,
                    referenceAmount = referenceAmountFor(unit)
                )
                onSave(food)
            }) { Text("Save to Database") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Edits an already-saved [CustomFood] (e.g. fixing a typo'd macro) without having to
 * re-scan or re-search for it.
 */
@Composable
fun EditCustomFoodDialog(
    food: CustomFood,
    onDismiss: () -> Unit,
    onSave: (CustomFood) -> Unit
) {
    var name by remember { mutableStateOf(food.name) }
    var kcal by remember { mutableStateOf(food.kcalPer100.toString()) }
    var protein by remember { mutableStateOf(food.proteinPer100.toString()) }
    var carbs by remember { mutableStateOf(food.carbsPer100.toString()) }
    var fat by remember { mutableStateOf(food.fatPer100.toString()) }
    var fiber by remember { mutableStateOf(food.fiberPer100.toString()) }
    var salt by remember { mutableStateOf(food.saltPer100.toString()) }
    var unit by remember { mutableStateOf(food.unit) }
    val refLabel = referenceLabel(unit)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${food.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("P/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("C/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("F/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Fib/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Salt/$refLabel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item { UnitDropdown(unit = unit) { unit = it } }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    food.copy(
                        name = name,
                        kcalPer100 = kcal.toDoubleOrNull() ?: food.kcalPer100,
                        proteinPer100 = protein.toDoubleOrNull() ?: food.proteinPer100,
                        carbsPer100 = carbs.toDoubleOrNull() ?: food.carbsPer100,
                        fatPer100 = fat.toDoubleOrNull() ?: food.fatPer100,
                        fiberPer100 = fiber.toDoubleOrNull() ?: food.fiberPer100,
                        saltPer100 = salt.toDoubleOrNull() ?: food.saltPer100,
                        unit = unit,
                        referenceAmount = referenceAmountFor(unit)
                    )
                )
            }) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
