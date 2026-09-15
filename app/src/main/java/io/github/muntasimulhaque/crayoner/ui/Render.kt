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
 */
fun DrawScope.drawPage(
    page: Page,
    geometry: PageGeometry,
    fills: Map<Int, Long>,
) {
    val ink = Color(Crayons.INK)
    val stroke = pageOutlineStroke(size.width)
    for ((index, _) in page.regions.withIndex()) {
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
