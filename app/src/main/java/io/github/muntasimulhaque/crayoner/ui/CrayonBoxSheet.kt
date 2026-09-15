package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons

/**
 * The crayon box itself: thirty two real crayons, laid out the way the box
 * lays them out. It opens over the page rather than sitting under it
 * forever, because a tray of thirty two crayons on a phone costs the sheet
 * the room it needs to be colored on.
 *
 * A tap outside puts the box away, and so does a pull down: the lid is the
 * one thing in the app that answers a swipe, because a box held over the
 * page is exactly the thing a hand tries to push away.
 */
@Composable
fun CrayonBoxSheet(
    selected: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appear = remember { Animatable(0f) }
    val hint = stringResource(R.string.close_box)
    // How far the lid has been pulled down, in pixels.
    var pull by remember { mutableFloatStateOf(0f) }
    val dismissAt = with(LocalDensity.current) { DISMISS_PULL.roundToPx().toFloat() }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(190, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrayonerColors.Scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { dy ->
                    pull = (pull + dy).coerceAtLeast(0f)
                },
                onDragStopped = { velocity ->
                    if (pull > dismissAt || velocity > FLING_AWAY) {
                        onDismiss()
                    } else {
                        animate(
                            initialValue = pull,
                            targetValue = 0f,
                            initialVelocity = 0f,
                            animationSpec = tween(180),
                        ) { value, _ -> pull = value }
                        pull = 0f
                    }
                },
            )
            .semantics { contentDescription = hint },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val a = appear.value
                    alpha = a
                    scaleX = 0.92f + 0.08f * a
                    scaleY = 0.92f + 0.08f * a
                    translationY = pull
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .buttonShadow(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), elevation = 12.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(CrayonerColors.Cardboard)
                // The cardboard swallows its own taps, so putting a finger
                // down on the box is not the same as putting the box away.
                // A pull down still reaches the lid above.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 620.dp -> 8
                    maxWidth >= 460.dp -> 6
                    maxWidth >= 340.dp -> 5
                    else -> 4
                }
                val gaps = 8.dp
                val cell = (maxWidth - gaps * (columns - 1)) / columns
                val rows = Crayons.all.chunked(columns)
                Column(
                    verticalArrangement = Arrangement.spacedBy(gaps),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (row in rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gaps)) {
                            for (argb in row) {
                                ColorSeat(
                                    argb = argb,
                                    selected = argb == selected,
                                    onClick = { onPick(argb) },
                                    cell = cell,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** How far the lid must be pulled down before it lets go of the page. */
private val DISMISS_PULL = 64.dp

/** Or how fast it must be thrown down, in pixels a second. */
private const val FLING_AWAY = 1200f
