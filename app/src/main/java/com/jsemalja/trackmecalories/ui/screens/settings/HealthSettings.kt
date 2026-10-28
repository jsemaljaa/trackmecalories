package com.jsemalja.trackmecalories.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.jsemalja.trackmecalories.data.EatBack
import com.jsemalja.trackmecalories.data.HealthData
import com.jsemalja.trackmecalories.data.HealthStatus
import com.jsemalja.trackmecalories.ui.screens.FitText
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.data.label
import com.jsemalja.trackmecalories.resources.*

/**
 * Health Connect: connection status, the button to grant access (or fix why it can't be),
 * and how to get Zepp or Samsung Health syncing into it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HealthSettings(viewModel: MainViewModel) {
    val status by viewModel.healthStatus.collectAsState()
    val missing by viewModel.healthMissing.collectAsState()
    val context = LocalContext.current
    val samsungHealth = remember { context.packageManager.getLaunchIntentForPackage(HealthData.SAMSUNG_HEALTH) }
    var denied by remember { mutableStateOf(false) }
    val requestPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        denied = granted.isEmpty()
        viewModel.refreshHealth()
    }

    Text(
        stringResource(
            when (status) {
                HealthStatus.CONNECTED -> Res.string.health_status_connected
                HealthStatus.NOT_CONNECTED -> Res.string.health_status_not_connected
                HealthStatus.NEEDS_INSTALL -> Res.string.health_status_needs_install
                HealthStatus.UNAVAILABLE -> Res.string.health_status_unavailable
            }
        ),
        style = MaterialTheme.typography.bodyMedium
    )
    if (denied && status == HealthStatus.NOT_CONNECTED) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(Res.string.health_denied), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    when (status) {
        HealthStatus.NOT_CONNECTED -> Button(onClick = { requestPermissions.launch(HealthData.permissions) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.health_connect))
        }
        // Granting more (or revoking) happens in Health Connect's own screen.
        HealthStatus.CONNECTED -> {
            // E.g. total calories, added for Samsung Health after the user had connected.
            if (missing) {
                Button(onClick = { requestPermissions.launch(HealthData.permissions) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.health_grant_missing))
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    runCatching { context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(Res.string.health_manage)) }
        }
        HealthStatus.NEEDS_INSTALL -> OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"))
                            .setPackage("com.android.vending")
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(Res.string.health_install)) }
        HealthStatus.UNAVAILABLE -> Unit
    }
    if (status == HealthStatus.CONNECTED) {
        val eatBack by viewModel.eatBack.collectAsState()
        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.eat_back), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.eat_back_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            EatBack.entries.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = eatBack == option,
                    onClick = { viewModel.setEatBack(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, EatBack.entries.size),
                    colors = appSegmentedColors()
                ) { FitText(stringResource(option.label)) }
            }
        }
    }
    if (status != HealthStatus.UNAVAILABLE) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.health_zepp_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(Res.string.health_samsung_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (samsungHealth != null) {
            TextButton(onClick = { runCatching { context.startActivity(samsungHealth) } }) {
                Text(stringResource(Res.string.health_open_samsung))
            }
        }
    }
}
