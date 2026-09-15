package io.github.muntasimulhaque.crayoner.core.pages

import io.github.muntasimulhaque.crayoner.core.ArcBand
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.core.area
import io.github.muntasimulhaque.crayoner.core.areaOf
import io.github.muntasimulhaque.crayoner.core.band
import io.github.muntasimulhaque.crayoner.core.blob
import io.github.muntasimulhaque.crayoner.core.cloud
import io.github.muntasimulhaque.crayoner.core.point
import io.github.muntasimulhaque.crayoner.core.poly
import io.github.muntasimulhaque.crayoner.core.rect
import io.github.muntasimulhaque.crayoner.core.wavyEdge

/**
 * Pictures of sky and sea: a sailboat, a hot air balloon, a kite and a
 * rainbow. Everything here is inanimate (a boat, a basket, a shape, an
 * arch of light), and every page paints its background first, so a touch
 * anywhere on the paper always lands on something.
 *
 * There is no sun in this sky. A sun in the corner of every outdoor page is
 * the oldest habit in children's books, and sixteen pages of it is wallpaper.
 * Neither is there a cloud in the corner of every outdoor page, which is the
 * same habit wearing a different hat: every page used to open with one puffy
 * cloud up on the left, and sixteen of those made sixteen skies that were one
 * sky. Some of these pages carry weather and some do not, and no two put it
 * in the same place. The sail keeps two clouds because the wind is what the
 * page is about; the kite keeps one because a kite is nothing without moving
 * air; the rainbow keeps two because a rainbow needs rain to be leaving; and
 * the sea pages that have no weather at all are the ones whose empty sky
 * makes room for the sea.
 *
 * Each page's sky is a different blue, so sixteen pictures do not look like
 * one picture printed sixteen times: the balloon rides a cerulean morning,
 * the rainbow stands in a bright bluetiful afternoon, the kite flies in the
 * soft sky blue the box's own crayon is named for.
 */

/** Sailboat: the first page of the book, six areas, five crayons. */
fun sailPage(): Page = Page(
    id = "sail",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // One big cloud up on the left and a small wisp lower down on the
        // right: two different clouds, in two different bands of the sky,
        // which is what moving air looks like.
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.24, 0.14, 0.95)),
        areaOf("cloud_low", "cloud", Crayons.WHITE, cloud(0.78, 0.34, 0.55)),
        area("sea", "sea", Crayons.BLUE_GREEN, band(wavyEdge(0.66, 0.02, 3, 0))),
        area("sail", "sail", Crayons.WHITE, poly(0.47, 0.15, 0.79, 0.62, 0.47, 0.62)),
        area(
            "boat", "boat", Crayons.RED,
            blob(
                listOf(
                    point(0.20, 0.67),
                    point(0.82, 0.67),
                    point(0.73, 0.83),
                    point(0.50, 0.87),
                    point(0.27, 0.83),
                ),
            ),
            rect(0.455, 0.14, 0.034, 0.54),
        ),
    ),
)

/** Hot air balloon: a red envelope with a yellow orange gore, on a clear day. */
fun balloonPage(): Page = Page(
    id = "balloon",
    regions = listOf(
        area("sky", "sky", Crayons.CERULEAN, rect(0.0, 0.0, 1.0, 1.0)),
        // The weather on this page is all below and to the sides of the
        // balloon: one broad cloud low on the left where the envelope is
        // not, and one smaller and higher on the right, so the balloon is
        // the only thing in the middle of the sky.
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.18, 0.82, 1.0)),
        areaOf("cloud_high", "cloud", Crayons.WHITE, cloud(0.85, 0.24, 0.62)),
        area(
            "envelope", "balloon", Crayons.RED,
            blob(
                listOf(
                    point(0.50, 0.05),
                    point(0.70, 0.11),
                    point(0.76, 0.32),
                    point(0.62, 0.54),
                    point(0.55, 0.61),
                    point(0.45, 0.61),
                    point(0.38, 0.54),
                    point(0.24, 0.32),
                    point(0.30, 0.11),
                ),
            ),
        ),
        area(
            "gore", "stripe", Crayons.YELLOW_ORANGE,
            blob(
                listOf(
                    point(0.50, 0.06),
                    point(0.565, 0.17),
                    point(0.565, 0.40),
                    point(0.52, 0.59),
                    point(0.48, 0.59),
                    point(0.435, 0.40),
                    point(0.435, 0.17),
                ),
            ),
        ),
        area(
            "basket", "basket", Crayons.TAN,
            poly(0.43, 0.55, 0.452, 0.55, 0.472, 0.755, 0.45, 0.755),
            poly(0.57, 0.55, 0.548, 0.55, 0.528, 0.755, 0.55, 0.755),
            io.github.muntasimulhaque.crayoner.core.round(0.424, 0.742, 0.152, 0.122, 0.022),
        ),
    ),
)

/** Kite: a diamond with a yellow stripe and a violet red tail, high in the sky. */
fun kitePage(): Page = Page(
    id = "kite",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // The kite sits high and to the left and its tail runs down the
        // right of the page, so the sky it flies in is crowded on the right
        // and empty on the left: a kite needs moving air, and this is the
        // page where the air shows.
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.865, 0.105, 0.60)),
        areaOf("cloud_low", "cloud", Crayons.WHITE, cloud(0.135, 0.345, 0.78)),
        area("kite", "kite", Crayons.RED, poly(0.50, 0.10, 0.73, 0.34, 0.50, 0.58, 0.27, 0.34)),
        area("stripe", "stripe", Crayons.YELLOW, poly(0.50, 0.10, 0.565, 0.34, 0.50, 0.58, 0.435, 0.34)),
        // The tail's three bows are big enough to see and to aim at: a bow
        // a child cannot find on the paper is a bow the picture does not
        // really have.
        area(
            "tail", "tail", Crayons.VIOLET_RED,
            poly(
                0.494, 0.58, 0.462, 0.66, 0.424, 0.74, 0.408, 0.82, 0.396, 0.90,
                0.362, 0.90, 0.374, 0.82, 0.390, 0.74, 0.436, 0.66, 0.474, 0.575,
            ),
            poly(0.466, 0.640, 0.512, 0.678, 0.466, 0.716, 0.420, 0.678),
            poly(0.420, 0.745, 0.466, 0.783, 0.420, 0.821, 0.374, 0.783),
            poly(0.400, 0.850, 0.446, 0.888, 0.400, 0.926, 0.354, 0.888),
        ),
    ),
)

/** Rainbow: four bands arching over a green hill, clouds at both ends. */
fun rainbowPage(): Page {
    val hub = Vec2(0.5, 0.98)
    return Page(
        id = "rainbow",
        regions = listOf(
            area("sky", "sky", Crayons.BLUETIFUL, rect(0.0, 0.0, 1.0, 1.0)),
            area("band_red", "rainbow", Crayons.RED, ArcBand(hub, 0.0, 0.48, 180.0, 360.0)),
            area("band_yellow", "rainbow", Crayons.YELLOW, ArcBand(hub, 0.0, 0.40, 180.0, 360.0)),
            area("band_green", "rainbow", Crayons.GREEN_YELLOW, ArcBand(hub, 0.0, 0.32, 180.0, 360.0)),
            area("band_blue", "rainbow", Crayons.BLUE, ArcBand(hub, 0.0, 0.24, 180.0, 360.0)),
            area("ground", "hill", Crayons.GREEN, band(wavyEdge(0.86, 0.015, 3, 1))),
            // Two clouds, one at each foot of the arch, because that is
            // where the rain the rainbow came out of would be standing. They
            // are different sizes and sit at different heights, so the pair
            // does not read as a matched set of brackets.
            areaOf("cloud_left", "cloud", Crayons.WHITE, cloud(0.14, 0.22, 0.95)),
            areaOf("cloud_right", "cloud", Crayons.WHITE, cloud(0.88, 0.34, 0.68)),
        ),
    )
}
