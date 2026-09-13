package io.github.muntasimulhaque.crayoner.core

/**
 * The colors a drawn crayon is made of, derived from one number: the wax.
 *
 * A crayon is one stick of pigment and one band of paper wrapped around it,
 * so nothing about its color is a taste. Given the wax, everything else is a
 * ratio: the wrapper is that wax taken deeper, the two rules a real wrapper
 * wears are deeper again, and the cone is a hair deeper because that is
 * where the light leaves the tip. The launcher icon, the tray, the box of
 * colors and the box on the shelf all come through here, so a red crayon is
 * the same red whether it is 26 pixels wide in a hand or 200 in the app's
 * own icon.
 *
 * Two rules shape the ratios, and both of them exist to keep this a crayon
 * and not a pencil:
 *
 * - the wrapper is the wax's own color and never a lighter one. A pale
 *   sleeve around a colored stick is a pencil, and this app draws crayons;
 * - the body of the stick wears no outline at all. Real wax has no line
 *   around it, and a line drawn round a 26 pixel crayon is most of what the
 *   eye reads, which is what makes a drawn crayon look like a diagram of
 *   one.
 *
 * The rules on the wrapper are the exception, because a real wrapper really
 * does wear them, and they are what says the band is paper rather than more
 * wax.
 */
object CrayonInk {

    /** How much deeper the wrapper is than the wax it wraps. */
    const val WRAPPER_DEPTH = 0.22

    /** How much deeper the wrapper's two printed rules are. */
    const val RULE_DEPTH = 0.46

    /** How much deeper the cone is than the body, and the base end. */
    const val CONE_DEPTH = 0.10
    const val BASE_DEPTH = 0.30

    /** The wrapper around [wax]: the crayon's own color, taken deeper. */
    fun wrapper(wax: Long): Long = deepen(wax, WRAPPER_DEPTH)

    /** The two rules a wrapper wears. */
    fun rule(wax: Long): Long = deepen(wax, RULE_DEPTH)

    /** The cone at the tip: the one place the stick is shaded. */
    fun cone(wax: Long): Long = deepen(wax, CONE_DEPTH)

    /** The squared base end: a hair deeper, so the stick reads as round. */
    fun base(wax: Long): Long = deepen(wax, BASE_DEPTH)

    /**
     * [wax] mixed [amount] of the way toward the ink every picture is
     * printed in. Toward ink rather than toward black, because print is what
     * a wrapper's rules are: they are the picture's own line, printed on
     * paper, and they belong to the same family as every other line in the
     * app.
     */
    fun deepen(wax: Long, amount: Double): Long {
        val t = amount.coerceIn(0.0, 1.0)
        val a = (wax ushr 24) and 0xFF
        val r = mix((wax ushr 16) and 0xFF, (Crayons.INK ushr 16) and 0xFF, t)
        val g = mix((wax ushr 8) and 0xFF, (Crayons.INK ushr 8) and 0xFF, t)
        val b = mix(wax and 0xFF, Crayons.INK and 0xFF, t)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun mix(from: Long, to: Long, t: Double): Long =
        Math.round(from + (to - from) * t).coerceIn(0L, 255L)
}
