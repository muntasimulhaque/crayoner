package io.github.muntasimulhaque.crayoner.core

/**
 * The crayon box. One box, the same box, for every picture in the book:
 * a real box of crayons does not change its contents because of the page
 * you open. The child reaches for red because red is where red always is.
 *
 * Every color here is a real crayon color, taken from the Crayola standard
 * list (the sixteen a child actually opens: red, red orange, orange, yellow
 * orange, yellow, yellow green, green, blue green, sky blue, blue, blue
 * violet, violet, carnation pink, brown, gray, white), then softened by six
 * percent toward paper. The softening is the medium, not a taste: wax sits
 * on top of the paper's tooth instead of soaking in, so a crayon stroke is
 * always a little lighter and a little less absolute than the stick it came
 * from. The reference hexes: Red #EE204D, Orange #FF7538, Yellow #FCE883,
 * Green #1CAC78, Blue Green #0D98BA, Sky Blue #76D7EA, Blue #1F75FE, Violet
 * #926EAE, Carnation Pink #FFAACC, Brown #B4674D, Gray #95918C.
 *
 * Two of the sixteen are not single reference colors, and each says so:
 * [SAND] is Crayola's Tan and Peach held together, and [FOREST] is Crayola's
 * Green pressed harder, which is how a crayon really makes a deeper shade.
 *
 * A picture's own colors are drawn from this box, so every area of every
 * picture can always be matched exactly. Nothing here is a pure black:
 * [INK] is the darkest, and it belongs to the line work.
 */
object Crayons {

    const val RED: Long = 0xFFEF2D57
    const val ORANGE: Long = 0xFFFF7D44
    const val YELLOW: Long = 0xFFFCE98A
    const val SAND: Long = 0xFFFDB886
    const val GREEN: Long = 0xFF2AB180
    const val FOREST: Long = 0xFF22815F
    const val TEAL: Long = 0xFF1C9EBE
    const val SKY: Long = 0xFF7ED9EB
    const val BLUE: Long = 0xFF2C7DFE
    const val NAVY: Long = 0xFF28537E
    const val PURPLE: Long = 0xFF9977B2
    const val LILAC: Long = 0xFFCCA6DE
    const val PINK: Long = 0xFFFFAFCF
    const val BROWN: Long = 0xFFB97057
    const val GRAY: Long = 0xFF9B9792
    const val WHITE: Long = 0xFFFBF8F0

    /** The ink every picture's lines are drawn in. Dark, never black. */
    const val INK: Long = 0xFF3B4351

    /** The paper a picture sits on, and the color of an unfilled area. */
    const val PAPER: Long = 0xFFFFFDF8

    /**
     * The whole box, in the order a child opens it: the warm colors first,
     * then the greens and blues, then the quiet ones. Sixteen crayons, and
     * the same sixteen on every page, in the same places, forever.
     */
    val all: List<Long> = listOf(
        RED, ORANGE, YELLOW, SAND, GREEN, FOREST, TEAL, SKY,
        BLUE, NAVY, PURPLE, LILAC, PINK, BROWN, GRAY, WHITE,
    )

    /** True when [argb] is a real crayon from this box. */
    fun exists(argb: Long): Boolean = all.contains(argb)
}
