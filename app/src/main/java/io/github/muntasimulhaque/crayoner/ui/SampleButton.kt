package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Page

/**
 * The picture to copy, as one more round button in the bar.
 *
 * It is the same size, the same shape and the same shadow as every other
 * button on the screen, with the finished picture inside it instead of an
 * icon: the picture is the whole lesson of the app, and it should be the
 * most inviting thing to press. One tap holds it up big.
 *
 * On a fresh page it breathes, three slow swells and then stillness, because
 * a three year old cannot read a label that explains it and should not have
 * to guess.
 */
@Composable
fun SampleButton(
    page: Page,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = CoinSize,
    announce: Boolean = false,
) {
    val label = stringResource(R.string.show_sample)
    val breath = if (announce) rememberBreath(stamp = page.id.hashCode().toLong()) else 0f
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = 1f + 0.06f * breath
                scaleY = 1f + 0.06f * breath
            }
            .buttonShadow(CircleShape)
            .clip(CircleShape)
            .background(CrayonerColors.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        // The whole picture, mounted in the round button like a picture in a
        // round frame. It is inset well inside the coin, and it is the
        // sheet's *height* that is fitted to that inset: a page is taller
        // than it is wide, so fitting by width would push its top and bottom
        // corners out past the rim of the coin holding it.
        val height = size * PICTURE_IN_SET
        val width = height / Page.ASPECT.toFloat()
        Box(modifier = Modifier.size(width = width, height = height)) {
            PageCanvas(
                page = page,
                fills = remember(page) { sampleFills(page) },
                widthPx = with(LocalDensity.current) { width.roundToPx() },
                heightPx = with(LocalDensity.current) { height.roundToPx() },
                // The button never makes the child wait for it: its picture is
                // rendered off the main thread, and until it lands the coin
                // shows the picture's own outlines, which at this size is the
                // whole of the picture anyway.
                blocking = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** How much of the round sample button the picture itself fills. */
private const val PICTURE_IN_SET = 0.64f

/**
 * A once-in, three breath pulse at page start: zero, then three slow
 * swells, then zero forever. Reading a stamp of zero means no animation at
 * all, which is how the screenshot harness and a resumed page stay still.
 */
@Composable
private fun rememberBreath(stamp: Long): Float {
    var value by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(stamp) {
        if (stamp == 0L) {
            value = 0f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = BREATH_MS, easing = FastOutSlowInEasing),
        ) {
            // Three sine swells across one run, so it starts and ends at rest.
            value = (kotlin.math.sin(this.value * 3f * Math.PI).toFloat()).coerceAtLeast(0f)
        }
        value = 0f
    }
    return value
}

private const val BREATH_MS = 2400
