package com.jsemalja.trackmecalories.data

import androidx.annotation.StringRes
import com.jsemalja.trackmecalories.R

// Android string resources for the shared enums (the enums themselves live in :shared and
// can't see R). Every enum entry must be mapped here, so the `when`s stay exhaustive.

@get:StringRes
val Diet.label: Int get() = when (this) {
    Diet.BALANCED -> R.string.diet_balanced
    Diet.HIGH_PROTEIN -> R.string.diet_high_protein
    Diet.LOW_CARB -> R.string.diet_low_carb
    Diet.KETO -> R.string.diet_keto
    Diet.MEDITERRANEAN -> R.string.diet_mediterranean
    Diet.ZONE -> R.string.diet_zone
    Diet.CUSTOM -> R.string.diet_custom
}

@get:StringRes
val Diet.description: Int get() = when (this) {
    Diet.BALANCED -> R.string.diet_balanced_desc
    Diet.HIGH_PROTEIN -> R.string.diet_high_protein_desc
    Diet.LOW_CARB -> R.string.diet_low_carb_desc
    Diet.KETO -> R.string.diet_keto_desc
    Diet.MEDITERRANEAN -> R.string.diet_mediterranean_desc
    Diet.ZONE -> R.string.diet_zone_desc
    Diet.CUSTOM -> R.string.diet_custom_desc
}

@get:StringRes
val Sex.label: Int get() = when (this) {
    Sex.FEMALE -> R.string.sex_female
    Sex.MALE -> R.string.sex_male
}

@get:StringRes
val ActivityLevel.label: Int get() = when (this) {
    ActivityLevel.SEDENTARY -> R.string.activity_sedentary
    ActivityLevel.LIGHT -> R.string.activity_light
    ActivityLevel.MODERATE -> R.string.activity_moderate
    ActivityLevel.ACTIVE -> R.string.activity_active
    ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active
}

@get:StringRes
val ActivityLevel.description: Int get() = when (this) {
    ActivityLevel.SEDENTARY -> R.string.activity_sedentary_desc
    ActivityLevel.LIGHT -> R.string.activity_light_desc
    ActivityLevel.MODERATE -> R.string.activity_moderate_desc
    ActivityLevel.ACTIVE -> R.string.activity_active_desc
    ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active_desc
}

@get:StringRes
val WeightPace.label: Int get() = when (this) {
    WeightPace.LOSE_FAST -> R.string.pace_lose_fast
    WeightPace.LOSE -> R.string.pace_lose
    WeightPace.LOSE_SLOW -> R.string.pace_lose_slow
    WeightPace.MAINTAIN -> R.string.pace_maintain
    WeightPace.GAIN -> R.string.pace_gain
}

@get:StringRes
val Confidence.label: Int get() = when (this) {
    Confidence.WEIGHED -> R.string.confidence_weighed
    Confidence.ESTIMATED -> R.string.confidence_estimated
    Confidence.GUESSED -> R.string.confidence_guessed
}

@StringRes
fun bmiCategory(bmi: Double): Int = when (bmiClass(bmi)) {
    BmiClass.UNDER -> R.string.bmi_under
    BmiClass.NORMAL -> R.string.bmi_normal
    BmiClass.OVER -> R.string.bmi_over
    BmiClass.OBESE -> R.string.bmi_obese
}

@get:StringRes
val EatBack.label: Int get() = when (this) {
    EatBack.OFF -> R.string.eat_back_off
    EatBack.HALF -> R.string.eat_back_half
    EatBack.FULL -> R.string.eat_back_full
}

@get:StringRes
val SearchSource.label: Int get() = when (this) {
    SearchSource.BASIC -> R.string.source_basic
    SearchSource.MENU -> R.string.source_menu
    SearchSource.KALORICKE -> R.string.source_kaloricke
    SearchSource.OPEN_FOOD_FACTS -> R.string.source_off
}

@get:StringRes
val CookFactor.label: Int get() = when (this) {
    CookFactor.NONE -> R.string.cook_none
    CookFactor.RICE -> R.string.cook_rice
    CookFactor.PASTA -> R.string.cook_pasta
    CookFactor.GRAINS -> R.string.cook_grains
    CookFactor.LEGUMES -> R.string.cook_legumes
    CookFactor.MEAT -> R.string.cook_meat
    CookFactor.FISH -> R.string.cook_fish
    CookFactor.VEGETABLES -> R.string.cook_vegetables
}
