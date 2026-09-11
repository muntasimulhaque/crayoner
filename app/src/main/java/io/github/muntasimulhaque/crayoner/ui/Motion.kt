package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The app's one-shot clocks. Every flourish that runs once and stops reads
 * one of these, so nothing needs a timer of its own and no redraw can leave
 * a sparkle stranded on the page.
 */

/**
 * A value that runs from one to zero once, whenever [stamp] changes. Every
 * one shot flourish in the app reads one of these, so nothing needs a clock
 * of its own and a redraw can never leave a sparkle stranded.
 */
@Composable
internal fun fadeAfter(stamp: Long): Float {
    var value by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(stamp) {
        if (stamp == 0L) {
            value = 0f
            return@LaunchedEffect
        }
        val anim = Animatable(1f)
        anim.animateTo(0f, tween(FEEDBACK_MS, easing = LinearEasing)) {
            value = this.value
        }
        value = 0f
    }
    return value
}

internal const val FEEDBACK_MS = 560

/**
 * The small rise a page makes the first time it appears. One slow run, then
 * still forever: a page that breathes while a child is working would be a
 * distraction, not a welcome.
 */
@Composable
internal fun rememberSettle(pageId: String): Float {
    var value by remember(pageId) { mutableFloatStateOf(0f) }
    LaunchedEffect(pageId) {
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(SETTLE_MS, easing = FastOutSlowInEasing)) {
            value = this.value
        }
    }
    return value
}

internal const val SETTLE_MS = 420

