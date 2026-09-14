package io.github.muntasimulhaque.crayoner.ui

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonShape

/**
 * One capsule on the desk, holding the three things a hand reaches for while
 * coloring: the step back, the crayon it is drawing with, and the rubber.
 *
 * They are one object on purpose. Three separate coins put three separate
 * shadows on the desk and ask a three year old to find the small gap between
 * them; a single capsule says these three belong together and reads as one
 * thing to put a finger on. It also leaves the desk itself clear: the paper
 * is what the child is here for, and the tools are one object lying below it
 * rather than a row of them.
 *
 * The order runs the way the hand works. A step back sits at the left, out
 * of the way, because it is the one thing on the capsule that undoes rather
 * than draws and a child should have to mean it. The crayon sits in the
 * middle, where a thumb naturally lands, because it is the thing they pick
 * up a hundred times. The rubber sits at the right, as the other end of the
 * same axis: draw on the left of it, undo on the right of it.
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
    onOpenBox: (Boolean) -> Unit,
    onErase: (Boolean) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The three seats sit on the same rail the top bar uses, one at each
        // end and one in the middle, so the capsule and the bar above it line
        // up at both edges and the phone reads as one object rather than two
        // rows that happen to be centered. The seat size follows from the
        // room the rail got, with a floor a small hand can hit and a ceiling
        // so a tablet never gets a fence: the extra room goes into the air
        // between the seats, not into fat seats.
        val rail = minOf(maxWidth, RAIL_WIDTH)
        val reach = (rail / SEAT_SPAN).coerceIn(COIN_MIN, COIN_MAX)
        Row(
            modifier = Modifier
                .width(rail)
                .buttonShadow(RoundedCornerShape(reach * 0.62f), elevation = 5.dp)
                .clip(RoundedCornerShape(reach * 0.62f))
                .background(CrayonerColors.Card)
                .padding(horizontal = RAIL_PAD, vertical = reach * 0.08f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UndoSeat(
                ready = canUndo,
                onClick = onUndo,
                size = reach,
            )
            CapsuleSeat(
                onClick = { onOpenBox(!boxOpen) },
                armed = boxOpen,
                size = reach,
                label = stringResource(crayonNameRes(selected)) +
                    ", " + stringResource(R.string.pick_color),
            ) {
                // Standing, point down the way a held crayon is drawn: it is
                // the color in hand, so the child never has to remember which
                // one they picked up.
                val height = reach * 0.86f
                CrayonGlyph(
                    color = Color(selected),
                    modifier = Modifier.size(
                        width = height * CrayonShape.THICKNESS.toFloat(),
                        height = height,
                    ),
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

/**
 * The tallest and shortest a seat in the capsule is ever drawn.
 *
 * The floor is the accessibility rule: a target a small hand can find and
 * hit, and it is the number the floor may never go under. The ceiling is the
 * taste rule: the capsule is a tool lying on a desk under the paper, and it
 * must not compete with the picture for the eye, so on a phone it stays
 * close to its floor and only grows on a tablet where there is room to
 * spare. Three comfortable targets already cost most of a phone's width, so
 * the ceiling is deliberately near the floor and the glyphs inside the seats
 * are drawn as large as they can be without touching.
 */
private val COIN_MIN = 62.dp

/**
 * The shortest a seat is ever drawn. The ceiling is the taste rule: the
 * capsule is a tool lying on a desk under the paper and must not compete with
 * the picture, so it only grows on a tablet where there is room to spare.
 */
private val COIN_MAX = 66.dp

/**
 * Three seats, their air and the capsule's own padding, as seat widths. The
 * capsule and the top bar share [RAIL_WIDTH], so the leftmost and rightmost
 * controls of the two rows line up; the seat's own size follows from the
 * rail, and the room left over becomes the air between the seats rather than
 * a bigger target.
 */
private val SEAT_SPAN = 3.30f

/**
 * How far the outer controls sit from the rail's own edge, the same on the
 * capsule and on the [RAIL_WIDTH] bar above it, so the first seat and the
 * home button share a left edge and the last seat and the sound switch share
 * a right one.
 */
private val RAIL_PAD = 14.dp

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
        EraserGlyph(modifier = Modifier.size(size * 0.70f))
    }
}

/**
 * The step back: the mark the hand finished last comes off the paper, and
 * pressed again it keeps walking back, one finished mark a press.
 *
 * It is drawn quieter until there is a mark to take back, so a child who
 * presses it on a fresh sheet can see that the app heard the press and that
 * there was simply nothing to put down. Nothing is confirmed, nothing is
 * asked twice, and no press can lose a picture: the marks come back in the
 * reverse order they were drawn, and the rubber is what changes a whole
 * picture.
 */
@Composable
private fun UndoSeat(ready: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(R.string.undo)
    val ink = animateColorAsState(
        targetValue = if (ready) CrayonerColors.Ink else CrayonerColors.Ink.copy(alpha = 0.32f),
        label = "undo-ink",
    ).value
    CapsuleSeat(onClick = onClick, armed = false, size = size, label = label) {
        UndoGlyph(color = ink, size = size * 0.70f, modifier = Modifier)
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
