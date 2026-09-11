package io.github.muntasimulhaque.crayoner.core

import kotlin.math.hypot

/**
 * One mark the child's own hand made: a color, and the line the finger
 * traveled while the crayon was down. A stroke of a single point is a real
 * mark too, the dot a crayon leaves when it is pressed and lifted.
 *
 * A stroke can also be an eraser mark ([erase]): the same line, made with
 * the rubber end of the crayon box instead of wax. An eraser mark removes
 * the wax the child put on the paper and leaves the printed line alone,
 * the way a real rubber does, so [color] means nothing while [erase] is
 * true.
 *
 * The app no longer fills areas for the child. A coloring page is a piece
 * of paper with a picture printed on it, and coloring it means moving wax
 * across the paper: what the child sees on their sheet is exactly what
 * their hand did, and nothing appears that they did not draw.
 */
data class Stroke(
    val color: Long,
    val points: List<Vec2>,
    val erase: Boolean = false,
) {
    val isEmpty: Boolean get() = points.isEmpty()

    fun plus(p: Vec2): Stroke = copy(points = points + p)

    /** How far the hand traveled, in page units, for the tests. */
    fun length(): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            total += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }
        return total
    }

    companion object {
        /** The color an eraser mark carries; it is never painted. */
        const val ERASE_COLOR: Long = 0L
    }
}

/**
 * How a child's hand is read into strokes: sound points, no gaps, and a
 * fixed budget so a save can never grow without bound.
 *
 * The one rule that matters for how it looks is [MIN_STEP]: a point is kept
 * only when the finger has moved far enough that the mark would visibly
 * lengthen. Everything closer together is noise, and noise is what makes a
 * stroke list enormous without changing a single pixel.
 */
object Strokes {

    /** The shortest movement worth a point, in page units (0.3% of the page). */
    const val MIN_STEP = 0.003

    /** No single stroke may hold more than this many points. */
    const val MAX_POINTS = 900

    /** No page may hold more than this many strokes. */
    const val MAX_STROKES = 600

    /**
     * [stroke] with [p] appended, or the same stroke when the finger has
     * barely moved, or when the stroke is full. A full stroke keeps drawing
     * its mark but stops growing: the child's mark is never taken away.
     */
    fun extend(stroke: Stroke, p: Vec2): Stroke {
        if (stroke.points.size >= MAX_POINTS) return stroke
        val last = stroke.points.lastOrNull()
        if (last != null && hypot(p.x - last.x, p.y - last.y) < MIN_STEP) return stroke
        return stroke.plus(p)
    }

    /** The mark a first touch leaves, before the finger has moved at all. */
    fun dot(color: Long, at: Vec2): Stroke = Stroke(color, listOf(at))

    /** The eraser's mark: the same dot, made with the rubber. */
    fun eraseDot(at: Vec2): Stroke =
        Stroke(Stroke.ERASE_COLOR, listOf(at), erase = true)

    /**
     * A whole area colored by the app, for a screen reader: the child cannot
     * draw with a finger they cannot aim, so the app scribbles the area
     * evenly in the crayon in hand and the result is the same kind of mark
     * on the same paper.
     *
     * The scribble is a boustrophedon of horizontal passes, spaced so the
     * passes overlap and read as filled, and every point is placed where the
     * region really is, found by walking the scanline and keeping the points
     * inside the shape. Deterministic: the same area always gets the same
     * scribble, so a restored page and a fresh one look alike.
     */
    fun scribble(region: Region, color: Long, spacing: Double = SCRIBBLE_SPACINGS[0]): Stroke {
        // Three passes, coarse to fine, unless the caller asked for one
        // spacing on purpose. A page full of big areas is scribbled with a
        // few broad passes; a sprinkle sixteen pixels across would be missed
        // by those, so the spacing tightens until the area really is
        // covered. Deterministic: the same area always gets the same
        // scribble, so a restored page and a fresh one look alike.
        for (s in SCRIBBLE_SPACINGS) {
            if (s < spacing - 1e-9) continue
            val stroke = scribbleAt(region, color, s)
            if (stroke.points.size >= 4 && stroke.length() >= SCRIBBLE_ENOUGH) return stroke
        }
        return scribbleAt(region, color, spacing.coerceAtMost(SCRIBBLE_SPACINGS.last()))
    }

    /** How much wax a scribble must lay down before it counts as covering. */
    private const val SCRIBBLE_ENOUGH = 0.05

    private val SCRIBBLE_SPACINGS = doubleArrayOf(0.030, 0.012, 0.005)

    private fun scribbleAt(region: Region, color: Long, spacing: Double): Stroke {
        val b = region.bounds
        if (b.w <= 0.0 || b.h <= 0.0) return Stroke(color, emptyList())
        val rows = ((b.h / spacing).toInt() + 1).coerceIn(1, 64)
        val samples = 48
        val points = ArrayList<Vec2>(rows * 4)
        var flip = false
        for (row in 0..rows) {
            val y = (b.y + b.h * row / rows).coerceIn(0.0, 1.0)
            // Walk the row and keep its widest run inside the region: a
            // row across a ring crosses it twice, and the gap between the
            // crossings is not part of the area.
            var start = -1.0
            var end = -1.0
            var runStart = -1.0
            var runEnd = -1.0
            fun closeRun() {
                if (runStart >= 0.0 && runEnd - runStart > end - start) {
                    start = runStart
                    end = runEnd
                }
                runStart = -1.0
            }
            for (i in 0..samples) {
                val x = b.x + b.w * i / samples
                if (region.contains(Vec2(x, y))) {
                    if (runStart < 0.0) runStart = x
                    runEnd = x
                } else {
                    closeRun()
                }
            }
            closeRun()
            if (start < 0.0) continue
            val a = if (flip) end else start
            val z = if (flip) start else end
            flip = !flip
            val steps = ((kotlin.math.abs(z - a) / MIN_STEP).toInt() + 1).coerceAtMost(64)
            for (i in 0..steps) {
                // The run was found by sampling, so a point exactly between
                // two samples can still fall outside a thin part of the
                // shape. Every point of the scribble is checked against the
                // area it belongs to, so a mark is never placed outside the
                // line it is filling.
                val p = Vec2(a + (z - a) * i / steps, y)
                if (region.contains(p) && p != points.lastOrNull()) points += p
            }
        }
        return Stroke(color, decimate(points, MAX_POINTS))
    }

    /** Keeps the shape while never exceeding [max] points. */
    private fun decimate(points: List<Vec2>, max: Int): List<Vec2> {
        if (points.size <= max) return points
        val step = points.size.toDouble() / max
        return (0 until max).map { points[(it * step).toInt().coerceAtMost(points.size - 1)] }
    }
}

/**
 * The marks a child has made on one page. This is the whole game state: a
 * page plus these strokes is everything a renderer or a test needs.
 *
 * Serialization is coordinates and colors, nothing else, so a stroke list
 * survives a page whose areas are moved in code as easily as one that never
 * changed. Anything malformed, an unreadable color, a point off the paper,
 * is dropped, never guessed at, which is why a corrupt save can only lose
 * marks, never crash a launch.
 */
data class Progress(val strokes: List<Stroke> = emptyList()) {

    /** How many marks are on the paper. */
    val coloredCount: Int get() = strokes.size

    val isEmpty: Boolean get() = strokes.isEmpty()

    fun with(stroke: Stroke): Progress =
        if (stroke.isEmpty || strokes.size >= Strokes.MAX_STROKES) this
        else Progress(strokes + stroke)

    fun cleared(): Progress = Progress()

    fun serialize(): String = strokes.joinToString("|") { stroke ->
        buildString {
            if (stroke.erase) append(ERASE_TAG) else append(stroke.color.toString(16).uppercase())
            append('(')
            for ((i, p) in stroke.points.withIndex()) {
                if (i > 0) append(';')
                append(round3(p.x))
                append(',')
                append(round3(p.y))
            }
            append(')')
        }
    }

    companion object {
        val Empty: Progress = Progress()

        /** What an eraser mark's chunk starts with instead of a color. */
        const val ERASE_TAG = "X"

        /** Never more than this many strokes may be read back from a save. */
        private const val READ_LIMIT = 4000

        /** No stroke read back may hold more points than one drawn may. */
        private const val READ_POINTS = Strokes.MAX_POINTS

        /**
         * Reads back a save written by [serialize]. Anything malformed, an
         * unknown color, a point off the paper, is skipped; the rest of the
         * marks arrive intact.
         */
        fun parse(text: String?): Progress {
            if (text.isNullOrBlank()) return Empty
            val strokes = ArrayList<Stroke>()
            for (chunk in text.split('|')) {
                if (strokes.size >= READ_LIMIT) break
                val open = chunk.indexOf('(')
                val close = chunk.lastIndexOf(')')
                if (open <= 0 || close <= open) continue
                val head = chunk.substring(0, open)
                val erase = head == ERASE_TAG
                val argb = if (erase) Stroke.ERASE_COLOR else head.toLongOrNull(16) ?: continue
                if (!erase && !Crayons.exists(argb)) continue
                val points = ArrayList<Vec2>()
                for (pair in chunk.substring(open + 1, close).split(';')) {
                    if (points.size >= READ_POINTS) break
                    val cut = pair.indexOf(',')
                    if (cut <= 0) continue
                    val x = pair.substring(0, cut).toDoubleOrNull() ?: continue
                    val y = pair.substring(cut + 1).toDoubleOrNull() ?: continue
                    if (x.isNaN() || y.isNaN()) continue
                    if (x < 0.0 || x > 1.0 || y < 0.0 || y > 1.0) continue
                    points += Vec2(x, y)
                }
                if (points.isNotEmpty()) strokes += Stroke(argb, points, erase)
            }
            return Progress(strokes)
        }
    }
}

/** Three decimals of a page unit: a tenth of a pixel on a big tablet. */
private fun round3(v: Double): String {
    val scaled = kotlin.math.round(v * 1000.0) / 1000.0
    return if (scaled == scaled.toLong().toDouble()) scaled.toLong().toString() else scaled.toString()
}
