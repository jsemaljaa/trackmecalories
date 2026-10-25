package com.jsemalja.trackmecalories.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Shortcuts on the empty search screen; each lists that chain's bundled menu.
private val POPULAR_SPOTS = listOf(
    "Don Pačínko", "Bageterie Boulevard", "McDonald's", "KFC", "Burger King", "Subway",
    "Starbucks", "Costa Coffee", "Paul", "Ugo", "Pizza Hut", "Albert Fresh Bistro"
)

/**
 * One search box over everything: your saved foods first, then a single list ranked by
 * relevance from bundled restaurant menus, Kalorické tabulky and Open Food Facts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val savedMatches by viewModel.savedFoodMatches.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
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
                Toast.makeText(context, "Couldn't load ${hit.name}. Check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(Icons.Default.Search, "Search Foods")
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::searchFoods,
            placeholder = { Text("e.g. skyr, big mac, caesar bageta") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchFoods("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
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
            Column {
                SearchMessage(
                    icon = Icons.Default.Search,
                    title = "Find a food",
                    body = "Your saved foods, Czech restaurant menus, Kalorické tabulky and Open Food Facts — all in one search. Half-typed words and missing accents are fine."
                )
                SectionLabel("Popular spots")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    POPULAR_SPOTS.forEach { spot ->
                        AssistChip(
                            onClick = { viewModel.searchFoods(spot) },
                            label = { Text(spot) },
                            leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (savedMatches.isNotEmpty()) {
                    item(key = "saved_header") { SectionLabel("Your foods") }
                    items(savedMatches, key = { "saved_${it.id}" }) { food ->
                        SavedFoodResultRow(
                            food = food,
                            onClick = { foodToLog = food },
                            onEdit = { editingFood = food }
                        )
                    }
                }

                if (results.isNotEmpty()) {
                    item(key = "results_header") { SectionLabel("Results") }
                    items(results, key = { it.key }) { hit ->
                        SearchHitRow(hit, isLoading = resolvingKey == hit.key) { open(hit) }
                    }
                }

                searchError?.let { error ->
                    item(key = "error") {
                        SearchMessage(
                            icon = Icons.Default.CloudOff,
                            title = if (results.isEmpty()) "Search unavailable" else "Some results are missing",
                            body = error,
                            actionLabel = "Retry",
                            onAction = viewModel::retrySearch
                        )
                    }
                }

                if (!isSearching && searchError == null && nothingFound) {
                    item(key = "empty") {
                        SearchMessage(
                            icon = Icons.Default.SearchOff,
                            title = "No matches for “${query.trim()}”",
                            body = "Try fewer or more general words, or add it yourself.",
                            actionLabel = "Create custom food",
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
                            Text("Can't find it? Create a custom food")
                        }
                    }
                }
            }
        }
    }

    foodToLog?.let { food ->
        PortionEntryDialog(
            food = food,
            onDismiss = { foodToLog = null },
            onConfirm = { amount, mealType ->
                viewModel.logCustomFood(food, amount, mealType, selectedDate.toString())
                foodToLog = null
            },
            onEdit = if (food.id != 0) {
                {
                    foodToLog = null
                    editingFood = food
                }
            } else null
        )
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
private fun SavedFoodResultRow(food: CustomFood, onClick: () -> Unit, onEdit: () -> Unit) {
    ResultCard(
        title = food.name,
        subtitle = null,
        kcal = food.kcalPer100,
        macros = macroLine(food.proteinPer100, food.carbsPer100, food.fatPer100),
        per = perLabel(food.unit),
        source = null,
        leading = Icons.Default.Bookmark,
        onClick = onClick,
        trailing = {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit ${food.name}") }
        }
    )
}

@Composable
private fun SearchHitRow(hit: SearchHit, isLoading: Boolean, onClick: () -> Unit) {
    val macros = when {
        isLoading -> "Loading serving size…"
        hit.protein != null && hit.carbs != null && hit.fat != null -> macroLine(hit.protein, hit.carbs, hit.fat)
        else -> "Tap for macros & serving size"
    }
    ResultCard(
        title = hit.name,
        subtitle = listOfNotNull(hit.brand, hit.detail).joinToString(" · ").ifBlank { null },
        kcal = hit.kcal,
        macros = macros,
        per = perLabel(hit.unit),
        source = when (hit.source) {
            SearchSource.OPEN_FOOD_FACTS -> if (hit.isCzech) "Open Food Facts · sold in CZ" else "Open Food Facts"
            else -> hit.source.label
        },
        leading = if (hit.source == SearchSource.MENU) Icons.Default.Restaurant else null,
        onClick = onClick,
        trailing = if (isLoading) {
            { CircularProgressIndicator(Modifier.padding(start = 8.dp).size(20.dp), strokeWidth = 2.dp) }
        } else null
    )
}

private fun perLabel(unit: String) = if (unit == "pcs") "piece" else "100$unit"

private fun macroLine(protein: Double, carbs: Double, fat: Double) =
    "P ${fmt(protein)}g · C ${fmt(carbs)}g · F ${fmt(fat)}g"

@Composable
private fun ResultCard(
    title: String,
    subtitle: String?,
    kcal: Double,
    macros: String,
    per: String,
    source: String?,
    leading: ImageVector?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
            Column(horizontalAlignment = Alignment.End) {
                Text("${kcal.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("kcal / $per", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

private fun fmt(value: Double): String =
    if (value >= 10) value.roundToInt().toString()
    else "%.1f".format(value).removeSuffix(".0").removeSuffix(",0")
