package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.ArcBand
import io.github.muntasimulhaque.crayoner.core.Circ
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Ell
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Poly
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.RRect
import io.github.muntasimulhaque.crayoner.core.Shape
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The device half of the one renderer. The picture line is drawn the way a
 * real coloring book prints it: each area is painted in its color, and its
 * outline is drawn on top, so a later area's fill covers an earlier area's
 * line and hidden edges vanish.
 *
 * Paths are built once per page and size, outside the draw lambda, because
 * a page is redrawn on every touch of a crayon and rebuilding eight union
 * paths sixty times a second would be work nobody asked for.
 */
class PageGeometry(page: Page, private val side: Float) {

    /** One path per area, the union of its parts, for the ink line. */
    val outlines: List<Path> = page.regions.map { region ->
        unionOf(region.parts, side.toDouble())
    }

    /** One path per part, for filling. */
    val parts: List<List<Path>> = page.regions.map { region ->
        region.parts.map { pathOf(it, side.toDouble()) }
    }
}

/**
 * Draws one page the way a real coloring book prints it: each area is
 * filled, then its own outline is drawn, and only then is the next area
 * filled. Interleaving is the whole trick: a later fill covers an earlier
 * area's line, so a picture's hidden edges disappear on their own. Filling
 * everything first and drawing every line after would print the whole
 * skeleton through the colors, which is the bug this order exists to
 * prevent.
 *
 * The ground (region zero) is never outlined: it is the paper's own edge,
 * and a coloring page has no border around the sheet.
 *
 * An unfilled area shows paper, so passing no fills at all draws the bare
 * line art the child colors on.
 */
fun DrawScope.drawPage(
    page: Page,
    geometry: PageGeometry,
    fills: Map<Int, Long>,
) {
    val ink = Color(Crayons.INK)
    val stroke = Stroke(
        width = (size.width * STROKE_FRACTION).coerceAtLeast(1.6f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    val grain = GrainBrush
    for ((index, _) in page.regions.withIndex()) {
        val argb = fills[index] ?: Crayons.PAPER
        for (path in geometry.parts[index]) drawPath(path, Color(argb))
        if (grain != null && fills[index] != null) {
            for (path in geometry.parts[index]) {
                clipPath(path) { drawRect(brush = grain) }
            }
        }
        if (!page.isGround(index)) {
            drawPath(geometry.outlines[index], ink, style = stroke)
        }
    }
}

/**
 * The child's own marks, in wax, on top of the printed picture.
 *
 * A crayon is not a brush: the wax is dragged across the tooth of the paper,
 * so a mark is softer at its edges than down its middle, and where the hand
 * moves fast it lays down less than where it lingers. Three concentric
 * passes of the same path give the edge, and the wax grain over the whole
 * mark gives the tooth: the same grain every colored area in the book
 * carries, so a mark the child makes is made of the same material as the
 * sample they are copying.
 *
 * The grain is drawn between the middle pass and the core, not over the
 * finished mark, so it reads as wax pressed into paper rather than as a
 * veil laid on top of the color.
 *
 * Marks are drawn after the line art, not under it, for the plain reason
 * that wax covers ink: a child who colors over a line really does color over
 * it, and the page they end up with is the page they made.
 */
fun DrawScope.drawStrokes(strokes: List<WaxStroke>, side: Float) {
    if (strokes.isEmpty()) return
    val grain = GrainBrush
    val tip = side * CRAYON_TIP_FRACTION
    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        val color = Color(stroke.color)
        if (stroke.points.size == 1) {
            val p = stroke.points[0]
            val c = Offset((p.x * side).toFloat(), (p.y * side).toFloat())
            drawCircle(color.copy(alpha = 0.30f), radius = tip * 0.70f, center = c)
            if (grain != null) {
                drawCircle(grain, radius = tip * 0.64f, center = c)
            }
            drawCircle(color.copy(alpha = 0.76f), radius = tip * 0.52f, center = c)
            drawCircle(color.copy(alpha = 0.80f), radius = tip * 0.40f, center = c)
            continue
        }
        val path = strokePath(stroke, side)
        // Thin at the edges, waxed down the middle: the halo is the wax
        // feathering into the paper's tooth, the core is where the hand
        // pressed hardest. A real crayon mark is not translucent paint; it
        // covers the paper it was pressed onto, and it is the grain that
        // keeps it from reading as poured color.
        drawPath(path, color.copy(alpha = 0.26f), style = tipStroke(tip * 1.20f))
        drawPath(path, color.copy(alpha = 0.74f), style = tipStroke(tip))
        if (grain != null) {
            drawPath(path, grain, style = tipStroke(tip * 1.02f))
        }
        drawPath(path, color.copy(alpha = 0.78f), style = tipStroke(tip * 0.58f))
    }
}

private fun tipStroke(width: Float) = Stroke(
    width = width.coerceAtLeast(1f),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

/** The centerline of one mark, in pixels. */
private fun strokePath(stroke: WaxStroke, side: Float): Path = Path().apply {
    val first = stroke.points[0]
    moveTo((first.x * side).toFloat(), (first.y * side).toFloat())
    for (i in 1 until stroke.points.size) {
        val p = stroke.points[i]
        lineTo((p.x * side).toFloat(), (p.y * side).toFloat())
    }
}

/** The one line weight, as a fraction of the page side. Mirrors RenderKit. */
const val STROKE_FRACTION = 0.0072f

/**
 * How wide one crayon mark is, as a fraction of the page side. A real crayon
 * tip is about five millimeters across on a page of about two hundred, and
 * this is a little past that: wide enough that a three year old's scribble
 * covers the paper, narrow enough that a sprinkle sixteen pixels wide can
 * still be colored on purpose.
 */
const val CRAYON_TIP_FRACTION = 0.052f

/** One shape as a path, in page units scaled to [side] pixels. */
fun pathOf(shape: Shape, side: Double): Path {
    fun px(v: Double) = (v * side).toFloat()
    return when (shape) {
        is Circ -> Path().apply {
            addOval(
                Rect(
                    Offset(px(shape.c.x - shape.r), px(shape.c.y - shape.r)),
                    Size(px(shape.r * 2), px(shape.r * 2)),
                ),
            )
        }
        is Ell -> {
            val path = Path().apply {
                addOval(
                    Rect(
                        Offset(px(shape.c.x - shape.rx), px(shape.c.y - shape.ry)),
                        Size(px(shape.rx * 2), px(shape.ry * 2)),
                    ),
                )
            }
            if (shape.angleDeg != 0.0) {
                path.transform(rotationMatrix(shape.angleDeg, px(shape.c.x), px(shape.c.y)))
            }
            path
        }
        is RRect -> {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            Offset(px(shape.x), px(shape.y)),
                            Size(px(shape.w), px(shape.h)),
                        ),
                        cornerRadius = CornerRadius(px(shape.radius)),
                    ),
                )
            }
            if (shape.angleDeg != 0.0) {
                path.transform(
                    rotationMatrix(
                        shape.angleDeg,
                        px(shape.x + shape.w / 2),
                        px(shape.y + shape.h / 2),
                    ),
                )
            }
            path
        }
        is ArcBand -> arcBandPath(shape, side)
        is Poly -> Path().apply {
            val points = shape.points
            if (points.isEmpty()) return@apply
            moveTo(px(points[0].x), px(points[0].y))
            for (i in 1 until points.size) lineTo(px(points[i].x), px(points[i].y))
            close()
        }
    }
}

private fun arcBandPath(band: ArcBand, side: Double): Path {
    fun px(v: Double) = (v * side).toFloat()
    val steps = 64
    val start = Math.toRadians(band.startDeg)
    val span = Math.toRadians(band.sweepDegrees())
    val cx = band.c.x
    val cy = band.c.y
    return Path().apply {
        fun ring(radius: Double, i: Int) {
            val a = start + span * i / steps
            lineTo(px(cx + radius * Math.cos(a)), px(cy + radius * Math.sin(a)))
        }
        moveTo(px(cx + band.rInner * Math.cos(start)), px(cy + band.rInner * Math.sin(start)))
        lineTo(px(cx + band.rOuter * Math.cos(start)), px(cy + band.rOuter * Math.sin(start)))
        for (i in 1..steps) ring(band.rOuter, i)
        for (i in steps downTo 0) ring(band.rInner, i)
        close()
    }
}

/** A rotation around a pivot, as a matrix Compose's Path.transform takes. */
private fun rotationMatrix(angleDeg: Double, pivotX: Float, pivotY: Float): Matrix = Matrix().apply {
    translate(pivotX, pivotY)
    rotateZ(angleDeg.toFloat())
    translate(-pivotX, -pivotY)
}

/** The union of several shapes, so inner seams never print. */
fun unionOf(shapes: List<Shape>, side: Double): Path {
    if (shapes.isEmpty()) return Path()
    var path = pathOf(shapes[0], side)
    for (i in 1 until shapes.size) {
        val next = Path()
        next.op(path, pathOf(shapes[i], side), PathOperation.Union)
        path = next
    }
    return path
}

/**
 * One page, drawn to fill the canvas. [fills] may be empty: an uncolored
 * area shows paper, which is exactly what a coloring page looks like. The
 * child's own marks ride on top of the printed line art, the way wax rides
 * on a printed page.
 */
@Composable
fun PageCanvas(
    page: Page,
    fills: Map<Int, Long>,
    modifier: Modifier = Modifier,
    sidePx: Int,
    strokes: List<WaxStroke> = emptyList(),
    overlay: DrawScope.(PageGeometry) -> Unit = {},
) {
    val geometry = remember(page, sidePx) { PageGeometry(page, sidePx.toFloat()) }
    Canvas(modifier = modifier) {
        drawPage(page, geometry, fills)
        drawStrokes(strokes, size.width)
        overlay(geometry)
    }
}

/** A region's outline painted over the page, for the gentle tap feedback. */
fun DrawScope.pulseRegion(geometry: PageGeometry, index: Int, color: Color, alpha: Float, width: Float) {
    val path = geometry.outlines.getOrNull(index) ?: return
    drawPath(
        path,
        color.copy(alpha = alpha.coerceIn(0f, 1f)),
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** A page's paper ground, used by the shelf cards before the first frame. */
val PageCorner = 22.dp
