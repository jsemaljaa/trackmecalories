package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource

/** The app's languages; [SYSTEM] follows the phone's. */
internal enum class AppLanguage(val tag: String, val label: StringResource) {
    SYSTEM("", Res.string.language_system),
    ENGLISH("en", Res.string.language_english),
    RUSSIAN("ru", Res.string.language_russian)
}

/**
 * The in-app language, where the platform has one (see rememberAppLanguage in Platform.kt).
 */
internal class LanguageState(initial: AppLanguage, private val apply: (AppLanguage) -> Unit) {
    var current by mutableStateOf(initial)
        private set

    fun choose(language: AppLanguage) {
        current = language
        apply(language)
    }
}

@Composable
internal fun LanguageDialog(language: LanguageState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    val selected = language.current == option
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton) {
                                onDismiss()
                                if (!selected) language.choose(option)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
