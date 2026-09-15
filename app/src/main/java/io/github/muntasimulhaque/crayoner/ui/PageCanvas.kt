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
 * [live] is the mark under the finger, handed over as a question rather than
 * as an answer, and it is asked inside the draw itself. That is the whole of
 * how a touch becomes wax in the same frame the finger moved: nothing that
 * draws this canvas reads the mark while composing, so a move event costs a
 * redraw of one canvas and not a recomposition of the screen it sits on.
 *
 * The print may be absent (a device that could not allocate the bitmap, or a
 * shelf card whose picture is still being rendered off the main thread). A
 * page the child is looking at draws its own print in that case, slower and
 * correct; a card on the wall draws its print and nothing else, because the
 * wall is the one screen a hand scrolls while it draws.
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
    live: (() -> WaxStroke?)? = null,
    blocking: Boolean = true,
    keep: Boolean = false,
) {
    val image = rememberPageImage(page, fills, widthPx, heightPx, blocking, keep)
    val flat = rememberMarkImage(image, strokes, widthPx, heightPx, generation)
    // The eraser paints with the printed page itself, and the brush around
    // it is built once per picture: a live rubber mark is redrawn on every
    // frame of the hand's travel, and a new native shader on every frame is
    // a cost the hand pays for nothing.
    val print = remember(image) { image?.let { printBrush(it) } }
    Canvas(modifier = modifier) {
        val frame = size.width
        when {
            image != null -> {
                drawImage(image)
                if (flat != null) drawImage(flat)
            }
            blocking -> {
                // The slow path: the paper's own units, drawn at the frame's
                // scale, with the marks over them.
                drawPage(page, liveGeometry(page, frame), fills)
                if (flat == null) drawStrokes(strokes, frame)
            }
            else -> {
                // A card whose picture is still being made, on a wall a
                // finger may already be scrolling. It shows the picture's own
                // print and only its print: the wax is what a picture costs,
                // and a card that laid its own wax down on every frame of a
                // scroll would be the stutter the wall is not allowed to
                // have. Outlines are the picture's own lines, so what the
                // child sees is still the picture, uncolored.
                drawRect(CrayonerColors.Card)
                drawPage(page, liveGeometry(page, frame), emptyMap())
            }
        }
        // The mark under the finger is drawn last and drawn live, with the
        // printed page as its paint when it is the rubber: the child sees
        // the wax come off exactly where they are rubbing.
        live?.invoke()?.let { drawStrokes(listOf(it), frame, print) }
    }
}

/**
 * One page's paths at one width, built once and kept for the frame it is
 * drawn in. The map is bounded, and the bound is the whole book: a wall with
 * sixteen cards whose pictures are still being made asks for every page's
 * paths at one width, and a cache that held six of them would rebuild the
 * other ten on the next frame, which is a scroll stutter made by arithmetic.
 * Sixteen pages at one width, and room for the sheet's width beside them.
 *
 * It is read from the frame and from the background renders alike, so it is
 * guarded: the union of an area's shapes is the expensive part of a page and
 * two threads asking for the same one is exactly the work this avoids.
 */
private val geometries = object : LinkedHashMap<Pair<String, Float>, PageGeometry>(40) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<Pair<String, Float>, PageGeometry>?,
    ): Boolean = size > GEOMETRY_LIMIT
}

private const val GEOMETRY_LIMIT = 40

internal fun liveGeometry(page: Page, width: Float): PageGeometry =
    synchronized(geometries) {
        geometries.getOrPut(page.id to width) { PageGeometry(page, width) }
    }
