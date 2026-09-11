package io.github.muntasimulhaque.crayoner.core

/**
 * The crayon box. One box, the same box, for every picture in the book:
 * a real box of crayons does not change its contents because of the page
 * you open. The child reaches for red because red is where red always is.
 *
 * The thirty-two colors are the ones in the real thirty-two count box, in
 * the order the box itself lays them out: the sixteen a child meets first,
 * then the eight the twenty-four adds, then the eight the thirty-two adds.
 * Every hex is the printed crayon's own color, taken from Crayola's
 * published values (Red #EE204D, Scarlet #FC2847, Red Orange #FF5349,
 * Orange #FF7538, Yellow Orange #FFAE42, Apricot #FDD9B5, Yellow #FCE883,
 * Green Yellow #F0E891, Yellow Green #C5E384, Green #1CAC78, Blue Green
 * #0D98BA, Cerulean #1DACD6, Sky Blue #76D7EA, Blue #1F75FE, Bluetiful
 * #3C88EE, Indigo #5D76CB, Blue Violet #7366BD, Violet #926EAE, Wisteria
 * #C9A0DC, Red Violet #C0448F, Violet Red #F75394, Carnation Pink #FFAACC,
 * Melon #FEBAAD, Peach #FFCBA4, Tan #D99A6C, Brown #B4674D, Chestnut
 * #B94E48, Gray #95918C, Timberwolf #DBD7D2, Cadet Blue #B0B7C6, Black
 * #000000, White #FFFFFF).
 *
 * Nothing here is softened toward paper, and that is the whole point: a
 * crayon over a paper tooth leaves crumbly edges and bare paper between its
 * passes, but the pigment it does lay down is the stick's own color. A
 * lightened palette is not a softer crayon, it is faded wax, which is what
 * this file used to carry.
 *
 * A picture's own colors are drawn from this box, so every area of every
 * picture can always be matched exactly. [INK] is not a crayon: it is the
 * line the pictures are printed with, and it belongs to the paper.
 */
object Crayons {

    const val RED: Long = 0xFFEE204D
    const val SCARLET: Long = 0xFFFC2847
    const val RED_ORANGE: Long = 0xFFFF5349
    const val ORANGE: Long = 0xFFFF7538
    const val YELLOW_ORANGE: Long = 0xFFFFAE42
    const val APRICOT: Long = 0xFFFDD9B5
    const val YELLOW: Long = 0xFFFCE883
    const val GREEN_YELLOW: Long = 0xFFF0E891
    const val YELLOW_GREEN: Long = 0xFFC5E384
    const val GREEN: Long = 0xFF1CAC78
    const val BLUE_GREEN: Long = 0xFF0D98BA
    const val CERULEAN: Long = 0xFF1DACD6
    const val SKY_BLUE: Long = 0xFF76D7EA
    const val BLUE: Long = 0xFF1F75FE
    const val BLUETIFUL: Long = 0xFF3C88EE
    const val INDIGO: Long = 0xFF5D76CB
    const val BLUE_VIOLET: Long = 0xFF7366BD
    const val VIOLET: Long = 0xFF926EAE
    const val WISTERIA: Long = 0xFFC9A0DC
    const val RED_VIOLET: Long = 0xFFC0448F
    const val VIOLET_RED: Long = 0xFFF75394
    const val CARNATION_PINK: Long = 0xFFFFAACC
    const val MELON: Long = 0xFFFEBAAD
    const val PEACH: Long = 0xFFFFCBA4
    const val TAN: Long = 0xFFD99A6C
    const val BROWN: Long = 0xFFB4674D
    const val CHESTNUT: Long = 0xFFB94E48
    const val GRAY: Long = 0xFF95918C
    const val TIMBERWOLF: Long = 0xFFDBD7D2
    const val CADET_BLUE: Long = 0xFFB0B7C6
    const val BLACK: Long = 0xFF000000
    const val WHITE: Long = 0xFFFFFFFF

    /** The ink every picture's lines are drawn in. Dark, never black. */
    const val INK: Long = 0xFF3B4351

    /** The paper a picture sits on, and the color of an unfilled area. */
    const val PAPER: Long = 0xFFFFFDF8

    /**
     * The whole box, in the order a child lifts the lid on it: the warm
     * colors first, then the greens and blues, then the quiet ones. Thirty
     * two crayons, and the same thirty two on every page, in the same
     * places, forever.
     */
    val all: List<Long> = listOf(
        RED, SCARLET, RED_ORANGE, ORANGE, YELLOW_ORANGE, APRICOT, YELLOW, GREEN_YELLOW,
        YELLOW_GREEN, GREEN, BLUE_GREEN, CERULEAN, SKY_BLUE, BLUE, BLUETIFUL, INDIGO,
        BLUE_VIOLET, VIOLET, WISTERIA, RED_VIOLET, VIOLET_RED, CARNATION_PINK, MELON, PEACH,
        TAN, BROWN, CHESTNUT, GRAY, TIMBERWOLF, CADET_BLUE, BLACK, WHITE,
    )

    /** True when [argb] is a real crayon from this box. */
    fun exists(argb: Long): Boolean = all.contains(argb)
}
