package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.CrayonInk
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Vec2
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** How far the app's own crayon leans, judged at the sizes a person meets it. */
object LeanReview {

    private val INK = Color(0x38404F)
    private val CARD = Color(0xFFFDF8)
    private val CARDBOARD = Color(0xEFE1C6)
    private val DESK = Color(0xF6EFE3)
    private const val CORAL = 0xEE204D

    @JvmStatic
    fun main(args: Array<String>) {
        val leans = listOf(20.0, 26.0, 32.0, 38.0)
        val img = BufferedImage(1520, 900, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = DESK
        g.fillRect(0, 0, img.width, img.height)

        // Row 1: the wall's nameplate, with the name and the mark at 40 dp.
        var x = 60
        for (lean in leans) {
            wordmark(g, x, 60, lean, 40.0)
            x += 360
        }
        // Row 2: the seat on the capsule: the mark in a coin.
        x = 60
        for (lean in leans) {
            coin(g, x, 220)
            crayon(g, x + 39.0, 220 + 39.0, 78.0, lean)
            g.color = INK
            g.font = Font("SansSerif", Font.PLAIN, 13)
            g.drawString("${lean.toInt()} degrees", x + 4, 220 + 156 + 20)
            x += 180
        }
        // Row 3: the launcher mark, at the sizes a launcher really draws it.
        val sizes = listOf(96, 72, 48, 36, 24)
        x = 60
        for (lean in leans) {
            var y = 420
            for (size in sizes) {
                val tile = markTile(size, lean)
                g.drawImage(tile, x, y, null)
                y += size + 14
            }
            g.color = INK
            g.font = Font("SansSerif", Font.PLAIN, 13)
            g.drawString("${lean.toInt()} degrees", x + 4, y + 4)
            x += 200
        }
        // Row 4: the same launcher marks, zoomed, so the eye can judge them.
        x = 60
        for (lean in leans) {
            val tile = markTile(24, lean)
            val big = BufferedImage(192, 192, BufferedImage.TYPE_INT_ARGB)
            val g2 = big.createGraphics()
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            g2.drawImage(tile, 0, 0, 192, 192, null)
            g2.dispose()
            g.drawImage(big, x, 700, null)
            x += 200
        }
        g.dispose()
        ImageIO.write(img, "png", File(System.getProperty("java.io.tmpdir"), "probe/lean.png"))
        println("wrote lean.png")
    }

    private fun coin(g: Graphics2D, x: Int, y: Int) {
        g.color = Color(56, 64, 79, 46)
        g.fillRoundRect(x + 2, y + 7, 156, 156, 156, 156)
        g.color = CARDBOARD
        g.fillRoundRect(x, y, 156, 156, 156, 156)
    }

    /** The wall's wordmark: the mark, then the name, both on one line. */
    private fun wordmark(g: Graphics2D, x: Int, y: Int, lean: Double, markDp: Double) {
        val bounds = CrayonShape.turnedBounds(CrayonShape.HELD_TURN + lean)
        val unit = markDp / CrayonShape.LENGTH
        val w = bounds.w * unit
        val h = bounds.h * unit
        crayon(g, x.toDouble(), y.toDouble(), markDp, lean)
        g.color = INK
        g.font = Font("SansSerif", Font.PLAIN, 46)
        g.drawString("Crayoner", (x + w + 12).toInt(), (y + h * 0.5 + 16).toInt())
    }

    /** The mark on a tile, exactly as the icon generator draws it. */
    private fun markTile(size: Int, lean: Double): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = Color(0xF3E9D7)
        g.fillRoundRect(0, 0, size, size, (size * 0.44).toInt(), (size * 0.44).toInt())
        g.dispose()
        // The real mark, from the real generator: the review has to show the
        // drawing the app ships, or it is reviewing a second drawing.
        val real = paintLayer(size, Layer.TILE, IconDesign.LEGACY_CORNER_FRACTION)
        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g3 = out.createGraphics()
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g3.color = Color(0xF3E9D7)
        g3.fillRoundRect(0, 0, size, size, (size * 0.44).toInt(), (size * 0.44).toInt())
        g3.drawImage(real, 0, 0, null)
        g3.dispose()
        return out
    }

    /** The app's crayon, at [markDp] long, leaning [lean] degrees. */
    private fun crayon(g: Graphics2D, left: Double, top: Double, markDp: Double, lean: Double) {
        val wax = Color(CORAL)
        val turn = CrayonShape.HELD_TURN + lean
        val bounds = CrayonShape.turnedBounds(turn)
        val unit = minOf(markDp / bounds.w, markDp / bounds.h)
        val ox = left + (markDp - bounds.w * unit) / 2.0 - bounds.x * unit
        val oy = top + (markDp - bounds.h * unit) / 2.0 - bounds.y * unit
        fun px(p: Vec2): Pair<Double, Double> {
            val t = CrayonShape.turned(p, turn)
            return ox + t.x * unit to oy + t.y * unit
        }
        fun path(points: List<Vec2>): Path2D {
            val p = Path2D.Double()
            points.forEachIndexed { i, v ->
                val (px0, py0) = px(v)
                if (i == 0) p.moveTo(px0, py0) else p.lineTo(px0, py0)
            }
            p.closePath()
            return p
        }
        val outline = CrayonShape.outline()
        g.color = wax
        g.fill(path(outline))
        val cone = outline.takeWhile { it.y <= CrayonShape.TIP_LENGTH + 1e-9 } +
            listOf(Vec2(1.0, CrayonShape.TIP_LENGTH), Vec2(0.0, CrayonShape.TIP_LENGTH))
        g.color = Color(CrayonInk.cone(CORAL.toLong()).toInt(), true)
        g.fill(path(cone))
        val band = CrayonShape.wrapperBand()
        g.color = Color(CrayonInk.wrapper(CORAL.toLong()).toInt(), true)
        g.fill(
            path(
                listOf(
                    Vec2(band.x, band.y), Vec2(band.right, band.y),
                    Vec2(band.right, band.bottom), Vec2(band.x, band.bottom),
                ),
            ),
        )
        g.color = Color(CrayonInk.rule(CORAL.toLong()).toInt(), true)
        g.stroke = BasicStroke(
            (unit * CrayonShape.RULE_WEIGHT * 1.6).toFloat().coerceAtLeast(0.7f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        val near = band.h * CrayonShape.RULE_INSET
        for (yy in listOf(band.y + near, band.bottom - near)) {
            val a = px(Vec2(band.x, yy))
            val b = px(Vec2(band.right, yy))
            g.draw(java.awt.geom.Line2D.Double(a.first, a.second, b.first, b.second))
        }
        g.color = Color(CrayonInk.base(CORAL.toLong()).toInt(), true)
        val base = CrayonShape.baseBand()
        g.fill(
            path(
                listOf(
                    Vec2(base.x, base.y), Vec2(base.right, base.y),
                    Vec2(base.right, base.bottom), Vec2(base.x, base.bottom),
                ),
            ),
        )
    }
}
