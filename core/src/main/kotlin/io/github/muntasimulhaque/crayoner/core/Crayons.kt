package io.github.muntasimulhaque.crayoner.core

/**
 * The crayon box. One box, the same box, for every picture in the book:
 * a real box of crayons does not change its contents because of the page
 * you open. The child reaches for red because red is where red always is.
 *
 * The colors are crayon colors, not paint colors. Wax crayons are a little
 * softer and a little warmer than acrylic or ink: they sit slightly toward
 * earth, they never reach a screen's full saturation, and none of them is
 * pure. Every one of these is chosen by hand to read as a crayon tip rather
 * than as a pixel, and the surface of a colored area is given a real wax
 * grain by the renderers (see [WaxGrain]), which is what does most of the
 * work of saying crayon.
 *
 * A picture's own colors are drawn from this box, so every area of every
 * picture can always be matched exactly. Nothing here is a pure black:
 * ink is the darkest, and it belongs to the line work.
 */
object Crayons {

    const val RED: Long = 0xFFD6423A
    const val ORANGE: Long = 0xFFE48134
    const val YELLOW: Long = 0xFFEFC33C
    const val GREEN: Long = 0xFF5FA85A
    const val FOREST: Long = 0xFF38794E
    const val TEAL: Long = 0xFF37948C
    const val SKY: Long = 0xFF77BEDC
    const val BLUE: Long = 0xFF3F6FBD
    const val NAVY: Long = 0xFF39507A
    const val PURPLE: Long = 0xFF7B5CA7
    const val LILAC: Long = 0xFFB59FD3
    const val PINK: Long = 0xFFE784AE
    const val BROWN: Long = 0xFF96603F
    const val SAND: Long = 0xFFE0C48E
    const val GRAY: Long = 0xFF949BA6
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
