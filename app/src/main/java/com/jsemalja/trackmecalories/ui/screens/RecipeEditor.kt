package com.jsemalja.trackmecalories.ui.screens

import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.CookFactor
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.Recipe
import com.jsemalja.trackmecalories.data.RecipeIngredient
import com.jsemalja.trackmecalories.data.estimatedCookedWeightG
import com.jsemalja.trackmecalories.data.hasCookFactors
import com.jsemalja.trackmecalories.data.inPieces
import com.jsemalja.trackmecalories.data.missingWeights
import com.jsemalja.trackmecalories.data.per100g
import com.jsemalja.trackmecalories.data.rawWeightG
import com.jsemalja.trackmecalories.data.toIngredient
import com.jsemalja.trackmecalories.data.totals
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.jsemalja.trackmecalories.data.label

/**
 * Create or edit a recipe the way you cook it: add the ingredients as weighed raw, optionally
 * weigh the finished dish, and the app shows calories and macros per 100 g. The dish is then
 * logged by the weight on your plate (default) or per serving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditor(viewModel: MainViewModel, recipeId: Int?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(recipeId == null) }
    var recipe by remember { mutableStateOf(Recipe(name = "", logBy = "g")) }
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    var name by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("1") }
    var potWeight by remember { mutableStateOf("") }
    var logBy by remember { mutableStateOf("g") }
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

    // A food counted in pieces needs its piece weight to count toward the dish, so ask right
    // away unless it's already known.
    fun addIngredient(food: CustomFood, amount: Double) {
        val ingredient = food.toIngredient(amount)
        ingredients.add(ingredient)
        if (ingredient.unit == "pcs" && ingredient.gramsPerUnit <= 0.0) editing = ingredients.lastIndex
    }

    val list = ingredients.toList()
    val totals = list.totals()
    val servingsValue = servings.toDecimal()?.takeIf { it > 0 } ?: 1.0
    val rawWeight = list.rawWeightG()
    val estimated = list.estimatedCookedWeightG()
    val weighed = potWeight.toDecimal()?.takeIf { it > 0 }
    val dishWeight = weighed ?: estimated
    val per100 = totals.per100g(dishWeight)
    val missingWeights = list.missingWeights().isNotEmpty()
    val canSave = name.isNotBlank() && ingredients.isNotEmpty() && (logBy == "pcs" || dishWeight > 0)

    fun save() {
        val updated = recipe.copy(
            name = name.trim(),
            servings = servingsValue,
            cookedWeightG = weighed,
            logBy = logBy
        )
        scope.launch {
            viewModel.saveRecipe(updated, ingredients.toList())
            onDismiss()
        }
    }

    FullScreenDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                // Close icon and Save line up with the 16 dp content edges below.
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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

                    // Step 1: what went into the pot, weighed raw.
                    item {
                        StepHeader(stringResource(R.string.recipe_step_ingredients), stringResource(R.string.recipe_ingredients_hint))
                    }
                    itemsIndexed(ingredients, key = { i, ing -> "$i-${ing.name}" }) { i, ing ->
                        IngredientRow(ing, onClick = { editing = i }, onRemove = { ingredients.removeAt(i) })
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Half the width each: labels shrink rather than wrap on narrow phones.
                            val padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            OutlinedButton(onClick = { picking = true }, modifier = Modifier.weight(1f), contentPadding = padding) {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                FitText(stringResource(R.string.recipe_add_search))
                            }
                            OutlinedButton(onClick = { scanning = true }, modifier = Modifier.weight(1f), contentPadding = padding) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                FitText(stringResource(R.string.recipe_add_scan))
                            }
                        }
                    }

                    // Step 2: optionally weigh the finished dish, since cooking adds or removes water.
                    item {
                        Spacer(Modifier.height(6.dp))
                        StepHeader(stringResource(R.string.recipe_step_cooked), stringResource(R.string.recipe_pot_weight_hint))
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = potWeight,
                            onValueChange = { potWeight = it },
                            label = { Text(stringResource(R.string.recipe_pot_weight)) },
                            suffix = { Text(stringResource(R.string.unit_g)) },
                            placeholder = { if (estimated > 0) Text(estimated.roundToInt().toString()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // The result: per 100 g up front, the whole dish (and a serving) below.
                    item {
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.recipe_per_100g), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ValueWithUnit(
                                    per100?.kcal?.roundToInt()?.toString() ?: "—",
                                    stringResource(R.string.unit_kcal),
                                    MaterialTheme.typography.displaySmall,
                                    unitStyle = MaterialTheme.typography.titleMedium,
                                    unitColor = LocalContentColor.current,
                                    gap = 6.dp
                                )
                                if (per100 != null) {
                                    Text(
                                        stringResource(R.string.macro_line, short(per100.protein), short(per100.carbs), short(per100.fat)),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    when {
                                        weighed != null -> stringResource(R.string.recipe_basis_weighed, weighed.roundToInt(), rawWeight.roundToInt())
                                        estimated <= 0 -> stringResource(R.string.recipe_basis_unknown)
                                        list.hasCookFactors() -> stringResource(R.string.recipe_basis_estimated, estimated.roundToInt(), rawWeight.roundToInt())
                                        else -> stringResource(R.string.recipe_basis_raw, rawWeight.roundToInt())
                                    } + if (missingWeights && weighed == null) " " + stringResource(R.string.recipe_pot_pieces_note) else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                TotalsRow(stringResource(R.string.recipe_total), totals.kcal, totals.protein, totals.carbs, totals.fat)
                                if (logBy == "pcs") {
                                    TotalsRow(stringResource(R.string.recipe_per_serving), totals.kcal / servingsValue, totals.protein / servingsValue, totals.carbs / servingsValue, totals.fat / servingsValue)
                                    if (dishWeight > 0) {
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

                    // Step 3: by weight (the per-100 g values above) or per serving.
                    item {
                        Spacer(Modifier.height(6.dp))
                        StepHeader(stringResource(R.string.recipe_step_log), null)
                        Spacer(Modifier.height(6.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("g" to R.string.recipe_log_weight, "pcs" to R.string.recipe_log_serving).forEachIndexed { i, (value, label) ->
                                SegmentedButton(
                                    selected = logBy == value,
                                    onClick = { logBy = value },
                                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                                    colors = appSegmentedColors(),
                                    icon = {}
                                ) { FitText(stringResource(label)) }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(if (logBy == "pcs") R.string.recipe_log_serving_desc else R.string.recipe_log_weight_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (logBy == "pcs") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = servings,
                                onValueChange = { servings = it },
                                label = { Text(stringResource(R.string.recipe_servings)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (recipeId != null) {
                        item {
                            TextButton(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 0.dp)
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
        FullScreenDialog(onDismissRequest = { picking = false }) {
            Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                SearchScreen(
                    viewModel,
                    onOpenScanner = { picking = false; scanning = true },
                    pick = IngredientPick(
                        onPick = { food, amount -> picking = false; addIngredient(food, amount) },
                        onClose = { picking = false }
                    )
                )
            }
        }
    }

    if (scanning) {
        // The scanner pads itself for the status bar, like its overlay in MainActivity.
        FullScreenDialog(onDismissRequest = { scanning = false }) {
            ScannerScreen(
                viewModel,
                onClose = { scanning = false },
                onPick = { food, amount -> scanning = false; addIngredient(food, amount) }
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

/**
 * A full-screen dialog drawn edge to edge. With the default decor fitting, a full-height dialog
 * window is pushed below the status bar and its bottom ends up off screen; here the content
 * pads itself for the system bars instead.
 */
@Composable
private fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        val lightBars = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f
        SideEffect {
            window?.let {
                // decorFitsSystemWindows alone leaves the window fitting the system bars.
                it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                WindowCompat.setDecorFitsSystemWindows(it, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.attributes = it.attributes.apply {
                        fitInsetsTypes = 0
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        content()
    }
}

/** Search in "add ingredient" mode: picking a food asks for an amount, then hands it back. */
class IngredientPick(val onPick: (CustomFood, Double) -> Unit, val onClose: () -> Unit)

@Composable
private fun StepHeader(title: String, hint: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Per-100 g macros are small numbers, so keep one decimal below 10 g. */
private fun short(value: Double): String =
    if (value >= 10) value.roundToInt().toString()
    else "%.1f".format(value).removeSuffix(".0").removeSuffix(",0")

@Composable
private fun IngredientRow(ing: RecipeIngredient, onClick: () -> Unit, onRemove: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, strong = true, onClick = onClick) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ing.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val grams = ing.amount * ing.gramsPerUnit
                val gramsLabel = "${grams.roundToInt()} ${stringResource(R.string.unit_g)}"
                val amount = when {
                    ing.unit != "pcs" -> stringResource(R.string.recipe_raw_amount, "${ing.amount.clean()} ${unitLabel(ing.unit)}")
                    // Entered in grams: a share of a piece ("0.01 pcs") means nothing.
                    !ing.inPieces() -> stringResource(R.string.recipe_raw_amount, gramsLabel)
                    grams > 0 -> "${ing.amount.clean()} ${unitLabel(ing.unit)} · " + stringResource(R.string.recipe_raw_amount, gramsLabel)
                    else -> "${ing.amount.clean()} ${unitLabel(ing.unit)}"
                }
                val cook = if (abs(ing.cookFactor - 1.0) > 0.001 && grams > 0) " · " + stringResource(R.string.recipe_cooks_to, (grams * ing.cookFactor).roundToInt()) else ""
                Text(amount + cook, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ing.gramsPerUnit <= 0.0) {
                    Text(stringResource(R.string.recipe_piece_weight_missing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Text("${ing.totals().kcal.roundToInt()} ${stringResource(R.string.unit_kcal)}", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
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

/**
 * Raw amount and piece weight (for "pcs" foods) of one ingredient. A food counted in pieces
 * can be entered as pieces or grams. The cooking factor is tucked away: it only matters when
 * the finished dish isn't weighed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientDialog(ingredient: RecipeIngredient, onDismiss: () -> Unit, onSave: (RecipeIngredient) -> Unit) {
    val pieces = ingredient.unit == "pcs"
    // Grams of a piece-counted food are stored as a share of a piece, so they need its weight.
    var inGrams by remember { mutableStateOf(pieces && !ingredient.inPieces()) }
    var amount by remember {
        mutableStateOf((if (inGrams) Math.round(ingredient.amount * ingredient.gramsPerUnit * 10) / 10.0 else ingredient.amount).clean())
    }
    var pieceGrams by remember { mutableStateOf(if (pieces && ingredient.gramsPerUnit > 0) ingredient.gramsPerUnit.clean() else "") }
    val pieceWeight = pieceGrams.toDecimal()?.takeIf { it > 0 }
    var factor by remember { mutableStateOf(ingredient.cookFactor.clean()) }
    var showFactor by remember { mutableStateOf(abs(ingredient.cookFactor - 1.0) > 0.001) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ingredient.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pieces) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(false, true).forEachIndexed { i, grams ->
                            SegmentedButton(
                                selected = inGrams == grams,
                                onClick = {
                                    // Same quantity in the other unit, once a piece's weight is known.
                                    if (inGrams != grams) {
                                        val n = amount.toDecimal()
                                        if (n != null && pieceWeight != null) {
                                            amount = (if (grams) Math.round(n * pieceWeight * 10) / 10.0 else Math.round(n / pieceWeight * 100) / 100.0).clean()
                                        }
                                        inGrams = grams
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                                colors = appSegmentedColors(),
                                icon = {}
                            ) { Text(stringResource(if (grams) R.string.recipe_in_grams else R.string.recipe_in_pieces)) }
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.recipe_amount)) },
                    suffix = { Text(unitLabel(if (inGrams) "g" else ingredient.unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pieces) {
                    OutlinedTextField(
                        value = pieceGrams,
                        onValueChange = { pieceGrams = it },
                        label = { Text(stringResource(R.string.recipe_piece_weight)) },
                        suffix = { Text(stringResource(R.string.unit_g)) },
                        supportingText = { Text(stringResource(if (inGrams) R.string.piece_weight_needed else R.string.recipe_piece_weight_help)) },
                        isError = inGrams && pieceWeight == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(onClick = { showFactor = !showFactor }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(stringResource(R.string.recipe_cook_factor_toggle), modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(4.dp))
                    Icon(if (showFactor) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                if (showFactor) {
                    Text(stringResource(R.string.recipe_cook_factor_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entered = amount.toDecimal()?.takeIf { it > 0 }
                    onSave(
                        ingredient.copy(
                            // Always stored in the food's unit (pieces).
                            amount = (if (inGrams) entered?.let { n -> pieceWeight?.let { n / it } } else entered) ?: ingredient.amount,
                            gramsPerUnit = if (pieces) pieceWeight ?: 0.0 else ingredient.gramsPerUnit,
                            cookFactor = factor.toDecimal()?.takeIf { it > 0 } ?: 1.0
                        )
                    )
                },
                // Grams can't be turned into pieces without a piece's weight.
                enabled = !(inGrams && pieceWeight == null)
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
