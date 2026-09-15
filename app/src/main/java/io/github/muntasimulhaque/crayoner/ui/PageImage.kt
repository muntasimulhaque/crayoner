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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

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
/**
 * One rendered picture's identity: the page, whether it is the finished
 * sample, and the exact pixel size it was drawn at.
 *
 * It is a data class on purpose. A key that compares by identity means the
 * cache never finds anything: every look-up makes a new key object, every
 * look-up misses, and every screen draws its own copy of a picture that was
 * already drawn. The wall's own prewarm, the peek's, the bar's and the
 * sheet's all went that way, and the store's whole reason for existing (draw
 * each page at each size once) was quietly not happening.
 */
private data class Key(val pageId: String, val sample: Boolean, val widthPx: Int, val heightPx: Int)

private fun bytesOf(image: ImageBitmap): Long =
    image.width.toLong() * image.height * 4

/**
 * A bounded store of rendered pictures, the least recently used let go
 * first.
 *
 * There are two of these, and the split is the point. The wall's sixteen
 * cards are the whole book: they are looked at together, they are the
 * largest thing on the screen, and dropping one costs a picture and the
 * work of drawing it again. The page the child is coloring is the opposite:
 * one print, one layer of marks, and a sample held up close, all of them
 * coming and going with the page. Under one budget the page's pictures
 * pushed the wall's out, and a card with no picture left is a card that has
 * to be drawn line by line on the frame the finger is already asking for,
 * which is exactly the stutter the wall is not allowed to have.
 */
private class Cache(private val budget: Long) {
    /** Newest first: read order is access order, so the wall's own order wins. */
    private val entries = LinkedHashMap<Key, ImageBitmap>(16, 0.75f, true)
    private var bytes = 0L

    @Synchronized
    fun get(key: Key): ImageBitmap? = entries[key]

    @Synchronized
    fun put(key: Key, image: ImageBitmap) {
        val size = bytesOf(image)
        entries.remove(key)?.let { bytes -= bytesOf(it) }
        if (size > budget) return
        entries[key] = image
        bytes += size
        val stale = entries.entries.iterator()
        while (bytes > budget && stale.hasNext()) {
            val eldest = stale.next()
            if (eldest.key == key) continue
            bytes -= bytesOf(eldest.value)
            stale.remove()
        }
    }

    /**
     * Let go of everything drawn at a width other than [widthPx].
     *
     * The wall's pictures are all drawn at one width, and when the desk
     * changes shape (a rotation, a fold, a split screen) that width moves
     * with it: the pictures for the old width are pictures of a size nothing
     * on the screen is asking for any more, and they are the largest thing
     * in the cache.
     */
    @Synchronized
    fun keepWidth(widthPx: Int) {
        val stale = entries.entries.iterator()
        while (stale.hasNext()) {
            val eldest = stale.next()
            if (eldest.key.widthPx == widthPx) continue
            bytes -= bytesOf(eldest.value)
            stale.remove()
        }
    }
}

/** The wall's own pictures: the whole book, and it has to stay on the wall. */
private val shelfImages = Cache(32L * 1024 * 1024)

/** Everything else: the sheet, its marks, the sample held up, the bar. */
private val pageImages = Cache(20L * 1024 * 1024)

/**
 * One render at a time, whoever is asking.
 *
 * A picture of a page is a real piece of drawing, and two of them at once
 * take two cores away from the frame a finger is asking for. The gate is
 * also the priority: whoever asks first is served first, so a card that has
 * just scrolled into view is drawn before the pages after it in the wall's
 * own march through the book, and the second asker for a picture the first
 * one already made finds it in the store instead of drawing it again.
 */
private val renderGate = Mutex()

/** The height an image of a given width has, rounded up: page units again. */
internal fun heightFor(widthPx: Int): Int =
    Math.ceil(widthPx * Page.ASPECT).toInt().coerceAtLeast(1)

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

/**
 * Every finished mark, flattened into one layer over the printed page, so a
 * finger moving across paper that already carries two hundred marks costs
 * one image and the one mark under the finger, and not two hundred and one.
 *
 * [generation] is the host's own count of changes to the paper: when it
 * changes, the layer moves on by exactly one step. A finished mark only ever
 * adds to the top of the pile, so the one thing a new mark costs is that
 * mark drawn onto the layer already there, and the hundreds under it are not
 * drawn again. Undo and the rubber change the pile itself, and those rebuild
 * the layer whole; they are presses, not the middle of a drawing hand.
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
    val layer = remember(print, widthPx, heightPx) { MarkLayer() }
    return remember(print, generation, widthPx, heightPx) {
        layer.of(print, strokes, widthPx, heightPx)
    }
}

/**
 * The marks' own layer, and the list it was made from, kept between marks.
 *
 * A finished mark is added to the layer that is already there, in place. The
 * layer is a full sheet of pixels (megabytes of them), and making a fresh
 * copy of that sheet every time a mark lands was a stall the hand felt at
 * the end of every stroke; drawing the one new mark onto the layer costs the
 * mark and nothing else.
 */
private class MarkLayer {
    private var strokes: List<Stroke> = emptyList()
    private var image: ImageBitmap? = null

    fun of(
        print: ImageBitmap,
        strokes: List<Stroke>,
        widthPx: Int,
        heightPx: Int,
    ): ImageBitmap? {
        val old = image
        // The list has to be the same paper plus whatever was done on top of
        // it: every stroke the layer already holds, in the same order, by
        // identity, because a rebuilt page can hold equal marks that are
        // not the ones the layer drew.
        val extends = old != null && strokes.size >= this.strokes.size &&
            this.strokes.indices.all { this.strokes[it] === strokes[it] }
        val next = when {
            old == null || !extends -> renderMarkImage(print, strokes, widthPx, heightPx)
            strokes.size == this.strokes.size -> old
            else -> appendMarks(
                print,
                old,
                strokes.subList(this.strokes.size, strokes.size),
                widthPx,
                heightPx,
            )
        }
        this.strokes = strokes
        this.image = next
        return next
    }
}

/**
 * The new marks, drawn onto the layer that is already there and left there.
 *
 * A bitmap remembers when it was last written to, and every graphics engine
 * that has it cached (the wheel's own texture of the page, for one) asks
 * before it reuses its copy, so drawing into the layer is what tells the
 * screen the picture moved on.
 */
private fun appendMarks(
    print: ImageBitmap,
    layer: ImageBitmap,
    strokes: List<Stroke>,
    widthPx: Int,
    heightPx: Int,
): ImageBitmap? = runCatching {
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(layer),
        size = Size(widthPx.toFloat(), heightPx.toFloat()),
    ) {
        drawStrokes(strokes, widthPx.toFloat(), printBrush(print))
    }
    layer
}.getOrNull()

/**
 * The marks, drawn from scratch onto a fresh sheet of pixels.
 */
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
