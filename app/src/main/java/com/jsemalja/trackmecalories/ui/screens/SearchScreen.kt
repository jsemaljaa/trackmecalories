package com.jsemalja.trackmecalories.ui.screens

import com.jsemalja.trackmecalories.data.healthOrder
import com.jsemalja.trackmecalories.data.rating
import com.jsemalja.trackmecalories.ui.theme.AppCard
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Eco
import com.jsemalja.trackmecalories.data.portionList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.SearchHit
import com.jsemalja.trackmecalories.data.SearchSource
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import androidx.compose.ui.res.stringResource
import com.jsemalja.trackmecalories.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.jsemalja.trackmecalories.data.label

/**
 * The Food tab. One search box over everything: your saved foods first, then a single list
 * ranked by relevance from bundled restaurant menus, Kalorické tabulky and Open Food Facts.
 * With nothing typed it lists your saved foods (recent / favorites / all), recipes and
 * restaurant shortcuts, and the box has a barcode button that opens the scanner.
 */
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit,
    // Set when picking a recipe ingredient: tapping a food asks for an amount and hands it back.
    pick: IngredientPick? = null
) {
    val recipes by viewModel.recipes.collectAsState()
    var recipeEditorOpen by remember { mutableStateOf(false) }
    var recipeEditorId by remember { mutableStateOf<Int?>(null) }
    if (pick != null) {
        // Start the picker with an empty box, and leave the Food tab's search clean after.
        DisposableEffect(Unit) {
            viewModel.searchFoods("")
            onDispose { viewModel.searchFoods("") }
        }
    }
    val recentFoods by viewModel.recentFoods.collectAsState()
    val favoriteFoods by viewModel.favoriteFoods.collectAsState()
    val allFoods by viewModel.allCustomFoods.collectAsState()
    var savedTab by rememberSaveable { mutableStateOf(SavedFoodsTab.RECENT) }
    var allSort by rememberSaveable { mutableStateOf(FoodSort.NAME) }
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    // Results come best match first; this re-sorts them healthiest first.
    var healthFirst by rememberSaveable { mutableStateOf(false) }
    val shownResults = remember(results, healthFirst) {
        if (healthFirst) results.sortedWith(compareBy(healthOrder) { (it.food ?: it.rated)?.rating() }) else results
    }
    val savedMatches by viewModel.savedFoodMatches.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val couldNotLoad = stringResource(R.string.could_not_load)
    val selectedDate by viewModel.selectedDate.collectAsState()

    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var foodToLog by remember { mutableStateOf<CustomFood?>(null) }
    var editingFood by remember { mutableStateOf<CustomFood?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var resolvingKey by remember { mutableStateOf<String?>(null) }

    val hasQuery = query.trim().length >= 2
    val nothingFound = results.isEmpty() && savedMatches.isEmpty()

    // New query -> start at the top instead of wherever the previous list was scrolled.
    LaunchedEffect(query.trim()) { listState.scrollToItem(0) }

    fun editFood(food: CustomFood) {
        scope.launch {
            val recipeId = if (food.id != 0) viewModel.recipeIdForFood(food.id) else null
            if (recipeId != null) {
                recipeEditorId = recipeId
                recipeEditorOpen = true
            } else {
                editingFood = food
            }
        }
    }

    fun open(hit: SearchHit) {
        hit.food?.let { foodToLog = it; return }
        val ktHit = hit.ktHit ?: return
        if (resolvingKey != null) return
        resolvingKey = hit.key
        scope.launch {
            val food = viewModel.resolveKtFood(ktHit)
            resolvingKey = null
            if (food != null) {
                foodToLog = food
            } else {
                Toast.makeText(context, couldNotLoad.format(hit.name), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (pick != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = pick.onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) }
                Text(stringResource(R.string.recipe_add_ingredient), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        } else {
            ScreenHeader(Icons.Outlined.Restaurant, stringResource(R.string.nav_food))
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::searchFoods,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchFoods("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                } else {
                    IconButton(onClick = onOpenScanner) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = stringResource(R.string.add_scan))
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
        )

        // Fixed-height slot so the list doesn't jump when the indicator appears.
        Box(Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 4.dp)) {
            if (isSearching) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))

        if (!hasQuery) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                savedFoodsSection(
                    tab = savedTab,
                    onTabChange = { savedTab = it },
                    foods = when (savedTab) {
                        SavedFoodsTab.RECENT -> recentFoods
                        SavedFoodsTab.FAVORITES -> favoriteFoods
                        SavedFoodsTab.ALL -> allFoods
                        SavedFoodsTab.RECIPES, SavedFoodsTab.RESTAURANTS -> emptyList()
                    },
                    onLog = { foodToLog = it },
                    onEdit = { editFood(it) },
                    onToggleFavorite = { viewModel.setFavorite(it, !it.isFavorite) },
                    recipes = recipes,
                    onNewRecipe = { recipeEditorId = null; recipeEditorOpen = true },
                    onOpenRecipe = { recipeEditorId = it; recipeEditorOpen = true },
                    onOpenSpot = { viewModel.searchFoods(it) },
                    showRecipesTab = pick == null,
                    sort = allSort,
                    onSortChange = { allSort = it }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (savedMatches.isNotEmpty()) {
                    item(key = "saved_header") { SectionLabel(stringResource(R.string.your_foods)) }
                    items(savedMatches, key = { "saved_${it.id}" }) { food ->
                        SavedFoodRow(
                            food = food,
                            onClick = { foodToLog = food },
                            onEdit = { editFood(food) },
                            onToggleFavorite = { viewModel.setFavorite(food, !food.isFavorite) }
                        )
                    }
                }

                if (results.isNotEmpty()) {
                    item(key = "results_header") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { SectionLabel(stringResource(R.string.results)) }
                            TextButton(onClick = { healthFirst = !healthFirst }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(if (healthFirst) R.string.sort_nutriscore else R.string.sort_relevance))
                            }
                        }
                    }
                    items(shownResults, key = { it.key }) { hit ->
                        SearchHitRow(hit, isLoading = resolvingKey == hit.key) { open(hit) }
                    }
                }

                // Skeletons while the online databases answer: a full list's worth when there's
                // nothing to show yet, a couple under the offline hits otherwise.
                if (isSearching) {
                    items(if (nothingFound) 5 else 2, key = { "skeleton_$it" }) { SkeletonResultCard() }
                }

                searchError?.let { error ->
                    item(key = "error") {
                        SearchMessage(
                            icon = Icons.Default.CloudOff,
                            title = stringResource(if (results.isEmpty()) R.string.search_unavailable else R.string.search_partial),
                            body = stringResource(error),
                            actionLabel = stringResource(R.string.retry),
                            onAction = viewModel::retrySearch
                        )
                    }
                }

                if (!isSearching && searchError == null && nothingFound) {
                    item(key = "empty") {
                        SearchMessage(
                            icon = Icons.Default.SearchOff,
                            title = stringResource(R.string.no_matches, query.trim()),
                            body = stringResource(R.string.no_matches_body),
                            actionLabel = stringResource(R.string.add_custom),
                            onAction = { showCreateDialog = true }
                        )
                    }
                } else if (!isSearching) {
                    item(key = "create") {
                        TextButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.cant_find_create))
                        }
                    }
                }
            }
        }
    }

    foodToLog?.let { food ->
        val requestKey = rememberRequestKey()
        PortionEntryDialog(
            food = food,
            onDismiss = { foodToLog = null },
            onConfirm = { logged, amount, mealType, confidence, portion ->
                if (pick != null) {
                    pick.onPick(logged, amount)
                } else {
                    viewModel.logCustomFood(logged, amount, mealType, selectedDate.toString(), confidence, portion, requestKey)
                }
                foodToLog = null
            },
            onEdit = if (food.id != 0 && pick == null) {
                {
                    foodToLog = null
                    editFood(food)
                }
            } else null,
            favorite = if (food.id != 0 && pick == null) food.isFavorite else null,
            onFavoriteChange = { viewModel.setFavorite(food, it) },
            showMeal = pick == null,
            confirmLabel = if (pick != null) stringResource(R.string.recipe_add) else null
        )
    }

    if (recipeEditorOpen) {
        RecipeEditor(viewModel, recipeEditorId, onDismiss = { recipeEditorOpen = false })
    }

    editingFood?.let { food ->
        EditSavedFoodDialog(viewModel, food, onDismiss = { editingFood = null })
    }

    if (showCreateDialog) {
        CustomFoodEntryDialog(
            initialName = query.trim(),
            onDismiss = { showCreateDialog = false },
            onSave = { food ->
                showCreateDialog = false
                foodToLog = food
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun SearchMessage(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, isLoading: Boolean, onClick: () -> Unit) {
    val protein = hit.protein
    val carbs = hit.carbs
    val fat = hit.fat
    val macros = when {
        isLoading -> stringResource(R.string.loading_serving)
        protein != null && carbs != null && fat != null -> macroLine(protein, carbs, fat)
        else -> stringResource(R.string.tap_for_macros)
    }
    // Base foods show their usual portion ("Medium · 51 g") so it's clear they can be logged that way.
    val usualPortion = hit.food?.let { food -> food.portionList.firstOrNull { it.key == food.defaultPortion } }
        ?.let { portionWithWeight(it, hit.unit) }
    ResultCard(
        title = hit.name,
        subtitle = listOfNotNull(hit.brand, hit.detail, usualPortion).joinToString(" · ").ifBlank { null },
        kcal = hit.kcal,
        macros = macros,
        per = perLabel(hit.unit),
        source = when (hit.source) {
            SearchSource.OPEN_FOOD_FACTS -> if (hit.isCzech) stringResource(R.string.source_off_cz) else stringResource(R.string.source_off)
            else -> stringResource(hit.source.label)
        },
        leading = when (hit.source) {
            SearchSource.MENU -> Icons.Default.Restaurant
            SearchSource.BASIC -> Icons.Default.Eco
            else -> null
        },
        onClick = onClick,
        rated = hit.food ?: hit.rated,
        trailing = if (isLoading) {
            { CircularProgressIndicator(Modifier.padding(start = 8.dp).size(20.dp), strokeWidth = 2.dp) }
        } else null
    )
}

@Composable
internal fun perLabel(unit: String) = if (unit == "pcs") stringResource(R.string.per_piece_short) else "100 ${unitLabel(unit)}"

@Composable
internal fun macroLine(protein: Double, carbs: Double, fat: Double) = stringResource(R.string.macro_line, fmt(protein), fmt(carbs), fmt(fat))

@Composable
internal fun ResultCard(
    title: String,
    subtitle: String?,
    kcal: Double,
    macros: String,
    per: String,
    source: String?,
    leading: ImageVector?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    rated: CustomFood? = null
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        strong = true,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Icon(leading, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    macros,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (source != null) {
                    Text(
                        source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            rated?.rating()?.let { r ->
                r.grade?.let {
                    NutriScoreBadge(it, r.estimated)
                    Spacer(Modifier.width(10.dp))
                }
            }
            // A minimum width keeps the badges in one column when the "per" label varies.
            Column(Modifier.widthIn(min = 64.dp), horizontalAlignment = Alignment.End) {
                Text("${kcal.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.kcal_per, per), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

private fun fmt(value: Double): String =
    if (value >= 10) value.roundToInt().toString()
    else "%.1f".format(value).removeSuffix(".0").removeSuffix(",0")
