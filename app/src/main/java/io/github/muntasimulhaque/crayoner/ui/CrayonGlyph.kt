package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
