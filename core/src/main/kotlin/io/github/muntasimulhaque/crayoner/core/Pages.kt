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
    )

    fun byId(id: String): Page? = all.firstOrNull { it.id == id }
}
