package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shapes every picture is built from, pinned one by one. */
class ShapeTest {

    @Test
    fun circleKnowsItself() {
        val c = circle(0.5, 0.5, 0.25)
        assertTrue(c.contains(Vec2(0.5, 0.5)))
        assertTrue(c.contains(Vec2(0.74, 0.5)))
        assertFalse(c.contains(Vec2(0.76, 0.5)))
        assertEquals(Area(0.25, 0.25, 0.5, 0.5), c.bounds())
    }

    @Test
    fun ellipseRotatesLikeARealOne() {
        val flat = ellipse(0.5, 0.5, 0.3, 0.1)
        assertTrue(flat.contains(Vec2(0.79, 0.5)))
        assertFalse(flat.contains(Vec2(0.5, 0.39)))

        val tall = ellipse(0.5, 0.5, 0.3, 0.1, 90.0)
        assertFalse(tall.contains(Vec2(0.79, 0.5)))
        assertTrue(tall.contains(Vec2(0.5, 0.39)))
    }

    @Test
    fun roundedRectHonorsItsCorners() {
        val shape = round(0.2, 0.2, 0.4, 0.4, 0.1)
        assertTrue(shape.contains(Vec2(0.4, 0.4)))
        assertTrue(shape.contains(Vec2(0.21, 0.4)))
        assertFalse(shape.contains(Vec2(0.201, 0.201)))
        assertEquals(Area(0.2, 0.2, 0.4, 0.4), shape.bounds())
    }

    @Test
    fun polygonIsEvenOddAndWindingFree() {
        val clockwise = poly(0.1, 0.1, 0.9, 0.1, 0.9, 0.9)
        val counter = poly(0.1, 0.1, 0.9, 0.9, 0.9, 0.1)
        val inside = Vec2(0.8, 0.4)
        assertTrue(clockwise.contains(inside))
        assertTrue(counter.contains(inside))
        assertFalse(clockwise.contains(Vec2(0.5, 0.8)))
        assertEquals(clockwise.area(), counter.area(), 1e-9)
    }

    @Test
    fun arcBandSweepsWhereItSaysItDoes() {
        // The top half of a ring, the rainbow's shape.
        val arch = ArcBand(Vec2(0.5, 1.0), 0.4, 0.6, 180.0, 360.0)
        assertTrue(arch.contains(Vec2(0.5, 0.5)))
        assertFalse(arch.contains(Vec2(0.5, 0.9)))
        assertFalse(arch.contains(Vec2(0.5, 0.3)))
        assertFalse(arch.contains(Vec2(0.51, 0.9)))
    }

    @Test
    fun smoothShapesStayInsideTheirControlPoints() {
        val hill = band(wavyEdge(0.7, 0.02, 3, 0))
        val b = hill.bounds()
        assertTrue(b.x >= -0.001 && b.right <= 1.001)
        assertTrue(b.bottom >= 0.999)
        assertTrue(hill.area() > 0.25)
    }

    @Test
    fun bufferHelpersSumUp() {
        val parts = listOf(circle(0.25, 0.5, 0.1), circle(0.75, 0.5, 0.1))
        assertTrue(parts.contains(Vec2(0.25, 0.5)))
        assertFalse(parts.contains(Vec2(0.5, 0.5)))
        val sum = parts.totalArea()
        assertEquals(2 * Math.PI * 0.01, sum, 1e-6)
        val centroid = parts.centroidOrNull()
        assertEquals(0.5, centroid?.x ?: 0.0, 1e-6)
    }
}
