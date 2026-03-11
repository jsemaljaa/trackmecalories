package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel

/**
 * Settings screen for user configuration.
 */
@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val currentGoals by viewModel.userGoal.collectAsState()
    
    var calorieGoal by remember(currentGoals) { mutableStateOf(currentGoals.calorieGoal.toString()) }
    var proteinGoal by remember(currentGoals) { mutableStateOf(currentGoals.proteinGoal.toString()) }
    var carbGoal by remember(currentGoals) { mutableStateOf(currentGoals.carbGoal.toString()) }
    var fatGoal by remember(currentGoals) { mutableStateOf(currentGoals.fatGoal.toString()) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(24.dp))
        
        Text("Nutritional Goals", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
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
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = {
                viewModel.updateGoals(
                    currentGoals.copy(
                        calorieGoal = calorieGoal.toIntOrNull() ?: currentGoals.calorieGoal,
                        proteinGoal = proteinGoal.toDoubleOrNull() ?: currentGoals.proteinGoal,
                        carbGoal = carbGoal.toDoubleOrNull() ?: currentGoals.carbGoal,
                        fatGoal = fatGoal.toDoubleOrNull() ?: currentGoals.fatGoal
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Goals")
        }
        
        Spacer(Modifier.weight(1f))
        
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        
        Text("Data Management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        
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
