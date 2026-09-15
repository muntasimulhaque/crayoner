package io.github.muntasimulhaque.crayoner.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import io.github.muntasimulhaque.crayoner.core.Stroke

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
