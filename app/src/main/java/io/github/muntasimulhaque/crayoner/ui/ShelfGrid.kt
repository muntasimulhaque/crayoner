package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.host.ShelfState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ShelfGrid(shelf: ShelfState, onOpen: (String) -> Unit) {
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
                prewarmPageImages(pages, ::sampleFills, cardWidthPx, keep = true)
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
