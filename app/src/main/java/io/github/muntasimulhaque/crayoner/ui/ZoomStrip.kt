package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.PageView

/**
 * How much of the paper is on screen, as one more thing lying on the desk.
 *
 * A slider is a control, and this app has controls in exactly one place, so
 * the strip is drawn as an object instead: a groove cut in a chip of paper
 * with a crayon end to push along it. The left end of the chip is marked
 * with the whole sheet, the right end with a closer look, and where the knob
 * sits is the answer.
 *
 * A page opens with the knob at the left, and one press anywhere on the chip
 * puts the paper back down whole, which is the one thing a child needs after
 * a look they did not mean to take.
 *
 * The chip is laid out, not drawn: the ends are their own little sheets with
 * real padding around them, and the groove is the space between, so nothing
 * overlaps anything and no part of it can be clipped by the desk's edge.
 *
 * There is no pinch anywhere in the app (see [SheetOf]): a hand rests on the
 * paper while it colors, and a two-finger gesture on a page a three year old
 * is drawing on is exactly the mistake that ruins the mark under it.
 */
@Composable
fun ZoomStrip(
    view: PageView,
    onZoom: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(if (view.isWhole) R.string.zoom_whole else R.string.zoom_close)
    val at = fractionOf(view)
    Box(
        modifier = modifier
            .height(CHIP_HEIGHT)
            .buttonShadow(RoundedCornerShape(CHIP_ROUND), elevation = 4.dp)
            .clip(RoundedCornerShape(CHIP_ROUND))
            .background(CrayonerColors.Card)
            .clickable(role = Role.Button, onClick = onReset)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(at, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SheetEnd(whole = true)
            // The groove, with the knob on it: the one thing on the chip a
            // child moves, and the only part that takes a drag.
            Groove(
                at = at,
                onZoom = onZoom,
                modifier = Modifier.width(GROOVE_WIDTH).fillMaxHeight(),
            )
            SheetEnd(whole = false)
        }
    }
}

/** How big the chip is, and how round its own corners are. */
private val CHIP_HEIGHT = 52.dp
private val CHIP_ROUND = 20.dp

/** How long the groove is, and how big the knob that travels along it. */
private val GROOVE_WIDTH = 132.dp
private val KNOB_RADIUS_PX = 15.dp

/** How much of the closest look the window is showing, from 0 to 1. */
private fun fractionOf(view: PageView): Float =
    ((view.scale - PageView.WHOLE_ZOOM) / (PageView.MAX_ZOOM - PageView.WHOLE_ZOOM))
        .coerceIn(0.0, 1.0)
        .toFloat()

/**
 * One end of the strip: a little sheet of paper saying what that end gives.
 * The whole sheet is a thin outline, because that is the rest state a page
 * opens at; the closer look is a sheet drawn bigger and filled, because that
 * is the state the child has moved to. A child who cannot read still sees
 * which end makes the picture bigger.
 */
@Composable
private fun SheetEnd(whole: Boolean) {
    val size = if (whole) 20.dp else 26.dp
    Canvas(Modifier.size(size)) {
        val side = size.toPx()
        val line = (side * 0.085f).coerceAtLeast(1f)
        val ink = CrayonerColors.Ink.copy(alpha = if (whole) 0.32f else 0.70f)
        // Half the stroke sits outside the path, so the sheet is inset by
        // that much and its own edge is never clipped by the canvas.
        val inset = line / 2f
        val sheet = side - line
        drawRect(
            color = ink,
            topLeft = Offset(inset, inset),
            size = Size(sheet, sheet),
            style = Stroke(line, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
    }
}

/**
 * The groove and the knob: a straight track with one round chip of paper to
 * push along it, wearing the brand's own coral so it is the one thing on the
 * chip a child looks at.
 */
@Composable
private fun Groove(
    at: Float,
    onZoom: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.pointerInput(Unit) { dragToZoom(onZoom) }) {
        val knob = KNOB_RADIUS_PX.toPx()
        val middle = size.height / 2f
        val left = knob
        val right = size.width - knob
        if (right <= left) return@Canvas
        drawLine(
            CrayonerColors.TapeEdge,
            Offset(left, middle),
            Offset(right, middle),
            strokeWidth = (knob * 0.14f).coerceAtLeast(1f),
            cap = StrokeCap.Round,
        )
        val x = left + (right - left) * at
        drawCircle(CrayonerColors.Card, radius = knob, center = Offset(x, middle))
        drawCircle(
            CrayonerColors.Ink.copy(alpha = 0.30f),
            radius = knob,
            center = Offset(x, middle),
            style = Stroke((knob * 0.12f).coerceAtLeast(1f)),
        )
        drawCircle(CrayonerColors.Coral, radius = knob * 0.42f, center = Offset(x, middle))
    }
}

/** Moves the knob along the groove, and nowhere else. */
private suspend fun PointerInputScope.dragToZoom(onZoom: (Double) -> Unit) {
    detectHorizontalDragGestures { change, _ ->
        val knob = KNOB_RADIUS_PX.toPx()
        val left = knob
        val right = size.width - knob
        if (right <= left) return@detectHorizontalDragGestures
        val fraction = ((change.position.x - left) / (right - left)).coerceIn(0f, 1f)
        change.consume()
        onZoom(PageView.WHOLE_ZOOM + (PageView.MAX_ZOOM - PageView.WHOLE_ZOOM) * fraction)
    }
}
