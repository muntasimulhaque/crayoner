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
 */

/** Rocket: white body, indigo space, blue violet fins, orange flame. */
fun rocketPage(): Page = Page(
    id = "rocket",
    regions = listOf(
        area("space", "space", Crayons.INDIGO, rect(0.0, 0.0, 1.0, 1.0)),
        area(
            "stars", "stars", Crayons.YELLOW,
            star(0.16, 0.18, 0.048, 0.019, 4),
            star(0.84, 0.15, 0.052, 0.021, 4),
            star(0.75, 0.42, 0.036, 0.014, 4),
            star(0.22, 0.46, 0.032, 0.013, 4),
            star(0.60, 0.14, 0.026, 0.010, 4),
        ),
        area(
            "flame", "flame", Crayons.ORANGE,
            blob(
                listOf(
                    point(0.44, 0.70),
                    point(0.56, 0.70),
                    point(0.545, 0.84),
                    point(0.50, 0.93),
                    point(0.455, 0.84),
                ),
            ),
        ),
        area(
            "fins", "fins", Crayons.BLUE_VIOLET,
            poly(0.395, 0.56, 0.395, 0.72, 0.28, 0.765, 0.30, 0.655),
            poly(0.605, 0.56, 0.605, 0.72, 0.72, 0.765, 0.70, 0.655),
        ),
        area("body", "body", Crayons.WHITE, round(0.395, 0.26, 0.21, 0.48, 0.03)),
        area("nose", "nose", Crayons.RED, poly(0.38, 0.30, 0.50, 0.07, 0.62, 0.30)),
        area("window", "window", Crayons.SKY_BLUE, circle(0.50, 0.40, 0.065)),
    ),
)

/** Lighthouse: white tower with red stripes on gray rocks, in blue green sea. */
fun lighthousePage(): Page = Page(
    id = "lighthouse",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf(
            "clouds", "cloud", Crayons.WHITE,
            cloud(0.16, 0.15, 0.85),
            cloud(0.72, 0.10, 0.55),
        ),
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
        area("tower", "tower", Crayons.WHITE, poly(0.42, 0.28, 0.58, 0.28, 0.66, 0.76, 0.34, 0.76)),
        area(
            "stripes", "stripes", Crayons.RED,
            poly(0.400, 0.40, 0.600, 0.40, 0.613, 0.48, 0.387, 0.48),
            poly(0.370, 0.58, 0.630, 0.58, 0.643, 0.66, 0.357, 0.66),
        ),
        area("lamp", "lamp", Crayons.YELLOW, round(0.435, 0.175, 0.13, 0.115, 0.025)),
        area("roof", "roof", Crayons.RED_ORANGE, poly(0.415, 0.185, 0.50, 0.075, 0.585, 0.185)),
    ),
)

/** Castle: timberwolf stone with yellow orange roofs, on a green hill. */
fun castlePage(): Page = Page(
    id = "castle",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("clouds", "cloud", Crayons.WHITE, cloud(0.83, 0.13, 0.62)),
        area("hill", "hill", Crayons.GREEN, band(wavyEdge(0.72, 0.02, 3, 1))),
        area(
            "walls", "walls", Crayons.TIMBERWOLF,
            round(0.28, 0.50, 0.44, 0.30, 0.02),
            round(0.20, 0.44, 0.16, 0.36, 0.03),
            round(0.64, 0.44, 0.16, 0.36, 0.03),
            rect(0.305, 0.445, 0.055, 0.055),
            rect(0.405, 0.445, 0.055, 0.055),
            rect(0.505, 0.445, 0.055, 0.055),
            rect(0.605, 0.445, 0.055, 0.055),
        ),
        area(
            "roofs", "roof", Crayons.YELLOW_ORANGE,
            poly(0.17, 0.445, 0.28, 0.295, 0.39, 0.445),
            poly(0.61, 0.445, 0.72, 0.295, 0.83, 0.445),
        ),
        area(
            "windows", "windows", Crayons.SKY_BLUE,
            circle(0.28, 0.56, 0.036),
            circle(0.72, 0.56, 0.036),
        ),
        area("door", "door", Crayons.BROWN, round(0.44, 0.62, 0.12, 0.18, 0.05)),
    ),
)

/** Umbrella: a red canopy with a yellow panel, under a rainy gray sky. */
fun umbrellaPage(): Page = Page(
    id = "umbrella",
    regions = listOf(
        area("sky", "sky", Crayons.CADET_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.50, 0.17, 1.5)),
        area(
            "rain", "rain", Crayons.BLUE,
            ellipse(0.19, 0.30, 0.012, 0.026, 0.0),
            ellipse(0.11, 0.38, 0.012, 0.026, 0.0),
            ellipse(0.10, 0.48, 0.012, 0.026, 0.0),
            ellipse(0.81, 0.30, 0.012, 0.026, 0.0),
            ellipse(0.89, 0.40, 0.012, 0.026, 0.0),
            ellipse(0.92, 0.52, 0.012, 0.026, 0.0),
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
        area("pole", "pole", Crayons.BROWN, rect(0.487, 0.28, 0.026, 0.505), circle(0.50, 0.79, 0.032)),
    ),
)
