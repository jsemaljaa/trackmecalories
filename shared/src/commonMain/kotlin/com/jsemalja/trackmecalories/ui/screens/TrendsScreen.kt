package com.jsemalja.trackmecalories.ui.screens

import org.jetbrains.compose.resources.pluralStringResource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.DailyTotal
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource
import com.jsemalja.trackmecalories.data.grouped
import com.jsemalja.trackmecalories.data.fixed
import com.jsemalja.trackmecalories.data.currentDate
import com.jsemalja.trackmecalories.data.plusDays
import com.jsemalja.trackmecalories.data.minusDays
import com.jsemalja.trackmecalories.data.minusWeeks
import com.jsemalja.trackmecalories.data.isAfter
import com.jsemalja.trackmecalories.data.weekStartOf
import com.jsemalja.trackmecalories.data.formatDate
import com.jsemalja.trackmecalories.data.shortName

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

    val today = currentDate()
    val goal = goals.calorieGoal.coerceAtLeast(1)
    val days = (0L..6L).map { weekStart.plusDays(it) }
    val byDate = totals.associateBy { it.date }
    val incompleteDays by viewModel.trendsIncompleteDays.collectAsState()
    val weekTotals = days.map { byDate[it.toString()] }
    // Days marked incomplete are shown but don't count toward any average.
    val logged = weekTotals.filterNotNull().filter { it.date !in incompleteDays }
    val excludedCount = weekTotals.filterNotNull().count { it.date in incompleteDays }
    val previousLogged = (1L..7L).mapNotNull { byDate[weekStart.minusDays(it).toString()] }
        .filter { it.date !in incompleteDays }
    val daysSoFar = days.count { !it.isAfter(today) }
    val isCurrentWeek = weekStart == weekStartOf(today)
    var section by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(Icons.Default.BarChart, stringResource(Res.string.nav_trends))
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val tabs = listOf(Res.string.tab_calories, Res.string.tab_weight, Res.string.tab_insights)
            tabs.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = section == i,
                    onClick = { section = i },
                    shape = SegmentedButtonDefaults.itemShape(i, tabs.size),
                    colors = appSegmentedColors(),
                    icon = {}
                ) { FitText(stringResource(label)) }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (section == 2) {
            val insights by viewModel.satietyInsights.collectAsState()
            InsightsSection(insights)
            return@Column
        }

        if (section == 1) {
            val points by viewModel.weightPoints.collectAsState()
            val entries by viewModel.weightEntries.collectAsState()
            val adaptive by viewModel.adaptiveMaintenance.collectAsState()
            val undo = LocalUndo.current
            val deletedWeight = stringResource(Res.string.deleted_weight)
            WeightSection(
                points = points,
                entries = entries,
                goalKg = goals.targetWeightKg,
                heightCm = goals.heightCm,
                adaptive = adaptive,
                onLog = viewModel::logWeight,
                onDelete = { entry ->
                    viewModel.deleteWeight(entry)
                    undo.offer(deletedWeight) { viewModel.restoreWeight(entry) }
                },
                onSetGoal = viewModel::setTargetWeight
            )
            return@Column
        }

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
                Text(stringResource(Res.string.calories_per_day), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                ChartLegend(goal)
                Spacer(Modifier.height(12.dp))
                WeeklyBarChart(
                    days = days,
                    values = weekTotals.map { it?.totalKcal },
                    uncertainties = weekTotals.map { kotlin.math.sqrt(it?.kcalVariance ?: 0.0) },
                    incomplete = days.map { it.toString() in incompleteDays },
                    goal = goal,
                    today = today,
                    onDayClick = onOpenDay
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(Res.string.tap_day_to_open),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (logged.isEmpty()) {
            Text(
                stringResource(if (daysSoFar == 0) Res.string.week_not_started else Res.string.week_nothing_logged),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        } else {
            WeekStats(logged, previousLogged, goal, daysSoFar)
            if (excludedCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(Res.plurals.incomplete_excluded, excludedCount, excludedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            MacroAverages(logged, goals.proteinGoal, goals.carbGoal, goals.fatGoal)
        }

        // Optional (Settings > Streaks & badges).
        val gamification by viewModel.gamification.collectAsState()
        gamification?.let { state ->
            Spacer(Modifier.height(16.dp))
            ConsistencyCard(state, today)
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
    val weekEnd = weekStart.plusDays(6)
    val thisWeek = weekStartOf(today)
    val title = when (weekStart) {
        thisWeek -> stringResource(Res.string.this_week)
        thisWeek.minusWeeks(1) -> stringResource(Res.string.last_week)
        else -> stringResource(Res.string.week_of, formatDate(weekStart, "d MMM"))
    }
    val range = if (weekStart.month == weekEnd.month) {
        "${weekStart.day} – ${formatDate(weekEnd, "d MMM yyyy")}"
    } else {
        "${formatDate(weekStart, "d MMM")} – ${formatDate(weekEnd, "d MMM yyyy")}"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.previous_week))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(range, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.next_week))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(goal: Int) {
    val colors = MaterialTheme.colorScheme
    // Wraps onto a second line when the labels don't fit (narrow phone, longer language).
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendSwatch(colors.primary, stringResource(Res.string.within_goal))
        LegendSwatch(colors.error, stringResource(Res.string.over_goal))
        LegendSwatch(colors.onSurfaceVariant.copy(alpha = 0.3f), stringResource(Res.string.incomplete_short))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.width(16.dp).height(2.dp)) {
                drawLine(
                    colors.onSurfaceVariant, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.goal_value, goal.grouped()), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
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

@Composable
private fun WeeklyBarChart(
    days: List<LocalDate>,
    values: List<Int?>,
    goal: Int,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    // ± kcal per day from entry confidence; drawn as error bars.
    uncertainties: List<Double> = emptyList(),
    incomplete: List<Boolean> = emptyList()
) {
    val colors = MaterialTheme.colorScheme
    // Headroom above the tallest bar (or the goal line) for its value label.
    val maxValue = maxOf(values.indices.maxOf { (values[it] ?: 0) + (uncertainties.getOrNull(it) ?: 0.0).toInt() }, goal) * 1.08f
    val goalFraction = goal / maxValue
    // Label rows sized from the actual text, so a large system font isn't clipped.
    val labelLine = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
    val valueLabelHeight = labelLine + 2.dp
    val dayLabelHeight = labelLine * 2 + 6.dp

    Box(Modifier.fillMaxWidth().height(valueLabelHeight + BAR_AREA + dayLabelHeight)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(BAR_AREA)
                .align(Alignment.BottomCenter)
                .offset(y = -dayLabelHeight)
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
                        FitText(
                            compactKcal(value),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (value > goal) colors.error else colors.onSurface,
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
                                .background(
                                    when {
                                        incomplete.getOrNull(i) == true -> colors.onSurfaceVariant.copy(alpha = 0.3f)
                                        value > goal -> colors.error
                                        else -> colors.primary
                                    }
                                )
                        )
                    } else if (!isFuture) {
                        // Not logged: a small dash on the baseline rather than a misleading 0 bar.
                        Text("–", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                    Column(
                        Modifier.height(dayLabelHeight).padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val labelColor = when {
                            isToday -> colors.primary
                            isFuture -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                            else -> colors.onSurfaceVariant
                        }
                        FitText(
                            day.dayOfWeek.shortName().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = labelColor
                        )
                        FitText(
                            "${day.day}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = labelColor
                        )
                    }
                }
            }
        }
        // Error bars: how far each day's total could be off, from entry confidence.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(BAR_AREA)
                .align(Alignment.BottomCenter)
                .offset(y = -dayLabelHeight)
        ) {
            val slot = size.width / days.size
            values.forEachIndexed { i, v ->
                val sigma = uncertainties.getOrNull(i) ?: 0.0
                if (v == null || v <= 0 || sigma < 5) return@forEachIndexed
                val x = slot * (i + 0.5f)
                val top = size.height * (1f - ((v + sigma) / maxValue).toFloat())
                val bottom = size.height * (1f - ((v - sigma).coerceAtLeast(0.0) / maxValue).toFloat())
                val cap = slot * 0.12f
                val stroke = 1.5.dp.toPx()
                val c = colors.onSurface.copy(alpha = 0.55f)
                drawLine(c, Offset(x, top), Offset(x, bottom), strokeWidth = stroke)
                drawLine(c, Offset(x - cap, top), Offset(x + cap, top), strokeWidth = stroke)
                drawLine(c, Offset(x - cap, bottom), Offset(x + cap, bottom), strokeWidth = stroke)
            }
        }
    }
}

/** 1834 -> "1834", 12500 -> "12.5k" — keeps bar labels to one line. */
private fun compactKcal(kcal: Int): String =
    if (kcal < 10_000) "$kcal" else "${(kcal / 1000f).fixed(1)}k"

@Composable
private fun WeekStats(logged: List<DailyTotal>, previousLogged: List<DailyTotal>, goal: Int, daysSoFar: Int) {
    val colors = MaterialTheme.colorScheme
    val avg = logged.sumOf { it.totalKcal } / logged.size
    // Independent per-entry errors: the week's ± is the root of the summed variances.
    val weekSigma = kotlin.math.sqrt(logged.sumOf { it.kcalVariance })
    val avgSigma = weekSigma / logged.size
    val prevAvg = previousLogged.takeIf { it.isNotEmpty() }?.let { p -> p.sumOf { it.totalKcal } / p.size }
    val withinGoal = logged.count { it.totalKcal <= goal }
    val balance = logged.sumOf { it.totalKcal - goal }
    val kcal = stringResource(Res.string.unit_kcal)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(Res.string.daily_average),
                value = avg.grouped(),
                unit = kcal,
                detail = pluralStringResource(Res.plurals.over_logged_days, logged.size, logged.size) +
                    (if (avgSigma >= 5) "\n" + stringResource(Res.string.plus_minus_kcal, avgSigma.roundToInt().grouped()) else "") + when {
                    prevAvg == null -> ""
                    avg > prevAvg -> "\n" + stringResource(Res.string.vs_last_week_up, (avg - prevAvg).grouped())
                    avg < prevAvg -> "\n" + stringResource(Res.string.vs_last_week_down, (prevAvg - avg).grouped())
                    else -> "\n" + stringResource(Res.string.vs_last_week_same)
                },
                valueColor = if (avg > goal) colors.error else colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(Res.string.week_balance),
                value = (if (balance > 0) "+" else if (balance < 0) "−" else "") + (abs(balance)).grouped(),
                unit = kcal,
                detail = stringResource(
                    when {
                        balance > 0 -> Res.string.balance_over
                        balance < 0 -> Res.string.balance_under
                        else -> Res.string.balance_exact
                    }
                ) + if (weekSigma >= 5) "\n" + stringResource(Res.string.plus_minus_kcal, weekSigma.roundToInt().grouped()) else "",
                valueColor = if (balance > 0) colors.error else colors.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(Res.string.within_goal),
                value = "$withinGoal / ${logged.size}",
                unit = pluralStringResource(Res.plurals.days_unit, logged.size),
                detail = stringResource(Res.string.within_goal_detail, goal.grouped()),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(Res.string.days_logged),
                value = "${logged.size} / $daysSoFar",
                unit = pluralStringResource(Res.plurals.days_unit, daysSoFar),
                detail = stringResource(if (daysSoFar < 7) Res.string.so_far_this_week else Res.string.this_week_lower),
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
            ValueWithUnit(value, unit, MaterialTheme.typography.headlineSmall, valueColor = valueColor)
            Spacer(Modifier.height(2.dp))
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                Text(stringResource(Res.string.average_macros), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(Res.string.average_macros_detail),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MacroGoalRow(stringResource(Res.string.protein), protein, proteinGoal, MacroProtein)
            MacroGoalRow(stringResource(Res.string.carbs), carbs, carbGoal, MacroCarbs)
            MacroGoalRow(stringResource(Res.string.fat), fat, fatGoal, MacroFat)

            if (macroKcal > 0) {
                val shares = listOf(protein * 4 / macroKcal, carbs * 4 / macroKcal, fat * 9 / macroKcal)
                HorizontalDivider()
                Text(stringResource(Res.string.calories_come_from), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                    listOf(MacroProtein, MacroCarbs, MacroFat).forEachIndexed { i, color ->
                        if (shares[i] > 0) Box(Modifier.weight(shares[i].toFloat()).fillMaxHeight().background(color))
                    }
                }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(stringResource(Res.string.protein) to MacroProtein, stringResource(Res.string.carbs) to MacroCarbs, stringResource(Res.string.fat) to MacroFat).forEachIndexed { i, (label, color) ->
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
            Text("${value.roundToInt()} ${stringResource(Res.string.unit_g)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                " / ${goal.roundToInt()} ${stringResource(Res.string.unit_g)} · $pct%",
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
