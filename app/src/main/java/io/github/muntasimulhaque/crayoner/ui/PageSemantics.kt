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
import io.github.muntasimulhaque.crayoner.core.PageView
import io.github.muntasimulhaque.crayoner.core.Vec2

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
 *
 * A target is the area's own piece of paper, placed through the same window
 * the picture is drawn through: when the paper has been brought closer, the
 * target a reader lands on is still the part of the picture a finger would
 * touch, and a part of the page that is off the window is off the overlay
 * too, because it is not on the paper the child is looking at.
 */
@Composable
fun PageSemantics(
    page: Page,
    crayon: Long?,
    onColor: (Int) -> Unit,
    modifier: Modifier = Modifier,
    view: PageView = PageView.Whole,
) {
    BoxWithConstraints(modifier) {
        val framePx = with(LocalDensity.current) { maxWidth.toPx() }
        if (framePx <= 0f) return@BoxWithConstraints
        // Reverse order, so the last painted area, which is the one on top
        // and the one a finger would hit, is the first the reader meets.
        for (index in page.regions.indices.reversed()) {
            val region = page.regions[index]
            val bounds = region.bounds
            val placed = view.inWindow(Vec2(bounds.x, bounds.y))
            val across = (bounds.w.coerceAtLeast(0.05) / view.span).coerceAtMost(1.0)
            // Anything at all off the paper's own edge is dropped: a target
            // over the desk would be a control for a part of the picture that
            // is not on screen.
            if (placed.x >= 1.0 || placed.y >= 1.0) continue
            if (placed.x + across <= 0.0 || placed.y + across <= 0.0) continue
            val label = areaLabel(
                kind = stringResource(areaNameRes(region.kind)),
                wanted = stringResource(crayonNameRes(region.fillArgb)).lowercase(),
                ready = crayon == region.fillArgb,
            )
            PageAreaTarget(
                label = label,
                onColor = { onColor(index) },
                x = (placed.x * framePx).toFloat(),
                y = (placed.y * framePx).toFloat(),
                size = (across * framePx).toFloat(),
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
