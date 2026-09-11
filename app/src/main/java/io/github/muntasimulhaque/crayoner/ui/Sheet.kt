package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen

/** The shape of a real sheet of paper: square, with only the least
 *  softening so the corners do not alias. */
val PaperShape = RoundedCornerShape(2.dp)

/**
 * The sheet: one square of paper, sized to the room it is given.
 *
 * It is paper, not a card: real corners, square, the way a sheet torn from a
 * pad has them, and a strip of tape over each corner holding it to the desk.
 * The tape reaches past the paper onto the desk on purpose: it is what tells
 * the eye that this is a sheet lying on a table and not a panel in a layout,
 * and it is the same roll the pictures on the shelf are hung with.
 *
 * The sheet owns the one gesture in the app: a finger down starts a mark, a
 * finger moving draws it, a finger up finishes it. Nothing appears on the
 * paper that the child's own hand did not put there.
 *
 * The answers a mark can give are drawn over the page: a warm pulse and a
 * sparkle for each area the picture asked for, a soft ink ring for each area
 * it did not. None of them block the next mark, so a child who colors at
 * speed is never held up by praise.
 */
@Composable
internal fun SheetOf(
    state: Screen.Coloring,
    onStrokeStart: (Vec2) -> Unit,
    onStrokeMove: (Vec2) -> Unit,
    onStrokeEnd: () -> Unit,
    onColorArea: (Int) -> Unit,
    side: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sidePx = with(density) { side.roundToPx() }
    val tapeWidth = with(density) { SHEET_TAPE_WIDTH.roundToPx() }.toFloat()
    val tapeHeight = with(density) { SHEET_TAPE_HEIGHT.roundToPx() }.toFloat()
    val rightFade = fadeAfter(state.rightAt)
    val wrongFade = fadeAfter(state.wrongAt)
    val settle = rememberSettle(state.page.id)
    // The mark under the finger right now, and every finished mark. They are
    // drawn together so the live one is always the topmost, exactly as it
    // will look the moment the finger lifts.
    val strokes = if (state.live != null) {
        state.progress.strokes + state.live
    } else {
        state.progress.strokes
    }
    Box(
        modifier = modifier
            .size(side + SHEET_TAPE_REACH * 2)
            .graphicsLayer {
                scaleX = 0.96f + 0.04f * settle
                scaleY = 0.96f + 0.04f * settle
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(side)
                .buttonShadow(PaperShape, elevation = 8.dp)
                .clip(PaperShape)
                .background(CrayonerColors.Card)
                .pointerInput(state.page.id, sidePx) {
                    if (sidePx <= 0) return@pointerInput
                    val s = sidePx.toFloat()
                    fun at(offset: Offset) = Vec2(
                        (offset.x / s).toDouble().coerceIn(0.0, 1.0),
                        (offset.y / s).toDouble().coerceIn(0.0, 1.0),
                    )
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onStrokeStart(at(down.position))
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) break
                            if (change.pressed) {
                                onStrokeMove(at(change.position))
                                change.consume()
                            } else {
                                onStrokeEnd()
                                break
                            }
                        }
                        onStrokeEnd()
                        // A second finger must not leave a mark hanging: wait
                        // for every pointer to lift before the next mark.
                        while (eventPressed()) {
                            awaitPointerEvent()
                        }
                    }
                },
        ) {
            PageCanvas(
                page = state.page,
                fills = emptyMap(),
                strokes = strokes,
                sidePx = sidePx,
                modifier = Modifier.fillMaxSize(),
                overlay = { geometry ->
                    drawPaintedFeedback(
                        page = state.page,
                        geometry = geometry,
                        rightIndices = state.rightIndices,
                        rightFade = rightFade,
                        wrongIndices = state.wrongIndices,
                        wrongFade = wrongFade,
                        strokePx = size.width * STROKE_FRACTION,
                    )
                },
            )
            // The screen reader's view of the page: one focusable target per
            // area, naming what it is and what color it wants. Semantics
            // only, so a finger passes straight through to the sheet.
            PageSemantics(
                page = state.page,
                reached = state.reached,
                crayon = state.crayon,
                onColor = { index -> onColorArea(index) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // The tape, laid over the paper's own corners and a little past them
        // onto the desk. The canvas is exactly the paper's size, so a corner
        // is always a corner, whatever the screen the sheet lands on.
        Canvas(modifier = Modifier.size(side).align(Alignment.Center)) {
            val spots = listOf(
                Offset(0f, 0f) to -45f,
                Offset(size.width, 0f) to 45f,
                Offset(0f, size.height) to 45f,
                Offset(size.width, size.height) to -45f,
            )
            for ((center, angle) in spots) {
                drawTape(center, tapeWidth, tapeHeight, angle)
            }
        }
    }
}

/** True while any pointer on the screen is still down. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.eventPressed(): Boolean =
    currentEvent.changes.any { it.pressed }

/** The sheet, sized to the space its parent gives it. */
@Composable
internal fun Sheet(
    state: Screen.Coloring,
    onStrokeStart: (Vec2) -> Unit,
    onStrokeMove: (Vec2) -> Unit,
    onStrokeEnd: () -> Unit,
    onColorArea: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side: Dp = (minOf(maxWidth, maxHeight) - SHEET_TAPE_REACH * 2)
            .coerceAtLeast(80.dp)
        SheetOf(
            state = state,
            onStrokeStart = onStrokeStart,
            onStrokeMove = onStrokeMove,
            onStrokeEnd = onStrokeEnd,
            onColorArea = onColorArea,
            side = side,
        )
    }
}

/**
 * The answers a mark can give, drawn over the page: a warm sparkle on every
 * area the color was right for, and a soft ink ring on every area it was not.
 * Both fade on their own and neither blocks the next mark.
 */
internal fun DrawScope.drawPaintedFeedback(
    page: Page,
    geometry: PageGeometry,
    rightIndices: Set<Int>,
    rightFade: Float,
    wrongIndices: Set<Int>,
    wrongFade: Float,
    strokePx: Float,
) {
    if (rightFade > 0f) {
        for (index in rightIndices) {
            val argb = page.region(index)?.fillArgb ?: continue
            pulseRegion(
                geometry = geometry,
                index = index,
                color = Color(argb),
                alpha = 0.60f * rightFade,
                width = strokePx * (2.4f + 2.2f * (1f - rightFade)),
            )
            // The sparkle sits at the area's own center, taken from the shape
            // data rather than from a path's bounds: a path's bounds are the
            // bounds of its control points, which for a curve or a ring sits
            // far from where the area actually looks centered.
            val center = page.region(index)?.centroid ?: continue
            drawSparkle(
                Offset(
                    (center.x * size.width).toFloat(),
                    (center.y * size.width).toFloat(),
                ),
                rightFade,
            )
        }
    }
    if (wrongFade > 0f) {
        for (index in wrongIndices) {
            pulseRegion(
                geometry = geometry,
                index = index,
                color = CrayonerColors.Ink,
                alpha = 0.26f * wrongFade,
                width = strokePx * (1.9f + 1.6f * (1f - wrongFade)),
            )
        }
    }
}

/** A four pointed sparkle, the quiet little cheer for a color well placed. */
internal fun DrawScope.drawSparkle(center: Offset, fade: Float) {
    if (center == Offset.Unspecified) return
    val r = size.minDimension * 0.055f * (0.45f + 0.55f * fade)
    val alpha = (fade * 0.95f).coerceIn(0f, 1f)
    val color = CrayonerColors.Honey.copy(alpha = alpha)
    val spark = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.26f, center.y - r * 0.26f)
        lineTo(center.x + r, center.y)
        lineTo(center.x + r * 0.26f, center.y + r * 0.26f)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r * 0.26f, center.y + r * 0.26f)
        lineTo(center.x - r, center.y)
        lineTo(center.x - r * 0.26f, center.y - r * 0.26f)
        close()
    }
    drawPath(spark, color)
}

/** How much of the sheet has felt the crayon, as one quiet line, for parents. */
@Composable
internal fun ProgressLine(
    colored: Int,
    total: Int,
    accent: Long,
    modifier: Modifier = Modifier,
) {
    val label = androidx.compose.ui.res.stringResource(
        io.github.muntasimulhaque.crayoner.R.string.page_progress,
        colored,
        total,
    )
    val fraction = if (total <= 0) 0f else colored.toFloat() / total.toFloat()
    val color = Color(accent)
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(mix(color, CrayonerColors.Card, 0.82f))
            .semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}
