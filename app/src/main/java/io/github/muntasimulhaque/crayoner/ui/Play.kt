package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * The coloring screen: the sheet, and the crayons under it.
 *
 * The sheet takes the room it can: on a phone the box sits under it, on
 * anything wide and sideways it stands beside it. There is no picture above
 * the page any more, because the picture lives in the bar as one more round
 * button the same size and shape as the rest: look at it any time, and one
 * tap holds it up big.
 *
 * Every finished mark is saved a moment after it is made, so Home, a phone
 * call or a rotation cost at most the mark in flight.
 */
@Composable
fun PlayScreen(
    state: Screen.Coloring,
    soundOn: Boolean,
    onStrokeStart: (Vec2) -> Unit,
    onStrokeMove: (Vec2) -> Unit,
    onStrokeEnd: () -> Unit,
    onPick: (Long) -> Unit,
    onHome: () -> Unit,
    onSound: (Boolean) -> Unit,
    onPeek: (Boolean) -> Unit,
    onAskClear: (Boolean) -> Unit,
    onClear: () -> Unit,
    onColorArea: (Int) -> Unit = {},
) {
    val haptics = rememberHaptics()
    // One answer per finished mark: the warm tick where the picture was
    // asked for it, a plain touch of wax anywhere else. A mark over paper
    // that is already colored answers too, because the child's hand did
    // something, which is the whole point of a haptic.
    LaunchedEffect(state.stamp) {
        if (state.stamp != 0L) {
            if (state.rightIndices.isNotEmpty()) haptics.correct() else haptics.paint()
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
            val toolsWidth: Dp = minOf(maxWidth * 0.42f, TOOLS_MAX_WIDTH)
            val bar: @Composable () -> Unit = {
                TopBar(
                    onHome = onHome,
                    soundOn = soundOn,
                    onSound = onSound,
                    onSample = { onPeek(true) },
                    onErase = { onAskClear(true) },
                    canErase = state.progress.strokes.isNotEmpty(),
                    page = state,
                    colored = state.progress.reached(state.page).size,
                )
            }
            val sheet: @Composable (Modifier) -> Unit = { modifier ->
                Sheet(
                    state = state,
                    onStrokeStart = onStrokeStart,
                    onStrokeMove = onStrokeMove,
                    onStrokeEnd = onStrokeEnd,
                    onColorArea = onColorArea,
                    modifier = modifier,
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
                Column(Modifier.fillMaxSize()) {
                    bar()
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        sheet(Modifier.weight(1f).fillMaxSize().padding(4.dp))
                        Box(modifier = Modifier.width(toolsWidth).fillMaxSize(), contentAlignment = Alignment.Center) {
                            box(Modifier.fillMaxWidth().padding(end = 12.dp))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    bar()
                    sheet(Modifier.fillMaxWidth().weight(1f))
                    box(Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                }
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
                strokes = state.progress.strokes,
                onAgain = onClear,
                onHome = onHome,
            )
        }
    }
}

/**
 * The width, and the landscape-ness, at which there is room to stand the
 * sheet and the box beside each other. Below it, or in portrait, stacking is
 * the only honest answer: a sheet squeezed into half a screen is smaller
 * than the hand coloring it.
 */
private val WIDE_AT = 640.dp

/** How tall the bar is, in both shapes. Home, progress, sample, sound. */
private val BAR_HEIGHT = 66.dp

/** The tool column's ceiling beside the sheet. */
private val TOOLS_MAX_WIDTH = 420.dp

/**
 * Home on the left, how much of the page has been colored, then the three
 * round buttons: the sample to look at, the eraser when there is something
 * to erase, and the sound switch. They are one size, one shape and one
 * shadow, so a small hand learns the row once.
 */
@Composable
private fun TopBar(
    onHome: () -> Unit,
    soundOn: Boolean,
    onSound: (Boolean) -> Unit,
    onSample: () -> Unit,
    onErase: () -> Unit,
    canErase: Boolean,
    page: Screen.Coloring,
    colored: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircleButton(
            onClick = onHome,
            background = CrayonerColors.Card,
            label = stringResource(R.string.home),
        ) {
            HomeIcon(color = CrayonerColors.Ink, size = 26.dp)
        }
        // How much of the picture has felt the crayon lives up here, out of
        // the way: a parent's glance, not a score for the child. It never
        // makes a sound, never counts aloud, and never changes color to say
        // the child is behind.
        ProgressLine(
            colored = colored,
            total = page.page.regionCount,
            accent = page.page.regions.first().fillArgb,
            modifier = Modifier.width(120.dp).padding(horizontal = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        if (canErase) {
            CircleButton(
                onClick = onErase,
                background = CrayonerColors.Card,
                label = stringResource(R.string.erase),
            ) {
                EraseIcon(color = CrayonerColors.Ink, size = 26.dp)
            }
        }
        // The sample stands right beside the sound switch, the two buttons a
        // child reaches for while working: look, and hush.
        SampleButton(
            page = page.page,
            onClick = onSample,
            announce = page.progress.strokes.isEmpty(),
        )
        CircleButton(
            onClick = { onSound(!soundOn) },
            background = CrayonerColors.Card,
            label = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off),
        ) {
            SoundIcon(on = soundOn, color = CrayonerColors.Ink, size = 26.dp)
        }
    }
}
