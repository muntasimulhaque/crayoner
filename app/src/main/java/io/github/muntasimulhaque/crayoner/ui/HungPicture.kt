package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.Page
import kotlin.math.abs

/**
 * One picture on the wall: a sheet of paper with the picture on it and the
 * name under it.
 *
 * The card is a sheet lying flat on the desk, the same sheet the child will
 * color, drawn with the same corners and the same shadow. Nothing is stuck
 * over it: no tape and no pin, because the picture is the thing and a strip
 * of tape across its top edge is one more thing to look past. Pictures on a
 * wall are held up by being on the wall; the wall is the app's own desk.
 *
 * The whole card is one button: a small hand never has to find the picture
 * inside the plate, and wherever it lands on the card it opens the same
 * picture.
 */
@Composable
internal fun HungPicture(
    page: Page,
    nameSize: TextUnit,
    widthPx: Int,
    onOpen: () -> Unit,
) {
    val name = stringResource(pageNameRes(page.id))
    // A small, stable tilt per picture: enough that the wall looks placed by
    // hand, small enough that nothing ever looks broken. Derived from the id
    // so a picture hangs at the same angle every single launch.
    val tilt = remember(page.id) { tiltFor(page.id) }
    val heightPx = heightFor(widthPx)
    val density = LocalDensity.current
    val cardWidth = with(density) { widthPx.toDp() }
    val cardHeight = with(density) { heightPx.toDp() }

    Column(
        modifier = Modifier.rotate(tilt),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .buttonShadow(RoundedCornerShape(6.dp), elevation = 5.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CrayonerColors.Card)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics { contentDescription = name }
                // A plate of paper, even on all four sides: the picture is
                // the reason the card exists, and the mount around it is the
                // width of a hand's worth of blank margin and no more.
                .padding(CARD_MOUNT),
        ) {
            Box(
                modifier = Modifier
                    // The card's paper is exactly the size of the picture
                    // drawn on it, to the pixel, so the prewarmed image and
                    // the card that asks for it can never miss each other.
                    .size(width = cardWidth, height = cardHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .border(
                        width = 1.dp,
                        color = CrayonerColors.Ink.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(3.dp),
                    ),
            ) {
                SamplePlate(page = page, widthPx = widthPx, heightPx = heightPx)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = nameSize),
            color = CrayonerColors.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** The paper margin around a card's picture, on each of its four sides. */
internal val CARD_MOUNT = 6.dp

/** How far one picture leans on the wall: small, stable, never zero. */
private fun tiltFor(id: String): Float {
    val hash = abs(id.hashCode())
    val steps = (hash % 7) - 3 // -3 .. 3
    return steps * 0.6f
}

/** The sample picture filling the card's sheet, at the size it was drawn. */
@Composable
private fun SamplePlate(page: Page, widthPx: Int, heightPx: Int) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier.size(
            width = with(density) { widthPx.toDp() },
            height = with(density) { heightPx.toDp() },
        ),
    ) {
        PageCanvas(
            page = page,
            fills = remember(page) { sampleFills(page) },
            widthPx = widthPx,
            heightPx = heightPx,
            // A card never stalls a frame: until its picture is rendered it
            // draws the page's own print, and the shelf renders every card
            // off the main thread as soon as it knows how wide one is. Its
            // picture is then kept where a page can never push it out, so
            // the wall does not have to draw itself again on the way back
            // from a coloring.
            blocking = false,
            keep = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
