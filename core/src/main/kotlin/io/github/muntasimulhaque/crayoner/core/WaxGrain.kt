package io.github.muntasimulhaque.crayoner.core

import kotlin.math.abs
import kotlin.math.pow

/**
 * The grain of wax on paper, as pure data.
 *
 * A flat fill is paint. Wax is dragged across the tooth of the paper, so a
 * real crayon mark is never even: it breaks up into fine speckle, it skips
 * where the paper is low, and it piles up where the hand pressed. This
 * object produces the tiles both renderers use, deterministically, so the
 * page on the device and the page in the store art are grained identically.
 *
 * Two tiles come out of here, and they are two sides of the same material:
 *
 * - [pixels] is the paper's own tooth over a colored area, a whisper of
 *   light and dark that keeps a fill from reading as poured paint;
 * - [inkPixels] is the ink of the app's own words laid down like wax, so a
 *   word covers the way a colored area covers, with the paper coming
 *   through it.
 *
 * Both carry two scales, because real wax has two. The tooth is a four pixel
 * speckle, the scale of paper under a crayon. Over it sits a soft mottle at
 * the scale of a hand moving: patches where the wax is laid on, patches
 * where the paper still shows through. A tile with only the tooth reads as
 * dirt; the mottle is what makes a filled area read as colored in, by hand,
 * rather than poured.
 *
 * The tiles wrap seamlessly: the noise runs on lattices that wrap at the
 * tile's edge, so a tiled shader has no visible seams.
 */
object WaxGrain {

    /** The tile's side, in pixels. Small enough to build in a blink. */
    const val SIZE = 192

    /**
     * How many random cells the fine tooth spans, the drag of one pass of
     * the hand, and the broad mottle underneath it.
     *
     * The drag is stretched along the rubbing direction (many cells across,
     * few down), which is what a hand moving back and forth leaves behind;
     * a round cell would read as a stain rather than as a stroke.
     */
    private const val CELLS = 40
    private const val DRAG_X = 4
    private const val DRAG_Y = 13
    private const val BROAD_CELLS = 7

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
     * How much of the ink a word keeps where the paper's tooth is high, and
     * how far it thins where the wax skipped. A word is still a word: it
     * thins, it never becomes unreadable.
     */
    private const val INK_FLOOR = 0.62
    private const val INK_BODY = 0.80
    private const val INK_SWING = 1.15

    /**
     * One tile of uneven wax, as ARGB ints. Dark specks are black at a low
     * alpha, light specks are white at a lower one: the two together read as
     * wax catching on paper rather than as either shadow or shine.
     */
    fun pixels(seed: Int = SEED): IntArray {
        val tooth = blur(value(lattice(seed, CELLS, CELLS), CELLS, CELLS))
        val drag = value(lattice(seed * 31 + 7, DRAG_X, DRAG_Y), DRAG_X, DRAG_Y)
        val broad = value(lattice(seed * 17 + 3, BROAD_CELLS, BROAD_CELLS), BROAD_CELLS, BROAD_CELLS)
        val out = IntArray(SIZE * SIZE)
        for (i in out.indices) {
            // One signed field per pixel: above zero the wax is thicker,
            // below zero the paper is showing. The noises read together, so
            // no pixel can be dark and light at once.
            val s =
                ((tooth[i] - 0.5) * 2.0 * TOOTH_WEIGHT) +
                    ((drag[i] - 0.5) * 2.0 * 1.4) +
                    ((broad[i] - 0.5) * 2.0 * COVER_WEIGHT)
            val t = abs(s) / (TOOTH_WEIGHT + 1.4 + COVER_WEIGHT)
            val shaped = t.pow(CURVE)
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

    /**
     * One tile of ink laid down as wax: opaque where the crayon pressed, a
     * little thinner where it skipped. It carries the same two noises as
     * [pixels] and the same ink everything else in the app is written in,
     * so a word and a printed line are the same color and the same material.
     */
    fun inkPixels(seed: Int = SEED, argb: Long = INK): IntArray {
        val tooth = blur(value(lattice(seed, CELLS, CELLS), CELLS, CELLS))
        val drag = value(lattice(seed * 31 + 7, DRAG_X, DRAG_Y), DRAG_X, DRAG_Y)
        val broad = value(lattice(seed * 17 + 3, BROAD_CELLS, BROAD_CELLS), BROAD_CELLS, BROAD_CELLS)
        val rgb = (argb and 0xFFFFFF).toInt()
        val out = IntArray(SIZE * SIZE)
        for (i in out.indices) {
            val s =
                (tooth[i] - 0.5) * 0.45 +
                    (drag[i] - 0.5) * 0.30 +
                    (broad[i] - 0.5) * 0.16
            val coverage = (INK_BODY + s * INK_SWING).coerceIn(0.0, 1.0)
            val alpha = (maxOf(INK_FLOOR, coverage) * 255.0).toInt().coerceIn(0, 255)
            out[i] = (alpha shl 24) or rgb
        }
        return out
    }

    /** The mean alpha of the tile, the number that keeps the grain honest. */
    fun averageAlpha(pixels: IntArray = pixels()): Double =
        pixels.sumOf { (it ushr 24) and 0xFF } / pixels.size.toDouble()

    private const val SEED = 20260911

    /** The ink the words are written in, mirroring Crayons.INK. */
    private const val INK = 0xFF3B4351

    private fun lattice(seed: Int, cellsX: Int, cellsY: Int): DoubleArray {
        var state = seed
        fun next(): Double {
            state = state * 1103515245 + 12345
            return ((state ushr 8) and 0xFFFF) / 65535.0
        }
        return DoubleArray(cellsX * cellsY) { next() }
    }

    /** Smoothly interpolated value noise on a lattice that wraps. */
    private fun value(g: DoubleArray, cellsX: Int, cellsY: Int): DoubleArray {
        val out = DoubleArray(SIZE * SIZE)
        for (y in 0 until SIZE) {
            val fy = y.toDouble() * cellsY / SIZE
            val y0 = fy.toInt() % cellsY
            val y1 = (y0 + 1) % cellsY
            val ty = smooth(fy - fy.toInt())
            for (x in 0 until SIZE) {
                val fx = x.toDouble() * cellsX / SIZE
                val x0 = fx.toInt() % cellsX
                val x1 = (x0 + 1) % cellsX
                val tx = smooth(fx - fx.toInt())
                val a = g[y0 * cellsX + x0]
                val b = g[y0 * cellsX + x1]
                val c = g[y1 * cellsX + x0]
                val d = g[y1 * cellsX + x1]
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
