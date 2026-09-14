package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonInk
import io.github.muntasimulhaque.crayoner.core.Crayons

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
 * Standing, the crayon is drawn point down, which is how it is held and how
 * a child recognizes one. [leanDeg] turns it about its own middle, and a
 * turned crayon is drawn in a box of its own turned bounds, so the box has
 * to be the mark's own proportion or a corner of the wrapper is clipped off.
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
    leanDeg: Double = 0.0,
    lineBoost: Float = 1f,
) {
    val inks = remember(color) { CrayonPaints.of(color) }
    Canvas(modifier = modifier) {
        drawCrayonShape(
            wax = color,
            inks = inks,
            contact = contact,
            left = 0f,
            top = 0f,
            lying = lying,
            leanDeg = leanDeg,
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
 * The rubber: the app's own object, drawn the way the tray draws the crayon.
 *
 * It is a block of pale rubber with a band of printed paper wound around it,
 * which is what a real one is: a squared block lying at an angle, the bare
 * eraser end at the top right, and the sleeve at the bottom left. Nothing
 * here is a crayon, and the difference has to read at the size of a
 * fingertip: the rubber is a block with corners, not a stick with a cone.
 *
 * The rubber itself never changes color, whether it is lying on the desk or
 * picked up. A real rubber is one object whatever its owner is doing with
 * it, and a sleeve that fills in when the tool is in hand paints the object
 * a second time: the seat's own plate already says which thing is picked
 * up, and one selection language is enough. The sleeve is the printed paper
 * wound around the block, always, in the app's one `Cardboard`.
 */
@Composable
fun EraserGlyph(
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
        drawPath(band, CrayonerColors.Cardboard)
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
fun UndoGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) {
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

/**
 * The step forward: undo's own mark, facing the other way.
 *
 * It is the same turn and the same wedge, mirrored about the mark's own
 * middle, because the two are one pair and a child should read them as one
 * pair. Two marks that meant back and forward but were drawn as different
 * shapes would be two things to learn instead of one thing with two
 * directions, and the pair sits side by side on the capsule where the eye
 * compares them directly.
 *
 * The mirror is drawn rather than applied as a transform, so the arrow's
 * stroke weight and its head are exactly the ones the step back has: a
 * flipped copy of a drawing can end up a hair narrower at the head, and at
 * the size of a seat that hair is the whole mark.
 */
@Composable
fun RedoGlyph(modifier: Modifier = Modifier, color: Color, size: Dp = IconSize) {
    val side = size
    Canvas(modifier = modifier.size(side)) {
        val w = side.toPx()
        val line = iconStroke(w)
        // The turn, mirrored: over the top and down the left.
        val turn = Path().apply {
            moveTo(w * 0.70f, w * 0.30f)
            cubicTo(w * 0.44f, w * 0.02f, w * 0.04f, w * 0.20f, w * 0.14f, w * 0.56f)
            cubicTo(w * 0.22f, w * 0.88f, w * 0.56f, w * 0.92f, w * 0.74f, w * 0.80f)
        }
        drawPath(turn, color, style = line)
        // The head, mirrored: a wedge whose point is the direction, at the
        // upper right.
        val head = Path().apply {
            moveTo(w * 0.97f, w * 0.46f)
            lineTo(w * 0.56f, w * 0.03f)
            lineTo(w * 0.54f, w * 0.50f)
            close()
        }
        drawPath(head, color)
    }
}
