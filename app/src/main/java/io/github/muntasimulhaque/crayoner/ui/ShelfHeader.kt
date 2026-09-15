package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonShape

/**
 * The wall's own nameplate: the brand's mark, and the name in the app's own
 * hand.
 *
 * The mark comes first, the way a crayon is picked up before anything is
 * written with it: the crayon is the thing a child of three can read, and the
 * word beside it is for the grown-up. It is the very same mark the launcher
 * icon draws, at the same angle, with the same proportions, from the same
 * code: a mark that stood bolt upright in the app and leaned in the launcher
 * would be two crayons, and the wall is the first place a child meets the
 * app.
 *
 * It is sized to sit beside the name rather than above it. The name is set in
 * type and the mark is a drawing, so the two can never match to the pixel;
 * what they can do is stand the same height, which is what makes one line of
 * a wordmark instead of a drawing next to a heading. The mark's own box is
 * measured from the leaning crayon itself ([CrayonShape.turnedBounds]), so
 * the stick is as large as the line of type it stands beside without a corner
 * of its wrapper being clipped, and it is drawn a touch shorter than the
 * name's own height rather than a touch taller: a mark set that way reads as
 * part of the word, and one set taller reads as a badge beside it.
 */
@Composable
internal fun ShelfHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The brand's mark, at the app's own lean: the same crayon, at the
        // same angle, drawn by the same code as the launcher icon and the
        // seat on the capsule.
        CrayonGlyph(
            color = CrayonerColors.Coral,
            leanDeg = CrayonShape.MARK_LEAN,
            modifier = Modifier.size(width = MarkBox.WIDTH, height = MarkBox.HEIGHT),
        )
        Spacer(Modifier.width(MarkBox.GAP))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = CrayonerColors.Ink,
        )
    }
}

/**
 * The nameplate's crayon: how big it is drawn, and nothing else.
 *
 * The lean is the app's own mark's ([CrayonShape.MARK_LEAN]), which the
 * launcher icon draws too, so the crayon a child taps on the home screen and
 * the crayon beside the app's name are the same object at the same angle.
 */
private object MarkBox {
    private val bounds = CrayonShape.turnedBounds(CrayonShape.MARK_TURN)

    /**
     * How long the crayon itself is drawn, in dp. The name is set at 37 sp,
     * which stands about 44 dp tall on a screen, so the stick beside it is
     * drawn to land just under that: close enough to stand in the same line
     * as the word, and short enough that it never towers over it.
     */
    private const val LENGTH_DP = 42f

    /** The air between the mark and the word it belongs to. */
    val GAP: Dp = 12.dp

    val WIDTH: Dp = (LENGTH_DP * bounds.w / CrayonShape.LENGTH).dp
    val HEIGHT: Dp = (LENGTH_DP * bounds.h / CrayonShape.LENGTH).dp
}
