package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Strokes
import java.awt.BasicStroke
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
 * app: the brand coral ground, one crayon, the name, and one line under it.
 *
 * The banner's left side is a real coloring page from the book (the
 * sailboat), half finished: the child's own view of the app, in the app's
 * own colors.
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
        // the app and not an artist's idea of the app.
        val plate = 372
        val px = 62
        val py = (h - plate) / 2
        g.color = Color(0xFFFFFDF8.toInt(), true)
        g.fill(
            RoundRectangle2D.Double(
                px.toDouble() - 14, py.toDouble() - 14,
                plate.toDouble() + 28, plate.toDouble() + 28,
                6.0, 6.0,
            ),
        )
        // The tape at its corners, the same roll the app uses: short strips
        // crossing each corner, half on the paper and half on the desk behind
        // it.
        for ((corner, angle) in listOf(
            (px - 4 to py - 4) to 45,
            (px + plate + 4 to py - 4) to -45,
            (px - 4 to py + plate + 4) to -45,
            (px + plate + 4 to py + plate + 4) to 45,
        )) {
            val tape = java.awt.geom.AffineTransform.getRotateInstance(
                Math.toRadians(angle.toDouble()), corner.first.toDouble(), corner.second.toDouble(),
            )
            val strip = tape.createTransformedShape(
                Rectangle2D.Double(
                    corner.first - 38.0, corner.second - 11.0, 76.0, 22.0,
                ),
            )
            g.color = Color(0xD6F6E7C4.toInt(), true)
            g.fill(strip)
            g.color = Color(0x38A08B5E.toInt(), true)
            g.stroke = BasicStroke(1.5f)
            g.draw(strip)
        }
        val plateG = g.create(px, py, plate, plate) as Graphics2D
        val page = Pages.byId("sail") ?: error("the sail page is gone")
        // Everything printed, and then the child's hand: the sky, the clouds,
        // the sea and the sail scribbled in, the boat and its mast left for
        // next time. Two marks even miss nothing: a real page is like that.
        RenderKit.renderPage(plateG, page, plate.toDouble(), emptyMap(), grain = false)
        val half = listOf("sky", "cloud", "cloud_high", "sea")
            .mapNotNull { id -> page.region(page.indexOfRegion(id)) }
            .flatMap { region ->
                listOf(
                    Strokes.scribble(region, region.fillArgb),
                    Strokes.scribble(region, region.fillArgb, 0.012),
                )
            }
        RenderKit.renderStrokes(plateG, half, plate.toDouble())
        plateG.dispose()

        drawCleanString(g, "Crayoner", "baloo2_extrabold.ttf", 116f, 0xFFFFFDF8.toInt(), 486f, 246f, rootDir)
        drawCleanString(
            g,
            "Color the picture, just like the book.",
            "baloo2_bold.ttf",
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
 * Baloo slices above 96 pt under Java2D (gaps across the stems), so store
 * words render small and scale up. Same font files the app bundles, same
 * shapes, verified clean.
 */
internal fun balooFont(rootDir: File, file: String, size: Float): java.awt.Font =
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
    val font = balooFont(rootDir, file, base)
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
