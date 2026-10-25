package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.rating
import kotlin.math.roundToInt

/** The official Nutri-Score colours. */
private fun gradeColor(grade: Char) = when (grade) {
    'A' -> Color(0xFF038141)
    'B' -> Color(0xFF85BB2F)
    'C' -> Color(0xFFFECB02)
    'D' -> Color(0xFFEE8100)
    else -> Color(0xFFE63E11)
}

/**
 * A Nutri-Score letter. An estimated grade (computed here, not from Open Food Facts) is
 * drawn as an outline so it isn't mistaken for the official label.
 */
@Composable
fun NutriScoreBadge(grade: Char, estimated: Boolean, size: Dp = 22.dp) {
    val color = gradeColor(grade)
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(size)
            .then(
                if (estimated) Modifier.border(1.5.dp, color, shape)
                else Modifier.background(color, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            grade.toString(),
            color = if (estimated) color else if (grade == 'C') Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.6f).sp
        )
    }
}

/** Badge plus the densities, for the log dialog. */
@Composable
fun RatingLine(food: CustomFood) {
    val r = food.rating()
    if (r.grade == null && food.kcalPer100 <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        r.grade?.let {
            NutriScoreBadge(it, r.estimated)
            Spacer(Modifier.width(8.dp))
        }
        val parts = buildList {
            if (r.grade != null && r.estimated) add(stringResource(R.string.nutriscore_estimated))
            add(stringResource(R.string.protein_per_100kcal, "%.1f".format(r.proteinPer100kcal)))
            if (r.fiberPer100kcal > 0) add(stringResource(R.string.fiber_per_100kcal, "%.1f".format(r.fiberPer100kcal)))
            r.energyDensity?.let { add(stringResource(R.string.energy_density, "%.1f".format(it))) }
        }
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Ways to sort the saved-foods list. */
enum class FoodSort(@androidx.annotation.StringRes val label: Int) {
    NAME(R.string.sort_name),
    NUTRISCORE(R.string.sort_nutriscore),
    PROTEIN(R.string.sort_protein),
    FIBER(R.string.sort_fiber),
    ENERGY_DENSITY(R.string.sort_energy_density);

    fun sort(foods: List<CustomFood>): List<CustomFood> = when (this) {
        NAME -> foods
        // Ungraded foods go last.
        NUTRISCORE -> foods.sortedBy { it.rating().grade ?: 'Z' }
        PROTEIN -> foods.sortedByDescending { it.rating().proteinPer100kcal }
        FIBER -> foods.sortedByDescending { it.rating().fiberPer100kcal }
        ENERGY_DENSITY -> foods.sortedBy { it.rating().energyDensity ?: Double.MAX_VALUE }
    }
}

/** "12" for whole numbers, one decimal otherwise — compact figures in rows. */
internal fun Double.short(): String = if (this >= 10) roundToInt().toString() else "%.1f".format(this)
