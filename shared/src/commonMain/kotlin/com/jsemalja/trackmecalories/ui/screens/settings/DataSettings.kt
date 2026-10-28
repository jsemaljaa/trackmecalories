package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import com.jsemalja.trackmecalories.resources.*
import com.jsemalja.trackmecalories.data.currentDate
import com.jsemalja.trackmecalories.ui.rememberShareFile
import com.jsemalja.trackmecalories.data.withArgs

/** Export to CSV, and wiping everything (behind a confirmation). */
@Composable
internal fun DataSettings(viewModel: MainViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val shareFile = rememberShareFile()
    val scope = rememberCoroutineScope()
    val exportChooserTitle = stringResource(Res.string.export_chooser)
    val exportFailed = stringResource(Res.string.export_failed)

    SettingsSectionCard {
        Text(
            stringResource(Res.string.settings_export_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    exportError = try {
                        val csv = viewModel.exportCsv()
                        shareFile("trackmecalories_export_${currentDate()}.csv", "text/csv", csv, exportChooserTitle)
                        null
                    } catch (e: Exception) {
                        exportFailed.withArgs(e.message ?: "")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.export_csv))
        }

        exportError?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    SettingsSectionCard(title = stringResource(Res.string.danger_zone), titleColor = MaterialTheme.colorScheme.error) {
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.wipe_all))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.wipe_all_title)) },
            text = { Text(stringResource(Res.string.wipe_all_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(Res.string.wipe_all_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }
}
