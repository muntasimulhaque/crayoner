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
import io.github.muntasimulhaque.crayoner.core.round
import io.github.muntasimulhaque.crayoner.core.sun
import io.github.muntasimulhaque.crayoner.core.wavyEdge

/**
 * Pictures of sky and sea: a sailboat, a hot air balloon, a kite and a
 * rainbow. Everything here is inanimate (a boat, a basket, a shape, an
 * arch of light), and every page paints its background first, so a tap
 * anywhere on the paper always lands on something.
 */

/** Sailboat: the first page of the book, six areas, five crayons. */
fun sailPage(): Page = Page(
    id = "sail",
    regions = listOf(
        area("sky", "sky", Crayons.SKY, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("sun", "sun", Crayons.YELLOW, sun(0.83, 0.15, 0.072)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.20, 0.15, 0.9)),
        area("sea", "sea", Crayons.BLUE, band(wavyEdge(0.66, 0.02, 3, 0))),
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
            rect(0.458, 0.14, 0.030, 0.54),
        ),
    ),
)

/** Hot air balloon: a red envelope with a yellow gore, on a clear day. */
fun balloonPage(): Page = Page(
    id = "balloon",
    regions = listOf(
        area("sky", "sky", Crayons.SKY, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("sun", "sun", Crayons.YELLOW, sun(0.85, 0.13, 0.068)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.17, 0.80, 1.0)),
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
            "gore", "stripe", Crayons.YELLOW,
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
            "basket", "basket", Crayons.BROWN,
            poly(0.43, 0.55, 0.452, 0.55, 0.472, 0.755, 0.45, 0.755),
            poly(0.57, 0.55, 0.548, 0.55, 0.528, 0.755, 0.55, 0.755),
            round(0.428, 0.745, 0.144, 0.115, 0.022),
        ),
    ),
)

/** Kite: a diamond with a yellow stripe and a pink tail, high in the sky. */
fun kitePage(): Page = Page(
    id = "kite",
    regions = listOf(
        area("sky", "sky", Crayons.SKY, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("sun", "sun", Crayons.YELLOW, sun(0.14, 0.14, 0.062)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.79, 0.16, 0.9)),
        area("kite", "kite", Crayons.RED, poly(0.50, 0.10, 0.73, 0.34, 0.50, 0.58, 0.27, 0.34)),
        area("stripe", "stripe", Crayons.YELLOW, poly(0.50, 0.10, 0.565, 0.34, 0.50, 0.58, 0.435, 0.34)),
        area(
            "tail", "tail", Crayons.PINK,
            poly(
                0.494, 0.58, 0.462, 0.66, 0.424, 0.74, 0.410, 0.82, 0.400, 0.90,
                0.372, 0.90, 0.382, 0.82, 0.396, 0.74, 0.436, 0.66, 0.474, 0.575,
            ),
            poly(0.452, 0.640, 0.482, 0.665, 0.452, 0.690, 0.422, 0.665),
            poly(0.414, 0.740, 0.444, 0.765, 0.414, 0.790, 0.384, 0.765),
            poly(0.398, 0.840, 0.428, 0.865, 0.398, 0.890, 0.368, 0.865),
        ),
    ),
)

/** Rainbow: four bands arching over a green hill, clouds at both ends. */
fun rainbowPage(): Page {
    val hub = Vec2(0.5, 0.98)
    return Page(
        id = "rainbow",
        regions = listOf(
            area("sky", "sky", Crayons.SKY, rect(0.0, 0.0, 1.0, 1.0)),
            area("band_red", "rainbow", Crayons.RED, ArcBand(hub, 0.0, 0.48, 180.0, 360.0)),
            area("band_yellow", "rainbow", Crayons.YELLOW, ArcBand(hub, 0.0, 0.40, 180.0, 360.0)),
            area("band_green", "rainbow", Crayons.GREEN, ArcBand(hub, 0.0, 0.32, 180.0, 360.0)),
            area("band_blue", "rainbow", Crayons.BLUE, ArcBand(hub, 0.0, 0.24, 180.0, 360.0)),
            area("ground", "hill", Crayons.GREEN, band(wavyEdge(0.86, 0.015, 3, 1))),
            areaOf("cloud_left", "cloud", Crayons.WHITE, cloud(0.16, 0.20, 0.95)),
            areaOf("cloud_right", "cloud", Crayons.WHITE, cloud(0.84, 0.32, 0.8)),
        ),
    )
}
