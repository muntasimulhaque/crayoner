package io.github.muntasimulhaque.crayoner.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The shapes a picture is made of. Every shape answers two questions the
 * whole app rests on: is this point inside it, and what is its bounding box.
 * Tap detection, the accessibility overlays, the structural tests and both
 * renderers (Compose on the device, Java2D in the generators) all read these
 * two methods, so a shape can never mean one thing in play and another in a
 * screenshot.
 */
sealed interface Shape {
    fun contains(p: Vec2): Boolean
    fun bounds(): Area
    /** Approximate area, used by the structural tests to refuse slivers. */
    fun area(): Double
    /** Approximate centroid, used by the tests to prove a region is visible. */
    fun centroid(): Vec2
}

/** A circle. */
data class Circ(val c: Vec2, val r: Double) : Shape {
    override fun contains(p: Vec2): Boolean {
        val dx = p.x - c.x
        val dy = p.y - c.y
        return dx * dx + dy * dy <= r * r
    }

    override fun bounds(): Area = Area(c.x - r, c.y - r, r * 2.0, r * 2.0)
    override fun area(): Double = Math.PI * r * r
    override fun centroid(): Vec2 = c
}

/** A true ellipse; [angleDeg] rotates it around its center. */
data class Ell(
    val c: Vec2,
    val rx: Double,
    val ry: Double,
    val angleDeg: Double = 0.0,
) : Shape {
    override fun contains(p: Vec2): Boolean {
        val local = rotateAround(p, c, -angleDeg)
        val dx = (local.x - c.x) / rx
        val dy = (local.y - c.y) / ry
        return dx * dx + dy * dy <= 1.0
    }

    override fun bounds(): Area {
        val a = angleDeg.toRadians()
        val halfW = sqrt((rx * cos(a)).let { it * it } + (ry * sin(a)).let { it * it })
        val halfH = sqrt((rx * sin(a)).let { it * it } + (ry * cos(a)).let { it * it })
        return Area(c.x - halfW, c.y - halfH, halfW * 2.0, halfH * 2.0)
    }

    override fun area(): Double = Math.PI * rx * ry
    override fun centroid(): Vec2 = c
}

/** A rounded rectangle; [angleDeg] rotates it around its center. */
data class RRect(
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val radius: Double,
    val angleDeg: Double = 0.0,
) : Shape {
    override fun contains(p: Vec2): Boolean {
        val cx = x + w / 2.0
        val cy = y + h / 2.0
        val local = rotateAround(p, Vec2(cx, cy), -angleDeg)
        val r = radius.coerceAtMost(minOf(w, h) / 2.0)
        val dx = abs(local.x - cx) - (w / 2.0 - r)
        val dy = abs(local.y - cy) - (h / 2.0 - r)
        if (dx <= 0.0 && dy <= 0.0) return true
        val ox = dx.coerceAtLeast(0.0)
        val oy = dy.coerceAtLeast(0.0)
        return ox * ox + oy * oy <= r * r
    }

    override fun bounds(): Area {
        if (angleDeg == 0.0) return Area(x, y, w, h)
        val cx = x + w / 2.0
        val cy = y + h / 2.0
        val corners = listOf(
            Vec2(x, y), Vec2(x + w, y), Vec2(x + w, y + h), Vec2(x, y + h),
        ).map { rotateAround(it, Vec2(cx, cy), angleDeg) }
        val minX = corners.minOf { it.x }
        val minY = corners.minOf { it.y }
        val maxX = corners.maxOf { it.x }
        val maxY = corners.maxOf { it.y }
        return Area(minX, minY, maxX - minX, maxY - minY)
    }

    override fun area(): Double {
        val r = radius.coerceAtMost(minOf(w, h) / 2.0)
        return w * h - (4.0 - Math.PI) * r * r
    }

    override fun centroid(): Vec2 = Vec2(x + w / 2.0, y + h / 2.0)
}

/**
 * A band of a ring, the shape rainbows are made of: everything between
 * [rInner] and [rOuter] within the sweep from [startDeg] to [endDeg],
 * angles measured clockwise from the positive x axis, y down. With zero
 * inner radius it is a pie slice; with a half turn it is an arch.
 */
data class ArcBand(
    val c: Vec2,
    val rInner: Double,
    val rOuter: Double,
    val startDeg: Double,
    val endDeg: Double,
) : Shape {
    private val span: Double get() {
        var span = (endDeg - startDeg) % 360.0
        if (span <= 0.0) span += 360.0
        return span
    }

    /** The sweep, in degrees, always positive. */
    fun sweepDegrees(): Double = span

    override fun contains(p: Vec2): Boolean {
        val dx = p.x - c.x
        val dy = p.y - c.y
        val r = sqrt(dx * dx + dy * dy)
        if (r < rInner || r > rOuter) return false
        var angle = Math.toDegrees(kotlin.math.atan2(dy, dx))
        if (angle < 0.0) angle += 360.0
        var start = startDeg % 360.0
        if (start < 0.0) start += 360.0
        var rel = (angle - start) % 360.0
        if (rel < 0.0) rel += 360.0
        return rel <= span
    }

    override fun bounds(): Area {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        val steps = 48
        for (i in 0..steps) {
            val deg = startDeg + span * i / steps
            val a = deg.toRadians()
            val x = c.x + rOuter * cos(a)
            val y = c.y + rOuter * sin(a)
            minX = minOf(minX, x); maxX = maxOf(maxX, x)
            minY = minOf(minY, y); maxY = maxOf(maxY, y)
        }
        return Area(minX, minY, maxX - minX, maxY - minY)
    }

    override fun area(): Double = (span / 360.0) * Math.PI * (rOuter * rOuter - rInner * rInner)

    override fun centroid(): Vec2 {
        val mid = (startDeg + span / 2.0).toRadians()
        val r = (rInner + rOuter) / 2.0
        return Vec2(c.x + r * cos(mid), c.y + r * sin(mid))
    }
}

/** A closed polygon. Winding is irrelevant: tests use the even-odd rule. */
data class Poly(val points: List<Vec2>) : Shape {
    override fun contains(p: Vec2): Boolean {
        var inside = false
        var j = points.size - 1
        for (i in points.indices) {
            val a = points[i]
            val b = points[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val t = (p.y - a.y) / (b.y - a.y)
                if (p.x < a.x + t * (b.x - a.x)) inside = !inside
            }
            j = i
        }
        return inside
    }

    override fun bounds(): Area {
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        return Area(minX, minY, maxX - minX, maxY - minY)
    }

    /** The shoelace area, always positive. */
    override fun area(): Double {
        var sum = 0.0
        var j = points.size - 1
        for (i in points.indices) {
            sum += (points[j].x + points[i].x) * (points[j].y - points[i].y)
            j = i
        }
        return abs(sum) / 2.0
    }

    override fun centroid(): Vec2 {
        var cx = 0.0
        var cy = 0.0
        var a = 0.0
        var j = points.size - 1
        for (i in points.indices) {
            val cross = points[j].x * points[i].y - points[i].x * points[j].y
            a += cross
            cx += (points[j].x + points[i].x) * cross
            cy += (points[j].y + points[i].y) * cross
            j = i
        }
        if (abs(a) < 1e-12) return points.firstOrNull() ?: Vec2(0.5, 0.5)
        return Vec2(cx / (3.0 * a), cy / (3.0 * a))
    }
}

/** True when [p] lies inside any part of this list. */
fun List<Shape>.contains(p: Vec2): Boolean = any { it.contains(p) }

/** The union of every part's bounds. */
fun List<Shape>.boundsOrNull(): Area? = fold<Shape, Area?>(null) { acc, s ->
    acc?.union(s.bounds()) ?: s.bounds()
}

/** Total area of the parts; overlapping parts count twice, fine for tests. */
fun List<Shape>.totalArea(): Double = sumOf { it.area() }

/** The area weighted centroid of the parts. */
fun List<Shape>.centroidOrNull(): Vec2? {
    val total = totalArea()
    if (total <= 0.0) return boundsOrNull()?.center
    var x = 0.0
    var y = 0.0
    for (s in this) {
        val c = s.centroid()
        x += c.x * s.area()
        y += c.y * s.area()
    }
    return Vec2(x / total, y / total)
}


