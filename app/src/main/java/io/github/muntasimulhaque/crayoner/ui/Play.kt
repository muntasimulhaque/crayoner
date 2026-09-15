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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The coloring screen: the sheet, and the things a hand reaches for.
 *
 * On a phone the bar sits above the paper and the capsule of tools below it,
 * the two rows on one rail so the screen reads as one object. On anything
 * wide and sideways the capsule stands on the left and the sheet takes the
 * rest: a column of tools costs the paper its own width and the eye nothing,
 * because the hand coloring the page reaches past them once, and the picture
 * it is coloring is what every pixel of the screen is for.
 *
 * There is no picture above the page, because the picture lives in the bar as
 * one more round button the same size and shape as the rest: look at it any
 * time, and one tap holds it up big.
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
    /** The mark under the finger, asked for by the draw that paints it. */
    live: State<Stroke?>? = null,
    onStrokeStart: (Vec2) -> Unit,
    onStrokeMove: (Vec2) -> Unit,
    onStrokeEnd: () -> Unit,
    onPick: (Long) -> Unit,
    onErase: (Boolean) -> Unit,
    onOpenBox: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHome: () -> Unit,
    onSound: (Boolean) -> Unit,
    onPeek: (Boolean) -> Unit,
    onColorArea: (Int) -> Unit = {},
    onKeep: () -> Unit = {},
    onStartFresh: () -> Unit = {},
    onDismissAsk: () -> Unit = {},
) {
    val haptics = rememberHaptics()
    // One answer per finished mark, and only one: the small touch of wax that
    // says the hand did something. It is never a judgment of what the mark
    // did, because the app has no opinion about where the color went. A step
    // back is answered the same way, because a hand that just took a mark off
    // the paper also did something.
    val answers = state.marks
    LaunchedEffect(answers) {
        if (answers != 0L) haptics.paint()
    }

    Box(modifier = Modifier.fillMaxSize().background(CrayonerColors.Desk)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The sample held up close is the picture the child copies, and it
            // is one tap away at all times. Its own picture is rendered the
            // moment the page opens, off the main thread, at exactly the size
            // the peek will ask for, so the tap finds a picture instead of
            // making one: a picture of a whole page is real work, and the
            // main thread is where a tap is felt.
            val density = LocalDensity.current
            val page = state.page
            LaunchedEffect(page.id) {
                val widthPx = with(density) { peekSide(maxWidth, maxHeight).roundToPx() }
                withContext(Dispatchers.Default) {
                    prewarmPageImages(listOf(page), ::sampleFills, widthPx)
                }
            }
            // The tools stand beside the sheet only when there is genuinely
            // room for both, which needs width AND a screen that is wider
            // than it is tall. A portrait tablet is wide, but standing the
            // tools beside the sheet there steals half the page's size; the
            // same tablet gets the stacked shape and a much bigger sheet.
            // This is decided from the real measured size, so it is right on
            // every device rather than on the ones we happened to test.
            val wide = maxWidth >= WIDE_AT && maxWidth > maxHeight * 1.15f
            val bar: @Composable (Modifier) -> Unit = { m ->
                TopBar(
                    onHome = onHome,
                    soundOn = soundOn,
                    onSound = onSound,
                    onSample = { onPeek(true) },
                    page = state.page,
                    announce = state.progress.strokes.isEmpty(),
                    modifier = m,
                )
            }
            val sheet: @Composable (Modifier) -> Unit = { modifier ->
                Sheet(
                    state = state,
                    live = live,
                    onStrokeStart = onStrokeStart,
                    onStrokeMove = onStrokeMove,
                    onStrokeEnd = onStrokeEnd,
                    onColorArea = onColorArea,
                    modifier = modifier,
                )
            }
            val tools: @Composable (Modifier) -> Unit = { modifier ->
                ToolCapsule(
                    selected = state.crayon ?: Crayons.RED,
                    erasing = state.erasing,
                    boxOpen = state.boxOpen,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onOpenBox = onOpenBox,
                    onErase = onErase,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    modifier = modifier,
                    vertical = wide,
                )
            }
            if (wide) {
                // Sideways: the tools stand in a column at the left edge of
                // the desk and the paper takes everything else. A column of
                // controls beside a sheet costs the sheet a column's width
                // and not a row's, and the paper keeps the whole height of
                // the screen above its tools. The bar still runs across the
                // top of everything, because the way home should not move
                // when the phone turns over.
                Column(Modifier.fillMaxSize()) {
                    bar(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        Box(
                            modifier = Modifier.width(TOOLS_COLUMN).fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            tools(Modifier)
                        }
                        sheet(Modifier.weight(1f).fillMaxSize())
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // The bar runs the whole width of the screen on a phone:
                    // Home lands in the top left corner and the sound switch
                    // in the top right, where a hand already knows to look,
                    // and the picture sits between them. Nothing is inset
                    // from the screen's own edge, because a bar inset from
                    // the edge reads as a panel dropped onto the desk.
                    bar(Modifier.fillMaxWidth())
                    // The sheet is given every pixel the capsule does not
                    // need. It keeps the sheet's own proportion, so a phone
                    // shows a tall page rather than a square with two bands
                    // of empty desk above and below it.
                    sheet(Modifier.fillMaxWidth().weight(1f))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(TOOLS_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        tools(Modifier)
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
                // Picking a color takes the crayon and closes the lid in one
                // act, so the box is never left open over a hand that is
                // already drawing.
                onPick = onPick,
                onDismiss = { onOpenBox(false) },
            )
        }
        // The one question the app ever asks. It comes up only when a child
        // with work on the page presses Home, it is two big answers and no
        // words to read, and either answer is safe: keeping the picture costs
        // nothing, and starting fresh only means the next visit opens on a
        // clean sheet. The app never asks this on a bare page, because there
        // is nothing there to keep.
        if (state.asking) {
            SaveQuestion(
                onKeep = onKeep,
                onStartFresh = onStartFresh,
                onDismiss = onDismissAsk,
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

/**
 * How much room the tool column takes beside the sheet. It is the capsule's
 * own thickness and its shadow, so the column is the object on it rather than
 * a panel around it.
 */
private val TOOLS_COLUMN = 74.dp

/** How tall the bar is, in both shapes. Home, the sample, the sound switch. */
private val BAR_HEIGHT = 66.dp

/**
 * How much room the capsule is given under the sheet on a phone. It is the
 * capsule's own height and a little air, so the tools sit on the desk rather
 * than pressing against the paper or the edge of the screen.
 */
private val TOOLS_HEIGHT = 84.dp

/**
 * Home at the left edge, the sample and the sound switch at the right.
 *
 * The bar is the full width of the screen and its two ends sit on the
 * screen's own corners: Home is the first thing a hand finds without looking,
 * and a sound switch hidden in from the edge is a switch a parent has to hunt
 * for. The picture stands between them, one step in from the sound switch so
 * the two round buttons on the right are two targets and not one.
 */
@Composable
private fun TopBar(
    onHome: () -> Unit,
    soundOn: Boolean,
    onSound: (Boolean) -> Unit,
    onSample: () -> Unit,
    page: Page,
    announce: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(BAR_HEIGHT)
            .padding(horizontal = BAR_PAD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
    ) {
        CircleButton(
            onClick = onHome,
            background = CrayonerColors.Card,
            label = stringResource(R.string.home),
        ) {
            HomeIcon(color = CrayonerColors.Ink)
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
            SoundIcon(on = soundOn, color = CrayonerColors.Ink)
        }
    }
}

/**
 * How far the bar's own buttons sit from the edge of the screen, and the air
 * between the two at the right. The pad is the screen's own margin, not the
 * rail's: the bar is the one row that runs edge to edge, and its ends are the
 * corners a hand aims at.
 */
private val BAR_PAD = 10.dp
private val BAR_GAP = 12.dp
