package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R

/**
 * The one question the app ever asks: keep this picture, or start fresh.
 *
 * It is asked at the only moment it means anything, which is when a child
 * with work on the page presses Home. Both answers are as safe as the app can
 * make them: keeping the picture costs nothing, because the marks are already
 * on the desk where they were left, and starting fresh only means the next
 * visit opens on a clean sheet, which is a thing a child may well want. A
 * coloring is never thrown away by this question, and nothing here is a
 * scold.
 *
 * It is two big round buttons on a plate of desk, and the whole thing is
 * built from the app's own objects: the same coins as the bar, so both
 * answers read as buttons before either is read as a word. A child who cannot
 * read still has two answers under two thumbs, and the words say the same
 * thing to the grown-up standing behind them.
 *
 * Tapping the desk around the question, or pressing the system's own Back,
 * puts the question away and goes back to the page: the way out of a question
 * is never into one of its answers. A screen reader hears the way out named
 * for what it is, because a scrim that swallows a double tap silently is a
 * trap for the one child who cannot see the two buttons.
 *
 * Nothing is asked on a bare page. A fresh sheet has nothing to keep and
 * nothing to clear, so the question never appears where it would be noise.
 */
@Composable
fun SaveQuestion(
    onKeep: () -> Unit,
    onStartFresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val label = stringResource(R.string.keep_coloring)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrayonerColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .semantics { contentDescription = label }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .buttonShadow(RoundedCornerShape(28.dp), elevation = 12.dp)
                .background(CrayonerColors.Desk, RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.keep_picture),
                style = MaterialTheme.typography.titleMedium,
                color = CrayonerColors.Ink,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Keep it: on the left, where a child who is reading left to
                // right meets it first, and on the capsule's own cardboard,
                // because keeping the work is what the app does by default.
                CircleButton(
                    onClick = onKeep,
                    background = CrayonerColors.Cardboard,
                    label = stringResource(R.string.keep_it),
                ) {
                    TickGlyph(color = CrayonerColors.Ink)
                }
                // Start fresh: a coin of plain paper, and a cross, so the
                // two answers are told apart by their shape before their
                // color. Fresh paper is what this one means, so plain paper
                // is what it wears.
                CircleButton(
                    onClick = onStartFresh,
                    background = CrayonerColors.Card,
                    label = stringResource(R.string.start_fresh),
                ) {
                    CrossGlyph(color = CrayonerColors.Ink)
                }
            }
        }
    }
}
