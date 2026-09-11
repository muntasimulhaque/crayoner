package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Area
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The two things a child holds: a crayon, and the rubber.
 *
 * The tray is deliberately two things rather than thirty two. A three year
 * old does not choose between thirty two crayons; they pick one up and draw.
 * The crayon on the left always shows the color in hand, so the child can
 * see what they are about to draw with, and pressing it opens the box, where
 * every color is one touch away. The rubber on the right is the other real
 * thing in the box, kept beside the crayon it belongs to the way it is on a
 * real box, and it is the only way anything ever comes off the paper.
 */
@Composable
fun CrayonTray(
    selected: Long,
    erasing: Boolean,
    boxOpen: Boolean,
    onOpenBox: (Boolean) -> Unit,
    onPick: (Long) -> Unit,
    onErase: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val coin = (minOf(maxWidth.value, COIN_MAX) * 0.5f).dp.coerceIn(COIN_MIN, COIN_MAX_DP)
        Row(
            horizontalArrangement = Arrangement.spacedBy(coin * 0.34f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrayonLift(
                argb = selected,
                open = boxOpen,
                onClick = { onOpenBox(!boxOpen) },
                size = coin,
            )
            EraseLift(
                armed = erasing,
                onClick = { onErase(!erasing) },
                size = coin,
            )
        }
    }
}

/** The tallest and shortest the two coins are ever drawn. */
private val COIN_MIN = 62.dp
private val COIN_MAX_DP = 96.dp
private const val COIN_MAX = 96f

/**
 * The crayon in hand, standing on the cardboard. It is the crayon the child
 * is drawing with, drawn from the same geometry as every other crayon in the
 * app, and it lifts on a spring while the box is open, so the tray visibly
 * opens: what was in the box has come out of it.
 */
@Composable
private fun CrayonLift(
    argb: Long,
    open: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val label = stringResource(crayonNameRes(argb))
    val hint = stringResource(R.string.pick_color)
    val lift = animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "box-lift",
    ).value
    TrayCoin(
        onClick = onClick,
        size = size,
        lift = lift,
        armed = open,
        label = "$label, $hint",
    ) {
        // Standing, point down the way a held crayon is drawn: it is the
        // color in hand, so the child never has to remember which one they
        // picked up.
        val height = size * 0.62f
        CrayonGlyph(
            color = Color(argb),
            modifier = Modifier.size(width = height * CrayonShape.THICKNESS.toFloat(), height = height),
        )
    }
}

/**
 * The rubber end of the box. Press it and the crayon becomes an eraser: the
 * next mark rubs wax off the paper. Press it again and the crayon comes
 * back. It wears the same coin as everything else, and when it is armed it
 * is filled the way a held object is, so the child can see at a glance which
 * of the two ends of the box they are holding.
 */
@Composable
private fun EraseLift(
    armed: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val label = stringResource(if (armed) R.string.eraser_on else R.string.eraser_off)
    val press = animateFloatAsState(
        targetValue = if (armed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "eraser-arm",
    ).value
    TrayCoin(
        onClick = onClick,
        size = size,
        lift = press,
        armed = armed,
        label = label,
    ) {
        EraserGlyph(
            armed = armed,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

/**
 * One coin of the tray: the round card a child presses, lifted a little when
 * the thing it holds is in hand. Both the crayon and the rubber wear it, so
 * the two are always the same size, the same shadow and the same reach.
 */
@Composable
private fun TrayCoin(
    onClick: () -> Unit,
    size: Dp,
    lift: Float,
    armed: Boolean,
    label: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { translationY = -size.toPx() * 0.05f * lift }
            .buttonShadow(CircleShape, elevation = 6.dp)
            .clip(CircleShape)
            .background(CrayonerColors.Card)
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
