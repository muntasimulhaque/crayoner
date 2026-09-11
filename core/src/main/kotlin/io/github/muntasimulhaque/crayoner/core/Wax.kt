package io.github.muntasimulhaque.crayoner.core

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * One pass of the hand: the line the wax was dragged along, how wide the wax
 * was at the time, and how much of the paper it left showing.
 *
 * The points are in page units, the width is a fraction of the page side, and
 * [alpha] is the pass's own coverage. Both renderers draw these passes in
 * order, so a filled area on the device and the same area in the store art
 * are made of exactly the same passes.
 */
data class WaxPass(val points: List<Vec2>, val width: Double, val alpha: Double)

/**
 * How a colored area is made, as pure data.
 *
 * A flat fill is paint, and a flat fill with a texture over it is paint with
 * a texture over it. Wax is different in kind. It is pressed onto the paper
 * by a hand going back and forth, so a colored area is:
 *
 * 1. a **surface**: wax down on most of the paper, but never all of it, with
 *    the tooth of the paper breaking through in speckle and the drag of the
 *    hand leaving soft patches along the direction it moved;
 * 2. the **passes**: the visible streaks of the back and forth itself, which
 *    is what makes an area read as colored in rather than poured.
 *
 * Both come out of here, and both are deterministic functions of the area's
 * own geometry and a hash of its id, so the same area is colored identically
 * in every run, on every device, and in the offline generators. A small area
 * is rubbed with a few fine passes and a big one with many broad ones, so a
 * sprinkle is not colored with a house brush.
 */
object Wax {

    /**
     * The spacing between passes, and the pass width, as fractions of the
     * page: about a finger's width across, which is what a crayon held flat
     * really lays down. Both shrink on a small area so a sprinkle is not
     * buried under one heavy band.
     */
    private const val SPACING = 0.024
    private const val WIDTH = 0.048
    private const val MIN_SPACING = 0.009
    private const val MIN_WIDTH = 0.014

    /** How far a pass wanders off its own line, as a fraction of spacing. */
    private const val WOBBLE = 0.5

    /** No area is rubbed with more passes than this, however big it is. */
    private const val MAX_PASSES = 80

    /**
     * What one pass of the hand lays down, and what a second pass over it
     * does. The surface underneath carries most of the color; these are the
     * streaks of the back and forth, visible as streaks and not as stripes.
     */
    private const val FIRST_ALPHA = 0.36
    private const val SECOND_ALPHA = 0.26

    /** The angles an arm actually uses, in degrees, chosen by region id. */
    private val ANGLES = doubleArrayOf(-38.0, -14.0, 12.0, 37.0, -62.0, 64.0)

    /**
     * The angle the [region] is rubbed at. Chosen from the region's own id,
     * so it never changes between runs and the whole picture never rubs in
     * one direction: a wall was waxed up and down while the grass beside it
     * went across.
     */
    fun angleDeg(region: Region): Double = ANGLES[abs(region.id.hashCode()) % ANGLES.size]

    /** Which seed an area's wax is generated from. */
    fun seed(region: Region): Int = region.id.hashCode() * 2654435761L.toInt() + 17

    /**
     * The spacing and width the [region] is rubbed at, in page units. A
     * caller that needs to place the passes and a caller that needs to draw
     * them both read these, so the surface and the passes agree.
     */
    fun spacing(region: Region): Double {
        val b = region.bounds
        val thin = if (b.w <= 0.0 || b.h <= 0.0) 1.0 else min(b.w, b.h)
        return min(SPACING, max(MIN_SPACING, thin * 0.62))
    }

    fun width(region: Region): Double {
        val b = region.bounds
        val thin = if (b.w <= 0.0 || b.h <= 0.0) 1.0 else min(b.w, b.h)
        return min(WIDTH, max(MIN_WIDTH, thin * 0.85))
    }

    /**
     * Every pass of the hand that colors [region], in the order it was laid
     * down, in page units. The list is clipped to the region by the caller,
     * so the passes are generated long enough to cross it and no further.
     */
    fun passes(region: Region): List<WaxPass> {
        val b = region.bounds
        if (b.w <= 0.0 || b.h <= 0.0) return emptyList()
        val spacing = spacing(region)
        val width = width(region)
        val angle = Math.toRadians(angleDeg(region))
        // The pass normal: passes run along the angle, and step across it.
        val dx = cos(angle)
        val dy = sin(angle)
        val nx = -dy
        val ny = dx
        val cx = b.center.x
        val cy = b.center.y
        // Long enough to leave the region on both sides whatever the angle.
        val reach = hypot(b.w, b.h) * 0.62 + width
        val across = b.w * abs(nx) + b.h * abs(ny)
        val count = min(MAX_PASSES, ceil(across / spacing).toInt() + 2)
        val rng = Lcg(seed(region).toLong())
        val out = ArrayList<WaxPass>(count * 2)
        val half = (count - 1) / 2.0
        for (i in 0 until count) {
            // Where this pass lies across the region, wobbled so no two
            // passes sit exactly one spacing apart.
            val lane = (i - half) * spacing + rng.range(-spacing * 0.22, spacing * 0.22)
            val ox = cx + nx * lane
            val oy = cy + ny * lane
            val start = Vec2(ox - dx * reach, oy - dy * reach)
            val end = Vec2(ox + dx * reach, oy + dy * reach)
            // Each pass is drawn twice, the second a little narrower and
            // lighter, which is what a second stroke over waxed paper does.
            out += WaxPass(wobbled(start, end, spacing * WOBBLE, rng), width, FIRST_ALPHA)
            out += WaxPass(wobbled(start, end, spacing * WOBBLE * 0.6, rng), width * 0.72, SECOND_ALPHA)
        }
        return out
    }

    /**
     * The wax of [region] as a tile of ARGB pixels, [size] on a side, to be
     * repeated across an area as a texture.
     *
     * The tile carries the coverage itself, not a veil over a flat color:
     * every pixel is the crayon's own color at the alpha the wax actually
     * laid down there. The paper's tooth is the fine speckle; the drag is
     * the same noise stretched along the rubbing direction, which is the
     * streak a hand leaves; and the broad mottle is where the hand pressed
     * harder and where it lifted. The result is a surface that reads as wax
     * on paper at arm's length and as paper's tooth up close.
     *
     * [fine] is for the child's own marks rather than a colored area. A mark
     * is about a fortieth of the page across, so a tile scaled for a sky
     * would show one or two of its blotches across the whole stroke and read
     * as gloss; the fine tile carries the same two noises at the scale of a
     * mark, so a stroke of wax looks like a stroke of wax.
     *
     * [phase] slides the tile, so two areas of the same picture do not share
     * one texture: a wall and the grass under it were not waxed in step.
     */
    fun surface(
        argb: Long,
        size: Int,
        angleDeg: Double,
        phase: Int,
        seed: Int,
        fine: Boolean = false,
    ): IntArray {
        require(size > 0) { "a wax tile needs a positive size" }
        val toothCells = if (fine) TOOTH_CELLS / 2 else TOOTH_CELLS
        val broadCells = if (fine) BROAD_CELLS / 2 else BROAD_CELLS
        val tooth = blur(noise(size, toothCells, toothCells, seed))
        val drag = stretchedNoise(size, angleDeg, seed + 101, fine)
        val broad = noise(size, broadCells, broadCells, seed + 977)
        val cover = if (fine) MARK_COVERAGE else COVERAGE
        val swing = if (fine) MARK_COVERAGE_SWING else COVERAGE_SWING
        val rgb = (argb and 0xFFFFFF).toInt()
        val out = IntArray(size * size)
        for (i in out.indices) {
            val s =
                (tooth[i] - 0.5) * 1.00 +
                    (drag[i] - 0.5) * 1.15 +
                    (broad[i] - 0.5) * 0.55
            // The middle of the range is held back and the extremes pushed:
            // wax is mostly down, with places it skipped and places it piled.
            val c = (cover + s * swing).coerceIn(0.0, 1.0).pow(COVERAGE_CURVE)
            val alpha = (c * 255.0).toInt().coerceIn(0, 255)
            out[(i + phase) % out.size] = (alpha shl 24) or rgb
        }
        return out
    }

    /** How much wax the surface lays down on average, and how it varies. */
    private const val COVERAGE = 0.78
    private const val COVERAGE_SWING = 0.34
    private const val COVERAGE_CURVE = 0.80

    /**
     * The same two numbers for a mark. A hand drawing a line presses less
     * evenly than a hand filling an area, so a stroke is thinner and more
     * broken up and the paper keeps coming through it. That break is what
     * makes a line read as crayon rather than as paint out of a tube.
     */
    private const val MARK_COVERAGE = 0.68
    private const val MARK_COVERAGE_SWING = 0.60

    private const val TOOTH_CELLS = 40
    private const val BROAD_CELLS = 7

    /** Noise on a lattice that wraps, at [cx] by [cy] cells over the tile. */
    private fun noise(size: Int, cx: Int, cy: Int, seed: Int): DoubleArray {
        var state = seed
        fun next(): Double {
            state = state * 1103515245 + 12345
            return ((state ushr 8) and 0xFFFF) / 65535.0
        }
        val lattice = DoubleArray(cx * cy) { next() }
        val out = DoubleArray(size * size)
        for (y in 0 until size) {
            val fy = y.toDouble() * cy / size
            val y0 = fy.toInt() % cy
            val y1 = (y0 + 1) % cy
            val ty = smooth(fy - fy.toInt())
            for (x in 0 until size) {
                val fx = x.toDouble() * cx / size
                val x0 = fx.toInt() % cx
                val x1 = (x0 + 1) % cx
                val tx = smooth(fx - fx.toInt())
                val a = lattice[y0 * cx + x0]
                val b = lattice[y0 * cx + x1]
                val c = lattice[y1 * cx + x0]
                val d = lattice[y1 * cx + x1]
                val top = a + (b - a) * tx
                val bottom = c + (d - c) * tx
                out[y * size + x] = top + (bottom - top) * ty
            }
        }
        return out
    }

    /**
     * Noise stretched along [angleDeg], on a lattice that wraps: many cells
     * across the drag and few along it, so the patch runs the way the hand
     * did. A round cell here reads as a stain.
     */
    private fun stretchedNoise(size: Int, angleDeg: Double, seed: Int, fine: Boolean = false): DoubleArray {
        val across = if (fine) 3 else 4
        val along = if (fine) 22 else 13
        // Rotating the sample point into the drag's own frame is what turns
        // round cells into long ones, and the lattice still wraps because the
        // sample is taken modulo the cell counts.
        val a = Math.toRadians(angleDeg)
        val ca = cos(a)
        val sa = sin(a)
        var state = seed
        fun next(): Double {
            state = state * 1103515245 + 12345
            return ((state ushr 8) and 0xFFFF) / 65535.0
        }
        val lattice = DoubleArray(along * across) { next() }
        val out = DoubleArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Drag's own axes: u across the movement, v along it.
                val u = x * ca + y * sa
                val v = -x * sa + y * ca
                val fu = u.toDouble() * across / size
                val fv = v.toDouble() * along / size
                out[y * size + x] = sample(lattice, along, across, fv, fu)
            }
        }
        return out
    }

    /** One bilinear sample of a wrapping lattice, at ([fy], [fx]) cells. */
    private fun sample(lattice: DoubleArray, rows: Int, cols: Int, fy: Double, fx: Double): Double {
        val y0 = wrap(fy.toInt(), rows)
        val y1 = wrap(y0 + 1, rows)
        val x0 = wrap(fx.toInt(), cols)
        val x1 = wrap(x0 + 1, cols)
        val ty = smooth(fy - fy.toInt())
        val tx = smooth(fx - fx.toInt())
        val a = lattice[y0 * cols + x0]
        val b = lattice[y0 * cols + x1]
        val c = lattice[y1 * cols + x0]
        val d = lattice[y1 * cols + x1]
        val top = a + (b - a) * tx
        val bottom = c + (d - c) * tx
        return top + (bottom - top) * ty
    }

    private fun wrap(v: Int, n: Int): Int {
        val m = v % n
        return if (m < 0) m + n else m
    }

    private fun smooth(t: Double): Double = t * t * (3.0 - 2.0 * t)

    /** A wrapping 3x3 box blur, which turns speckle into wax. */
    private fun blur(src: DoubleArray): DoubleArray {
        val size = sqrtInt(src.size)
        val out = DoubleArray(src.size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var sum = 0.0
                for (dy in -1..1) {
                    val yy = (y + dy + size) % size
                    for (dx in -1..1) {
                        val xx = (x + dx + size) % size
                        sum += src[yy * size + xx]
                    }
                }
                out[y * size + x] = sum / 9.0
            }
        }
        return out
    }

    private fun sqrtInt(n: Int): Int {
        var r = 1
        while (r * r < n) r++
        return r
    }

    /** A straight pass with the small wander a wrist gives it. */
    private fun wobbled(from: Vec2, to: Vec2, amount: Double, rng: Lcg): List<Vec2> {
        val steps = 6
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = hypot(dx, dy).takeIf { it > 1e-9 } ?: 1.0
        val nx = -dy / len
        val ny = dx / len
        return (0..steps).map { i ->
            val t = i.toDouble() / steps
            val off = if (i == 0 || i == steps) 0.0 else rng.range(-amount, amount)
            Vec2(from.x + dx * t + nx * off, from.y + dy * t + ny * off)
        }
    }

    /** A tiny deterministic generator: the same area, the same wax, always. */
    private class Lcg(seed: Long) {
        private var state = seed
        fun next(): Double {
            state = state * 6364136223846793005L + 1442695040888963407L
            return ((state ushr 11) and 0x1FFFFFFFFFFFFFL) / 9007199254740991.0
        }

        fun range(lo: Double, hi: Double): Double = lo + (hi - lo) * next()
    }
}
