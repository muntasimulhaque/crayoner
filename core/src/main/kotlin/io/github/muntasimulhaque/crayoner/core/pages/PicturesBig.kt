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
        // Plump four pointed stars, unevenly scattered, none of them near
        // another, and none of them near enough to the paper's own edge for
        // the sheet to cut one. A star is a sticker, and a sticker a child
        // cannot fit a fingertip inside is printed texture, not a sticker:
        // these are wide enough in their own middle to be colored as the
        // stars they are.
        area(
            "stars", "stars", Crayons.YELLOW,
            star(0.20, 0.20, 0.11, 0.070, 4),
            star(0.79, 0.16, 0.11, 0.070, 4),
            star(0.74, 0.47, 0.10, 0.064, 4),
            star(0.26, 0.50, 0.10, 0.064, 4),
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
            poly(0.395, 0.53, 0.395, 0.74, 0.220, 0.80, 0.260, 0.635),
            poly(0.605, 0.53, 0.605, 0.74, 0.780, 0.80, 0.740, 0.635),
        ),
        area("body", "body", Crayons.WHITE, round(0.385, 0.25, 0.23, 0.49, 0.03)),
        area("nose", "nose", Crayons.RED, poly(0.365, 0.295, 0.50, 0.05, 0.635, 0.295)),
        // A porthole a hand can actually aim at.
        area("window", "window", Crayons.SKY_BLUE, circle(0.50, 0.40, 0.082)),
    ),
)

/** Lighthouse: white tower with a red belt on gray rocks, in blue green sea. */
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
                    point(0.28, 0.78),
                    point(0.42, 0.70),
                    point(0.58, 0.70),
                    point(0.74, 0.78),
                    point(0.68, 0.88),
                    point(0.32, 0.88),
                ),
            ),
        ),
        // The tower, the lamp and the roof are all big enough to hold a
        // finger, and so is the one red belt around the tower: a lighthouse
        // is read by its tower, its light and its belt, and three bands a
        // child can color are worth more than six bands they cannot. The
        // belt grew out of two thin ones, which left three white slivers of
        // tower between them that no hand could find.
        area("tower", "tower", Crayons.WHITE, poly(0.415, 0.33, 0.585, 0.33, 0.67, 0.78, 0.33, 0.78)),
        area(
            "stripes", "stripes", Crayons.RED,
            poly(0.382, 0.49, 0.618, 0.49, 0.650, 0.65, 0.350, 0.65),
        ),
        area("lamp", "lamp", Crayons.YELLOW, round(0.410, 0.195, 0.18, 0.145, 0.03)),
        area("roof", "roof", Crayons.RED_ORANGE, poly(0.350, 0.205, 0.50, 0.025, 0.650, 0.205)),
    ),
)

/** Castle: timberwolf stone walls on a green hill, with a door. */
fun castlePage(): Page = Page(
    id = "castle",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // One cloud, up on the right, above the taller wall rather than
        // between the two.
        areaOf("clouds", "cloud", Crayons.WHITE, cloud(0.82, 0.12, 0.90)),
        area("hill", "hill", Crayons.GREEN, band(wavyEdge(0.72, 0.02, 3, 1))),
        // Two walls of different heights: a tall keep on the right and a low
        // wall running out to the left, each wearing a row of square
        // battlements. The castle used to be two towers of one height with
        // two pointed roofs and a door on the center line, which is the
        // shape of a face from across the room: two roofs for eyes and a
        // door for a mouth. Nothing here is symmetrical, and there are no
        // windows and no roofs at all, so no part of it can be read as a
        // face. See D-072.
        area(
            "walls", "walls", Crayons.TIMBERWOLF,
            round(0.46, 0.40, 0.34, 0.40, 0.02),
            round(0.18, 0.56, 0.30, 0.24, 0.02),
            rect(0.470, 0.325, 0.075, 0.085),
            rect(0.575, 0.325, 0.085, 0.085),
            rect(0.700, 0.325, 0.075, 0.085),
            rect(0.200, 0.490, 0.075, 0.075),
            rect(0.315, 0.490, 0.065, 0.075),
        ),
        // One door, off the castle's center line and big enough for a
        // fingertip: a castle needs a way in, and a way in is the one thing
        // a wall cannot do without.
        area("door", "door", Crayons.BROWN, round(0.30, 0.62, 0.15, 0.18, 0.05)),
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
            rect(0.482, 0.245, 0.036, 0.535),
            // The handle: a knob with something to it, centered on the shaft
            // it belongs to and wide enough to hold a fingertip, because a
            // knob the width of the shaft is not a knob and a knob beside
            // the shaft is a knob that fell off.
            round(0.420, 0.740, 0.16, 0.13, 0.058),
        ),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.50, 0.18, 1.5)),
        // Four raindrops big enough to be colored, two falling on each side
        // and clear of the canopy. A drop the width of a hair is not weather
        // on a coloring page, it is a mark the child cannot find, and a drop
        // near the paper's own edge is a drop the sheet cuts in half, which
        // reads as a mistake and not as rain.
        area(
            "rain", "rain", Crayons.BLUE,
            ellipse(0.155, 0.300, 0.068, 0.105, 0.0),
            ellipse(0.205, 0.630, 0.068, 0.105, 0.0),
            ellipse(0.845, 0.320, 0.068, 0.105, 0.0),
            ellipse(0.795, 0.630, 0.068, 0.105, 0.0),
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
