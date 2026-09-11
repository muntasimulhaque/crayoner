package io.github.muntasimulhaque.crayoner.tools

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
 * app's own tray draws it, with its point down and its body leaning a
 * little, as if it had just come off a page. It is drawn in a crayon's real
 * material: the wax color of the tip, a paper wrapper in the wax's own hue,
 * the two dark rules a real wrapper wears, the same wax grain every colored
 * area in the book carries, and the ink line the app draws everything in.
 *
 * The point is down for two reasons. A crayon is used point down, so that is
 * the way it looks in a hand and the way a child recognizes it; and a crayon
 * standing on its point cannot read as anything but a crayon.
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
    /** The wrapper: the wax's own color, a whisper lightened, like paper. */
    val WRAPPER: Int = 0xFFF25C7F.toInt()
    /** The two rules and the shadow the wrapper wears. */
    val WAX_SHADE: Int = 0xFFA62841.toInt()
    val INK: Int = Crayons.INK.toInt()

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
    val lean = -14.0
    g.rotate(Math.toRadians(180.0), size / 2.0, size / 2.0)
    g.rotate(Math.toRadians(lean), size / 2.0, size / 2.0)

    val s = size.toDouble()
    // The crayon's length, and the thickness that follows from it.
    val h = s * inset * 0.86
    val thickness = h * CrayonShape.THICKNESS
    val cx = s / 2.0
    val top = (s - h) / 2.0

    val wax = Color(if (mono) IconDesign.WHITE else IconDesign.WAX, true)
    val wrapper = Color(if (mono) IconDesign.WHITE else IconDesign.WRAPPER, true)
    val shade = Color(if (mono) IconDesign.WHITE else IconDesign.WAX_SHADE, true)
    val ink = Color(if (mono) IconDesign.WHITE else IconDesign.INK, true)

    // The one crayon in the project (core/CrayonShape), scaled into the
    // canvas: the same outline the tray draws, so the icon on the home
    // screen and the crayon in the box are the same object. The shape is
    // measured in the crayon's own thicknesses, so one unit is the
    // thickness on both axes and the crayon cannot be stretched.
    fun pxX(v: Double) = cx + (v - 0.5) * thickness
    fun pxY(v: Double) = top + v * thickness
    val body = Path2D.Double().apply {
        val points = CrayonShape.outline()
        moveTo(pxX(points[0].x), pxY(points[0].y))
        for (i in 1 until points.size) lineTo(pxX(points[i].x), pxY(points[i].y))
        closePath()
    }
    g.color = wax
    g.fill(body)

    // The wrapper's band, shared with the tray. The wrapper is the crayon's
    // own wax, barely lightened, so it reads as paper over wax and not as a
    // pale sleeve, which would be a pencil; the grain is the same tile the
    // pages carry, so the mark is made of the same material as the pictures
    // it draws; and the two dark rules are the ones a real wrapper wears.
    if (!mono) {
        val band = CrayonShape.wrapperBand()
        val wrap = Path2D.Double().apply {
            moveTo(pxX(band.x), pxY(band.y))
            lineTo(pxX(band.right), pxY(band.y))
            lineTo(pxX(band.right), pxY(band.bottom))
            lineTo(pxX(band.x), pxY(band.bottom))
            closePath()
        }
        g.color = wrapper
        g.fill(wrap)
        // The wax grain, the same tile the pages carry, so the mark is made
        // of the same material as the pictures it draws.
        paintGrain(g, body)
        // The two dark rules a real wrapper wears.
        g.color = shade
        g.stroke = BasicStroke(
            (thickness * 0.077).toFloat().coerceAtLeast(1f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        g.drawLine(
            pxX(band.x).toInt(), pxY(band.y + 0.012).toInt(),
            pxX(band.right).toInt(), pxY(band.y + 0.012).toInt(),
        )
        g.drawLine(
            pxX(band.x).toInt(), pxY(band.bottom - 0.012).toInt(),
            pxX(band.right).toInt(), pxY(band.bottom - 0.012).toInt(),
        )
    }

    g.color = ink
    g.stroke = BasicStroke(
        (thickness * CrayonShape.LINE).toFloat().coerceAtLeast(1f),
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND,
    )
    g.draw(body)
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

private fun pngBytes(image: BufferedImage): ByteArray {
    val bytes = java.io.ByteArrayOutputStream()
    ImageIO.write(image, "png", bytes)
    return bytes.toByteArray()
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
        if (!pngBytes(file.image()).contentEquals(committed.readBytes())) {
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
