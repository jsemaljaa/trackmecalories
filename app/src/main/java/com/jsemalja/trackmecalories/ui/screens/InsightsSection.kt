package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.FoodSatiety
import com.jsemalja.trackmecalories.data.MIN_RATED_MEALS
import com.jsemalja.trackmecalories.data.SatietyInsights
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import kotlin.math.roundToInt

/**
 * Trends > Insights: which foods keep you full, from the meal ratings on Today. Every row
 * shows its sample size, since a handful of ratings is only a hint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsSection(insights: SatietyInsights) {
    var perKcal by rememberSaveable { mutableStateOf(false) }
    val foods = if (perKcal) {
        insights.foods.filter { it.fullnessPer100Kcal != null }.sortedByDescending { it.fullnessPer100Kcal }
    } else {
        insights.foods
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.insights_desc, MIN_RATED_MEALS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (insights.foods.isEmpty()) {
                Text(stringResource(R.string.insights_empty, MIN_RATED_MEALS), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.insights_so_far, pluralStringResource(R.plurals.rated_meals, insights.ratedMeals, insights.ratedMeals)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.fullness, R.string.insights_per_100_kcal).forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = perKcal == (i == 1),
                        onClick = { perKcal = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        colors = appSegmentedColors(),
                        icon = {}
                    ) { Text(stringResource(label)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            foods.forEachIndexed { i, food ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                InsightRow(food, perKcal)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                pluralStringResource(R.plurals.rated_meals, insights.ratedMeals, insights.ratedMeals),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightRow(food: FoodSatiety, perKcal: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    pluralStringResource(R.plurals.meals_count, food.meals, food.meals),
                    stringResource(R.string.insights_meal_kcal, "%,d".format(food.avgMealKcal.roundToInt())),
                    food.avgEnergy?.let { stringResource(R.string.insights_energy, "%.1f".format(it)) }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            if (perKcal) {
                Text("%.2f".format(food.fullnessPer100Kcal ?: 0.0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.insights_per_100_kcal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("%.1f".format(food.avgFullness), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.insights_out_of_5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
