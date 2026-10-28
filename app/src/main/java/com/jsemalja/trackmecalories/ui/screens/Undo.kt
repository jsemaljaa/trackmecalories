package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.jsemalja.trackmecalories.resources.*

/**
 * The app's one "Deleted … Undo" snackbar. Deletes happen at once and can be taken back
 * for a few seconds, instead of asking "are you sure?" first. It lives at the app level,
 * so the snackbar outlives the dialog or row that triggered it.
 */
class UndoController(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
    private val undoLabel: String
) {
    /** Shows [message] with an Undo action, replacing any snackbar already up. */
    fun offer(message: String, onUndo: () -> Unit) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(message = message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

val LocalUndo = staticCompositionLocalOf<UndoController> { error("No UndoController provided") }

@Composable
fun rememberUndoController(): UndoController {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(Res.string.undo)
    return remember(undoLabel) { UndoController(hostState, scope, undoLabel) }
}
