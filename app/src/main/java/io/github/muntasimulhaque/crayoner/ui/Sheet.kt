package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.core.pagePointOf
import io.github.muntasimulhaque.crayoner.host.Screen

/** The shape of a real sheet of paper: only the least softening so the
 *  corners do not alias. */
val PaperShape = RoundedCornerShape(2.dp)

/**
 * The sheet: one sheet of paper, sized to the room it is given, with the
 * child's own picture printed on it.
 *
 * It is paper, not a card: real corners, the way a sheet torn from a pad has
 * them, and the soft shadow of one lying on a desk. It is taller than it is
 * wide ([Page.ASPECT]), the way a page of a coloring pad is, so the picture
 * gets the whole of a phone's screen instead of a square with two bands of
 * empty desk above and below it.
 *
 * A finger on the paper draws, always. Dragging on the paper is how a child
 * colors, and no gesture on the picture may ever take that away: there is no
 * pinch and no double tap anywhere on the sheet, because a hand rests on the
 * page while it colors. A page opens whole and stays whole.
 *
 * A finger's position is read in the paper's own units through
 * [pagePointOf], so the mark lands exactly where the fingertip is. The sheet
 * is taller than it is wide, which means the frame's height is *not* the
 * number to divide y by: page units are isotropic, and both axes are
 * measured by the sheet's width.
 */
@Composable
internal fun SheetOf(
    state: Screen.Coloring,
    live: State<WaxStroke?>?,
    onStrokeStart: (Vec2) -> Unit,
    onStrokeMove: (Vec2) -> Unit,
    onStrokeEnd: () -> Unit,
    onColorArea: (Int) -> Unit,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val height = width * Page.ASPECT.toFloat()
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val heightPx = with(LocalDensity.current) { height.roundToPx() }
    Box(
        modifier = modifier.size(width = width, height = height),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .buttonShadow(PaperShape, elevation = 8.dp)
                .clip(PaperShape)
                .background(CrayonerColors.Card)
                .clipToBounds()
                // The pointer input reads its own frame's size on every
                // event rather than once when the gesture starts: the mark's
                // geometry and the finger's coordinates come from the same
                // box at the same moment, to the pixel, so nothing that
                // resizes the sheet (a fold, a split screen, a rotation)
                // can pull the wax away from the fingertip.
                .pointerInput(state.page.id) {
                    fun at(offset: Offset): Vec2? {
                        val w = size.width.toDouble()
                        if (w <= 0.0) return null
                        return pagePointOf(offset.x.toDouble(), offset.y.toDouble(), w)
                    }
                    awaitEachGesture {
                        // A gesture can be taken away in the middle of itself:
                        // the screen turns over, a phone call arrives, the
                        // sheet is taken off the desk. Whatever the hand had
                        // already put down is finished rather than dropped,
                        // because work the hand has done is never thrown
                        // away, and no half mark is left hanging on the
                        // paper either. On a gesture that ends the way it
                        // should, this is a second knock on a door that is
                        // already closed.
                        try {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            at(down.position)?.let(onStrokeStart)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                // Every point the system batched into this
                                // event, oldest first: a quick flick arrives
                                // as one event carrying the whole line, and a
                                // mark that only ever saw the last point of
                                // it would cut the corner the hand drew.
                                for (past in change.historical) {
                                    at(past.position)?.let(onStrokeMove)
                                }
                                if (change.pressed) {
                                    at(change.position)?.let(onStrokeMove)
                                    change.consume()
                                } else {
                                    // The point the finger lifted at is the
                                    // last point of the line it drew. Without
                                    // it a quick flick ends a finger's width
                                    // short of where the hand really stopped.
                                    at(change.position)?.let(onStrokeMove)
                                    onStrokeEnd()
                                    break
                                }
                            }
                            onStrokeEnd()
                            // A second finger must not leave a mark hanging:
                            // wait for every pointer to lift before the next
                            // mark.
                            while (eventPressed()) {
                                awaitPointerEvent()
                            }
                        } finally {
                            onStrokeEnd()
                        }
                    }
                },
        ) {
            PageCanvas(
                page = state.page,
                fills = emptyMap(),
                strokes = state.progress.strokes,
                generation = state.marks,
                live = live?.let { mark -> { mark.value } },
                widthPx = widthPx,
                heightPx = heightPx,
                modifier = Modifier.fillMaxSize(),
            )
            // The screen reader's view of the page: one focusable target per
            // area, named for what it is and what color the book prints it in.
            PageSemantics(
                page = state.page,
                crayon = state.crayon,
                onColor = { index -> onColorArea(index) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The sheet, sized to the space its parent gives it: as wide as it can be,
 * and as tall as the page's own proportion needs, whichever runs out first.
 * Nothing is ever squeezed: the paper keeps its own shape and the desk takes
 * the leftover.
 */
@Composable
internal fun Sheet(
    state: Screen.Coloring,
    live: State<WaxStroke?>?,
    onStrokeStart: (Vec2) -> Unit,
    onStrokeMove: (Vec2) -> Unit,
    onStrokeEnd: () -> Unit,
    onColorArea: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The paper's own proportion, in both directions: a sheet as wide as
        // the room and as tall as its shape needs, capped so it still fits
        // when the room is the short side.
        val byWidth = maxWidth
        val byHeight = maxHeight / Page.ASPECT.toFloat()
        val width = minOf(byWidth, byHeight).coerceAtLeast(SHEET_MIN)
        SheetOf(
            state = state,
            live = live,
            onStrokeStart = onStrokeStart,
            onStrokeMove = onStrokeMove,
            onStrokeEnd = onStrokeEnd,
            onColorArea = onColorArea,
            width = width,
        )
    }
}

/** The smallest a sheet is ever drawn, so a tiny window still has paper. */
private val SHEET_MIN = 96.dp

/** True while any pointer on the screen is still down. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.eventPressed(): Boolean =
    currentEvent.changes.any { it.pressed }
