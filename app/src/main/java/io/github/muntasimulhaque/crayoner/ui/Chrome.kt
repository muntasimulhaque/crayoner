package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
 * A round pressable used across the app: the floating shadow is baked in
 * here, so every coin wears it and no caller can forget it. [label] names
 * it for TalkBack.
 */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .buttonShadow(CircleShape)
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (label != null) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
