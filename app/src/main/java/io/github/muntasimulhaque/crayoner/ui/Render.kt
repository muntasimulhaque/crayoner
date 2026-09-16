package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page

/**
 * Colors one page the way a child would: every area is rubbed in with wax,
 * its outline is printed over the wax, and only then is the next area
 * covered.
 *
 * An unfilled area shows paper, so passing no fills at all draws the bare
 * line art the child colors on. That is the picture on the sheet; the sample
 * passes every area its own color and is the picture on the box.
 *
 * [keepGoing] is asked before each area, because a page is a real piece of
 * drawing and some of that drawing happens where nobody is waiting for it: a
 * picture being made in the background for a screen that has already moved
 * on should cost the area it is in the middle of and nothing more. The
 * sheet the child is looking at passes the default, because a page someone
 * is looking at is always worth finishing.
 */
fun DrawScope.drawPage(
    page: Page,
    geometry: PageGeometry,
    fills: Map<Int, Long>,
    keepGoing: () -> Boolean = { true },
) {
    val ink = Color(Crayons.INK)
    val stroke = pageOutlineStroke(size.width)
    for ((index, _) in page.regions.withIndex()) {
        if (!keepGoing()) return
        val argb = fills[index]
        val outline = geometry.outlines[index]
        if (argb != null) {
            drawWaxFill(geometry.region(index), outline, Color(argb))
        }
        if (!page.isGround(index)) {
            drawPath(outline, ink, style = stroke)
        }
    }
}
