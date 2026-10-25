package com.jsemalja.trackmecalories.ui.screens

import com.jsemalja.trackmecalories.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.jsemalja.trackmecalories.data.Confidence
import com.jsemalja.trackmecalories.data.CustomFood
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Visibility
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroFiber
import kotlin.math.roundToInt
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime

private val MEAL_TYPES = listOf("Breakfast", "Lunch", "Dinner", "Snack")

/** Meal types are stored in English (the database key); this is the display name. */
@Composable
fun mealLabel(key: String): String = when (key) {
    "Breakfast" -> stringResource(R.string.meal_breakfast)
    "Lunch" -> stringResource(R.string.meal_lunch)
    "Dinner" -> stringResource(R.string.meal_dinner)
    "Snack" -> stringResource(R.string.meal_snack)
    "Quick Add" -> stringResource(R.string.meal_quick_add)
    else -> key
}

/** Display name of a stored unit key ("g", "ml", "pcs"). */
@Composable
fun unitLabel(unit: String): String = when (unit) {
    "g" -> stringResource(R.string.unit_g)
    "ml" -> stringResource(R.string.unit_ml)
    "pcs" -> stringResource(R.string.unit_pcs)
    else -> unit
}

/** "1 piece" / "2 pieces" (or the localized plural), "150 g" otherwise. */
@Composable
fun amountLabel(amount: Double, unit: String): String =
    if (unit == "pcs") {
        val quantity = if (amount == Math.floor(amount)) amount.toInt() else 2
        pluralStringResource(R.plurals.pieces, quantity, amount.clean())
    } else {
        "${amount.clean()} ${unitLabel(unit)}"
    }

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
    onConfirm: (Double, String, Confidence) -> Unit,
    // Shown for already-saved foods, so a wrong macro can be fixed right where it's noticed.
    onEdit: (() -> Unit)? = null,
    // Ingredient mode (recipes): no meal choice, and a different confirm label.
    showMeal: Boolean = true,
    confirmLabel: String? = null
) {
    // The package size for a scanned product, then whatever was logged last time.
    var amount by remember { mutableStateOf((food.defaultAmount ?: food.referenceAmount).clean()) }
    var confidence by remember { mutableStateOf(Confidence.ESTIMATED) }
    var mealType by remember { mutableStateOf(mealForNow()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.Top) {
                Text(if (showMeal) stringResource(R.string.log_food_title, food.name) else food.name, modifier = Modifier.weight(1f))
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_food))
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
                    fiber = food.fiberPer100,
                    amount = amount.toDecimal() ?: food.referenceAmount,
                    referenceAmount = food.referenceAmount,
                    unit = food.unit
                )
                RatingLine(food)

                if (showMeal) {
                    Text(stringResource(R.string.meal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    // Wraps instead of pushing "Snack" off the edge on narrow phones.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MEAL_TYPES.forEach { type ->
                            FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(mealLabel(type)) })
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (food.unit == "pcs") stringResource(R.string.how_many_pieces) else stringResource(R.string.how_many_unit, unitLabel(food.unit))) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // One-tap amounts: the usual portion and the reference amount.
                val usual = food.defaultAmount
                if (usual != null && usual != food.referenceAmount) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { amount = usual.clean() }, label = { Text(stringResource(R.string.amount_usual, amountLabel(usual, food.unit))) })
                        AssistChip(onClick = { amount = food.referenceAmount.clean() }, label = { Text(amountLabel(food.referenceAmount, food.unit)) })
                    }
                }

                if (showMeal) ConfidenceChips(confidence) { confidence = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(amount.toDecimal() ?: food.defaultAmount ?: food.referenceAmount, mealType, confidence)
            }) { Text(confirmLabel ?: stringResource(R.string.log_food)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
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
    var confidence by remember { mutableStateOf(Confidence.from(entry.confidence)) }

    // Back out the per-reference-amount values for the live summary preview.
    val ratio = entry.referenceAmount / entry.amount
    val kcalPer100 = entry.kcal * ratio
    val proteinPer100 = entry.protein * ratio
    val carbsPer100 = entry.carbs * ratio
    val fatPer100 = entry.fat * ratio
    val fiberPer100 = entry.fiber * ratio

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_entry_title, entry.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroSummary(
                    kcal = kcalPer100,
                    protein = proteinPer100,
                    carbs = carbsPer100,
                    fat = fatPer100,
                    fiber = fiberPer100,
                    amount = amount.toDecimal() ?: entry.amount,
                    referenceAmount = entry.referenceAmount,
                    unit = entry.unit
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (entry.unit == "pcs") stringResource(R.string.how_many_pieces) else stringResource(R.string.how_many_unit, unitLabel(entry.unit))) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ConfidenceChips(confidence) { confidence = it }

                Text(stringResource(R.string.quick_copy), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onCopyToDate(LocalDate.now().minusDays(1)); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.copy_yesterday))
                    }
                    Button(onClick = { onCopyToDate(LocalDate.now().plusDays(1)); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.copy_tomorrow))
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
                    Text(stringResource(R.string.delete_from_log))
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
                    salt = entry.salt * updateRatio,
                    confidence = confidence.name
                ))
                onDismiss()
            }) { Text(stringResource(R.string.update)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun MacroSummary(
    kcal: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    fiber: Double,
    amount: Double,
    unit: String,
    referenceAmount: Double = 100.0
) {
    val ratio = amount / referenceAmount
    val currentKcal = (kcal * ratio).toInt()
    val currentProtein = protein * ratio
    val currentCarbs = carbs * ratio
    val currentFat = fat * ratio
    val currentFiber = fiber * ratio
    val amountText = amountLabel(amount, unit)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$currentKcal ${stringResource(R.string.unit_kcal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.for_amount, amountText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroItem(stringResource(R.string.protein), currentProtein, MacroProtein)
            MacroItem(stringResource(R.string.carbs), currentCarbs, MacroCarbs)
            MacroItem(stringResource(R.string.fat), currentFat, MacroFat)
            MacroItem(stringResource(R.string.fiber), currentFiber, MacroFiber)
        }
    }
}

@Composable
fun MacroItem(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        // One decimal below 10 g, so 2.5 g of fiber doesn't show as "2 g".
        val grams = if (value < 10) "%.1f".format(value).removeSuffix(".0").removeSuffix(",0") else value.roundToInt().toString()
        Text("$grams ${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

/** "100 g"/"100 ml" for weight/volume units, or "1 piece" for piece-counted foods. */
@Composable
private fun referenceLabel(unit: String): String =
    if (unit == "pcs") stringResource(R.string.per_one_piece) else "100 ${unitLabel(unit)}"

private fun referenceAmountFor(unit: String): Double = if (unit == "pcs") 1.0 else 100.0

@Composable
private fun UnitDropdown(unit: String, onUnitChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.values_per_unit, referenceLabel(unit)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("g" to R.string.per_100g, "ml" to R.string.per_100ml, "pcs" to R.string.per_piece).forEach { (u, label) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onUnitChange(u); expanded = false })
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
    var sugar by remember { mutableStateOf("") }
    var satFat by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    val refLabel = referenceLabel(unit)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (barcode != null) R.string.food_not_found_create else R.string.add_custom_food)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (barcode != null) {
                    item { Text(stringResource(R.string.barcode_value, barcode), style = MaterialTheme.typography.labelSmall) }
                }
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.food_name)) }, modifier = Modifier.fillMaxWidth()) }
                item { UnitDropdown(unit = unit) { unit = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_kcal_per, refLabel), kcal) { kcal = it }
                        NutrientField(stringResource(R.string.nutrient_p_per, refLabel), protein) { protein = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_c_per, refLabel), carbs) { carbs = it }
                        NutrientField(stringResource(R.string.nutrient_f_per, refLabel), fat) { fat = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_fib_per, refLabel), fiber) { fiber = it }
                        NutrientField(stringResource(R.string.nutrient_salt_per, refLabel), salt) { salt = it }
                    }
                }
                // Optional: with these, the food gets a Nutri-Score.
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_sugar_per, refLabel), sugar) { sugar = it }
                        NutrientField(stringResource(R.string.nutrient_satfat_per, refLabel), satFat) { satFat = it }
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
                    sugarPer100 = sugar.toDecimal(),
                    saturatedFatPer100 = satFat.toDecimal(),
                    unit = unit,
                    referenceAmount = referenceAmountFor(unit)
                )
                onSave(food)
            }) { Text(stringResource(R.string.save_food)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
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
    var sugar by remember { mutableStateOf(food.sugarPer100?.clean() ?: "") }
    var satFat by remember { mutableStateOf(food.saturatedFatPer100?.clean() ?: "") }
    var unit by remember { mutableStateOf(food.unit) }
    var updateLogged by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val refLabel = referenceLabel(unit)
    // Logged amounts are in the old unit, so they can only be rescaled if it's unchanged.
    val canUpdateLogged = loggedEntryCount > 0 && unit == food.unit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_food)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.food_name)) }, modifier = Modifier.fillMaxWidth()) }
                item { UnitDropdown(unit = unit) { unit = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_kcal_per, refLabel), kcal) { kcal = it }
                        NutrientField(stringResource(R.string.nutrient_p_per, refLabel), protein) { protein = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_c_per, refLabel), carbs) { carbs = it }
                        NutrientField(stringResource(R.string.nutrient_f_per, refLabel), fat) { fat = it }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_fib_per, refLabel), fiber) { fiber = it }
                        NutrientField(stringResource(R.string.nutrient_salt_per, refLabel), salt) { salt = it }
                    }
                }
                // Optional: with these, the food gets a Nutri-Score.
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientField(stringResource(R.string.nutrient_sugar_per, refLabel), sugar) { sugar = it }
                        NutrientField(stringResource(R.string.nutrient_satfat_per, refLabel), satFat) { satFat = it }
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
                                    pluralStringResource(R.plurals.also_fix_entries, loggedEntryCount, loggedEntryCount)
                                } else {
                                    stringResource(R.string.unit_change_keeps_entries)
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
                    ) { Text(stringResource(R.string.delete_saved_food)) }
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
                        sugarPer100 = sugar.toDecimal(),
                        saturatedFatPer100 = satFat.toDecimal(),
                        unit = unit,
                        referenceAmount = referenceAmountFor(unit)
                    ),
                    updateLogged && canUpdateLogged
                )
            }) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_food_title, food.name)) },
            text = { Text(stringResource(R.string.delete_food_body)) },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
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

/**
 * How sure the amount is. Feeds the ± band on daily and weekly totals: a weighed portion
 * is close to exact, a guessed restaurant plate can be a third off either way.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfidenceChips(selected: Confidence, onSelect: (Confidence) -> Unit) {
    Column {
        Text(stringResource(R.string.confidence_question), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Confidence.entries.forEach { c ->
                FilterChip(
                    selected = selected == c,
                    onClick = { onSelect(c) },
                    label = { Text(stringResource(c.label)) },
                    leadingIcon = { Icon(confidenceIcon(c), contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
        Text(
            stringResource(R.string.confidence_hint, (selected.relativeError * 100).roundToInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun confidenceIcon(c: Confidence) = when (c) {
    Confidence.WEIGHED -> Icons.Outlined.Scale
    Confidence.ESTIMATED -> Icons.Outlined.Visibility
    Confidence.GUESSED -> Icons.Outlined.HelpOutline
}
