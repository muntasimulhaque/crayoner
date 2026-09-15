package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.AppIcon
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The app's marks, drawn from the geometry in [AppIcon].
 *
 * There is no drawing here to speak of: every mark's shape lives in :core, as
 * a list of points, and this file only walks those points onto a canvas. That
 * is the same arrangement the pictures use, and for the same reason: a mark
 * that is described in one place can be measured by a test, drawn by the
 * review sheets, and read by a person, and it cannot come out different in
 * two places.
 *
 * Every mark is [IconSize] and every line is [AppIcon.LINE] of the mark's own
 * box, so the bar and the capsule are one row of controls in two places
 * rather than two sets of buttons that happen to be on one screen.
 */
@Composable
fun MarkIcon(
    name: AppIcon.Name,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = IconSize,
) {
    Canvas(modifier = modifier.size(size)) {
        drawMark(name, color)
    }
}

/** The house: back to the pictures. */
@Composable
fun HomeIcon(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) =
    MarkIcon(AppIcon.Name.HOME, color, modifier, size)

/** The sound switch, in the state it is in: waves out, or crossed through. */
@Composable
fun SoundIcon(modifier: Modifier = Modifier, on: Boolean, color: Color, size: Dp = IconSize) =
    MarkIcon(if (on) AppIcon.Name.SOUND_ON else AppIcon.Name.SOUND_OFF, color, modifier, size)

/** The step back: one mark comes off the paper. */
@Composable
fun UndoGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) =
    MarkIcon(AppIcon.Name.UNDO, color, modifier, size)

/** The step forward: the mark the step back took off comes back. */
@Composable
fun RedoGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) =
    MarkIcon(AppIcon.Name.REDO, color, modifier, size)

/** The rubber, lying on the desk the way a real one does. */
@Composable
fun EraserGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) =
    MarkIcon(AppIcon.Name.ERASER, color, modifier, size)

/** Keep this picture: the tick on the save question's first answer. */
@Composable
fun TickGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) =
    MarkIcon(AppIcon.Name.TICK, color, modifier, size)

/** Start it fresh: the cross on the save question's second answer. */
@Composable
fun CrossGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) =
    MarkIcon(AppIcon.Name.CROSS, color, modifier, size)

/** Paints one mark of the app into this scope, at one weight. */
internal fun DrawScope.drawMark(name: AppIcon.Name, color: Color) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val stroke = Stroke(width = (w * AppIcon.LINE).toFloat().coerceAtLeast(1.4f))
    for (piece in AppIcon.pieces(name)) {
        // A mark is a line the app drew: two points at the very least.
        if (piece.points.size < 2) continue
        val path = pathOf(piece.points, w, h, piece.closed)
        if (piece.fill) drawPath(path, color) else drawPath(path, color, style = stroke)
    }
}

/** One mark's points as a path in this scope's own box. */
private fun pathOf(points: List<Vec2>, w: Float, h: Float, closed: Boolean): Path = Path().apply {
    moveTo((points[0].x * w).toFloat(), (points[0].y * h).toFloat())
    for (i in 1 until points.size) {
        lineTo((points[i].x * w).toFloat(), (points[i].y * h).toFloat())
    }
    if (closed) close()
}
