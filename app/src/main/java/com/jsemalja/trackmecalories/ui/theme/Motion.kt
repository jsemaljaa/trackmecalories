package com.jsemalja.trackmecalories.ui.theme

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Material "fade through" between unrelated places (the nav tabs): the old screen fades
 * out quickly, then the new one fades in while settling from 96% scale. Like every Compose
 * transition it's interruptible: tapping another tab mid-way retargets from where it is.
 */
fun <S> AnimatedContentTransitionScope<S>.fadeThrough(): ContentTransform =
    (fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.96f))
        .togetherWith(fadeOut(tween(90)))

/** How far a back gesture has gone (0 = not in progress) and which edge it came from. */
class PredictiveBackState {
    var progress by mutableFloatStateOf(0f)
        internal set
    var fromLeftEdge by mutableStateOf(true)
        internal set
}

/**
 * Handles back with Android 14+'s predictive back gesture: [state] follows the finger so
 * the screen can preview leaving (see [predictiveBack]); letting go calls [onBack], and
 * cancelling springs the screen back. On older Android it's an ordinary back press.
 */
@Composable
fun rememberPredictiveBack(enabled: Boolean, onBack: () -> Unit): PredictiveBackState {
    val state = remember { PredictiveBackState() }
    val currentOnBack by rememberUpdatedState(onBack)
    PredictiveBackHandler(enabled) { events ->
        try {
            events.collect { event ->
                state.progress = event.progress
                state.fromLeftEdge = event.swipeEdge == BackEventCompat.EDGE_LEFT
            }
            currentOnBack()
            state.progress = 0f
        } catch (e: CancellationException) {
            withContext(NonCancellable) { animate(state.progress, 0f) { value, _ -> state.progress = value } }
            throw e
        }
    }
    return state
}

/**
 * Material's predictive back preview: while the gesture is held the screen shrinks to 90%,
 * drifts away from the edge being swiped and rounds its corners, so it's clear what back
 * will do before letting go.
 */
fun Modifier.predictiveBack(state: PredictiveBackState): Modifier = graphicsLayer {
    val progress = state.progress
    if (progress == 0f) return@graphicsLayer
    val scale = 1f - 0.1f * progress
    scaleX = scale
    scaleY = scale
    translationX = (if (state.fromLeftEdge) 1f else -1f) * 8.dp.toPx() * progress
    shape = RoundedCornerShape((32 * progress).dp)
    clip = true
}
