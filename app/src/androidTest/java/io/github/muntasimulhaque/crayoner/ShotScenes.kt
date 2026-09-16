package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Draft
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Region
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.core.Wax
import io.github.muntasimulhaque.crayoner.host.Screen
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The states the store captures are made of: one page, in each of the
 * positions a child's hand can leave it in.
 *
 * The marks are real marks, not decoration. Every colored area is swept the
 * way an arm covers a shape, at the angle the page's own wax is rubbed at,
 * with each sweep a little longer or shorter than the one before and the
 * wrist wobbling along it. That is what makes a capture a picture of a child
 * coloring rather than a picture of a program that knows how to fill a
 * shape. The fixtures live beside the capture test rather than inside it
 * because they are content: a page added to the book, or a color changed,
 * arrives here and not in the machinery of copying pixels off the window.
 */

/** One page of the book, by id, or a loud failure. */
internal fun shotPage(id: String): Page = Pages.byId(id) ?: error("no page $id")

/** A brand new page: paper, print, and nothing else. */
internal fun blankState(id: String) =
    Screen.Coloring(page = shotPage(id), draft = Draft.Empty)

/**
 * A page a child has been working on: real marks, made the way a hand
 * makes them, in the colors the picture asks for, with the last area
 * still bare so the sheet reads as unfinished.
 */
internal fun coloredState(
    id: String,
    crayon: Long,
    erasing: Boolean = false,
): Screen.Coloring {
    val page = shotPage(id)
    val last = page.regionCount - 1
    val strokes = page.regions.indices
        .filter { it != last }
        .flatMap { index -> sweeps(page.regions[index]) }
    return Screen.Coloring(
        page = page,
        draft = Draft.of(Progress(strokes)),
        crayon = crayon,
        erasing = erasing,
    )
}

/** A page the child has taken all the way: every area has been colored. */
internal fun wholeState(id: String, crayon: Long): Screen.Coloring {
    val page = shotPage(id)
    val strokes = page.regions.indices.flatMap { index -> sweeps(page.regions[index]) }
    return Screen.Coloring(
        page = page,
        draft = Draft.of(Progress(strokes)),
        crayon = crayon,
    )
}

/**
 * One area, colored in by hand: a few long sweeps across it, each at a
 * slightly different angle and slightly different length, the way an arm
 * covers a shape. Some strokes run a little past the line, because a
 * three year old's do.
 */
internal fun sweeps(region: Region): List<Stroke> {
    val b = region.bounds
    if (b.w <= 0.0 || b.h <= 0.0) return emptyList()
    val angle = Math.toRadians(Wax.angleDeg(region))
    val dx = cos(angle)
    val dy = sin(angle)
    val nx = -dy
    val ny = dx
    val reach = hypot(b.w, b.h) * 0.55
    val lanes = (minOf(b.w, b.h) / 0.055).toInt().coerceIn(2, 9)
    val seed = abs(region.id.hashCode())
    return (0 until lanes).map { lane ->
        val t = (lane + 0.5) / lanes - 0.5
        val offX = b.center.x + nx * t * b.w * 0.9
        val offY = b.center.y + ny * t * b.h * 0.9
        val wobble = 0.012 + 0.004 * ((seed + lane) % 3)
        val points = ArrayList<Vec2>(24)
        val steps = 18
        for (i in 0..steps) {
            val u = i.toDouble() / steps
            val spread = (u - 0.5) * 2.0 * reach
            // The wrist wobbles along the sweep, and no two sweeps wobble
            // the same way.
            val w = sin(u * 3.0 * PI + lane) * wobble + sin(u * 7.0 * PI + seed) * wobble * 0.4
            points += Vec2(
                offX + dx * spread + nx * w,
                offY + dy * spread + ny * w,
            )
        }
        Stroke(region.fillArgb, points)
    }
}

/**
 * A page the child has gone over with the rubber: colored in, then rubbed
 * at, so the capture shows what an eraser mark really leaves behind. The
 * print comes back; the wax does not.
 */
internal fun erasedState(id: String): Screen.Coloring {
    val page = shotPage(id)
    val last = page.regionCount - 1
    val colored = page.regions.indices
        .filter { it != last }
        .flatMap { index -> sweeps(page.regions[index]) }
    val rubbed = listOf(
        Stroke(
            Stroke.ERASE_COLOR,
            (0..24).map { i ->
                val t = i / 24.0
                Vec2(0.20 + t * 0.55, 0.42 + sin(t * 5.0) * 0.05)
            },
            erase = true,
        ),
    )
    return Screen.Coloring(
        page = page,
        draft = Draft.of(Progress(colored + rubbed)),
        crayon = page.regions.last().fillArgb,
        erasing = true,
    )
}
