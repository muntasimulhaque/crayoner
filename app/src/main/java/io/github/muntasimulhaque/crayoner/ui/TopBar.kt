package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Page

/** How tall the bar is, in both shapes. Home, the sample, the sound switch. */
private val BAR_HEIGHT = 66.dp

/**
 * Home at the left edge, the sample and the sound switch at the right.
 *
 * The bar is the full width of the screen and its two ends sit on the
 * screen's own corners: Home is the first thing a hand finds without looking,
 * and a sound switch hidden in from the edge is a switch a parent has to hunt
 * for. The picture stands between them, one step in from the sound switch so
 * the two round buttons on the right are two targets and not one.
 */
@Composable
internal fun TopBar(
    onHome: () -> Unit,
    soundOn: Boolean,
    onSound: (Boolean) -> Unit,
    onSample: () -> Unit,
    page: Page,
    announce: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(BAR_HEIGHT)
            .padding(horizontal = BAR_PAD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
    ) {
        CircleButton(
            onClick = onHome,
            background = CrayonerColors.Card,
            label = stringResource(R.string.home),
        ) {
            HomeIcon(color = CrayonerColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        // The sample stands at the right, one tap from the page: look at the
        // picture, look at the sheet, color.
        SampleButton(onClick = onSample, page = page, announce = announce)
        CircleButton(
            onClick = { onSound(!soundOn) },
            background = CrayonerColors.Card,
            label = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off),
        ) {
            SoundIcon(on = soundOn, color = CrayonerColors.Ink)
        }
    }
}

/**
 * How far the bar's own buttons sit from the edge of the screen, and the air
 * between the two at the right. The pad is the screen's own margin, not the
 * rail's: the bar is the one row that runs edge to edge, and its ends are the
 * corners a hand aims at.
 */
private val BAR_PAD = 10.dp
private val BAR_GAP = 12.dp
