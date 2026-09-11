package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.crayoner.core.Page

/**
 * The page, described to a screen reader.
 *
 * A coloring page is one picture, not a list of controls, so a sighted child
 * simply draws on it. Without help, a screen reader would see one big canvas
 * and nothing to aim at. This overlay gives every area its own focusable
 * target, named for what it is and what color it wants, with one action that
 * colors the area with the crayon in hand.
 *
 * The nodes carry semantics only: no pointer input, no clickable, so a
 * normal finger passes straight through to the sheet underneath and drawing
 * is exactly as it was. The nodes are drawn invisible; a screen reader draws
 * its own focus rectangle around whichever one it is on.
 *
 * Nothing here says done, because the app does not know what done means. It
 * says what the area is and what color the book prints it in, which is
 * everything a child needs to color it.
 */
@Composable
fun PageSemantics(
    page: Page,
    crayon: Long?,
    onColor: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val sidePx = with(LocalDensity.current) { maxWidth.toPx() }
        if (sidePx <= 0f) return@BoxWithConstraints
        val scale = sidePx.toFloat()
        // Reverse order, so the last painted area, which is the one on top
        // and the one a finger would hit, is the first the reader meets.
        for (index in page.regions.indices.reversed()) {
            val region = page.regions[index]
            val label = areaLabel(
                kind = stringResource(areaNameRes(region.kind)),
                wanted = stringResource(crayonNameRes(region.fillArgb)).lowercase(),
                ready = crayon == region.fillArgb,
            )
            val b = region.bounds
            PageAreaTarget(
                label = label,
                onColor = { onColor(index) },
                x = (b.x * scale).toFloat(),
                y = (b.y * scale).toFloat(),
                size = (b.w.coerceAtLeast(0.05) * scale).toFloat(),
            )
        }
    }
}

/** Builds the one sentence a screen reader reads for an area. */
internal fun areaLabel(kind: String, wanted: String, ready: Boolean): String =
    if (ready) "$kind, needs $wanted, ready to color" else "$kind, needs $wanted"

/** One invisible focus target, sized to its own area on the page. */
@Composable
private fun PageAreaTarget(
    label: String,
    onColor: () -> Unit,
    x: Float,
    y: Float,
    size: Float,
) {
    val density = LocalDensity.current
    val xDp: Dp = with(density) { x.toDp() }
    val yDp: Dp = with(density) { y.toDp() }
    val sizeDp: Dp = with(density) { size.toDp() }
    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(sizeDp)
            .semantics {
                contentDescription = label
                onClick(label = label) {
                    onColor()
                    true
                }
            },
    )
}
