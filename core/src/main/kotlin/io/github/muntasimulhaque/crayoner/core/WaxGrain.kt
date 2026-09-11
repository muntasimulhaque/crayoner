package io.github.muntasimulhaque.crayoner.core

/**
 * The grain of wax on paper, as pure data.
 *
 * A flat fill is paint. Wax is dragged across the tooth of the paper, so a
 * real crayon mark is never even: it breaks up into fine speckle, it skips
 * where the paper is low, and it piles up where the hand pressed. This
 * object produces one small tile of that, deterministically, and both
 * renderers lay it over every colored area (Compose through an image
 * shader, Java2D through a texture paint). One generator, so the page on
 * the device and the page in the store art are grained identically.
 *
 * The tile carries two scales, because real wax has two. The tooth is a
 * four pixel speckle, the scale of paper under a crayon. Over it sits a
 * soft, twenty-four pixel mottle, the scale of a hand moving: patches where
 * the wax is laid on, patches where the paper still shows through. A tile
 * with only the tooth reads as dirt; the mottle is what makes a filled area
 * read as colored in, by hand, rather than poured.
 *
 * The tile wraps seamlessly: both noises run on lattices that wrap at the
 * tile's edge, so a tiled shader has no visible seams.
 */
object WaxGrain {

    /** The tile's side, in pixels. Small enough to build in a blink. */
    const val SIZE = 192

    /**
     * How many random cells the fine tooth spans, and the coarse mottle.
     * 48 cells over 192 pixels is a four pixel tooth; 16 cells is the drag
     * of one short pass of the hand, which is the scale wax really piles at.
     */
    private const val CELLS = 48
    private const val COVER_CELLS = 16

    /** The strongest a dark speck may be. Average coverage stays under 12. */
    private const val DARK_ALPHA = 30.0

    /** Paper showing through is the lighter half, and it carries further. */
    private const val LIGHT_ALPHA = 40.0

    /**
     * How much each layer weighs. The mottle carries the wax look, so it
     * leans heavier than the tooth, and neither ever reaches full strength:
     * at its darkest a speck only cools the color under it.
     */
    private const val TOOTH_WEIGHT = 1.0
    private const val COVER_WEIGHT = 0.8

    /**
     * How sharply the field turns into alpha. A crayon's coverage is mostly
     * even, with occasional skips and piles, so the middle of the range is
     * held back and only the extremes show.
     */
    private const val CURVE = 1.0

    /**
     * One tile of uneven wax, as ARGB ints. Dark specks are black at a low
     * alpha, light specks are white at a lower one: the two together read as
     * wax catching on paper rather than as either shadow or shine.
     */
    fun pixels(seed: Int = 20260911): IntArray {
        val tooth = blur(value(lattice(seed, CELLS), CELLS))
        val cover = value(lattice(seed * 31 + 7, COVER_CELLS), COVER_CELLS)
        val out = IntArray(SIZE * SIZE)
        for (i in out.indices) {
            // One signed field per pixel: above zero the wax is thicker,
            // below zero the paper is showing. Two noises, one reading, so
            // no pixel can be dark and light at once.
            val s = ((tooth[i] - 0.5) * 2.0 * TOOTH_WEIGHT) +
                ((cover[i] - 0.5) * 2.0 * COVER_WEIGHT)
            val t = kotlin.math.abs(s) / (TOOTH_WEIGHT + COVER_WEIGHT)
            val shaped = Math.pow(t, CURVE)
            out[i] = if (s >= 0.0) {
                val a = (shaped * DARK_ALPHA).toInt().coerceIn(0, 255)
                (a shl 24)
            } else {
                val a = (shaped * LIGHT_ALPHA).toInt().coerceIn(0, 255)
                (a shl 24) or 0xFFFFFF
            }
        }
        return out
    }

    /** The mean alpha of the tile, the number that keeps the grain honest. */
    fun averageAlpha(pixels: IntArray = pixels()): Double =
        pixels.sumOf { (it ushr 24) and 0xFF } / pixels.size.toDouble()

    private fun lattice(seed: Int, cells: Int): DoubleArray {
        var state = seed
        fun next(): Double {
            state = state * 1103515245 + 12345
            return ((state ushr 8) and 0xFFFF) / 65535.0
        }
        return DoubleArray(cells * cells) { next() }
    }

    /** Smoothly interpolated value noise on a lattice that wraps. */
    private fun value(g: DoubleArray, cells: Int): DoubleArray {
        val out = DoubleArray(SIZE * SIZE)
        for (y in 0 until SIZE) {
            val fy = y.toDouble() * cells / SIZE
            val y0 = fy.toInt() % cells
            val y1 = (y0 + 1) % cells
            val ty = smooth(fy - fy.toInt())
            for (x in 0 until SIZE) {
                val fx = x.toDouble() * cells / SIZE
                val x0 = fx.toInt() % cells
                val x1 = (x0 + 1) % cells
                val tx = smooth(fx - fx.toInt())
                val a = g[y0 * cells + x0]
                val b = g[y0 * cells + x1]
                val c = g[y1 * cells + x0]
                val d = g[y1 * cells + x1]
                val top = a + (b - a) * tx
                val bottom = c + (d - c) * tx
                out[y * SIZE + x] = top + (bottom - top) * ty
            }
        }
        return out
    }

    private fun smooth(t: Double): Double = t * t * (3.0 - 2.0 * t)

    /** A wrapping 3x3 box blur, which turns speckle into wax. */
    private fun blur(src: DoubleArray): DoubleArray {
        val out = DoubleArray(src.size)
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                var sum = 0.0
                for (dy in -1..1) {
                    val yy = (y + dy + SIZE) % SIZE
                    for (dx in -1..1) {
                        val xx = (x + dx + SIZE) % SIZE
                        sum += src[yy * SIZE + xx]
                    }
                }
                out[y * SIZE + x] = sum / 9.0
            }
        }
        return out
    }
}
