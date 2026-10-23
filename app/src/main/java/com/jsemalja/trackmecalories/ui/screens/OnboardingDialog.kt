package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.UserGoal

/**
 * First-run goal setup. Shown whenever no [UserGoal] has ever been saved, so new users
 * understand where the defaults come from instead of landing on an unexplained 2000kcal plan.
 */
@Composable
fun OnboardingDialog(onComplete: (UserGoal) -> Unit) {
    var calorieGoal by remember { mutableStateOf("2000") }
    var proteinGoal by remember { mutableStateOf("150") }
    var carbGoal by remember { mutableStateOf("200") }
    var fatGoal by remember { mutableStateOf("70") }

    AlertDialog(
        onDismissRequest = { /* Must set up goals before dismissing. */ },
        title = { Text("Welcome!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Let's set your daily nutrition goals. These defaults are a reasonable " +
                        "starting point — you can fine-tune them anytime in Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = calorieGoal,
                    onValueChange = { calorieGoal = it },
                    label = { Text("Daily Calorie Goal (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
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
            }
        },
        confirmButton = {
            Button(onClick = {
                onComplete(
                    UserGoal(
                        calorieGoal = calorieGoal.toIntOrNull() ?: 2000,
                        proteinGoal = proteinGoal.toDoubleOrNull() ?: 150.0,
                        carbGoal = carbGoal.toDoubleOrNull() ?: 200.0,
                        fatGoal = fatGoal.toDoubleOrNull() ?: 70.0
                    )
                )
            }) { Text("Get Started") }
        }
    )
}
