package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.AdaptiveMaintenance
import com.jsemalja.trackmecalories.data.WeightEntry
import com.jsemalja.trackmecalories.data.WeightPoint
import com.jsemalja.trackmecalories.data.bmi
import com.jsemalja.trackmecalories.data.bmiCategory
import com.jsemalja.trackmecalories.data.weightStats
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource
import com.jsemalja.trackmecalories.data.grouped
import com.jsemalja.trackmecalories.data.fixed
import com.jsemalja.trackmecalories.data.currentDate
import com.jsemalja.trackmecalories.data.minusDays
import com.jsemalja.trackmecalories.data.isAfter
import com.jsemalja.trackmecalories.data.isBefore
import kotlinx.datetime.daysUntil
import com.jsemalja.trackmecalories.data.formatDate
import com.jsemalja.trackmecalories.data.toPickerMillis
import com.jsemalja.trackmecalories.data.dateOfPickerMillis

private enum class WeightRange(val label: StringResource, val days: Long?) {
    MONTH(Res.string.range_1m, 30),
    QUARTER(Res.string.range_3m, 91),
    YEAR(Res.string.range_1y, 365),
    ALL(Res.string.range_all, null)
}

/** "72.4" with one decimal, localised (72,4 in Czech/Russian). */
fun kg(value: Double): String = value.fixed(1)

private fun signedKg(value: Double): String = (if (value > 0) "+" else if (value < 0) "−" else "") + kg(abs(value))

/**
 * Trends > Weight: trend weight (smoothed), progress to the goal, weekly rate and projected
 * date, BMI, measured maintenance, a chart of readings vs trend, and the history.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeightSection(
    points: List<WeightPoint>,
    entries: List<WeightEntry>,
    goalKg: Double?,
    heightCm: Double?,
    adaptive: AdaptiveMaintenance?,
    onLog: (Double, LocalDate) -> Unit,
    onDelete: (WeightEntry) -> Unit,
    onSetGoal: (Double?) -> Unit
) {
    var showLog by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var range by rememberSaveable { mutableStateOf(WeightRange.QUARTER) }
    val stats = remember(points, goalKg) { weightStats(points, goalKg) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { showLog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.MonitorWeight, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.weight_log))
        }

        if (stats == null) {
            Text(
                stringResource(Res.string.weight_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        } else {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.weight_trend), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ValueWithUnit(
                        kg(stats.trend),
                        stringResource(Res.string.unit_kg),
                        MaterialTheme.typography.displaySmall,
                        unitStyle = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(Res.string.weight_last_reading, kg(stats.latest), formatDate(stats.latestDate, "d MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(Res.string.weight_since_start, signedKg(stats.change), formatDate(stats.startDate, "d MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(Res.string.weight_trend_explainer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeightTile(
                    label = stringResource(Res.string.weight_weekly),
                    value = stats.weeklyRate?.let { signedKg(it) } ?: "–",
                    unit = stringResource(Res.string.unit_kg_per_week),
                    detail = stringResource(if (stats.weeklyRate == null) Res.string.weight_weekly_need_data else Res.string.weight_weekly_detail),
                    modifier = Modifier.weight(1f)
                )
                WeightTile(
                    label = stringResource(Res.string.weight_goal),
                    value = goalKg?.let { kg(it) } ?: "–",
                    unit = stringResource(Res.string.unit_kg),
                    detail = when {
                        goalKg == null -> stringResource(Res.string.weight_goal_set)
                        stats.projectedGoalDate != null -> stringResource(
                            Res.string.weight_goal_eta,
                            signedKg(stats.toGoal!!),
                            formatDate(stats.projectedGoalDate!!, "d MMM yyyy")
                        )
                        else -> stringResource(Res.string.weight_goal_to_go, signedKg(stats.toGoal!!))
                    },
                    modifier = Modifier.weight(1f),
                    onEdit = { showGoal = true }
                )
            }

            bmi(stats.trend, heightCm)?.let { value ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.bmi), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text(value.fixed(1), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(" · " + stringResource(bmiCategory(value)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.weight_maintenance), style = MaterialTheme.typography.labelLarge)
                    if (adaptive != null) {
                        ValueWithUnit(adaptive.kcal.grouped(), stringResource(Res.string.unit_kcal_day), MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(Res.string.weight_maintenance_detail, adaptive.loggedDays, adaptive.spanDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(stringResource(Res.string.weight_maintenance_need_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        WeightRange.entries.forEachIndexed { i, r ->
                            SegmentedButton(
                                selected = range == r,
                                onClick = { range = r },
                                shape = SegmentedButtonDefaults.itemShape(i, WeightRange.entries.size),
                                colors = appSegmentedColors(),
                                icon = {}
                            ) { FitText(stringResource(r.label)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val from = range.days?.let { currentDate().minusDays(it) }
                    WeightChart(points.filter { from == null || !it.date.isBefore(from) }, goalKg)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendItem(MaterialTheme.colorScheme.primary, stringResource(Res.string.weight_trend))
                        LegendItem(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), stringResource(Res.string.weight_readings))
                    }
                }
            }

            Text(stringResource(Res.string.weight_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val trendByDate = points.associate { it.date.toString() to it.trend }
            entries.sortedByDescending { it.date }.forEach { entry ->
                AppCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDate(LocalDate.parse(entry.date), "EEE d MMM yyyy"),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${kg(entry.weightKg)} ${stringResource(Res.string.unit_kg)}", fontWeight = FontWeight.SemiBold)
                            trendByDate[entry.date]?.let {
                                Text(stringResource(Res.string.weight_trend_short, kg(it)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onDelete(entry) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showLog) {
        LogWeightDialog(
            initialKg = entries.maxByOrNull { it.date }?.weightKg,
            onDismiss = { showLog = false },
            onSave = { kgValue, date -> onLog(kgValue, date); showLog = false }
        )
    }
    if (showGoal) {
        GoalWeightDialog(
            current = goalKg,
            onDismiss = { showGoal = false },
            onSave = { onSetGoal(it); showGoal = false }
        )
    }
}

@Composable
private fun WeightTile(label: String, value: String, unit: String, detail: String, modifier: Modifier, onEdit: (() -> Unit)? = null) {
    AppCard(modifier.fillMaxHeight(), onClick = onEdit) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (onEdit != null) Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ValueWithUnit(value, unit, MaterialTheme.typography.headlineSmall)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 14.dp, height = 3.dp)) { drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height, cap = StrokeCap.Round) }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Readings as dots, the trend as a line, the goal as a dashed line. */
@Composable
private fun WeightChart(points: List<WeightPoint>, goalKg: Double?) {
    val colors = MaterialTheme.colorScheme
    if (points.size < 2) {
        Text(stringResource(Res.string.weight_chart_need_two), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    val values = points.flatMap { listOf(it.weight, it.trend) } + listOfNotNull(goalKg?.takeIf { g ->
        // Only include the goal in the scale when it's reasonably near, so a distant goal
        // doesn't flatten the line.
        val mid = points.last().trend
        abs(g - mid) < 8
    })
    val min = values.min() - 0.5
    val max = values.max() + 0.5
    val firstDay = points.first().date
    val spanDays = firstDay.daysUntil(points.last().date).coerceAtLeast(1).toFloat()

    Row {
        Column(Modifier.height(180.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(kg(max), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(kg(min), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                fun x(d: LocalDate) = firstDay.daysUntil(d) / spanDays * size.width
                fun y(v: Double) = ((max - v) / (max - min)).toFloat() * size.height

                goalKg?.takeIf { it in min..max }?.let { g ->
                    drawLine(
                        colors.secondary, Offset(0f, y(g)), Offset(size.width, y(g)),
                        strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }
                points.forEach { p ->
                    drawCircle(colors.onSurfaceVariant.copy(alpha = 0.45f), radius = 3.dp.toPx(), center = Offset(x(p.date), y(p.weight)))
                }
                val path = Path()
                points.forEachIndexed { i, p ->
                    val px = x(p.date)
                    if (i == 0) path.moveTo(px, y(p.trend)) else path.lineTo(px, y(p.trend))
                }
                drawPath(path, colors.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(points.first().date, "d MMM"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(formatDate(points.last().date, "d MMM"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Log a weigh-in for today, or another day via the date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWeightDialog(initialKg: Double?, onDismiss: () -> Unit, onSave: (Double, LocalDate) -> Unit) {
    var value by remember { mutableStateOf(initialKg?.let { kg(it) } ?: "") }
    var date by remember { mutableStateOf(currentDate()) }
    var showPicker by remember { mutableStateOf(false) }
    val parsed = value.toDecimal()?.takeIf { it in 20.0..400.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.weight_log)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(Res.string.calc_weight_kg)) },
                    suffix = { Text(stringResource(Res.string.unit_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (date == currentDate()) stringResource(Res.string.today)
                        else formatDate(date, "EEE d MMM yyyy")
                    )
                }
                Text(stringResource(Res.string.weight_log_tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(enabled = parsed != null, onClick = { onSave(parsed!!, date) }) { Text(stringResource(Res.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val picked = dateOfPickerMillis(it)
                        if (!picked.isAfter(currentDate())) date = picked
                    }
                    showPicker = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.cancel)) } }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalWeightDialog(current: Double?, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    var value by remember { mutableStateOf(current?.let { kg(it) } ?: "") }
    val parsed = value.toDecimal()?.takeIf { it in 20.0..400.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.weight_goal)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(Res.string.weight_goal_kg)) },
                suffix = { Text(stringResource(Res.string.unit_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(enabled = parsed != null, onClick = { onSave(parsed) }) { Text(stringResource(Res.string.save)) } },
        dismissButton = {
            // Three buttons can outgrow a narrow dialog; these two wrap under each other then.
            FlowRow(horizontalArrangement = Arrangement.End) {
                if (current != null) TextButton(onClick = { onSave(null) }) { Text(stringResource(Res.string.weight_goal_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            }
        }
    )
}
