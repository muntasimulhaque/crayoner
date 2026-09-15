package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.AppIcon
import io.github.muntasimulhaque.crayoner.core.CrayonInk
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Vec2
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * A review bench for the app's own marks: it draws the real geometry from
 * :core at the size a coin really draws it, so a mark can be judged by eye
 * before it is wired into the UI.
 */
object MarkReview {
    private val INK = Color(0x38404F)
    private val CARD = Color(0xFFFDF8)
    private val CARDBOARD = Color(0xEFE1C6)
    private val DESK = Color(0xF6EFE3)
    private const val COIN = 156
    private const val ICON = 78

    @JvmStatic
    fun main(args: Array<String>) {
        val names = AppIcon.Name.entries.toList()
        val cols = names.size
        val img = BufferedImage(40 + cols * (COIN + 16), 40 + 2 * (COIN + 54) + 300, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = DESK
        g.fillRect(0, 0, img.width, img.height)
        for ((i, name) in names.withIndex()) {
            coin(g, i, 0, CARD)
            mark(g, name, i, 0)
            g.color = INK
            g.font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13)
            g.drawString(name.name, cellX(i) + 4, cellY(0) + COIN + 20)
            coin(g, i, 1, CARDBOARD)
            mark(g, name, i, 1)
        }
        // The crayon, which comes from its own file, and the launcher mark at
        // every size a launcher really draws it: the smallest size is where a
        // mark either reads or does not.
        coin(g, 2, 1, CARDBOARD)
        crayon(g, cellX(2) + (COIN - ICON) / 2.0, cellY(1) + (COIN - ICON) / 2.0)
        launcher(g)
        g.dispose()
        ImageIO.write(img, "png", File(System.getProperty("java.io.tmpdir"), "probe/marks.png"))
        println("wrote marks.png ${img.width}x${img.height}")
    }

    private fun cellX(col: Int) = 40 + col * (COIN + 16)
    private fun cellY(row: Int) = 40 + row * (COIN + 54)

    private fun coin(g: Graphics2D, col: Int, row: Int, plate: Color) {
        val x = cellX(col).toDouble()
        val y = cellY(row).toDouble()
        g.color = Color(56, 64, 79, 46)
        g.fill(Ellipse2D.Double(x + 2, y + 7, COIN.toDouble(), COIN.toDouble()))
        g.color = plate
        g.fill(Ellipse2D.Double(x, y, COIN.toDouble(), COIN.toDouble()))
    }

    private fun mark(g: Graphics2D, name: AppIcon.Name, col: Int, row: Int) {
        val left = cellX(col) + (COIN - ICON) / 2.0
        val top = cellY(row) + (COIN - ICON) / 2.0
        val s = ICON.toDouble()
        val stroke = BasicStroke(
            (s * AppIcon.LINE).toFloat().coerceAtLeast(1.4f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        g.color = INK
        for (piece in AppIcon.pieces(name)) {
            val path = Path2D.Double()
            var first = true
            for (p in piece.points) {
                val x = left + p.x * s
                val y = top + p.y * s
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
            }
            if (piece.closed) path.closePath()
            if (piece.fill) g.fill(path) else g.stroke = stroke.also { g.draw(path) }
        }
    }

    /** The launcher mark, at every size a launcher draws it. */
    private fun launcher(g: Graphics2D) {
        val sizes = listOf(192, 96, 72, 48, 36, 24)
        var x = 40
        val y = cellY(1) + COIN + 40
        for (size in sizes) {
            val tile = paintLayer(size, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)
            g.drawImage(tile, x, y, null)
            x += size + 16
        }
        g.color = INK
        g.font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13)
        g.drawString("the launcher mark, 192 down to 24 px", 40, y + 200)
    }

    /** The app's crayon, from the project's own shape, at its own lean. */
    private fun crayon(g: Graphics2D, left: Double, top: Double) {
        val s = ICON.toDouble()
        val wax = Color(0xEE204D)
        val turn = CrayonShape.MARK_TURN
        val bounds = CrayonShape.turnedBounds(turn)
        val unit = minOf(s / bounds.w, s / bounds.h)
        val ox = left + (s - bounds.w * unit) / 2.0 - bounds.x * unit
        val oy = top + (s - bounds.h * unit) / 2.0 - bounds.y * unit
        fun px(p: Vec2): Pair<Double, Double> {
            val t = CrayonShape.turned(p, turn)
            return ox + t.x * unit to oy + t.y * unit
        }
        fun path(points: List<Vec2>): Path2D {
            val p = Path2D.Double()
            points.forEachIndexed { i, v ->
                val (x, y) = px(v)
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.closePath()
            return p
        }
        val outline = CrayonShape.outline()
        g.color = wax
        g.fill(path(outline))
        val cone = outline.takeWhile { it.y <= CrayonShape.TIP_LENGTH + 1e-9 } +
            listOf(Vec2(1.0, CrayonShape.TIP_LENGTH), Vec2(0.0, CrayonShape.TIP_LENGTH))
        g.color = Color(CrayonInk.cone(0xEE204D.toLong()).toInt(), true)
        g.fill(path(cone))
        val band = CrayonShape.wrapperBand()
        g.color = Color(CrayonInk.wrapper(0xEE204D.toLong()).toInt(), true)
        g.fill(path(listOf(Vec2(band.x, band.y), Vec2(band.right, band.y), Vec2(band.right, band.bottom), Vec2(band.x, band.bottom))))
        g.color = Color(CrayonInk.rule(0xEE204D.toLong()).toInt(), true)
        g.stroke = BasicStroke((unit * CrayonShape.RULE_WEIGHT * 1.6).toFloat().coerceAtLeast(0.7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val near = band.h * CrayonShape.RULE_INSET
        for (y in listOf(band.y + near, band.bottom - near)) {
            val a = px(Vec2(band.x, y))
            val b = px(Vec2(band.right, y))
            g.draw(java.awt.geom.Line2D.Double(a.first, a.second, b.first, b.second))
        }
        g.color = Color(CrayonInk.base(0xEE204D.toLong()).toInt(), true)
        val base = CrayonShape.baseBand()
        g.fill(path(listOf(Vec2(base.x, base.y), Vec2(base.right, base.y), Vec2(base.right, base.bottom), Vec2(base.x, base.bottom))))
    }
}
