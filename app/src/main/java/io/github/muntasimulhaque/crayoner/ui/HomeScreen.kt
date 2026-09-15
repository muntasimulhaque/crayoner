package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.muntasimulhaque.crayoner.host.ShelfState

/**
 * The picture wall: every picture in the book, hung the way a child's
 * drawing gets hung on a wall. Each one is a sheet of paper, set at a small
 * angle of its own so the wall looks made by hand rather than laid out by a
 * grid, and named underneath in the app's one hand.
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
