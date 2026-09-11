package io.github.muntasimulhaque.crayoner.core

import kotlin.math.cos
import kotlin.math.sin

/**
 * The picture builder's toolbox. Every picture in the book is drawn with
 * these few calls, all in the unit square, which is what keeps sixteen
 * different scenes looking like one hand painted them.
 *
 * Content policy lives in the pages, and every page obeys it: only
 * inanimate things. No people, no animals, no faces, no eyes on objects,
 * ever. Warmth comes from shape and color instead.
 */

/** A plain rectangle. */
fun rect(x: Double, y: Double, w: Double, h: Double): RRect = RRect(x, y, w, h, 0.0)

/** A rounded rectangle. */
fun round(x: Double, y: Double, w: Double, h: Double, r: Double): RRect =
    RRect(x, y, w, h, r)

fun circle(cx: Double, cy: Double, r: Double): Circ = Circ(Vec2(cx, cy), r)

fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double, angleDeg: Double = 0.0): Ell =
    Ell(Vec2(cx, cy), rx, ry, angleDeg)

fun point(x: Double, y: Double): Vec2 = Vec2(x, y)

/** A polygon from x/y pairs: tri(0.0, 0.0, 1.0, 0.0, 0.5, 1.0). */
fun poly(vararg xy: Double): Poly {
    require(xy.size >= 6 && xy.size % 2 == 0) { "A polygon needs at least 3 points" }
    val points = ArrayList<Vec2>(xy.size / 2)
    var i = 0
    while (i < xy.size) {
        points += Vec2(xy[i], xy[i + 1])
        i += 2
    }
    return Poly(points)
}

/** A regular polygon, [sides] corners around a circle. */
fun ngon(cx: Double, cy: Double, r: Double, sides: Int, rotDeg: Double = 0.0): Poly {
    require(sides >= 3) { "A polygon needs at least 3 sides" }
    val points = (0 until sides).map { i ->
        val a = (rotDeg + i * 360.0 / sides).toRadians()
        Vec2(cx + r * cos(a), cy + r * sin(a))
    }
    return Poly(points)
}

/** A five pointed star, the shape of a sticker and not of a creature. */
fun star(cx: Double, cy: Double, outer: Double, inner: Double, points: Int = 5, rotDeg: Double = 0.0): Poly {
    val list = ArrayList<Vec2>(points * 2)
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = (rotDeg - 90.0 + i * 180.0 / points).toRadians()
        list += Vec2(cx + r * cos(a), cy + r * sin(a))
    }
    return Poly(list)
}

/** A puffy cloud: four puffs and a flat base, read as one region. */
fun cloud(cx: Double, cy: Double, s: Double = 1.0): List<Shape> = listOf(
    circle(cx - 0.085 * s, cy + 0.005 * s, 0.055 * s),
    circle(cx - 0.020 * s, cy - 0.030 * s, 0.075 * s),
    circle(cx + 0.060 * s, cy - 0.005 * s, 0.060 * s),
    circle(cx + 0.115 * s, cy + 0.020 * s, 0.045 * s),
    ellipse(cx + 0.010 * s, cy + 0.030 * s, 0.155 * s, 0.045 * s),
)

/** The petals of one flower: overlapping ovals around a center. */
fun flowerPetals(cx: Double, cy: Double, r: Double, count: Int = 6): List<Shape> =
    (0 until count).map { i ->
        val a = i * 360.0 / count
        val ar = a.toRadians()
        ellipse(
            cx + cos(ar) * r * 0.55,
            cy + sin(ar) * r * 0.55,
            r * 0.44,
            r * 0.30,
            a,
        )
    }

/** A wavy top edge for a band of ground or sea, left to right. */
fun wavyEdge(y: Double, amp: Double = 0.02, bumps: Int = 3, phase: Int = 0): List<Vec2> =
    (0..bumps).map { i ->
        val sign = if ((i + phase) % 2 == 0) 1.0 else -1.0
        Vec2(i.toDouble() / bumps, y + sign * amp)
    }

/** A band from a smooth left to right edge down to the bottom corners. */
fun band(edge: List<Vec2>, samples: Int = 12): Poly =
    Poly(smoothPoints(edge, samples) + listOf(Vec2(1.0, 1.0), Vec2(0.0, 1.0)))

/** A smooth closed shape through control points. */
fun blob(points: List<Vec2>, samples: Int = 12): Poly = smooth(points, samples, closed = true)

/** One tappable area: an id, the word for it, its color, and its shapes. */
fun area(id: String, kind: String, fillArgb: Long, vararg parts: Shape): Region =
    Region(id, kind, fillArgb, parts.toList())

/** One tappable area from one or more already built shape lists. */
fun areaOf(id: String, kind: String, fillArgb: Long, vararg parts: List<Shape>): Region =
    Region(id, kind, fillArgb, parts.flatMap { it })
