package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
 * The sheet: one square of paper, sized to the room it is given, with the
 * child's own picture printed on it.
 *
 * It is paper, not a card: real corners, square, the way a sheet torn from a
 * pad has them, and the soft shadow of one lying on a desk. There is no tape
 * at its corners any more, because the sheet is not held down at one fixed
 * size: the child can bring the paper closer (see [ZoomStrip]), and tape
 * that stayed the same size while the paper grew would be the one thing on
 * screen that gave the illusion away.
 *
 * The sheet owns the one gesture in the app: a finger down starts a mark, a
 * finger moving draws it, a finger up finishes it. There is deliberately no
 * pinch and no double tap here. A pinch is two fingers on a page a small
 * hand is coloring, which is a mistake that ruins the mark under it, and a
 * double tap is what a child making two dots in the same place looks like;
 * neither may move the paper. What brings the paper closer is the chip on
 * the desk below, which is a deliberate act and cannot happen by accident.
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
    val sidePx = with(LocalDensity.current) { side.roundToPx() }
    // The finished marks are handed to the canvas as one flattened layer,
    // and the mark still under the finger is the only thing drawn live, so
    // the finger's own frame costs the same whether the page carries three
    // marks or three hundred.
    val view = state.view
    Box(
        modifier = modifier.size(side),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(side)
                .buttonShadow(PaperShape, elevation = 8.dp)
                .clip(PaperShape)
                .background(CrayonerColors.Card)
                // A closer look is the paper drawn bigger, so only the picture
                // grows: the sheet's own frame never moves and the paper is
                // clipped to it, which is what makes a closer look a window on
                // the sheet rather than a bigger sheet on a smaller desk.
                .clipToBounds()
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
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
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
            // The window is not a transform applied to a finished picture:
            // PageCanvas draws the page itself through [view], at this frame's
            // own resolution, so a closer look is a sharper look and the paper
            // is never enlarged from a smaller drawing.
            PageCanvas(
                page = state.page,
                fills = emptyMap(),
                strokes = state.progress.strokes,
                generation = state.marks,
                live = state.live,
                view = view,
                sidePx = sidePx,
                modifier = Modifier.fillMaxSize(),
            )
            // The screen reader's view of the page: one focusable target per
            // area, named for what it is. Area targets are placed in page
            // units and scaled by the window, so the target a reader lands on
            // is the part of the picture the child would touch.
            PageSemantics(
                page = state.page,
                crayon = state.crayon,
                view = view,
                onColor = { index -> onColorArea(index) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

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
        val side: Dp = minOf(maxWidth, maxHeight).coerceAtLeast(SHEET_MIN)
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

/** The smallest a sheet is ever drawn, so a tiny window still has paper. */
private val SHEET_MIN = 96.dp

/** True while any pointer on the screen is still down. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.eventPressed(): Boolean =
    currentEvent.changes.any { it.pressed }
