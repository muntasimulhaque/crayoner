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
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.PageView
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen

/**
 * The coloring screen: the sheet, and the things a hand reaches for.
 *
 * The sheet takes the room it can: on a phone the cylinder of tools sits
 * under it, on anything wide and sideways it stands beside it. There is no
 * picture above the page, because the picture lives in the bar as one more
 * round button the same size and shape as the rest: look at it any time, and
 * one tap holds it up big.
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
    onUndo: () -> Unit,
    onZoom: (Double) -> Unit,
    onHome: () -> Unit,
    onSound: (Boolean) -> Unit,
    onPeek: (Boolean) -> Unit,
    onColorArea: (Int) -> Unit = {},
) {
    val haptics = rememberHaptics()
    // One answer per finished mark, and only one: the small touch of wax that
    // says the hand did something. It is never a judgment of what the mark
    // did, because the app has no opinion about where the color went. A step
    // back is the same answer, because a hand that just took a mark off the
    // paper also did something.
    val answers = state.marks
    LaunchedEffect(answers) {
        if (answers != 0L) haptics.paint()
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
                    onSample = { onPeek(true) },
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
            val tools: @Composable (Modifier) -> Unit = { modifier ->
                ToolBox(
                    state = state,
                    onOpenBox = onOpenBox,
                    onPick = onPick,
                    onErase = onErase,
                    onUndo = onUndo,
                    onZoom = onZoom,
                    modifier = modifier,
                )
            }
            if (wide) {
                Column(Modifier.fillMaxSize()) {
                    bar()
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        sheet(Modifier.weight(1f).fillMaxSize())
                        Box(
                            modifier = Modifier.width(TOOLS_WIDTH).fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            tools(Modifier.fillMaxWidth().padding(end = 12.dp))
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
                        modifier = Modifier.fillMaxWidth().height(TOOLS_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        tools(Modifier.padding(horizontal = 16.dp))
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
 * sheet and the tools beside each other. Below it, or in portrait, stacking
 * is the only honest answer: a sheet squeezed into half a screen is smaller
 * than the hand coloring it.
 */
private val WIDE_AT = 640.dp

/** How tall the bar is, in both shapes. Home, the sample, the sound switch. */
private val BAR_HEIGHT = 66.dp

/** How much room the tools are given under the sheet on a phone. */
private val TOOLS_HEIGHT = 190.dp

/** The tool column's width beside the sheet. */
private val TOOLS_WIDTH = 300.dp

/**
 * The things a hand reaches for while coloring, in one column under (or
 * beside) the sheet: the capsule with the crayon, the rubber and the step
 * back in it, and under that the little chip that brings the paper closer.
 * Both are quiet objects on the desk; the sheet is what the child is here
 * for.
 */
@Composable
private fun ToolBox(
    state: Screen.Coloring,
    onOpenBox: (Boolean) -> Unit,
    onPick: (Long) -> Unit,
    onErase: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onZoom: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolCapsule(
            selected = state.crayon ?: Crayons.RED,
            erasing = state.erasing,
            boxOpen = state.boxOpen,
            canUndo = state.canUndo,
            onOpenBox = onOpenBox,
            onErase = onErase,
            onUndo = onUndo,
            modifier = Modifier.fillMaxWidth(),
        )
        ZoomStrip(
            view = state.view,
            onZoom = onZoom,
            onReset = { onZoom(PageView.WHOLE_ZOOM) },
        )
    }
}

/**
 * Home on the left, then the sample and the sound switch. One size, one
 * shape and one shadow, so a small hand learns the row once.
 */
@Composable
private fun TopBar(
    onHome: () -> Unit,
    soundOn: Boolean,
    onSound: (Boolean) -> Unit,
    onSample: () -> Unit,
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
        // The sample stands at the right, one tap from the page: look at the
        // picture, look at the sheet, color.
        SampleButton(onClick = onSample, page = page, announce = announce)
        CircleButton(
            onClick = { onSound(!soundOn) },
            background = CrayonerColors.Card,
            label = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off),
        ) {
            SoundIcon(on = soundOn, color = CrayonerColors.Ink, size = 26.dp)
        }
    }
}
