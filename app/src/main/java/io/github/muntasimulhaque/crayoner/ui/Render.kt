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
import io.github.muntasimulhaque.crayoner.core.RRect
import io.github.muntasimulhaque.crayoner.core.Shape

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
 * A color arriving. [x] and [y] are the point in page units where the
 * crayon landed, and [progress] runs from zero to one: the color sweeps out
 * from under the finger like wet paint finding its edges, instead of
 * appearing all at once. Nothing about the finished page changes; this is
 * only how it gets there. [before] is the color the area wore a moment ago,
 * so coloring over a color sweeps just as clearly as coloring a bare one.
 */
data class PaintSweep(
    val index: Int,
    val x: Double,
    val y: Double,
    val progress: Float,
    val before: Long? = null,
)

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
 * [sweep] is the one area currently arriving under a finger; when it is
 * null, or when it has finished, every area draws the plain way.
 */
fun DrawScope.drawPage(
    page: Page,
    geometry: PageGeometry,
    fills: Map<Int, Long>,
    sweep: PaintSweep? = null,
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
        val color = Color(argb)
        val painted = fills[index] != null
        val revealing = sweep != null && sweep.index == index && sweep.progress < 1f
        if (revealing) {
            val s = sweep!!
            val b = page.regions[index].bounds
            val reach = maxOf(
                distance(s.x, s.y, b.x, b.y),
                distance(s.x, s.y, b.right, b.y),
                distance(s.x, s.y, b.x, b.bottom),
                distance(s.x, s.y, b.right, b.bottom),
            ) * size.width
            val radius = (reach * s.progress).toFloat().coerceAtLeast(0.5f)
            val clip = Path().apply {
                addOval(
                    Rect(
                        Offset((s.x * size.width).toFloat(), (s.y * size.width).toFloat()),
                        Size(radius * 2f, radius * 2f),
                    ),
                )
            }
            // The area is drawn twice: once as it was, then the new color
            // wiped in over it through a growing circle. Drawing it twice is
            // what lets a recolor sweep the same way a first color does.
            val before = sweep.before
            if (before != null) {
                for (path in geometry.parts[index]) drawPath(path, Color(before))
            }
            clipPath(clip) {
                for (path in geometry.parts[index]) drawPath(path, color)
                if (grain != null) {
                    // The wax grain rides in with the color, clipped to the
                    // same growing circle, so the arriving paint looks like
                    // wax from its first pixel.
                    for (path in geometry.parts[index]) {
                        clipPath(path) { drawRect(brush = grain) }
                    }
                }
            }
        } else {
            for (path in geometry.parts[index]) drawPath(path, color)
            if (grain != null && painted) {
                for (path in geometry.parts[index]) {
                    clipPath(path) { drawRect(brush = grain) }
                }
            }
        }
        if (!page.isGround(index)) {
            drawPath(geometry.outlines[index], ink, style = stroke)
        }
    }
}

private fun distance(x0: Double, y0: Double, x1: Double, y1: Double): Double {
    val dx = x1 - x0
    val dy = y1 - y0
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/** The one line weight, as a fraction of the page side. Mirrors RenderKit. */
const val STROKE_FRACTION = 0.0072f

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
 * area shows paper, which is exactly what a coloring page looks like.
 */
@Composable
fun PageCanvas(
    page: Page,
    fills: Map<Int, Long>,
    modifier: Modifier = Modifier,
    sidePx: Int,
    sweep: PaintSweep? = null,
    overlay: DrawScope.(PageGeometry) -> Unit = {},
) {
    val geometry = remember(page, sidePx) { PageGeometry(page, sidePx.toFloat()) }
    Canvas(modifier = modifier) {
        drawPage(page, geometry, fills, sweep)
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
