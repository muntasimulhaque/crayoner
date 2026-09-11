package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Area
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * One crayon, drawn once. The tray, the box of colors, the shelf header and
 * the store's own art all draw through here, so a crayon is the same object
 * wherever it appears, and a rubber is the same rubber.
 */
/**
 * The name of each crayon. Every crayon in the box has its own word, held to
 * that by a test, because a crayon the screen reader cannot name is a crayon
 * a blind child cannot find.
 */
internal fun crayonNameRes(argb: Long): Int = when (argb) {
    Crayons.RED -> R.string.crayon_red
    Crayons.SCARLET -> R.string.crayon_scarlet
    Crayons.RED_ORANGE -> R.string.crayon_red_orange
    Crayons.ORANGE -> R.string.crayon_orange
    Crayons.YELLOW_ORANGE -> R.string.crayon_yellow_orange
    Crayons.APRICOT -> R.string.crayon_apricot
    Crayons.YELLOW -> R.string.crayon_yellow
    Crayons.GREEN_YELLOW -> R.string.crayon_green_yellow
    Crayons.YELLOW_GREEN -> R.string.crayon_yellow_green
    Crayons.GREEN -> R.string.crayon_green
    Crayons.BLUE_GREEN -> R.string.crayon_blue_green
    Crayons.CERULEAN -> R.string.crayon_cerulean
    Crayons.SKY_BLUE -> R.string.crayon_sky_blue
    Crayons.BLUE -> R.string.crayon_blue
    Crayons.BLUETIFUL -> R.string.crayon_bluetiful
    Crayons.INDIGO -> R.string.crayon_indigo
    Crayons.BLUE_VIOLET -> R.string.crayon_blue_violet
    Crayons.VIOLET -> R.string.crayon_violet
    Crayons.WISTERIA -> R.string.crayon_wisteria
    Crayons.RED_VIOLET -> R.string.crayon_red_violet
    Crayons.VIOLET_RED -> R.string.crayon_violet_red
    Crayons.CARNATION_PINK -> R.string.crayon_carnation_pink
    Crayons.MELON -> R.string.crayon_melon
    Crayons.PEACH -> R.string.crayon_peach
    Crayons.TAN -> R.string.crayon_tan
    Crayons.BROWN -> R.string.crayon_brown
    Crayons.CHESTNUT -> R.string.crayon_chestnut
    Crayons.GRAY -> R.string.crayon_gray
    Crayons.TIMBERWOLF -> R.string.crayon_timberwolf
    Crayons.CADET_BLUE -> R.string.crayon_cadet_blue
    Crayons.BLACK -> R.string.crayon_black
    Crayons.WHITE -> R.string.crayon_white
    else -> R.string.app_name
}

/**
 * A crayon drawn in the book's own hand. Both the tray and the box draw
 * through here, so a crayon is the same object wherever it appears.
 */
@Composable
fun CrayonGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    contact: Boolean = false,
    lying: Boolean = false,
) {
    val ink = CrayonerColors.Ink
    // The wrapper is the crayon's own wax, barely lightened: a pale sleeve
    // on a colored body is a pencil, and this app draws crayons. The rules
    // that band the wrapper are the same wax taken deeper, the way print on
    // paper reads.
    val shade = remember(color) { mix(color, ink, 0.32f) }
    val wrapper = remember(color) { mix(color, CrayonerColors.Card, 0.12f) }
    Canvas(modifier = modifier) {
        val length = if (lying) size.width else size.height
        drawCrayonShape(
            color = color,
            shade = shade,
            wrapper = wrapper,
            ink = ink,
            contact = contact,
            left = 0f,
            top = 0f,
            length = length,
            lying = lying,
        )
    }
}

/**
 * Draws one crayon, [length] pixels long, in the box starting at ([left],
 * [top]).
 *
 * Lying, its length runs across the canvas and its tip is at the right, the
 * way a crayon rests in a tray. Standing, its length runs down the canvas
 * with the tip at the bottom, the way a crayon is held. The width always
 * follows from the length, because a crayon's thickness is a property of the
 * crayon and not of the box it happens to be drawn in.
 *
 * The shape itself is core's one crayon: the body, the blunt cone, the
 * squared base, the wrapper in the wax's own color with its two dark rules,
 * and the paper collar a held crayon wears. This function only decides the
 * colors and the place it lands.
 */
internal fun DrawScope.drawCrayonShape(
    color: Color,
    shade: Color,
    wrapper: Color,
    ink: Color,
    contact: Boolean,
    left: Float,
    top: Float,
    length: Float,
    lying: Boolean,
) {
    // The shape is measured in the crayon's own thicknesses, so one unit is
    // the thickness on both axes and the crayon cannot be stretched.
    val unit = length / CrayonShape.LENGTH.toFloat()
    val line = Stroke(
        (unit * CrayonShape.LINE.toFloat()).coerceAtLeast(1f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    // One mapping for the whole drawing: a point in the shape's own unit box
    // to a point on the canvas, in whichever way the crayon is turned.
    fun px(p: Vec2): Offset = if (lying) {
        Offset(left + length - p.y.toFloat() * unit, top + p.x.toFloat() * unit)
    } else {
        Offset(left + p.x.toFloat() * unit, top + p.y.toFloat() * unit)
    }
    val silhouette = outlinePath(::px)
    drawPath(silhouette, color)

    // The wrapper: the crayon's own color, a whisper lighter, the way paper
    // takes wax. Never a pale sleeve, which would make it a pencil.
    val band = CrayonShape.wrapperBand()
    drawPath(rectPath(::px, band.x, band.y, band.right, band.bottom), wrapper)
    drawWrapperRules(::px, band, shade, line)
    if (contact) {
        // The picked crayon wears a paper collar, the way a held crayon is
        // banded by a hand, so the choice reads at a glance even when two
        // colors are hard to tell apart.
        val collar = CrayonShape.collarBand()
        val path = rectPath(::px, collar.x, collar.y, collar.right, collar.bottom)
        drawPath(path, CrayonerColors.Card)
        drawPath(path, ink, style = Stroke(line.width * 0.5f, cap = StrokeCap.Round))
    }
    drawPath(silhouette, ink, style = line)
}

/** The crayon's own silhouette, mapped onto the canvas by [px]. */
private fun outlinePath(px: (Vec2) -> Offset): Path = Path().apply {
    val points = CrayonShape.outline()
    val first = px(points[0])
    moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val o = px(points[i])
        lineTo(o.x, o.y)
    }
    close()
}

/** One band of the crayon, mapped onto the canvas by [px]. */
private fun rectPath(px: (Vec2) -> Offset, x: Double, y: Double, right: Double, bottom: Double): Path {
    val a = px(Vec2(x, y))
    val b = px(Vec2(right, bottom))
    return Path().apply {
        addRect(
            Rect(
                minOf(a.x, b.x),
                minOf(a.y, b.y),
                maxOf(a.x, b.x),
                maxOf(a.y, b.y),
            ),
        )
    }
}

/**
 * The two dark rules every real wrapper wears, one above the label and one
 * below it. They follow the crayon's own turn, so a lying crayon's rules run
 * across it and a standing one's run down it.
 */
private fun DrawScope.drawWrapperRules(
    px: (Vec2) -> Offset,
    band: Area,
    shade: Color,
    line: Stroke,
) {
    val rule = Stroke(line.width * 0.40f, cap = StrokeCap.Round)
    for (y in listOf(band.y + 0.012, band.bottom - 0.012)) {
        val a = px(Vec2(band.x, y))
        val b = px(Vec2(band.right, y))
        drawPath(
            Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
            },
            shade,
            style = rule,
        )
    }
}

/**
 * The rubber: a real eraser, drawn the way one sits in a box, with the paper
 * sleeve a new one comes in. The rubber end shows above the sleeve, which is
 * the half that touches the paper and the half that wears away.
 *
 * [armed] fills the sleeve dark, which is the same language the whole app
 * speaks: an object in the child's hand is drawn solid, an object waiting on
 * the tray is drawn open. That is also why the rubber needs no other
 * selection mark.
 */
@Composable
fun EraserGlyph(
    armed: Boolean,
    modifier: Modifier = Modifier,
) {
    val ink = CrayonerColors.Ink
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val line = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // The whole eraser, upright and tilted a little: a block with squared
        // corners, because this is not a crayon and the difference has to read
        // at the size of a fingertip.
        val body = Path().apply {
            moveTo(w * 0.26f, h * 0.94f)
            lineTo(w * 0.16f, h * 0.12f)
            lineTo(w * 0.70f, h * 0.06f)
            lineTo(w * 0.84f, h * 0.84f)
            close()
        }
        drawPath(body, CrayonerColors.Card)
        // The mineral end above the sleeve: the part that erases.
        val bare = Path().apply {
            moveTo(w * 0.16f, h * 0.12f)
            lineTo(w * 0.70f, h * 0.06f)
            lineTo(w * 0.73f, h * 0.26f)
            lineTo(w * 0.19f, h * 0.33f)
            close()
        }
        drawPath(bare, CrayonerColors.Coral)
        // The paper sleeve: everything below the fold.
        val sleeve = Path().apply {
            moveTo(w * 0.19f, h * 0.33f)
            lineTo(w * 0.73f, h * 0.26f)
            lineTo(w * 0.84f, h * 0.84f)
            lineTo(w * 0.26f, h * 0.94f)
            close()
        }
        drawPath(sleeve, if (armed) ink else CrayonerColors.Tape)
        drawPath(body, ink, style = line)
        drawPath(sleeve, ink, style = line)
    }
}

/** Mixes two colors in sRGB; [amount] is how much of [other] lands. */
internal fun mix(base: Color, other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = base.red + (other.red - base.red) * t,
        green = base.green + (other.green - base.green) * t,
        blue = base.blue + (other.blue - base.blue) * t,
        alpha = base.alpha + (other.alpha - base.alpha) * t,
    )
}
