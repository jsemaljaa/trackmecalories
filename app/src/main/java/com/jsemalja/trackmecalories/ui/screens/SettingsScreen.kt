package com.jsemalja.trackmecalories.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/**
 * Settings screen for user configuration.
 */
@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val currentGoals by viewModel.userGoal.collectAsState()
    
    var calorieGoal by remember(currentGoals) { mutableStateOf(currentGoals.calorieGoal.toString()) }
    var proteinGoal by remember(currentGoals) { mutableStateOf(currentGoals.proteinGoal.clean()) }
    var carbGoal by remember(currentGoals) { mutableStateOf(currentGoals.carbGoal.clean()) }
    var fatGoal by remember(currentGoals) { mutableStateOf(currentGoals.fatGoal.clean()) }
    var fiberGoal by remember(currentGoals) { mutableStateOf(currentGoals.fiberGoal.clean()) }
    var saltGoal by remember(currentGoals) { mutableStateOf(currentGoals.saltGoal.clean()) }
    var waterGoal by remember(currentGoals) { mutableStateOf(currentGoals.waterGoalMl.clean()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader(Icons.Default.Settings, "Settings")

        Spacer(Modifier.height(24.dp))

        SettingsSectionCard(title = "Nutritional Goals", titleColor = MaterialTheme.colorScheme.primary) {
            OutlinedTextField(
                value = calorieGoal,
                onValueChange = { calorieGoal = it },
                label = { Text("Daily Calorie Goal (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proteinGoal,
                    onValueChange = { proteinGoal = it },
                    label = { Text("Protein (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = carbGoal,
                    onValueChange = { carbGoal = it },
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = fatGoal,
                    onValueChange = { fatGoal = it },
                    label = { Text("Fat (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fiberGoal,
                    onValueChange = { fiberGoal = it },
                    label = { Text("Fiber (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = saltGoal,
                    onValueChange = { saltGoal = it },
                    label = { Text("Salt (g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = waterGoal,
                onValueChange = { waterGoal = it },
                label = { Text("Water Goal (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.updateGoals(
                        currentGoals.copy(
                            calorieGoal = calorieGoal.toIntOrNull() ?: currentGoals.calorieGoal,
                            proteinGoal = proteinGoal.toDecimal() ?: currentGoals.proteinGoal,
                            carbGoal = carbGoal.toDecimal() ?: currentGoals.carbGoal,
                            fatGoal = fatGoal.toDecimal() ?: currentGoals.fatGoal,
                            fiberGoal = fiberGoal.toDecimal() ?: currentGoals.fiberGoal,
                            saltGoal = saltGoal.toDecimal() ?: currentGoals.saltGoal,
                            waterGoalMl = waterGoal.toDecimal() ?: currentGoals.waterGoalMl
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Goals")
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = "Data", titleColor = MaterialTheme.colorScheme.primary) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        exportError = try {
                            val csv = viewModel.exportCsv()
                            val file = File(context.cacheDir, "trackmecalories_export_${LocalDate.now()}.csv")
                            file.writeText(csv)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Food Log"))
                            null
                        } catch (e: Exception) {
                            "Export failed: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Data (CSV)")
            }

            exportError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = "Danger Zone", titleColor = MaterialTheme.colorScheme.error) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wipe All Data")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Wipe All Data?") },
            text = { Text("This will permanently delete all your food logs. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
