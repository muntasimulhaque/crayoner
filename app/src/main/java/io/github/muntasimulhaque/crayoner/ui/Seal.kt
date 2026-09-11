package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The wax seal: what a picture wears when the child says it is done.
 *
 * A finished piece of work gets a stamp, and a stamp is a real thing a child
 * has seen: a grown-up presses it onto the paper, and from then on the sheet
 * is a finished piece and not a page in progress. That is the whole of the
 * app's celebration, and it is enough. Nothing here counts areas or matches
 * colors, so the child is never told their picture is finished before they
 * say so, and never told that it is not.
 *
 * The seal is pressed, not drawn: a disc of coral wax with the app's own
 * crayon stamped into it, the paper's tooth over the whole thing, and a ring
 * of wax squeezed out around its edge.
 */
fun DrawScope.drawWaxSeal(center: Offset, radius: Float, squash: Float = 1f) {
    if (radius <= 0f) return
    val pressed = radius * (0.94f + 0.06f * squash)
    // The wax squeezed out around the stamp, a hair wider and softer than
    // the stamp itself.
    drawCircle(
        CrayonerColors.Coral.copy(alpha = 0.28f),
        radius = pressed * 1.16f,
        center = center,
    )
    drawCircle(CrayonerColors.Coral, radius = pressed, center = center)
    // The stamp's own rim, pressed into the wax.
    drawCircle(
        CrayonerColors.Ink.copy(alpha = 0.42f),
        radius = pressed * 0.90f,
        center = center,
        style = Stroke(width = (pressed * 0.06f).coerceAtLeast(1f), cap = StrokeCap.Round),
    )
    // The crayon, the app's own mark, stamped into the middle of the seal.
    val unit = pressed * 1.06f / CrayonShape.LENGTH.toFloat()
    val left = center.x - unit / 2f
    val top = center.y - CrayonShape.LENGTH.toFloat() * unit / 2f
    val outline = Path().apply {
        val points = CrayonShape.outline()
        fun px(p: Vec2) = Offset(left + p.x.toFloat() * unit, top + p.y.toFloat() * unit)
        val first = px(points[0])
        moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val o = px(points[i])
            lineTo(o.x, o.y)
        }
        close()
    }
    clipPath(outline) {
        drawRect(CrayonerColors.Card.copy(alpha = 0.92f))
    }
    drawPath(
        outline,
        CrayonerColors.Ink.copy(alpha = 0.55f),
        style = Stroke(
            width = (unit * CrayonShape.LINE.toFloat() * 0.9f).coerceAtLeast(1f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
    // The paper's tooth over the whole stamp, the same grain a colored area
    // carries: the seal is made of the same wax as the picture under it.
    val grain = GrainBrush
    if (grain != null) {
        clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, pressed)) }) {
            drawRect(grain)
        }
    }
}

/**
 * The seal on the shelf and on the page: one small stamp, pressed where it
 * belongs. [pressStamp] is the host's own count of stamps: zero means the
 * seal is already down (opening a finished picture again), and anything else
 * means it is being pressed, so it lands the way a stamp does, down onto the
 * paper and settling. That landing is the one moment the app marks the end
 * of a picture, and it only ever happens because the child pressed it.
 */
@Composable
fun WaxSeal(
    modifier: Modifier = Modifier,
    pressStamp: Long = 0L,
) {
    val press = remember { Animatable(if (pressStamp == 0L) 1f else 0f) }
    LaunchedEffect(pressStamp) {
        if (pressStamp == 0L) {
            press.snapTo(1f)
            return@LaunchedEffect
        }
        press.snapTo(0f)
        press.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        // The stamp comes down from above and lands: the scale gives it the
        // hand that pressed it.
        val p = press.value.coerceIn(0f, 1.6f)
        val scale = 0.55f + 0.45f * p.coerceAtMost(1f)
        drawWaxSeal(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = radius * scale,
            squash = p.coerceAtMost(1f),
        )
    }
}

/** One size for the seal wherever it appears, so it is one object. */
internal val SealSize = 34.dp
