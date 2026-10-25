package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.Diet
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.withDietMacros

/**
 * First-run goal setup. Shown whenever no [UserGoal] has ever been saved, so new users
 * understand where the defaults come from instead of landing on an unexplained 2000kcal plan.
 * Uses the same editor as Settings, starting from a Balanced diet.
 */
@Composable
fun OnboardingDialog(onComplete: (UserGoal) -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Must set up goals before dismissing. */ },
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                GoalsEditor(
                    goals = UserGoal(dietType = Diet.BALANCED.name).withDietMacros(),
                    latestWeightKg = null,
                    onSave = onComplete
                )
            }
        },
        // The editor has its own save button.
        confirmButton = {}
    )
}
