package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Page
import kotlin.math.roundToInt

/**
 * The sample held up: the finished picture, laid down exactly over the page
 * the child is coloring, at the page's own size and in the page's own place.
 *
 * It used to be a plate of paper floating in the middle of the screen with
 * the picture and its name on it, and on a phone the picture inside that
 * plate came out at half the size of the sheet the child was working on: the
 * thing held up to copy was smaller than the thing being copied. A sample
 * belongs where the work is. Lying over the page, one tap away and one tap
 * back, it can be compared part by part without the eye hunting for the
 * shape it is looking at, and the flip between outlines and colors reads as
 * what it is: the same sheet, colored.
 *
 * There is no name on it and no mount around it, for the same reason the
 * sheet under it carries neither: the picture is the thing. The page behind
 * is dimmed but never hidden, so the child keeps their place, and the sample
 * arrives with the small settle of a sheet being laid down.
 *
 * The rectangle is the working sheet's own, measured from the layout rather
 * than computed again here, so the two can never drift apart by a pixel, on
 * any screen, in any shape.
 */
@Composable
fun SamplePeek(
    page: Page,
    bounds: Rect,
    onDismiss: () -> Unit,
) {
    val hint = stringResource(R.string.hide_sample)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .background(CrayonerColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .semantics { contentDescription = hint },
    ) {
        val size = sampleSizeOf(bounds)
        if (size.width <= 0 || size.height <= 0) return@Box
        val density = LocalDensity.current
        val widthDp = with(density) { size.width.toDp() }
        val heightDp = with(density) { size.height.toDp() }
        Box(
            modifier = Modifier
                .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                .size(width = widthDp, height = heightDp)
                .graphicsLayer {
                    val a = appear.value
                    alpha = a
                    scaleX = 0.97f + 0.03f * a
                    scaleY = 0.97f + 0.03f * a
                }
                // A sheet lying on top of a sheet still has to be told apart
                // from it, and a shadow is how paper on paper says so.
                .buttonShadow(PaperShape, elevation = 12.dp)
                .clip(PaperShape)
                .background(CrayonerColors.Card),
        ) {
            PageCanvas(
                page = page,
                fills = remember(page) { sampleFills(page) },
                widthPx = size.width,
                heightPx = size.height,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The exact pixel size the sample is drawn at, from the working sheet's own
 * measured rectangle. One function, so the picture the page opens the sample
 * with and the picture the tap asks for can never be two different sizes:
 * a prewarm that misses its size is a whole finished picture drawn on the
 * main thread at the moment a child is waiting to look at it.
 */
internal fun sampleSizeOf(bounds: Rect): IntSize =
    IntSize(bounds.width.roundToInt(), bounds.height.roundToInt())
