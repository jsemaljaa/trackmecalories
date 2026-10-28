package com.jsemalja.trackmecalories.data

import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource

// Display strings for the domain enums, kept apart so the domain code stays free of UI
// resources. Every enum entry must be mapped here, so the `when`s stay exhaustive.

val Diet.label: StringResource get() = when (this) {
    Diet.BALANCED -> Res.string.diet_balanced
    Diet.HIGH_PROTEIN -> Res.string.diet_high_protein
    Diet.LOW_CARB -> Res.string.diet_low_carb
    Diet.KETO -> Res.string.diet_keto
    Diet.MEDITERRANEAN -> Res.string.diet_mediterranean
    Diet.ZONE -> Res.string.diet_zone
    Diet.CUSTOM -> Res.string.diet_custom
}

val Diet.description: StringResource get() = when (this) {
    Diet.BALANCED -> Res.string.diet_balanced_desc
    Diet.HIGH_PROTEIN -> Res.string.diet_high_protein_desc
    Diet.LOW_CARB -> Res.string.diet_low_carb_desc
    Diet.KETO -> Res.string.diet_keto_desc
    Diet.MEDITERRANEAN -> Res.string.diet_mediterranean_desc
    Diet.ZONE -> Res.string.diet_zone_desc
    Diet.CUSTOM -> Res.string.diet_custom_desc
}

val Sex.label: StringResource get() = when (this) {
    Sex.FEMALE -> Res.string.sex_female
    Sex.MALE -> Res.string.sex_male
}

val ActivityLevel.label: StringResource get() = when (this) {
    ActivityLevel.SEDENTARY -> Res.string.activity_sedentary
    ActivityLevel.LIGHT -> Res.string.activity_light
    ActivityLevel.MODERATE -> Res.string.activity_moderate
    ActivityLevel.ACTIVE -> Res.string.activity_active
    ActivityLevel.VERY_ACTIVE -> Res.string.activity_very_active
}

val ActivityLevel.description: StringResource get() = when (this) {
    ActivityLevel.SEDENTARY -> Res.string.activity_sedentary_desc
    ActivityLevel.LIGHT -> Res.string.activity_light_desc
    ActivityLevel.MODERATE -> Res.string.activity_moderate_desc
    ActivityLevel.ACTIVE -> Res.string.activity_active_desc
    ActivityLevel.VERY_ACTIVE -> Res.string.activity_very_active_desc
}

val WeightPace.label: StringResource get() = when (this) {
    WeightPace.LOSE_FAST -> Res.string.pace_lose_fast
    WeightPace.LOSE -> Res.string.pace_lose
    WeightPace.LOSE_SLOW -> Res.string.pace_lose_slow
    WeightPace.MAINTAIN -> Res.string.pace_maintain
    WeightPace.GAIN -> Res.string.pace_gain
}

val Confidence.label: StringResource get() = when (this) {
    Confidence.WEIGHED -> Res.string.confidence_weighed
    Confidence.ESTIMATED -> Res.string.confidence_estimated
    Confidence.GUESSED -> Res.string.confidence_guessed
}

fun bmiCategory(bmi: Double): StringResource = when (bmiClass(bmi)) {
    BmiClass.UNDER -> Res.string.bmi_under
    BmiClass.NORMAL -> Res.string.bmi_normal
    BmiClass.OVER -> Res.string.bmi_over
    BmiClass.OBESE -> Res.string.bmi_obese
}

val EatBack.label: StringResource get() = when (this) {
    EatBack.OFF -> Res.string.eat_back_off
    EatBack.HALF -> Res.string.eat_back_half
    EatBack.FULL -> Res.string.eat_back_full
}

val SearchSource.label: StringResource get() = when (this) {
    SearchSource.BASIC -> Res.string.source_basic
    SearchSource.MENU -> Res.string.source_menu
    SearchSource.KALORICKE -> Res.string.source_kaloricke
    SearchSource.OPEN_FOOD_FACTS -> Res.string.source_off
}

val CookFactor.label: StringResource get() = when (this) {
    CookFactor.NONE -> Res.string.cook_none
    CookFactor.RICE -> Res.string.cook_rice
    CookFactor.PASTA -> Res.string.cook_pasta
    CookFactor.GRAINS -> Res.string.cook_grains
    CookFactor.LEGUMES -> Res.string.cook_legumes
    CookFactor.MEAT -> Res.string.cook_meat
    CookFactor.FISH -> Res.string.cook_fish
    CookFactor.VEGETABLES -> Res.string.cook_vegetables
}
