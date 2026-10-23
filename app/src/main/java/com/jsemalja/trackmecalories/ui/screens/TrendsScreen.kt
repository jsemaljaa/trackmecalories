package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Weekly summary: calories vs goal per day (trailing 7 days) and average macros.
 */
@Composable
fun TrendsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val weeklyTotals by viewModel.weeklyTotals.collectAsState()
    val goals by viewModel.userGoal.collectAsState()

    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val totalsByDate = weeklyTotals.associateBy { it.date }

    val dailyKcal = days.map { totalsByDate[it.toString()]?.totalKcal ?: 0 }
    val avgKcal = if (dailyKcal.isNotEmpty()) dailyKcal.sum() / dailyKcal.size else 0
    val avgProtein = if (weeklyTotals.isNotEmpty()) weeklyTotals.sumOf { it.totalProtein } / 7.0 else 0.0
    val avgCarbs = if (weeklyTotals.isNotEmpty()) weeklyTotals.sumOf { it.totalCarbs } / 7.0 else 0.0
    val avgFat = if (weeklyTotals.isNotEmpty()) weeklyTotals.sumOf { it.totalFat } / 7.0 else 0.0

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(Icons.Default.BarChart, "Weekly Summary")
        Spacer(Modifier.height(24.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Calories vs Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                WeeklyBarChart(
                    days = days,
                    values = dailyKcal,
                    goal = goals.calorieGoal,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Daily Average", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Row(
                Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AverageStat("Calories", "$avgKcal", "kcal")
                AverageStat("Protein", "${avgProtein.toInt()}", "g")
                AverageStat("Carbs", "${avgCarbs.toInt()}", "g")
                AverageStat("Fat", "${avgFat.toInt()}", "g")
            }
        }
    }
}

@Composable
private fun AverageStat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("$label ($unit)", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WeeklyBarChart(
    days: List<LocalDate>,
    values: List<Int>,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val overColor = MaterialTheme.colorScheme.error
    val goalLineColor = MaterialTheme.colorScheme.onSurface
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(goal).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val slot = size.width / values.size
            val barWidth = slot * 0.5f
            val goalY = size.height * (1f - goal.toFloat() / maxValue)

            drawLine(
                color = goalLineColor.copy(alpha = 0.5f),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )

            values.forEachIndexed { index, value ->
                val barHeight = size.height * (value.toFloat() / maxValue)
                val x = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    color = if (value > goal) overColor else barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
