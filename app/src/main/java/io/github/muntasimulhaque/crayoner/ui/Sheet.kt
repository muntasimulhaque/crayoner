package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * paper that the child's own hand did not put there, and nothing is ever
 * taken away except by the rubber.
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
    // The finished marks are handed to the canvas as one flattened layer,
    // and the mark still under the finger is the only thing drawn live, so
    // the finger's own frame costs the same whether the page carries three
    // marks or three hundred.
    val live = state.live
    Box(
        modifier = modifier.size(side + SHEET_TAPE_REACH * 2),
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
                strokes = state.progress.strokes,
                generation = state.stamp,
                live = live,
                sidePx = sidePx,
                modifier = Modifier.fillMaxSize(),
            )
            // The screen reader's view of the page: one focusable target per
            // area, named for what it is. Semantics only, so a finger passes
            // straight through to the sheet.
            PageSemantics(
                page = state.page,
                crayon = state.crayon,
                onColor = { index -> onColorArea(index) },
                modifier = Modifier.fillMaxSize(),
            )
            // A stamped picture wears its stamp where a stamp goes: on the
            // paper, in the corner, over the work. It is the child's own seal,
            // and it lands when they press it, which is the only moment the
            // app ever marks a picture as done.
            if (state.sealed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(side * 0.04f)
                        .size(side * 0.20f),
                ) {
                    WaxSeal(
                        modifier = Modifier.fillMaxSize(),
                        pressStamp = state.sealStamp,
                    )
                }
            }
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

