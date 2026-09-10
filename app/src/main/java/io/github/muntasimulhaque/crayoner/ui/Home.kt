package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.host.ShelfState
import kotlin.math.abs

/**
 * The picture wall: every picture in the book, hung the way a child's
 * drawing gets hung on a wall. Each one is a sheet of paper with a strip of
 * washi tape over its top edge, set at a small angle of its own so the wall
 * looks made by hand rather than laid out by a grid, and named underneath in
 * the app's one display face.
 *
 * The wall never changes what it holds: all sixteen pictures are here from
 * the first launch. A finished one wears a honey star sticker; the one being
 * colored wears a small crayon. Both sit beside the name, never over the
 * picture, because the picture is the reason the card exists.
 */
@Composable
fun HomeScreen(
    shelf: ShelfState,
    onOpen: (String) -> Unit,
    onSound: (Boolean) -> Unit,
) {
    // The saved shelf arrives in a few milliseconds. Until it does, the desk
    // holds the screen rather than cards that would flip over as soon as the
    // read lands.
    if (!shelf.loaded) {
        Box(Modifier.fillMaxSize().background(CrayonerColors.Desk))
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrayonerColors.Desk),
    ) {
        ShelfHeader(soundOn = shelf.soundOn, onSound = onSound)
        ShelfGrid(shelf = shelf, onOpen = onOpen)
    }
}

@Composable
private fun ShelfHeader(soundOn: Boolean, onSound: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The brand's own mark, drawn by the same hand that draws the
            // box: one crayon, in the coral the first picture in the book
            // wears, leaning the way a crayon put down on a desk leans.
            CrayonGlyph(
                color = CrayonerColors.Coral,
                lying = true,
                modifier = Modifier
                    .size(width = 54.dp, height = 26.dp)
                    .rotate(-18f),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = CrayonerColors.Ink,
            )
            Spacer(Modifier.weight(1f))
            CircleButton(
                onClick = { onSound(!soundOn) },
                background = CrayonerColors.Card,
                label = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off),
            ) {
                SoundIcon(on = soundOn, color = CrayonerColors.Ink, size = 26.dp)
            }
        }
        // A crayon-drawn rule under the name: one wavering stroke, the way a
        // ruler-less line actually comes out, in the brand coral.
        CrayonUnderline(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(10.dp),
        )
    }
}

@Composable
private fun ShelfGrid(shelf: ShelfState, onOpen: (String) -> Unit) {
    BoxWithConstraints {
        val columns = when {
            maxWidth < 470.dp -> 2
            maxWidth < 720.dp -> 3
            maxWidth < 1010.dp -> 4
            else -> 5
        }
        // One size for every name: the wall speaks in one voice, and the
        // longest name sets the size for all of them.
        val pages = Pages.all
        val names = pages.map { stringResource(pageNameRes(it.id)) }
        val horizontal = 18.dp
        val gap = 14.dp
        val cell = (maxWidth - horizontal * 2 - gap * (columns - 1)) / columns
        val nameSize = rememberNameFontSize(names, cell - 12.dp)

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontal,
                end = horizontal,
                top = 10.dp,
                bottom = 28.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap + 4.dp),
        ) {
            items(pages, key = { it.id }) { page ->
                HungPicture(
                    page = page,
                    finished = shelf.isFinished(page.id),
                    started = shelf.hasDraft(page.id),
                    nameSize = nameSize,
                    onOpen = { onOpen(page.id) },
                )
            }
        }
    }
}

/** The floor a picture name never steps below, so it stays readable. */
private val MIN_NAME_SIZE = 14.sp

/**
 * One font size for every picture name, measured from the longest name in
 * the card's real width, so no name can ever clip on a narrow phone.
 */
@Composable
private fun rememberNameFontSize(names: List<String>, textWidth: Dp): TextUnit {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.titleLarge
    val density = LocalDensity.current
    return remember(names, textWidth, style, density) {
        val available = with(density) { textWidth.toPx() } * 0.98f
        val widest = names.maxOfOrNull { name ->
            measurer.measure(
                text = name,
                style = style,
                maxLines = 1,
                softWrap = false,
            ).size.width.toFloat()
        } ?: 0f
        if (widest <= available || widest == 0f) style.fontSize
        else (style.fontSize.value * (available / widest))
            .coerceAtLeast(MIN_NAME_SIZE.value)
            .sp
    }
}

/**
 * One picture on the wall: a sheet of paper, taped at its top edge, with the
 * picture on it, the name under it, and the tape's color taken from the
 * picture itself, so the wall is scattered with the same colors the crayons
 * hold.
 *
 * The whole card is one button: a small hand never has to find the picture
 * inside the plate, and where it lands on the card it always opens the same
 * picture.
 */
@Composable
private fun HungPicture(
    page: Page,
    finished: Boolean,
    started: Boolean,
    nameSize: TextUnit,
    onOpen: () -> Unit,
) {
    val name = stringResource(pageNameRes(page.id))
    val areas = stringResource(R.string.page_areas, page.regionCount)
    val state = when {
        finished -> ", " + stringResource(R.string.finished_mark)
        started -> ", " + stringResource(R.string.started_mark)
        else -> ""
    }
    // A small, stable tilt per picture: enough that the wall looks placed by
    // hand, small enough that nothing ever looks broken. Derived from the id
    // so a picture hangs at the same angle every single launch.
    val tilt = remember(page.id) { tiltFor(page.id) }

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
                .semantics { contentDescription = "$name, $areas$state" }
                // A taller top margin, so the tape crosses the mount's own
                // paper and only kisses the picture's top edge. The picture
                // is the reason the card exists and no art may hide behind
                // the thing that holds it up.
                .padding(start = 6.dp, end = 6.dp, top = 17.dp, bottom = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .border(
                        width = 1.dp,
                        color = CrayonerColors.Ink.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(3.dp),
                    ),
            ) {
                SamplePlate(page = page)
            }
            WashiTape(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )
            if (finished) {
                FinishedSticker(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (started && !finished) {
                CrayonMarkIcon(color = CrayonerColors.Coral, size = 14.dp)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = nameSize),
                color = CrayonerColors.Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** How far one picture leans on the wall: small, stable, never zero. */
private fun tiltFor(id: String): Float {
    val hash = abs(id.hashCode())
    val steps = (hash % 7) - 3 // -3 .. 3
    return steps * 0.6f
}

/**
 * The strip of tape holding a picture to the wall.
 *
 * It is one material everywhere, the way real tape off a roll is: a warm
 * translucent paper with a faint fiber running through it, a little wider
 * than it is deep, and never quite straight. Tinting it per picture was the
 * first idea and it went muddy, because a tint of a picture's own colors is
 * a tint of the picture; a single honest material stays clean on all sixteen.
 */
@Composable
private fun WashiTape(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(46.dp).height(15.dp).rotate(-2.5f)) {
        val w = size.width
        val h = size.height
        // The strip itself, with a shallow nick cut into each short edge, so
        // it was cut by hand rather than stamped.
        val strip = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.04f, h * 0.5f)
            lineTo(0f, h)
            lineTo(w, h)
            lineTo(w * 0.96f, h * 0.5f)
            lineTo(w, 0f)
            close()
        }
        drawPath(strip, CrayonerColors.Tape)
        // Its fibers: two fine lines along the roll's direction, a whisper
        // darker, so the strip reads as paper rather than as a painted bar.
        clipPath(strip) {
            drawLine(
                CrayonerColors.TapeFiber,
                androidx.compose.ui.geometry.Offset(0f, h * 0.34f),
                androidx.compose.ui.geometry.Offset(w, h * 0.28f),
                strokeWidth = h * 0.07f,
            )
            drawLine(
                CrayonerColors.TapeFiber,
                androidx.compose.ui.geometry.Offset(0f, h * 0.72f),
                androidx.compose.ui.geometry.Offset(w, h * 0.66f),
                strokeWidth = h * 0.05f,
            )
        }
    }
}

/** The honey star sticker a finished picture earns. */
@Composable
private fun FinishedSticker(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.finished_mark)
    Box(
        modifier = modifier
            .size(30.dp)
            .rotate(-12f)
            .buttonShadow(CircleShape, elevation = 3.dp)
            .clip(CircleShape)
            .background(CrayonerColors.Honey)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        StarIcon(color = CrayonerColors.Card, size = 17.dp)
    }
}

/** The sample picture filling the card's square. */
@Composable
private fun SamplePlate(page: Page) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
        val sidePx = with(LocalDensity.current) { maxWidth.roundToPx() }
        PageCanvas(
            page = page,
            fills = sampleFills(page),
            sidePx = sidePx,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
