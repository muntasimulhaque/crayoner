package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one strip of tape in the app.
 *
 * It is a single material everywhere, the way real tape off a roll is: a
 * warm translucent paper with a faint fiber running through it, a little
 * wider than it is deep, and never quite square. The shelf uses it to hold
 * pictures to the wall; the coloring sheet uses it to hold paper to the
 * desk. One drawing, so the same roll was used on every page of the book.
 */
internal fun DrawScope.drawTape(
    center: Offset,
    width: Float,
    height: Float,
    angleDeg: Float,
    alpha: Float = 1f,
    color: Color = CrayonerColors.Tape,
    fiber: Color = CrayonerColors.TapeFiber,
) {
    if (width <= 0f || height <= 0f) return
    rotate(angleDeg, pivot = center) {
        val halfW = width / 2f
        val halfH = height / 2f
        val left = center.x - halfW
        val top = center.y - halfH
        // The strip, with a shallow nick cut into each short edge, so it was
        // torn off the roll by hand rather than stamped out.
        val strip = Path().apply {
            moveTo(left, top)
            lineTo(left + width * 0.04f, top + halfH)
            lineTo(left, top + height)
            lineTo(left + width, top + height)
            lineTo(left + width * 0.96f, top + halfH)
            lineTo(left + width, top)
            close()
        }
        drawPath(strip, color.copy(alpha = color.alpha * alpha))
        // Its fibers: two fine lines along the roll's direction, a whisper
        // darker, so the strip reads as paper rather than as a painted bar.
        clipPath(strip) {
            drawLine(
                fiber.copy(alpha = fiber.alpha * alpha),
                Offset(left, top + height * 0.34f),
                Offset(left + width, top + height * 0.28f),
                strokeWidth = (height * 0.07f).coerceAtLeast(0.5f),
            )
            drawLine(
                fiber.copy(alpha = fiber.alpha * alpha),
                Offset(left, top + height * 0.72f),
                Offset(left + width, top + height * 0.66f),
                strokeWidth = (height * 0.05f).coerceAtLeast(0.5f),
            )
        }
    }
}

/** The strip of tape on a picture hung on the wall: one size, tilted. */
internal fun DrawScope.drawWallTape(width: Float, height: Float) {
    drawTape(
        center = Offset(width / 2f, height / 2f),
        width = width,
        height = height,
        angleDeg = -2.5f,
    )
}

/** How wide the strip of tape on the coloring sheet is. */
internal val SHEET_TAPE_WIDTH = 62.dp
internal val SHEET_TAPE_HEIGHT = 19.dp

/** How far the tape reaches beyond the paper's own corner, onto the desk. */
internal val SHEET_TAPE_REACH = 22.dp
