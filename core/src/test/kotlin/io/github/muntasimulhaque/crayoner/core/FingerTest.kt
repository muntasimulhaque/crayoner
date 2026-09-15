package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * How small a part of a picture may be.
 *
 * A coloring book for three year olds is colored by a finger, and a finger is
 * about as wide as a fingertip: a mark the child lays down is wide enough
 * that a shape smaller than the finger holding the crayon cannot be aimed at,
 * only smeared over. So every separate thing a child can see in a picture has
 * to be big enough to put a finger on, measured as the widest circle that
 * fits inside it.
 *
 * The rule is per *piece*, not per region, and that is the whole point of
 * this file. A region is one named thing to a screen reader (the apples, the
 * stars, the raindrops), and measuring the region as a whole lets a big
 * sibling hide a small one: four apples pass because the largest apple is
 * wide, and a child who wants the little one still cannot hit it. The audit
 * rasterizes every page, keeps the topmost region at each cell (exactly the
 * rule a finger uses), splits each region into its connected pieces, and
 * holds every piece to the floor.
 *
 * Two floors, because two kinds of thing live in a picture:
 *
 * - a compact thing must hold a circle [FLOOR_OF_PIECE] across, which is a
 *   fingertip on the smallest phone the app is used on;
 * - a long band (a stem, a pole, a rainbow arc, the track under a train) is
 *   followed by a stroke rather than dabbed at, and must hold a circle
 *   [FLOOR_OF_BAND] across, which is more than the wax tip itself, so the
 *   band is wide enough to draw a line along.
 *
 * The fix for a piece under the floor is never to draw the speck smaller: a
 * sprinkle drawn large stops being a sprinkle. It is to take the part out of
 * the picture, or to make the thing the part belonged to bigger. What a page
 * must not do is print something a child can see and cannot touch.
 *
 * The measurement is exact rather than approximate: for every candidate point
 * of a piece, the radius of the largest circle that fits inside is the
 * distance to the nearest point outside, and the largest of those is found by
 * a squared-distance transform. Sampling every piece on a grid a few hundred
 * cells across costs a second of test time and catches every real case.
 */
class FingerTest {

    @Test
    fun everyVisiblePieceOfEveryPictureIsBigEnoughForAFinger() {
        val tooSmall = ArrayList<String>()
        for (page in Pages.all) {
            for (piece in visiblePieces(page)) {
                val floor = if (piece.longestSide() >= BAND_LENGTH) FLOOR_OF_BAND else FLOOR_OF_PIECE
                if (piece.widest < floor) {
                    tooSmall += "${page.id}/${piece.regionId} at " +
                        "(${"%.2f".format(piece.center.x)}, ${"%.2f".format(piece.center.y)})" +
                        " fits a circle ${"%.3f".format(piece.widest)} across, " +
                        "a ${"%.2f".format(piece.longestSide())} by ${"%.2f".format(piece.shortestSide())} piece, " +
                        "floor ${"%.3f".format(floor)}"
                }
            }
        }
        assertTrue(
            "these pieces are too small for a fingertip to color:\n" +
                tooSmall.joinToString("\n"),
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
            val measured = widestInside(region.parts, region.bounds)
            assertTrue(
                "$name measured ${"%.4f".format(measured)}, expected about $expected",
                kotlin.math.abs(measured - expected) < 0.008,
            )
        }
    }

    /** One connected piece of a picture's print, at the size a finger sees. */
    private class Piece(
        val regionId: String,
        val boxWidth: Double,
        val boxHeight: Double,
        val widest: Double,
        val center: Vec2,
    ) {
        fun longestSide(): Double = max(boxWidth, boxHeight)
        fun shortestSide(): Double = min(boxWidth, boxHeight)
    }

    /**
     * Every separate piece a child can see, on the whole page.
     *
     * The page is rasterized once; the topmost region at each cell is the
     * region a finger would color there. Then each region's cells are split
     * into 4-connected pieces and each piece is measured on its own.
     */
    private fun visiblePieces(page: Page): List<Piece> {
        val cols = GRID
        val rows = (Page.ASPECT * GRID).toInt()
        val cell = 1.0 / GRID
        val top = IntArray(cols * rows) { -1 }

        // Back to front, so the first region to claim a cell is the one the
        // child sees there. Each region only visits cells inside its own
        // bounds, which is what keeps a big book of blobs affordable.
        for (r in page.regions.indices.reversed()) {
            val region = page.regions[r]
            val b = region.bounds
            val i0 = max(0, (b.x / cell).toInt())
            val i1 = min(cols - 1, (b.right / cell).toInt())
            val j0 = max(0, (b.y / cell).toInt())
            val j1 = min(rows - 1, (b.bottom / cell).toInt())
            for (j in j0..j1) {
                for (i in i0..i1) {
                    val at = j * cols + i
                    if (top[at] != -1) continue
                    val p = Vec2((i + 0.5) * cell, (j + 0.5) * cell)
                    if (region.contains(p)) top[at] = r
                }
            }
        }

        val pieces = ArrayList<Piece>()
        val seen = BooleanArray(cols * rows)
        val queue = IntArray(cols * rows)
        for (start in top.indices) {
            val r = top[start]
            if (r <= 0 || seen[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            seen[start] = true
            val cells = ArrayList<Int>()
            var minI = cols
            var maxI = -1
            var minJ = rows
            var maxJ = -1
            while (head < tail) {
                val c = queue[head++]
                cells += c
                val i = c % cols
                val j = c / cols
                if (i < minI) minI = i
                if (i > maxI) maxI = i
                if (j < minJ) minJ = j
                if (j > maxJ) maxJ = j
                for (d in DIRECTIONS) {
                    val ni = i + d[0]
                    val nj = j + d[1]
                    if (ni < 0 || ni >= cols || nj < 0 || nj >= rows) continue
                    val nb = nj * cols + ni
                    if (seen[nb] || top[nb] != r) continue
                    seen[nb] = true
                    queue[tail++] = nb
                }
            }
            // A piece of one or two cells is rasterization noise around a
            // shape's own edge, not a thing a child can see.
            if (cells.size < MIN_CELLS) continue

            // The piece's own box, with a one cell skirt of outside for the
            // distance transform to measure against on every side.
            val w = maxI - minI + 3
            val h = maxJ - minJ + 3
            val inside = BooleanArray(w * h)
            for (c in cells) {
                val i = c % cols - minI + 1
                val j = c / cols - minJ + 1
                inside[j * w + i] = true
            }
            val widest = widestInsideLocal(inside, w, h) * cell * 2.0
            pieces += Piece(
                regionId = page.regions[r].id,
                boxWidth = (maxI - minI + 1) * cell,
                boxHeight = (maxJ - minJ + 1) * cell,
                widest = widest,
                center = Vec2(
                    (minI + maxI + 1) * cell / 2.0,
                    (minJ + maxJ + 1) * cell / 2.0,
                ),
            )
        }
        return pieces
    }

    /** The widest circle that fits inside a piece given as its own mask. */
    private fun widestInsideLocal(inside: BooleanArray, w: Int, h: Int): Double {
        val f = Array(h) { j ->
            DoubleArray(w) { i -> if (inside[j * w + i]) FAR else 0.0 }
        }
        for (row in f) transform(row)
        val g = Array(w) { i -> DoubleArray(h) { j -> f[j][i] } }
        for (col in g) transform(col)
        var best = 0.0
        for (j in 0 until h) for (i in 0 until w) best = max(best, g[i][j])
        return Math.sqrt(best)
    }

    /**
     * The widest circle that fits inside a list of shapes, in page widths:
     * the distance from the shapes' own best point out to the nearest place
     * they stop. Used by the ruler's own test.
     */
    private fun widestInside(parts: List<Shape>, b: Area): Double {
        if (b.w <= 0.0 || b.h <= 0.0) return 0.0
        val cell = min(b.w, b.h) / GRID
        val pad = 2
        val cols = max(3, (b.w / cell).toInt() + 1) + pad * 2
        val rows = max(3, (b.h / cell).toInt() + 1) + pad * 2
        val f = Array(rows) { j ->
            DoubleArray(cols) { i ->
                val x = b.x + (i - pad + 0.5) * cell
                val y = b.y + (j - pad + 0.5) * cell
                if (parts.contains(Vec2(x, y))) FAR else 0.0
            }
        }
        for (row in f) transform(row)
        val g = Array(cols) { i -> DoubleArray(rows) { j -> f[j][i] } }
        for (col in g) transform(col)
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
         * The floor a compact piece is held to, in page widths: a circle this
         * wide is a fingertip on the smallest phone the app is used on. The
         * crayon's own tip is 0.030 of the page (CRAYON_TIP_FRACTION), so a
         * piece at this floor holds five tip widths across, which is what
         * lets a small hand dab at it without smearing the whole thing.
         */
        const val FLOOR_OF_PIECE = 0.155

        /**
         * The floor a long band is held to: thicker than the wax tip itself,
         * so a stroke drawn along the band stays on it. A band is traced, not
         * dabbed at, which is why it is allowed to be narrower than a finger.
         */
        const val FLOOR_OF_BAND = 0.075

        /** How long a piece has to be to count as a band rather than a thing. */
        const val BAND_LENGTH = 0.45

        /** How many cells the page is walked in across its width. */
        const val GRID = 300

        /** A piece smaller than this many cells is edge noise. */
        const val MIN_CELLS = 4

        /** A distance larger than any page before the transform runs. */
        const val FAR = 1e9

        private val DIRECTIONS = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
        )
    }
}
