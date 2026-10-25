package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.CookFactor
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.Recipe
import com.jsemalja.trackmecalories.data.RecipeIngredient
import com.jsemalja.trackmecalories.data.estimatedCookedWeightG
import com.jsemalja.trackmecalories.data.rawWeightG
import com.jsemalja.trackmecalories.data.toIngredient
import com.jsemalja.trackmecalories.data.totals
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Create or edit a recipe from foods (search, barcode, saved). Each ingredient keeps its own
 * amount and an optional raw -> cooked factor; the dish is logged per serving or per 100 g
 * of the finished pot — weigh the pot, then log your portion by the weight on your plate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditor(viewModel: MainViewModel, recipeId: Int?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(recipeId == null) }
    var recipe by remember { mutableStateOf(Recipe(name = "")) }
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    var name by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("1") }
    var potWeight by remember { mutableStateOf("") }
    var logBy by remember { mutableStateOf("pcs") }
    var editing by remember { mutableStateOf<Int?>(null) }
    var picking by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            viewModel.loadRecipe(recipeId)?.let { (r, ings) ->
                recipe = r
                name = r.name
                servings = r.servings.clean()
                potWeight = r.cookedWeightG?.clean() ?: ""
                logBy = r.logBy
                ingredients.clear()
                ingredients.addAll(ings)
            }
            loaded = true
        }
    }

    val totals = ingredients.toList().totals()
    val servingsValue = servings.toDecimal()?.takeIf { it > 0 } ?: 1.0
    val estimated = ingredients.toList().estimatedCookedWeightG()
    val dishWeight = potWeight.toDecimal()?.takeIf { it > 0 } ?: estimated
    val missingWeights = ingredients.any { it.gramsPerUnit == 0.0 }
    val canSave = name.isNotBlank() && ingredients.isNotEmpty() && (logBy == "pcs" || dishWeight > 0)

    fun save() {
        val updated = recipe.copy(
            name = name.trim(),
            servings = servingsValue,
            cookedWeightG = potWeight.toDecimal()?.takeIf { it > 0 },
            logBy = logBy
        )
        scope.launch {
            viewModel.saveRecipe(updated, ingredients.toList())
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) }
                    Text(
                        stringResource(if (recipeId == null) R.string.recipe_new else R.string.recipe_edit),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = ::save, enabled = canSave && loaded) { Text(stringResource(R.string.save)) }
                }

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.recipe_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(stringResource(R.string.recipe_ingredients), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (ingredients.isEmpty()) {
                        item {
                            Text(stringResource(R.string.recipe_no_ingredients), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(ingredients, key = { i, ing -> "$i-${ing.name}" }) { i, ing ->
                        IngredientRow(ing, onClick = { editing = i }, onRemove = { ingredients.removeAt(i) })
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.recipe_add_search))
                            }
                            OutlinedButton(onClick = { scanning = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.recipe_add_scan))
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.recipe_how_to_log), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("pcs" to R.string.recipe_log_serving, "g" to R.string.recipe_log_weight).forEachIndexed { i, (value, label) ->
                                SegmentedButton(
                                    selected = logBy == value,
                                    onClick = { logBy = value },
                                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                                    colors = appSegmentedColors(),
                                    icon = {}
                                ) { Text(stringResource(label)) }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(if (logBy == "pcs") R.string.recipe_log_serving_desc else R.string.recipe_log_weight_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = servings,
                                onValueChange = { servings = it },
                                label = { Text(stringResource(R.string.recipe_servings)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = potWeight,
                                onValueChange = { potWeight = it },
                                label = { Text(stringResource(R.string.recipe_pot_weight)) },
                                suffix = { Text(stringResource(R.string.unit_g)) },
                                placeholder = { if (estimated > 0) Text(estimated.roundToInt().toString()) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            when {
                                potWeight.toDecimal() != null -> stringResource(R.string.recipe_pot_weighed)
                                estimated > 0 -> stringResource(R.string.recipe_pot_estimated, estimated.roundToInt(), ingredients.toList().rawWeightG().roundToInt())
                                else -> stringResource(R.string.recipe_pot_unknown)
                            } + if (missingWeights && potWeight.toDecimal() == null) " " + stringResource(R.string.recipe_pot_pieces_note) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TotalsRow(stringResource(R.string.recipe_total), totals.kcal, totals.protein, totals.carbs, totals.fat)
                                TotalsRow(stringResource(R.string.recipe_per_serving), totals.kcal / servingsValue, totals.protein / servingsValue, totals.carbs / servingsValue, totals.fat / servingsValue)
                                if (dishWeight > 0) {
                                    val f = 100 / dishWeight
                                    TotalsRow(stringResource(R.string.recipe_per_100g), totals.kcal * f, totals.protein * f, totals.carbs * f, totals.fat * f)
                                    if (servingsValue > 0) {
                                        Text(
                                            stringResource(R.string.recipe_serving_weighs, (dishWeight / servingsValue).roundToInt()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (recipeId != null) {
                        item {
                            TextButton(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text(stringResource(R.string.recipe_delete)) }
                        }
                    }
                }
            }
        }
    }

    editing?.let { index ->
        ingredients.getOrNull(index)?.let { ing ->
            IngredientDialog(
                ingredient = ing,
                onDismiss = { editing = null },
                onSave = { ingredients[index] = it; editing = null }
            )
        }
    }

    if (picking) {
        Dialog(onDismissRequest = { picking = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                SearchScreen(
                    viewModel,
                    onOpenScanner = { picking = false; scanning = true },
                    pick = IngredientPick(
                        onPick = { food, amount -> ingredients.add(food.toIngredient(amount)); picking = false },
                        onClose = { picking = false }
                    )
                )
            }
        }
    }

    if (scanning) {
        Dialog(onDismissRequest = { scanning = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ScannerScreen(
                viewModel,
                onClose = { scanning = false },
                onPick = { food, amount -> ingredients.add(food.toIngredient(amount)); scanning = false }
            )
        }
    }

    if (confirmDelete && recipeId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.recipe_delete_title, name)) },
            text = { Text(stringResource(R.string.recipe_delete_body)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteRecipe(recipeId); confirmDelete = false; onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Search in "add ingredient" mode: picking a food asks for an amount, then hands it back. */
class IngredientPick(val onPick: (CustomFood, Double) -> Unit, val onClose: () -> Unit)

@Composable
private fun IngredientRow(ing: RecipeIngredient, onClick: () -> Unit, onRemove: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, strong = true, onClick = onClick) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ing.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val amount = "${ing.amount.clean()} ${unitLabel(ing.unit)}"
                val cook = if (abs(ing.cookFactor - 1.0) > 0.001) " · " + stringResource(R.string.recipe_cooks_to, (ing.amount * ing.gramsPerUnit * ing.cookFactor).roundToInt()) else ""
                Text(amount + cook, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${ing.totals().kcal.roundToInt()} ${stringResource(R.string.unit_kcal)}", fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TotalsRow(label: String, kcal: Double, protein: Double, carbs: Double, fat: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("${kcal.roundToInt()} ${stringResource(R.string.unit_kcal)}", fontWeight = FontWeight.Bold)
            Text(
                "${stringResource(R.string.macro_p)} ${protein.roundToInt()} · ${stringResource(R.string.macro_c)} ${carbs.roundToInt()} · ${stringResource(R.string.macro_f)} ${fat.roundToInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Amount, piece weight (for "pcs" foods) and cooking factor of one ingredient. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientDialog(ingredient: RecipeIngredient, onDismiss: () -> Unit, onSave: (RecipeIngredient) -> Unit) {
    var amount by remember { mutableStateOf(ingredient.amount.clean()) }
    var pieceGrams by remember { mutableStateOf(if (ingredient.unit == "pcs" && ingredient.gramsPerUnit > 0) ingredient.gramsPerUnit.clean() else "") }
    var factor by remember { mutableStateOf(ingredient.cookFactor.clean()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ingredient.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.recipe_amount)) },
                    suffix = { Text(unitLabel(ingredient.unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (ingredient.unit == "pcs") {
                    OutlinedTextField(
                        value = pieceGrams,
                        onValueChange = { pieceGrams = it },
                        label = { Text(stringResource(R.string.recipe_piece_weight)) },
                        suffix = { Text(stringResource(R.string.unit_g)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(stringResource(R.string.recipe_cook_factor), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CookFactor.entries.forEach { cf ->
                        FilterChip(
                            selected = factor.toDecimal() == cf.factor,
                            onClick = { factor = cf.factor.clean() },
                            label = { Text(stringResource(cf.label)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = factor,
                    onValueChange = { factor = it },
                    label = { Text(stringResource(R.string.recipe_cook_factor_custom)) },
                    prefix = { Text("×") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.recipe_cook_factor_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    ingredient.copy(
                        amount = amount.toDecimal()?.takeIf { it > 0 } ?: ingredient.amount,
                        gramsPerUnit = if (ingredient.unit == "pcs") pieceGrams.toDecimal()?.takeIf { it > 0 } ?: 0.0 else ingredient.gramsPerUnit,
                        cookFactor = factor.toDecimal()?.takeIf { it > 0 } ?: 1.0
                    )
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
