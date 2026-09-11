package io.github.muntasimulhaque.crayoner.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import io.github.muntasimulhaque.crayoner.core.ArcBand
import io.github.muntasimulhaque.crayoner.core.Circ
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Ell
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Poly
import io.github.muntasimulhaque.crayoner.core.RRect
import io.github.muntasimulhaque.crayoner.core.Region
import io.github.muntasimulhaque.crayoner.core.Shape
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.core.Wax

/**
 * The device half of the one renderer. A picture is printed the way a real
 * coloring book prints it: each area is colored in wax, and its outline is
 * drawn on top, so a later area's color covers an earlier area's line and
 * hidden edges vanish.
 *
 * The paths for one page at one size are built once and kept, because a page
 * is otherwise rebuilt on every touch of a crayon, and the whole printed
 * picture is kept as pixels (see [rememberPageImage]); a finger crossing the
 * paper then costs one image and a handful of marks, however rich the wax
 * underneath is.
 */
class PageGeometry(private val page: Page, private val side: Float) {

    /** One path per area, the union of its parts. */
    val outlines: List<Path> = page.regions.map { region ->
        unionOf(region.parts, side.toDouble())
    }

    /** The area at [index], for the wax its color is made of. */
    fun region(index: Int): Region? = page.region(index)
}

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
