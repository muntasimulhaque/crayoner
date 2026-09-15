package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * One capsule on the desk, holding the four things a hand reaches for while
 * coloring: the crayon it is drawing with, the rubber, and the two steps that
 * walk the paper back and forward.
 *
 * They are one object on purpose. Four separate coins put four separate
 * shadows on the desk and ask a three year old to find the small gap between
 * them; a single capsule says these belong together and reads as one thing to
 * put a finger on. It also leaves the desk itself clear: the paper is what
 * the child is here for, and the tools are one object lying below it rather
 * than a row of them.
 *
 * The order runs the way the hand works: the two things that touch the paper
 * first, and the two that move it after them. The crayon comes first, because
 * it is what a child picks up a hundred times and a hand reaches for the near
 * end of a row before it reaches across it. The rubber sits beside it, the
 * other thing that is held and laid down. The step back and the step forward
 * take the far end as one pair, so a child who walked a mistake out has to
 * pass the crayon to reach them (a thing worth meaning) and finds the other
 * end of the pair immediately next door rather than at the far side of the
 * screen.
 *
 * On a phone the capsule lies flat under the paper and on a sideways screen
 * it stands up beside it ([vertical]), which is the same four seats in the
 * same order either way: the tool a hand wants is in the first seat, and the
 * row grows away from the corner a hand comes from.
 *
 * Every seat is [CoinSize] and every mark in it is [IconSize], the bar's own
 * two numbers, and every seat is the same [CoinPlate] the bar's buttons wear.
 * A row of round controls is read as one object, so the capsule and the bar
 * are the same coins with the same marks in them, and the capsule is exactly
 * as wide (or as tall) as its four coins need: the two rows line up at both
 * ends, and the capsule is the size of the things it holds rather than a pill
 * stretched to fill the desk.
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
    vertical: Boolean = false,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // The capsule is as long as its own four coins need, and no longer:
        // the bar and the capsule share [RAIL_WIDTH] on a phone, so adding a
        // seat makes the whole rail a coin wider rather than squeezing the
        // others. On a desk too narrow for that (a small phone) the pad and
        // the air between the seats give way first, and the coins themselves
        // stop shrinking at [MIN_REACH], the smallest target a fingertip is
        // owed: a narrow screen gets smaller gaps, never smaller buttons.
        val extent = if (vertical) maxHeight else maxWidth
        val rail = minOf(extent, RAIL_WIDTH)
        val slack = (rail - MIN_REACH * SEATS).coerceAtLeast(0.dp)
        val pad = minOf(RAIL_PAD, slack / 2)
        val gap = minOf(SEAT_GAP, (slack - pad * 2) / (SEATS - 1))
        val reach = minOf(CoinSize, (rail - pad * 2 - gap * (SEATS - 1)) / SEATS)
        val seats: @Composable () -> Unit = {
            CrayonSeat(
                selected = selected,
                boxing = boxOpen,
                onClick = { onOpenBox(!boxOpen) },
                size = reach,
            )
            RubberSeat(
                armed = erasing,
                onClick = { onErase(!erasing) },
                size = reach,
            )
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
        }
        if (vertical) {
            Column(
                modifier = Modifier
                    .buttonShadow(RoundedCornerShape(reach * 0.62f), elevation = 5.dp)
                    .clip(RoundedCornerShape(reach * 0.62f))
                    .background(CrayonerColors.Card)
                    .padding(horizontal = reach * 0.07f, vertical = pad),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                seats()
            }
        } else {
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
                seats()
            }
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
 * How far the outer seats sit from the capsule's own edge. It is a hair
 * inside the coin, so the first and last seat sit on the capsule's own
 * cardboard rather than in the middle of a pool of it.
 */
private val RAIL_PAD = 8.dp

/**
 * The width of the rail the bar and the capsule both stand on, measured from
 * the coins it holds rather than chosen: [SEATS] coins, the air between them
 * and the two pads. It is one number for both rows, so the seats of the
 * capsule and the controls of the bar line up at both ends, and neither row
 * can be re-sized without the other following it.
 */
internal val RAIL_WIDTH =
    RAIL_PAD * 2 + CoinSize * SEATS + SEAT_GAP * (SEATS - 1)
