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
 * The sheet is taller than it is wide ([Page.ASPECT]), and page units are
 * isotropic, so one pixel scale serves both axes: a page drawn at [widthPx]
 * across is [Page.ASPECT] times that tall, and a mark is exactly as wide
 * whichever way the hand dragged it.
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
    widthPx: Int,
    heightPx: Int,
    strokes: List<WaxStroke> = emptyList(),
    generation: Long = 0L,
    live: WaxStroke? = null,
    blocking: Boolean = true,
) {
    val image = rememberPageImage(page, fills, widthPx, heightPx, blocking)
    val flat = rememberMarkImage(image, strokes, widthPx, heightPx, generation)
    // The eraser paints with the printed page itself, and the brush around
    // it is built once per picture: a live rubber mark is redrawn on every
    // frame of the hand's travel, and a new native shader on every frame is
    // a cost the hand pays for nothing.
    val print = remember(image) { image?.let { printBrush(it) } }
    Canvas(modifier = modifier) {
        val frame = size.width
        if (image != null) {
            drawImage(image)
            if (flat != null) drawImage(flat)
        } else {
            // The slow path: the paper's own units, drawn at the frame's scale.
            drawPage(page, liveGeometry(page, frame), fills)
            if (flat == null) drawStrokes(strokes, frame)
        }
        // The mark under the finger is drawn last and drawn live, with the
        // printed page as its paint when it is the rubber: the child sees
        // the wax come off exactly where they are rubbing.
        if (live != null) {
            drawStrokes(listOf(live), frame, print)
        }
    }
}

/**
 * One page's paths at one width, built once and kept for the frame it is
 * drawn in. The map is tiny and bounded: a page is drawn at one size at a
 * time on one screen.
 */
private val geometries = object : LinkedHashMap<Pair<String, Float>, PageGeometry>(4) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<Pair<String, Float>, PageGeometry>?,
    ): Boolean = size > GEOMETRY_LIMIT
}

private const val GEOMETRY_LIMIT = 6

private fun liveGeometry(page: Page, width: Float): PageGeometry =
    geometries.getOrPut(page.id to width) { PageGeometry(page, width) }
