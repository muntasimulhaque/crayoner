package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * How small a part of a picture may be.
 *
 * A coloring book for three year olds is colored by a finger, and a finger is
 * about as wide as a fingertip: the crayon's own tip is [PageFinger.TIP] of
 * the page, and a mark cannot be aimed at anything smaller than the wax it
 * lays down. So every area of every picture has to contain a circle at least
 * that wide ([FLOOR]), measured as the widest circle that fits inside the
 * area's own outline.
 *
 * The rule is here because it was violated by pictures that looked fine on a
 * review sheet and could not be colored in a hand: sprinkles the width of a
 * grain of rice, a rainstorm of hairlines, stars that were specks. The fix is
 * never to draw the speck smaller, because a sprinkle drawn large stops being
 * a sprinkle; it is to take the part out of the picture, or to make the thing
 * the part belonged to bigger. What a page must not do is print a region a
 * child can see and cannot touch.
 *
 * The measurement is exact rather than approximate: for every candidate
 * point of the area, the radius of the largest circle that fits inside is the
 * distance to the nearest point outside, and the largest of those is found by
 * a squared-distance transform. Sampling every area on a grid a few hundred
 * cells across costs a second of test time and catches every real case.
 */
class FingerTest {

    @Test
    fun everyAreaIsBigEnoughForAFinger() {
        val tooSmall = ArrayList<String>()
        for (page in Pages.all) {
            for (region in page.regions) {
                if (page.isGround(page.indexOfRegion(region.id))) continue
                val width = widestInside(region)
                if (width < FLOOR) {
                    tooSmall += "${page.id}/${region.id} is ${"%.3f".format(width)} wide"
                }
            }
        }
        assertTrue(
            "these areas are too small for a fingertip to color (" +
                "the floor is ${"%.3f".format(FLOOR)} of the page): $tooSmall",
            tooSmall.isEmpty(),
        )
    }

    @Test
    fun theMeasurementAgreesWithShapesWeKnow() {
        // A rule about drawing is worth nothing if the ruler is wrong, so the
        // ruler is held to shapes whose answer can be worked out by hand.
        val cases = listOf(
            Triple("a band 0.10 wide", 0.10, Region("a", "a", 0L, listOf(rect(0.0, 0.0, 0.5, 0.10)))),
            Triple("a band 0.20 wide", 0.20, Region("b", "b", 0L, listOf(rect(0.0, 0.0, 0.5, 0.20)))),
            Triple("a circle of radius 0.05", 0.099, Region("c", "c", 0L, listOf(circle(0.5, 0.5, 0.05)))),
            Triple(
                "two circles of radius 0.05", 0.099,
                Region("d", "d", 0L, listOf(circle(0.3, 0.5, 0.05), circle(0.7, 0.5, 0.05))),
            ),
        )
        for ((name, expected, region) in cases) {
            val measured = widestInside(region)
            assertTrue(
                "$name measured ${"%.4f".format(measured)}, expected about $expected",
                kotlin.math.abs(measured - expected) < 0.008,
            )
        }
    }

    /**
     * The widest circle that fits inside [region], in page widths: the
     * distance from the region's own best point out to the nearest place the
     * region stops.
     */
    private fun widestInside(region: Region): Double {
        val b = region.bounds
        if (b.w <= 0.0 || b.h <= 0.0) return 0.0
        val cell = min(b.w, b.h) / GRID
        // A skirt of cells around the region's own box, so the distance
        // transform has an outside to measure against on every side.
        val pad = 2
        val cols = max(3, (b.w / cell).toInt() + 1) + pad * 2
        val rows = max(3, (b.h / cell).toInt() + 1) + pad * 2
        val f = Array(rows) { j ->
            DoubleArray(cols) { i ->
                val x = b.x + (i - pad + 0.5) * cell
                val y = b.y + (j - pad + 0.5) * cell
                if (region.contains(Vec2(x, y))) Double.MAX_VALUE / 4.0 else 0.0
            }
        }
        for (j in 0 until rows) transform(f[j])
        val g = Array(cols) { i -> DoubleArray(rows) { j -> f[j][i] } }
        for (i in 0 until cols) transform(g[i])
        var best = 0.0
        for (j in 0 until rows) for (i in 0 until cols) best = max(best, g[i][j])
        return Math.sqrt(best) * cell * 2.0
    }

    /** Felzenszwalb and Huttenlocher's exact 1D squared distance transform. */
    private fun transform(f: DoubleArray) {
        val n = f.size
        val d = DoubleArray(n)
        val v = IntArray(n)
        val z = DoubleArray(n + 1)
        var k = 0
        v[0] = 0
        z[0] = -1e20
        z[1] = 1e20
        for (q in 1 until n) {
            var s = (f[q] + q.toDouble() * q - (f[v[k]] + v[k].toDouble() * v[k])) /
                (2.0 * q - 2.0 * v[k])
            while (s <= z[k]) {
                k--
                s = (f[q] + q.toDouble() * q - (f[v[k]] + v[k].toDouble() * v[k])) /
                    (2.0 * q - 2.0 * v[k])
            }
            k++
            v[k] = q
            z[k] = s
            z[k + 1] = 1e20
        }
        k = 0
        for (q in 0 until n) {
            while (z[k + 1] < q) k++
            val x = q - v[k]
            d[q] = x.toDouble() * x + f[v[k]]
        }
        d.copyInto(f)
    }

    private companion object {
        /**
         * The floor every area is held to, in page widths. It is a shade
         * under the crayon's own tip ([PageFinger.TIP]), because a fingertip
         * that lands in a space as wide as the wax it lays down has colored
         * that part of the picture and nothing more is owed.
         */
        const val FLOOR = 0.070

        /** How many cells the smallest side of an area is walked in. */
        const val GRID = 220
    }
}
