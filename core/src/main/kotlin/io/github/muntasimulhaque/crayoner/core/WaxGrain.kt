package io.github.muntasimulhaque.crayoner.core

/**
 * The grain of wax on paper, as pure data.
 *
 * A flat fill is paint. A fill with a soft, uneven, slightly darker speckle
 * is a crayon: the wax catches on the tooth of the paper, so coverage is
 * never perfectly even. This object produces one small tile of that grain,
 * deterministically, and both renderers lay it over every colored area
 * (Compose through an image shader, Java2D through a texture paint). One
 * generator, so the page on the device and the page in the store art are
 * grained identically.
 *
 * The tile wraps seamlessly: the value noise is generated on a lattice that
 * wraps at its own edge, so a tiled shader has no visible seams.
 */
object WaxGrain {

    /** The tile's side, in pixels. Small enough to build in a blink. */
    const val SIZE = 192

    /**
     * How many random cells across the tile. This is the grain's real size:
     * 48 cells over 192 pixels is a four pixel tooth, which is the scale of
     * paper under a crayon, not the scale of a stain.
     */
    private const val CELLS = 48

    /**
     * One tile of grayscale specks, as ARGB ints. Dark specks are black with
     * a low alpha, light specks are white with a lower one: the two together
     * read as uneven wax rather than as either shadow or shine.
     */
    fun pixels(seed: Int = 20260911): IntArray {
        val lattice = lattice(seed)
        val raw = DoubleArray(SIZE * SIZE) { i -> value(lattice, i % SIZE, i / SIZE) }
        val soft = blur(raw)
        val out = IntArray(SIZE * SIZE)
        for (i in soft.indices) {
            val v = soft[i]
            out[i] = if (v >= 0.5) {
                val t = ((v - 0.5) / 0.5)
                val a = (t * t * DARK_ALPHA).toInt().coerceIn(0, 255)
                (a shl 24) or 0x000000
            } else {
                val t = ((0.5 - v) / 0.5)
                val a = (t * t * LIGHT_ALPHA).toInt().coerceIn(0, 255)
                (a shl 24) or 0xFFFFFF
            }
        }
        return out
    }

    /** The strongest a dark speck may be; kept low, this is a whisper. */
    private const val DARK_ALPHA = 13.0

    /** Light specks stay under the dark ones, or the wax looks wet. */
    private const val LIGHT_ALPHA = 9.0

    private fun lattice(seed: Int): DoubleArray {
        var state = seed
        fun next(): Double {
            state = state * 1103515245 + 12345
            return ((state ushr 8) and 0xFFFF) / 65535.0
        }
        return DoubleArray(CELLS * CELLS) { next() }
    }

    /** Smoothly interpolated value noise on a lattice that wraps. */
    private fun value(g: DoubleArray, x: Int, y: Int): Double {
        val fx = x.toDouble() * CELLS / SIZE
        val fy = y.toDouble() * CELLS / SIZE
        val x0 = fx.toInt() % CELLS
        val y0 = fy.toInt() % CELLS
        val x1 = (x0 + 1) % CELLS
        val y1 = (y0 + 1) % CELLS
        val tx = smooth(fx - fx.toInt())
        val ty = smooth(fy - fy.toInt())
        val a = g[y0 * CELLS + x0]
        val b = g[y0 * CELLS + x1]
        val c = g[y1 * CELLS + x0]
        val d = g[y1 * CELLS + x1]
        val top = a + (b - a) * tx
        val bottom = c + (d - c) * tx
        return top + (bottom - top) * ty
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
