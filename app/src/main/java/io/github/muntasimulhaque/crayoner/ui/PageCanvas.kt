package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke

/**
 * The printed page, with the child's own marks on top of it.
 *
 * Three layers, drawn cheapest first: the printed picture (one image, made
 * once per page and size), every finished mark flattened into one more image
 * ([generation] says when to rebuild it), and the single mark still under the
 * finger. A page carrying two hundred marks costs two images and one path to
 * draw, which is what keeps a long coloring session smooth.
 *
 * The print may be absent (a device that could not allocate the bitmap, or a
 * shelf card whose picture is still being rendered off the main thread), in
 * which case the picture is drawn live every frame: slower, and correct.
 */
@Composable
fun PageCanvas(
    page: Page,
    fills: Map<Int, Long>,
    modifier: Modifier = Modifier,
    sidePx: Int,
    strokes: List<WaxStroke> = emptyList(),
    generation: Long = 0L,
    live: WaxStroke? = null,
    blocking: Boolean = true,
    overlay: DrawScope.(PageGeometry) -> Unit = {},
) {
    val geometry = remember(page, sidePx) { PageGeometry(page, sidePx.toFloat()) }
    val image = rememberPageImage(page, fills, sidePx, blocking)
    val print = remember(image) { image?.let { printBrush(it) } }
    val flat = rememberMarkImage(image, strokes, sidePx, generation)
    Canvas(modifier = modifier) {
        if (image != null) {
            drawImage(image)
            if (flat != null) drawImage(flat)
        } else {
            drawPage(page, geometry, fills)
            if (flat == null) drawStrokes(strokes, size.width)
        }
        // The mark under the finger is drawn last and drawn live, with the
        // printed page as its paint when it is the rubber: the child sees
        // the wax come off exactly where they are rubbing.
        if (live != null) drawStrokes(listOf(live), size.width, print)
        overlay(geometry)
    }
}
