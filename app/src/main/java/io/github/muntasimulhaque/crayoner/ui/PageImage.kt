package io.github.muntasimulhaque.crayoner.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The printed picture, and the wax on top of it, kept as pixels.
 *
 * Coloring a page means rubbing wax over it hundreds of passes at a time, and
 * the shelf shows sixteen of those pictures at once. Rebuilding them on every
 * frame is what makes a wall of sixteen feel like work; rendering each page
 * at each size once, and then drawing one image, is what makes it feel like
 * paper. The same picture is used by the shelf cards, the sample held up
 * close, and the sheet being colored, so all of them show the same print.
 *
 * A sheet is taller than it is wide by [Page.ASPECT], and page units are
 * isotropic, so one pixel scale serves both axes: an image is [widthPx]
 * across and that many times the aspect tall. A picture is always rendered
 * at the size it is being looked at, never drawn small and then blown up, so
 * the wax grain a child sees is the grain the paper really has.
 *
 * A size is part of a picture's identity, so the shelf's card and the sheet
 * are two different pictures and each is built once. The one the child is
 * coloring is rebuilt only when a mark is finished, and never while the
 * finger is down.
 */
private class Key(val pageId: String, val sample: Boolean, val widthPx: Int, val heightPx: Int)

private class Cache {
    /** Newest first: read order is access order, so the wall's own order wins. */
    private val entries = LinkedHashMap<Key, ImageBitmap>(16, 0.75f, true)
    private var bytes = 0L

    @Synchronized
    fun get(key: Key): ImageBitmap? = entries[key]

    @Synchronized
    fun put(key: Key, image: ImageBitmap) {
        val size = image.width.toLong() * image.height * 4
        if (size > BUDGET) return
        entries[key] = image
        bytes += size
        val stale = entries.entries.iterator()
        while (bytes > BUDGET && stale.hasNext()) {
            val eldest = stale.next()
            if (eldest.key == key) continue
            bytes -= eldest.value.width.toLong() * eldest.value.height * 4
            stale.remove()
        }
    }

    private companion object {
        /** Not much more than a full shelf at tablet size, and no more. */
        const val BUDGET = 28L * 1024 * 1024
    }
}

private val pageImages = Cache()

/** The height an image of a given width has, rounded up: page units again. */
private fun heightFor(widthPx: Int): Int =
    Math.ceil(widthPx * Page.ASPECT).toInt().coerceAtLeast(1)

/**
 * The page as printed, rendered once for this page and this size, and reused
 * from then on.
 *
 * When [blocking] is true the picture is rendered during composition if it is
 * not there yet, which is what the sheet the child is coloring wants: one
 * page, once, before it is looked at. The shelf asks for [blocking] false, so
 * a card never stalls a frame: it draws live until its picture lands, and
 * every card is rendered off the main thread the moment the shelf knows how
 * wide a card is (see [prewarmPageImages]).
 */
@Composable
fun rememberPageImage(
    page: Page,
    fills: Map<Int, Long>,
    widthPx: Int,
    heightPx: Int,
    blocking: Boolean = true,
): ImageBitmap? {
    if (widthPx <= 0 || heightPx <= 0) return null
    val sample = fills.isNotEmpty()
    val key = Key(page.id, sample, widthPx, heightPx)
    val cached = remember(page, sample, widthPx, heightPx) { pageImages.get(key) }
    if (blocking) {
        return cached ?: remember(page, sample, widthPx, heightPx) {
            renderPageImage(page, fills, widthPx, heightPx)?.also { pageImages.put(key, it) }
        }
    }
    var image by remember(page, sample, widthPx, heightPx) { mutableStateOf(cached) }
    LaunchedEffect(page, sample, widthPx, heightPx) {
        if (image != null) return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            pageImages.get(key)?.also { return@withContext it }
            renderPageImage(page, fills, widthPx, heightPx)?.also { pageImages.put(key, it) }
        }
        image = rendered
    }
    return image
}

/**
 * Renders the whole shelf ahead of time, on whatever thread the caller is
 * on, so the first scroll down the wall meets pictures that are already
 * drawn rather than sixteen pages of wax being made as the finger moves.
 *
 * Anything that goes wrong (a device out of memory for one more sheet of
 * pixels) is skipped: the shelf then draws that picture live, which is
 * slower and still correct.
 */
fun prewarmPageImages(pages: List<Page>, fills: (Page) -> Map<Int, Long>, widthPx: Int) {
    if (widthPx <= 0) return
    val height = heightFor(widthPx)
    for (page in pages) {
        val key = Key(page.id, sample = true, widthPx, height)
        if (pageImages.get(key) != null) continue
        val image = renderPageImage(page, fills(page), widthPx, height) ?: continue
        pageImages.put(key, image)
    }
}

/**
 * Renders the printed page at one size. Anything that goes wrong in here (a
 * device out of memory for one more sheet of pixels) answers null, and the
 * caller draws the page directly instead: a slower frame is a much better
 * outcome than a crash.
 *
 * The paper goes down first, and it is opaque. The rubber is drawn by laying
 * this image back over the wax, so it has to carry the sheet itself and not
 * only the lines printed on it: an image with a transparent ground would put
 * nothing back and the wax would stay.
 */
private fun renderPageImage(
    page: Page,
    fills: Map<Int, Long>,
    widthPx: Int,
    heightPx: Int,
): ImageBitmap? = runCatching {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val image = bitmap.asImageBitmap()
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(image),
        size = Size(widthPx.toFloat(), heightPx.toFloat()),
    ) {
        drawRect(CrayonerColors.Card)
        drawPage(page, PageGeometry(page, widthPx.toFloat()), fills)
    }
    image
}.getOrNull()

/**
 * Every finished mark, flattened into one layer over the printed page, so a
 * finger moving across paper that already carries two hundred marks costs
 * one image and the one mark under the finger, and not two hundred and one.
 *
 * [generation] is the host's own count of changes to the paper: when it
 * changes, the layer is rebuilt exactly once.
 */
@Composable
fun rememberMarkImage(
    print: ImageBitmap?,
    strokes: List<Stroke>,
    widthPx: Int,
    heightPx: Int,
    generation: Long,
): ImageBitmap? {
    if (print == null || widthPx <= 0 || heightPx <= 0 || strokes.isEmpty()) return null
    return remember(print, generation, widthPx, heightPx) {
        renderMarkImage(print, strokes, widthPx, heightPx)
    }
}

private fun renderMarkImage(
    print: ImageBitmap,
    strokes: List<Stroke>,
    widthPx: Int,
    heightPx: Int,
): ImageBitmap? = runCatching {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val image = bitmap.asImageBitmap()
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(image),
        size = Size(widthPx.toFloat(), heightPx.toFloat()),
    ) {
        // The print image is already the sheet at the frame's own size, so
        // the rubber paints back exactly the paper the child is looking at.
        drawStrokes(strokes, widthPx.toFloat(), printBrush(print))
    }
    image
}.getOrNull()

/**
 * The page itself as a paint, paper and print together, so the eraser can
 * lay it back down wherever the rubber travels. Built once per picture,
 * never per frame.
 */
fun printBrush(image: ImageBitmap): Brush = ShaderBrush(
    ImageShader(image, TileMode.Clamp, TileMode.Clamp),
)
