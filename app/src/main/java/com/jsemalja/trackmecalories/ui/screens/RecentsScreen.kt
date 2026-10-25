package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.viewmodel.RecipeListItem
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.ui.res.stringResource
import com.jsemalja.trackmecalories.R

// Chains with bundled menus; picking one searches its menu.
private val POPULAR_SPOTS = listOf(
    "Don Pačínko", "Bageterie Boulevard", "McDonald's", "KFC", "Burger King", "Subway",
    "Starbucks", "Costa Coffee", "Paul", "Ugo", "Pizza Hut", "Albert Fresh Bistro"
)

/** Which list the Food tab shows while the search box is empty. */
enum class SavedFoodsTab(@androidx.annotation.StringRes val label: Int) {
    RECENT(R.string.tab_recent),
    FAVORITES(R.string.tab_favorites),
    ALL(R.string.tab_all),
    RECIPES(R.string.tab_recipes),
    RESTAURANTS(R.string.tab_restaurants)
}

/**
 * Saved foods (recently used, pinned favorites, or everything), recipes and restaurant
 * shortcuts as a section of the Food tab's list — shown while nothing is typed. Rows only
 * log on tap; editing and pinning live in the portion dialog to keep the list quiet.
 */
fun LazyListScope.savedFoodsSection(
    tab: SavedFoodsTab,
    onTabChange: (SavedFoodsTab) -> Unit,
    foods: List<CustomFood>,
    onLog: (CustomFood) -> Unit,
    recipes: List<RecipeListItem> = emptyList(),
    onNewRecipe: () -> Unit = {},
    onOpenRecipe: (Int) -> Unit = {},
    onOpenSpot: (String) -> Unit = {},
    showRecipesTab: Boolean = true,
    sort: FoodSort = FoodSort.NAME,
    onSortChange: (FoodSort) -> Unit = {}
) {
    val tabs = SavedFoodsTab.entries.filter { showRecipesTab || it != SavedFoodsTab.RECIPES }
    item(key = "saved_tabs") {
        // Chips scroll sideways, so long translations never squeeze or wrap.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs) { entry ->
                FilterChip(
                    selected = tab == entry,
                    onClick = { onTabChange(entry) },
                    label = { Text(stringResource(entry.label), maxLines = 1) }
                )
            }
        }
    }

    when (tab) {
        SavedFoodsTab.RECIPES -> {
            recipesSection(recipes, onLog, onNewRecipe, onOpenRecipe)
            return
        }
        SavedFoodsTab.RESTAURANTS -> {
            items(POPULAR_SPOTS, key = { "spot_$it" }) { spot ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    strong = true,
                    onClick = { onOpenSpot(spot) }
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(spot, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            return
        }
        else -> Unit
    }

    if (tab == SavedFoodsTab.ALL && foods.isNotEmpty()) {
        item(key = "saved_sort") { SortMenu(sort, onSortChange) }
    }

    if (foods.isEmpty()) {
        item(key = "saved_empty") {
            Text(
                when (tab) {
                    SavedFoodsTab.FAVORITES -> stringResource(R.string.favorites_empty)
                    else -> stringResource(R.string.saved_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )
        }
    } else {
        items(if (tab == SavedFoodsTab.ALL) sort.sort(foods) else foods, key = { "saved_list_${it.id}" }) { food ->
            SavedFoodRow(food, onClick = { onLog(food) })
        }
    }
}

/** One small "sort: A–Z" button instead of a row of chips. */
@Composable
private fun SortMenu(sort: FoodSort, onSortChange: (FoodSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box {
            TextButton(onClick = { open = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(sort.label))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                FoodSort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(stringResource(s.label), fontWeight = if (s == sort) FontWeight.Bold else null) },
                        onClick = { onSortChange(s); open = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedFoodRow(food: CustomFood, onClick: () -> Unit) {
    ResultCard(
        title = food.name,
        subtitle = null,
        kcal = food.kcalPer100,
        macros = macroLine(food.proteinPer100, food.carbsPer100, food.fatPer100),
        per = perLabel(food.unit),
        source = null,
        leading = null,
        onClick = onClick,
        rated = food
    )
}

/** Recipes tab: create a recipe, or tap one to log it (the portion dialog's edit opens the editor). */
private fun LazyListScope.recipesSection(
    recipes: List<RecipeListItem>,
    onLog: (CustomFood) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenRecipe: (Int) -> Unit
) {
    item(key = "recipe_new") {
        OutlinedButton(onClick = onNewRecipe, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.recipe_new))
        }
    }
    if (recipes.isEmpty()) {
        item(key = "recipe_empty") {
            Text(
                stringResource(R.string.recipe_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        }
    }
    items(recipes, key = { "recipe_${it.recipe.id}" }) { item ->
        val food = item.food
        if (food != null) {
            ResultCard(
                title = item.recipe.name,
                subtitle = null,
                kcal = food.kcalPer100,
                macros = macroLine(food.proteinPer100, food.carbsPer100, food.fatPer100),
                per = perLabel(food.unit),
                source = null,
                leading = Icons.Outlined.MenuBook,
                onClick = { onLog(food) },
                rated = food
            )
        } else {
            // A recipe without a saved food yet can only be opened and finished.
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                strong = true,
                onClick = { onOpenRecipe(item.recipe.id) }
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(item.recipe.name, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
