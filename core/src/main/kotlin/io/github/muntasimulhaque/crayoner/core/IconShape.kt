package io.github.muntasimulhaque.crayoner.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The one set of marks the app's round controls wear.
 *
 * Every button face in Crayoner comes from here: the house in the bar, the
 * speaker beside it, the rubber and the two steps on the capsule, and the
 * tick and the cross the save question wears. They are geometry in :core
 * rather than drawing code in a composable for the reason every other
 * drawing in this project is: one definition, so the screen, the store art
 * and the review sheets can never disagree about what a mark looks like, and
 * a mark can be measured by a test instead of judged by an eye.
 *
 * A mark lives in a unit box, from `(0,0)` to `(1,1)`, and is struck at one
 * weight ([LINE], a share of that box's own width) and drawn at one size
 * wherever it appears. The two steps are mirror images of one another, drawn
 * from the same numbers, because they are one pair and a child should learn
 * them as one thing with two directions. Every curve is sampled into a
 * polyline rather than handed to a curve call, so the device and the offline
 * renderers walk the same list of points and draw the same mark.
 *
 * The crayon is not here: it has a file of its own ([CrayonShape]), because
 * it is the app's own object rather than a control's mark.
 */
object AppIcon {

    /**
     * The weight of every outline mark in the app, as a fraction of the
     * mark's own box.
     *
     * One weight, everywhere, because a row of buttons is read as one object:
     * a house drawn at a hairline beside a rubber drawn at a marker looks
     * like two apps stacked on one screen. The fraction is chosen for the
     * smallest size a mark is ever drawn at, which is the size of a fingertip
     * on a phone: a mark that reads at 26 dp reads larger too, and the other
     * way round is not true.
     */
    const val LINE = 0.098

    /** How many steps a quarter turn of an arc is sampled into. */
    private const val ARC_STEPS = 24

    /** One mark in the app, by name. */
    enum class Name { HOME, SOUND_ON, SOUND_OFF, ERASER, UNDO, REDO, TICK, CROSS }

    /**
     * One piece of a mark: a polyline, open or closed, stroked or filled.
     * Stroked is the default, because a mark is a line the app drew.
     */
    data class Piece(
        val points: List<Vec2>,
        val closed: Boolean = false,
        val fill: Boolean = false,
    )

    /** The pieces of one mark, in paint order. */
    fun pieces(name: Name): List<Piece> = when (name) {
        Name.HOME -> home()
        Name.SOUND_ON -> sound(waves = true)
        Name.SOUND_OFF -> sound(waves = false)
        Name.ERASER -> eraser()
        Name.UNDO -> step(forward = false)
        Name.REDO -> step(forward = true)
        Name.TICK -> listOf(Piece(listOf(Vec2(0.16, 0.52), Vec2(0.40, 0.78), Vec2(0.86, 0.22))))
        Name.CROSS -> listOf(
            Piece(listOf(Vec2(0.14, 0.14), Vec2(0.86, 0.86))),
            Piece(listOf(Vec2(0.86, 0.14), Vec2(0.14, 0.86))),
        )
    }

    /**
     * The house: a roof, the walls under it, and one little door so the mark
     * reads as a home rather than as a tent.
     */
    private fun home(): List<Piece> = listOf(
        Piece(listOf(Vec2(0.16, 0.48), Vec2(0.50, 0.18), Vec2(0.84, 0.48))),
        Piece(listOf(Vec2(0.26, 0.44), Vec2(0.26, 0.82), Vec2(0.74, 0.82), Vec2(0.74, 0.44))),
        Piece(roundedRect(0.43, 0.60, 0.14, 0.22, 0.05), closed = true),
    )

    /**
     * The speaker: the cone every child already knows, with two waves leaving
     * it while the sound is on and a cross through it while it is off. The
     * waves are open arcs at the mark's own weight, which is what makes an
     * off speaker and an on speaker one object in two states rather than two
     * different buttons.
     */
    private fun sound(waves: Boolean): List<Piece> {
        val cone = Piece(
            listOf(
                Vec2(0.15, 0.40), Vec2(0.30, 0.40), Vec2(0.49, 0.19),
                Vec2(0.49, 0.81), Vec2(0.30, 0.60), Vec2(0.15, 0.60),
            ),
            closed = true,
        )
        if (!waves) {
            val arm = 0.105
            val cx = 0.755
            val cy = 0.5
            return listOf(
                cone,
                Piece(listOf(Vec2(cx - arm, cy - arm), Vec2(cx + arm, cy + arm))),
                Piece(listOf(Vec2(cx + arm, cy - arm), Vec2(cx - arm, cy + arm))),
            )
        }
        val hub = Vec2(0.49, 0.5)
        return listOf(
            cone,
            Piece(arc(hub, 0.175, -52.0, 104.0)),
            Piece(arc(hub, 0.315, -58.0, 116.0)),
        )
    }

    /**
     * The rubber lying on the desk: a squared block on its own edge, with the
     * two printed rules a wrapped sleeve wears across its near end. The rules
     * are the whole of what says rubber rather than eraser block, which is why
     * there are two of them and why neither reaches the block's own edges.
     *
     * It is a line drawing like every other mark in the app: the block is not
     * filled, so a seat that is picked up can show the capsule's own cardboard
     * through it without the object painting itself a second color. Nothing
     * about a rubber changes when a hand picks it up (see D-059).
     */
    private fun eraser(): List<Piece> = listOf(
        Piece(block(cx = 0.5, cy = 0.5, len = 0.42, wid = 0.62, angleDeg = -14.0), closed = true),
        Piece(rule(cx = 0.5, cy = 0.47, len = 0.42, half = 0.86, angleDeg = -14.0)),
        Piece(rule(cx = 0.5, cy = 0.71, len = 0.42, half = 0.86, angleDeg = -14.0)),
    )

    /**
     * One step of the walk: an open ring with a wedge riding its own end,
     * pointing the way the paper is being moved.
     *
     * The head sits at the top of the ring pointing left, which is the
     * gesture every child has already seen: the picture is being wound back.
     * The head's base is radial, the way an arrowhead's is, so it sits across
     * the thickness of the stroke it belongs to, and the tip leads out of it
     * on the side the step means.
     *
     * The step forward is that same mark reflected about its own middle,
     * whole and unchanged. The reflection is drawn point by point rather than
     * applied as a transform, so the two arrows are exactly the same weight
     * at every size, and the ring is always wound the same way: a mark built
     * by walking its own ring backwards and then reflecting the result is
     * reflected twice and points the way it started, which is the bug this
     * function is shaped to make impossible.
     */
    private fun step(forward: Boolean): List<Piece> {
        val c = Vec2(0.5, 0.5)
        val r = 0.30
        // The head, at the top of the ring.
        val headDeg = 270.0
        // The gap the head sits in. The ring runs from just past the head all
        // the way round to the head's own base, so the stroke arrives at the
        // head from the far side and no arc shows through the wedge.
        val gapDeg = 86.0
        val ring = arc(c, r, headDeg + 360.0 - gapDeg, -(360.0 - gapDeg))
        val ang = Math.toRadians(headDeg)
        val hx = c.x + r * cos(ang)
        val hy = c.y + r * sin(ang)
        // The direction the stroke is travelling where it ends, which is the
        // direction the head points.
        val dirX = sin(ang)
        val dirY = -cos(ang)
        // The head: a wedge as long as it is broad, with its base across the
        // stroke and its tip leading out of it.
        val long = 0.20
        val broad = 0.115
        val perpX = -dirY
        val perpY = dirX
        val wedge = Piece(
            listOf(
                Vec2(hx + dirX * long, hy + dirY * long),
                Vec2(hx + perpX * broad, hy + perpY * broad),
                Vec2(hx - perpX * broad, hy - perpY * broad),
            ),
            closed = true,
            fill = true,
        )
        val pieces = listOf(Piece(ring), wedge)
        return if (forward) pieces.map { mirrored(it) } else pieces
    }

    /** One piece reflected about the mark's own middle. */
    private fun mirrored(piece: Piece): Piece =
        piece.copy(points = piece.points.map { Vec2(1.0 - it.x, it.y) })

    /** One point of a circle, in the mark's own box, y down. */
    private fun pointAt(c: Vec2, r: Double, deg: Double): Vec2 {
        val a = Math.toRadians(deg)
        return Vec2(c.x + r * cos(a), c.y + r * sin(a))
    }

    /** A sampled arc, always walked the same way, so both renderers agree. */
    private fun arc(c: Vec2, r: Double, startDeg: Double, sweepDeg: Double): List<Vec2> {
        val steps = (ARC_STEPS * abs(sweepDeg) / 90.0).toInt().coerceAtLeast(2)
        return (0..steps).map { i -> pointAt(c, r, startDeg + sweepDeg * i / steps) }
    }

    /**
     * The block itself: a rectangle with square corners, turned about its own
     * middle. [len] runs along the block, [wid] across it.
     */
    private fun block(
        cx: Double,
        cy: Double,
        len: Double,
        wid: Double,
        angleDeg: Double,
    ): List<Vec2> = corners(cx, cy, len, wid).map { rotateAround(it, Vec2(cx, cy), angleDeg) }

    /**
     * One rule across the block, a share of the block's own width in from its
     * ends, so the two rules sit inside the sleeve rather than on its seams.
     */
    private fun rule(cx: Double, cy: Double, len: Double, half: Double, angleDeg: Double): List<Vec2> {
        val span = len / 2.0 * half
        return listOf(
            Vec2(cx - span, cy),
            Vec2(cx + span, cy),
        ).map { rotateAround(it, Vec2(cx, cy), angleDeg) }
    }

    /** The four corners of an upright rectangle, in paint order. */
    private fun corners(cx: Double, cy: Double, len: Double, wid: Double): List<Vec2> = listOf(
        Vec2(cx - len / 2, cy - wid / 2),
        Vec2(cx + len / 2, cy - wid / 2),
        Vec2(cx + len / 2, cy + wid / 2),
        Vec2(cx - len / 2, cy + wid / 2),
    )

    /** A rounded rectangle, sampled so both renderers walk the same points. */
    private fun roundedRect(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        r: Double,
        steps: Int = 4,
    ): List<Vec2> {
        val radius = minOf(r, w / 2.0, h / 2.0)
        val out = ArrayList<Vec2>(steps * 4 + 4)
        val corners = listOf(
            Triple(Vec2(x + radius, y + radius), 180.0, 90.0),
            Triple(Vec2(x + w - radius, y + radius), 270.0, 90.0),
            Triple(Vec2(x + w - radius, y + h - radius), 0.0, 90.0),
            Triple(Vec2(x + radius, y + h - radius), 90.0, 90.0),
        )
        for ((center, start, sweep) in corners) {
            for (i in 0..steps) out += pointAt(center, radius, start + sweep * i / steps)
        }
        return out
    }
}
