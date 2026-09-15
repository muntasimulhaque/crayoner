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
 * Nothing here draws a line around the wax: a real crayon has no line around
 * it, and [outline] is only ever used to fill. The rules on the wrapper and
 * the shade on the tip and the base are the whole of a drawn crayon's
 * shading, and they come from [CrayonInk].
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

    /** Where the base's own shade starts, along the stick. */
    const val BASE_SHADE_TOP = LENGTH * 0.955

    /** Where the printed wrapper starts and ends, along the length. The
     *  wrapper starts below the cone, because a wrapper never wraps a tip. */
    const val WRAPPER_TOP = LENGTH * 0.40
    const val WRAPPER_BOTTOM = LENGTH * 0.90

    /** How heavy the wrapper's two printed rules are, in thicknesses. */
    const val RULE_WEIGHT = 0.05

    /**
     * How far in from the band's own ends the two rules run, as a share of
     * the band's length. Both rules and band are in the shape's own units,
     * so a caller can never mix a pixel distance into a shape-space band:
     * that mistake put the rules off the end of the crayon and drew two
     * floating hairs beside it (see ui/CrayonGlyph.kt).
     */
    const val RULE_INSET = 0.12

    /**
     * How far in from the crayon's own flanks each rule stops, in units of
     * the thickness.
     *
     * A rule is drawn with a round cap, and a round cap reaches half a stroke
     * past the point it is given, so a rule drawn from flank to flank paints
     * two little nubs of ink out in the paper beside the crayon. A real
     * wrapper's rules are printed inside its edges anyway: the line is on the
     * paper wound around the stick, and the paper stops short of the wax.
     * This inset is the rule's own half-width plus a hair of margin, so the
     * cap lands inside the wax at every size.
     */
    const val RULE_END_INSET = 0.045

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

    /**
     * The last sliver of the stick, at the squared base end: where a crayon
     * is a hair deeper, because the mold it came out of left the end a
     * little denser. It is why a drawn crayon reads as round rather than as
     * a block, and it is the only shading the stick gets.
     */
    fun baseBand(): Area = Area(0.0, BASE_SHADE_TOP, 1.0, LENGTH - BASE_SHADE_TOP)

    /** The collar a held crayon wears, in the same unit. */
    fun collarBand(): Area = Area(
        -COLLAR_OVERHANG,
        COLLAR_TOP,
        1.0 + COLLAR_OVERHANG * 2.0,
        COLLAR_HEIGHT,
    )

    /** True when [p] is inside the crayon's own silhouette. */
    fun contains(p: Vec2): Boolean = Poly(outline()).contains(p)

    /**
     * One point of the shape, turned by [turnDeg] about the crayon's own
     * middle, in shape units. A turn of zero is the shape as it is authored,
     * with the tip at the top of its box; a turn of a right angle lays the
     * crayon down with the tip leading.
     */
    fun turned(p: Vec2, turnDeg: Double): Vec2 =
        rotateAround(p, Vec2(0.5, LENGTH / 2.0), turnDeg)

    /**
     * The box a turned crayon needs, in shape units: its own silhouette
     * turned and measured, so a caller can size a box the whole stick fits
     * inside rather than guessing at one and clipping a corner off. A turn of
     * zero answers the shape's own box: one thickness across, [LENGTH] along.
     */
    fun turnedBounds(turnDeg: Double): Area {
        val points = outline().map { turned(it, turnDeg) }
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        return Area(minX, minY, maxX - minX, maxY - minY)
    }

    /**
     * The turn the crayon is held at: a half turn of the shape, which puts
     * the tip at the bottom of the box. Point down is how a crayon is held
     * and how a child recognizes one, and it is the stance of every crayon
     * that is not lying in the box.
     */
    const val HELD_TURN = 180.0

    /**
     * The turn that lays the crayon down, tip leading to the right, the way
     * it rests in the tray of a real box.
     */
    const val LYING_TURN = 90.0

    /**
     * How far the app's own mark leans from point down, in degrees. It is the
     * launcher icon's own lean, and it is the one angle every standing crayon
     * in the app is drawn at.
     *
     * The angle is a held crayon's, not a laid-down one. At a small lean the
     * stick stands to attention beside the name, which reads as a stick and
     * not as a crayon in a hand; at a large one it lies almost on its side
     * and reads as a stick that has fallen over. This is the lean of the hand
     * that is holding it: enough that the tip trails the base the way a
     * crayon does while it is being drawn with, and not so much that the
     * crayon stops standing up.
     */
    const val MARK_LEAN = 26.0

    /**
     * The turn the app's mark is drawn at: held, and leaning [MARK_LEAN] to
     * the right, so the tip sits low and to the left and the base is up and
     * to the right, the way a right hand holds a crayon to draw a line that
     * runs away to the right.
     *
     * This is the app's one crayon. The launcher icon draws this turn, the
     * wall's nameplate draws this turn, and the capsule's crayon is this
     * turn, so the mark on the home screen, the mark beside the app's name
     * and the crayon in the child's hand are one object at one angle.
     */
    const val MARK_TURN = HELD_TURN + MARK_LEAN
}
