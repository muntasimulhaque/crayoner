package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen

/**
 * The coloring screen: the picture to copy, the sheet, and the crayon box.
 * Nothing else, because nothing else is needed.
 *
 * The layout has exactly two shapes, chosen by how much room the screen
 * actually has rather than by whether it is a phone or a tablet:
 *
 * - **Stacked**, on anything narrower than [WIDE_AT]: the sample above the
 *   sheet, the box below it. A phone's width is the sheet's width.
 * - **Side by side**, on anything wider: the sheet takes the whole height it
 *   can on the left, and the sample and the box share a column on the right.
 *   A tablet turned sideways still shows the page at its biggest.
 *
 * Either way the sample sits directly against the sheet it belongs to, the
 * way a coloring book prints the finished picture facing the page: look up,
 * look down, color.
 *
 * Every color is saved a moment after it lands, so Home, a phone call or a
 * rotation cost at most the tap in flight.
 */
@Composable
fun PlayScreen(
    state: Screen.Coloring,
    soundOn: Boolean,
    onTap: (Vec2) -> Unit,
    onPick: (Long) -> Unit,
    onHome: () -> Unit,
    onSound: (Boolean) -> Unit,
    onPeek: (Boolean) -> Unit,
    onAskClear: (Boolean) -> Unit,
    onClear: () -> Unit,
    onColorArea: (Int) -> Unit = {},
) {
    val haptics = rememberHaptics()
    val paint = state.lastPaint
    val paintLandedRight = paint != null &&
        state.progress.colorOf(paint.index) == state.page.region(paint.index)?.fillArgb
    LaunchedEffect(paint?.stamp ?: 0L) {
        if (paint != null) {
            if (paintLandedRight) haptics.correct() else haptics.paint()
        }
    }
    LaunchedEffect(state.celebrating) {
        if (state.celebrating) haptics.done()
    }

    Box(modifier = Modifier.fillMaxSize().background(CrayonerColors.Desk)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The tools stand beside the sheet only when there is genuinely
            // room for both, which needs width AND a screen that is wider
            // than it is tall. A portrait tablet is wide, but standing the
            // tools beside the sheet there steals half the page's size; the
            // same tablet gets the stacked shape and a much bigger sheet.
            // This is decided from the real measured size, so it is right on
            // every device rather than on the ones we happened to test.
            val wide = maxWidth >= WIDE_AT && maxWidth > maxHeight * 1.15f
            val bar: @Composable () -> Unit = {
                TopBar(
                    onHome = onHome,
                    soundOn = soundOn,
                    onSound = onSound,
                    colored = state.progress.coloredCount,
                    total = state.page.regionCount,
                    accent = state.page.regions.first().fillArgb,
                )
            }
            val sample: @Composable (Dp) -> Unit = { side ->
                SampleCard(
                    page = state.page,
                    onClick = { onPeek(true) },
                    size = side,
                    announce = state.progress.coloredCount == 0,
                )
            }
            val box: @Composable (Modifier) -> Unit = { modifier ->
                CrayonBox(
                    selected = state.crayon,
                    onPick = onPick,
                    modifier = modifier,
                )
            }
            if (wide) {
                SideBySide(
                    state = state,
                    onTap = onTap,
                    onColorArea = onColorArea,
                    bar = bar,
                    sample = sample,
                    box = box,
                )
            } else {
                Stacked(
                    state = state,
                    onTap = onTap,
                    onColorArea = onColorArea,
                    bar = bar,
                    sample = sample,
                    box = box,
                )
            }
        }

        if (state.peeking) {
            SamplePeek(page = state.page, onDismiss = { onPeek(false) })
        }
        if (state.confirmingClear) {
            ClearConfirm(onStay = { onAskClear(false) }, onClear = onClear)
        }
        if (state.celebrating) {
            Celebration(
                page = state.page,
                fills = state.progress.asMap(),
                onAgain = onClear,
                onHome = onHome,
            )
        }
    }
}

/**
 * The width, and the landscape-ness, at which there is room to stand the
 * sheet and the tools beside each other. Below it, or in portrait, stacking
 * is the only honest answer: a sheet squeezed into half a screen is smaller
 * than the hand coloring it.
 */
private val WIDE_AT = 640.dp

/** How tall the bar is, in both shapes. Home, progress, sound. */
private val BAR_HEIGHT = 62.dp

/** How wide the tools column is beside the sheet, and its ceiling. */
private val TOOLS_WIDTH = 340.dp
private val TOOLS_MAX_WIDTH = 420.dp

/** The sample is the thing being copied, so it is never a thumbnail. */
private val SAMPLE_MIN = 112.dp
private val SAMPLE_MAX = 190.dp

/** The air between the sample and the sheet it belongs to. */
private val SHEET_GAP = 10.dp

/**
 * Stacked: the sample above the sheet, the box under it. The sheet takes the
 * whole width unless the screen is so short that the height is the binding
 * constraint, and whatever vertical slack is left over goes to the sample,
 * so the sight line from picture to page carries no dead space.
 */
@Composable
private fun Stacked(
    state: Screen.Coloring,
    onTap: (Vec2) -> Unit,
    onColorArea: (Int) -> Unit,
    bar: @Composable () -> Unit,
    sample: @Composable (Dp) -> Unit,
    box: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        bar()
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val sheetSide: Dp = minOf(maxWidth, maxHeight - SAMPLE_MIN - SHEET_GAP)
                .coerceAtLeast(140.dp)
            val slack = maxHeight - sheetSide - SHEET_GAP
            val sampleSide: Dp = slack.coerceIn(SAMPLE_MIN, SAMPLE_MAX)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                sample(sampleSide)
                Spacer(Modifier.height(SHEET_GAP))
                SheetOf(
                    state = state,
                    onTap = onTap,
                    onColorArea = onColorArea,
                    side = sheetSide,
                )
            }
        }
        box(Modifier.fillMaxWidth().padding(horizontal = 8.dp))
    }
}

/**
 * Side by side: the sheet takes the height it can on the left, and the
 * sample and box take a column on the right. The tools column is capped, so
 * on a wide tablet the sheet keeps the biggest share of the screen and the
 * box never stretches into a row of crayons lost in empty cardboard.
 */
@Composable
private fun SideBySide(
    state: Screen.Coloring,
    onTap: (Vec2) -> Unit,
    onColorArea: (Int) -> Unit,
    bar: @Composable () -> Unit,
    sample: @Composable (Dp) -> Unit,
    box: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        bar()
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val toolsWidth: Dp = minOf(maxWidth * 0.42f, TOOLS_MAX_WIDTH)
                .coerceAtLeast(TOOLS_WIDTH)
            Row(Modifier.fillMaxSize()) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val side: Dp = minOf(maxWidth, maxHeight)
                    SheetOf(
                        state = state,
                        onTap = onTap,
                        onColorArea = onColorArea,
                        side = side,
                    )
                }
                Column(
                    modifier = Modifier
                        .width(toolsWidth)
                        .fillMaxHeight()
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    sample(SAMPLE_MIN)
                    Spacer(Modifier.height(12.dp))
                    box(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Home on the left, how much is done, and the sound switch on the right. */
@Composable
private fun TopBar(
    onHome: () -> Unit,
    soundOn: Boolean,
    onSound: (Boolean) -> Unit,
    colored: Int,
    total: Int,
    accent: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleButton(
            onClick = onHome,
            background = CrayonerColors.Card,
            label = stringResource(R.string.home),
        ) {
            HomeIcon(color = CrayonerColors.Ink, size = 26.dp)
        }
        // How much of the picture is colored lives up here, out of the way:
        // a parent's glance, not a score for the child. It never makes a
        // sound, never counts aloud, and never changes color to say the
        // child is behind.
        ProgressLine(
            colored = colored,
            total = total,
            accent = accent,
            modifier = Modifier.width(140.dp).padding(horizontal = 14.dp),
        )
        Spacer(Modifier.weight(1f))
        CircleButton(
            onClick = { onSound(!soundOn) },
            background = CrayonerColors.Card,
            label = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off),
        ) {
            SoundIcon(on = soundOn, color = CrayonerColors.Ink, size = 26.dp)
        }
    }
}
