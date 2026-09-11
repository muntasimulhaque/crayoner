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
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's own icon set, drawn as geometry: a house for the shelf, a
 * speaker for the sound switch, a stamp for a finished picture, and a small
 * crayon for a picture being worked on. No icon fonts, no third party packs:
 * same hand, same weights, everywhere.
 */
@Composable
fun HomeIcon(modifier: Modifier = Modifier, color: Color, size: Dp = 24.dp) {
    GeoIcon(modifier, color, size) { w, h ->
        val line = Stroke(w * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
fun SoundIcon(modifier: Modifier = Modifier, on: Boolean, color: Color, size: Dp = 26.dp) {
    GeoIcon(modifier, color, size) { w, h ->
        val line = Stroke(w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
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

/**
 * The stamp: a rubber stamp seen from the side, which is what a finished
 * piece of work wears. It is not a star and not a tick, because neither of
 * those means this is mine and I finished it.
 */
@Composable
fun SealIcon(modifier: Modifier = Modifier, color: Color, size: Dp = 24.dp) {
    GeoIcon(modifier, color, size) { w, h ->
        val line = Stroke(w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // The handle, the collar, and the pad: three strokes, and it reads
        // as a stamp from across a room.
        drawLine(
            color,
            Offset(w * 0.38f, h * 0.14f),
            Offset(w * 0.38f, h * 0.42f),
            line.width,
            line.cap,
        )
        drawLine(
            color,
            Offset(w * 0.62f, h * 0.14f),
            Offset(w * 0.62f, h * 0.42f),
            line.width,
            line.cap,
        )
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.30f, h * 0.42f),
            size = Size(w * 0.40f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = line,
        )
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.16f, h * 0.62f),
            size = Size(w * 0.68f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = line,
        )
        // The mark it leaves.
        drawLine(
            color,
            Offset(w * 0.24f, h * 0.88f),
            Offset(w * 0.76f, h * 0.88f),
            line.width * 0.9f,
            line.cap,
        )
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
