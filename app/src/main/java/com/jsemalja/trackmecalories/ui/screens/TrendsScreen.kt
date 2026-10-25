package com.jsemalja.trackmecalories.ui.screens

import com.jsemalja.trackmecalories.ui.theme.isGlass
import com.jsemalja.trackmecalories.ui.theme.AppCard
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.DailyTotal
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.ui.viewmodel.weekStartOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Weekly summary for one calendar week (navigable): calories per day against the goal,
 * plus stats averaged over the days that were actually logged — an unlogged day isn't a
 * 0 kcal day, so it shouldn't drag the average down.
 */
@Composable
fun TrendsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenDay: (LocalDate) -> Unit = {}
) {
    val totals by viewModel.trendsTotals.collectAsState()
    val weekStart by viewModel.trendsWeekStart.collectAsState()
    val goals by viewModel.userGoal.collectAsState()

    val today = LocalDate.now()
    val goal = goals.calorieGoal.coerceAtLeast(1)
    val days = (0L..6L).map { weekStart.plusDays(it) }
    val byDate = totals.associateBy { it.date }
    val weekTotals = days.map { byDate[it.toString()] }
    val logged = weekTotals.filterNotNull()
    val previousLogged = (1L..7L).mapNotNull { byDate[weekStart.minusDays(it).toString()] }
    val daysSoFar = days.count { !it.isAfter(today) }
    val isCurrentWeek = weekStart == weekStartOf(today)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(Icons.Default.BarChart, "Trends")
        Spacer(Modifier.height(12.dp))

        WeekSelector(
            weekStart = weekStart,
            today = today,
            canGoForward = !isCurrentWeek,
            onPrevious = { viewModel.changeTrendsWeek(-1) },
            onNext = { viewModel.changeTrendsWeek(1) }
        )
        Spacer(Modifier.height(12.dp))

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Calories per day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                ChartLegend(goal)
                Spacer(Modifier.height(12.dp))
                WeeklyBarChart(
                    days = days,
                    values = weekTotals.map { it?.totalKcal },
                    goal = goal,
                    today = today,
                    onDayClick = onOpenDay
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap a day to open its log",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (logged.isEmpty()) {
            Text(
                if (daysSoFar == 0) "This week hasn't started yet." else "Nothing logged this week yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        } else {
            WeekStats(logged, previousLogged, goal, daysSoFar)
            Spacer(Modifier.height(16.dp))
            MacroAverages(logged, goals.proteinGoal, goals.carbGoal, goals.fatGoal)
        }
    }
}

@Composable
private fun WeekSelector(
    weekStart: LocalDate,
    today: LocalDate,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val locale = Locale.getDefault()
    val weekEnd = weekStart.plusDays(6)
    val thisWeek = weekStartOf(today)
    val title = when (weekStart) {
        thisWeek -> "This week"
        thisWeek.minusWeeks(1) -> "Last week"
        else -> "Week of ${weekStart.format(DateTimeFormatter.ofPattern("d MMM", locale))}"
    }
    val range = if (weekStart.month == weekEnd.month) {
        "${weekStart.dayOfMonth} – ${weekEnd.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))}"
    } else {
        "${weekStart.format(DateTimeFormatter.ofPattern("d MMM", locale))} – ${weekEnd.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))}"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(range, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun ChartLegend(goal: Int) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendSwatch(colors.primary, "Within goal")
        LegendSwatch(colors.error, "Over goal")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.width(16.dp).height(2.dp)) {
                drawLine(
                    colors.onSurfaceVariant, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("Goal ${"%,d".format(goal)}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val BAR_AREA: Dp = 160.dp
private val VALUE_LABEL: Dp = 18.dp
private val DAY_LABEL: Dp = 38.dp

@Composable
private fun WeeklyBarChart(
    days: List<LocalDate>,
    values: List<Int?>,
    goal: Int,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    // Headroom above the tallest bar (or the goal line) for its value label.
    val maxValue = maxOf(values.maxOf { it ?: 0 }, goal) * 1.08f
    val goalFraction = goal / maxValue

    Box(Modifier.fillMaxWidth().height(VALUE_LABEL + BAR_AREA + DAY_LABEL)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(BAR_AREA)
                .align(Alignment.BottomCenter)
                .offset(y = -DAY_LABEL)
        ) {
            val y = size.height * (1f - goalFraction)
            drawLine(
                colors.onSurfaceVariant.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }

        Row(Modifier.fillMaxSize()) {
            days.forEachIndexed { i, day ->
                val value = values[i]
                val isToday = day == today
                val isFuture = day.isAfter(today)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = !isFuture) { onDayClick(day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (value != null && value > 0) {
                        Text(
                            compactKcal(value),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (value > goal) colors.error else colors.onSurface,
                            maxLines = 1,
                            // Backed by the card colour so the dashed goal line doesn't strike through it.
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isGlass) Color.Transparent else colors.surfaceContainerLow)
                                .padding(horizontal = 3.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .height(BAR_AREA * (value / maxValue).coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (value > goal) colors.error else colors.primary)
                        )
                    } else if (!isFuture) {
                        // Not logged: a small dash on the baseline rather than a misleading 0 bar.
                        Text("–", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                    Column(
                        Modifier.height(DAY_LABEL).padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val labelColor = when {
                            isToday -> colors.primary
                            isFuture -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                            else -> colors.onSurfaceVariant
                        }
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = labelColor
                        )
                        Text(
                            "${day.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}

/** 1834 -> "1834", 12500 -> "12.5k" — keeps bar labels to one line. */
private fun compactKcal(kcal: Int): String =
    if (kcal < 10_000) "$kcal" else "%.1fk".format(kcal / 1000f)

@Composable
private fun WeekStats(logged: List<DailyTotal>, previousLogged: List<DailyTotal>, goal: Int, daysSoFar: Int) {
    val colors = MaterialTheme.colorScheme
    val avg = logged.sumOf { it.totalKcal } / logged.size
    val prevAvg = previousLogged.takeIf { it.isNotEmpty() }?.let { p -> p.sumOf { it.totalKcal } / p.size }
    val withinGoal = logged.count { it.totalKcal <= goal }
    val balance = logged.sumOf { it.totalKcal - goal }
    val dayWord = { n: Int -> if (n == 1) "day" else "days" }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Daily average",
                value = "%,d".format(avg),
                unit = "kcal",
                detail = buildString {
                    append("over ${logged.size} logged ${dayWord(logged.size)}")
                    if (prevAvg != null) {
                        val diff = avg - prevAvg
                        append(
                            when {
                                diff > 0 -> "\n▲ ${"%,d".format(diff)} vs last week"
                                diff < 0 -> "\n▼ ${"%,d".format(-diff)} vs last week"
                                else -> "\nsame as last week"
                            }
                        )
                    }
                },
                valueColor = if (avg > goal) colors.error else colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Week balance",
                value = (if (balance > 0) "+" else if (balance < 0) "−" else "") + "%,d".format(abs(balance)),
                unit = "kcal",
                detail = when {
                    balance > 0 -> "over your goal in total"
                    balance < 0 -> "under your goal in total"
                    else -> "exactly on goal"
                },
                valueColor = if (balance > 0) colors.error else colors.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Within goal",
                value = "$withinGoal / ${logged.size}",
                unit = dayWord(logged.size),
                detail = "logged days at or under ${"%,d".format(goal)} kcal",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Days logged",
                value = "${logged.size} / $daysSoFar",
                unit = dayWord(daysSoFar),
                detail = if (daysSoFar < 7) "so far this week" else "this week",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    unit: String,
    detail: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    AppCard(modifier = modifier.fillMaxHeight()) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MacroAverages(logged: List<DailyTotal>, proteinGoal: Double, carbGoal: Double, fatGoal: Double) {
    val n = logged.size
    val protein = logged.sumOf { it.totalProtein } / n
    val carbs = logged.sumOf { it.totalCarbs } / n
    val fat = logged.sumOf { it.totalFat } / n
    // Share of calories from each macro (4/4/9 kcal per gram).
    val macroKcal = protein * 4 + carbs * 4 + fat * 9

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Average macros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "per logged day, against your daily goals",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MacroGoalRow("Protein", protein, proteinGoal, MacroProtein)
            MacroGoalRow("Carbs", carbs, carbGoal, MacroCarbs)
            MacroGoalRow("Fat", fat, fatGoal, MacroFat)

            if (macroKcal > 0) {
                val shares = listOf(protein * 4 / macroKcal, carbs * 4 / macroKcal, fat * 9 / macroKcal)
                HorizontalDivider()
                Text("Where your calories come from", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                    listOf(MacroProtein, MacroCarbs, MacroFat).forEachIndexed { i, color ->
                        if (shares[i] > 0) Box(Modifier.weight(shares[i].toFloat()).fillMaxHeight().background(color))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Protein" to MacroProtein, "Carbs" to MacroCarbs, "Fat" to MacroFat).forEachIndexed { i, (label, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Spacer(Modifier.width(4.dp))
                            Text("$label ${(shares[i] * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroGoalRow(label: String, value: Double, goal: Double, color: Color) {
    val pct = if (goal > 0) (value / goal * 100).roundToInt() else 0
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${value.roundToInt()} g", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                " / ${goal.roundToInt()} g · $pct%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.2f))) {
            Box(
                Modifier
                    .fillMaxWidth(if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
