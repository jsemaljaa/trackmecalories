package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime

private val MEAL_TYPES = listOf("Breakfast", "Lunch", "Dinner", "Snack")

/** Sensible meal preselection for "right now", so the common case needs no tap. */
private fun mealForNow(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 4..10 -> "Breakfast"
    in 11..14 -> "Lunch"
    in 17..21 -> "Dinner"
    else -> "Snack"
}

/** "1" instead of "1.0", "12.5" stays "12.5" — for prefilled text fields and labels. */
fun Double.clean(): String =
    if (this == Math.floor(this) && !isInfinite()) toLong().toString()
    else "%.2f".format(java.util.Locale.US, this).trimEnd('0').trimEnd('.')

/** Accepts both "1.5" and the Czech-keyboard "1,5". */
fun String.toDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortionEntryDialog(
    food: CustomFood,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
    // Shown for already-saved foods, so a wrong macro can be fixed right where it's noticed.
    onEdit: (() -> Unit)? = null
) {
    var amount by remember { mutableStateOf(food.referenceAmount.clean()) }
    var mealType by remember { mutableStateOf(mealForNow()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.Top) {
                Text("Log ${food.name}", modifier = Modifier.weight(1f))
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit food")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroSummary(
                    kcal = food.kcalPer100,
                    protein = food.proteinPer100,
                    carbs = food.carbsPer100,
                    fat = food.fatPer100,
                    amount = amount.toDecimal() ?: food.referenceAmount,
                    referenceAmount = food.referenceAmount,
                    unit = food.unit
                )

                Text("Meal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                // Wraps instead of pushing "Snack" off the edge on narrow phones.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MEAL_TYPES.forEach { type ->
                        FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type) })
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (food.unit == "pcs") "How many pieces?" else "How many ${food.unit}?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(amount.toDecimal() ?: food.referenceAmount, mealType)
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
    onCopyToDate: (LocalDate) -> Unit,
    onDelete: () -> Unit
) {
    var amount by remember { mutableStateOf(entry.amount.clean()) }

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
                    amount = amount.toDecimal() ?: entry.amount,
                    referenceAmount = entry.referenceAmount,
                    unit = entry.unit
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (entry.unit == "pcs") "How many pieces?" else "How many ${entry.unit}?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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

                // Undo is offered on Home right after, so no extra confirmation step here.
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete from log")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newAmount = amount.toDecimal() ?: entry.amount
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
    val amountLabel = if (unit == "pcs") {
        "${amount.clean()} ${if (amount == 1.0) "piece" else "pieces"}"
    } else {
        "${amount.clean()} $unit"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$currentKcal kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("for $amountLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Values are per ${referenceLabel(unit)} · change unit")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("g" to "per 100 g", "ml" to "per 100 ml", "pcs" to "per piece").forEach { (u, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onUnitChange(u); expanded = false })
            }
        }
    }
}

/** Number field for one nutrient, labelled with the unit it's relative to. */
@Composable
private fun RowScope.NutrientField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.weight(1f),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
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
                item { UnitDropdown(unit = unit) { unit = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField("kcal/$refLabel", kcal) { kcal = it }
                        NutrientField("P/$refLabel", protein) { protein = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField("C/$refLabel", carbs) { carbs = it }
                        NutrientField("F/$refLabel", fat) { fat = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField("Fib/$refLabel", fiber) { fiber = it }
                        NutrientField("Salt/$refLabel", salt) { salt = it }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val food = CustomFood(
                    barcode = barcode,
                    name = name,
                    kcalPer100 = kcal.toDecimal() ?: 0.0,
                    proteinPer100 = protein.toDecimal() ?: 0.0,
                    carbsPer100 = carbs.toDecimal() ?: 0.0,
                    fatPer100 = fat.toDecimal() ?: 0.0,
                    fiberPer100 = fiber.toDecimal() ?: 0.0,
                    saltPer100 = salt.toDecimal() ?: 0.0,
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
 *
 * @param loggedEntryCount how many log entries came from this food; when > 0 the user can
 *   choose to have those recalculated with the corrected values too.
 */
@Composable
fun EditCustomFoodDialog(
    food: CustomFood,
    loggedEntryCount: Int,
    onDismiss: () -> Unit,
    onSave: (updated: CustomFood, updateLoggedEntries: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(food.name) }
    var kcal by remember { mutableStateOf(food.kcalPer100.clean()) }
    var protein by remember { mutableStateOf(food.proteinPer100.clean()) }
    var carbs by remember { mutableStateOf(food.carbsPer100.clean()) }
    var fat by remember { mutableStateOf(food.fatPer100.clean()) }
    var fiber by remember { mutableStateOf(food.fiberPer100.clean()) }
    var salt by remember { mutableStateOf(food.saltPer100.clean()) }
    var unit by remember { mutableStateOf(food.unit) }
    var updateLogged by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val refLabel = referenceLabel(unit)
    // Logged amounts are in the old unit, so they can only be rescaled if it's unchanged.
    val canUpdateLogged = loggedEntryCount > 0 && unit == food.unit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit food") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth()) }
                item { UnitDropdown(unit = unit) { unit = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField("kcal/$refLabel", kcal) { kcal = it }
                        NutrientField("P/$refLabel", protein) { protein = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField("C/$refLabel", carbs) { carbs = it }
                        NutrientField("F/$refLabel", fat) { fat = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField("Fib/$refLabel", fiber) { fiber = it }
                        NutrientField("Salt/$refLabel", salt) { salt = it }
                    }
                }
                if (loggedEntryCount > 0) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable(enabled = canUpdateLogged) { updateLogged = !updateLogged }
                        ) {
                            Checkbox(
                                checked = updateLogged && canUpdateLogged,
                                onCheckedChange = { updateLogged = it },
                                enabled = canUpdateLogged
                            )
                            Text(
                                if (canUpdateLogged) {
                                    "Also fix the $loggedEntryCount logged ${if (loggedEntryCount == 1) "entry" else "entries"} of this food"
                                } else {
                                    "Logged entries keep their values when the unit changes"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete this saved food") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    food.copy(
                        name = name.trim().ifBlank { food.name },
                        kcalPer100 = kcal.toDecimal() ?: food.kcalPer100,
                        proteinPer100 = protein.toDecimal() ?: food.proteinPer100,
                        carbsPer100 = carbs.toDecimal() ?: food.carbsPer100,
                        fatPer100 = fat.toDecimal() ?: food.fatPer100,
                        fiberPer100 = fiber.toDecimal() ?: food.fiberPer100,
                        saltPer100 = salt.toDecimal() ?: food.saltPer100,
                        unit = unit,
                        referenceAmount = referenceAmountFor(unit)
                    ),
                    updateLogged && canUpdateLogged
                )
            }) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${food.name}?") },
            text = { Text("It's removed from your saved foods. Anything you already logged stays in your log.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

/**
 * [EditCustomFoodDialog] wired to the view model: looks up how many entries were logged
 * from the food and applies the save/delete. Every screen that edits saved foods uses this.
 */
@Composable
fun EditSavedFoodDialog(viewModel: MainViewModel, food: CustomFood, onDismiss: () -> Unit) {
    var loggedCount by remember(food.id) { mutableIntStateOf(0) }
    LaunchedEffect(food.id) { loggedCount = viewModel.countLoggedEntries(food) }

    EditCustomFoodDialog(
        food = food,
        loggedEntryCount = loggedCount,
        onDismiss = onDismiss,
        onSave = { updated, updateLogged ->
            viewModel.updateCustomFood(food, updated, updateLogged)
            onDismiss()
        },
        onDelete = {
            viewModel.deleteCustomFood(food)
            onDismiss()
        }
    )
}
