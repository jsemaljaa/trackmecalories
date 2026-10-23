package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.toCustomFood
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

/**
 * Search across the user's saved foods (instant, offline) and Open Food Facts.
 */
@Composable
fun SearchScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val savedMatches by viewModel.savedFoodMatches.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val keyboard = LocalSoftwareKeyboardController.current
    var foodToLog by remember { mutableStateOf<CustomFood?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val hasQuery = query.trim().length >= 2

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(Icons.Default.Search, "Search Foods")
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::searchFoods,
            placeholder = { Text("e.g. skyr, oat milk, banana") },
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

        when {
            !hasQuery -> SearchMessage(
                icon = Icons.Default.Search,
                title = "Find a food",
                body = "Search your saved foods and millions of products from Open Food Facts."
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (savedMatches.isNotEmpty()) {
                    item(key = "saved_header") { SectionLabel("Your foods") }
                    items(savedMatches, key = { "saved_${it.id}" }) { food ->
                        SavedFoodResultRow(food) { foodToLog = food }
                    }
                }

                if (searchResults.isNotEmpty()) {
                    item(key = "off_header") { SectionLabel("Open Food Facts") }
                    // Must match the repository's distinctBy key — duplicate keys crash LazyColumn.
                    items(searchResults, key = { "off_${it.barcode ?: "${it.name}|${it.brand}"}" }) { info ->
                        FoodInfoRow(info) { foodToLog = info.toCustomFood() }
                    }
                }

                searchError?.let { error ->
                    item(key = "error") {
                        SearchMessage(
                            icon = Icons.Default.CloudOff,
                            title = "Search unavailable",
                            body = error,
                            actionLabel = "Retry",
                            onAction = viewModel::retrySearch
                        )
                    }
                }

                if (!isSearching && searchError == null && searchResults.isEmpty() && savedMatches.isEmpty()) {
                    item(key = "empty") {
                        SearchMessage(
                            icon = Icons.Default.SearchOff,
                            title = "No matches for “${query.trim()}”",
                            body = "Try a simpler or more generic term, or add it yourself.",
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
            }
        )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
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
private fun SavedFoodResultRow(food: CustomFood, onClick: () -> Unit) {
    ResultCard(
        title = food.name,
        subtitle = null,
        kcal = food.kcalPer100,
        protein = food.proteinPer100,
        carbs = food.carbsPer100,
        fat = food.fatPer100,
        per = if (food.unit == "pcs") "pc" else "100${food.unit}",
        leading = Icons.Default.Bookmark,
        onClick = onClick
    )
}

@Composable
fun FoodInfoRow(info: FoodInfo, onClick: () -> Unit) {
    ResultCard(
        title = info.name,
        subtitle = listOfNotNull(info.brand, info.quantity).joinToString(" · ").ifBlank { null },
        kcal = info.kcalPer100,
        protein = info.proteinPer100,
        carbs = info.carbsPer100,
        fat = info.fatPer100,
        per = "100${info.unit}",
        leading = null,
        onClick = onClick
    )
}

@Composable
private fun ResultCard(
    title: String,
    subtitle: String?,
    kcal: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    per: String,
    leading: ImageVector?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    "P ${fmt(protein)}g · C ${fmt(carbs)}g · F ${fmt(fat)}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${kcal.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("kcal / $per", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun fmt(value: Double): String =
    if (value >= 10) value.roundToInt().toString()
    else "%.1f".format(value).removeSuffix(".0").removeSuffix(",0")
