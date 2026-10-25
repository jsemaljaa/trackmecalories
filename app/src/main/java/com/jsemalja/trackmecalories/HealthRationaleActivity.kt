package com.jsemalja.trackmecalories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.theme.AppearancePrefs
import com.jsemalja.trackmecalories.ui.theme.TrackmecaloriesTheme

/**
 * Shown by Health Connect when the user asks why we want their data (it won't show the
 * permission screen without one): what is read, what for, and that it stays on the phone.
 */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appearancePrefs = AppearancePrefs(applicationContext)
        enableEdgeToEdge()
        setContent {
            val appearance by appearancePrefs.appearance.collectAsState()
            TrackmecaloriesTheme(appearance = appearance) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.health_rationale_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.health_rationale_body), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.health_rationale_privacy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { finish() }) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}
