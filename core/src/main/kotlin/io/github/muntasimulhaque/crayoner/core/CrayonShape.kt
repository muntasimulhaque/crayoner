package io.github.muntasimulhaque.crayoner.core

import kotlin.math.cos
import kotlin.math.sin

/**
 * One crayon, as geometry, in one place.
 *
 * The launcher icon draws this crayon, the tray draws this crayon, and the
 * review sheet draws this crayon, so there is exactly one crayon in the
 * project and no renderer can quietly invent its own idea of one. It is the
 * app's own object, at the app's own proportions, which are the real
 * object's proportions where they can be:
 *
 * - The body is about three times as long as it is thick. A real crayon is
 *   nearer twelve, but a twelve to one crayon in a tray cell as tall as a
 *   fingertip is a wire; this is the stub a child actually holds.
 * - The cone at the tip is as long as the crayon is thick, which is what a
 *   real crayon's cone is, and it ends in a small flat nose with rounded
 *   corners rather than a point. A point is a pencil, and this app must
 *   never draw a pencil.
 * - The base is squared off, with only a hint of softness at its corners.
 * - The wrapper is the crayon's own wax, barely lightened, with two dark
 *   rules, the way a real wrapper is printed. A pale sleeve would be a
 *   pencil; a plain wax body would be a marker.
 *
 * ## The unit
 *
 * Every number here is in units of the crayon's own thickness, which is the
 * one measurement a drawing of a crayon can be scaled by without coming out
 * fatter or thinner than a crayon. So the shape spans x from 0 to 1 (one
 * thickness across) and y from 0 to [LENGTH] (the whole crayon along), with
 * the tip at y = 0 and the base at y = [LENGTH]. A caller who wants a crayon
 * [thickness] pixels thick draws this box at exactly that scale on both
 * axes, and the crayon cannot come out wrong.
 *
 * [AspectTest] holds those bounds, so a change to any proportion that breaks
 * the unit is caught before it reaches a screen.
 */
object CrayonShape {

    /** The body's thickness, as a fraction of the crayon's whole length. */
    const val THICKNESS = 0.319

    /** The whole crayon, in units of its own thickness. */
    const val LENGTH = 1.0 / THICKNESS

    /** The cone's length, in units of the crayon's thickness. */
    const val TIP_LENGTH = 1.0

    /** The tip's nose, in units of the thickness: blunt, never a point. */
    const val NOSE = 0.19

    /** The softness of the base's corners, in units of the thickness. */
    const val BASE_SOFT = 0.06

    /** The base of the body, a hair above the crayon's own end. */
    const val BASE = LENGTH * 0.97

    /** Where the printed wrapper starts and ends, along the length. The
     *  wrapper starts below the cone, because a wrapper never wraps a tip. */
    const val WRAPPER_TOP = LENGTH * 0.40
    const val WRAPPER_BOTTOM = LENGTH * 0.90

    /** The ink line, in units of the thickness. */
    const val LINE = 0.11

    /** The paper collar a held crayon wears: its top and its height. */
    const val COLLAR_TOP = LENGTH * 0.48
    const val COLLAR_HEIGHT = LENGTH * 0.16

    /** The collar's overhang past the body, in units of the thickness. */
    const val COLLAR_OVERHANG = 0.16

    /** The collar's own corners, in units of the thickness. */
    const val COLLAR_ROUND = 0.08

    private const val NOSE_STEPS = 6

    /**
     * The crayon's silhouette, as a closed ring of points with the tip at
     * y = 0. Straight flanks on the cone, a rounded nose, squared base
     * corners; the nose is sampled rather than curved so both renderers walk
     * the same list of points and draw the same shape.
     */
    fun outline(): List<Vec2> {
        val cx = 0.5
        val shoulder = TIP_LENGTH
        // The nose sits where a semicircle of the nose's own width, a hair
        // flattened, comes down to meet the two flanks.
        val noseTop = NOSE * 0.9
        val points = ArrayList<Vec2>(12)
        points += Vec2(cx - 0.5, shoulder)
        points += Vec2(cx - NOSE, noseTop)
        // The nose: a half turn of wax over the top, a hair flatter than a
        // semicircle, which is what makes it read as pressed wax rather than
        // as a spike.
        for (i in 1 until NOSE_STEPS) {
            val a = Math.PI * (1.0 - i.toDouble() / NOSE_STEPS)
            points += Vec2(cx + NOSE * cos(a), noseTop - noseTop * sin(a))
        }
        points += Vec2(cx + NOSE, noseTop)
        points += Vec2(cx + 0.5, shoulder)
        points += Vec2(cx + 0.5, BASE)
        // The base: two soft corners and a flat bottom, the way a crayon
        // comes out of its mold.
        points += Vec2(cx + 0.5 - BASE_SOFT, LENGTH)
        points += Vec2(cx - 0.5 + BASE_SOFT, LENGTH)
        points += Vec2(cx - 0.5, BASE)
        return points
    }

    /** The wrapper's band, in the same unit. */
    fun wrapperBand(): Area = Area(0.0, WRAPPER_TOP, 1.0, WRAPPER_BOTTOM - WRAPPER_TOP)

    /** The collar a held crayon wears, in the same unit. */
    fun collarBand(): Area = Area(
        -COLLAR_OVERHANG,
        COLLAR_TOP,
        1.0 + COLLAR_OVERHANG * 2.0,
        COLLAR_HEIGHT,
    )

    /** True when [p] is inside the crayon's own silhouette. */
    fun contains(p: Vec2): Boolean = Poly(outline()).contains(p)
}
