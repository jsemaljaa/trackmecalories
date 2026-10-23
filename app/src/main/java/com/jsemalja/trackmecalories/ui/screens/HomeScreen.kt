package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Section order for the daily log; anything else (custom meal types) is appended alphabetically.
private val MEAL_ORDER = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Quick Add")

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.foodEntries.collectAsState()
    val goals by viewModel.userGoal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val waterEntries by viewModel.waterEntries.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }

    var showPortionDialogFor by remember { mutableStateOf<CustomFood?>(null) }

    val grouped = remember(entries) { entries.groupBy { it.mealType } }
    val orderedMeals = remember(grouped) {
        MEAL_ORDER.filter { grouped.containsKey(it) } +
            grouped.keys.filterNot { it in MEAL_ORDER }.sorted()
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showQuickAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Quick Add")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { showManualDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Manual Add")
                }
            }
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            DateSelector(selectedDate) { viewModel.changeDate(it) }
            Spacer(Modifier.height(24.dp))
            DailyProgressCard(entries, goals)
            Spacer(Modifier.height(12.dp))
            WaterCard(
                waterEntries = waterEntries,
                goalMl = goals.waterGoalMl,
                onAddWater = { viewModel.addWater(it) }
            )
            Spacer(Modifier.height(24.dp))
            Text("Daily Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No entries for this day", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    orderedMeals.forEach { meal ->
                        val mealEntries = grouped[meal].orEmpty()
                        item(key = "header_$meal") {
                            MealSectionHeader(meal, mealEntries.sumOf { it.kcal })
                        }
                        items(mealEntries, key = { it.id }) { entry ->
                            SwipeToDeleteContainer(
                                onDelete = { viewModel.deleteEntry(entry) }
                            ) {
                                FoodItemRow(
                                    entry = entry,
                                    onClick = { editingEntry = entry }
                                )
                            }
                        }
                        item(key = "spacer_$meal") { Spacer(Modifier.height(8.dp)) }
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
            onConfirm = { amount, mealType ->
                viewModel.logCustomFood(showPortionDialogFor!!, amount, mealType, selectedDate.toString())
                showPortionDialogFor = null
            }
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
            onCopyToDate = { date -> viewModel.copyEntryToDate(entry, date) }
        )
    }
}

@Composable
fun MealSectionHeader(meal: String, totalKcal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(meal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("$totalKcal kcal", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
    }
}

@Composable
fun WaterCard(waterEntries: List<WaterEntry>, goalMl: Double, onAddWater: (Double) -> Unit) {
    val totalMl = waterEntries.sumOf { it.amountMl }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Water", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${totalMl.toInt()} / ${goalMl.toInt()} ml", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = { onAddWater(250.0) }) { Text("+250") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { onAddWater(500.0) }) { Text("+500") }
        }
    }
}

@Composable
fun DailyProgressCard(entries: List<FoodEntry>, goals: UserGoal) {
    val totalKcal = entries.sumOf { it.kcal }
    val remainingKcal = goals.calorieGoal - totalKcal

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Calories Remaining", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text("$remainingKcal", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                }
                Text("of ${goals.calorieGoal} kcal", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            }

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { (totalKcal.toFloat() / goals.calorieGoal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroPill("Protein", entries.sumOf { it.protein }, goals.proteinGoal, "g", MacroProtein)
                MacroPill("Carbs", entries.sumOf { it.carbs }, goals.carbGoal, "g", MacroCarbs)
                MacroPill("Fat", entries.sumOf { it.fat }, goals.fatGoal, "g", MacroFat)
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MacroPill("Fiber", entries.sumOf { it.fiber }, goals.fiberGoal, "g", MacroFiber)
                MacroPill("Salt", entries.sumOf { it.salt }, goals.saltGoal, "g", MacroSalt)
            }
        }
    }
}

@Composable
fun MacroPill(label: String, value: Double, goal: Double, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth((value / goal).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color)
            )
        }
        Text("${value.toInt()}$unit", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FoodItemRow(entry: FoodEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${entry.amount.toInt()}${entry.unit}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.kcal} kcal", fontWeight = FontWeight.Bold)
                Text("P: ${entry.protein.toInt()}g · C: ${entry.carbs.toInt()}g · F: ${entry.fat.toInt()}g", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (state.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                    else -> Color.Transparent
                }, label = ""
            )
            Box(
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(color).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
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
        title = { Text("Quick Add Calories") },
        text = {
            OutlinedTextField(
                value = kcal,
                onValueChange = { kcal = it },
                label = { Text("Total Calories") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 500") }
            )
        },
        confirmButton = {
            Button(onClick = {
                kcal.toIntOrNull()?.let { onSave(it) }
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (-3..3).forEach { dayOffset ->
            val date = LocalDate.now().plusDays(dayOffset.toLong())
            val isSelected = date == selectedDate

            FilterChip(
                selected = isSelected,
                onClick = { onDateSelected(date) },
                label = {
                    Text(
                        text = when(dayOffset) {
                            0 -> "Today"
                            -1 -> "Yest."
                            1 -> "Tomw."
                            else -> "${date.dayOfMonth}.${date.monthValue}."
                        },
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
        IconButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick a date")
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}
