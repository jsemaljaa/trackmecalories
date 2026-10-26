package com.jsemalja.trackmecalories.ui.screens

import com.jsemalja.trackmecalories.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.Confidence
import com.jsemalja.trackmecalories.data.CustomFood
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Visibility
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.Portion
import com.jsemalja.trackmecalories.data.initialPortion
import com.jsemalja.trackmecalories.data.portionList
import com.jsemalja.trackmecalories.data.LOG_UNITS
import com.jsemalja.trackmecalories.data.WEIGHT_UNITS
import com.jsemalja.trackmecalories.data.amountIn
import com.jsemalja.trackmecalories.data.logUnit
import com.jsemalja.trackmecalories.data.unitGrams
import com.jsemalja.trackmecalories.data.unitPortion
import com.jsemalja.trackmecalories.data.usesPieceWeight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroFiber
import kotlin.math.roundToInt
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.jsemalja.trackmecalories.data.stepAmount
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors

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

/** Short name of a [LOG_UNITS] key, for the log sheet's unit picker ("pcs", "g", "tsp"). */
@Composable
fun logUnitLabel(key: String): String = when (key) {
    "piece" -> stringResource(R.string.unit_pcs)
    "tsp" -> stringResource(R.string.unit_tsp)
    "tbsp" -> stringResource(R.string.unit_tbsp)
    else -> unitLabel(key)
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

/** Display name of a stored portion key ("medium", "slice", see [Portion]). */
@Composable
fun portionLabel(key: String): String = when (key) {
    "small" -> stringResource(R.string.portion_small)
    "medium" -> stringResource(R.string.portion_medium)
    "large" -> stringResource(R.string.portion_large)
    "extra_large" -> stringResource(R.string.portion_extra_large)
    "piece" -> stringResource(R.string.portion_piece)
    "slice" -> stringResource(R.string.portion_slice)
    "clove" -> stringResource(R.string.portion_clove)
    "tbsp" -> stringResource(R.string.portion_tbsp)
    "tsp" -> stringResource(R.string.portion_tsp)
    "glass" -> stringResource(R.string.portion_glass)
    "cup" -> stringResource(R.string.portion_cup)
    else -> key
}

/** "Medium · 51 g" */
@Composable
fun portionWithWeight(portion: Portion, unit: String): String =
    stringResource(R.string.portion_with_weight, portionLabel(portion.key), amountLabel(portion.grams, unit))

/** "2 × Medium (102 g)" for an entry logged in portions, "150 g" of a burger, else the plain amount. */
@Composable
fun entryAmountLabel(entry: FoodEntry): String {
    val grams = entry.portionGrams
    val key = entry.portion
    return if (key in WEIGHT_UNITS && grams != null && grams > 0) {
        amountLabel(entry.amount / grams, key!!)
    } else if (key != null && grams != null && grams > 0) {
        stringResource(R.string.portion_amount, (entry.amount / grams).clean(), portionLabel(key), amountLabel(entry.amount, entry.unit))
    } else {
        amountLabel(entry.amount, entry.unit)
    }
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

/**
 * The log sheet: how much, which meal, how sure. Everything is preselected (last time's
 * portion, the meal for this time of day), so the common case is one tap on the button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortionEntryDialog(
    food: CustomFood,
    onDismiss: () -> Unit,
    // The food (with any piece weight entered here), amount in the food's unit, meal,
    // confidence, and the portion or unit the amount was entered as (null for the food's own unit).
    onConfirm: (CustomFood, Double, String, Confidence, Portion?) -> Unit,
    // Shown for already-saved foods, so a wrong macro can be fixed right where it's noticed.
    onEdit: (() -> Unit)? = null,
    // Whether the food is pinned to favorites; null hides the star (unsaved foods, recipes' ingredients).
    favorite: Boolean? = null,
    onFavoriteChange: (Boolean) -> Unit = {},
    // Ingredient mode (recipes): no meal choice, and a different confirm label.
    showMeal: Boolean = true,
    confirmLabel: String? = null
) {
    // Natural portions ("1 medium egg"); the unit picker covers pieces and spoons.
    val sizes = remember(food) { food.portionList.filter { it.key !in LOG_UNITS } }
    val initial = remember(food) { initialPortion(food) }
    // What the amount counts: one of [LOG_UNITS] or of [sizes]. Starts from last time's
    // portion, or the food's usual one.
    var measure by remember { mutableStateOf(initial?.first?.key ?: food.logUnit) }
    // Typed here when the food doesn't know it yet; needed to turn pieces into weight.
    var pieceWeight by remember(food) { mutableStateOf(food.pieceGrams?.clean().orEmpty()) }
    val pieceGrams = pieceWeight.toDecimal()?.takeIf { it > 0 }
    fun portionOf(key: String): Portion? =
        if (key == food.logUnit) null else sizes.firstOrNull { it.key == key } ?: unitPortion(food, key, pieceGrams)
    val portion = portionOf(measure)
    val missingWeight = measure != food.logUnit && portion == null
    val counting = measure !in WEIGHT_UNITS
    // A count of [measure], or else the package size for a scanned product, then whatever
    // was logged last time.
    var amount by remember { mutableStateOf(initial?.second?.clean() ?: (food.defaultAmount ?: food.referenceAmount).clean()) }
    // What's in the field, in the food's unit.
    val enteredAmount = if (missingWeight) null else amount.toDecimal()?.let { n -> portion?.let { n * it.grams } ?: n }
    var confidence by remember { mutableStateOf(Confidence.ESTIMATED) }
    var mealType by remember { mutableStateOf(mealForNow()) }
    var isFavorite by remember { mutableStateOf(favorite) }
    val gutter = Modifier.padding(horizontal = 20.dp)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        // Scrolls on short screens, with a large font, or above the keyboard.
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(gutter, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(food.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    RatingLine(food)
                }
                isFavorite?.let { pinned ->
                    IconButton(onClick = { isFavorite = !pinned; onFavoriteChange(!pinned) }) {
                        Icon(
                            if (pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(if (pinned) R.string.unpin_favorite else R.string.pin_favorite),
                            tint = if (pinned) MaterialTheme.colorScheme.secondary else LocalContentColor.current
                        )
                    }
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_food))
                    }
                }
            }

            LogTotals(
                food = food,
                amount = enteredAmount ?: portion?.grams ?: food.referenceAmount,
                // Ingredients carry no confidence of their own.
                relativeError = if (showMeal) confidence.relativeError else null,
                // In portions the amount in the food's unit isn't on screen otherwise.
                showAmount = portion != null,
                modifier = gutter
            )

            AmountStepper(
                value = amount,
                onValueChange = { amount = it },
                counting = counting,
                modifier = gutter
            ) {
                LogUnitPicker(
                    food = food,
                    selected = measure,
                    label = if (measure in LOG_UNITS) logUnitLabel(measure) else portionLabel(measure),
                    pieceGrams = pieceGrams,
                    onSelect = { key ->
                        // Same quantity in the new unit; without a piece weight yet, a typical amount.
                        val next = portionOf(key)
                        amount = when {
                            enteredAmount != null && (next != null || key == food.logUnit) -> amountIn(enteredAmount, next, key !in WEIGHT_UNITS).clean()
                            key in WEIGHT_UNITS -> "100"
                            else -> "1"
                        }
                        measure = key
                    }
                )
            }

            if (usesPieceWeight(food, measure)) {
                OutlinedTextField(
                    value = pieceWeight,
                    onValueChange = { pieceWeight = it },
                    label = { Text(stringResource(R.string.recipe_piece_weight)) },
                    suffix = { Text(unitLabel(if (food.unit == "ml") "ml" else "g")) },
                    supportingText = if (pieceGrams == null) {
                        { Text(stringResource(R.string.piece_weight_needed)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = gutter.fillMaxWidth()
                )
            }

            // Portions, then one-tap amounts (the usual one and the reference amount), in one
            // scrolling row rather than wrapped blocks under headings.
            val usual = food.defaultAmount?.takeIf { measure == food.logUnit && it != food.referenceAmount }
            if (sizes.isNotEmpty() || usual != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizes.forEach { p ->
                        FilterChip(
                            selected = measure == p.key,
                            onClick = {
                                // Switching from weighing: start at one; between portions: keep the count.
                                if (!counting || portion == null) amount = "1"
                                measure = p.key
                            },
                            label = { Text(portionWithWeight(p, food.unit)) }
                        )
                    }
                    if (usual != null) {
                        SuggestionChip(onClick = { amount = usual.clean() }, label = { Text(stringResource(R.string.amount_usual, amountLabel(usual, food.unit))) })
                        SuggestionChip(onClick = { amount = food.referenceAmount.clean() }, label = { Text(amountLabel(food.referenceAmount, food.unit)) })
                    }
                }
            }

            if (showMeal) {
                SingleChoiceSegmentedButtonRow(gutter.fillMaxWidth()) {
                    MEAL_TYPES.forEachIndexed { i, type ->
                        SegmentedButton(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            shape = SegmentedButtonDefaults.itemShape(i, MEAL_TYPES.size),
                            colors = appSegmentedColors(),
                            icon = {}
                        ) { FitText(mealLabel(type)) }
                    }
                }
                // How sure the amount is; its effect shows as the ± on the calories above.
                SingleChoiceSegmentedButtonRow(gutter.fillMaxWidth()) {
                    Confidence.entries.forEachIndexed { i, c ->
                        SegmentedButton(
                            selected = confidence == c,
                            onClick = { confidence = c },
                            shape = SegmentedButtonDefaults.itemShape(i, Confidence.entries.size),
                            colors = appSegmentedColors(),
                            icon = { Icon(confidenceIcon(c), contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) }
                        ) { FitText(stringResource(c.label)) }
                    }
                }
            }

            Button(
                onClick = {
                    onConfirm(
                        food.copy(pieceGrams = pieceGrams ?: food.pieceGrams),
                        enteredAmount ?: portion?.grams ?: food.defaultAmount ?: food.referenceAmount,
                        mealType, confidence, portion
                    )
                },
                // Grams of a burger can't be logged until one burger's weight is known.
                enabled = !missingWeight,
                modifier = gutter.fillMaxWidth().height(52.dp)
            ) { Text(confirmLabel ?: stringResource(R.string.log_food)) }
        }
    }
}

/** The log sheet's headline: calories (± what [relativeError] allows) and macros for [amount]. */
@Composable
private fun LogTotals(food: CustomFood, amount: Double, relativeError: Double?, showAmount: Boolean, modifier: Modifier = Modifier) {
    val ratio = amount / food.referenceAmount
    val kcal = food.kcalPer100 * ratio
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val kcalUnit = stringResource(R.string.unit_kcal)
    val plusMinus = relativeError?.let { stringResource(R.string.plus_minus_value, (kcal * it).roundToInt()) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FitText(
                buildAnnotatedString {
                    append(kcal.roundToInt().toString())
                    withStyle(SpanStyle(fontSize = 0.5.em, fontWeight = FontWeight.Medium)) { append(" $kcalUnit") }
                    plusMinus?.let { withStyle(SpanStyle(fontSize = 0.45.em, fontWeight = FontWeight.Normal, color = muted)) { append("  $it") } }
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            if (showAmount) {
                Text(stringResource(R.string.for_amount, amountLabel(amount, food.unit)), style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LogMacro(stringResource(R.string.protein), food.proteinPer100 * ratio, MacroProtein, Modifier.weight(1f))
            LogMacro(stringResource(R.string.carbs), food.carbsPer100 * ratio, MacroCarbs, Modifier.weight(1f))
            LogMacro(stringResource(R.string.fat), food.fatPer100 * ratio, MacroFat, Modifier.weight(1f))
            LogMacro(stringResource(R.string.fiber), food.fiberPer100 * ratio, MacroFiber, Modifier.weight(1f))
        }
    }
}

/** Value over name: the number is what's read at a glance. */
@Composable
private fun LogMacro(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FitText("${value.macroGrams()} ${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        FitText(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The amount between − and +, so the usual tweak needs no keyboard; [unit] sits at its end. */
@Composable
private fun AmountStepper(value: String, onValueChange: (String) -> Unit, counting: Boolean, modifier: Modifier = Modifier, unit: @Composable () -> Unit) {
    fun step(up: Boolean) = onValueChange(stepAmount(value.toDecimal() ?: 0.0, counting, up).clean())

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalIconButton(onClick = { step(up = false) }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.amount_less))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            trailingIcon = unit,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        FilledTonalIconButton(onClick = { step(up = true) }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.amount_more))
        }
    }
}

/**
 * The amount's unit, at the end of the field: any food can be logged in pieces, grams, ml or
 * spoons. Pieces and spoons show their weight when it's known.
 */
@Composable
private fun LogUnitPicker(food: CustomFood, selected: String, label: String, pieceGrams: Double?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(start = 8.dp, end = 2.dp)) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 96.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.log_unit))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LOG_UNITS.forEach { key ->
                val grams = unitGrams(food, key, pieceGrams)?.takeIf { key !in WEIGHT_UNITS }
                DropdownMenuItem(
                    text = {
                        Text(
                            grams?.let { stringResource(R.string.portion_with_weight, logUnitLabel(key), amountLabel(it, if (food.unit == "ml") "ml" else "g")) }
                                ?: logUnitLabel(key)
                        )
                    },
                    onClick = { onSelect(key); expanded = false },
                    trailingIcon = if (key == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun EditEntryDialog(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onUpdate: (FoodEntry) -> Unit,
    onCopyToDate: (LocalDate) -> Unit,
    onDelete: () -> Unit
) {
    // An entry logged in portions is edited as a count of that portion.
    val portionGrams = entry.portionGrams?.takeIf { entry.portion != null && it > 0 }
    var amount by remember { mutableStateOf((portionGrams?.let { entry.amount / it } ?: entry.amount).clean()) }
    val enteredAmount = amount.toDecimal()?.let { n -> portionGrams?.let { n * it } ?: n }
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroSummary(
                    kcal = kcalPer100,
                    protein = proteinPer100,
                    carbs = carbsPer100,
                    fat = fatPer100,
                    fiber = fiberPer100,
                    amount = enteredAmount ?: entry.amount,
                    referenceAmount = entry.referenceAmount,
                    unit = entry.unit
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = {
                        Text(
                            when {
                                portionGrams != null && entry.portion in WEIGHT_UNITS -> stringResource(R.string.how_many_unit, unitLabel(entry.portion!!))
                                portionGrams != null -> stringResource(
                                    R.string.how_many_portions,
                                    portionWithWeight(Portion(entry.portion!!, portionGrams), entry.unit)
                                )
                                entry.unit == "pcs" -> stringResource(R.string.how_many_pieces)
                                else -> stringResource(R.string.how_many_unit, unitLabel(entry.unit))
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ConfidenceChips(confidence) { confidence = it }

                Text(stringResource(R.string.quick_copy), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    Button(onClick = { onCopyToDate(LocalDate.now().minusDays(1)); onDismiss() }, modifier = Modifier.weight(1f), contentPadding = padding) {
                        FitText(stringResource(R.string.copy_yesterday))
                    }
                    Button(onClick = { onCopyToDate(LocalDate.now().plusDays(1)); onDismiss() }, modifier = Modifier.weight(1f), contentPadding = padding) {
                        FitText(stringResource(R.string.copy_tomorrow))
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
                val newAmount = enteredAmount ?: entry.amount
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
        FitText("$currentKcal ${stringResource(R.string.unit_kcal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.for_amount, amountText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(8.dp))

        // Four equal columns: a long nutrient name shrinks instead of crowding its neighbours.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MacroItem(stringResource(R.string.protein), currentProtein, MacroProtein, Modifier.weight(1f))
            MacroItem(stringResource(R.string.carbs), currentCarbs, MacroCarbs, Modifier.weight(1f))
            MacroItem(stringResource(R.string.fat), currentFat, MacroFat, Modifier.weight(1f))
            MacroItem(stringResource(R.string.fiber), currentFiber, MacroFiber, Modifier.weight(1f))
        }
    }
}

@Composable
fun MacroItem(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FitText(label, style = MaterialTheme.typography.labelSmall)
        FitText("${value.macroGrams()} ${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

/** One decimal below 10 g, so 2.5 g of fiber doesn't show as "2 g". */
private fun Double.macroGrams(): String =
    if (this < 10) "%.1f".format(this).removeSuffix(".0").removeSuffix(",0") else roundToInt().toString()

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
        // Half the dialog's width: one line, ellipsized, rather than a label wrapping over the value.
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
