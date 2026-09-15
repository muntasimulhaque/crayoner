package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonShape

/**
 * The crayon's seat: the color in hand, and the lid to the box of colors.
 *
 * The crayon is drawn at the app's own lean ([CrayonShape.MARK_LEAN]), the
 * same angle the launcher icon, the wall's nameplate and the crayon beside
 * the app's name wear, so the crayon a child taps on the home screen and the
 * crayon they hold on the page are one object. Its box is [IconSize], the
 * same box the rubber and the two steps are drawn in, so the shaped mark is
 * fitted to the size of its neighbors and cannot come out taller than they
 * are.
 */
@Composable
internal fun CrayonSeat(
    selected: Long,
    boxing: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val label = stringResource(crayonNameRes(selected)) +
        ", " + stringResource(R.string.pick_color)
    CapsuleSeat(
        onClick = onClick,
        armed = boxing,
        size = size,
        label = label,
    ) {
        CrayonGlyph(
            color = Color(selected),
            leanDeg = CrayonShape.MARK_LEAN,
            modifier = Modifier.size(IconSize),
        )
    }
}

/**
 * The rubber's seat. Press it and the crayon becomes an eraser: the next
 * mark rubs wax off the paper. Press it again and the crayon comes back.
 *
 * The rubber itself looks the same whether it is picked up or lying down:
 * the seat's own plate is the whole selection mark (see [CapsuleSeat]), and
 * a rubber is one object whatever its owner is doing with it.
 */
@Composable
internal fun RubberSeat(armed: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(if (armed) R.string.eraser_on else R.string.eraser_off)
    CapsuleSeat(onClick = onClick, armed = armed, size = size, label = label) {
        EraserGlyph(color = CrayonerColors.Ink, size = IconSize)
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
 *
 * Quieter means a lighter ink, not a ghost: at a third of the ink the two
 * steps read as washed-out marks beside the crisp crayon and rubber, which
 * is the difference between a row of buttons and a row of some buttons.
 * They sit at a little under half, which is quiet enough to say there is
 * nothing to take back and still solid enough to be a button.
 */
@Composable
internal fun StepSeat(ready: Boolean, forward: Boolean, onClick: () -> Unit, size: Dp) {
    val label = stringResource(if (forward) R.string.redo else R.string.undo)
    val ink = animateColorAsState(
        targetValue = if (ready) CrayonerColors.Ink else CrayonerColors.Ink.copy(alpha = 0.45f),
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
 * One seat in the capsule: the same coin the bar's buttons wear, lifted a
 * little on its spring when the thing it holds is in hand.
 *
 * [armed] is the one selection language in the app, and it is the same one on
 * every seat: the plate under the thing in hand takes the capsule's own
 * cardboard, so which tool is picked up reads at a glance and no seat means
 * anything different from any other. The plate is not the button, so it is
 * drawn behind the mark and never as a ring around it.
 */
@Composable
internal fun CapsuleSeat(
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
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .offset(y = -(size * 0.045f) * lift),
    ) {
        CoinPlate(
            background = if (armed) CrayonerColors.Cardboard else Color.Transparent,
            size = size,
            label = label,
            selected = armed,
            // Inside the capsule the plate is a mark of what is in hand, not a
            // second floating object: the capsule already carries the shadow
            // for everything on it.
            elevation = 0.dp,
            onClick = onClick,
        ) {
            content()
        }
    }
}
