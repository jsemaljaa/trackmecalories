package com.jsemalja.trackmecalories.data

import androidx.annotation.StringRes
import com.jsemalja.trackmecalories.R
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Diet presets as shares of daily calories. Choosing one derives the protein/carb/fat
 * gram goals from the calorie goal (4/4/9 kcal per gram); [CUSTOM] keeps hand-entered grams.
 */
enum class Diet(
    @StringRes val label: Int,
    @StringRes val description: Int,
    val proteinPct: Int,
    val carbsPct: Int,
    val fatPct: Int
) {
    BALANCED(R.string.diet_balanced, R.string.diet_balanced_desc, 25, 50, 25),
    HIGH_PROTEIN(R.string.diet_high_protein, R.string.diet_high_protein_desc, 35, 40, 25),
    LOW_CARB(R.string.diet_low_carb, R.string.diet_low_carb_desc, 30, 20, 50),
    KETO(R.string.diet_keto, R.string.diet_keto_desc, 20, 5, 75),
    MEDITERRANEAN(R.string.diet_mediterranean, R.string.diet_mediterranean_desc, 20, 45, 35),
    ZONE(R.string.diet_zone, R.string.diet_zone_desc, 30, 40, 30),
    CUSTOM(R.string.diet_custom, R.string.diet_custom_desc, 0, 0, 0);

    /** Gram goals for [kcal] under this diet, or null for [CUSTOM]. */
    fun macrosFor(kcal: Int): Macros? {
        if (this == CUSTOM) return null
        return Macros(
            protein = (kcal * proteinPct / 100.0 / 4).roundToInt().toDouble(),
            carbs = (kcal * carbsPct / 100.0 / 4).roundToInt().toDouble(),
            fat = (kcal * fatPct / 100.0 / 9).roundToInt().toDouble(),
            // 14 g per 1000 kcal (Dietary Guidelines for Americans), at least 20 g.
            fiber = (kcal * 14 / 1000.0).roundToInt().coerceAtLeast(20).toDouble()
        )
    }

    companion object {
        fun from(name: String?): Diet = entries.firstOrNull { it.name == name } ?: CUSTOM
    }
}

data class Macros(val protein: Double, val carbs: Double, val fat: Double, val fiber: Double)

/** Goal macros recalculated for the goal's own diet (no change for CUSTOM). */
fun UserGoal.withDietMacros(): UserGoal {
    val macros = Diet.from(dietType).macrosFor(calorieGoal) ?: return this
    return copy(proteinGoal = macros.protein, carbGoal = macros.carbs, fatGoal = macros.fat, fiberGoal = macros.fiber)
}

enum class Sex(@StringRes val label: Int) {
    FEMALE(R.string.sex_female),
    MALE(R.string.sex_male)
}

enum class ActivityLevel(@StringRes val label: Int, @StringRes val description: Int, val factor: Double) {
    SEDENTARY(R.string.activity_sedentary, R.string.activity_sedentary_desc, 1.2),
    LIGHT(R.string.activity_light, R.string.activity_light_desc, 1.375),
    MODERATE(R.string.activity_moderate, R.string.activity_moderate_desc, 1.55),
    ACTIVE(R.string.activity_active, R.string.activity_active_desc, 1.725),
    VERY_ACTIVE(R.string.activity_very_active, R.string.activity_very_active_desc, 1.9)
}

/** Weekly weight change the calorie target aims for. */
enum class WeightPace(@StringRes val label: Int, val kgPerWeek: Double) {
    LOSE_FAST(R.string.pace_lose_fast, -0.75),
    LOSE(R.string.pace_lose, -0.5),
    LOSE_SLOW(R.string.pace_lose_slow, -0.25),
    MAINTAIN(R.string.pace_maintain, 0.0),
    GAIN(R.string.pace_gain, 0.25)
}

/**
 * Daily calorie estimate: Mifflin–St Jeor resting energy × activity factor, adjusted for the
 * chosen pace (≈7700 kcal per kg of body weight). Never suggests less than 1200 kcal.
 */
object CalorieCalculator {
    private const val KCAL_PER_KG = 7700.0
    const val MIN_KCAL = 1200

    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double =
        10 * weightKg + 6.25 * heightCm - 5 * age + if (sex == Sex.MALE) 5 else -161

    fun maintenance(sex: Sex, weightKg: Double, heightCm: Double, age: Int, activity: ActivityLevel): Int =
        (bmr(sex, weightKg, heightCm, age) * activity.factor).roundToInt()

    fun target(maintenance: Int, pace: WeightPace): Int =
        (maintenance + pace.kgPerWeek * KCAL_PER_KG / 7).roundToInt().coerceAtLeast(MIN_KCAL)

    fun age(birthYear: Int, today: LocalDate = LocalDate.now()): Int = today.year - birthYear
}

/** Body-mass index, or null without a height. */
fun bmi(weightKg: Double, heightCm: Double?): Double? =
    heightCm?.takeIf { it > 0 }?.let { weightKg / ((it / 100) * (it / 100)) }

@StringRes
fun bmiCategory(bmi: Double): Int = when {
    bmi < 18.5 -> R.string.bmi_under
    bmi < 25 -> R.string.bmi_normal
    bmi < 30 -> R.string.bmi_over
    else -> R.string.bmi_obese
}

/**
 * How sure an entry's amount is. Weekly totals get an error band from these: a weighed
 * chicken breast is close to exact, a restaurant meal can easily be 25% off either way.
 */
enum class Confidence(@StringRes val label: Int, val relativeError: Double) {
    WEIGHED(R.string.confidence_weighed, 0.05),
    ESTIMATED(R.string.confidence_estimated, 0.15),
    GUESSED(R.string.confidence_guessed, 0.30);

    companion object {
        fun from(name: String?): Confidence = entries.firstOrNull { it.name == name } ?: ESTIMATED
    }
}

/**
 * ± kcal for a set of entries. Errors of separate entries are treated as independent, so
 * they partly cancel: the band is the square root of the summed squared errors, not their sum.
 */
fun List<FoodEntry>.kcalUncertainty(): Double =
    kotlin.math.sqrt(sumOf { e -> (e.kcal * Confidence.from(e.confidence).relativeError).let { it * it } })
