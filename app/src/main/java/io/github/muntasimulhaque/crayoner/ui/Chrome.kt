package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One floating shadow for every button in the app: the round coins and the
 * shelf cards lift off the paper by the same ink shadow, so a button never
 * melts into the ground it sits on and every button reads as the same kind
 * of thing.
 */
internal fun Modifier.buttonShadow(shape: Shape, elevation: Dp = 5.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = CrayonerColors.Shadow,
    spotColor = CrayonerColors.Shadow,
)

/**
 * The one round control in the app, in the two states of the thing itself:
 * a plate a child presses, and a plate that is only holding something up.
 *
 * Every round surface is this composable, the bar's home button, the sample
 * and the sound switch as much as the capsule's four seats, which is what
 * keeps one hand's work from reading as two apps stacked on one screen. The
 * plate is a coin of paper with the app's one shadow under it; a press dips
 * it by a hair and ripples in ink, so a child can see the app has heard the
 * press even when the press had nothing to do.
 *
 * [background] is the plate's own paper. A seat that is picked up passes the
 * capsule's cardboard instead, which is the app's one selection language: a
 * plate under the thing in hand and nothing else.
 */
@Composable
internal fun CoinPlate(
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = CoinSize,
    label: String? = null,
    selected: Boolean = false,
    elevation: Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val dip by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "coin-press",
    )
    val shape = CircleShape
    val indication = LocalIndication.current
    Box(
        modifier = modifier
            // The layer is outermost so the press carries the plate, its
            // shadow and the mark on it as one object: a coin that dipped
            // only its own drawing would read as two things moving.
            .graphicsLayer {
                val k = 1f - 0.055f * dip
                scaleX = k
                scaleY = k
                translationY = 1.6.dp.toPx() * dip
            }
            .buttonShadow(shape, elevation)
            .size(size)
            .clip(shape)
            .background(background)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = source,
                            indication = indication,
                            role = Role.Button,
                            onClick = onClick,
                        )
                        .semantics {
                            if (label != null) contentDescription = label
                            this.selected = selected
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** One round press: [CoinPlate] with the plate's own paper and a press. */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = CoinSize,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    CoinPlate(
        background = background,
        modifier = modifier,
        size = size,
        label = label,
        elevation = 5.dp,
        onClick = onClick,
        content = content,
    )
}
