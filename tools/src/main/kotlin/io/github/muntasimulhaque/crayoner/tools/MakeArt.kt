package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
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
 * app: the brand coral ground, the book's first page shown twice, the name
 * written in the app's own hand, and one line under it.
 *
 * The banner's left side is the sailboat page exactly as the book prints it,
 * and beside it the same page finished: the pair is the whole app in one
 * glance, in the app's own colors, with the wax laid down by the same code
 * the app colors with. Under the name stands the app's own mark, the crayon
 * at the end of the line it has just drawn, in paper white: the banner and
 * the launcher icon carry one sentence between them, drawn by one piece of
 * code.
 *
 * Outputs (never hand-edited; regenerate with :tools:makeArt):
 *   play-store/feature-graphic-1024x500.png
 *   play-store/play-icon-512.png
 */
object MakeArt {

    private val BRAND: Int = Crayons.RED.toInt()
    private val CARD: Int = 0xFFFFFDF8.toInt()

    /** The color of the one line under the name: paper, a step back. */
    private val TAGLINE: Int = 0xFFF7DCD7.toInt()

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

        // Two sheets of the same page, side by side: on the left exactly what
        // the book prints, and on the right the same page finished. The pair
        // is the whole app in one glance, and it reads in the order it is
        // played: here are the lines, and here is what a child makes of them.
        // Both wear the same soft shadow a sheet wears on the desk in the
        // app, so the banner's sheets and the child's sheet are visibly the
        // same kind of object.
        val page = Pages.byId("sail") ?: error("the sail page is gone")
        val sheetW = 180
        val sheetH = (sheetW * 1.2).toInt()
        val sheetY = (h - sheetH) / 2
        val gap = 24
        val bareX = 60
        val finishedX = bareX + sheetW + gap
        // As printed: paper, and the book's own lines, and nothing else.
        sheet(g, page, bareX, sheetY, sheetW, sheetH, emptyMap())
        // And finished: every area in its own color.
        val finished = page.regions.indices.associateWith { page.regions[it].fillArgb }
        sheet(g, page, finishedX, sheetY, sheetW, sheetH, finished)
        paintWordmark(g, rootDir)
        g.dispose()
        return image
    }

    /**
     * The name, the line under it, and the app's own mark below them: the
     * whole right side of the banner.
     *
     * The name is set to fill its half of the banner without ever reaching
     * the sheets or the edge, and the line under it is broken where a
     * reader's voice would break it, so it stays clear of the mark below it.
     * The mark is paper white on the coral: the same crayon at the same lean,
     * standing at the end of the same line, drawn from the same geometry as
     * the launcher icon and rendered as one flat white silhouette, because at
     * this size four shades of white on a coral ground read as a smudge and
     * the shape alone reads as the app.
     */
    private fun paintWordmark(g: Graphics2D, rootDir: File) {
        drawCleanString(g, "Crayoner", "chewy.ttf", 100f, CARD, 520f, 224f, rootDir)
        drawCleanString(g, "Color the picture,", "chewy.ttf", 27f, TAGLINE, 526f, 282f, rootDir)
        drawCleanString(g, "just like the book.", "chewy.ttf", 27f, TAGLINE, 526f, 318f, rootDir)
        val mark = g.create(800, 310, 190, 176) as Graphics2D
        paintMark(mark, 176, inset = 0.94, palette = MarkPalette.MONO)
        mark.dispose()
    }

    /**
     * One sheet on the banner: paper, its shadow, and the page on it, with
     * only the areas in [fills] colored in. The same call draws the sheet as
     * printed and the sheet as a child could finish it, so the two can never
     * come out as different paper.
     */
    private fun sheet(
        g: Graphics2D,
        page: Page,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        fills: Map<Int, Long>,
    ) {
        sheetShadow(g, x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        g.color = Color(CARD, true)
        g.fill(Rectangle2D.Double(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble()))
        val sheetG = g.create(x, y, w, h) as Graphics2D
        RenderKit.renderPage(sheetG, page, w.toDouble(), fills)
        sheetG.dispose()
    }

    /**
     * A sheet's own shadow, on the banner's ground: the same soft lift a sheet
     * wears on the desk in the app, so a sheet reads as a sheet and not as a
     * rectangle pasted on the banner.
     */
    private fun sheetShadow(g: Graphics2D, x: Double, y: Double, w: Double, h: Double) {
        for (step in 1..5) {
            val spread = step * 5.0
            val alpha = (0.05 * (6 - step)).coerceAtLeast(0.02)
            g.color = Color(0, 0, 0, (alpha * 255).toInt())
            g.fill(
                RoundRectangle2D.Double(
                    x - spread * 0.35,
                    y + spread * 0.35,
                    w + spread * 0.7,
                    h + spread * 0.7,
                    spread,
                    spread,
                ),
            )
        }
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
