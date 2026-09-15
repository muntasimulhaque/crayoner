package io.github.muntasimulhaque.crayoner.core.pages

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.area
import io.github.muntasimulhaque.crayoner.core.areaOf
import io.github.muntasimulhaque.crayoner.core.band
import io.github.muntasimulhaque.crayoner.core.blob
import io.github.muntasimulhaque.crayoner.core.circle
import io.github.muntasimulhaque.crayoner.core.cloud
import io.github.muntasimulhaque.crayoner.core.ellipse
import io.github.muntasimulhaque.crayoner.core.point
import io.github.muntasimulhaque.crayoner.core.poly
import io.github.muntasimulhaque.crayoner.core.rect
import io.github.muntasimulhaque.crayoner.core.round
import io.github.muntasimulhaque.crayoner.core.star
import io.github.muntasimulhaque.crayoner.core.wavyEdge

/**
 * Big things on the horizon: a rocket, a lighthouse, a castle and an
 * umbrella in the rain. Still nothing that lives: a tower is a tower, a
 * rocket is a shape, and the rain is water.
 *
 * The night sky on the rocket page is the one sky in the book with no
 * weather in it, and its stars are drawn big. A star small enough to read as
 * a sparkle is a star no fingertip can find; these are stickers, which is
 * what a star on a child's page is anyway.
 *
 * The other three pages each carry weather once, in a different place and at
 * a different scale, and the castle keeps a single cloud where the towers
 * are not. The lighthouse looks out on a sea with no cloud in the sky it is
 * lit against, because a sky with nothing in it is what you want when you
 * are looking for a light.
 */

/** Rocket: white body, indigo space, blue violet fins, orange flame. */
fun rocketPage(): Page = Page(
    id = "rocket",
    regions = listOf(
        area("space", "space", Crayons.INDIGO, rect(0.0, 0.0, 1.0, 1.0)),
        // Big stars, unevenly scattered, none of them near another, and none
        // of them near enough to the paper's own edge for the sheet to cut
        // one. A field of small stars is texture; five stars a child can
        // color are a sky.
        area(
            "stars", "stars", Crayons.YELLOW,
            star(0.19, 0.17, 0.068, 0.030, 4),
            star(0.81, 0.14, 0.072, 0.032, 4),
            star(0.75, 0.44, 0.058, 0.026, 4),
            star(0.25, 0.47, 0.054, 0.024, 4),
            star(0.62, 0.09, 0.050, 0.022, 4),
        ),
        area(
            "flame", "flame", Crayons.ORANGE,
            blob(
                listOf(
                    point(0.42, 0.70),
                    point(0.58, 0.70),
                    point(0.560, 0.84),
                    point(0.50, 0.94),
                    point(0.440, 0.84),
                ),
            ),
        ),
        area(
            "fins", "fins", Crayons.BLUE_VIOLET,
            poly(0.385, 0.55, 0.385, 0.72, 0.255, 0.775, 0.280, 0.650),
            poly(0.615, 0.55, 0.615, 0.72, 0.745, 0.775, 0.720, 0.650),
        ),
        area("body", "body", Crayons.WHITE, round(0.385, 0.25, 0.23, 0.49, 0.03)),
        area("nose", "nose", Crayons.RED, poly(0.365, 0.295, 0.50, 0.05, 0.635, 0.295)),
        // A porthole a hand can actually aim at.
        area("window", "window", Crayons.SKY_BLUE, circle(0.50, 0.40, 0.082)),
    ),
)

/** Lighthouse: white tower with red stripes on gray rocks, in blue green sea. */
fun lighthousePage(): Page = Page(
    id = "lighthouse",
    regions = listOf(
        // No cloud on this page: the sky a lamp is lit against is a clean
        // one, and this is the book's own answer to four pages of cloud.
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        area("sea", "sea", Crayons.BLUE_GREEN, band(wavyEdge(0.72, 0.02, 3, 0))),
        area(
            "rocks", "rocks", Crayons.GRAY,
            blob(
                listOf(
                    point(0.30, 0.78),
                    point(0.42, 0.70),
                    point(0.58, 0.70),
                    point(0.72, 0.78),
                    point(0.66, 0.88),
                    point(0.34, 0.88),
                ),
            ),
        ),
        area("tower", "tower", Crayons.WHITE, poly(0.415, 0.28, 0.585, 0.28, 0.66, 0.76, 0.34, 0.76)),
        // Two wide bands, each of them a mark of its own size: a stripe as
        // tall as a crayon is a stripe nobody can color.
        area(
            "stripes", "stripes", Crayons.RED,
            poly(0.394, 0.395, 0.606, 0.395, 0.622, 0.495, 0.378, 0.495),
            poly(0.360, 0.585, 0.640, 0.585, 0.656, 0.685, 0.344, 0.685),
        ),
        area("lamp", "lamp", Crayons.YELLOW, round(0.430, 0.165, 0.14, 0.125, 0.025)),
        area("roof", "roof", Crayons.RED_ORANGE, poly(0.405, 0.185, 0.50, 0.065, 0.595, 0.185)),
    ),
)

/** Castle: timberwolf stone with yellow orange roofs, on a green hill. */
fun castlePage(): Page = Page(
    id = "castle",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // One cloud, up on the right, above the roofs rather than between
        // them.
        areaOf("clouds", "cloud", Crayons.WHITE, cloud(0.82, 0.12, 0.66)),
        area("hill", "hill", Crayons.GREEN, band(wavyEdge(0.72, 0.02, 3, 1))),
        // Battlements with real teeth: the merlons are painted as squares on
        // the wall's own top edge, which is the shape of a castle and a row
        // of things a child can color.
        area(
            "walls", "walls", Crayons.TIMBERWOLF,
            round(0.28, 0.50, 0.44, 0.30, 0.02),
            round(0.20, 0.44, 0.16, 0.36, 0.03),
            round(0.64, 0.44, 0.16, 0.36, 0.03),
            rect(0.28, 0.435, 0.075, 0.075),
            rect(0.39, 0.435, 0.075, 0.075),
            rect(0.535, 0.435, 0.075, 0.075),
            rect(0.645, 0.435, 0.075, 0.075),
        ),
        area(
            "roofs", "roof", Crayons.YELLOW_ORANGE,
            poly(0.165, 0.445, 0.28, 0.275, 0.395, 0.445),
            poly(0.605, 0.445, 0.72, 0.275, 0.835, 0.445),
        ),
        // Two tower windows, each a circle a fingertip can find.
        area(
            "windows", "windows", Crayons.SKY_BLUE,
            circle(0.28, 0.565, 0.050),
            circle(0.72, 0.565, 0.050),
        ),
        area("door", "door", Crayons.BROWN, round(0.435, 0.615, 0.13, 0.185, 0.05)),
    ),
)

/** Umbrella: a red canopy with a yellow panel, under a rainy gray sky. */
fun umbrellaPage(): Page = Page(
    id = "umbrella",
    regions = listOf(
        area("sky", "sky", Crayons.CADET_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // The stick comes first of everything that stands in the weather:
        // everything else on this page is in front of it. An umbrella is a
        // stick with a roof over it, and a stick painted last is a stick
        // drawn through its own roof and through the cloud it is raining
        // from. Its top stands a little above the canopy, which is the one
        // part of the pole a real umbrella shows from the outside.
        area(
            "pole", "pole", Crayons.BROWN,
            rect(0.486, 0.245, 0.028, 0.535),
            // The handle: a knob with something to it, centered on the shaft
            // it belongs to, because a knob the width of the shaft is not a
            // knob and a knob beside the shaft is a knob that fell off.
            round(0.464, 0.762, 0.072, 0.062, 0.026),
        ),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.50, 0.18, 1.5)),
        // Raindrops big enough to be colored, falling on both sides and clear
        // of the canopy. A drop the width of a hair is not weather on a
        // coloring page, it is a mark the child cannot find, and a drop near
        // the paper's own edge is a drop the sheet cuts in half, which reads
        // as a mistake and not as rain.
        area(
            "rain", "rain", Crayons.BLUE,
            ellipse(0.185, 0.27, 0.032, 0.060, 0.0),
            ellipse(0.135, 0.44, 0.032, 0.060, 0.0),
            ellipse(0.215, 0.58, 0.032, 0.060, 0.0),
            ellipse(0.815, 0.29, 0.032, 0.060, 0.0),
            ellipse(0.865, 0.46, 0.032, 0.060, 0.0),
            ellipse(0.785, 0.59, 0.032, 0.060, 0.0),
        ),
        area(
            "canopy", "canopy", Crayons.RED,
            blob(
                listOf(
                    point(0.50, 0.28),
                    point(0.72, 0.33),
                    point(0.86, 0.50),
                    point(0.50, 0.52),
                    point(0.14, 0.50),
                    point(0.28, 0.33),
                ),
            ),
        ),
        area(
            "panel", "stripe", Crayons.YELLOW,
            poly(0.50, 0.29, 0.68, 0.335, 0.62, 0.515, 0.50, 0.52),
        ),
    ),
)
