package com.jsemalja.trackmecalories.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette: green = nutrition/health (primary), amber = energy/quick actions
// (secondary), blue = hydration (tertiary, used for the water tracker).

// Light
val GreenPrimaryLight = Color(0xFF1B6E4F)
val OnGreenPrimaryLight = Color(0xFFFFFFFF)
val GreenPrimaryContainerLight = Color(0xFFA6F2C5)
val OnGreenPrimaryContainerLight = Color(0xFF00210F)

val AmberSecondaryLight = Color(0xFF7A5900)
val OnAmberSecondaryLight = Color(0xFFFFFFFF)
val AmberSecondaryContainerLight = Color(0xFFFFDEA6)
val OnAmberSecondaryContainerLight = Color(0xFF271900)

val BlueTertiaryLight = Color(0xFF3A608F)
val OnBlueTertiaryLight = Color(0xFFFFFFFF)
val BlueTertiaryContainerLight = Color(0xFFD2E4FF)
val OnBlueTertiaryContainerLight = Color(0xFF001C37)

// Neutrals are tinted green. Every surface role is set explicitly: the ones left out fall
// back to Material's baseline purple-greys, which is what made dialogs, tiles and the
// navigation bar look off-brand.
val BackgroundLight = Color(0xFFF6FAF6)
val OnBackgroundLight = Color(0xFF171D19)
val SurfaceVariantLight = Color(0xFFDCE5DC)
val OnSurfaceVariantLight = Color(0xFF404943)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF0F5F0)
val SurfaceContainerLight = Color(0xFFEAEFEA)
val SurfaceContainerHighLight = Color(0xFFE4EAE4)
val SurfaceContainerHighestLight = Color(0xFFDEE4DE)
val SurfaceDimLight = Color(0xFFD6DBD6)
val OutlineLight = Color(0xFF707971)
val OutlineVariantLight = Color(0xFFC0C9C0)
val InverseSurfaceLight = Color(0xFF2C322E)
val InverseOnSurfaceLight = Color(0xFFEDF2EC)

// Dark
val GreenPrimaryDark = Color(0xFF8ED6AE)
val OnGreenPrimaryDark = Color(0xFF00391C)
val GreenPrimaryContainerDark = Color(0xFF00522F)
val OnGreenPrimaryContainerDark = Color(0xFFA6F2C5)

val AmberSecondaryDark = Color(0xFFEEC16C)
val OnAmberSecondaryDark = Color(0xFF412D00)
val AmberSecondaryContainerDark = Color(0xFF5E4200)
val OnAmberSecondaryContainerDark = Color(0xFFFFDEA6)

val BlueTertiaryDark = Color(0xFFA3C9FF)
val OnBlueTertiaryDark = Color(0xFF00315A)
val BlueTertiaryContainerDark = Color(0xFF1C4A79)
val OnBlueTertiaryContainerDark = Color(0xFFD2E4FF)

val BackgroundDark = Color(0xFF0F1411)
val OnBackgroundDark = Color(0xFFDFE4DE)
val SurfaceVariantDark = Color(0xFF3B453E)
val OnSurfaceVariantDark = Color(0xFFBFC9C1)
val SurfaceContainerLowestDark = Color(0xFF0A0F0C)
val SurfaceContainerLowDark = Color(0xFF171D19)
val SurfaceContainerDark = Color(0xFF1B211D)
val SurfaceContainerHighDark = Color(0xFF242B26)
val SurfaceContainerHighestDark = Color(0xFF2F3631)
val SurfaceBrightDark = Color(0xFF353B37)
val OutlineDark = Color(0xFF89938B)
val OutlineVariantDark = Color(0xFF3B453E)
val InverseSurfaceDark = Color(0xFFDFE4DE)
val InverseOnSurfaceDark = Color(0xFF2C322E)

// Macro accent colors — intentionally independent of the brand palette so each
// macro reads as its own category at a glance (used in progress pills/summaries).
val MacroProtein = Color(0xFFE57373)
val MacroCarbs = Color(0xFF81C784)
val MacroFat = Color(0xFFFFB74D)
val MacroFiber = Color(0xFF64B5F6)
val MacroSalt = Color(0xFFBA68C8)
