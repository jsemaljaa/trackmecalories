package com.jsemalja.trackmecalories.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tabular figures: every digit has the same width, so calorie counts and macro values
// don't shift sideways as they change, and numbers line up in lists.
private const val TABULAR = "tnum"

private val base = Typography()

/**
 * Material 3 scale with heavier, tighter headings (the app is number-heavy, and big
 * numbers read better bold) and tabular figures throughout, set in [fontFamily]
 * (the user's [AppFont] choice).
 */
fun appTypography(fontFamily: FontFamily = FontFamily.Default) = Typography(
    displayLarge = base.displayLarge.tuned(fontFamily, FontWeight.Bold, (-0.5).sp),
    displayMedium = base.displayMedium.tuned(fontFamily, FontWeight.Bold, (-0.5).sp),
    displaySmall = base.displaySmall.tuned(fontFamily, FontWeight.Bold, (-0.25).sp),
    headlineLarge = base.headlineLarge.tuned(fontFamily, FontWeight.Bold, (-0.5).sp),
    headlineMedium = base.headlineMedium.tuned(fontFamily, FontWeight.Bold, (-0.25).sp),
    headlineSmall = base.headlineSmall.tuned(fontFamily, FontWeight.Bold, 0.sp),
    titleLarge = base.titleLarge.tuned(fontFamily, FontWeight.SemiBold, 0.sp),
    titleMedium = base.titleMedium.tuned(fontFamily, FontWeight.SemiBold, 0.1.sp),
    titleSmall = base.titleSmall.tuned(fontFamily, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = base.bodyLarge.tuned(fontFamily, FontWeight.Normal, 0.15.sp),
    bodyMedium = base.bodyMedium.tuned(fontFamily, FontWeight.Normal, 0.15.sp),
    bodySmall = base.bodySmall.tuned(fontFamily, FontWeight.Normal, 0.2.sp),
    labelLarge = base.labelLarge.tuned(fontFamily, FontWeight.SemiBold, 0.1.sp),
    labelMedium = base.labelMedium.tuned(fontFamily, FontWeight.Medium, 0.3.sp),
    labelSmall = base.labelSmall.tuned(fontFamily, FontWeight.Medium, 0.3.sp)
)

private fun TextStyle.tuned(family: FontFamily, weight: FontWeight, spacing: androidx.compose.ui.unit.TextUnit) = copy(
    fontFamily = family,
    fontWeight = weight,
    letterSpacing = spacing,
    fontFeatureSettings = TABULAR
)
