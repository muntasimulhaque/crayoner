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
import io.github.muntasimulhaque.crayoner.core.PageView
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The child's own marks, in wax, on top of the printed picture.
 *
 * A crayon is not a brush, and a mark is not a painted line: it is wax, the
 * same wax every colored area in the book is made of, laid down by the tip
 * the child is holding. So a mark is one pass of that wax, at that width,
 * and that is all of it.
 *
 * There used to be a second, narrower pass of the same wax down the middle,
 * standing in for the pressure of the hand. It was wrong twice over. A real
 * drag of a crayon does not come out paler at its edges and darker in its
 * core: it comes out one band of pigment, broken by the paper's tooth, and
 * the pale edge the second pass left behind is what made a mark read as
 * marker ink rather than as wax. And two translucent passes of one color do
 * not average to that color, they multiply, so the middle of every mark came
 * out almost pure and the grain was pushed out of the one place it was most
 * needed.
 *
 * Nothing is drawn around a mark. A paler edge was tried and it was wrong:
 * wax dragged over paper does not lay a light halo of itself beside the
 * stroke, it simply stops, sometimes with the tooth of the paper left bare.
 * A wide faint pass reads as a border, and a border reads as a sticker, so
 * the mark's edge is the wax's own broken edge and nothing else.
 *
 * An eraser mark is drawn with the printed page itself as its paint, paper
 * and print together: the rubber takes the wax off and leaves the paper with
 * the line that was printed on it, which is what happens on real paper,
 * where the print is under the wax and never made of it.
 *
 * [side] is the size of the frame the marks are drawn in, and [view] is the
 * piece of the paper that frame is showing, so a mark is exactly as wide and
 * exactly as far apart at any closeness: the same wax, looked at from
 * further away or nearer.
 */
fun DrawScope.drawStrokes(
    strokes: List<WaxStroke>,
    side: Float,
    view: PageView = PageView.Whole,
    print: Brush? = null,
) {
    if (strokes.isEmpty()) return
    val zoom = view.scale.toFloat()
    val tip = side * CRAYON_TIP_FRACTION * zoom
    val eraser = side * ERASER_TIP_FRACTION * zoom
    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        if (stroke.erase) {
            if (print != null) drawEraser(stroke, side, view, eraser, print)
            continue
        }
        // The wax itself, and nothing over it: one drag of one crayon, of
        // the crayon's own color, in the same grain every colored area in
        // the book carries.
        val wax: Brush = waxStroke(stroke.color) ?: SolidColor(Color(stroke.color))
        if (stroke.points.size == 1) {
            // The dot a pressed and lifted crayon leaves: one round of wax,
            // the width of the tip.
            drawCircle(wax, radius = tip * 0.5f, center = at(stroke.points[0], view, side))
            continue
        }
        drawPath(inView(stroke.points, view, side), wax, style = tipStroke(tip))
    }
}

/** One eraser mark: the printed page, laid back down along the rubber. */
private fun DrawScope.drawEraser(
    stroke: WaxStroke,
    side: Float,
    view: PageView,
    width: Float,
    print: Brush,
) {
    if (stroke.points.size == 1) {
        drawCircle(
            print,
            radius = width * 0.5f,
            center = at(stroke.points[0], view, side),
        )
        return
    }
    drawPath(inView(stroke.points, view, side), print, style = tipStroke(width))
}

internal fun tipStroke(width: Float) = Stroke(
    width = width.coerceAtLeast(1f),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

/** One point of page units, in the frame the child is looking at. */
internal fun at(p: Vec2, view: PageView, side: Float): Offset {
    val w = view.inWindow(p)
    return Offset((w.x * side).toFloat(), (w.y * side).toFloat())
}

/** One point of page units, in the page's own space. */
internal fun pagePoint(p: Vec2, side: Float): Offset =
    Offset((p.x * side).toFloat(), (p.y * side).toFloat())

/** One polyline of page units, in the frame the child is looking at. */
internal fun inView(points: List<Vec2>, view: PageView, side: Float): Path = Path().apply {
    if (points.isEmpty()) return@apply
    val first = at(points[0], view, side)
    moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val o = at(points[i], view, side)
        lineTo(o.x, o.y)
    }
}

/** One polyline of page units, in the page's own space. */
internal fun pathOfPoints(points: List<Vec2>, side: Float): Path = Path().apply {
    if (points.isEmpty()) return@apply
    val first = pagePoint(points[0], side)
    moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val o = pagePoint(points[i], side)
        lineTo(o.x, o.y)
    }
}
