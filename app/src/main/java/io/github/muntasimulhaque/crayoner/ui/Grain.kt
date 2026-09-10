package io.github.muntasimulhaque.crayoner.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import io.github.muntasimulhaque.crayoner.core.WaxGrain

/**
 * The wax grain, ready to lay over a colored area.
 *
 * The tile is generated once by core/WaxGrain and wrapped in a shader that
 * repeats it forever, so a colored area of any shape and any size at all
 * gets the same even speckle at no per-area cost. This is what turns a flat
 * fill into a crayon: wax catches on the tooth of the paper, so coverage is
 * never perfectly even.
 *
 * Built lazily and never disposed: it lives as long as the process, like the
 * palette it belongs to.
 */
private val grainTile: ImageBitmap by lazy {
    val pixels = WaxGrain.pixels()
    val bitmap = Bitmap.createBitmap(pixels, WaxGrain.SIZE, WaxGrain.SIZE, Bitmap.Config.ARGB_8888)
    bitmap.asImageBitmap()
}

/** The repeating grain brush. Falls back to nothing if the tile failed. */
internal val GrainBrush: ShaderBrush? by lazy {
    runCatching {
        ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated))
    }.getOrNull()
}
