package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's own icon set, drawn as geometry: a house for the shelf, a
 * speaker for the sound switch, a rubber, a step back. No icon fonts, no
 * third party packs: same hand, same weights, everywhere.
 *
 * The whole set shares one weight, and that is the point of [IconLine]. A
 * row of buttons is read as one object, so a round mark drawn with a hair
 * line beside one drawn with a marker looks like two different apps. Every
 * outline mark in the app is struck at the same fraction of its own box, and
 * the fraction is chosen for the smallest size the mark is ever drawn at: a
 * mark that reads at 24 dp reads at 26 dp too, and the other way round is
 * not true.
 */

/**
 * The one line weight for an outline icon, as a fraction of the icon's own
 * box. A mark has to read at the size of a fingertip, which is where a
 * lighter line stops being a line at all.
 */
private const val ICON_LINE = 0.098f

/**
 * The stroke every outline icon is drawn with: one weight, one set of caps,
 * scaled to the box it is drawn in.
 */
internal fun iconStroke(w: Float): Stroke = Stroke(
    width = (w * ICON_LINE).coerceAtLeast(1.4f),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

@Composable
fun HomeIcon(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) {
    GeoIcon(modifier, color, size) { w, h ->
        val line = iconStroke(w)
        val roof = Path().apply {
            moveTo(w * 0.16f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.84f, h * 0.48f)
        }
        drawPath(roof, color, style = line)
        val body = Path().apply {
            moveTo(w * 0.26f, h * 0.44f)
            lineTo(w * 0.26f, h * 0.82f)
            lineTo(w * 0.74f, h * 0.82f)
            lineTo(w * 0.74f, h * 0.44f)
        }
        drawPath(body, color, style = line)
        // One little door, so the mark reads as a home and not a tent.
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.43f, h * 0.60f),
            size = Size(w * 0.14f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = line,
        )
    }
}

@Composable
fun SoundIcon(modifier: Modifier = Modifier, on: Boolean, color: Color, size: Dp = IconSize) {
    GeoIcon(modifier, color, size) { w, h ->
        val line = iconStroke(w)
        val cone = Path().apply {
            moveTo(w * 0.15f, h * 0.40f)
            lineTo(w * 0.30f, h * 0.40f)
            lineTo(w * 0.49f, h * 0.19f)
            lineTo(w * 0.49f, h * 0.81f)
            lineTo(w * 0.30f, h * 0.60f)
            lineTo(w * 0.15f, h * 0.60f)
            close()
        }
        drawPath(cone, color, style = line)
        if (on) {
            wave(color, line.width, w, h, radius = 0.175f, halfAngleDeg = 52f)
            wave(color, line.width, w, h, radius = 0.315f, halfAngleDeg = 58f)
        } else {
            val arm = w * 0.105f
            val cx = w * 0.755f
            val cy = h * 0.5f
            drawLine(color, Offset(cx - arm, cy - arm), Offset(cx + arm, cy + arm), line.width, line.cap)
            drawLine(color, Offset(cx + arm, cy - arm), Offset(cx - arm, cy + arm), line.width, line.cap)
        }
    }
}

private fun DrawScope.wave(color: Color, strokeW: Float, w: Float, h: Float, radius: Float, halfAngleDeg: Float) {
    val r = w * radius
    drawArc(
        color,
        startAngle = -halfAngleDeg,
        sweepAngle = 2f * halfAngleDeg,
        useCenter = false,
        topLeft = Offset(w * 0.49f - r, h * 0.5f - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
    )
}

@Composable
private fun GeoIcon(
    modifier: Modifier,
    color: Color,
    size: Dp,
    content: DrawScope.(Float, Float) -> Unit,
) {
    Canvas(modifier = modifier.size(size)) {
        content(this.size.width, this.size.height)
    }
}
