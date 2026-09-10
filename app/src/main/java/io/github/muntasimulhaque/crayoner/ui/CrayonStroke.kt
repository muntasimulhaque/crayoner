package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.muntasimulhaque.crayoner.core.smoothPoints
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * A crayon-drawn rule: one stroke, no ruler, with the small wobble a hand
 * gives it. It is the app's own way of underlining a word, and the only
 * decoration on the shelf header.
 */
@Composable
fun CrayonUnderline(modifier: Modifier = Modifier, color: Color = CrayonerColors.Coral) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        // Control points along the width, each nudged up or down a little,
        // then smoothed: a real stroke, not a stutter.
        val control = listOf(
            Vec2(0.0, 0.55),
            Vec2(0.18, 0.30),
            Vec2(0.40, 0.68),
            Vec2(0.62, 0.34),
            Vec2(0.84, 0.66),
            Vec2(1.0, 0.44),
        )
        val points = smoothPoints(control, samples = 18)
        val path = Path().apply {
            moveTo(points[0].x.toFloat() * w, points[0].y.toFloat() * h)
            for (i in 1 until points.size) {
                lineTo(points[i].x.toFloat() * w, points[i].y.toFloat() * h)
            }
        }
        drawPath(
            path,
            color,
            style = Stroke(
                width = h * 0.55f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        // A faint second pass, so the stroke has the uneven edge of wax.
        val echo = Path().apply {
            val pts = smoothPoints(control.map { Vec2(it.x, it.y + 0.22) }, samples = 14)
            moveTo(pts[0].x.toFloat() * w, pts[0].y.toFloat() * h)
            for (i in 1 until pts.size) lineTo(pts[i].x.toFloat() * w, pts[i].y.toFloat() * h)
        }
        drawPath(
            echo,
            color.copy(alpha = 0.45f),
            style = Stroke(width = h * 0.30f, cap = StrokeCap.Round),
        )
    }
}
