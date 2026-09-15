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
 * These four are the book's one neighborhood, and each of them has its own
 * weather instead of the same two puffs in the same two corners: the house
 * has a cloud moving off to the right, the tree has one low cloud the crown
 * stands beside, the mushroom has none at all, and the flowers have a small
 * one high up where the stems are not. A row of pages that each open with an
 * identical cloud is a row of the same picture.
 *
 * The greens are the box's own three: green for grass and leaves, yellow
 * green for the apple tree's young crown, green yellow where the rainbow
 * picks up the light.
 */

/** House: sky, one cloud, grass, wall, roof, door, window. */
fun housePage(): Page = Page(
    id = "house",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // One cloud, high and to the right, with the roof rising toward the
        // left of it: the weather and the house are not in each other's way.
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.80, 0.205, 0.90)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.78, 0.018, 3, 0))),
        area("wall", "wall", Crayons.PEACH, round(0.26, 0.44, 0.48, 0.34, 0.02)),
        area("roof", "roof", Crayons.RED, poly(0.20, 0.465, 0.50, 0.205, 0.80, 0.465)),
        // The window is a real circle a child can aim at, not a dot: at this
        // book's smallest, a fingertip needs somewhere to land, and a
        // painted-on window the width of a crayon is not a place to color.
        area("window", "window", Crayons.SKY_BLUE, circle(0.345, 0.555, 0.072)),
        area("door", "door", Crayons.BROWN, round(0.435, 0.575, 0.14, 0.205, 0.05)),
    ),
)

/** Tree: trunk, a leafy crown, and four apples waiting to be picked. */
fun treePage(): Page = Page(
    id = "tree",
    regions = listOf(
        // No weather on this page. The crown is the whole top of the picture
        // and a cloud behind it reads as a second bush, so the tree stands
        // against open sky; the pages around it are the ones that carry
        // weather, which is what keeps any of them from being wallpaper.
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.78, 0.02, 3, 1))),
        area("trunk", "trunk", Crayons.BROWN, poly(0.44, 0.42, 0.56, 0.42, 0.585, 0.80, 0.415, 0.80)),
        area(
            "crown", "leaves", Crayons.YELLOW_GREEN,
            circle(0.50, 0.33, 0.185),
            circle(0.345, 0.42, 0.125),
            circle(0.655, 0.42, 0.125),
            circle(0.40, 0.235, 0.115),
            circle(0.60, 0.235, 0.115),
        ),
        // Four apples, all large enough to be picked out with a fingertip,
        // and unevenly spaced: an orchard of four identical dots in a ring
        // is an ornament, not fruit. Each one has to hold a finger of its
        // own: a basket of four apples where three are wide passes an audit
        // that measures the basket and fails the child who wants the fourth.
        area(
            "apples", "apples", Crayons.SCARLET,
            circle(0.42, 0.30, 0.070),
            circle(0.615, 0.395, 0.068),
            circle(0.545, 0.205, 0.068),
            circle(0.352, 0.455, 0.066),
        ),
    ),
)

/** Mushroom: a red cap with white spots on a soft stem. */
fun mushroomPage(): Page = Page(
    id = "mushroom",
    regions = listOf(
        // No weather on this page at all. Three of the four pages in this
        // corner of the book used to carry the same cloud; this one's empty
        // sky is what makes the others' clouds weather rather than wallpaper.
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
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
        // Five spots, none smaller than a fingertip, and no two of them
        // sitting opposite each other at the same height: two round spots
        // facing each other across a cap read as eyes, and this book has no
        // faces. The three across the top are at three different heights and
        // unevenly spaced, and the two low ones sit at clearly different
        // heights, one tucked in and one out at the cap's edge.
        area(
            "spots", "spots", Crayons.WHITE,
            circle(0.355, 0.395, 0.078),
            circle(0.645, 0.325, 0.074),
            circle(0.50, 0.285, 0.070),
            circle(0.235, 0.500, 0.068),
            circle(0.745, 0.545, 0.072),
        ),
    ),
)

/** Flowers: a pink blossom and a violet one on leafy stems, under a cloud. */
fun flowersPage(): Page = Page(
    id = "flowers",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // One small cloud, high and to the right, well clear of both
        // blossoms: the flowers are what this page is about.
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.17, 0.115, 0.90)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.76, 0.02, 3, 0))),
        area(
            "stems", "stems", Crayons.GREEN,
            poly(0.345, 0.76, 0.362, 0.38, 0.378, 0.38, 0.392, 0.76),
            poly(0.625, 0.76, 0.642, 0.30, 0.658, 0.30, 0.672, 0.76),
            ellipse(0.430, 0.615, 0.095, 0.060, -32.0),
            ellipse(0.292, 0.660, 0.095, 0.060, 32.0),
            ellipse(0.704, 0.575, 0.088, 0.056, -32.0),
            ellipse(0.578, 0.620, 0.088, 0.056, 32.0),
        ),
        areaOf("petals_a", "petals", Crayons.CARNATION_PINK, flowerPetals(0.36, 0.44, 0.20)),
        area("center_a", "flower_center", Crayons.YELLOW, circle(0.36, 0.44, 0.068)),
        areaOf("petals_b", "petals", Crayons.VIOLET, flowerPetals(0.64, 0.36, 0.19)),
        area("center_b", "flower_center", Crayons.YELLOW, circle(0.64, 0.36, 0.068)),
    ),
)
