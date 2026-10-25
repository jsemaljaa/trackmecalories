package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors

/** Which saved foods the Food tab lists while the search box is empty. */
enum class SavedFoodsTab(val label: String) {
    RECENT("Recent"),
    FAVORITES("Favorites"),
    ALL("All")
}

/**
 * Saved foods (recently used, pinned favorites, or everything) as a section of the Food
 * tab's list — shown while nothing is typed, so the former Recents tab needs no tab of
 * its own. Typing searches these too, accent-insensitively.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.savedFoodsSection(
    tab: SavedFoodsTab,
    onTabChange: (SavedFoodsTab) -> Unit,
    foods: List<CustomFood>,
    onLog: (CustomFood) -> Unit,
    onEdit: (CustomFood) -> Unit,
    onToggleFavorite: (CustomFood) -> Unit
) {
    item(key = "saved_tabs") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            SavedFoodsTab.entries.forEachIndexed { i, entry ->
                SegmentedButton(
                    selected = tab == entry,
                    onClick = { onTabChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(i, SavedFoodsTab.entries.size),
                    colors = appSegmentedColors(),
                    icon = {}
                ) { Text(entry.label) }
            }
        }
    }

    if (foods.isEmpty()) {
        item(key = "saved_empty") {
            Text(
                when (tab) {
                    SavedFoodsTab.FAVORITES -> "Tap the star on a saved food to pin it here."
                    else -> "Foods you log or create show up here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )
        }
    } else {
        items(foods, key = { "saved_list_${it.id}" }) { food ->
            SavedFoodRow(
                food = food,
                onClick = { onLog(food) },
                onEdit = { onEdit(food) },
                onToggleFavorite = { onToggleFavorite(food) }
            )
        }
    }
}

@Composable
fun SavedFoodRow(
    food: CustomFood,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        strong = true,
        onClick = onClick
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val refLabel = if (food.unit == "pcs") "piece" else "100 ${food.unit}"
                Text(
                    "${food.kcalPer100.toInt()} kcal per $refLabel · P ${food.proteinPer100.toInt()} · C ${food.carbsPer100.toInt()} · F ${food.fatPer100.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (food.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (food.isFavorite) "Unpin from favorites" else "Pin to favorites",
                    tint = if (food.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
