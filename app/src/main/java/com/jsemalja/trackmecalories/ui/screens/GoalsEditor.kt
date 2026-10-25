package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.ActivityLevel
import com.jsemalja.trackmecalories.data.CalorieCalculator
import com.jsemalja.trackmecalories.data.Diet
import com.jsemalja.trackmecalories.data.Sex
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.WeightPace
import com.jsemalja.trackmecalories.ui.theme.MacroCarbs
import com.jsemalja.trackmecalories.ui.theme.MacroFat
import com.jsemalja.trackmecalories.ui.theme.MacroFiber
import com.jsemalja.trackmecalories.ui.theme.MacroProtein
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import kotlin.math.roundToInt

/**
 * Goals: pick a diet and enter calories — protein, carbs, fat and fiber follow
 * automatically (or set them by hand with "Custom"). "Calculate" estimates the calorie goal
 * from body data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsEditor(goals: UserGoal, latestWeightKg: Double?, measuredMaintenance: Int? = null, showWater: Boolean = true, onSave: (UserGoal) -> Unit) {
    var kcal by remember(goals) { mutableStateOf(goals.calorieGoal.toString()) }
    var diet by remember(goals) { mutableStateOf(Diet.from(goals.dietType)) }
    var protein by remember(goals) { mutableStateOf(goals.proteinGoal.clean()) }
    var carbs by remember(goals) { mutableStateOf(goals.carbGoal.clean()) }
    var fat by remember(goals) { mutableStateOf(goals.fatGoal.clean()) }
    var fiber by remember(goals) { mutableStateOf(goals.fiberGoal.clean()) }
    var salt by remember(goals) { mutableStateOf(goals.saltGoal.clean()) }
    var water by remember(goals) { mutableStateOf(goals.waterGoalMl.clean()) }
    var profile by remember(goals) { mutableStateOf(goals) }
    var showCalculator by remember { mutableStateOf(false) }
    var saved by remember(goals) { mutableStateOf(false) }

    val kcalValue = kcal.toIntOrNull() ?: goals.calorieGoal
    val computed = diet.macrosFor(kcalValue)

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = kcal,
            onValueChange = { kcal = it.filter(Char::isDigit); saved = false },
            label = { Text(stringResource(R.string.goal_calories)) },
            suffix = { Text(stringResource(R.string.unit_kcal)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(onClick = { showCalculator = true }, contentPadding = PaddingValues(horizontal = 14.dp)) {
            Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.goal_calculate))
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.goal_diet), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Diet.entries.forEach { d ->
            FilterChip(
                selected = diet == d,
                onClick = {
                    diet = d
                    saved = false
                    // Switching to Custom starts from the numbers the previous diet produced.
                    if (d == Diet.CUSTOM && computed != null) {
                        protein = computed.protein.clean(); carbs = computed.carbs.clean()
                        fat = computed.fat.clean(); fiber = computed.fiber.clean()
                    }
                },
                label = { Text(stringResource(d.label)) }
            )
        }
    }
    Text(
        stringResource(diet.description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(14.dp))
    if (computed != null) {
        // Calculated from calories; shown read-only so it's obvious they follow the diet.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComputedMacro(stringResource(R.string.protein), computed.protein, MacroProtein, Modifier.weight(1f))
            ComputedMacro(stringResource(R.string.carbs), computed.carbs, MacroCarbs, Modifier.weight(1f))
            ComputedMacro(stringResource(R.string.fat), computed.fat, MacroFat, Modifier.weight(1f))
            ComputedMacro(stringResource(R.string.fiber), computed.fiber, MacroFiber, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        MacroSplitBar(diet.proteinPct, diet.carbsPct, diet.fatPct)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalField(stringResource(R.string.protein_g), protein, Modifier.weight(1f)) { protein = it; saved = false }
            GoalField(stringResource(R.string.carbs_g), carbs, Modifier.weight(1f)) { carbs = it; saved = false }
            GoalField(stringResource(R.string.fat_g), fat, Modifier.weight(1f)) { fat = it; saved = false }
        }
        Spacer(Modifier.height(8.dp))
        GoalField(stringResource(R.string.fiber_g), fiber, Modifier.fillMaxWidth()) { fiber = it; saved = false }
        val p = protein.toDecimal() ?: 0.0
        val c = carbs.toDecimal() ?: 0.0
        val f = fat.toDecimal() ?: 0.0
        val macroKcal = p * 4 + c * 4 + f * 9
        if (macroKcal > 0) {
            Spacer(Modifier.height(8.dp))
            MacroSplitBar((p * 4 / macroKcal * 100).roundToInt(), (c * 4 / macroKcal * 100).roundToInt(), (f * 9 / macroKcal * 100).roundToInt())
            Text(
                stringResource(R.string.goal_macros_add_up, macroKcal.roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = if (kotlin.math.abs(macroKcal - kcalValue) > kcalValue * 0.1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalField(stringResource(R.string.salt_limit_g), salt, Modifier.weight(1f)) { salt = it; saved = false }
        if (showWater) {
            GoalField(stringResource(R.string.water_goal_ml), water, Modifier.weight(1f)) { water = it; saved = false }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val base = profile.copy(
                calorieGoal = kcalValue,
                dietType = diet.name,
                saltGoal = salt.toDecimal() ?: goals.saltGoal,
                waterGoalMl = water.toDecimal() ?: goals.waterGoalMl
            )
            onSave(
                if (computed != null) {
                    base.copy(proteinGoal = computed.protein, carbGoal = computed.carbs, fatGoal = computed.fat, fiberGoal = computed.fiber)
                } else {
                    base.copy(
                        proteinGoal = protein.toDecimal() ?: goals.proteinGoal,
                        carbGoal = carbs.toDecimal() ?: goals.carbGoal,
                        fatGoal = fat.toDecimal() ?: goals.fatGoal,
                        fiberGoal = fiber.toDecimal() ?: goals.fiberGoal
                    )
                }
            )
            saved = true
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(if (saved) R.string.goal_saved else R.string.goal_save))
    }

    if (showCalculator) {
        CalorieCalculatorDialog(
            goals = profile,
            latestWeightKg = latestWeightKg,
            measuredMaintenance = measuredMaintenance,
            onDismiss = { showCalculator = false },
            onUse = { target, updatedProfile ->
                kcal = target.toString()
                profile = updatedProfile
                saved = false
                showCalculator = false
            }
        )
    }
}

@Composable
private fun GoalField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun ComputedMacro(label: String, grams: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${grams.roundToInt()} ${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun MacroSplitBar(proteinPct: Int, carbsPct: Int, fatPct: Int) {
    val total = (proteinPct + carbsPct + fatPct).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
        if (proteinPct > 0) Box(Modifier.weight(proteinPct.toFloat() / total).fillMaxHeight().background(MacroProtein))
        if (carbsPct > 0) Box(Modifier.weight(carbsPct.toFloat() / total).fillMaxHeight().background(MacroCarbs))
        if (fatPct > 0) Box(Modifier.weight(fatPct.toFloat() / total).fillMaxHeight().background(MacroFat))
    }
}

/**
 * Estimates a daily calorie goal (Mifflin–St Jeor) from sex, age, height, weight, activity
 * and the pace you want. The profile is remembered with the goals for next time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieCalculatorDialog(
    goals: UserGoal,
    latestWeightKg: Double?,
    measuredMaintenance: Int? = null,
    onDismiss: () -> Unit,
    onUse: (Int, UserGoal) -> Unit
) {
    var sex by remember { mutableStateOf(goals.sex?.let { s -> Sex.entries.firstOrNull { it.name == s } } ?: Sex.FEMALE) }
    var birthYear by remember { mutableStateOf(goals.birthYear?.toString() ?: "") }
    var height by remember { mutableStateOf(goals.heightCm?.clean() ?: "") }
    var weight by remember { mutableStateOf(latestWeightKg?.clean() ?: "") }
    var activity by remember {
        mutableStateOf(goals.activityLevel?.let { a -> ActivityLevel.entries.firstOrNull { it.name == a } } ?: ActivityLevel.LIGHT)
    }
    var pace by remember { mutableStateOf(WeightPace.LOSE) }

    val year = birthYear.toIntOrNull()
    val h = height.toDecimal()
    val w = weight.toDecimal()
    val ready = year != null && year in 1900..2020 && h != null && h in 100.0..250.0 && w != null && w in 30.0..300.0
    val maintenance = if (ready) CalorieCalculator.maintenance(sex, w!!, h!!, CalorieCalculator.age(year!!), activity) else null
    val target = maintenance?.let { CalorieCalculator.target(it, pace) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calc_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Sex.entries.forEachIndexed { i, s ->
                            SegmentedButton(
                                selected = sex == s,
                                onClick = { sex = s },
                                shape = SegmentedButtonDefaults.itemShape(i, Sex.entries.size),
                                colors = appSegmentedColors()
                            ) { Text(stringResource(s.label)) }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalField(stringResource(R.string.calc_birth_year), birthYear, Modifier.weight(1f)) { birthYear = it.filter(Char::isDigit).take(4) }
                        GoalField(stringResource(R.string.calc_height_cm), height, Modifier.weight(1f)) { height = it }
                        GoalField(stringResource(R.string.calc_weight_kg), weight, Modifier.weight(1f)) { weight = it }
                    }
                }
                item { Text(stringResource(R.string.calc_activity), style = MaterialTheme.typography.labelLarge) }
                ActivityLevel.entries.forEach { a ->
                    item(key = "a_${a.name}") {
                        ChoiceRow(selected = activity == a, title = stringResource(a.label), subtitle = stringResource(a.description)) { activity = a }
                    }
                }
                item { Text(stringResource(R.string.calc_goal), style = MaterialTheme.typography.labelLarge) }
                WeightPace.entries.forEach { p ->
                    item(key = "p_${p.name}") {
                        ChoiceRow(selected = pace == p, title = stringResource(p.label), subtitle = null) { pace = p }
                    }
                }
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (target != null) {
                            Text("%,d %s".format(target, stringResource(R.string.unit_kcal)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.calc_maintenance, maintenance!!), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (target == CalorieCalculator.MIN_KCAL) {
                                Text(stringResource(R.string.calc_min_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text(stringResource(R.string.calc_fill_in), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Measured from real intake + weight trend: more accurate than any formula.
                if (measuredMaintenance != null) {
                    item {
                        OutlinedButton(
                            onClick = {
                                onUse(
                                    CalorieCalculator.target(measuredMaintenance, pace),
                                    goals.copy(sex = sex.name, birthYear = year, heightCm = h, activityLevel = activity.name)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.calc_use_measured, measuredMaintenance)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = target != null,
                onClick = {
                    onUse(
                        target!!,
                        goals.copy(sex = sex.name, birthYear = year, heightCm = h, activityLevel = activity.name)
                    )
                }
            ) { Text(stringResource(R.string.calc_use)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun ChoiceRow(selected: Boolean, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
