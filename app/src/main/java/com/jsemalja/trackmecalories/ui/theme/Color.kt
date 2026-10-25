package com.jsemalja.trackmecalories.ui.theme

import androidx.compose.ui.graphics.Color

// Fixed brand roles that don't follow the accent: amber = energy/quick actions
// (secondary), blue = hydration (tertiary, used for the water tracker). The primary
// colour and the accent-tinted neutrals come from [AccentColor].

// Light
val AmberSecondaryLight = Color(0xFF7A5900)
val OnAmberSecondaryLight = Color(0xFFFFFFFF)
val AmberSecondaryContainerLight = Color(0xFFFFDEA6)
val OnAmberSecondaryContainerLight = Color(0xFF271900)

val BlueTertiaryLight = Color(0xFF3A608F)
val OnBlueTertiaryLight = Color(0xFFFFFFFF)
val BlueTertiaryContainerLight = Color(0xFFD2E4FF)
val OnBlueTertiaryContainerLight = Color(0xFF001C37)

// Dark
val AmberSecondaryDark = Color(0xFFEEC16C)
val OnAmberSecondaryDark = Color(0xFF412D00)
val AmberSecondaryContainerDark = Color(0xFF5E4200)
val OnAmberSecondaryContainerDark = Color(0xFFFFDEA6)

val BlueTertiaryDark = Color(0xFFA3C9FF)
val OnBlueTertiaryDark = Color(0xFF00315A)
val BlueTertiaryContainerDark = Color(0xFF1C4A79)
val OnBlueTertiaryContainerDark = Color(0xFFD2E4FF)

// Macro accent colors — intentionally independent of the brand palette so each
// macro reads as its own category at a glance (used in progress pills/summaries).
val MacroProtein = Color(0xFFE57373)
val MacroCarbs = Color(0xFF81C784)
val MacroFat = Color(0xFFFFB74D)
val MacroFiber = Color(0xFF64B5F6)
val MacroSalt = Color(0xFFBA68C8)
