package io.github.muntasimulhaque.crayoner.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.host.ShelfState
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The picture wall: every picture in the book, hung the way a child's
 * drawing gets hung on a wall. Each one is a sheet of paper with a strip of
 * washi tape over its top edge, set at a small angle of its own so the wall
 * looks made by hand rather than laid out by a grid, and named underneath in
 * the app's one hand.
 *
 * Every card shows the finished picture, because the picture is the promise
 * the card makes: tap it and this is what you get to color.
 *
 * The wall never changes what it holds and never marks it: all sixteen are
 * here from the first launch, none of them is locked, none of them is
 * stamped, and nothing on the wall says where the child left off. A page
 * they were working on simply comes back the way they left it.
 */
@Composable
fun HomeScreen(
    shelf: ShelfState,
    onOpen: (String) -> Unit,
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
        ShelfHeader()
        ShelfGrid(shelf = shelf, onOpen = onOpen)
    }
}

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
private fun ShelfHeader() {
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

        // The wall's pictures are drawn once each, off the main thread, the
        // moment the shelf knows how wide its cards are: sixteen pages of wax
        // is real work, and doing it here rather than under the first
        // scrolling finger is the difference between a wall that slides and
        // a wall that stutters. The width is the card's own inner width, to
        // the pixel, which is the width the card will ask for when it draws:
        // a prewarmed picture that no card can find is work done for nobody,
        // and it was, once, because the prewarm measured the whole cell
        // while the card is inset by its own mount.
        val cardWidthPx = with(LocalDensity.current) { (cell - CARD_MOUNT * 2).roundToPx() }
        LaunchedEffect(cardWidthPx, pages) {
            if (cardWidthPx <= 0) return@LaunchedEffect
            withContext(Dispatchers.Default) {
                prewarmPageImages(pages, ::sampleFills, cardWidthPx)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = rememberLazyGridState(),
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
                    nameSize = nameSize,
                    widthPx = cardWidthPx,
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
private fun HungPicture(
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
private val CARD_MOUNT = 6.dp

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
            // A card never stalls a frame: until its picture is rendered,
            // it draws itself live, and the shelf renders every card off
            // the main thread as soon as it knows how wide one is.
            blocking = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
