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
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.LocalAppTheme
import com.jsemalja.trackmecalories.ui.theme.boldShadowColor
import com.jsemalja.trackmecalories.ui.theme.glow
import com.jsemalja.trackmecalories.ui.theme.hardShadow
import com.jsemalja.trackmecalories.ui.theme.neonGlowColor
import com.jsemalja.trackmecalories.ui.theme.raisedEdge
import com.jsemalja.trackmecalories.ui.theme.tintedCards
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
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
import com.jsemalja.trackmecalories.ui.viewmodel.weekStartOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    val waterTracking by viewModel.waterTrackingEnabled.collectAsState()
    val fasting by viewModel.fastingSettings.collectAsState()
    val activeFast by viewModel.activeFast.collectAsState()
    val lastFast by viewModel.lastFast.collectAsState()
    val dayComplete by viewModel.selectedDayComplete.collectAsState()
    val weekly by viewModel.weeklyBudget.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    val activity by viewModel.dayActivity.collectAsState()
    val eatBack by viewModel.eatBack.collectAsState()
    val history by viewModel.historyEntries.collectAsState()
    val savedFoods by viewModel.allCustomFoods.collectAsState()
    var showWhatFits by remember { mutableStateOf(false) }
    val isToday = selectedDate == LocalDate.now()
    // Eaten-back activity counts for today only: past days keep the goal they were planned with.
    val activityBonus = if (isToday && healthStatus == HealthStatus.CONNECTED) eatBack.bonus(activity?.activeKcal) else 0
    // Today's real target: the weekly-budget share (if on) plus the activity bonus.
    val todayGoal = ((if (isToday) weekly?.todayTarget else null) ?: goals.calorieGoal) + activityBonus
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
    var ratingMeal by remember { mutableStateOf<String?>(null) }

    var showPortionDialogFor by remember { mutableStateOf<CustomFood?>(null) }

    val undo = LocalUndo.current
    val deletedMessage = stringResource(R.string.deleted_entry)
    val deletedWater = stringResource(R.string.deleted_water)

    // Every delete (swipe or the dialog's button) is undoable for a few seconds, so an
    // accidental swipe never loses anything.
    fun deleteWithUndo(entry: FoodEntry) {
        viewModel.deleteEntry(entry)
        undo.offer(deletedMessage.format(entry.name)) { viewModel.restoreEntry(entry) }
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
        snackbarHost = { SnackbarHost(undo.hostState) },
        floatingActionButton = {
            // One entry point for every way of adding food (see AddFoodSheet).
            // Paper inks it; Bold and Neon give it the same edge as their cards.
            val fabShape = MaterialTheme.shapes.large
            val (fabEdge, fabBorder) = raisedEdge(fabShape)
            val ink = LocalAppTheme.current == AppTheme.PAPER
            FloatingActionButton(
                onClick = { showAddMenu = true },
                modifier = fabEdge.then(if (fabBorder != null) Modifier.border(fabBorder, fabShape) else Modifier),
                shape = fabShape,
                containerColor = if (ink) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                contentColor = if (ink) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimary,
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
                item(key = "progress") { DailyProgressCard(entries, goals, if (isToday) weekly else null, activityBonus) }
                item(key = "water") {
                    Column {
                        // Optional (Settings > Fasting); a fast runs in real time, so today only.
                        if (fasting.enabled && isToday) {
                            Spacer(Modifier.height(8.dp))
                            FastingCard(
                                active = activeFast,
                                last = lastFast,
                                planHours = fasting.planHours,
                                onStart = viewModel::startFast,
                                onEnd = viewModel::endFast,
                                onMoveStart = viewModel::moveFastStart
                            )
                        }
                        // Optional (Settings > Goals > Track water).
                        if (waterTracking) {
                            Spacer(Modifier.height(8.dp))
                            WaterCard(
                                waterEntries = waterEntries,
                                goalMl = goals.waterGoalMl,
                                onAddWater = { viewModel.addWater(it) },
                                onRemove = { entry ->
                                    viewModel.deleteWaterEntry(entry)
                                    undo.offer(deletedWater) { viewModel.restoreWaterEntry(entry) }
                                }
                            )
                        }
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
                            MealSectionHeader(
                                meal,
                                mealEntries.sumOf { it.kcal },
                                satiety = mealEntries.firstNotNullOfOrNull { it.satiety },
                                energy = mealEntries.firstNotNullOfOrNull { it.energyLevel },
                                // A bare calorie number says nothing about a food; future meals aren't eaten yet.
                                onRate = if (meal == "Quick Add" || selectedDate.isAfter(LocalDate.now())) null else { { ratingMeal = meal } }
                            )
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
        val requestKey = rememberRequestKey()
        PortionEntryDialog(
            food = showPortionDialogFor!!,
            onDismiss = { showPortionDialogFor = null },
            onConfirm = { food, amount, mealType, confidence, portion ->
                viewModel.logCustomFood(food, amount, mealType, selectedDate.toString(), confidence, portion, requestKey)
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
        val remainingKcal = (todayGoal - entries.sumOf { it.kcal }).toDouble()
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
        val requestKey = rememberRequestKey()
        QuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onSave = { kcal -> viewModel.addQuickCalories(kcal, requestKey) }
        )
    }

    ratingMeal?.let { meal ->
        val mealEntries = grouped[meal].orEmpty()
        RateMealDialog(
            meal = meal,
            satiety = mealEntries.firstNotNullOfOrNull { it.satiety },
            energy = mealEntries.firstNotNullOfOrNull { it.energyLevel },
            onDismiss = { ratingMeal = null },
            onSave = { satiety, energy ->
                viewModel.rateMeal(selectedDate, meal, satiety, energy)
                ratingMeal = null
            }
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
fun MealSectionHeader(
    meal: String,
    totalKcal: Int,
    satiety: Int? = null,
    energy: Int? = null,
    // Null hides the rating chip.
    onRate: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val theme = LocalAppTheme.current
    val label = mealLabel(meal)
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val labelModifier = Modifier.weight(1f, fill = false)
        // Paper: spaced small capitals. Bold: an ink-bordered tag. Neon: wide-spaced accent capitals.
        when (theme) {
            AppTheme.PAPER -> Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = labelModifier
            )
            AppTheme.BOLD -> Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = labelModifier
                    .background(colors.primary, MaterialTheme.shapes.extraSmall)
                    .border(2.dp, colors.outline, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            AppTheme.NEON -> Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = labelModifier
            )
            else -> Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = labelModifier
            )
        }
        // The chip takes whatever is left between the name and the total, and ellipsizes
        // there, so the total never gets pushed off a narrow screen.
        Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (onRate != null) MealRatingChip(satiety, energy, onRate)
        }
        Text(
            "$totalKcal ${stringResource(R.string.unit_kcal)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
    if (theme == AppTheme.PAPER) {
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(thickness = 1.dp, color = colors.onSurface)
    }
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
            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = if (tintedCards) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.water), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${totalMl.toInt()} / ${goalMl.toInt()} ${stringResource(R.string.unit_ml)}", style = MaterialTheme.typography.bodyMedium)
            }
            // Tighter than the default 24 dp padding, so the label keeps room on narrow phones.
            val buttonPadding = PaddingValues(horizontal = 12.dp)
            OutlinedButton(onClick = { onAddWater(250.0) }, contentPadding = buttonPadding) { Text("+250", maxLines = 1) }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { onAddWater(500.0) }, contentPadding = buttonPadding) { Text("+500", maxLines = 1) }
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
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
                        waterEntries.sortedByDescending { it.time }.forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormat),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.widthIn(min = 72.dp).padding(end = 8.dp)
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
fun DailyProgressCard(entries: List<FoodEntry>, goals: UserGoal, weekly: WeeklyBudget? = null, activityBonus: Int = 0) {
    val colors = MaterialTheme.colorScheme
    val eaten = entries.sumOf { it.kcal }
    // With weekly budgeting, today's target flexes with the rest of the week.
    // Eat-back adds part of today's active calories on top.
    val goal = ((weekly?.todayTarget ?: goals.calorieGoal) + activityBonus).coerceAtLeast(1)
    val remaining = goal - eaten
    val isOver = remaining < 0
    val statusColor = if (isOver) colors.error else colors.primary
    // Glass and Bold tint the card with primaryContainer; the others keep it neutral.
    val onCard = if (tintedCards) colors.onPrimaryContainer else colors.onSurface

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tint = colors.primaryContainer
    ) {
        Column(Modifier.padding(20.dp)) {
            BoxWithConstraints {
                // The ring gives up width on narrow phones so the stats beside it still fit.
                val ringSize = (maxWidth * 0.42f).coerceIn(100.dp, 132.dp)
                val ringGap = if (maxWidth < 280.dp) 14.dp else 20.dp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalorieRing(
                        progress = eaten.toFloat() / goal,
                        color = colors.primary,
                        overColor = colors.error,
                        trackColor = onCard.copy(alpha = 0.12f),
                        modifier = Modifier.size(ringSize)
                    ) {
                        // Both stay inside the stroke: the number shrinks if it's wide (big
                        // numbers, large system font), longer translations wrap.
                        FitText(
                            "%,d".format(kotlin.math.abs(remaining)),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOver) colors.error else onCard,
                            modifier = Modifier.widthIn(max = ringSize - 36.dp)
                        )
                        Text(
                            stringResource(if (isOver) R.string.kcal_over else R.string.kcal_left),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOver) colors.error else onCard.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = ringSize * 0.6f)
                        )
                    }

                    Spacer(Modifier.width(ringGap))

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalorieStat(stringResource(R.string.eaten), eaten, onCard, uncertainty = entries.kcalUncertainty().roundToInt())
                        Column {
                            CalorieStat(stringResource(if (weekly != null) R.string.goal_today_flex else R.string.goal), goal, onCard)
                            if (activityBonus > 0) {
                                Text(
                                    stringResource(R.string.goal_activity_bonus, "%,d".format(activityBonus)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onCard.copy(alpha = 0.7f)
                                )
                            }
                        }
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
        ValueWithUnit(
            value = "%,d".format(kcal),
            // How far off the total could be, given how each entry was measured.
            unit = stringResource(R.string.unit_kcal) + if (uncertainty >= 5) "  ± %,d".format(uncertainty) else "",
            valueStyle = MaterialTheme.typography.titleLarge,
            valueColor = color,
            unitColor = color.copy(alpha = 0.7f)
        )
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
    val theme = LocalAppTheme.current
    val ink = MaterialTheme.colorScheme.outline
    // Paper: a thin ink line on a hairline. Bold: a thick square-ended band between two ink
    // circles. Neon: a slim round line that glows. Classic and Glass: the soft round band.
    val strokeWidth = when (theme) {
        AppTheme.PAPER -> 5.dp
        AppTheme.BOLD -> 16.dp
        AppTheme.NEON -> 7.dp
        else -> 12.dp
    }
    val cap = if (theme == AppTheme.PAPER || theme == AppTheme.BOLD) StrokeCap.Butt else StrokeCap.Round
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val style = Stroke(width = stroke, cap = cap)
            // Bold's ink circles sit on the band's edges, so the band starts inside them.
            val edge = if (theme == AppTheme.BOLD) 1.dp.toPx() else 0f
            val inset = stroke / 2 + edge
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            if (theme == AppTheme.PAPER) {
                drawArc(trackColor.copy(alpha = (trackColor.alpha * 3f).coerceAtMost(1f)), 0f, 360f, false, topLeft, arcSize, style = Stroke(width = 1.dp.toPx()))
            } else {
                drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = stroke))
            }
            fun arc(arcColor: Color, sweep: Float) {
                if (theme == AppTheme.NEON) drawIntoCanvas { canvas ->
                    val glow = Paint().apply {
                        this.color = arcColor
                        this.style = PaintingStyle.Stroke
                        this.strokeWidth = stroke * 2f
                        this.strokeCap = StrokeCap.Round
                        asFrameworkPaint().maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.drawArc(topLeft.x, topLeft.y, topLeft.x + arcSize.width, topLeft.y + arcSize.height, -90f, sweep, false, glow)
                }
                drawArc(arcColor, -90f, sweep, false, topLeft, arcSize, style = style)
            }
            if (animated > 0f) arc(color, 360f * animated.coerceAtMost(1f))
            if (animated > 1f) arc(overColor, 360f * (animated - 1f).coerceAtMost(1f))
            if (theme == AppTheme.BOLD) {
                val ring = Stroke(width = 2.dp.toPx())
                drawCircle(ink, radius = size.minDimension / 2 - 1.dp.toPx(), style = ring)
                drawCircle(ink, radius = size.minDimension / 2 - stroke - 1.dp.toPx(), style = ring)
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
    val onCard = if (tintedCards) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val overLimit = isLimit && value > goal
    val fmt: (Double) -> String = { if (decimals) "%.1f".format(it).removeSuffix(".0").removeSuffix(",0") else "${it.toInt()}" }

    val unitG = stringResource(R.string.unit_g)

    // A third of the card: long nutrient names and big numbers shrink rather than wrap.
    Column(modifier) {
        FitText(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = onCard)
        Spacer(Modifier.height(2.dp))
        FitText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (overLimit) MaterialTheme.colorScheme.error else onCard)) {
                    append(fmt(value))
                }
                withStyle(SpanStyle(fontSize = 0.8.em, color = onCard.copy(alpha = 0.65f))) {
                    append(" / ${fmt(goal)} $unitG")
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        ProgressBar(
            fraction = if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f,
            color = if (overLimit) MaterialTheme.colorScheme.error else color,
            onCard = onCard
        )
    }
}

/**
 * A macro bar in the current theme: Paper a 2dp ink-thin rule, Bold a thick square bar in
 * an ink box, Neon a slim glowing line, Classic and Glass the soft rounded bar.
 */
@Composable
private fun ProgressBar(fraction: Float, color: Color, onCard: Color) {
    val theme = LocalAppTheme.current
    val height = when (theme) {
        AppTheme.PAPER -> 2.dp
        AppTheme.BOLD -> 10.dp
        AppTheme.NEON -> 4.dp
        else -> 5.dp
    }
    val shape = when (theme) {
        AppTheme.PAPER -> RectangleShape
        AppTheme.BOLD -> MaterialTheme.shapes.extraSmall
        else -> CircleShape
    }
    val track = when (theme) {
        AppTheme.PAPER -> onCard.copy(alpha = 0.15f)
        AppTheme.BOLD -> Color.Transparent
        AppTheme.NEON -> color.copy(alpha = 0.15f)
        else -> color.copy(alpha = 0.22f)
    }
    val inkBorder = if (theme == AppTheme.BOLD) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, shape) else Modifier
    Box(Modifier.fillMaxWidth().height(height).clip(shape).background(track).then(inkBorder)) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                // Neon's glow spills past the bar, so it's drawn before the clip.
                .then(if (theme == AppTheme.NEON && fraction > 0f) Modifier.glow(shape, color, 7.dp) else Modifier)
                .clip(shape)
                .background(color)
                .then(inkBorder)
        )
    }
}

@Composable
fun FoodItemRow(entry: FoodEntry, shape: Shape, onClick: () -> Unit) {
    val amount = entryAmountLabel(entry)
    AppCard(
        onClick = onClick,
        shape = shape,
        grouped = true,
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
@Composable
fun groupedShape(index: Int, count: Int): Shape {
    val (outer, inner) = when (LocalAppTheme.current) {
        AppTheme.PAPER -> 4.dp to 0.dp
        AppTheme.BOLD -> 10.dp to 4.dp
        AppTheme.NEON -> 22.dp to 6.dp
        else -> 18.dp to 4.dp
    }
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
    val weekStart = weekStartOf(selectedDate)

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
    val theme = LocalAppTheme.current
    val shape = MaterialTheme.shapes.medium
    // The selected day: Paper inks it, Bold blocks it with a border and shadow, Neon outlines
    // it in glowing accent; Classic and Glass fill it with the accent.
    val selectedFill = when (theme) {
        AppTheme.PAPER -> colors.onSurface
        // Opaque, so the glow behind it doesn't wash out the date.
        AppTheme.NEON -> androidx.compose.ui.graphics.lerp(colors.background, colors.primary, 0.18f)
        else -> colors.primary
    }
    val contentColor = when {
        isSelected -> when (theme) {
            AppTheme.PAPER -> colors.surface
            AppTheme.NEON -> colors.primary
            else -> colors.onPrimary
        }
        isToday -> colors.primary
        isFuture -> colors.onSurface.copy(alpha = 0.45f)
        else -> colors.onSurface
    }
    val selectedEdge = when {
        !isSelected -> Modifier
        theme == AppTheme.BOLD -> Modifier.hardShadow(shape, boldShadowColor, 3.dp)
        theme == AppTheme.NEON -> Modifier.glow(shape, neonGlowColor, 10.dp)
        else -> Modifier
    }
    val selectedBorder = when {
        !isSelected -> Modifier
        theme == AppTheme.BOLD -> Modifier.border(2.dp, colors.outline, shape)
        theme == AppTheme.NEON -> Modifier.border(1.5.dp, colors.primary, shape)
        else -> Modifier
    }

    Column(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .then(selectedEdge)
            .clip(shape)
            .background(if (isSelected) selectedFill else Color.Transparent)
            .then(selectedBorder)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A seventh of the screen: shrink rather than wrap on small phones with a large font.
        FitText(
            date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
                .replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = contentColor.alpha * 0.8f)
        )
        Spacer(Modifier.height(2.dp))
        FitText(
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
