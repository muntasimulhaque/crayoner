package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.graphics.ImageBitmap
import io.github.muntasimulhaque.crayoner.core.Page
import kotlinx.coroutines.sync.Mutex

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
internal data class Key(val pageId: String, val sample: Boolean, val widthPx: Int, val heightPx: Int)

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
internal class Cache(private val budget: Long) {
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
internal val shelfImages = Cache(32L * 1024 * 1024)

/** Everything else: the sheet, its marks, the sample held up, the bar. */
internal val pageImages = Cache(20L * 1024 * 1024)

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
internal val renderGate = Mutex()

/** The height an image of a given width has, rounded up: page units again. */
internal fun heightFor(widthPx: Int): Int =
    Math.ceil(widthPx * Page.ASPECT).toInt().coerceAtLeast(1)
