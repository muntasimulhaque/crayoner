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
 * Two unit systems meet in here, and mixing them up is a bug that already
 * shipped once: the crayon's shape is measured in its own thicknesses, and
 * the canvas is measured in pixels. The shape's two numbers have to be
 * turned into pixels exactly once, through [px]. The rules were once inset
 * a *pixel* distance into a band measured in shape units, which put both
 * lines roughly a hundred thicknesses along the crayon and off the end of
 * it: two floating hairs beside the stick. The inset is a share of the
 * band's own ends, so it is correct at every size by construction.
 */
private fun DrawScope.drawWrapperRules(
    px: (Vec2) -> Offset,
    band: Area,
    rule: Color,
    unit: Float,
) {
    val line = Stroke((unit * 0.055f).coerceAtLeast(0.7f), cap = StrokeCap.Round)
    // Derived from the band itself, not from a pixel count: a real wrapper's
    // rules sit inside its own ends by a fixed share of the band.
    val near = band.h * 0.12
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
 * The rubber: the app's own object, drawn the way the tray draws the crayon.
 *
 * It is a block of pale rubber with a band of printed paper wound around it,
 * which is what a real one is: a squared block lying at an angle, the bare
 * eraser end at the top right, and the sleeve at the bottom left. Nothing
 * here is a crayon, and the difference has to read at the size of a
 * fingertip: the rubber is a block with corners, not a stick with a cone.
 *
 * [armed] is the whole selection mark, and it is the language the rest of
 * the app already speaks: the sleeve goes solid ink, so a child can see from
 * across the room which thing their finger is holding. It is drawn in the
 * same ink, at the same weight, as every other outline mark in the app.
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
        val line = iconStroke(w)
        // The block, lying at an angle the way a rubber sits when it is put
        // down: four square corners, and no curve anywhere on it. Its four
        // corners are named for where they sit on the object: near and far
        // along the block, low and high across it. The long axis runs from
        // the lower left to the upper right, so the eraser end is up and to
        // the right and the hand holds the near end.
        val nearLow = 0.16f to 0.70f
        val farLow = 0.72f to 0.90f
        val farHigh = 0.90f to 0.34f
        val nearHigh = 0.34f to 0.14f
        // A point of the block: [along] runs from the near end to the far
        // end and [across] from the low edge to the high edge. Bilinear, so
        // the sleeve is a real band wound on the block rather than a shape
        // that happens to overlap it, which is how it came out as a bow tie.
        fun on(along: Float, across: Float): Offset {
            val x = nearLow.first + (farLow.first - nearLow.first) * along +
                (nearHigh.first - nearLow.first) * across
            val y = nearLow.second + (farLow.second - nearLow.second) * along +
                (nearHigh.second - nearLow.second) * across
            return Offset(x * w, y * h)
        }
        fun path(points: List<Offset>): Path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            close()
        }
        val body = path(listOf(on(0f, 0f), on(1f, 0f), on(1f, 1f), on(0f, 1f)))
        // The printed sleeve, wound across the block at right angles to its
        // own length, which is where a real one is: the band is the only
        // thing on the object that says rubber rather than eraser block.
        val band = path(listOf(on(0.52f, 0f), on(1f, 0f), on(1f, 1f), on(0.52f, 1f)))
        drawPath(body, CrayonerColors.Card)
        drawPath(band, if (armed) ink else CrayonerColors.Cardboard)
        drawPath(body, ink, style = line)
        // The fold where the paper is wrapped on, at the band's own edge.
        drawLine(ink, on(0.52f, 0f), on(0.52f, 1f), line.width, StrokeCap.Round)
    }
}

/**
 * The step back: one mark comes off the paper, and the mark says which way.
 *
 * It is an arrow, because an arrow is the one shape every child already
 * knows: a stroke turning over the top and down the right, and a head at the
 * upper left pointing the way the mark leaves. The head points left and up,
 * which reads as the picture being rewound the way film rewinds.
 *
 * It is deliberately not a circle with a notch. A ring with a gap and a
 * triangle beside it is a redraw arrow only if the eye can see the gap; at
 * the size of a seat on a phone the gap closes up and the mark reads as a
 * broken circle, which says nothing at all. A stroke and a head say back to
 * a three year old who has never seen either.
 */
@Composable
fun UndoGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = 24.dp) {
    val side = size
    Canvas(modifier = modifier.size(side)) {
        val w = side.toPx()
        val line = iconStroke(w)
        // The turn of the stroke: from the head's shoulder, over the top and
        // down the right, so the mark is the last thing a hand drew.
        val turn = Path().apply {
            moveTo(w * 0.30f, w * 0.30f)
            cubicTo(w * 0.56f, w * 0.02f, w * 0.96f, w * 0.20f, w * 0.86f, w * 0.56f)
            cubicTo(w * 0.78f, w * 0.88f, w * 0.44f, w * 0.92f, w * 0.26f, w * 0.80f)
        }
        drawPath(turn, color, style = line)
        // The head: a filled wedge whose point is the direction, at the upper
        // left. Filled rather than two bristles, because two thin strokes
        // beside a thin turn read as a broken circle and say nothing.
        val head = Path().apply {
            moveTo(w * 0.03f, w * 0.46f)
            lineTo(w * 0.44f, w * 0.03f)
            lineTo(w * 0.46f, w * 0.50f)
            close()
        }
        drawPath(head, color)
    }
}
