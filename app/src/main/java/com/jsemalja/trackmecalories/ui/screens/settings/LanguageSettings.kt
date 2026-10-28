package com.jsemalja.trackmecalories.ui.screens.settings

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * In-app language (Android 13+ per-app language; the same setting appears in the system's
 * app info).
 */
internal class LanguageState(initial: AppLanguage, private val apply: (AppLanguage) -> Unit) {
    var current by mutableStateOf(initial)
        private set

    fun choose(language: AppLanguage) {
        current = language
        apply(language)
    }
}

/** Null before Android 13, which has no per-app language. */
@Composable
internal fun rememberAppLanguage(): LanguageState? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val context = LocalContext.current
    return remember {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        // Set from the system's app info it can carry a region ("ru-RU"), so only the language is compared.
        val tag = localeManager.applicationLocales.get(0)?.language
        LanguageState(AppLanguage.entries.firstOrNull { it.tag.isNotEmpty() && it.tag == tag } ?: AppLanguage.SYSTEM) { language ->
            // The activity restarts in the new language; the open tab is kept.
            localeManager.applicationLocales =
                if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
        }
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
