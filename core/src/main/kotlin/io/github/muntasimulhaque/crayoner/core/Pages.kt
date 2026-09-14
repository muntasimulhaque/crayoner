package io.github.muntasimulhaque.crayoner.core

import io.github.muntasimulhaque.crayoner.core.pages.balloonPage
import io.github.muntasimulhaque.crayoner.core.pages.carPage
import io.github.muntasimulhaque.crayoner.core.pages.castlePage
import io.github.muntasimulhaque.crayoner.core.pages.cupcakePage
import io.github.muntasimulhaque.crayoner.core.pages.flowersPage
import io.github.muntasimulhaque.crayoner.core.pages.housePage
import io.github.muntasimulhaque.crayoner.core.pages.iceCreamPage
import io.github.muntasimulhaque.crayoner.core.pages.kitePage
import io.github.muntasimulhaque.crayoner.core.pages.lighthousePage
import io.github.muntasimulhaque.crayoner.core.pages.mushroomPage
import io.github.muntasimulhaque.crayoner.core.pages.rainbowPage
import io.github.muntasimulhaque.crayoner.core.pages.rocketPage
import io.github.muntasimulhaque.crayoner.core.pages.sailPage
import io.github.muntasimulhaque.crayoner.core.pages.trainPage
import io.github.muntasimulhaque.crayoner.core.pages.treePage
import io.github.muntasimulhaque.crayoner.core.pages.umbrellaPage

/**
 * The book: sixteen pages, ordered the way a parent would hand them over,
 * the calmest first. The order is content, not difficulty: nothing here is
 * ever locked. Every page's areas are colored in crayons from the one box
 * ([Crayons]), so whatever the child holds, every area of every picture can
 * be matched exactly.
 *
 * Every picture is composed in a square by its own builder, because a square
 * is the simplest box to draw in, and then fitted onto the taller sheet a
 * child really colors on ([Page.ASPECT]): the authored square is scaled
 * about its own center so it lands exactly on the paper, corner to corner,
 * which makes every subject a fifth bigger without stretching anything. A
 * full-bleed background grows a little past the paper's own left and right
 * edges and is clipped to the sheet, so no picture ever shows bare desk.
 */
object Pages {

    val all: List<Page> = listOf(
        sailPage(),
        treePage(),
        balloonPage(),
        iceCreamPage(),
        mushroomPage(),
        kitePage(),
        flowersPage(),
        rainbowPage(),
        cupcakePage(),
        housePage(),
        carPage(),
        umbrellaPage(),
        rocketPage(),
        trainPage(),
        lighthousePage(),
        castlePage(),
    ).map { it.laidOut() }

    fun byId(id: String): Page? = all.firstOrNull { it.id == id }
}

/**
 * One authored square composition, fitted onto the taller sheet.
 *
 * This is the one place the two coordinate spaces meet, so a picture builder
 * never has to know about the page's proportions and no page can come out
 * stretched. The ground is the whole paper by definition, and every other
 * shape is scaled uniformly onto it.
 */
private fun Page.laidOut(): Page {
    val laid = regions.mapIndexed { index, region ->
        if (isGround(index)) {
            Region(region.id, region.kind, region.fillArgb, listOf(rect(0.0, 0.0, 1.0, Page.ASPECT)))
        } else {
            region.copy(parts = region.parts.map { it.fitted(Page.ASPECT) })
        }
    }
    return Page(id, laid)
}
