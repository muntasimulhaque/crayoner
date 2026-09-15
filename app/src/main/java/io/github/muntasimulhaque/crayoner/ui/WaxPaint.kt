package io.github.muntasimulhaque.crayoner.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import io.github.muntasimulhaque.crayoner.core.Region
import io.github.muntasimulhaque.crayoner.core.Wax

/**
 * The wax in one area: the wax surface itself, then the passes of the hand
 * over it. Everything is clipped to the area's own outline, so a pass that
 * runs long never colors the paper beside the area.
 *
 * The surface is a shader built from the area's own wax tile, which carries
 * the crayon's color at the alpha the wax really laid down: the paper's
 * tooth breaking through, and the streak of the hand along the direction it
 * moved. The passes on top are the back and forth itself.
 */
internal fun DrawScope.drawWaxFill(region: Region?, outline: Path, color: Color) {
    if (region == null) return
    val side = size.width
    val surface = waxBrush(region, color, side)
    clipPath(outline) {
        if (surface != null) {
            drawRect(brush = surface)
        } else {
            drawRect(color)
        }
        for (pass in Wax.passes(region)) {
            drawPath(
                pathOfPoints(pass.points, side),
                color.copy(alpha = pass.alpha.toFloat()),
                style = tipStroke((pass.width * side).toFloat()),
            )
        }
        // The tooth once more over the passes, so the streaks are not the
        // only thing between the color and the paper.
        val grain = GrainBrush
        if (grain != null) drawRect(brush = grain)
    }
}

/**
 * The wax tiles, one per area, color and page size, built once and kept.
 *
 * A tile is a small square of pixels and there are a few dozen of them at
 * most, but they are held in a bounded map all the same: a picture drawn at
 * a size never seen before asks for a new tile, and a cache that only ever
 * grows is a leak with extra steps.
 */
private object WaxTileCache {
    private const val LIMIT = 192
    private val tiles = object : LinkedHashMap<String, ImageBitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
            size > LIMIT
    }

    @Synchronized
    operator fun get(key: String): ImageBitmap? = tiles[key]

    @Synchronized
    fun put(key: String, tile: ImageBitmap) {
        tiles[key] = tile
    }
}

/**
 * The area's own wax as a repeating brush. Keyed by page, area and color, so
 * a picture drawn at two sizes shares one surface, and a picture never
 * rebuilds its wax on a redraw.
 */
private fun waxBrush(region: Region, color: Color, side: Float): ShaderBrush? {
    val key = "${region.id}|${argbOf(color)}|${side.toInt()}"
    val tile = WaxTileCache[key] ?: runCatching {
        val pixels = Wax.surface(
            argb = argbOf(color),
            size = WAX_TILE,
            angleDeg = Wax.angleDeg(region),
            phase = 0,
            seed = Wax.seed(region),
        )
        val bitmap = Bitmap.createBitmap(pixels, WAX_TILE, WAX_TILE, Bitmap.Config.ARGB_8888)
        bitmap.asImageBitmap().also { WaxTileCache.put(key, it) }
    }.getOrNull() ?: return null
    return runCatching {
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }.getOrNull()
}

/**
 * The wax of the child's own marks: the same surface an area is colored
 * with, keyed by color alone, because a mark belongs to the hand and not to
 * any area of the picture. A mark the child makes is then literally made of
 * the material the sample is made of.
 *
 * One tile per color, and one pass of it under a mark. The tile is built
 * fine, which is the paper's own tooth at the scale of a line rather than of
 * a whole area.
 */
private val strokeWax = HashMap<Long, ImageBitmap>()

/**
 * The brushes themselves, one per color, kept for the life of the app.
 *
 * A brush is not the tile: building one wraps the tile in a native shader,
 * and a live mark is drawn sixty times a second while a finger moves. Doing
 * that work every frame was a cost the drawing hand paid for nothing, and it
 * showed up as the wax trailing the fingertip on a slow device.
 */
private val strokeBrushes = HashMap<Long, ShaderBrush>()

internal fun waxStroke(argb: Long): ShaderBrush? {
    strokeBrushes[argb]?.let { return it }
    val tile = strokeWax.getOrPut(argb) {
        val pixels = Wax.surface(
            argb = argb,
            size = WAX_TILE,
            angleDeg = MARK_ANGLE,
            phase = 0,
            seed = MARK_SEED,
            fine = true,
        )
        val bitmap = Bitmap.createBitmap(pixels, WAX_TILE, WAX_TILE, Bitmap.Config.ARGB_8888)
        bitmap.asImageBitmap()
    }
    return runCatching {
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }.getOrNull()?.also { strokeBrushes[argb] = it }
}

/** The angle a mark's wax lies at, and its seed: the same for every mark. */
private const val MARK_ANGLE = -24.0
private const val MARK_SEED = 0x5A17

/** The wax tile's side, in pixels. Small, and it wraps, so it repeats. */
private const val WAX_TILE = 96

/** A Compose color as the ARGB long core speaks in. */
private fun argbOf(color: Color): Long =
    ((color.alpha * 255).toLong() shl 24) or
        ((color.red * 255).toLong() shl 16) or
        ((color.green * 255).toLong() shl 8) or
        (color.blue * 255).toLong()
