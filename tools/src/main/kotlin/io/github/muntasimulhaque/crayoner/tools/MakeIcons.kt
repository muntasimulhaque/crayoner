package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.Area
import io.github.muntasimulhaque.crayoner.core.CrayonInk
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.core.WaxGrain
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.TexturePaint
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * The mark is one sentence with no words in it: a crayon standing on its
 * point, and the band of color it has just laid down beside itself. A child
 * of three cannot read the word Crayoner, and a crayon on its own says
 * writing tool, not coloring book. A crayon whose own tip sits in a swath of
 * wax says the whole thing at a glance: this is the thing you draw with, and
 * this is what comes out when you draw with it. The swath is broad and
 * ragged, the way a wax crayon dragged across paper covers it, so it reads
 * as a picture being colored in rather than as a line drawn with a pen.
 *
 * The crayon is the app's own object, drawn point down at the very lean the
 * tray in the app gives it, and in the crayon's real material: the wax color
 * of the stick, the cone a shade deeper where the light leaves it, a wrapper
 * in the wax's own hue, and the two dark rules a real wrapper wears. Nothing
 * is drawn around the wax, and that is deliberate: a real crayon has no line
 * around it, and at the size of an icon a line is most of what the eye reads,
 * which is what turns a drawing of a crayon into a diagram of one.
 *
 * Every number lives in [Mark], in the mark's own box. [MarkBox] measures
 * that box from the geometry itself and fits it to the canvas by the mark's
 * furthest point from its own center, so the same drawing fills a 48 pixel
 * legacy tile, a 432 pixel adaptive foreground and a 512 pixel store icon
 * without a tip being clipped by a launcher's round mask at any of them.
 *
 * The whole mark is mirrored about the canvas center, which is what makes
 * the launcher icon the app's own crayon facing the other way. One flip of
 * one finished drawing, so the mirror is a mirror of the mark and not a
 * change to it: same crayon, same lean, same proportions, other way round.
 *
 * Rendered three ways: the legacy tile for API 24-25, the adaptive
 * foreground for API 26+, and the white monochrome sibling (the same mark as
 * a flat silhouette) for Android 13+ themed icons.
 *
 * Colors mirror app/src/main/res/values/colors.xml; change both together,
 * run makeIcons, and commit the regenerated PNGs.
 */
object IconDesign {
    /** The paper ground: warm, a step under the app's own card. */
    const val PAPER: Int = 0xFFF3E9D7.toInt()

    /** The crayon's wax: the brand coral, the sailboat's red. */
    val WAX: Int = Crayons.RED.toInt()

    /** The white the monochrome layer is drawn in. */
    const val WHITE: Int = -0x1 // 0xFFFFFFFF, how Kotlin spells opaque white

    /**
     * The wrapper, the cone, the rules and the base end, all derived from
     * the wax by core's one recipe, so the launcher icon's crayon is made of
     * exactly the colors the device's tray draws, at every density.
     */
    val WRAPPER: Int = CrayonInk.wrapper(Crayons.RED).toInt()
    val WAX_CONE: Int = CrayonInk.cone(Crayons.RED).toInt()
    val WAX_SHADE: Int = CrayonInk.rule(Crayons.RED).toInt()
    val WAX_BASE: Int = CrayonInk.base(Crayons.RED).toInt()

    val DENSITY_DIRS = arrayOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val DENSITY_SCALES = doubleArrayOf(1.0, 1.5, 2.0, 3.0, 4.0)

    const val LEGACY_DP = 48.0
    const val ADAPTIVE_DP = 108.0
    const val LEGACY_CORNER_FRACTION = 0.22
    const val STORE_CORNER_FRACTION = 0.19

    /**
     * How close a stroke is allowed to come to a canvas's own edge on the
     * layers a launcher masks, as a fraction of the canvas radius. Android's
     * safe zone is 66 of the adaptive icon's 108dp; this leaves the mark a
     * hair inside it so antialiasing has room to finish.
     */
    const val ADAPTIVE_INSET = 0.56
    const val TILE_INSET = 0.80
}

internal enum class Layer { TILE, FOREGROUND, MONO }

/**
 * The mark, in its own box.
 *
 * The box is a unit square and the whole mark is laid out inside it, so
 * every number here is a proportion of the drawing rather than a size. The
 * crayon stands at the left with its tip low, and the swath it has just laid
 * runs away to the right, which is the one arrangement that reads as a hand
 * having moved rather than as two objects.
 */
private object Mark {

    /** The crayon's tip, and how long the stick is. */
    val TIP: Vec2 = Vec2(0.30, 0.78)
    const val LENGTH = 0.62

    /**
     * The lean, in degrees, turning the crayon's axis from straight up
     * toward the left. This is the tray's own posting, so the icon's crayon
     * and the app's crayon are one object and not two drawings of one.
     */
    const val LEAN = -14.0

    /**
     * The line the hand traveled, as control points of a smooth curve. The
     * swath is drawn as a band of growing width along this line, so the
     * stroke is thin where the crayon touched down and broad where the hand
     * was moving, which is what a real drag looks like.
     */
    val SWATH: List<Vec2> = listOf(
        Vec2(0.24, 0.775),
        Vec2(0.52, 0.810),
        Vec2(0.80, 0.790),
        Vec2(1.02, 0.720),
    )

    /** How wide the swath is at the tip, and at its far end. */
    const val SWATH_START = 0.12
    const val SWATH_END = 0.22

    /** How the swath's edges are finished: samples, and how ragged they are. */
    const val SWATH_STEPS = 64
    const val SWATH_RAGGED = 0.10
}

/** A point in the mark's own box, in the icon's own pixels. */
private class MarkBox(size: Int, inset: Double) {

    private val canvas = size.toDouble()
    private val scale: Double
    private val middleX: Double
    private val middleY: Double

    init {
        // The drawing's own extent, measured from its geometry rather than
        // guessed at, so a change to the swath can never push the crayon off
        // the edge of the tile. The mark is fitted by its furthest point from
        // its own center, not by its bounding box: a wide, low drawing in a
        // square canvas would otherwise have its corners cut off by a
        // launcher's round mask, which is exactly what a safe zone prevents.
        val points = ArrayList<Vec2>()
        points += Mark.SWATH
        points += CrayonShape.outline().map { crayonPoint(it) }
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        middleX = (minX + maxX) / 2.0
        middleY = (minY + maxY) / 2.0
        val reach = points.maxOf { Math.hypot(it.x - middleX, it.y - middleY) }
        scale = canvas / 2.0 * inset / reach
    }

    /** One unit of the mark's own box, in the icon's pixels. */
    val unit: Double get() = scale

    fun at(p: Vec2): DoubleArray = doubleArrayOf(
        canvas / 2.0 + (p.x - middleX) * scale,
        canvas / 2.0 + (p.y - middleY) * scale,
    )

    /** A point of the crayon's own shape, in the mark's box. */
    fun crayonPoint(p: Vec2): Vec2 {
        val len = Mark.LENGTH / CrayonShape.LENGTH
        val a = Math.toRadians(Mark.LEAN)
        // The shape's y runs from the tip along the stick, and its x runs
        // across it; the axis is "up" turned by the lean.
        val along = p.y * len
        val across = (p.x - 0.5) * len
        val dirX = Math.sin(a)
        val dirY = -Math.cos(a)
        return Vec2(
            Mark.TIP.x + along * dirX - across * dirY,
            Mark.TIP.y + along * dirY + across * dirX,
        )
    }

    /** The crayon's own thickness unit, in the icon's pixels. */
    fun crayonUnit(): Double = Mark.LENGTH / CrayonShape.LENGTH * unit
}

/**
 * Paints the mark: the swath of wax first, then the crayon standing in it.
 *
 * [mono] draws the whole mark as one flat white silhouette, which is what
 * Android's themed icons want: the launcher supplies the color.
 */
internal fun paintMark(g: Graphics2D, size: Int, mono: Boolean, inset: Double) {
    // One transform for everything: the canvas is mirrored about its own
    // center, so the finished mark faces the other way on the home screen.
    g.transform(AffineTransform(-1.0, 0.0, 0.0, 1.0, size.toDouble(), 0.0))

    val box = MarkBox(size, inset)
    val wax = Color(if (mono) IconDesign.WHITE else IconDesign.WAX, true)

    // The swath: the wax the crayon has already laid down. It is drawn as a
    // filled band rather than as a wide line, because the width has to grow
    // along the stroke, and it carries the same grain as every colored area
    // in the book, so the band is made of the material its own crayon is.
    val swath = swathBand(box)
    g.color = wax
    g.fill(swath)
    paintGrain(g, swath)

    // The crayon, standing with its tip in the swath: the nib is the top of
    // the line it made, which is the direction the wax really travels.
    val body = crayonPath(box, CrayonShape.outline())
    g.color = wax
    g.fill(body)
    if (!mono) {
        g.color = Color(IconDesign.WAX_CONE, true)
        g.fill(conePath(box))
        g.color = Color(IconDesign.WRAPPER, true)
        g.fill(bandPath(box, CrayonShape.wrapperBand()))
        // The two dark rules a real wrapper wears. They are drawn as floats:
        // rounding them to whole pixels would make the mark a hair different
        // on the two sides of the mirror that turns it the other way round.
        g.color = Color(IconDesign.WAX_SHADE, true)
        g.stroke = BasicStroke(
            (box.crayonUnit() * CrayonShape.RULE_WEIGHT).toFloat().coerceAtLeast(1f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        val band = CrayonShape.wrapperBand()
        // The inset is a share of the band's own ends, in the shape's own
        // units, so it is right at every size. A pixel count here would be
        // off the end of the crayon (see ui/CrayonGlyph.kt).
        val near = band.h * 0.12
        for (y in listOf(band.y + near, band.bottom - near)) {
            val a = box.at(box.crayonPoint(Vec2(band.x, y)))
            val b = box.at(box.crayonPoint(Vec2(band.right, y)))
            g.draw(java.awt.geom.Line2D.Double(a[0], a[1], b[0], b[1]))
        }
        g.color = Color(IconDesign.WAX_BASE, true)
        g.fill(bandPath(box, CrayonShape.baseBand()))
    }
    // The grain goes over the stick too, so the crayon is made of the same
    // wax as the band it is drawing.
    paintGrain(g, body)
}

/**
 * The swath as a filled band: a smooth line of growing width with ragged
 * edges, which is the shape a crayon drag leaves and not the shape a pen
 * does. Every point is measured off the same center line, so the band can
 * never fold back on itself.
 */
private fun swathBand(box: MarkBox): Path2D.Double {
    val line = smooth(Mark.SWATH, Mark.SWATH_STEPS)
    val near = ArrayList<Vec2>(line.size)
    val far = ArrayList<Vec2>(line.size)
    for ((i, p) in line.withIndex()) {
        val t = i.toDouble() / (line.size - 1)
        val width = Mark.SWATH_START + (Mark.SWATH_END - Mark.SWATH_START) * t
        val before = line[(i - 1).coerceAtLeast(0)]
        val after = line[(i + 1).coerceAtMost(line.size - 1)]
        val dx = after.x - before.x
        val dy = after.y - before.y
        val len = Math.hypot(dx, dy).takeIf { it > 1e-9 } ?: 1.0
        // The normal of the center line: which way this slice's edge runs.
        val nx = -dy / len
        val ny = dx / len
        // The ragged edge of wax skipping the paper's low spots, and a taper
        // at the far end so the stroke finishes the way wax does: thin and
        // broken, never chopped off square. The near end is not tapered: the
        // wax starts where the tip is standing, and a taper there would
        // pinch the stroke away from the crayon that made it.
        val wobble = Math.sin(t * 11.0) * width * Mark.SWATH_RAGGED
        val cap = minOf(1.0, (1.0 - t) * 5.0)
        val half = width * (0.72 + 0.28 * cap) / 2.0
        near += Vec2(p.x + nx * (half + wobble), p.y + ny * (half + wobble))
        far += Vec2(p.x - nx * (half - wobble), p.y - ny * (half - wobble))
    }
    val ring = near + far.asReversed()
    return Path2D.Double().apply {
        val first = box.at(ring[0])
        moveTo(first[0], first[1])
        for (i in 1 until ring.size) {
            val p = box.at(ring[i])
            lineTo(p[0], p[1])
        }
        closePath()
    }
}

/** A closed path of the crayon's own points, mapped into the icon. */
private fun crayonPath(box: MarkBox, points: List<Vec2>): Path2D.Double = Path2D.Double().apply {
    val first = box.at(box.crayonPoint(points[0]))
    moveTo(first[0], first[1])
    for (i in 1 until points.size) {
        val p = box.at(box.crayonPoint(points[i]))
        lineTo(p[0], p[1])
    }
    closePath()
}

/** The cone alone, so the tip can be a shade deeper than the body. */
private fun conePath(box: MarkBox): Path2D.Double {
    val outline = CrayonShape.outline()
    return crayonPath(
        box,
        outline.takeWhile { it.y <= CrayonShape.TIP_LENGTH + 1e-9 } +
            listOf(Vec2(1.0, CrayonShape.TIP_LENGTH), Vec2(0.0, CrayonShape.TIP_LENGTH)),
    )
}

/** One band of the crayon, mapped into the icon. */
private fun bandPath(box: MarkBox, band: Area): Path2D.Double =
    crayonPath(
        box,
        listOf(
            Vec2(band.x, band.y),
            Vec2(band.right, band.y),
            Vec2(band.right, band.bottom),
            Vec2(band.x, band.bottom),
        ),
    )

/** A Catmull-Rom polyline through [control], which is how a hand's line is. */
private fun smooth(control: List<Vec2>, samples: Int): List<Vec2> {
    val segs = control.size - 1
    val out = ArrayList<Vec2>(samples + 1)
    for (i in 0..samples) {
        val u = i.toDouble() / samples * segs
        val k = u.toInt().coerceAtMost(segs - 1)
        val f = u - k
        val a = control[(k - 1).coerceAtLeast(0)]
        val b = control[k]
        val c = control[(k + 1).coerceAtMost(segs)]
        val d = control[(k + 2).coerceAtMost(segs)]
        out += catmull(a, b, c, d, f)
    }
    return out
}

private fun catmull(p0: Vec2, p1: Vec2, p2: Vec2, p3: Vec2, t: Double): Vec2 {
    val t2 = t * t
    val t3 = t2 * t
    fun axis(a: Double, b: Double, c: Double, d: Double): Double = 0.5 * (
        2.0 * b + (-a + c) * t + (2.0 * a - 5.0 * b + 4.0 * c - d) * t2 +
            (-a + 3.0 * b - 3.0 * c + d) * t3
        )
    return Vec2(axis(p0.x, p1.x, p2.x, p3.x), axis(p0.y, p1.y, p2.y, p3.y))
}

/**
 * The crayon's own silhouette, in the icon's own pixels, so the mark can be
 * laid over a white desk by the icon pin. It is the one crayon in the
 * project, scaled into the canvas, and it is the shape the tray draws.
 */
internal fun crayonSilhouette(size: Int, inset: Double): Path2D.Double {
    val box = MarkBox(size, inset)
    return crayonPath(box, CrayonShape.outline())
}

/** Lays the app's wax grain over [shape], clipped to it by the paint itself. */
private fun paintGrain(g: Graphics2D, shape: java.awt.Shape) {
    val pixels = WaxGrain.pixels()
    val tile = BufferedImage(WaxGrain.SIZE, WaxGrain.SIZE, BufferedImage.TYPE_INT_ARGB)
    tile.setRGB(0, 0, WaxGrain.SIZE, WaxGrain.SIZE, pixels, 0, WaxGrain.SIZE)
    val saved = g.paint
    g.paint = TexturePaint(
        tile,
        Rectangle2D.Double(0.0, 0.0, WaxGrain.SIZE.toDouble(), WaxGrain.SIZE.toDouble()),
    )
    g.fill(shape)
    g.paint = saved
}

internal fun paintLayer(size: Int, layer: Layer, cornerFraction: Double): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    when (layer) {
        Layer.TILE -> {
            g.color = Color(IconDesign.PAPER, true)
            g.fill(
                RoundRectangle2D.Double(
                    0.0, 0.0, size.toDouble(), size.toDouble(),
                    size * cornerFraction * 2, size * cornerFraction * 2,
                ),
            )
            paintMark(g, size, mono = false, inset = IconDesign.TILE_INSET)
        }
        Layer.FOREGROUND -> paintMark(g, size, mono = false, inset = IconDesign.ADAPTIVE_INSET)
        Layer.MONO -> paintMark(g, size, mono = true, inset = IconDesign.ADAPTIVE_INSET)
    }
    g.dispose()
    return image
}

/** The legacy tile: paper with the mark on it, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: the mark, or its white silhouette. */
fun adaptiveLayer(sizePx: Int, argb: Int): BufferedImage {
    if (argb == IconDesign.WHITE) return paintLayer(sizePx, Layer.MONO, 0.0)
    return paintLayer(sizePx, Layer.FOREGROUND, 0.0)
}

/** The 512 store tile: full art with the store corner. */
fun storeTile(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.STORE_CORNER_FRACTION)

/** One icon file: where it lives under res, and the image that belongs there. */
data class IconFile(val relativePath: String, val image: () -> BufferedImage)

/** Every file makeIcons owns, with its size bound to its density. */
fun iconFiles(): List<IconFile> = buildList {
    for (i in IconDesign.DENSITY_DIRS.indices) {
        val dir = "mipmap-${IconDesign.DENSITY_DIRS[i]}"
        val scale = IconDesign.DENSITY_SCALES[i]
        add(IconFile("$dir/ic_launcher.png") { legacyIcon(Math.round(IconDesign.LEGACY_DP * scale).toInt()) })
        add(IconFile("$dir/ic_launcher_foreground.png") {
            adaptiveLayer(Math.round(IconDesign.ADAPTIVE_DP * scale).toInt(), IconDesign.PAPER)
        })
        add(IconFile("$dir/ic_launcher_monochrome.png") {
            adaptiveLayer(Math.round(IconDesign.ADAPTIVE_DP * scale).toInt(), IconDesign.WHITE)
        })
    }
}

/**
 * The pin's limits, and why they are not zero.
 *
 * Java2D's antialiasing is not byte-reproducible across JDK major versions.
 * The same shape, filled by the same code, lands a hair differently on an
 * antialiased edge: measured between JDK 17 (what CI runs) and JDK 25 (the
 * Android Studio JBR), 12 pixels of 100,000 differ, and the largest of them
 * is a pixel of coverage coming out as alpha 1 of 255 instead of alpha 0.
 * Both are invisible, and a byte-exact pin would refuse every honest
 * regeneration.
 *
 * So the comparison is made in the only terms that matter: what a person
 * sees. Each image is laid over the same white desk, pixel differences
 * inside [JITTER] are ignored outright as rasterizer noise, a difference
 * over [EDIT_DELTA] fails at once as a real change, and the pixels in
 * between get a small budget ([MAX_DRIFT_FRACTION] of the icon, with a
 * floor for a small one). A hand-edited or stale asset is nothing like
 * this: a recolored crayon, a moved line, an icon from another app, or a
 * different canvas size all change thousands of pixels by tens or hundreds,
 * which no budget here can hide.
 */
private const val JITTER = 8.0
private const val EDIT_DELTA = 48.0
private const val MAX_DRIFT_FRACTION = 0.005
private const val MIN_DRIFT_PIXELS = 24

/** One channel of [argb] as it looks laid over a white desk. */
private fun overWhite(channel: Int, alpha: Int): Double {
    val a = alpha / 255.0
    return channel * a + 255.0 * (1.0 - a)
}

/**
 * True when [committed] and [fresh] are the same icon to the eye: equal
 * within the drift a different JDK's antialiasing can produce, and not one
 * visible pixel more.
 */
private fun sameIcon(committed: BufferedImage, fresh: BufferedImage): Boolean {
    if (committed.width != fresh.width || committed.height != fresh.height) return false
    val pixels = committed.width * committed.height
    val allowed = (pixels * MAX_DRIFT_FRACTION).toInt().coerceAtLeast(MIN_DRIFT_PIXELS)
    var drifting = 0
    for (y in 0 until committed.height) {
        for (x in 0 until committed.width) {
            val a = committed.getRGB(x, y)
            val b = fresh.getRGB(x, y)
            if (a == b) continue
            val alphaA = (a ushr 24) and 0xFF
            val alphaB = (b ushr 24) and 0xFF
            var delta = 0.0
            for (shift in intArrayOf(16, 8, 0)) {
                val ca = overWhite((a ushr shift) and 0xFF, alphaA)
                val cb = overWhite((b ushr shift) and 0xFF, alphaB)
                delta = maxOf(delta, kotlin.math.abs(ca - cb))
            }
            if (delta <= JITTER) continue
            if (delta > EDIT_DELTA) return false
            drifting++
            if (drifting > allowed) return false
        }
    }
    return true
}

/** Write the whole icon set into a res directory, overwriting in place. */
fun writeIcons(resDir: File) {
    for (file in iconFiles()) {
        val out = File(resDir, file.relativePath)
        out.parentFile.mkdirs()
        ImageIO.write(file.image(), "png", out)
    }
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val resDir = File(rootDir, "app/src/main/res")
    val check = args.size > 1 && args[1] == "--check"

    if (!check) {
        writeIcons(resDir)
        println("makeIcons: wrote ${iconFiles().size} files under ${resDir.path}")
        return
    }

    val drift = mutableListOf<String>()
    for (file in iconFiles()) {
        val committed = File(resDir, file.relativePath)
        if (!committed.exists()) {
            drift.add("${file.relativePath}: missing")
            continue
        }
        val onDisk = runCatching { ImageIO.read(committed) }.getOrNull()
        if (onDisk == null || !sameIcon(onDisk, file.image())) {
            drift.add("${file.relativePath}: differs from regeneration")
        }
    }
    if (drift.isNotEmpty()) {
        println("checkIcons: committed icons drifted from the generator:")
        drift.forEach { println("  $it") }
        println("Run :tools:makeIcons, inspect, and commit the regenerated PNGs.")
        kotlin.system.exitProcess(1)
    }
    println("checkIcons: all ${iconFiles().size} icon files match the generator.")
}
