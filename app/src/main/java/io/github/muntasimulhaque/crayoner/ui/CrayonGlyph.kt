package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Area
import io.github.muntasimulhaque.crayoner.core.CrayonInk
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Vec2

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
 *
 * Nothing is drawn around the wax. A real crayon has no line around it: the
 * wrapper's edge is where the paper it is wound in ends, and the wax beside
 * it is the stick's own color, laid on thick. An outline here would be ink
 * the object does not have, and at this size the outline is most of what the
 * eye reads, which is what makes a drawn crayon look like a diagram of one.
 *
 * So the stick is made only of wax: the body's own color (which the caller
 * passes, because the color is the crayon), the cone a hair deeper where the
 * light leaves the tip, the wrapper the same wax taken deeper again, and the
 * wrapper's two rules deeper still. All four come out of [CrayonInk], so a
 * crayon is the same color at every size and in every place, and the rules
 * that keep it from being a pencil live in one file in :core.
 *
 * [contact] draws the paper collar a held crayon wears, and is reserved for
 * the crayon that is actually in the hand.
 */
@Composable
fun CrayonGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    contact: Boolean = false,
    lying: Boolean = false,
    lineBoost: Float = 1f,
) {
    val inks = remember(color) { CrayonPaints.of(color) }
    Canvas(modifier = modifier) {
        val length = if (lying) size.width else size.height
        drawCrayonShape(
            wax = color,
            inks = inks,
            contact = contact,
            left = 0f,
            top = 0f,
            length = length,
            lying = lying,
            lineBoost = lineBoost,
        )
    }
}

/** The four colors a drawn crayon is made of, from one wax. */
internal class CrayonPaints(
    val wrapper: Color,
    val cone: Color,
    val rule: Color,
    val base: Color,
) {
    companion object {
        fun of(wax: Color): CrayonPaints {
            val argb = argbOf(wax)
            return CrayonPaints(
                wrapper = Color(CrayonInk.wrapper(argb)),
                cone = Color(CrayonInk.cone(argb)),
                rule = Color(CrayonInk.rule(argb)),
                base = Color(CrayonInk.base(argb)),
            )
        }

        private fun argbOf(color: Color): Long =
            ((color.alpha * 255).toLong() shl 24) or
                ((color.red * 255).toLong() shl 16) or
                ((color.green * 255).toLong() shl 8) or
                (color.blue * 255).toLong()
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
 * squared base. This function only decides where the pieces land and which
 * of the four waxes paints each one.
 */
internal fun DrawScope.drawCrayonShape(
    wax: Color,
    inks: CrayonPaints,
    contact: Boolean,
    left: Float,
    top: Float,
    length: Float,
    lying: Boolean,
    lineBoost: Float = 1f,
) {
    // The shape is measured in the crayon's own thicknesses, so one unit is
    // the thickness on both axes and the crayon cannot be stretched.
    val unit = length / CrayonShape.LENGTH.toFloat()
    // One mapping for the whole drawing: a point in the shape's own unit box
    // to a point on the canvas, in whichever way the crayon is turned.
    fun px(p: Vec2): Offset = if (lying) {
        Offset(left + length - p.y.toFloat() * unit, top + p.x.toFloat() * unit)
    } else {
        Offset(left + p.x.toFloat() * unit, top + p.y.toFloat() * unit)
    }
    val silhouette = outlinePath(::px)
    drawPath(silhouette, wax)
    drawPath(conePath(::px), inks.cone)
    val band = CrayonShape.wrapperBand()
    drawPath(rectPath(::px, band.x, band.y, band.right, band.bottom), inks.wrapper)
    drawWrapperRules(::px, band, inks.rule, unit * lineBoost)
    val base = CrayonShape.baseBand()
    drawPath(rectPath(::px, base.x, base.y, base.right, base.bottom), inks.base.copy(alpha = 0.72f))
    if (contact) {
        // The picked crayon wears a paper collar, the way a held crayon is
        // banded by a hand, so the choice reads at a glance even when two
        // colors are hard to tell apart.
        val collar = CrayonShape.collarBand()
        drawPath(rectPath(::px, collar.x, collar.y, collar.right, collar.bottom), CrayonerColors.Card)
    }
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

/** The cone alone, so it can be a shade deeper than the body. */
private fun conePath(px: (Vec2) -> Offset): Path = Path().apply {
    val points = CrayonShape.outline()
    moveTo(px(points[0]).x, px(points[0]).y)
    for (point in points) {
        if (point.y > CrayonShape.TIP_LENGTH + 1e-9) break
        val o = px(point)
        lineTo(o.x, o.y)
    }
    // Back across the shoulders, so the cone's own band closes on itself.
    val left = px(Vec2(0.0, CrayonShape.TIP_LENGTH))
    val right = px(Vec2(1.0, CrayonShape.TIP_LENGTH))
    lineTo(right.x, right.y)
    lineTo(left.x, left.y)
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
 * The two rules every real wrapper wears, one above the label and one below
 * it. They follow the crayon's own turn, so a lying crayon's rules run across
 * it and a standing one's run down it.
 *
 * They are drawn with round caps at a width that follows the crayon, so a
 * crayon twenty pixels wide wears two fine lines and a crayon two hundred
 * pixels wide wears two lines in proportion: the wrapper is printed the same
 * way at every size.
 */
private fun DrawScope.drawWrapperRules(
    px: (Vec2) -> Offset,
    band: Area,
    rule: Color,
    unit: Float,
) {
    val line = Stroke((unit * 0.055f).coerceAtLeast(0.7f), cap = StrokeCap.Round)
    val near = unit * 0.070f
    for (y in listOf(band.y + near, band.bottom - near)) {
        val a = px(Vec2(band.x, y))
        val b = px(Vec2(band.right, y))
        drawPath(
            Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
            },
            rule,
            style = line,
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

/**
 * The undo mark, drawn on the thing that takes the last mark back: one turn
 * of a hand's own stroke running back on itself, with a chunky head where
 * the arrow is leaving from.
 *
 * It is deliberately not a line of type and not a stock glyph: it is one
 * stroke of wax in the same coral the brand is, with the small wobble of a
 * hand. The head is a solid triangle rather than two bristles, because at
 * the size of a seat a pair of thin strokes beside a thin arc reads as a
 * broken circle and says nothing about direction; a solid head reads as an
 * arrow from across the room.
 */
@Composable
fun UndoGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = 24.dp) {
    val side = size
    Canvas(modifier = modifier.size(side)) {
        val w = side.toPx()
        // The arc: over the top, down the right, and back along the bottom,
        // leaving the head its own room at the left.
        val stroke = Stroke(w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val sweep = Path().apply {
            moveTo(w * 0.26f, w * 0.32f)
            cubicTo(w * 0.46f, w * 0.04f, w * 0.88f, w * 0.16f, w * 0.84f, w * 0.54f)
            cubicTo(w * 0.80f, w * 0.86f, w * 0.48f, w * 0.94f, w * 0.30f, w * 0.82f)
        }
        drawPath(sweep, color, style = stroke)
        // The head: one solid wedge, point outward at the left, so the mark
        // says which way the step goes.
        val head = Path().apply {
            moveTo(w * 0.05f, w * 0.22f)
            lineTo(w * 0.37f, w * 0.10f)
            lineTo(w * 0.30f, w * 0.46f)
            close()
        }
        drawPath(head, color)
    }
}
