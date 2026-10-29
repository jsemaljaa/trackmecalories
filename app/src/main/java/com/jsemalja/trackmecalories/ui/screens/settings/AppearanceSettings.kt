package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.ui.screens.FitText
import com.jsemalja.trackmecalories.ui.theme.AccentColor
import com.jsemalja.trackmecalories.ui.theme.AccentPalette
import com.jsemalja.trackmecalories.ui.theme.AppFont
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.Appearance
import com.jsemalja.trackmecalories.ui.theme.CornerStyle
import com.jsemalja.trackmecalories.ui.theme.LocalDarkTheme
import com.jsemalja.trackmecalories.ui.theme.MAX_CUSTOM_CHROMA
import com.jsemalja.trackmecalories.ui.theme.ThemeMode
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import com.jsemalja.trackmecalories.ui.theme.colorAt
import com.jsemalja.trackmecalories.ui.theme.auroraColors
import com.jsemalja.trackmecalories.ui.theme.hardShadow
import com.jsemalja.trackmecalories.ui.theme.hexOf
import com.jsemalja.trackmecalories.ui.theme.hueChroma
import com.jsemalja.trackmecalories.ui.theme.nearestPreset
import com.jsemalja.trackmecalories.ui.theme.neutrals
import com.jsemalja.trackmecalories.ui.theme.parseHexColor
import com.jsemalja.trackmecalories.ui.theme.themeShapes
import com.jsemalja.trackmecalories.ui.theme.themeTypography
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSettings(
    appearance: Appearance,
    onThemeChange: (AppTheme) -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onCustomAccentChange: (Int) -> Unit,
    onFontChange: (AppFont?) -> Unit,
    onCornersChange: (CornerStyle?) -> Unit
) {
    val dark = LocalDarkTheme.current
    var editingCustom by rememberSaveable { mutableStateOf(false) }

    Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    // Scrolls sideways on narrow phones; the padding leaves room for Bold's shadow.
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppTheme.entries.forEach { theme ->
            ThemeTile(
                theme = theme,
                tint = appearance.palette.primaryLight,
                dark = dark,
                selected = appearance.theme == theme,
                onClick = { onThemeChange(theme) }
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { i, mode ->
            SegmentedButton(
                selected = appearance.mode == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                colors = appSegmentedColors()
            ) { FitText(stringResource(mode.label)) }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.accent_colour), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    // Every swatch is named, so the colours can be told apart by more than hue.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AccentColor.available.filter { it != AccentColor.CUSTOM }.forEach { accent ->
            val selected = appearance.accent == accent
            if (accent == AccentColor.WALLPAPER) {
                // Wallpaper colours are unknown up front: a soft colour wheel.
                AccentSwatch(
                    name = stringResource(accent.label),
                    fill = Brush.sweepGradient(listOf(Color(0xFF8ED6AE), Color(0xFFA8C8FF), Color(0xFFCFBDFF), Color(0xFFFFB0CA), Color(0xFFFFB68B), Color(0xFF8ED6AE))),
                    iconTint = Color.Black.copy(alpha = 0.7f),
                    selected = selected,
                    icon = Icons.Default.Wallpaper,
                    onClick = { onAccentChange(accent) }
                )
            } else {
                val color = accent.palette.primary(dark)
                AccentSwatch(
                    name = stringResource(accent.label),
                    fill = SolidColor(color),
                    iconTint = checkTint(color),
                    selected = selected,
                    onClick = { onAccentChange(accent) }
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    CustomAccentRow(
        selected = appearance.accent == AccentColor.CUSTOM,
        color = appearance.palette.primary(dark),
        hex = hexOf(appearance.customAccent),
        onClick = { editingCustom = true }
    )

    if (editingCustom) {
        val wallpaperPrimary = MaterialTheme.colorScheme.primary
        CustomAccentSheet(
            // Start from what's on screen, so a small nudge from a preset is easy.
            initial = when (appearance.accent) {
                AccentColor.CUSTOM -> appearance.customAccent
                AccentColor.WALLPAPER -> wallpaperPrimary.toArgb()
                else -> appearance.palette.primaryLight.toArgb()
            },
            theme = appearance.theme,
            onDismiss = { editingCustom = false },
            onApply = {
                onCustomAccentChange(it)
                editingCustom = false
            }
        )
    }

    // Corners and font follow the theme until overridden here; picking a theme resets them.
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.corners), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    val cornerOptions = listOf<CornerStyle?>(null) + CornerStyle.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        cornerOptions.forEachIndexed { i, corners ->
            SegmentedButton(
                selected = appearance.corners == corners,
                onClick = { onCornersChange(corners) },
                shape = SegmentedButtonDefaults.itemShape(i, cornerOptions.size),
                colors = appSegmentedColors()
            ) { FitText(stringResource(corners?.label ?: R.string.from_theme)) }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.font), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = appearance.font == null,
            onClick = { onFontChange(null) },
            label = { Text(stringResource(R.string.from_theme), fontFamily = themeTypography(appearance.theme).titleMedium.fontFamily) }
        )
        AppFont.entries.forEach { font ->
            FilterChip(
                selected = appearance.font == font,
                onClick = { onFontChange(font) },
                // Each chip is set in its own font, so it doubles as a preview.
                label = { Text(stringResource(font.label), fontFamily = font.family) }
            )
        }
    }
}

/**
 * A miniature of [theme] on its own page colour: a card drawn the theme's way (Classic a
 * white sheet on a shadow, Glass frosted over its aurora, Paper a ruled sheet, Bold
 * ink-bordered with a hard shadow, Terminal a square hairline panel) with "Aa" in the
 * theme's heading typeface and a progress bar in its style, so the themes can be compared
 * at a glance rather than by name.
 */
@Composable
private fun ThemeTile(
    theme: AppTheme,
    tint: Color,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val n = remember(theme, tint, dark) { neutrals(theme, tint, dark) }
    val shapes = remember(theme) { themeShapes(theme) }
    val headingFont = remember(theme) { themeTypography(theme).titleLarge.fontFamily }
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val frame = MaterialTheme.shapes.medium
    val card = shapes.medium

    val cardModifier = when (theme) {
        AppTheme.CLASSIC -> Modifier
            .then(if (dark) Modifier else Modifier.shadow(2.dp, card))
            .background(n.containerLow, card)
        AppTheme.GLASS -> Modifier
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))
                    else listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.5f))
                ),
                card
            )
            .border(1.dp, Color.White.copy(alpha = if (dark) 0.35f else 1f), card)
        AppTheme.PAPER -> Modifier.background(n.containerLow, card).border(1.dp, n.outlineVariant, card)
        AppTheme.BOLD -> Modifier
            .hardShadow(card, if (dark) primary else n.outline, 3.dp)
            .background(n.containerLow, card)
            .border(2.dp, n.outline, card)
        AppTheme.TERMINAL -> Modifier.background(n.containerLow, card).border(1.dp, n.outlineVariant, card)
    }
    val barHeight = when (theme) {
        AppTheme.PAPER -> 2.dp
        AppTheme.BOLD -> 7.dp
        else -> 5.dp
    }
    val barShape = if (theme == AppTheme.PAPER || theme == AppTheme.TERMINAL) RectangleShape else shapes.extraSmall

    Column(
        Modifier.width(92.dp).selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(frame)
                .background(n.background)
                .then(
                    if (theme == AppTheme.GLASS) Modifier.background(
                        Brush.linearGradient(auroraColors(primary, dark).map { it.copy(alpha = 0.8f) })
                    ) else Modifier
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) primary else colors.outlineVariant,
                    shape = frame
                )
                .padding(start = 10.dp, top = 10.dp, end = 13.dp, bottom = 13.dp)
        ) {
            Column(
                Modifier.fillMaxSize().then(cardModifier).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (theme == AppTheme.TERMINAL) ">_" else "Aa",
                    fontFamily = headingFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = n.onBackground
                )
                if (theme == AppTheme.TERMINAL) {
                    // Terminal's meter: separate cells.
                    Row(Modifier.fillMaxWidth().height(barHeight), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { i ->
                            Box(Modifier.weight(1f).fillMaxHeight().background(if (i < 3) primary else primary.copy(alpha = 0.2f)))
                        }
                    }
                } else Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(barShape)
                        .background(if (theme == AppTheme.BOLD) Color.Transparent else primary.copy(alpha = 0.2f))
                        .then(if (theme == AppTheme.BOLD) Modifier.border(1.5.dp, n.outline, barShape) else Modifier)
                ) {
                    Box(Modifier.fillMaxWidth(0.6f).fillMaxHeight().clip(barShape).background(primary))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        FitText(
            stringResource(theme.label),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) primary else colors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else null
        )
    }
}

/** Every hue as vivid as a custom colour can be, closed into a loop for a gradient. */
private val customWheel: List<Color> = (0..12).map { Color(colorAt(it * 30.0, MAX_CUSTOM_CHROMA, 60.0)) }

private fun checkTint(color: Color) = if (color.luminance() > 0.4f) Color.Black.copy(alpha = 0.75f) else Color.White

/**
 * One accent dot with its name under it, in the colour the current mode would use. Picking
 * it shrinks the dot inside a ring and shows a check (or [icon] for the special ones), like
 * the system wallpaper picker.
 */
@Composable
private fun AccentSwatch(
    name: String,
    fill: Brush,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val inset by animateDpAsState(if (selected) 5.dp else 1.dp, label = "swatch inset")
    Column(
        Modifier
            .width(60.dp)
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .border(if (selected) 2.5.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .padding(inset)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center
        ) {
            val shown = if (selected && icon == null) Icons.Default.Check else icon
            if (shown != null) Icon(shown, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        FitText(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else null
        )
    }
}

/**
 * The custom colour as a full-width row: a vivid wheel until one is picked, then the colour
 * itself and its hex code. Tapping it opens the editor either way.
 */
@Composable
private fun CustomAccentRow(selected: Boolean, color: Color, hex: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (selected) SolidColor(color) else Brush.sweepGradient(customWheel)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = if (selected) checkTint(color) else Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.custom_accent_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                if (selected) "#$hex" else stringResource(R.string.accent_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Icon(Icons.Default.Edit, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/**
 * Builds a custom accent on a hue × vividness pad (with sliders for fine steps and TalkBack),
 * or from a hex code. Everything works in OKLCh (see Tones.kt), so equal steps look equally
 * far apart. The previews draw the generated tones on the theme's own light and dark pages,
 * exactly as the app will, and the colour is named after the nearest preset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAccentSheet(initial: Int, theme: AppTheme, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val start = remember { hueChroma(initial, MAX_CUSTOM_CHROMA) }
    var hue by rememberSaveable { mutableFloatStateOf(start.first.toFloat()) }
    var chroma by rememberSaveable { mutableFloatStateOf(start.second.toFloat()) }
    var seed by rememberSaveable { mutableIntStateOf(initial) }
    var hex by rememberSaveable { mutableStateOf(hexOf(initial)) }
    val palette = remember(seed) { AccentPalette.fromSeed(seed, MAX_CUSTOM_CHROMA) }
    val nearest = remember(seed) { nearestPreset(seed) }
    val hexValid = parseHexColor(hex) != null

    // The seed from the pad and sliders is the light-mode primary itself (tone 40).
    fun fromSliders(newHue: Float, newChroma: Float) {
        hue = newHue
        chroma = newChroma
        seed = colorAt(newHue.toDouble(), newChroma.toDouble(), 40.0)
        hex = hexOf(seed)
    }

    val vividness = remember(hue) { (0..6).map { Color(colorAt(hue.toDouble(), MAX_CUSTOM_CHROMA * it / 6, 60.0)) } }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(stringResource(R.string.custom_accent_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.accent_close_to, stringResource(nearest.label)) + " · #" + hexOf(palette.primaryLight.toArgb()),
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentPreview(palette, theme, dark = false, Modifier.weight(1f))
                AccentPreview(palette, theme, dark = true, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            HueVividnessPad(hue, chroma) { h, c -> fromSliders(h, c) }
            Spacer(Modifier.height(12.dp))
            SliderLabel(stringResource(R.string.accent_hue), "${hue.roundToInt()}°")
            GradientSlider(hue, 0f..360f, customWheel) { fromSliders(it, chroma) }
            SliderLabel(stringResource(R.string.accent_vividness), "${(chroma / MAX_CUSTOM_CHROMA * 100).roundToInt()}%")
            GradientSlider(chroma, 0f..MAX_CUSTOM_CHROMA.toFloat(), vividness) { fromSliders(hue, it) }
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.accent_muted), style = MaterialTheme.typography.labelSmall, color = labelColor, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.accent_vivid), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = hex,
                onValueChange = { text ->
                    hex = text.removePrefix("#").filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6).uppercase()
                    parseHexColor(hex)?.let { typed ->
                        seed = typed
                        val (h, c) = hueChroma(typed, MAX_CUSTOM_CHROMA)
                        hue = h.toFloat()
                        chroma = c.toFloat()
                    }
                },
                label = { Text(stringResource(R.string.accent_hex)) },
                leadingIcon = { Box(Modifier.size(22.dp).clip(CircleShape).background(Color(seed))) },
                prefix = { Text("#") },
                singleLine = true,
                isError = !hexValid,
                supportingText = { Text(stringResource(if (hexValid) R.string.accent_tone_note else R.string.accent_hex_invalid)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onApply(seed) }, enabled = hexValid) { Text(stringResource(R.string.apply)) }
            }
        }
    }
}

@Composable
private fun SliderLabel(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Hue runs left to right, vividness bottom (grey) to top; tap or drag anywhere to pick.
 * The sliders below carry the same values for TalkBack, so the pad itself is hidden from it.
 */
@Composable
private fun HueVividnessPad(hue: Float, chroma: Float, onChange: (Float, Float) -> Unit) {
    val grey = remember { Color(colorAt(0.0, 0.0, 60.0)) }
    val picked = Color(colorAt(hue.toDouble(), chroma.toDouble(), 60.0))
    val change by rememberUpdatedState(onChange)
    val max = MAX_CUSTOM_CHROMA.toFloat()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(MaterialTheme.shapes.medium)
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                fun pick(at: Offset) = change(
                    (at.x / size.width).coerceIn(0f, 1f) * 360f,
                    (1f - (at.y / size.height).coerceIn(0f, 1f)) * max
                )
                // Consuming every move keeps the sheet from taking a downward drag as a dismiss.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pick(down.position)
                    drag(down.id) {
                        pick(it.position)
                        it.consume()
                    }
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(customWheel))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, grey)))
        val center = Offset(hue / 360f * size.width, (1f - chroma / max) * size.height)
        drawCircle(Color.White, 13.dp.toPx(), center)
        drawCircle(picked, 10.dp.toPx(), center)
        drawCircle(Color.Black.copy(alpha = 0.35f), 13.dp.toPx(), center, style = Stroke(1.dp.toPx()))
    }
}

/**
 * The palette on [theme]'s page in one mode, labelled: a ring and bar like the Today
 * screen's, a filled and a tonal pill.
 */
@Composable
private fun AccentPreview(palette: AccentPalette, theme: AppTheme, dark: Boolean, modifier: Modifier = Modifier) {
    val n = remember(theme, palette, dark) { neutrals(theme, palette.primaryLight, dark) }
    val primary = palette.primary(dark)
    val onPrimary = if (dark) palette.onPrimaryDark else palette.onPrimaryLight
    val container = if (dark) palette.containerDark else palette.containerLight
    val onContainer = if (dark) palette.onContainerDark else palette.onContainerLight
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier
            .clip(shape)
            .background(n.background)
            .border(1.dp, n.outlineVariant, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(if (dark) R.string.mode_dark else R.string.mode_light), style = MaterialTheme.typography.labelMedium, color = n.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                progress = { 0.7f },
                color = primary,
                trackColor = container,
                strokeWidth = 5.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Aa",
                    style = MaterialTheme.typography.labelLarge,
                    color = onPrimary,
                    modifier = Modifier.clip(CircleShape).background(primary).padding(horizontal = 14.dp, vertical = 4.dp)
                )
                Text(
                    "Aa",
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer,
                    modifier = Modifier.clip(CircleShape).background(container).padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
        LinearProgressIndicator(
            progress = { 0.6f },
            color = primary,
            trackColor = container,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** A slider over a gradient of what its values look like (the track itself is hidden). */
@Composable
private fun GradientSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    gradient: List<Color>,
    onChange: (Float) -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(gradient))
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onSurface,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}
