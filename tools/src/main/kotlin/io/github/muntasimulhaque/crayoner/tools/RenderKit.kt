package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.ArcBand
import io.github.muntasimulhaque.crayoner.core.Circ
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Ell
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Poly
import io.github.muntasimulhaque.crayoner.core.RRect
import io.github.muntasimulhaque.crayoner.core.Region
import io.github.muntasimulhaque.crayoner.core.Shape
import io.github.muntasimulhaque.crayoner.core.Stroke
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.RoundRectangle2D

/**
 * The Java2D half of the one renderer: the same shapes, the same paint
 * order, the same ink line as the device draws, so a review sheet in
 * build/sheets is the picture the app will show and not an artist's
 * impression of it.
 *
 * The paint order is the whole trick behind the line art: for every region
 * in order, fill it (its color, or paper when it is not colored yet), then
 * stroke the outline of its union. A later region's fill covers an earlier
 * region's line, which is exactly how hidden edges disappear in a real
 * coloring book.
 */
object RenderKit {

    /** Screen-space shapes are built at this many points per unit edge. */
    private const val CURVE_STEPS = 64

    /** The line weight, as a fraction of the page side, on the device. */
    const val STROKE_FRACTION = 0.0072

    /**
     * Draws the page the way a real coloring book prints it: each area is
     * filled, then its own outline is drawn, and only then is the next area
     * filled. Interleaving is the whole trick: a later fill covers an
     * earlier area's line, so hidden edges disappear on their own. Drawing
     * every fill first and every line after would print the skeleton of the
     * picture through its own colors, which is exactly the bug this order
     * exists to prevent.
     *
     * The ground (region zero) is never outlined: it is the paper's own
     * edge, and a coloring page has no border around the sheet.
     */
    fun renderPage(
        g: Graphics2D,
        page: Page,
        side: Double,
        fills: Map<Int, Long>,
        strokes: Boolean = true,
        grain: Boolean = true,
    ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = Color(Crayons.PAPER.toInt(), true)
        g.fillRect(0, 0, side.toInt() + 1, side.toInt() + 1)
        val stroke = BasicStroke(
            (side * STROKE_FRACTION).toFloat().coerceAtLeast(1.0f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        for ((index, region) in page.regions.withIndex()) {
            val argb = fills[index] ?: Crayons.PAPER
            g.color = Color(argb.toInt(), true)
            for (part in region.parts) g.fill(awtShape(part, side))
            if (grain && fills[index] != null) {
                // The wax grain, exactly the tile the device uses, painted
                // through the same shape, so the store art and the screen
                // show the same paper.
                for (part in region.parts) {
                    val paint = g.paint
                    val tile = grainTexture(g, side)
                    g.paint = java.awt.TexturePaint(
                        tile,
                        java.awt.geom.Rectangle2D.Double(
                            0.0, 0.0, tile.width.toDouble(), tile.height.toDouble(),
                        ),
                    )
                    g.fill(awtShape(part, side))
                    g.paint = paint
                }
            }
            if (strokes && !page.isGround(index)) {
                g.color = Color(Crayons.INK.toInt(), true)
                g.stroke = stroke
                g.draw(unionPath(region, side))
            }
        }
    }

    private fun grainTexture(g: Graphics2D, side: Double): java.awt.image.BufferedImage {
        // The tile is built once per page size: the anchor rectangle only
        // decides where the tile's phase starts, and one page uses one phase
        // so the grain runs continuously across neighboring areas.
        return grainTiles.getOrPut(side.toInt()) {
            val pixels = io.github.muntasimulhaque.crayoner.core.WaxGrain.pixels()
            val tile = java.awt.image.BufferedImage(
                io.github.muntasimulhaque.crayoner.core.WaxGrain.SIZE,
                io.github.muntasimulhaque.crayoner.core.WaxGrain.SIZE,
                java.awt.image.BufferedImage.TYPE_INT_ARGB,
            )
            tile.setRGB(
                0, 0, io.github.muntasimulhaque.crayoner.core.WaxGrain.SIZE,
                io.github.muntasimulhaque.crayoner.core.WaxGrain.SIZE,
                pixels, 0, io.github.muntasimulhaque.crayoner.core.WaxGrain.SIZE,
            )
            tile
        }
    }

    private val grainTiles = HashMap<Int, java.awt.image.BufferedImage>()

    /**
     * The child's own marks, in wax, exactly as the device draws them: the
     * same three passes, the same grain tile, the same tip as a fraction of
     * the page. The store art can then show a page a child really colored,
     * and not an artist's impression of one.
     */
    fun renderStrokes(g: Graphics2D, strokes: List<Stroke>, side: Double) {
        if (strokes.isEmpty()) return
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val tip = side * CRAYON_TIP_FRACTION
        val tile = grainTexture(g, side)
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            val color = Color(stroke.color.toInt(), true)
            val path = Path2D.Double()
            val first = stroke.points[0]
            path.moveTo(first.x * side, first.y * side)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x * side, stroke.points[i].y * side)
            }
            if (stroke.points.size == 1) {
                val r = tip * 0.52
                g.color = Color(color.red, color.green, color.blue, 210)
                g.fill(java.awt.geom.Ellipse2D.Double(
                    first.x * side - r, first.y * side - r, r * 2, r * 2,
                ))
                continue
            }
            fun pass(width: Double, alpha: Int, paint: java.awt.Paint) {
                g.paint = paint
                g.stroke = BasicStroke(
                    width.toFloat(),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                )
                g.draw(path)
            }
            pass(tip * 1.20, 66, Color(color.red, color.green, color.blue, 66))
            pass(tip, 189, Color(color.red, color.green, color.blue, 189))
            pass(
                tip * 1.02,
                255,
                java.awt.TexturePaint(
                    tile,
                    java.awt.geom.Rectangle2D.Double(
                        0.0, 0.0, tile.width.toDouble(), tile.height.toDouble(),
                    ),
                ),
            )
            pass(tip * 0.58, 199, Color(color.red, color.green, color.blue, 199))
        }
    }

    /** The crayon tip, as a fraction of the page side. Mirrors the device. */
    const val CRAYON_TIP_FRACTION = 0.052

    /** The picture's own colors, the way the sample card shows it. */
    fun sampleFills(page: Page): Map<Int, Long> =
        page.regions.indices.associateWith { page.regions[it].fillArgb }

    /** One region's outline: the union of its parts, so inner seams vanish. */
    fun unionPath(region: Region, side: Double): Path2D {
        val area = Area()
        for (part in region.parts) area.add(Area(awtShape(part, side)))
        return Path2D.Double(area)
    }

    fun awtShape(shape: Shape, side: Double): java.awt.Shape = when (shape) {
        is Circ -> java.awt.geom.Ellipse2D.Double(
            (shape.c.x - shape.r) * side, (shape.c.y - shape.r) * side,
            shape.r * 2 * side, shape.r * 2 * side,
        )
        is Ell -> java.awt.geom.AffineTransform.getRotateInstance(
            Math.toRadians(shape.angleDeg), shape.c.x * side, shape.c.y * side,
        ).createTransformedShape(
            java.awt.geom.Ellipse2D.Double(
                (shape.c.x - shape.rx) * side, (shape.c.y - shape.ry) * side,
                shape.rx * 2 * side, shape.ry * 2 * side,
            ),
        )
        is RRect -> java.awt.geom.AffineTransform.getRotateInstance(
            Math.toRadians(shape.angleDeg),
            (shape.x + shape.w / 2) * side,
            (shape.y + shape.h / 2) * side,
        ).createTransformedShape(
            RoundRectangle2D.Double(
                shape.x * side, shape.y * side, shape.w * side, shape.h * side,
                shape.radius * 2 * side, shape.radius * 2 * side,
            ),
        )
        is ArcBand -> arcBandPath(shape, side)
        is Poly -> awtPath(shape, side)
    }

    fun awtPath(poly: Poly, side: Double): Path2D {
        val path = Path2D.Double()
        val points = poly.points
        if (points.isEmpty()) return path
        path.moveTo(points[0].x * side, points[0].y * side)
        for (i in 1 until points.size) path.lineTo(points[i].x * side, points[i].y * side)
        path.closePath()
        return path
    }

    private fun arcBandPath(band: ArcBand, side: Double): Path2D {
        val path = Path2D.Double()
        val steps = CURVE_STEPS
        val start = Math.toRadians(band.startDeg)
        val span = Math.toRadians(band.sweepDegrees())
        val cx = band.c.x * side
        val cy = band.c.y * side
        val outer = band.rOuter * side
        val inner = band.rInner * side
        path.moveTo(cx + inner * Math.cos(start), cy + inner * Math.sin(start))
        path.lineTo(cx + outer * Math.cos(start), cy + outer * Math.sin(start))
        for (i in 1..steps) {
            val a = start + span * i / steps
            path.lineTo(cx + outer * Math.cos(a), cy + outer * Math.sin(a))
        }
        for (i in steps downTo 0) {
            val a = start + span * i / steps
            path.lineTo(cx + inner * Math.cos(a), cy + inner * Math.sin(a))
        }
        path.closePath()
        return path
    }

    /** True when the union has real area, used by the sheet's sanity strip. */
    fun unionArea(path: Path2D): Double {
        var area = 0.0
        val it = path.getPathIterator(null)
        val coords = DoubleArray(6)
        var startX = 0.0
        var startY = 0.0
        var lastX = 0.0
        var lastY = 0.0
        while (!it.isDone) {
            when (it.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> {
                    startX = coords[0]; startY = coords[1]
                    lastX = startX; lastY = startY
                }
                PathIterator.SEG_LINETO -> {
                    area += lastX * coords[1] - coords[0] * lastY
                    lastX = coords[0]; lastY = coords[1]
                }
                PathIterator.SEG_CLOSE -> {
                    area += lastX * startY - startX * lastY
                    lastX = startX; lastY = startY
                }
                else -> Unit
            }
            it.next()
        }
        return kotlin.math.abs(area) / 2.0
    }
}
