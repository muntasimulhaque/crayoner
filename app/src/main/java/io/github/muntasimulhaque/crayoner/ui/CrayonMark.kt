package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The child's own marks, in wax, on top of the printed picture.
 *
 * A crayon is not a brush, and a mark is not a painted line: it is wax, the
 * same wax every colored area in the book is made of. So the body of a mark
 * is drawn with the wax surface itself ([waxStroke]), which leaves the
 * paper's tooth breaking through it, and a narrower pass of the same wax
 * down the middle where the hand pressed hardest. The feather at the edges
 * is where the wax ran out into the tooth; the grain goes down between the
 * body and the core, so the mark is made of the same material as the sample
 * the child is copying.
 *
 * An eraser mark is drawn with the printed page itself as its paint, paper
 * and print together: the rubber takes the wax off and leaves the paper with
 * the line that was printed on it, which is what happens on real paper,
 * where the print is under the wax and never made of it.
 */
fun DrawScope.drawStrokes(strokes: List<WaxStroke>, side: Float, print: Brush? = null) {
    if (strokes.isEmpty()) return
    val tip = side * CRAYON_TIP_FRACTION
    val eraser = side * ERASER_TIP_FRACTION
    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        if (stroke.erase) {
            if (print != null) drawEraser(stroke, side, eraser, print)
            continue
        }
        val color = Color(stroke.color)
        // The wax itself, and a second tile of the same wax for the pass the
        // hand went back over: two tiles, so the strokes of the two passes
        // cross instead of lining up into one glossier band.
        val wax: Brush = waxStroke(stroke.color, 0) ?: SolidColor(color)
        val waxAgain: Brush = waxStroke(stroke.color, 1) ?: SolidColor(color)
        if (stroke.points.size == 1) {
            val p = stroke.points[0]
            val c = Offset((p.x * side).toFloat(), (p.y * side).toFloat())
            drawCircle(color.copy(alpha = 0.20f), radius = tip * 0.78f, center = c)
            drawCircle(wax, radius = tip * 0.60f, center = c)
            drawCircle(waxAgain, radius = tip * 0.34f, center = c)
            continue
        }
        val path = pathOfPoints(stroke.points, side)
        // The feather where the wax ran out into the paper's tooth: the same
        // color, wider and faint, never a gray halo.
        drawPath(path, color.copy(alpha = 0.20f), style = tipStroke(tip * 1.34f))
        drawPath(path, wax, style = tipStroke(tip))
        drawPath(path, waxAgain, style = tipStroke(tip * 0.74f))
    }
}

/** One eraser mark: the printed page, laid back down along the rubber. */
private fun DrawScope.drawEraser(stroke: WaxStroke, side: Float, width: Float, print: Brush) {
    if (stroke.points.size == 1) {
        val p = stroke.points[0]
        drawCircle(
            print,
            radius = width * 0.5f,
            center = Offset((p.x * side).toFloat(), (p.y * side).toFloat()),
        )
        return
    }
    drawPath(pathOfPoints(stroke.points, side), print, style = tipStroke(width))
}

internal fun tipStroke(width: Float) = Stroke(
    width = width.coerceAtLeast(1f),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

/** One polyline of page units, in pixels. */
internal fun pathOfPoints(points: List<Vec2>, side: Float): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo((points[0].x * side).toFloat(), (points[0].y * side).toFloat())
    for (i in 1 until points.size) {
        lineTo((points[i].x * side).toFloat(), (points[i].y * side).toFloat())
    }
}
