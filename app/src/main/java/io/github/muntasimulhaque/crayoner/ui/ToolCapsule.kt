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

/**
 * One capsule on the desk, holding the three things a hand reaches for while
 * coloring: the crayon it is drawing with, the rubber, and the step back.
 *
 * They are one object on purpose. Three separate coins put three separate
 * shadows on the desk and ask a three year old to find the small gap between
 * them; a single capsule says these three belong together and reads as one
 * thing to put a finger on. It also leaves the desk itself clear: the paper
 * is what the child is here for, and the tools are one object lying below it
 * rather than a row of them.
 *
 * The crayon is drawn the way a real crayon is held (point down), the rubber
 * the way a real rubber sits in a box, and the step back is the one mark
 * that is not a hand drawing: a stroke running round on itself, in the
 * brand's own coral.
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
        // The three seats and the padding around them are one width, so the
        // capsule's seat size follows from the room it was given: a capsule
        // that has to shrink shrinks its seats rather than hanging off the
        // side of the desk. The floor keeps a seat a comfortable target, and
        // the capsule is wrapped rather than allowed to overflow, so a screen
        // narrower than three targets stacks them instead of cutting one.
        val reach = (maxWidth / SEAT_SPAN).coerceIn(COIN_MIN, COIN_MAX)
        Row(
            modifier = Modifier
                .buttonShadow(RoundedCornerShape(reach * 0.62f), elevation = 7.dp)
                .clip(RoundedCornerShape(reach * 0.62f))
                .background(CrayonerColors.Card)
                .padding(horizontal = reach * 0.16f, vertical = reach * 0.14f),
            horizontalArrangement = Arrangement.spacedBy(reach * 0.08f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                val height = reach * 0.80f
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
            UndoSeat(
                ready = canUndo,
                onClick = onUndo,
                size = reach,
            )
        }
    }
}

/** The tallest and shortest a seat in the capsule is ever drawn. */
private val COIN_MIN = 62.dp
private val COIN_MAX = 84.dp

/**
 * How many seats' worth of width one seat takes up, including its own gaps
 * and the capsule's padding: three seats, the two gaps between them and the
 * padding at either end, all of them a fixed fraction of the seat itself.
 */
private val SEAT_SPAN = 3.48f

/**
 * The rubber's seat. Press it and the crayon becomes an eraser: the next
 * mark rubs wax off the paper. Press it again and the crayon comes back.
 */
@Composable
private fun RubberSeat(armed: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(if (armed) R.string.eraser_on else R.string.eraser_off)
    CapsuleSeat(onClick = onClick, armed = armed, size = size, label = label) {
        EraserGlyph(armed = armed, modifier = Modifier.size(size * 0.56f))
    }
}

/**
 * The step back: the mark the hand finished last comes off the paper.
 *
 * It is drawn quieter until there is a mark to take back, so a child who
 * presses it on a fresh sheet can see that the app heard the press and that
 * there was simply nothing to put down. Nothing is confirmed, nothing is
 * asked twice, and no press can lose a picture: one mark at a time is all
 * this ever takes.
 */
@Composable
private fun UndoSeat(ready: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(R.string.undo)
    val ink = animateColorAsState(
        targetValue = if (ready) CrayonerColors.Ink else CrayonerColors.Ink.copy(alpha = 0.32f),
        label = "undo-ink",
    ).value
    CapsuleSeat(onClick = onClick, armed = false, size = size, label = label) {
        UndoGlyph(color = ink, size = size * 0.58f)
    }
}

/**
 * One seat in the capsule: the round plate a child presses, lifted a little
 * on its spring when the thing it holds is in hand. All three wear it, so
 * they are always the same size, the same shadow and the same reach, and a
 * small hand learns the row once.
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
