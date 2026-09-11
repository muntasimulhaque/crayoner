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
 */
private class Key(val pageId: String, val sample: Boolean, val sidePx: Int)

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

/**
 * The page as printed, rendered once for this page, this size and this kind
 * of picture (the bare lines the child colors, or the finished sample), and
 * reused from then on.
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
    sidePx: Int,
    blocking: Boolean = true,
): ImageBitmap? {
    if (sidePx <= 0) return null
    val sample = fills.isNotEmpty()
    val key = Key(page.id, sample, sidePx)
    val cached = remember(page, sample, sidePx) { pageImages.get(key) }
    if (blocking) {
        return cached ?: remember(page, sample, sidePx) {
            renderPageImage(page, fills, sidePx)?.also { pageImages.put(key, it) }
        }
    }
    var image by remember(page, sample, sidePx) { mutableStateOf(cached) }
    LaunchedEffect(page, sample, sidePx) {
        if (image != null) return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            pageImages.get(key)?.also { return@withContext it }
            renderPageImage(page, fills, sidePx)?.also { pageImages.put(key, it) }
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
 * Anything that goes wrong (a device out of memory for one more square of
 * pixels) is skipped: the shelf then draws that picture live, which is
 * slower and still correct.
 */
fun prewarmPageImages(pages: List<Page>, fills: (Page) -> Map<Int, Long>, sidePx: Int) {
    if (sidePx <= 0) return
    for (page in pages) {
        val key = Key(page.id, sample = true, sidePx = sidePx)
        if (pageImages.get(key) != null) continue
        val image = renderPageImage(page, fills(page), sidePx) ?: continue
        pageImages.put(key, image)
    }
}

/**
 * Renders the printed page into a bitmap. Anything that goes wrong in here
 * (a device out of memory for one more square of pixels) answers null, and
 * the caller draws the page directly instead: a slower frame is a much
 * better outcome than a crash.
 */
private fun renderPageImage(page: Page, fills: Map<Int, Long>, sidePx: Int): ImageBitmap? = runCatching {
    val bitmap = Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888)
    val image = bitmap.asImageBitmap()
    val side = sidePx.toFloat()
    val geometry = PageGeometry(page, side)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(image),
        size = Size(side, side),
    ) {
        drawPage(page, geometry, fills)
    }
    image
}.getOrNull()

/**
 * Every finished mark, flattened into one layer over the printed page, so a
 * finger moving across paper that already carries two hundred marks costs
 * one image and the one mark under the finger, and not two hundred and one.
 *
 * [generation] is the host's own count of finished marks: when it changes,
 * one more mark exists and the layer is rebuilt exactly once.
 */
@Composable
fun rememberMarkImage(
    print: ImageBitmap?,
    strokes: List<Stroke>,
    sidePx: Int,
    generation: Long,
): ImageBitmap? {
    if (print == null || sidePx <= 0 || strokes.isEmpty()) return null
    return remember(print, generation, sidePx) {
        renderMarkImage(print, strokes, sidePx)
    }
}

private fun renderMarkImage(print: ImageBitmap, strokes: List<Stroke>, sidePx: Int): ImageBitmap? =
    runCatching {
        val bitmap = Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888)
        val image = bitmap.asImageBitmap()
        val side = sidePx.toFloat()
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(image),
            size = Size(side, side),
        ) {
            drawStrokes(strokes, side, printBrush(print))
        }
        image
    }.getOrNull()

/**
 * The page itself as a paint, so the eraser can lay the print back down
 * wherever the rubber travels. Built once per picture, never per frame.
 */
fun printBrush(image: ImageBitmap): Brush = ShaderBrush(
    ImageShader(image, TileMode.Clamp, TileMode.Clamp),
)
