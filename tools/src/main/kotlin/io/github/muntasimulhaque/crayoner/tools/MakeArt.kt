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
 * Nothing the banner draws may stand outside [SAFE]: the store shows the
 * picture in a card, and the card is a smaller box than the file, so the
 * store cuts what hangs over the edge (see [SAFE] for the numbers, and D-092
 * for what that cost us once).
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

    /** The size Play asks for: 1024 by 500, exactly, always. */
    private const val BANNER_W = 1024
    private const val BANNER_H = 500

    /**
     * The window every surface shows, and the reason this file composes
     * inside it.
     *
     * A feature graphic is never shown whole. The listing card is a sixteen
     * by nine box, so the banner is centered in it and
     * (BANNER_W - BANNER_H * 16 / 9) / 2, about 68 pixels, is cut away at
     * each end: the left sheet and the tip of the crayon were standing in
     * that band on the live listing and came out cut. SAFE is the window
     * that crop leaves, with another 20 pixels of air still held inside it
     * and the same air above and below for a surface that cuts there
     * instead, so no crop Play can put on this file can truncate anything
     * drawn here. `MakeArtTest` refuses a banner that breaks it.
     */
    internal val SAFE = Window(88, 56, 848, 388)

    /** One sheet on the banner: the page at its own [Page.ASPECT] of height. */
    private const val SHEET_W = 170
    private const val SHEET_GAP = 22

    /** A sheet's shadow, in [SHADOW_STEPS] steps of [SHADOW_STEP] pixels. */
    private const val SHADOW_STEPS = 5
    private const val SHADOW_STEP = 5.0

    /**
     * Where the widest step of a sheet's shadow reaches past the paper at a
     * side: a third of its spread, because the light sits above and to the
     * left and the shadow falls away from it. The sheets are placed by this
     * number, so the shadow rides inside [SAFE] with the paper instead of
     * over the crop. The shadow's foot reaches further than its side, twice
     * as far, and it hangs in the coral well above the bottom of the safe
     * window, so it is left to the test that measures the real ink.
     */
    private val SHADOW_SIDE = SHADOW_STEP * SHADOW_STEPS * 0.35

    /** The coral between the sheets and the name. */
    private const val WORDS_GAP = 46

    /** Where the name sits, measured from the sheets rather than guessed. */
    private val WORDS_X =
        (SAFE.x + SHADOW_SIDE + SHEET_W * 2 + SHEET_GAP + SHADOW_SIDE + WORDS_GAP).toFloat()

    /** The mark's own canvas, which the mark is fitted inside by geometry. */
    private const val MARK_SIZE = 176

    /** The 1024 x 500 feature graphic: the mark, the name, one line. */
    fun featureGraphic(rootDir: File): BufferedImage {
        val image = BufferedImage(BANNER_W, BANNER_H, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(BRAND, true)
        g.fill(Rectangle2D.Double(0.0, 0.0, BANNER_W.toDouble(), BANNER_H.toDouble()))

        // Two sheets of the same page, side by side: on the left exactly what
        // the book prints, and on the right the same page finished. The pair
        // is the whole app in one glance, and it reads in the order it is
        // played: here are the lines, and here is what a child makes of them.
        // Both wear the same soft shadow a sheet wears on the desk in the
        // app, so the banner's sheets and the child's sheet are visibly the
        // same kind of object. The pair is laid out in [SAFE], shadow and all.
        val page = Pages.byId("sail") ?: error("the sail page is gone")
        val sheetH = (SHEET_W * Page.ASPECT).toInt()
        val sheetX = SAFE.x + ceil(SHADOW_SIDE).toInt()
        val sheetY = SAFE.y + (SAFE.h - sheetH) / 2
        // As printed: paper, and the book's own lines, and nothing else.
        sheet(g, page, sheetX, sheetY, SHEET_W, sheetH, emptyMap())
        // And finished: every area in its own color.
        val finished = page.regions.indices.associateWith { page.regions[it].fillArgb }
        sheet(g, page, sheetX + SHEET_W + SHEET_GAP, sheetY, SHEET_W, sheetH, finished)
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
     *
     * The mark's canvas is fitted into the far corner of [SAFE], and it is
     * fitted by its own geometry, so the crayon stands inside the safe
     * window with room to spare however the store crops this file. Nothing
     * clips it but the banner itself: a clip drawn to its own canvas would
     * hide an overflow from the test that holds the banner inside [SAFE].
     */
    private fun paintWordmark(g: Graphics2D, rootDir: File) {
        drawCleanString(g, "Crayoner", "chewy.ttf", 100f, CARD, WORDS_X, 224f, rootDir)
        drawCleanString(g, "Color the picture,", "chewy.ttf", 27f, TAGLINE, WORDS_X + 6f, 282f, rootDir)
        drawCleanString(g, "just like the book.", "chewy.ttf", 27f, TAGLINE, WORDS_X + 6f, 318f, rootDir)
        val x = SAFE.right - MARK_SIZE
        val y = SAFE.bottom - MARK_SIZE
        val mark = g.create(x, y, BANNER_W - x, BANNER_H - y) as Graphics2D
        paintMark(mark, MARK_SIZE, inset = 0.94, palette = MarkPalette.MONO)
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
        for (step in 1..SHADOW_STEPS) {
            val spread = step * SHADOW_STEP
            val alpha = (0.05 * (SHADOW_STEPS + 1 - step)).coerceAtLeast(0.02)
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

/**
 * A rectangle in the banner's own pixels, used for the window the store
 * always shows and for the box a banner's ink really takes up.
 */
internal data class Window(val x: Int, val y: Int, val w: Int, val h: Int) {

    val right: Int get() = x + w

    val bottom: Int get() = y + h

    /** True when the pixel at ([px], [py]) is inside this window. */
    fun holds(px: Int, py: Int): Boolean = px >= x && px < right && py >= y && py < bottom

    /** True when every pixel of [other] is inside this window. */
    fun encloses(other: Window): Boolean = other.x >= x && other.right <= right &&
        other.y >= y && other.bottom <= bottom

    override fun toString(): String = "x $x..$right, y $y..$bottom"
}

/** Kept so callers that want raw bytes (the icon pin) share one encoder. */
internal fun png(image: BufferedImage): ByteArray {
    val bytes = ByteArrayOutputStream()
    ImageIO.write(image, "png", bytes)
    return bytes.toByteArray()
}
