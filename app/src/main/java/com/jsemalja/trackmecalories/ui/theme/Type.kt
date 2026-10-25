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
 * numbers read better bold) and tabular figures throughout.
 */
val Typography = Typography(
    displayLarge = base.displayLarge.tuned(FontWeight.Bold, (-0.5).sp),
    displayMedium = base.displayMedium.tuned(FontWeight.Bold, (-0.5).sp),
    displaySmall = base.displaySmall.tuned(FontWeight.Bold, (-0.25).sp),
    headlineLarge = base.headlineLarge.tuned(FontWeight.Bold, (-0.5).sp),
    headlineMedium = base.headlineMedium.tuned(FontWeight.Bold, (-0.25).sp),
    headlineSmall = base.headlineSmall.tuned(FontWeight.Bold, 0.sp),
    titleLarge = base.titleLarge.tuned(FontWeight.SemiBold, 0.sp),
    titleMedium = base.titleMedium.tuned(FontWeight.SemiBold, 0.1.sp),
    titleSmall = base.titleSmall.tuned(FontWeight.SemiBold, 0.1.sp),
    bodyLarge = base.bodyLarge.tuned(FontWeight.Normal, 0.15.sp),
    bodyMedium = base.bodyMedium.tuned(FontWeight.Normal, 0.15.sp),
    bodySmall = base.bodySmall.tuned(FontWeight.Normal, 0.2.sp),
    labelLarge = base.labelLarge.tuned(FontWeight.SemiBold, 0.1.sp),
    labelMedium = base.labelMedium.tuned(FontWeight.Medium, 0.3.sp),
    labelSmall = base.labelSmall.tuned(FontWeight.Medium, 0.3.sp)
)

private fun TextStyle.tuned(weight: FontWeight, spacing: androidx.compose.ui.unit.TextUnit) = copy(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    letterSpacing = spacing,
    fontFeatureSettings = TABULAR
)
