package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The picture sheets, drawn by the same data the app colors, so a review
 * round is pointing at pictures instead of reading a description of them.
 *
 * Three sheets go to build/sheets (never committed: working scratch):
 *   pairs.png    every page as the sample beside its line art
 *   samples.png  the shelf of finished samples, four across
 *   outlines.png the shelf of blank coloring pages, the child's view
 *
 * Plain JVM, Java2D, no third-party libraries, same as every other
 * generator here.
 */
private const val TILE = 300
private const val GAP = 22
private const val MARGIN = 40
private const val LABEL = 40
private const val COLUMNS = 4

private val PAPER = Color(0xF6F1E7)
private val CARD = Color(0xFFFDF7)
private val INK = Color(0x2E3444)

fun main(args: Array<String>) {
    val rootDir = File(args.firstOrNull() ?: ".").absoluteFile
    val outDir = File(rootDir, "build/sheets")
    check(outDir.isDirectory || outDir.mkdirs()) { "Could not create $outDir" }
    val pages = Pages.all
    val only = args.getOrNull(1)
    val chosen = if (only.isNullOrBlank()) pages else pages.filter { it.id == only }
    check(chosen.isNotEmpty()) { "No page named $only" }

    ImageIO.write(grid(chosen, sample = true, rootDir = rootDir), "png", File(outDir, "samples.png"))
    ImageIO.write(grid(chosen, sample = false, rootDir = rootDir), "png", File(outDir, "outlines.png"))
    ImageIO.write(pairs(chosen, rootDir), "png", File(outDir, "pairs.png"))
    println("makeSheets: wrote samples.png, outlines.png and pairs.png under ${outDir.path}")
}

private fun grid(pages: List<Page>, sample: Boolean, rootDir: File): BufferedImage {
    val rows = (pages.size + COLUMNS - 1) / COLUMNS
    val w = MARGIN * 2 + COLUMNS * TILE + (COLUMNS - 1) * GAP
    val h = MARGIN * 2 + rows * (TILE + LABEL) + (rows - 1) * GAP
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.color = PAPER
    g.fillRect(0, 0, w, h)
    for ((index, page) in pages.withIndex()) {
        val col = index % COLUMNS
        val row = index / COLUMNS
        val x = MARGIN + col * (TILE + GAP)
        val y = MARGIN + row * (TILE + LABEL + GAP)
        card(g, page, x, y, sample, rootDir)
    }
    g.dispose()
    return image
}

private fun pairs(pages: List<Page>, rootDir: File): BufferedImage {
    val cell = TILE
    val w = MARGIN * 2 + cell * 2 + GAP
    val h = MARGIN * 2 + pages.size * (TILE + LABEL) + (pages.size - 1) * GAP
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.color = PAPER
    g.fillRect(0, 0, w, h)
    for ((index, page) in pages.withIndex()) {
        val y = MARGIN + index * (TILE + LABEL + GAP)
        card(g, page, MARGIN, y, sample = true, rootDir = rootDir)
        card(g, page, MARGIN + cell + GAP, y, sample = false, rootDir = rootDir)
    }
    g.dispose()
    return image
}

/** One picture card: the page on a white plate, its name and count under it. */
private fun card(g: Graphics2D, page: Page, x: Int, y: Int, sample: Boolean, rootDir: File) {
    g.color = CARD
    g.fill(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), TILE.toDouble(), TILE.toDouble(), 24.0, 24.0))
    val page2 = g.create(x, y, TILE, TILE) as Graphics2D
    val fills = if (sample) RenderKit.sampleFills(page) else emptyMap()
    RenderKit.renderPage(page2, page, TILE.toDouble(), fills)
    page2.dispose()
    val label = if (sample) "${page.id} (sample)" else "${page.id} (${page.regionCount} areas)"
    drawText(g, label, x + TILE / 2, y + TILE + 26, rootDir)
}

private fun drawText(g: Graphics2D, text: String, centerX: Int, baselineY: Int, rootDir: File) {
    val font = brandFont(rootDir, "chewy.ttf", 24f)
    g.font = font
    g.color = INK
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    val width = g.fontMetrics.stringWidth(text)
    g.drawString(text, centerX - width / 2, baselineY)
}

