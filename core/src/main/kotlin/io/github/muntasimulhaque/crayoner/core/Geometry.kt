package io.github.muntasimulhaque.crayoner.core

import kotlin.math.hypot

/**
 * A point in page space: x to the right, y down, both in the unit square.
 * Every picture in the book is authored in that square, so one definition
 * stays crisp from a small phone card to a ten inch tablet page.
 */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(k: Double) = Vec2(x * k, y * k)
    val length: Double get() = hypot(x, y)
}

/** An axis-aligned rectangle in page space. */
data class Area(val x: Double, val y: Double, val w: Double, val h: Double) {
    val right: Double get() = x + w
    val bottom: Double get() = y + h
    val center: Vec2 get() = Vec2(x + w / 2.0, y + h / 2.0)
    val area: Double get() = (w.coerceAtLeast(0.0)) * (h.coerceAtLeast(0.0))

    fun contains(p: Vec2): Boolean = p.x in x..right && p.y in y..bottom

    fun union(other: Area): Area {
        val x0 = minOf(x, other.x)
        val y0 = minOf(y, other.y)
        val x1 = maxOf(right, other.right)
        val y1 = maxOf(bottom, other.bottom)
        return Area(x0, y0, x1 - x0, y1 - y0)
    }

    companion object {
        val Unit: Area = Area(0.0, 0.0, 1.0, 1.0)
    }
}

/** Degrees to radians, the one conversion every rotated shape needs. */
internal fun Double.toRadians(): Double = this * Math.PI / 180.0

/** Rotates [p] around [pivot] by [angleDeg] degrees, clockwise on screen. */
internal fun rotateAround(p: Vec2, pivot: Vec2, angleDeg: Double): Vec2 {
    if (angleDeg == 0.0) return p
    val a = angleDeg.toRadians()
    val cos = Math.cos(a)
    val sin = Math.sin(a)
    val dx = p.x - pivot.x
    val dy = p.y - pivot.y
    return Vec2(pivot.x + dx * cos - dy * sin, pivot.y + dx * sin + dy * cos)
}

/**
 * A smooth polyline through [points], using Catmull-Rom sampling. Pictures
 * are drawn with this for hills, waves, sails and clouds, so organic edges
 * are a function of a few control points rather than hundreds of hand typed
 * coordinates.
 */
fun smooth(points: List<Vec2>, samples: Int = 14, closed: Boolean = false): Poly =
    Poly(smoothPoints(points, samples, closed))

/**
 * The sampled points of a smooth polyline through [points]. Grounds, waves
 * and sails are built from this, then closed into a band or a shape.
 */
fun smoothPoints(points: List<Vec2>, samples: Int = 14, closed: Boolean = false): List<Vec2> {
    require(points.size >= 3) { "A smooth shape needs at least 3 control points" }
    val n = points.size
    val out = ArrayList<Vec2>(n * samples + 1)
    val lastSegment = if (closed) n - 1 else n - 2
    for (i in 0..lastSegment) {
        val p0 = points[if (closed) (i - 1 + n) % n else (i - 1).coerceAtLeast(0)]
        val p1 = points[i % n]
        val p2 = points[(i + 1) % n]
        val p3 = points[if (closed) (i + 2) % n else (i + 2).coerceAtMost(n - 1)]
        for (s in 0 until samples) {
            val t = s.toDouble() / samples
            out += catmullRom(p0, p1, p2, p3, t)
        }
    }
    out += if (closed) points[0] else points[n - 1]
    return out
}

private fun catmullRom(p0: Vec2, p1: Vec2, p2: Vec2, p3: Vec2, t: Double): Vec2 {
    val t2 = t * t
    val t3 = t2 * t
    fun axis(a: Double, b: Double, c: Double, d: Double): Double = 0.5 * (
        2.0 * b + (-a + c) * t + (2.0 * a - 5.0 * b + 4.0 * c - d) * t2 +
            (-a + 3.0 * b - 3.0 * c + d) * t3
        )
    return Vec2(axis(p0.x, p1.x, p2.x, p3.x), axis(p0.y, p1.y, p2.y, p3.y))
}
