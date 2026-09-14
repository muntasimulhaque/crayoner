package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Strokes
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil

/**
 * The Play Store art, drawn from the same pictures and the same mark as the
 * app: the brand coral ground, one crayon, the name written in the app's own
 * hand, and one line under it.
 *
 * The banner's left side is a real coloring page from the book (the
 * sailboat), half finished: the child's own view of the app, in the app's
 * own colors, with the wax laid down by the same code the app colors with.
 *
 * Outputs (never hand-edited; regenerate with :tools:makeArt):
 *   play-store/feature-graphic-1024x500.png
 *   play-store/play-icon-512.png
 */
object MakeArt {

    private val BRAND: Int = Crayons.RED.toInt()

    /** The 1024 x 500 feature graphic: the mark, the name, one line. */
    fun featureGraphic(rootDir: File): BufferedImage {
        val w = 1024
        val h = 500
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(BRAND, true)
        g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))

        // The picture plate on the left: the sailboat, the book's first page,
        // half colored by a child's own hand. Every mark on it is a real
        // scribble from the same code the app draws with, so the banner shows
        // the app and not an artist's idea of the app. The plate keeps the
        // page's own proportion, taller than it is wide, the way a sheet in
        // the pad is, so the banner shows the shape of the thing the child
        // really colors.
        val plateW = 300
        val plateH = (plateW * 1.2).toInt()
        val px = 74
        val py = (h - plateH) / 2
        g.color = Color(0xFFFFFDF8.toInt(), true)
        g.fill(
            RoundRectangle2D.Double(
                px.toDouble() - 14, py.toDouble() - 14,
                plateW.toDouble() + 28, plateH.toDouble() + 28,
                6.0, 6.0,
            ),
        )
        val plateG = g.create(px, py, plateW, plateH) as Graphics2D
        val page = Pages.byId("sail") ?: error("the sail page is gone")
        // Everything printed, and then the child's own hand: the sky, the
        // clouds and the sea rubbed in with real wax, the sail and the boat
        // still bare lines waiting for next time. The wax is the same wax the
        // app lays down, so the banner shows the app and not an artist's
        // idea of it.
        val started = setOf("sky", "cloud", "cloud_high", "sea")
            .mapNotNull { id -> page.indexOfRegion(id).takeIf { it >= 0 } }
        val half = page.regions.indices
            .filter { it in started }
            .associateWith { page.regions[it].fillArgb }
        RenderKit.renderPage(plateG, page, plateW.toDouble(), half)
        plateG.dispose()

        drawCleanString(g, "Crayoner", "chewy.ttf", 124f, 0xFFFFFDF8.toInt(), 470f, 250f, rootDir)
        drawCleanString(
            g,
            "Color the picture, just like the book.",
            "chewy.ttf",
            30f,
            0xFFF7DCD7.toInt(),
            490f,
            312f,
            rootDir,
        )
        g.dispose()
        return image
    }

    /** The 512 x 512 store icon: the launcher tile, full bleed. */
    fun storeIcon(): BufferedImage = storeTile(512)
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val outDir = File(rootDir, "play-store")
    outDir.mkdirs()
    ImageIO.write(MakeArt.featureGraphic(rootDir), "png", File(outDir, "feature-graphic-1024x500.png"))
    ImageIO.write(MakeArt.storeIcon(), "png", File(outDir, "play-icon-512.png"))
    println("makeArt: wrote the feature graphic and the store icon under ${outDir.path}")
}

/**
 * The app's own face, loaded from the files it bundles, so no word in the
 * store art is set in a font the app does not own.
 *
 * Chewy has thin joins that Java2D fills in when it is asked for a very
 * large point size, so the words are laid out small and scaled up: the same
 * shapes the device draws, with none of the joins lost.
 */
internal fun brandFont(rootDir: File, file: String, size: Float): java.awt.Font =
    java.awt.Font.createFont(
        java.awt.Font.TRUETYPE_FONT,
        File(rootDir, "app/src/main/res/font/$file"),
    ).deriveFont(size)

internal fun drawCleanString(
    g: Graphics2D,
    text: String,
    file: String,
    target: Float,
    argb: Int,
    x: Float,
    y: Float,
    rootDir: File,
) {
    val base = 96f
    val k = target / base
    val font = brandFont(rootDir, file, base)
    val tmp = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
    val g0 = tmp.createGraphics()
    g0.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    val bounds = font.getStringBounds(text, g0.fontRenderContext)
    g0.dispose()
    val sx = -bounds.x + 4.0
    val sy = -bounds.y + 4.0
    val small = BufferedImage(
        ceil(bounds.width + 8.0).toInt(),
        ceil(bounds.height + 8.0).toInt(),
        BufferedImage.TYPE_INT_ARGB,
    )
    val g1 = small.createGraphics()
    g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g1.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g1.font = font
    g1.color = Color(argb, true)
    g1.drawString(text, sx.toFloat(), sy.toFloat())
    g1.dispose()
    val bigW = ceil(small.width * k).toInt()
    val bigH = ceil(small.height * k).toInt()
    val big = BufferedImage(bigW, bigH, BufferedImage.TYPE_INT_ARGB)
    val g2 = big.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g2.drawImage(small, 0, 0, bigW, bigH, null)
    g2.dispose()
    g.drawImage(big, (x - sx * k).toInt(), (y - sy * k).toInt(), null)
}

/** Kept so callers that want raw bytes (the icon pin) share one encoder. */
internal fun png(image: BufferedImage): ByteArray {
    val bytes = ByteArrayOutputStream()
    ImageIO.write(image, "png", bytes)
    return bytes.toByteArray()
}
