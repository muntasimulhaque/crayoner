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
        // The tooth of the paper is the same size under a mark as under a
        // colored area, because it is the same paper either way. Only the
        // mottle changes scale: a mark is about a fortieth of the page
        // across, so the broad blotches that read as a hand in a big area
        // would read as gloss on a stroke, and they are laid at the scale of
        // the hand that drew the line instead.
        val mottle = if (fine) MARK_BROAD_CELLS else BROAD_CELLS
        val tooth = blur(noise(size, TOOTH_CELLS, TOOTH_CELLS, seed))
        val drag = dragField(size, angleDeg, seed + 101)
        val broad = noise(size, mottle, mottle, seed + 977)
        val cover = if (fine) MARK_COVERAGE else COVERAGE
        val swing = if (fine) MARK_COVERAGE_SWING else COVERAGE_SWING
        // The drag leans hard on both: the streak is what says a hand went
        // back and forth, and it is the difference in coverage along the
        // stroke, not a darker color, that shows it. Wax is never darker
        // than the pigment it is made of.
        val dragWeight = if (fine) MARK_DRAG_WEIGHT else COVERAGE_DRAG_WEIGHT
        val toothWeight = if (fine) MARK_TOOTH_WEIGHT else COVERAGE_TOOTH_WEIGHT
        val broadWeight = if (fine) MARK_BROAD_WEIGHT else COVERAGE_BROAD_WEIGHT
        val rgb = (argb and 0xFFFFFF).toInt()
        val out = IntArray(size * size)
        // [phase] slides the tile so two areas of one picture do not share a
        // texture; it is a rotation of the finished pixels and nothing else.
        val turn = (phase % out.size).coerceAtLeast(0)
        for (i in out.indices) {
            val s =
                (tooth[i] - 0.5) * toothWeight +
                    (drag[i] - 0.5) * dragWeight +
                    (broad[i] - 0.5) * broadWeight
            // The middle of the range is held back and the extremes pushed:
            // wax is mostly down, with places it skipped and places it piled.
            val c = coverageCurve((cover + s * swing).coerceIn(0.0, 1.0))
            val alpha = (c * 255.0).toInt().coerceIn(0, 255)
            val j = i + turn
            out[if (j >= out.size) j - out.size else j] = (alpha shl 24) or rgb
        }
        return out
    }

    /**
     * The coverage curve, read off a table rather than a `pow` per pixel.
     *
     * A tile is ninety thousand pixels and the curve is smooth, so a table of
     * this many segments is the same curve to well under a ten-thousandth of
     * an alpha level: what it saves is a transcendental call in the middle of
     * the one loop that runs for every pixel of every area of every picture.
     * The old table's worth of `pow` calls was a measurable part of a picture.
     */
    private fun coverageCurve(coverage: Double): Double {
        val at = coverage * CURVE_STEPS
        val whole = at.toInt()
        if (whole >= CURVE_STEPS) return CURVE[CURVE_STEPS]
        val low = CURVE[whole]
        return low + (CURVE[whole + 1] - low) * (at - whole)
    }

    private const val CURVE_STEPS = 4096

    private val CURVE = DoubleArray(CURVE_STEPS + 1) { i ->
        (i.toDouble() / CURVE_STEPS).pow(COVERAGE_CURVE)
    }

    /**
     * How much of the paper a pass of the hand leaves covered.
     *
     * A real crayon is not a wash: dragged across paper it lays nearly all
     * of its pigment down, and what breaks the color is the paper's tooth
     * and the places the stick skipped, not a thin veil of the color. So
     * coverage is high and the variation is what makes it wax. The old
     * model ran at under eighty percent and read as a marker: too pale to be
     * the stick the child picked up, and too flat to be pressed into paper.
     *
     * The second half of that lesson is what the coverage is *not* allowed
     * to do: at ninety percent with a quarter of a swing it came out at a
     * mean alpha of 233 with a standard deviation of 11, which is four
     * percent variation. A mark at that coverage is not wax on paper, it is
     * flat ink with a faint texture, which is exactly what a sign pen
     * leaves. The mean stays high and the swing and the tooth grow instead,
     * so the stick's own color still lands, and the paper's tooth is what
     * the eye reads as crayon. See D-056.
     */
    private const val COVERAGE = 0.88
    private const val COVERAGE_SWING = 0.70
    private const val COVERAGE_CURVE = 0.85

    /** How heavily each noise leans on a filled area. */
    private const val COVERAGE_DRAG_WEIGHT = 3.20
    private const val COVERAGE_TOOTH_WEIGHT = 1.20
    private const val COVERAGE_BROAD_WEIGHT = 0.20

    /**
     * The same numbers for a mark, and here the tooth carries the texture.
     *
     * A mark is one narrow band of wax about a thirtieth of the page across,
     * looked at from a reading distance: the paper's own tooth is a large
     * part of what the child sees, so the tooth weight is high and the
     * coverage swing is wide, while the drag stays the heaviest term so a
     * mark is still stretched along the direction the hand moved (see
     * `WaxTest.aMarkIsStretchedAlongTheHandThatMadeIt`).
     */
    private const val MARK_COVERAGE = 0.88
    private const val MARK_COVERAGE_SWING = 0.70

    /**
     * How heavily each noise leans on a mark. The drag is heavier here than
     * on an area, because a mark is a single line rather than a field of
     * passes: with nothing over it to carry the streak, the smear itself has
     * to be what says a hand went along it. The tooth is nearly as heavy, so
     * the paper breaks the line the way it breaks a real one.
     */
    private const val MARK_DRAG_WEIGHT = 3.20
    private const val MARK_TOOTH_WEIGHT = 1.20
    private const val MARK_BROAD_WEIGHT = 0.20

    private const val TOOTH_CELLS = 40
    private const val BROAD_CELLS = 7

    /** A mark's own mottle, at the scale of a line rather than an area. */
    private const val MARK_BROAD_CELLS = 20

    /** Noise on a lattice that wraps, at [cx] by [cy] cells over the tile. */
    private fun noise(size: Int, cx: Int, cy: Int, seed: Int): DoubleArray {
        var state = seed
        fun next(): Double {
            state = state * 1103515245 + 12345
            return ((state ushr 8) and 0xFFFF) / 65535.0
        }
        val lattice = DoubleArray(cx * cy) { next() }
        // Every pixel of a row reads the same two cells of the lattice with
        // the same weight, and every pixel of a column does the same the other
        // way, so both are worked out once per tile instead of twice per
        // pixel. The arithmetic is untouched: the cells and the weights are
        // the ones the pixel-by-pixel walk would have found.
        val ax = IntArray(size)
        val bx = IntArray(size)
        val tx = DoubleArray(size)
        for (x in 0 until size) {
            val fx = x.toDouble() * cx / size
            val cell = fx.toInt() % cx
            ax[x] = cell
            bx[x] = (cell + 1) % cx
            tx[x] = smooth(fx - fx.toInt())
        }
        val ay = IntArray(size)
        val by = IntArray(size)
        val ty = DoubleArray(size)
        for (y in 0 until size) {
            val fy = y.toDouble() * cy / size
            val cell = fy.toInt() % cy
            ay[y] = cell
            by[y] = (cell + 1) % cy
            ty[y] = smooth(fy - fy.toInt())
        }
        val out = DoubleArray(size * size)
        for (y in 0 until size) {
            val topRow = ay[y] * cx
            val bottomRow = by[y] * cx
            val across = ty[y]
            var i = y * size
            for (x in 0 until size) {
                val a = lattice[topRow + ax[x]]
                val b = lattice[topRow + bx[x]]
                val c = lattice[bottomRow + ax[x]]
                val d = lattice[bottomRow + bx[x]]
                val top = a + (b - a) * tx[x]
                val bottom = c + (d - c) * tx[x]
                out[i++] = top + (bottom - top) * across
            }
        }
        return out
    }

    /**
     * The drag of the hand: the material stretched along [angleDeg].
     *
     * Two scales go into it, and the split is why the streak reads as a hand
     * rather than as noise. The base carries long wisps, smeared far along
     * the direction of travel; over it rides a shorter smear, so the field
     * has structure at the size of a wrist's movement as well as at the size
     * of the whole pass. A single smear either reads as a stain (too short)
     * or as a gradient (too long).
     *
     * Both are wraps of one base field, sampled with wraparound indexing, so
     * the tile has no seam at any angle: a rotation does not commute with the
     * wrap, which is what the older sampling got wrong, and a smear does.
     * The offset along the drag leans a hair off the angle too, so the wisps
     * are not all parallel to each other either.
     */
    private fun dragField(size: Int, angleDeg: Double, seed: Int): DoubleArray {
        // One reach and one cell count for both kinds of surface: the drag is
        // the hand, and the hand is the same whether it is filling a sky or
        // drawing a line. The tile's own scale is what differs (a mark's
        // tooth and mottle are finer), not the shape of the smear.
        val long = smear(size, angleDeg, seed, 0.40, 12)
        val short = smear(size, angleDeg + 11.0, seed + 331, 0.14, 18)
        return DoubleArray(size * size) { i -> long[i] * 0.62 + short[i] * 0.38 }
    }

    /**
     * One smear: the base field sampled [taps] times along [angleDeg] and
     * averaged, each sample taken with wraparound indexing.
     *
     * The sample offsets are whole pixels and they are the same nine for
     * every pixel of the tile, so they are worked out once: the rounding and
     * the wrap inside the loop were most of what a tile cost.
     */
    private fun smear(
        size: Int,
        angleDeg: Double,
        seed: Int,
        reachFraction: Double,
        cells: Int,
    ): DoubleArray {
        val base = blur(noise(size, cells, cells, seed))
        val a = Math.toRadians(angleDeg)
        val dx = cos(a)
        val dy = sin(a)
        val reach = size * reachFraction
        val taps = 9
        val half = (taps - 1) / 2.0
        // Whole pixels, and never more than a tile away, so one add is all
        // the wrapping a sample can need.
        val offX = IntArray(taps)
        val offY = IntArray(taps)
        for (i in 0 until taps) {
            val t = (i - half) / half * reach
            offX[i] = Math.round(t * dx).toInt()
            offY[i] = Math.round(t * dy).toInt()
        }
        val out = DoubleArray(size * size)
        // Where each tap lands, for each pixel of a row and of a column: the
        // wrap is the only work in the sample loop, and it is the same for a
        // whole column and a whole row.
        val tapsX = Array(taps) { i -> IntArray(size) { x -> wrapIndex(x + offX[i], size) } }
        val tapsY = Array(taps) { i -> IntArray(size) { y -> wrapIndex(y + offY[i], size) * size } }
        for (y in 0 until size) {
            for (x in 0 until size) {
                var sum = 0.0
                for (i in 0 until taps) {
                    sum += base[tapsY[i][y] + tapsX[i][x]]
                }
                out[y * size + x] = sum / taps
            }
        }
        return out
    }

    /** One coordinate of a wrapping sample, with no division in the loop. */
    private fun wrapIndex(v: Int, n: Int): Int =
        if (v < 0) v + n else if (v >= n) v - n else v

    /** The weight between two lattice cells, eased so the speckle is wax. */
    private fun smooth(t: Double): Double = t * t * (3.0 - 2.0 * t)

    /** A wrapping 3x3 box blur, which turns speckle into wax. */
    private fun blur(src: DoubleArray): DoubleArray {
        val size = sqrtInt(src.size)
        val out = DoubleArray(src.size)
        // The three columns and the three rows a pixel reads are the same for
        // a whole row and a whole column, and the wrap is the only thing that
        // made finding them any work at all.
        val before = IntArray(size)
        val after = IntArray(size)
        for (i in 0 until size) {
            before[i] = if (i == 0) size - 1 else i - 1
            after[i] = if (i == size - 1) 0 else i + 1
        }
        for (y in 0 until size) {
            val above = before[y] * size
            val here = y * size
            val below = after[y] * size
            var i = y * size
            for (x in 0 until size) {
                val left = before[x]
                val right = after[x]
                out[i++] = (
                    src[above + left] + src[above + x] + src[above + right] +
                        src[here + left] + src[here + x] + src[here + right] +
                        src[below + left] + src[below + x] + src[below + right]
                    ) / 9.0
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
