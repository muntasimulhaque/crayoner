package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * How much of the paper is on screen, and which part of it.
 *
 * A slider is a control, and this app has controls in exactly one place, so
 * the strip is drawn as an object instead: a little map of the sheet with the
 * part being looked at marked on it, and a groove with a knob to push along.
 *
 * The map is what makes a close look usable. A closer look at the middle of
 * the page cannot show its corners, so without a way to move the window,
 * bringing the paper closer would put half of every picture out of reach,
 * which is worse than not zooming at all. The map is the answer a child can
 * read without being told: the whole sheet is drawn small, the part on screen
 * is the darker square inside it, and a finger anywhere on the map puts that
 * part of the paper in the middle of the screen. Moving the paper is a
 * deliberate act on a thing that is not the paper, so it can never happen by
 * accident while a hand is coloring.
 *
 * The groove is the closeness itself: left is the whole sheet, right is twice
 * as close. The left end of the chip is marked with the whole sheet and the
 * right end with a closer look, and one press anywhere on the chip puts the
 * paper back down whole, which is the one thing a child needs after a look
 * they did not mean to take.
 *
 * There is no pinch anywhere in the app (see [SheetOf]): a hand rests on the
 * paper while it colors, and a two-finger gesture on a page a three year old
 * is drawing on is exactly the mistake that ruins the mark under it.
 */
@Composable
fun ZoomStrip(
    view: PageView,
    onZoom: (Double) -> Unit,
    onPan: (Vec2) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(if (view.isWhole) R.string.zoom_whole else R.string.zoom_close)
    val at = fractionOf(view)
    Box(
        modifier = modifier
            .height(CHIP_HEIGHT)
            .buttonShadow(RoundedCornerShape(CHIP_ROUND), elevation = 3.dp)
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
            modifier = Modifier.fillMaxHeight().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PageMap(
                view = view,
                onPan = onPan,
                modifier = Modifier.height(MAP_SIDE).aspectRatio(1f),
            )
            // The groove, with the knob on it: the one part of the chip that
            // answers a drag across it rather than a press.
            Groove(
                at = at,
                onZoom = onZoom,
                modifier = Modifier.width(GROOVE_WIDTH).fillMaxHeight(),
            )
        }
    }
}

/** How big the chip is, and how round its own corners are. */
private val CHIP_HEIGHT = 46.dp
private val CHIP_ROUND = 18.dp

/** How long the groove is, and how big the knob that travels along it. */
private val GROOVE_WIDTH = 108.dp
private val KNOB_RADIUS_PX = 13.dp

/** The little sheet on the chip, and the room its own frame takes. */
private val MAP_SIDE = 30.dp

/** How much of the closest look the window is showing, from 0 to 1. */
private fun fractionOf(view: PageView): Float =
    ((view.scale - PageView.WHOLE_ZOOM) / (PageView.MAX_ZOOM - PageView.WHOLE_ZOOM))
        .coerceIn(0.0, 1.0)
        .toFloat()

/**
 * The map: the whole sheet as a small square, with the part of it that is on
 * screen drawn as a darker square inside it.
 *
 * A tap or a drag anywhere on the map puts that part of the paper in the
 * middle of the screen, which is the simplest possible sentence about what
 * moving the paper means. When the whole sheet is already on screen there is
 * nothing to move and the map is a plain sheet: the window covers all of it.
 */
@Composable
private fun PageMap(
    view: PageView,
    onPan: (Vec2) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.move_page)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                // Both: a tap jumps the paper there, a drag follows the
                // finger. A child too small to aim gets the drag, and a
                // single press anywhere still does something sensible.
                mapGestures(onPan)
            },
    ) {
        val side = size.width
        val line = (side * 0.070f).coerceAtLeast(1f)
        // The sheet's own edge, drawn as the faint square the whole page is.
        drawRect(
            CrayonerColors.Ink.copy(alpha = 0.34f),
            topLeft = Offset(line / 2f, line / 2f),
            size = Size(side - line, side - line),
            style = Stroke(line, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
        // The part of it being looked at. `span` is a fraction of the page,
        // and the window's corner is a fraction too, so the darker square is
        // exactly the piece of the sheet the child has on screen.
        val span = view.span.toFloat()
        val left = view.left.toFloat()
        val top = view.top.toFloat()
        val inset = span * side
        if (span < 1f) {
            drawRect(
                CrayonerColors.Coral,
                topLeft = Offset(left * side, top * side),
                size = Size(inset, inset),
            )
        }
    }
}

/** The groove and the knob: a straight track with one round chip of paper to
 *  push along it, wearing the brand's own coral so it is the one thing on the
 *  chip a child looks at. */
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
    detectDragGestures { change, _ ->
        val knob = KNOB_RADIUS_PX.toPx()
        val left = knob
        val right = size.width - knob
        if (right <= left) return@detectDragGestures
        val fraction = ((change.position.x - left) / (right - left)).coerceIn(0f, 1f)
        change.consume()
        onZoom(PageView.WHOLE_ZOOM + (PageView.MAX_ZOOM - PageView.WHOLE_ZOOM) * fraction)
    }
}

/**
 * A press or a drag on the map, as a point of the page.
 *
 * Both gestures land in the same place, and both are clamped to the paper by
 * the window itself, so aiming past the edge of the map pins the view to that
 * edge rather than doing nothing.
 */
private suspend fun PointerInputScope.mapGestures(onPan: (Vec2) -> Unit) {
    detectTapGestures { offset ->
        onPan(pagePointAt(offset, size.width.toFloat(), size.height.toFloat()))
    }
    detectDragGestures { change, _ ->
        change.consume()
        onPan(pagePointAt(change.position, size.width.toFloat(), size.height.toFloat()))
    }
}

private fun pagePointAt(offset: Offset, width: Float, height: Float): Vec2 = Vec2(
    (offset.x / width.coerceAtLeast(1f)).toDouble().coerceIn(0.0, 1.0),
    (offset.y / height.coerceAtLeast(1f)).toDouble().coerceIn(0.0, 1.0),
)
