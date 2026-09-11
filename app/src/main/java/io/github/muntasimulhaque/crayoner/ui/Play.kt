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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen

/**
 * The coloring screen: the sheet, and the two things a child holds.
 *
 * The sheet takes the room it can: on a phone the tray sits under it, on
 * anything wide and sideways it stands beside it. There is no picture above
 * the page, because the picture lives in the bar as one more round button
 * the same size and shape as the rest: look at it any time, and one tap
 * holds it up big.
 *
 * Nothing here counts anything. There is no line of progress to watch and no
 * score to keep: the child colors, and the only thing that ever changes on
 * the paper is what their own hand put there.
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
    onErase: (Boolean) -> Unit,
    onOpenBox: (Boolean) -> Unit,
    onSeal: () -> Unit,
    onHome: () -> Unit,
    onSound: (Boolean) -> Unit,
    onPeek: (Boolean) -> Unit,
    onColorArea: (Int) -> Unit = {},
) {
    val haptics = rememberHaptics()
    // One answer per finished mark, and only one: the small touch of wax that
    // says the hand did something. It is never a judgment of what the mark
    // did, because the app has no opinion about where the color went.
    LaunchedEffect(state.stamp) {
        if (state.stamp != 0L) haptics.paint()
    }
    LaunchedEffect(state.sealStamp) {
        if (state.sealStamp != 0L) haptics.done()
    }

    Box(modifier = Modifier.fillMaxSize().background(CrayonerColors.Desk)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The tray stands beside the sheet only when there is genuinely
            // room for both, which needs width AND a screen that is wider
            // than it is tall. A portrait tablet is wide, but standing the
            // tray beside the sheet there steals half the page's size; the
            // same tablet gets the stacked shape and a much bigger sheet.
            // This is decided from the real measured size, so it is right on
            // every device rather than on the ones we happened to test.
            val wide = maxWidth >= WIDE_AT && maxWidth > maxHeight * 1.15f
            val bar: @Composable () -> Unit = {
                TopBar(
                    onHome = onHome,
                    soundOn = soundOn,
                    onSound = onSound,
                    onSample = { onPeek(true) },
                    onSeal = onSeal,
                    sealed = state.sealed,
                    page = state.page,
                    announce = state.progress.strokes.isEmpty(),
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
            val tray: @Composable (Modifier) -> Unit = { modifier ->
                CrayonTray(
                    selected = state.crayon ?: Crayons.RED,
                    erasing = state.erasing,
                    boxOpen = state.boxOpen,
                    onOpenBox = onOpenBox,
                    onPick = onPick,
                    onErase = onErase,
                    modifier = modifier,
                )
            }
            if (wide) {
                Column(Modifier.fillMaxSize()) {
                    bar()
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        sheet(Modifier.weight(1f).fillMaxSize().padding(4.dp))
                        Box(
                            modifier = Modifier.width(TOOLS_WIDTH).fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            tray(Modifier.fillMaxWidth().padding(end = 12.dp))
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
                    Box(
                        modifier = Modifier.fillMaxWidth().height(TRAY_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        tray(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        if (state.peeking) {
            SamplePeek(page = state.page, onDismiss = { onPeek(false) })
        }
        if (state.boxOpen) {
            CrayonBoxSheet(
                selected = state.crayon ?: Crayons.RED,
                onPick = { argb ->
                    onPick(argb)
                    onOpenBox(false)
                },
                onDismiss = { onOpenBox(false) },
            )
        }
    }
}

/**
 * The width, and the landscape-ness, at which there is room to stand the
 * sheet and the tray beside each other. Below it, or in portrait, stacking
 * is the only honest answer: a sheet squeezed into half a screen is smaller
 * than the hand coloring it.
 */
private val WIDE_AT = 640.dp

/** How tall the bar is, in both shapes. Home, the seal, the sample, sound. */
private val BAR_HEIGHT = 66.dp

/** How much room the two coins are given under the sheet on a phone. */
private val TRAY_HEIGHT = 126.dp

/** The tool column's width beside the sheet. */
private val TOOLS_WIDTH = 300.dp

/**
 * Home on the left, the seal, then the sample and the sound switch. One
 * size, one shape and one shadow, so a small hand learns the row once.
 *
 * The seal is here rather than in the tray because it is not a tool: the
 * child picks up a crayon and a rubber to work with, and stamps the page when
 * the work is done. It is the one button that says I am finished, and it is
 * the only thing in the app that ever says so.
 */
@Composable
private fun TopBar(
    onHome: () -> Unit,
    soundOn: Boolean,
    onSound: (Boolean) -> Unit,
    onSample: () -> Unit,
    onSeal: () -> Unit,
    sealed: Boolean,
    page: Page,
    announce: Boolean,
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
        Spacer(Modifier.weight(1f))
        // The sample stands beside the seal: the two things a child reaches
        // for while working, look at the picture and finish the picture.
        SampleButton(onClick = onSample, page = page, announce = announce)
        SealButton(sealed = sealed, onClick = onSeal)
        CircleButton(
            onClick = { onSound(!soundOn) },
            background = CrayonerColors.Card,
            label = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off),
        ) {
            SoundIcon(on = soundOn, color = CrayonerColors.Ink, size = 26.dp)
        }
    }
}

/** The stamp: press it when the picture is done, press it again to take it off. */
@Composable
private fun SealButton(sealed: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (sealed) R.string.unseal else R.string.seal)
    CircleButton(
        onClick = onClick,
        background = CrayonerColors.Card,
        label = label,
    ) {
        if (sealed) {
            // Already stamped: the button wears the stamp itself, so the
            // child can see from the bar that this picture has been sealed.
            WaxSeal(modifier = Modifier.size(SealSize))
        } else {
            SealIcon(color = CrayonerColors.Ink, size = 26.dp)
        }
    }
}
