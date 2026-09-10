package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R

/**
 * The one confirm in the app: wiping a whole picture is a lot to lose, so
 * it asks first. The leading coin is always the gentle answer, the way
 * Stay leads on every screen of the house's apps.
 */
@Composable
fun ClearConfirm(onStay: () -> Unit, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrayonerColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onStay() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(CrayonerColors.Card)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.clear_title),
                style = MaterialTheme.typography.titleLarge,
                color = CrayonerColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.clear_message),
                style = MaterialTheme.typography.bodyLarge,
                color = CrayonerColors.Ink.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                ChoiceCoin(
                    onClick = onStay,
                    background = CrayonerColors.Coral,
                    text = stringResource(R.string.stay),
                    color = CrayonerColors.Card,
                )
                Spacer(Modifier.width(36.dp))
                ChoiceCoin(
                    onClick = onClear,
                    background = CrayonerColors.Cardboard,
                    text = stringResource(R.string.clear),
                    color = CrayonerColors.Ink,
                )
            }
        }
    }
}

/** One plain coin with a word under it, for the two answers that matter. */
@Composable
private fun ChoiceCoin(
    onClick: () -> Unit,
    background: Color,
    text: String,
    color: Color,
    size: Dp = 56.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleButton(onClick = onClick, background = background, size = size, label = text) {
            Text(
                text = text.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = CrayonerColors.Ink,
        )
    }
}
