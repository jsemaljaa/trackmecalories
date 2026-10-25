package com.jsemalja.trackmecalories.ui.screens

import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.jsemalja.trackmecalories.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import com.jsemalja.trackmecalories.ui.theme.AccentColor
import com.jsemalja.trackmecalories.ui.theme.Appearance
import com.jsemalja.trackmecalories.ui.theme.DesignStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/**
 * Settings screen for user configuration.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    appearance: Appearance,
    onStyleChange: (DesignStyle) -> Unit,
    onAccentChange: (AccentColor) -> Unit
) {
    val currentGoals by viewModel.userGoal.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader(Icons.Default.Settings, "Settings")

        Spacer(Modifier.height(24.dp))

        SettingsSectionCard(title = "Appearance", titleColor = MaterialTheme.colorScheme.primary) {
            AppearanceSettings(appearance, onStyleChange, onAccentChange)
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.goals_title), titleColor = MaterialTheme.colorScheme.primary) {
            GoalsEditor(goals = currentGoals, latestWeightKg = null, onSave = viewModel::updateGoals)
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = "Data", titleColor = MaterialTheme.colorScheme.primary) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        exportError = try {
                            val csv = viewModel.exportCsv()
                            val file = File(context.cacheDir, "trackmecalories_export_${LocalDate.now()}.csv")
                            file.writeText(csv)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Food Log"))
                            null
                        } catch (e: Exception) {
                            "Export failed: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Data (CSV)")
            }

            exportError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = "Danger Zone", titleColor = MaterialTheme.colorScheme.error) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wipe All Data")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Wipe All Data?") },
            text = { Text("This will permanently delete all your food logs. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettings(
    appearance: Appearance,
    onStyleChange: (DesignStyle) -> Unit,
    onAccentChange: (AccentColor) -> Unit
) {
    Text("Style", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        DesignStyle.entries.forEachIndexed { i, style ->
            SegmentedButton(
                selected = appearance.style == style,
                onClick = { onStyleChange(style) },
                shape = SegmentedButtonDefaults.itemShape(i, DesignStyle.entries.size),
                colors = appSegmentedColors()
            ) { Text(style.label) }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text("Accent colour", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    val dark = isSystemInDarkTheme()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentColor.available.forEach { accent ->
            AccentSwatch(
                accent = accent,
                color = if (dark) accent.primaryDark else accent.primaryLight,
                selected = appearance.accent == accent,
                onClick = { onAccentChange(accent) }
            )
        }
    }
}

@Composable
private fun AccentSwatch(accent: AccentColor, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (accent == AccentColor.WALLPAPER) {
                        // Wallpaper colours are unknown up front: show a colour wheel.
                        Modifier.background(Brush.sweepGradient(listOf(Color(0xFF8ED6AE), Color(0xFFA8C8FF), Color(0xFFCFBDFF), Color(0xFFFFB0CA), Color(0xFFFFB68B), Color(0xFF8ED6AE))))
                    } else {
                        Modifier.background(color)
                    }
                )
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (color.luminance() > 0.4f) Color.Black.copy(alpha = 0.75f) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(accent.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
