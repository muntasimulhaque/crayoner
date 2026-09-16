package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * One size for a line of words that has to fit the room it is given, whatever
 * size the reader has asked the system to draw text at.
 *
 * The app is a fixed play surface: a wall of cards, a bar of coins, a sheet
 * of paper. Text that grew without limit would push those surfaces apart and
 * spill over them, which serves nobody. So a line of words is measured at the
 * reader's own text size and, when it does not fit its room, set smaller by
 * exactly the ratio it needs. What the reader gets is the biggest words the
 * surface can hold, at every system text size, rather than a cap that stops
 * their setting from mattering at all.
 *
 * The measurement is in pixels through the composition's own density, which
 * already carries the system's text scale, so the ratio is right at any
 * scale. Nothing here needs a floor: a word that fits at a legible size in
 * its room is not made illegible by fitting, and the alternative to fitting
 * is words running off the edge of the screen.
 */
@Composable
internal fun rememberFittedFontSize(
    texts: List<String>,
    style: TextStyle,
    width: Dp,
): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(texts, width, style, density) {
        val available = with(density) { width.toPx() } * 0.98f
        val widest = texts.maxOfOrNull { text ->
            measurer.measure(
                text = text,
                style = style,
                maxLines = 1,
                softWrap = false,
            ).size.width.toFloat()
        } ?: 0f
        if (widest <= available || widest == 0f || available <= 0f) {
            style.fontSize
        } else {
            (style.fontSize.value * (available / widest)).sp
        }
    }
}
