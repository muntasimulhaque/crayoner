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
import io.github.muntasimulhaque.crayoner.core.Wax
import io.github.muntasimulhaque.crayoner.core.WaxGrain
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.TexturePaint
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * The Java2D half of the one renderer: the same shapes, the same paint
 * order, the same wax and the same ink line as the device draws, so a review
 * sheet in build/sheets is the picture the app will show and not an artist's
 * impression of it.
 *
 * A colored area is made the way the device makes it: the passes a hand
 * would make, from the same geometry core produces, with the paper's tooth
 * over the whole area. A later area's wax covers an earlier area's line,
 * which is exactly how hidden edges disappear in a real coloring book.
 */
object RenderKit {

    /** Screen-space shapes are built at this many points per unit edge. */
    private const val CURVE_STEPS = 64

    /** The line weight, as a fraction of the page side, on the device. */
    const val STROKE_FRACTION = 0.0072

    /**
     * Draws the page the way a real coloring book prints it: each area is
     * colored in wax, then its own outline is drawn, and only then is the
     * next area colored. Interleaving is the whole trick: a later area's wax
     * covers an earlier area's line, so hidden edges disappear on their own.
     * Coloring every area first and drawing every line after would print the
     * skeleton of the picture through its own colors, which is exactly the
     * bug this order exists to prevent.
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
            val argb = fills[index]
            if (argb != null) drawWaxFill(g, region, unionArea(region, side), side, argb, grain)
            if (strokes && !page.isGround(index)) {
                g.color = Color(Crayons.INK.toInt(), true)
                g.stroke = stroke
                g.draw(unionPath(region, side))
            }
        }
    }

    /** One area's wax, clipped to the area's own union of shapes. */
    private fun drawWaxFill(
        g: Graphics2D,
        region: Region,
        clip: java.awt.Shape,
        side: Double,
        argb: Long,
        grain: Boolean,
    ) {
        val saved = g.clip
        g.clip(clip)
        val color = Color(argb.toInt(), true)
        // The wax itself, as a surface: the crayon's own color at the alpha
        // the wax laid down, with the paper's tooth and the drag of the hand
        // in it.
        val surface = waxSurface(region, argb)
        g.paint = TexturePaint(
            surface,
            Rectangle2D.Double(0.0, 0.0, surface.width.toDouble(), surface.height.toDouble()),
        )
        g.fill(clip)
        for (pass in Wax.passes(region)) {
            val path = Path2D.Double()
            val points = pass.points
            path.moveTo(points[0].x * side, points[0].y * side)
            for (i in 1 until points.size) path.lineTo(points[i].x * side, points[i].y * side)
            g.color = Color(color.red, color.green, color.blue, (pass.alpha * 255).toInt())
            g.stroke = BasicStroke(
                (pass.width * side).toFloat().coerceAtLeast(1f),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
            )
            g.draw(path)
        }
        if (grain) {
            val tile = grainTexture(side)
            val paint = g.paint
            g.paint = TexturePaint(
                tile,
                Rectangle2D.Double(0.0, 0.0, tile.width.toDouble(), tile.height.toDouble()),
            )
            g.fill(clip)
            g.paint = paint
        }
        g.clip = saved
    }

    /** One region's own area, at the page's own size, for clipping wax. */
    /**
     * An area's wax surface as a tile, keyed by area and color so a picture
     * drawn twice shares one.
     */
    private fun waxSurface(region: Region, argb: Long): BufferedImage =
        waxTiles.getOrPut("${region.id}|$argb") {
            val pixels = Wax.surface(
                argb = argb,
                size = WAX_TILE,
                angleDeg = Wax.angleDeg(region),
                phase = 0,
                seed = Wax.seed(region),
            )
            BufferedImage(WAX_TILE, WAX_TILE, BufferedImage.TYPE_INT_ARGB).apply {
                setRGB(0, 0, WAX_TILE, WAX_TILE, pixels, 0, WAX_TILE)
            }
        }

    private val waxTiles = HashMap<String, BufferedImage>()

    /** The wax tile's side, in pixels. Mirrors the device. */
    private const val WAX_TILE = 96

    private fun unionArea(region: Region, side: Double): Area {
        val area = Area()
        for (part in region.parts) area.add(Area(awtShape(part, side)))
        return area
    }

    private fun grainTexture(side: Double): BufferedImage = grainTiles.getOrPut(1) {
        val pixels = WaxGrain.pixels()
        val tile = BufferedImage(WaxGrain.SIZE, WaxGrain.SIZE, BufferedImage.TYPE_INT_ARGB)
        tile.setRGB(0, 0, WaxGrain.SIZE, WaxGrain.SIZE, pixels, 0, WaxGrain.SIZE)
        tile
    }

    private val grainTiles = HashMap<Int, BufferedImage>()

    /**
     * The child's own marks, in wax, exactly as the device draws them: the
     * same passes, the same grain tile, the same tip as a fraction of the
     * page. The store art can then show a page a child really colored, and
     * not an artist's impression of one.
     */
    fun renderStrokes(g: Graphics2D, strokes: List<Stroke>, side: Double) {
        drawStrokes(g, strokes, side, print = null)
    }

    /**
     * The marks, with the printed page available as the rubber's own paint:
     * an eraser mark lays the print back down over the wax, the way a real
     * rubber uncovers the line that was under it.
     */
    fun drawStrokes(g: Graphics2D, strokes: List<Stroke>, side: Double, print: BufferedImage?) {
        if (strokes.isEmpty()) return
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val tip = side * CRAYON_TIP_FRACTION
        val eraser = side * ERASER_TIP_FRACTION
        val tile = grainTexture(side)
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            if (stroke.erase) {
                if (print != null) drawEraser(g, stroke, side, eraser, print)
                continue
            }
            val color = Color(stroke.color.toInt(), true)
            val path = Path2D.Double()
            val first = stroke.points[0]
            path.moveTo(first.x * side, first.y * side)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x * side, stroke.points[i].y * side)
            }
            if (stroke.points.size == 1) {
                val r = tip * 0.52
                g.color = Color(color.red, color.green, color.blue, (0.70 * 255).toInt())
                g.fill(Ellipse2D.Double(first.x * side - r, first.y * side - r, r * 2, r * 2))
                continue
            }
            // The mark is wax, the same wax an area is colored with: the
            // feather where it ran into the tooth, then two passes of wax,
            // the second narrower, which is the hand going back over its own
            // line. Nothing here is gray: a crayon never lays a film of gray
            // over its own color.
            val waxSurface = markWax(stroke.color, 0)
            val waxAgain = markWax(stroke.color, 1)
            fun pass(width: Double, paint: java.awt.Paint) {
                g.paint = paint
                g.stroke = BasicStroke(
                    width.toFloat(),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                )
                g.draw(path)
            }
            val p = { image: BufferedImage ->
                TexturePaint(
                    image,
                    Rectangle2D.Double(0.0, 0.0, image.width.toDouble(), image.height.toDouble()),
                )
            }
            pass(tip * 1.34, Color(color.red, color.green, color.blue, (0.20 * 255).toInt()))
            pass(tip, p(waxSurface))
            pass(tip * 0.74, p(waxAgain))
        }
    }

    private fun drawEraser(
        g: Graphics2D,
        stroke: Stroke,
        side: Double,
        width: Double,
        print: BufferedImage,
    ) {
        val path = Path2D.Double()
        val first = stroke.points[0]
        path.moveTo(first.x * side, first.y * side)
        for (i in 1 until stroke.points.size) path.lineTo(stroke.points[i].x * side, stroke.points[i].y * side)
        val paint = TexturePaint(
            print,
            Rectangle2D.Double(0.0, 0.0, print.width.toDouble(), print.height.toDouble()),
        )
        g.paint = paint
        if (stroke.points.size == 1) {
            val r = width / 2.0
            g.fill(Ellipse2D.Double(first.x * side - r, first.y * side - r, r * 2, r * 2))
            return
        }
        g.stroke = BasicStroke(
            width.toFloat().coerceAtLeast(1f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        g.draw(path)
    }

    /**
     * The wax of the child's own marks: the same surface an area is colored
     * with, keyed by color alone, because a mark belongs to the hand and not
     * to any area of the picture.
     */
    private fun markWax(argb: Long, pass: Int): BufferedImage =
        markWaxes.getOrPut(argb * 2 + pass) {
            val pixels = Wax.surface(argb, WAX_TILE, -24.0 + pass * 26.0, 0, 0x5A17 + pass * 6161, fine = true)
            BufferedImage(WAX_TILE, WAX_TILE, BufferedImage.TYPE_INT_ARGB).apply {
                setRGB(0, 0, WAX_TILE, WAX_TILE, pixels, 0, WAX_TILE)
            }
        }

    private val markWaxes = HashMap<Long, BufferedImage>()

    /** The crayon tip, as a fraction of the page side. Mirrors the device. */
    const val CRAYON_TIP_FRACTION = 0.030

    /** The rubber, as a fraction of the page side. Mirrors the device. */
    const val ERASER_TIP_FRACTION = 0.044

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
        is Circ -> Ellipse2D.Double(
            (shape.c.x - shape.r) * side, (shape.c.y - shape.r) * side,
            shape.r * 2 * side, shape.r * 2 * side,
        )
        is Ell -> java.awt.geom.AffineTransform.getRotateInstance(
            Math.toRadians(shape.angleDeg), shape.c.x * side, shape.c.y * side,
        ).createTransformedShape(
            Ellipse2D.Double(
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
