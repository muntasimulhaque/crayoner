package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.star
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Lays the falling confetti at [t] through its fall. */
internal fun DrawScope.drawConfetti(pieces: List<Confetti>, t: Float) {
    for (p in pieces) {
        val local = ((t - p.delay) / p.fall).coerceIn(0.0, 1.0)
        if (local <= 0.0) continue
        val y = ((-0.08 + local * 1.25) * size.height).toFloat()
        val x = ((p.x0 + p.sway * sin(local * p.freq * 2 * PI + p.phase)) * size.width).toFloat()
        val alpha = if (local > 0.82) ((1.0 - local) / 0.18).toFloat() else 1f
        val angle = (p.rot0 + t * p.spin).toFloat()
        withTransform({
            translate(x, y)
            rotate(angle)
        }) {
            when (p.kind) {
                0 -> drawCircle(p.color.copy(alpha = alpha), radius = p.size.toFloat())
                1 -> drawRoundRect(
                    p.color.copy(alpha = alpha),
                    topLeft = Offset(-p.size.toFloat(), -p.size.toFloat()),
                    size = Size(p.size.toFloat() * 2, p.size.toFloat() * 2),
                    cornerRadius = CornerRadius(2f),
                )
                2 -> drawPath(confettiTriangle(p.size.toFloat()), p.color.copy(alpha = alpha))
                else -> drawPath(confettiStar(p.size.toFloat()), p.color.copy(alpha = alpha))
            }
        }
    }
}

private fun confettiTriangle(s: Float): Path = Path().apply {
    moveTo(0f, -s)
    lineTo(s * 0.9f, s * 0.7f)
    lineTo(-s * 0.9f, s * 0.7f)
    close()
}

private fun confettiStar(s: Float): Path = Path().apply {
    val pts = star(0.0, 0.0, s.toDouble(), (s * 0.45).toDouble(), 5)
    val list = pts.points
    moveTo(list[0].x.toFloat(), list[0].y.toFloat())
    for (i in 1 until list.size) lineTo(list[i].x.toFloat(), list[i].y.toFloat())
    close()
}

internal class Confetti(
    /** Where it enters, across the width, and when: all fractions. */
    val x0: Double,
    val delay: Double,
    val fall: Double,
    /** How far it sways, how fast, and where it starts in that sway. */
    val sway: Double,
    val freq: Double,
    val phase: Double,
    /** Its starting angle and how fast it turns on the way down. */
    val rot0: Double,
    val spin: Double,
    /** Its size in pixels, its color, and which of the four shapes it is. */
    val size: Double,
    val color: Color,
    val kind: Int,
)

/**
 * Confetti in the child's own colors: whatever they actually put on the
 * page, plus one honey for the celebration. A picture they colored in their
 * own way celebrates in their own way.
 */
internal fun buildConfetti(page: Page, strokes: List<Stroke>): List<Confetti> {
    val rnd = Random(page.id.hashCode().toLong())
    val used = strokes.map { it.color }.toSet()
        .ifEmpty { setOf(Crayons.YELLOW, Crayons.SKY) }
    val colors = used.map { Color(it) } + CrayonerColors.Honey
    return List(56) {
        Confetti(
            x0 = 0.05 + rnd.nextDouble() * 0.9,
            delay = rnd.nextDouble() * 0.25,
            fall = 0.85 + rnd.nextDouble() * 0.5,
            sway = 0.02 + rnd.nextDouble() * 0.05,
            freq = 1.0 + rnd.nextDouble() * 2.0,
            phase = rnd.nextDouble() * 2 * PI,
            rot0 = rnd.nextDouble() * 360,
            spin = (rnd.nextDouble() - 0.5) * 720,
            size = 5.0 + rnd.nextDouble() * 7.0,
            color = colors[rnd.nextInt(colors.size)],
            kind = rnd.nextInt(4),
        )
    }
}
