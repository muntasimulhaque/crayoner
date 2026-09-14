package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.ui.IconSize

/**
 * One capsule on the desk, holding the four things a hand reaches for while
 * coloring: the step back, the crayon it is drawing with, the step forward,
 * and the rubber.
 *
 * They are one object on purpose. Four separate coins put four separate
 * shadows on the desk and ask a three year old to find the small gap between
 * them; a single capsule says these belong together and reads as one thing to
 * put a finger on. It also leaves the desk itself clear: the paper is what
 * the child is here for, and the tools are one object lying below it rather
 * than a row of them.
 *
 * The order runs the way the hand works and the way the two steps belong
 * together. The step back and the step forward sit at the left as one pair,
 * out of the way, because they are the two things on the capsule that move
 * the paper rather than draw on it and a child should have to mean them; and
 * because they are a pair, they are next to each other, so a child who steps
 * back over a mark they did not mean can step forward again without hunting
 * for the other end of the row. The crayon sits next, in the working part of
 * the row where a thumb naturally lands, because it is the thing they pick up
 * a hundred times, and the rubber sits last, at the right end: draw with the
 * crayon, change the whole picture with the rubber.
 *
 * Every seat is [CoinSize] and every mark in it is [IconSize], the bar's own
 * two numbers. A row of round controls is read as one object, so the capsule
 * and the bar above it are the same coins with the same marks in them, and
 * the capsule is exactly as wide as its four coins need ([RAIL_WIDTH], which
 * is measured from the coin and not chosen): the two rows line up at both
 * ends, and the capsule is the size of the things it holds rather than a
 * pill stretched to fill the desk. A capsule with its own larger marks and
 * its own larger seats was tried, and it read as a second, louder object
 * lying under the picture.
 *
 * The crayon is drawn the way a real crayon is held (point down) and the
 * rubber the way a real rubber sits on a desk.
 */
@Composable
fun ToolCapsule(
    selected: Long,
    erasing: Boolean,
    boxOpen: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onOpenBox: (Boolean) -> Unit,
    onErase: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The capsule is as wide as its own four coins need, and no wider:
        // the bar and the capsule share [RAIL_WIDTH], so adding the step
        // forward made the whole rail a coin wider rather than squeezing the
        // seats. On a desk too narrow for that (a small phone, or the tool
        // column beside the sheet on a short landscape screen) the pad and
        // the air between the seats give way first, and the coins themselves
        // stop shrinking at [MIN_REACH], the smallest target a fingertip is
        // owed: a narrow screen gets smaller gaps, never smaller buttons.
        val rail = minOf(maxWidth, RAIL_WIDTH)
        val slack = (rail - MIN_REACH * SEATS).coerceAtLeast(0.dp)
        val pad = minOf(RAIL_PAD, slack / 2)
        val gap = minOf(SEAT_GAP, (slack - pad * 2) / (SEATS - 1))
        val reach = minOf(CoinSize, (rail - pad * 2 - gap * (SEATS - 1)) / SEATS)
        Row(
            modifier = Modifier
                .width(rail)
                .buttonShadow(RoundedCornerShape(reach * 0.62f), elevation = 5.dp)
                .clip(RoundedCornerShape(reach * 0.62f))
                .background(CrayonerColors.Card)
                .padding(horizontal = pad, vertical = reach * 0.07f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepSeat(
                ready = canUndo,
                forward = false,
                onClick = onUndo,
                size = reach,
            )
            StepSeat(
                ready = canRedo,
                forward = true,
                onClick = onRedo,
                size = reach,
            )
            CapsuleSeat(
                onClick = { onOpenBox(!boxOpen) },
                armed = boxOpen,
                size = reach,
                label = stringResource(crayonNameRes(selected)) +
                    ", " + stringResource(R.string.pick_color),
            ) {
                // The color in hand, drawn the way the app's own mark is
                // drawn: the same crayon, at the same lean, in the shape's
                // own proportions, so the crayon on the capsule is the same
                // object as the crayon beside the app's name and the one on
                // the launcher. Its box is [IconSize], the same box the
                // rubber, the two steps and the bar's own marks are drawn
                // in, so the shaped mark is fitted to the size of its
                // neighbors and cannot come out taller than they are.
                CrayonGlyph(
                    color = Color(selected),
                    leanDeg = CrayonShape.MARK_LEAN,
                    modifier = Modifier.size(IconSize),
                )
            }
            RubberSeat(
                armed = erasing,
                onClick = { onErase(!erasing) },
                size = reach,
            )
        }
    }
}

/** How many seats the capsule holds. See [RAIL_WIDTH]. */
private const val SEATS = 4

/**
 * The air between two seats, at the coin's own scale: enough that two
 * targets are two targets, and no more, because every dp between the seats
 * is a dp the capsule takes off the desk.
 */
private val SEAT_GAP = 12.dp

/**
 * The smallest a seat may ever come out, however narrow the screen. It is the
 * accessibility floor: below the 48 dp a fingertip is owed, a target stops
 * being a target and the child is aiming rather than pressing.
 */
private val MIN_REACH = 48.dp

/**
 * How far the outer seats sit from the capsule's own edge. The capsule and
 * the top bar share it, so the first seat and the home button share a left
 * edge and the last seat and the sound switch share a right one.
 */
private val RAIL_PAD = 14.dp

/**
 * The width of the rail the bar and the capsule both stand on, measured from
 * the coins it holds rather than chosen: [SEATS] coins, the air between them
 * and the two pads. It is one number for both rows, so the seats of the
 * capsule and the controls of the bar line up at both ends, and neither row
 * can be re-sized without the other following it.
 */
internal val RAIL_WIDTH =
    RAIL_PAD * 2 + CoinSize * SEATS + SEAT_GAP * (SEATS - 1)

/**
 * The rubber's seat. Press it and the crayon becomes an eraser: the next
 * mark rubs wax off the paper. Press it again and the crayon comes back.
 *
 * The rubber itself looks the same whether it is picked up or lying down:
 * the seat's own plate is the whole selection mark (see [CapsuleSeat]).
 */
@Composable
private fun RubberSeat(armed: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(if (armed) R.string.eraser_on else R.string.eraser_off)
    CapsuleSeat(onClick = onClick, armed = armed, size = size, label = label) {
        EraserGlyph(modifier = Modifier.size(IconSize))
    }
}

/**
 * The two steps: back takes the last mark off the paper, forward puts the
 * mark the step back took off back on it. Pressed again, either one keeps
 * walking, one finished mark a press, so a child who stepped back over three
 * marks they did not mean can walk the whole way forward again.
 *
 * They are drawn quieter until there is a step to take, so a child who
 * presses one on a sheet with nothing on that side can see that the app heard
 * the press and that there was simply nothing to move. Nothing is confirmed,
 * nothing is asked twice, and no press can lose a picture: the marks walk
 * back and forward in the order the hand made them, and the rubber is what
 * changes a whole picture.
 */
@Composable
private fun StepSeat(ready: Boolean, forward: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(if (forward) R.string.redo else R.string.undo)
    val ink = animateColorAsState(
        targetValue = if (ready) CrayonerColors.Ink else CrayonerColors.Ink.copy(alpha = 0.32f),
        label = if (forward) "redo-ink" else "undo-ink",
    ).value
    CapsuleSeat(onClick = onClick, armed = false, size = size, label = label) {
        if (forward) {
            RedoGlyph(color = ink, size = IconSize)
        } else {
            UndoGlyph(color = ink, size = IconSize)
        }
    }
}

/**
 * One seat in the capsule: the round plate a child presses, lifted a little
 * on its spring when the thing it holds is in hand.
 *
 * [armed] is the one selection language in the app, and it is the same one
 * on every seat: the plate under the thing in hand takes the capsule's own
 * cardboard, so which tool is picked up reads at a glance and no seat means
 * anything different from any other. The plate is not the button, so it is
 * drawn behind the glyph and never as a ring around it.
 */
@Composable
private fun CapsuleSeat(
    onClick: () -> Unit,
    armed: Boolean,
    size: Dp,
    label: String,
    content: @Composable () -> Unit,
) {
    val lift = animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "seat-lift",
    ).value
    Box(
        modifier = Modifier
            .size(size)
            .offset(y = -(size * 0.045f) * lift)
            .clip(CircleShape)
            .background(if (armed) CrayonerColors.Cardboard else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                selected = armed
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
