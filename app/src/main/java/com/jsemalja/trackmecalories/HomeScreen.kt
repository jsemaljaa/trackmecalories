package com.jsemalja.trackmecalories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.foodEntries.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showManualDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

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

            SummaryCard(entries)

            Spacer(Modifier.height(24.dp))

            Text("Eaten today", style = MaterialTheme.typography.titleLarge)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(entries) { entry ->
                    FoodItemRow(entry)
                }
            }
        }
    }

    if (showManualDialog) {
        ManualEntryDialog(
            onDismiss = { showManualDialog = false },
            onSave = { newEntry ->
                viewModel.addEntry(newEntry.copy(date = selectedDate.toString()))
            }
        )
    }

    if (showQuickAddDialog) {
        QuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onSave = { kcal ->
                viewModel.addQuickCalories(kcal)
            }
        )
    }
}

@Composable
fun SummaryCard(entries: List<FoodEntry>) {
    val totalKcal = entries.sumOf { it.kcal }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Total Calories", style = MaterialTheme.typography.labelMedium)
            Text("$totalKcal kcal", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroStat("P", entries.sumOf { it.protein }, "g")
                MacroStat("C", entries.sumOf { it.carbs }, "g")
                MacroStat("F", entries.sumOf { it.fat }, "g")
                MacroStat("Fib", entries.sumOf { it.fiber }, "g")
                MacroStat("Salt", entries.sumOf { it.salt }, "g")
            }
        }
    }
}

@Composable
fun FoodItemRow(entry: FoodEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Bold)
                Text("${entry.kcal} kcal · ${entry.amount}${entry.unit}", style = MaterialTheme.typography.bodySmall)
            }
            Text("P: ${String.format("%.1f", entry.protein)}g", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MacroStat(label: String, value: Double, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        val formattedValue = String.format(Locale.getDefault(), "%.1f", value)
        Text(text = "$formattedValue$unit", fontWeight = FontWeight.Bold)
    }
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
                label = { Text("Calories") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
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

@Composable
fun ManualEntryDialog(onDismiss: () -> Unit, onSave: (FoodEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Entry") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(0.6f)) {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(unit)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("g", "ml", "pcs").forEach { u ->
                                    DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; expanded = false })
                                }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Prot (g)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carb") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Fiber") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Salt") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(FoodEntry(
                    name = name,
                    kcal = kcal.toIntOrNull() ?: 0,
                    protein = protein.toDoubleOrNull() ?: 0.0,
                    carbs = carbs.toDoubleOrNull() ?: 0.0,
                    fat = fat.toDoubleOrNull() ?: 0.0,
                    fiber = fiber.toDoubleOrNull() ?: 0.0,
                    salt = salt.toDoubleOrNull() ?: 0.0,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    unit = unit,
                    date = ""
                ))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DateSelector(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        (-2..2).forEach { dayOffset ->
            val date = LocalDate.now().plusDays(dayOffset.toLong())
            val isSelected = date == selectedDate

            FilterChip(
                selected = isSelected,
                onClick = { onDateSelected(date) },
                label = {
                    Text(if (dayOffset == 0) "Today" else "${date.dayOfMonth}.${date.monthValue}.")
                }
            )
        }
    }
}