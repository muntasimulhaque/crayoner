package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.CrayonInk
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.WaxGrain
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.TexturePaint
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The launcher icon, drawn from code so every PNG has exactly one author.
 *
 * The mark: one crayon, the tool the whole app is about, held the way the
 * app's own tray draws it, with its point down and its body leaning, as if
 * it had just come off a page. It is drawn in a crayon's real material: the
 * wax color of the tip, the cone a shade deeper where the light leaves it,
 * a wrapper in the wax's own hue, the two dark rules a real wrapper wears,
 * the same wax grain every colored area in the book carries, and no ink
 * line around the wax at all.
 *
 * Nothing is drawn around the stick, and that is deliberate. A real crayon
 * has no line around it: the wrapper's edge is where the paper wound over
 * the wax ends, and the wax beside it is the stick's own color laid on
 * thick. A line there is ink the object does not have, and at the size of an
 * icon the line is most of what the eye reads, which is what turns a drawing
 * of a crayon into a diagram of one. The two printed rules stay, because
 * they are the one line a real wrapper really wears.
 *
 * The point is down for two reasons. A crayon is used point down, so that is
 * the way it looks in a hand and the way a child recognizes it; and a crayon
 * standing on its point cannot read as anything but a crayon.
 *
 * The whole mark leans the other way from the wall's own crayon and points
 * the other way round: the app's icon is the one mark that faces inward at
 * the launcher, and nothing else in the app is drawn mirrored.
 *
 * Rendered three ways: the legacy tile for API 24-25, the adaptive
 * foreground for API 26+, and a white monochrome sibling (the crayon
 * silhouette) for Android 13+ themed icons.
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
}

internal enum class Layer { TILE, FOREGROUND, MONO }

/**
 * The crayon mark: the app's own object, drawn the way the device draws it
 * (see `ui/CrayonBox.drawCrayonShape`), standing point down and leaning a
 * little, the way a crayon looks the moment it has been used.
 *
 * The proportions are the real object's and they are the whole reason this
 * reads as a crayon: the body is a little under three times as long as it is
 * thick, the cone is as wide as the body and a little longer than it is
 * wide, and the nose is bluntly rounded rather than sharp. A shorter cone is
 * a block, a longer or sharper one is a pencil, and a fatter body is a
 * marker.
 *
 * [inset] is how much of the canvas the mark may use: 1.0 fills it, and the
 * adaptive layer asks for less, so no launcher shape can clip a tip.
 */
internal fun paintCrayon(g: Graphics2D, size: Int, mono: Boolean, inset: Double = 1.0) {
    // One turn around the canvas center. The half turn stands the crayon on
    // its point, and the lean is the wrist: because both happen here, every
    // layer and every density lean by exactly the same amount.
    //
    // The whole mark is then mirrored about the canvas center, which is what
    // makes the launcher icon the wall's crayon facing the other way. The
    // mirror goes on first in code and so lands outermost in the transform,
    // which is what keeps it a mirror of the finished drawing rather than a
    // change to the drawing itself: same crayon, same lean, other way round.
    val lean = -14.0
    g.translate(size.toDouble(), 0.0)
    g.scale(-1.0, 1.0)
    g.rotate(Math.toRadians(180.0), size / 2.0, size / 2.0)
    g.rotate(Math.toRadians(lean), size / 2.0, size / 2.0)

    // One mapping for the whole drawing: a point in the shape's own unit
    // box to a point in the icon, in the icon's own scale. It is the same
    // mapping the device uses (see ui/CrayonGlyph.drawCrayonShape), so the
    // pieces land in the same proportions and the icon cannot drift from
    // the crayon the tray draws.
    val unit = size.toDouble() * inset * 0.86
    val left = (size - unit) / 2.0
    fun pxX(v: Double) = left + v * unit
    fun pxY(v: Double) = left + v * unit
    fun path(points: List<Pair<Double, Double>>): Path2D.Double = Path2D.Double().apply {
        val first = points.first()
        moveTo(pxX(first.first), pxY(first.second))
        for (i in 1 until points.size) lineTo(pxX(points[i].first), pxY(points[i].second))
        closePath()
    }
    val outline = CrayonShape.outline().map { it.x to it.y }
    val body = path(outline)
    val band = CrayonShape.wrapperBand()
    val wrap = path(
        listOf(
            band.x to band.y,
            band.right to band.y,
            band.right to band.bottom,
            band.x to band.bottom,
        ),
    )
    val coneBreaks = CrayonShape.TIP_LENGTH * 0.72
    val cone = path(
        outline.takeWhile { it.second <= CrayonShape.TIP_LENGTH + 1e-9 } +
            listOf(1.0 to CrayonShape.TIP_LENGTH, 0.0 to CrayonShape.TIP_LENGTH),
    )
    val base = CrayonShape.baseBand()
    val end = path(
        listOf(
            base.x to base.y,
            base.right to base.y,
            base.right to base.bottom,
            base.x to base.bottom,
        ),
    )
    val coneSplit = coneBreaks

    val wax = Color(if (mono) IconDesign.WHITE else IconDesign.WAX, true)
    val wrapper = Color(if (mono) IconDesign.WHITE else IconDesign.WRAPPER, true)
    val rule = Color(if (mono) IconDesign.WHITE else IconDesign.WAX_SHADE, true)
    val tip = Color(if (mono) IconDesign.WHITE else IconDesign.WAX_CONE, true)
    val baseShade = Color(if (mono) IconDesign.WHITE else IconDesign.WAX_BASE, true)

    g.color = wax
    g.fill(body)

    // The cone, a shade deeper than the body: a drawn crayon's tip is where
    // the light leaves it, and that is the only shading a stick gets. Then
    // the wrapper's band, from the same geometry the tray uses, and the
    // base's own sliver at the far end, so the stick reads as round and not
    // as a block. No line is drawn around any of it (see the class comment).
    if (!mono) {
        g.color = tip
        g.fill(cone)

        g.color = wrapper
        g.fill(wrap)
        // The two dark rules a real wrapper wears. They are drawn as floats:
        // rounding them to whole pixels would make the mark a hair different
        // on the two sides of the mirror that turns it the other way round.
        g.color = rule
        g.stroke = BasicStroke(
            (unit * CrayonShape.RULE_WEIGHT).toFloat().coerceAtLeast(1f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        val ruleInset = CrayonShape.RULE_INSET
        g.draw(
            java.awt.geom.Line2D.Double(
                pxX(band.x), pxY(band.y + ruleInset),
                pxX(band.right), pxY(band.y + ruleInset),
            ),
        )
        g.draw(
            java.awt.geom.Line2D.Double(
                pxX(band.x), pxY(band.bottom - ruleInset),
                pxX(band.right), pxY(band.bottom - ruleInset),
            ),
        )
        g.color = baseShade
        g.fill(end)
    }
    // The wax grain goes down last, over the whole stick: the same tile every
    // colored area in the book carries, so the crayon is made of the same
    // material as the pictures it draws. It is a whisper over the wax, the
    // wrapper and the rules alike, exactly as it lies on a page.
    paintGrain(g, body)
}

/**
 * The crayon's own silhouette, in the icon's own pixels, so the same shape
 * can be filled, grained and (for the monochrome layer) drawn as a flat
 * white mark. It is the one crayon in the project, scaled into the canvas,
 * and it is the shape the tray draws.
 */
internal fun crayonSilhouette(size: Int, inset: Double): Path2D.Double {
    val s = size.toDouble()
    val h = s * inset * 0.86
    val thickness = h * CrayonShape.THICKNESS
    val cx = s / 2.0
    val top = (s - h) / 2.0
    val points = CrayonShape.outline()
    return Path2D.Double().apply {
        moveTo(cx + (points[0].x - 0.5) * thickness, top + points[0].y * thickness)
        for (i in 1 until points.size) {
            lineTo(cx + (points[i].x - 0.5) * thickness, top + points[i].y * thickness)
        }
        closePath()
    }
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
            paintCrayon(g, size, mono = false, inset = 0.66)
        }
        Layer.FOREGROUND -> paintCrayon(g, size, mono = false, inset = 0.52)
        Layer.MONO -> paintCrayon(g, size, mono = true, inset = 0.52)
    }
    g.dispose()
    return image
}

/** The legacy tile: paper with the crayon on it, for API 24-25. */
fun legacyIcon(sizePx: Int): BufferedImage =
    paintLayer(sizePx, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)

/** One adaptive layer: the crayon, or its white silhouette. */
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
