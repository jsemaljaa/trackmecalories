package com.jsemalja.trackmecalories.ui.screens

import com.jsemalja.trackmecalories.data.WeeklyBudget
import com.jsemalja.trackmecalories.data.HealthStatus
import com.jsemalja.trackmecalories.QuickAction
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import java.time.LocalDateTime
import com.jsemalja.trackmecalories.data.whatFits
import com.jsemalja.trackmecalories.data.timeSuggestions
import com.jsemalja.trackmecalories.data.FitSuggestion
import com.jsemalja.trackmecalories.data.FoodHabit
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.TaskAlt
import kotlin.math.roundToInt
import com.jsemalja.trackmecalories.data.kcalUncertainty
import com.jsemalja.trackmecalories.data.Confidence
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.isGlass
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.ui.res.stringResource
import com.jsemalja.trackmecalories.R
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.WaterEntry
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroFiber
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import com.jsemalja.trackmecalories.ui.theme.MacroSalt
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

// Section order for the daily log; anything else (custom meal types) is appended alphabetically.
private val MEAL_ORDER = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Quick Add")

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit,
    onOpenScanner: () -> Unit,
    // From a launcher shortcut or the widget.
    request: QuickAction? = null,
    onRequestHandled: () -> Unit = {}
) {
    val entries by viewModel.foodEntries.collectAsState()
    val goals by viewModel.userGoal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val waterEntries by viewModel.waterEntries.collectAsState()
    val dayComplete by viewModel.selectedDayComplete.collectAsState()
    val weekly by viewModel.weeklyBudget.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    val activity by viewModel.dayActivity.collectAsState()
    val history by viewModel.historyEntries.collectAsState()
    val savedFoods by viewModel.allCustomFoods.collectAsState()
    var showWhatFits by remember { mutableStateOf(false) }
    val isToday = selectedDate == LocalDate.now()
    // Recomputed when the history changes; the clock moving a few minutes doesn't matter.
    val suggestions = remember(history, savedFoods, isToday) {
        if (isToday) timeSuggestions(history, savedFoods, LocalDateTime.now()) else emptyList()
    }

    var showManualDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showLogWeight by remember { mutableStateOf(false) }
    LaunchedEffect(request) {
        when (request) {
            QuickAction.QUICK_ADD -> showQuickAddDialog = true
            QuickAction.LOG_WEIGHT -> showLogWeight = true
            else -> return@LaunchedEffect
        }
        onRequestHandled()
    }
    val weightEntries by viewModel.weightEntries.collectAsState()
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }

    var showPortionDialogFor by remember { mutableStateOf<CustomFood?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Resolved here: the snackbar is shown from a coroutine, outside composition.
    val deletedMessage = stringResource(R.string.deleted_entry)
    val undoLabel = stringResource(R.string.undo)

    // Every delete (swipe or the dialog's button) is undoable for a few seconds, so an
    // accidental swipe never loses anything.
    fun deleteWithUndo(entry: FoodEntry) {
        viewModel.deleteEntry(entry)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage.format(entry.name),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(entry)
        }
    }

    val grouped = remember(entries) { entries.groupBy { it.mealType } }
    val orderedMeals = remember(grouped) {
        MEAL_ORDER.filter { grouped.containsKey(it) } +
            grouped.keys.filterNot { it in MEAL_ORDER }.sorted()
    }

    Scaffold(
        // Padded by the app scaffold, so the button and snackbar sit above the nav bar.
        modifier = modifier,
        // The app-level Scaffold already applies system-bar insets; applying them again here
        // pushed Home's content down by an extra status-bar height.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // One entry point for every way of adding food (see AddFoodSheet).
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_food))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            DateSelector(selectedDate) { viewModel.changeDate(it) }
            Spacer(Modifier.height(8.dp))

            // Summary cards scroll with the log, so a long day doesn't get squeezed into
            // the bottom third of the screen.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp), // clear the add button
                modifier = Modifier.weight(1f)
            ) {
                // The flexible target only applies to today.
                item(key = "progress") { DailyProgressCard(entries, goals, if (selectedDate == LocalDate.now()) weekly else null) }
                item(key = "water") {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        WaterCard(
                            waterEntries = waterEntries,
                            goalMl = goals.waterGoalMl,
                            onAddWater = { viewModel.addWater(it) },
                            onRemove = { viewModel.deleteWaterEntry(it) }
                        )
                        // Only once connected (Settings > Health data); otherwise nothing to show.
                        val dayActivity = activity
                        if (healthStatus == HealthStatus.CONNECTED && dayActivity != null) {
                            Spacer(Modifier.height(8.dp))
                            ActivityCard(dayActivity)
                        }
                        if (suggestions.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            SuggestionsRow(suggestions) { habit ->
                                showPortionDialogFor = habit.food.copy(defaultAmount = habit.typicalAmount)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(stringResource(R.string.daily_log), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (entries.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(R.string.no_entries_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        )
                    }
                } else {
                    orderedMeals.forEach { meal ->
                        val mealEntries = grouped[meal].orEmpty()
                        item(key = "header_$meal") {
                            MealSectionHeader(meal, mealEntries.sumOf { it.kcal })
                        }
                        itemsIndexed(mealEntries, key = { _, entry -> entry.id }) { index, entry ->
                            // One rounded block per meal: outer corners round, inner ones tight.
                            val shape = groupedShape(index, mealEntries.size)
                            SwipeToDeleteContainer(
                                shape = shape,
                                onDelete = { deleteWithUndo(entry) }
                            ) {
                                FoodItemRow(
                                    entry = entry,
                                    shape = shape,
                                    onClick = { editingEntry = entry }
                                )
                            }
                        }
                        item(key = "spacer_$meal") { Spacer(Modifier.height(8.dp)) }
                    }
                }

                // Not for future days: there's nothing to judge yet.
                if (!selectedDate.isAfter(LocalDate.now())) {
                    item(key = "day_status") {
                        DayStatusRow(
                            complete = dayComplete,
                            onChange = { viewModel.setDayComplete(selectedDate, it) }
                        )
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        CustomFoodEntryDialog(
            onDismiss = { showManualDialog = false },
            onSave = { customFood ->
                showPortionDialogFor = customFood
                showManualDialog = false
            }
        )
    }

    if (showPortionDialogFor != null) {
        PortionEntryDialog(
            food = showPortionDialogFor!!,
            onDismiss = { showPortionDialogFor = null },
            onConfirm = { amount, mealType, confidence ->
                viewModel.logCustomFood(showPortionDialogFor!!, amount, mealType, selectedDate.toString(), confidence)
                showPortionDialogFor = null
            }
        )
    }

    if (showAddMenu) {
        AddFoodSheet(
            onDismiss = { showAddMenu = false },
            onSearch = { showAddMenu = false; onOpenSearch() },
            onScan = { showAddMenu = false; onOpenScanner() },
            onQuickAdd = { showAddMenu = false; showQuickAddDialog = true },
            onCustom = { showAddMenu = false; showManualDialog = true },
            onLogWeight = { showAddMenu = false; showLogWeight = true },
            onWhatFits = { showAddMenu = false; showWhatFits = true }
        )
    }

    if (showWhatFits) {
        val remainingKcal = (goals.calorieGoal - entries.sumOf { it.kcal }).toDouble()
        val remainingProtein = goals.proteinGoal - entries.sumOf { it.protein }
        WhatFitsDialog(
            fits = remember(history, savedFoods, remainingKcal, remainingProtein) {
                whatFits(history, savedFoods, remainingKcal, remainingProtein)
            },
            remainingKcal = remainingKcal,
            onDismiss = { showWhatFits = false },
            onPick = { fit ->
                showWhatFits = false
                showPortionDialogFor = fit.food.copy(defaultAmount = fit.amount)
            }
        )
    }

    if (showLogWeight) {
        LogWeightDialog(
            initialKg = weightEntries.maxByOrNull { it.date }?.weightKg,
            onDismiss = { showLogWeight = false },
            onSave = { kg, date -> viewModel.logWeight(kg, date); showLogWeight = false }
        )
    }

    if (showQuickAddDialog) {
        QuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onSave = { kcal -> viewModel.addQuickCalories(kcal) }
        )
    }

    editingEntry?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onUpdate = { updated -> viewModel.updateEntry(updated) },
            onCopyToDate = { date -> viewModel.copyEntryToDate(entry, date) },
            onDelete = {
                editingEntry = null
                deleteWithUndo(entry)
            }
        )
    }
}

@Composable
fun MealSectionHeader(meal: String, totalKcal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(mealLabel(meal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("$totalKcal ${stringResource(R.string.unit_kcal)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WaterCard(
    waterEntries: List<WaterEntry>,
    goalMl: Double,
    onAddWater: (Double) -> Unit,
    onRemove: (WaterEntry) -> Unit
) {
    val totalMl = waterEntries.sumOf { it.amountMl }
    var showEntries by remember { mutableStateOf(false) }

    // Tap the card to see (and undo) the day's water entries.
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tint = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = { showEntries = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = if (isGlass) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.water), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${totalMl.toInt()} / ${goalMl.toInt()} ${stringResource(R.string.unit_ml)}", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = { onAddWater(250.0) }) { Text("+250") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { onAddWater(500.0) }) { Text("+500") }
        }
    }

    if (showEntries) {
        AlertDialog(
            onDismissRequest = { showEntries = false },
            title = { Text(stringResource(R.string.water_entries_title)) },
            text = {
                if (waterEntries.isEmpty()) {
                    Text(stringResource(R.string.water_entries_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column {
                        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
                        waterEntries.sortedByDescending { it.time }.forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormat),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(72.dp)
                                )
                                Text("${entry.amountMl.toInt()} ${stringResource(R.string.unit_ml)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onRemove(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEntries = false }) { Text(stringResource(R.string.close)) } }
        )
    }
}

/**
 * Today's calories at a glance: a ring filling up toward the goal with what's left (or how
 * far over) in the middle, eaten/goal beside it, and each nutrient as "eaten / goal".
 */
@Composable
fun DailyProgressCard(entries: List<FoodEntry>, goals: UserGoal, weekly: WeeklyBudget? = null) {
    val colors = MaterialTheme.colorScheme
    val eaten = entries.sumOf { it.kcal }
    // With weekly budgeting, today's target flexes with the rest of the week.
    val goal = (weekly?.todayTarget ?: goals.calorieGoal).coerceAtLeast(1)
    val remaining = goal - eaten
    val isOver = remaining < 0
    val statusColor = if (isOver) colors.error else colors.primary
    // Glass tints the card with primaryContainer; flat keeps it neutral.
    val onCard = if (isGlass) colors.onPrimaryContainer else colors.onSurface

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tint = colors.primaryContainer
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CalorieRing(
                    progress = eaten.toFloat() / goal,
                    color = colors.primary,
                    overColor = colors.error,
                    trackColor = onCard.copy(alpha = 0.12f),
                    modifier = Modifier.size(132.dp)
                ) {
                    Text(
                        "%,d".format(kotlin.math.abs(remaining)),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) colors.error else onCard
                    )
                    Text(
                        stringResource(if (isOver) R.string.kcal_over else R.string.kcal_left),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOver) colors.error else onCard.copy(alpha = 0.75f)
                    )
                }

                Spacer(Modifier.width(20.dp))

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalorieStat(stringResource(R.string.eaten), eaten, onCard, uncertainty = entries.kcalUncertainty().roundToInt())
                    CalorieStat(stringResource(if (weekly != null) R.string.goal_today_flex else R.string.goal), goal, onCard)
                    HorizontalDivider(color = onCard.copy(alpha = 0.15f))
                    Text(
                        when {
                            eaten == 0 -> stringResource(R.string.nothing_logged_yet)
                            isOver -> stringResource(R.string.percent_of_goal, eaten * 100 / goal)
                            else -> stringResource(R.string.percent_of_goal_eaten, eaten * 100 / goal)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOver) statusColor else onCard.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Fixed 3-column grid so the second row lines up with the first.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutrientProgress(stringResource(R.string.protein), entries.sumOf { it.protein }, goals.proteinGoal, MacroProtein, Modifier.weight(1f))
                NutrientProgress(stringResource(R.string.carbs), entries.sumOf { it.carbs }, goals.carbGoal, MacroCarbs, Modifier.weight(1f))
                NutrientProgress(stringResource(R.string.fat), entries.sumOf { it.fat }, goals.fatGoal, MacroFat, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutrientProgress(stringResource(R.string.fiber), entries.sumOf { it.fiber }, goals.fiberGoal, MacroFiber, Modifier.weight(1f))
                // Salt is a limit, not a target — going over it is flagged.
                NutrientProgress(stringResource(R.string.salt), entries.sumOf { it.salt }, goals.saltGoal, MacroSalt, Modifier.weight(1f), isLimit = true, decimals = true)
                Spacer(Modifier.weight(1f))
            }
            if (weekly != null) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = onCard.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(
                        R.string.week_budget_line,
                        "%,d".format(weekly.weekLeft - eaten),
                        "%,d".format(weekly.weekBudget),
                        weekly.daysLeft
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = onCard.copy(alpha = 0.8f)
                )
                if (weekly.capped) {
                    Text(stringResource(R.string.week_budget_capped), style = MaterialTheme.typography.labelSmall, color = onCard.copy(alpha = 0.65f))
                }
            }
        }
    }
}

@Composable
private fun CalorieStat(label: String, kcal: Int, color: Color, uncertainty: Int = 0) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("%,d".format(kcal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(" " + stringResource(R.string.unit_kcal), style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 3.dp))
            // How far off the total could be, given how each entry was measured.
            if (uncertainty >= 5) {
                Text(
                    "  ± %,d".format(uncertainty),
                    style = MaterialTheme.typography.labelMedium,
                    color = color.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

/**
 * Circular progress toward the goal. Past 100% the ring is full and the excess is drawn
 * on top in [overColor], so "over" is visible at a glance instead of just a full bar.
 */
@Composable
private fun CalorieRing(
    progress: Float,
    color: Color,
    overColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 2f), label = "calorieRing")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val style = Stroke(width = stroke, cap = StrokeCap.Round)
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = stroke))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated.coerceAtMost(1f), false, topLeft, arcSize, style = style)
            }
            if (animated > 1f) {
                drawArc(overColor, -90f, 360f * (animated - 1f).coerceAtMost(1f), false, topLeft, arcSize, style = style)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun NutrientProgress(
    label: String,
    value: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier,
    isLimit: Boolean = false,
    decimals: Boolean = false
) {
    val onCard = if (isGlass) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val overLimit = isLimit && value > goal
    val fmt: (Double) -> String = { if (decimals) "%.1f".format(it).removeSuffix(".0").removeSuffix(",0") else "${it.toInt()}" }

    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = onCard)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                fmt(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (overLimit) MaterialTheme.colorScheme.error else onCard
            )
            Text(
                " / ${fmt(goal)} ${stringResource(R.string.unit_g)}",
                style = MaterialTheme.typography.labelSmall,
                color = onCard.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (overLimit) MaterialTheme.colorScheme.error else color)
            )
        }
    }
}

@Composable
fun FoodItemRow(entry: FoodEntry, shape: Shape, onClick: () -> Unit) {
    val amount = amountLabel(entry.amount, entry.unit)
    AppCard(
        onClick = onClick,
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.entry_line, amount, entry.protein.toInt(), entry.carbs.toInt(), entry.fat.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                val confidence = Confidence.from(entry.confidence)
                // Estimated is the default; only mark the outliers.
                if (confidence != Confidence.ESTIMATED) {
                    Icon(
                        confidenceIcon(confidence),
                        contentDescription = stringResource(confidence.label),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).align(Alignment.CenterVertically)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text("${entry.kcal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    " " + stringResource(R.string.unit_kcal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/** Corner shape for row [index] of [count] in a grouped list: rounded only at the block's ends. */
private fun groupedShape(index: Int, count: Int): Shape {
    val outer = 18.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            // Reset right away. Undo brings the entry back with the same id, and the list
            // can reuse this row's state for it — left "dismissed", the restored row stayed
            // swiped off-screen (or got deleted again), so Undo looked like it did nothing.
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Only while swiping: glass rows are translucent, so a resting icon would show through.
            if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            val color by animateColorAsState(
                when (state.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }, label = ""
            )
            Box(
                modifier = Modifier.fillMaxSize().clip(shape).background(color).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = { content() }
    )
}

@Composable
fun QuickAddDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var kcal by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_quick)) },
        text = {
            OutlinedTextField(
                value = kcal,
                onValueChange = { kcal = it },
                label = { Text(stringResource(R.string.quick_add_total)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.quick_add_example)) }
            )
        },
        confirmButton = {
            Button(onClick = {
                kcal.toIntOrNull()?.let { onSave(it) }
                onDismiss()
            }) { Text(stringResource(R.string.recipe_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/**
 * Header with the selected date (tap for a full calendar), week navigation, and a strip
 * of the 7 days in the selected date's week.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val locale = Locale.getDefault()
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))

    val title = when (selectedDate) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    val subtitle = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDateSelected(selectedDate.minusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_week))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { showPicker = true }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.open_calendar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = { onDateSelected(selectedDate.plusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_week))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            (0L..6L).forEach { offset ->
                val date = weekStart.plusDays(offset)
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    isFuture = date.isAfter(today),
                    locale = locale,
                    modifier = Modifier.weight(1f),
                    onClick = { onDateSelected(date) }
                )
            }
        }

        if (selectedDate != today) {
            TextButton(
                onClick = { onDateSelected(today) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.back_to_today))
            }
        }
    }

    if (showPicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateSelected(date)
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val contentColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.primary
        isFuture -> colors.onSurface.copy(alpha = 0.45f)
        else -> colors.onSurface
    }

    Column(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .clip(shape)
            .background(if (isSelected) colors.primary else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
                .replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = contentColor.alpha * 0.8f),
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/** Every way to add food, behind Home's single + button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onQuickAdd: () -> Unit,
    onCustom: () -> Unit,
    onLogWeight: () -> Unit,
    onWhatFits: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            Text(
                stringResource(R.string.add_food),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
            AddFoodAction(Icons.Outlined.AutoAwesome, stringResource(R.string.what_fits), stringResource(R.string.what_fits_desc), onWhatFits)
            AddFoodAction(Icons.Outlined.Search, stringResource(R.string.add_search), stringResource(R.string.add_search_desc), onSearch)
            AddFoodAction(Icons.Outlined.QrCodeScanner, stringResource(R.string.add_scan), stringResource(R.string.add_scan_desc), onScan)
            AddFoodAction(Icons.Outlined.Bolt, stringResource(R.string.add_quick), stringResource(R.string.add_quick_desc), onQuickAdd)
            AddFoodAction(Icons.Outlined.EditNote, stringResource(R.string.add_custom), stringResource(R.string.add_custom_desc), onCustom)
            HorizontalDivider(Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
            AddFoodAction(Icons.Outlined.MonitorWeight, stringResource(R.string.weight_log), stringResource(R.string.weight_log_desc), onLogWeight)
        }
    }
}

@Composable
private fun AddFoodAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * "Is this day's log finished?" Marking it incomplete keeps a forgotten dinner from reading
 * as eating less: the day is left out of averages and measured maintenance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayStatusRow(complete: Boolean?, onChange: (Boolean?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(stringResource(R.string.day_status), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = complete == true,
                onClick = { onChange(if (complete == true) null else true) },
                label = { Text(stringResource(R.string.day_complete)) },
                leadingIcon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = complete == false,
                onClick = { onChange(if (complete == false) null else false) },
                label = { Text(stringResource(R.string.day_incomplete)) },
                leadingIcon = { Icon(Icons.Outlined.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        if (complete == false) {
            Text(stringResource(R.string.day_incomplete_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Usually around now": one tap opens the food at your usual portion. */
@Composable
private fun SuggestionsRow(habits: List<FoodHabit>, onPick: (FoodHabit) -> Unit) {
    Column {
        Text(stringResource(R.string.suggestions_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits, key = { it.food.id }) { habit ->
                AssistChip(
                    onClick = { onPick(habit) },
                    label = {
                        Text(
                            "${habit.food.name} · ${amountLabel(habit.typicalAmount, habit.food.unit)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

/** Foods from your own history that fit what's left today, best protein value first. */
@Composable
private fun WhatFitsDialog(fits: List<FitSuggestion>, remainingKcal: Double, onDismiss: () -> Unit, onPick: (FitSuggestion) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.what_fits)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.what_fits_left, remainingKcal.roundToInt().coerceAtLeast(0)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (fits.isEmpty()) {
                    Text(stringResource(R.string.what_fits_empty), style = MaterialTheme.typography.bodyMedium)
                }
                fits.forEach { fit ->
                    AppCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, strong = true, onClick = { onPick(fit) }) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(fit.food.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(
                                    R.string.what_fits_line,
                                    amountLabel(fit.amount, fit.food.unit),
                                    fit.kcal.roundToInt(),
                                    fit.protein.roundToInt(),
                                    "%.1f".format(fit.fiber)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}
