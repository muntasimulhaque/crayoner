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
import io.github.muntasimulhaque.crayoner.core.flowerPetals
import io.github.muntasimulhaque.crayoner.core.point
import io.github.muntasimulhaque.crayoner.core.poly
import io.github.muntasimulhaque.crayoner.core.rect
import io.github.muntasimulhaque.crayoner.core.round
import io.github.muntasimulhaque.crayoner.core.wavyEdge

/**
 * A quiet garden street: a house, a tree, a mushroom and flowers. All
 * inanimate, all on one warm palette, and no sun in any of these skies, so
 * one page's weather never repeats the page before it.
 *
 * The greens are the box's own three: green for grass and leaves, yellow
 * green for the apple tree's young crown, green yellow where the rainbow
 * picks up the light.
 */

/** House: sky, two clouds, grass, wall, roof, door, window. */
fun housePage(): Page = Page(
    id = "house",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.18, 0.16, 0.85)),
        areaOf("cloud_high", "cloud", Crayons.WHITE, cloud(0.82, 0.12, 0.6)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.78, 0.018, 3, 0))),
        area("wall", "wall", Crayons.PEACH, round(0.26, 0.44, 0.48, 0.34, 0.02)),
        area("roof", "roof", Crayons.RED, poly(0.20, 0.465, 0.50, 0.205, 0.80, 0.465)),
        area("window", "window", Crayons.SKY_BLUE, circle(0.355, 0.555, 0.055)),
        area("door", "door", Crayons.BROWN, round(0.44, 0.58, 0.13, 0.20, 0.05)),
    ),
)

/** Tree: trunk, a leafy crown, and four apples waiting to be picked. */
fun treePage(): Page = Page(
    id = "tree",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.16, 0.15, 0.8)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.78, 0.02, 3, 1))),
        area("trunk", "trunk", Crayons.BROWN, poly(0.462, 0.42, 0.538, 0.42, 0.548, 0.80, 0.452, 0.80)),
        area(
            "crown", "leaves", Crayons.YELLOW_GREEN,
            circle(0.50, 0.33, 0.185),
            circle(0.345, 0.42, 0.125),
            circle(0.655, 0.42, 0.125),
            circle(0.40, 0.235, 0.115),
            circle(0.60, 0.235, 0.115),
        ),
        area(
            "apples", "apples", Crayons.SCARLET,
            circle(0.42, 0.30, 0.038),
            circle(0.60, 0.40, 0.038),
            circle(0.53, 0.21, 0.035),
            circle(0.36, 0.45, 0.032),
        ),
    ),
)

/** Mushroom: a red cap with white spots on a soft stem. */
fun mushroomPage(): Page = Page(
    id = "mushroom",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.17, 0.15, 0.8)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.75, 0.02, 3, 0))),
        area("stem", "stem", Crayons.PEACH, round(0.43, 0.50, 0.14, 0.27, 0.05)),
        area(
            "cap", "cap", Crayons.RED,
            blob(
                listOf(
                    point(0.50, 0.24),
                    point(0.72, 0.29),
                    point(0.85, 0.48),
                    point(0.83, 0.58),
                    point(0.50, 0.62),
                    point(0.17, 0.58),
                    point(0.15, 0.48),
                    point(0.28, 0.29),
                ),
            ),
        ),
        area(
            "spots", "spots", Crayons.WHITE,
            circle(0.36, 0.40, 0.05),
            circle(0.62, 0.375, 0.055),
            circle(0.50, 0.30, 0.042),
            circle(0.26, 0.50, 0.038),
            circle(0.74, 0.50, 0.038),
        ),
    ),
)

/** Flowers: a pink blossom and a violet one on leafy stems, under a cloud. */
fun flowersPage(): Page = Page(
    id = "flowers",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.18, 0.15, 0.85)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.76, 0.02, 3, 0))),
        area(
            "stems", "stems", Crayons.GREEN,
            poly(0.345, 0.76, 0.362, 0.38, 0.378, 0.38, 0.392, 0.76),
            poly(0.625, 0.76, 0.642, 0.30, 0.658, 0.30, 0.672, 0.76),
            ellipse(0.425, 0.615, 0.062, 0.030, -32.0),
            ellipse(0.298, 0.660, 0.062, 0.030, 32.0),
            ellipse(0.700, 0.575, 0.056, 0.028, -32.0),
            ellipse(0.582, 0.620, 0.056, 0.028, 32.0),
        ),
        areaOf("petals_a", "petals", Crayons.CARNATION_PINK, flowerPetals(0.36, 0.44, 0.17)),
        area("center_a", "flower_center", Crayons.YELLOW, circle(0.36, 0.44, 0.051)),
        areaOf("petals_b", "petals", Crayons.VIOLET, flowerPetals(0.64, 0.36, 0.14)),
        area("center_b", "flower_center", Crayons.YELLOW, circle(0.64, 0.36, 0.042)),
    ),
)
