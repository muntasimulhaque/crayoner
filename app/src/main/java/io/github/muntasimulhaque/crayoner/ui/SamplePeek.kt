package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Page

/**
 * The sample held up big: one tap anywhere puts it back down. The page
 * underneath is dimmed but never hidden, so the child keeps their place.
 *
 * The plate is a sheet of paper on the desk like the one it explains, with
 * the same soft shadow and the same name along its bottom. It carries no
 * tape, because the sheet the child is coloring does not either: what holds
 * a sheet on this desk is that it is lying on the desk, and tape would be
 * one more thing to look past.
 */
@Composable
fun SamplePeek(page: Page, onDismiss: () -> Unit) {
    val name = stringResource(pageNameRes(page.id))
    val hint = stringResource(R.string.hide_sample)
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CrayonerColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .semantics { contentDescription = hint },
        contentAlignment = Alignment.Center,
    ) {
        // The plate is sized so the picture AND its name both fit inside it,
        // with a clear margin on every side: the name belongs on the plate,
        // never floating over the dimmed screen behind it. The margin grows
        // with the screen, so the panel never feels cramped on a tablet or
        // fills the screen on a phone.
        val side = peekSide(maxWidth, maxHeight)
        val widthPx = with(density) { side.roundToPx() }
        // The picture's own height, from the one function that gives every
        // page image its height, so the box the sheet is looked at in and
        // the picture rendered for it are the same rectangle to the pixel:
        // a prewarm that cannot find the size the peek asks for is work
        // done for nobody.
        val heightPx = heightFor(widthPx)
        val sideDp = with(density) { widthPx.toDp() }
        val sheetDp = with(density) { heightPx.toDp() }
        val totalW = sideDp + PEEK_PADDING * 2
        val totalH = sheetDp + PEEK_PADDING * 2 + NAME_BLOCK
        Box(
            modifier = Modifier.size(width = totalW, height = totalH),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = totalW, height = totalH)
                    .buttonShadow(PaperShape, elevation = 12.dp)
                    .clip(PaperShape)
                    .background(CrayonerColors.Card)
                    .padding(PEEK_PADDING),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier.size(width = sideDp, height = sheetDp)) {
                        PageCanvas(
                            page = page,
                            fills = remember(page) { sampleFills(page) },
                            widthPx = widthPx,
                            heightPx = heightPx,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier.height(NAME_BLOCK),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = CrayonerColors.Ink,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * How wide the peek's own sheet is drawn, from the room the whole screen
 * holds: the picture's column, inside the plate's padding, inside the plate's
 * margin from the desk's edges.
 *
 * It is a function rather than a line inside the peek because one other
 * thing needs the same answer: the picture for the sheet held up close is
 * rendered, off the main thread, the moment a page opens, and a prewarm that
 * measured the plate any other way than the plate measures itself would
 * leave the tap to make a picture of its own. See `PlayScreen.kt`.
 */
internal fun peekSide(maxWidth: Dp, maxHeight: Dp): Dp {
    if (maxWidth <= 0.dp || maxHeight <= 0.dp) return PEEK_MIN
    val margin = minOf(maxWidth, maxHeight) * PEEK_MARGIN
    val plateMax = minOf(maxWidth - margin * 2f, maxHeight - margin * 2f)
    val innerW = (plateMax - PEEK_PADDING * 2).coerceAtLeast(PEEK_MIN)
    val innerH = (plateMax - PEEK_PADDING * 2 - NAME_BLOCK).coerceAtLeast(PEEK_MIN)
    return minOf(innerW, innerH / Page.ASPECT.toFloat()).coerceAtLeast(PEEK_MIN)
}

/** The plate's own margin from the desk's edges, as a share of the screen. */
private const val PEEK_MARGIN = 0.09f

/** How much paper the peek's plate keeps around its picture. */
private val PEEK_PADDING = 16.dp

/** The room under the picture the peek keeps for the picture's name. */
private val NAME_BLOCK = 50.dp

/** The smallest the peek's sheet is ever drawn, so a tiny screen still has one. */
private val PEEK_MIN = 96.dp
