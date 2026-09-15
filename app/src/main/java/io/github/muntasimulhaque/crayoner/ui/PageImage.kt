package io.github.muntasimulhaque.crayoner.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.muntasimulhaque.crayoner.core.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * The page as printed, rendered once for this page and this size, and reused
 * from then on.
 *
 * When [blocking] is true the picture is rendered during composition if it is
 * not there yet, which is what the sheet the child is coloring wants: one
 * page, once, before it is looked at. The shelf asks for [blocking] false, so
 * a card never stalls a frame: it draws the page's own print until its
 * picture lands, and every card is rendered off the main thread the moment
 * the shelf knows how wide a card is (see [prewarmPageImages]). [keep] says
 * the picture belongs to the wall, and the wall's pictures are held in their
 * own store where a page can never push them out.
 */
@Composable
fun rememberPageImage(
    page: Page,
    fills: Map<Int, Long>,
    widthPx: Int,
    heightPx: Int,
    blocking: Boolean = true,
    keep: Boolean = false,
): ImageBitmap? {
    if (widthPx <= 0 || heightPx <= 0) return null
    val sample = fills.isNotEmpty()
    val key = Key(page.id, sample, widthPx, heightPx)
    val store = if (keep) shelfImages else pageImages
    val cached = remember(page, sample, widthPx, heightPx, keep) { store.get(key) }
    if (blocking) {
        // The store is read on every composition rather than remembered: a
        // picture that was being rendered in the background may have landed
        // since the last look, and finding it late is the whole point of
        // having it. The answer IS remembered when there is nothing there
        // yet, so a page is still rendered once.
        return store.get(key) ?: remember(page, sample, widthPx, heightPx, keep) {
            renderPageImage(page, fills, widthPx, heightPx)?.also { store.put(key, it) }
        }
    }
    var image by remember(page, sample, widthPx, heightPx, keep) { mutableStateOf(cached) }
    LaunchedEffect(page, sample, widthPx, heightPx, keep) {
        if (image != null) return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            renderGate.withLock {
                store.get(key) ?: renderPageImage(page, fills, widthPx, heightPx)
                    ?.also { store.put(key, it) }
            }
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
 * The wall's pictures are exact needs and nothing more: the width it is
 * asked to render at is the card's own width to the pixel, because a
 * prewarmed picture no card can find is work done for nobody, and it was,
 * once, when the prewarm measured the grid's cell while every card draws its
 * own inner plate.
 *
 * Anything that goes wrong (a device out of memory for one more sheet of
 * pixels) is skipped: the shelf then draws that picture's own print instead,
 * which is slower and still correct.
 */
suspend fun prewarmPageImages(
    pages: List<Page>,
    fills: (Page) -> Map<Int, Long>,
    widthPx: Int,
    keep: Boolean = false,
) {
    if (widthPx <= 0) return
    val store = if (keep) shelfImages else pageImages
    if (keep) store.keepWidth(widthPx)
    val height = heightFor(widthPx)
    for (page in pages) {
        val key = Key(page.id, sample = true, widthPx, height)
        if (store.get(key) != null) continue
        renderGate.withLock {
            if (store.get(key) != null) return@withLock
            val image = renderPageImage(page, fills(page), widthPx, height) ?: return@withLock
            store.put(key, image)
        }
        // Between pictures, let everything else on the default pool have the
        // thread: a wall of sixteen sheets is real work, and none of it is
        // more urgent than the frames a finger is already asking for, which
        // is the whole reason the wall's pictures are made here rather than
        // in the frame that needed them.
        yield()
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
        // The page's paths come from the one cache that builds them, so a
        // picture being printed and the same picture drawn live share the
        // work: the union of an area's own shapes is arithmetic, and it is
        // not worth doing twice for one page.
        drawPage(page, liveGeometry(page, widthPx.toFloat()), fills)
    }
    image
}.getOrNull()

