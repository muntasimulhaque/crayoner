package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.PageView
import io.github.muntasimulhaque.crayoner.core.Stroke as WaxStroke

/**
 * The printed page, with the child's own marks on top of it.
 *
 * Three layers, drawn cheapest first: the printed picture (one image, made
 * once per page, size and window), every finished mark flattened into one
 * more image ([generation] says when to rebuild it), and the single mark
 * still under the finger. A page carrying two hundred marks costs two images
 * and one path to draw, which is what keeps a long coloring session smooth.
 *
 * Every layer is drawn through the same [view], the window onto the paper,
 * and at the frame's own resolution: a closer look is a sharper look, not a
 * magnified one.
 *
 * The print may be absent (a device that could not allocate the bitmap, or a
 * shelf card whose picture is still being rendered off the main thread), in
 * which case the picture is drawn live every frame: slower, and correct. A
 * live page is drawn through the same window, so the fallback shows exactly
 * what the image would have shown.
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
    view: PageView = PageView.Whole,
    blocking: Boolean = true,
) {
    val image = rememberPageImage(page, fills, sidePx, view, blocking)
    val flat = rememberMarkImage(image, strokes, sidePx, generation, view)
    Canvas(modifier = modifier) {
        val frame = size.width
        if (image != null) {
            drawImage(image)
            if (flat != null) drawImage(flat)
        } else {
            // The slow path: the paper's own units, seen through the window.
            val side = frame / view.span.toFloat()
            inWindow(view, frame) {
                drawPage(page, liveGeometry(page, side), fills)
            }
            if (flat == null) drawStrokes(strokes, frame, view)
        }
        // The mark under the finger is drawn last and drawn live, with the
        // printed page as its paint when it is the rubber: the child sees
        // the wax come off exactly where they are rubbing.
        if (live != null) {
            val print = image?.let { printBrush(it) }
            drawStrokes(listOf(live), frame, view, print)
        }
    }
}

/**
 * One page's paths at one size, built once and kept for the frame it is
 * drawn in. The map is tiny and bounded: a page is drawn at one size at a
 * time on one screen, and a picture that is looked at more closely is the
 * same page at another size.
 */
private val geometries = object : LinkedHashMap<Pair<String, Float>, PageGeometry>(4) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<Pair<String, Float>, PageGeometry>?,
    ): Boolean = size > GEOMETRY_LIMIT
}

private const val GEOMETRY_LIMIT = 6

private fun liveGeometry(page: Page, side: Float): PageGeometry =
    geometries.getOrPut(page.id to side) { PageGeometry(page, side) }

/**
 * Runs [block] with the drawing space laid on the window: the page is drawn
 * at the window's own scale, slid so the window's corner sits in the frame's
 * corner, and clipped to the frame so nothing spills onto the desk.
 */
internal fun DrawScope.inWindow(view: PageView, frame: Float, block: DrawScope.(Float) -> Unit) {
    if (view.isWhole) {
        block(frame)
        return
    }
    val side = frame * view.scale.toFloat()
    clipRect {
        translate(left = (-view.left * side).toFloat(), top = (-view.top * side).toFloat()) {
            block(side)
        }
    }
}
