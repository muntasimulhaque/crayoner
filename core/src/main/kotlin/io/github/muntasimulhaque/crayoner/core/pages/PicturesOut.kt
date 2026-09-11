package io.github.muntasimulhaque.crayoner.core.pages

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.area
import io.github.muntasimulhaque.crayoner.core.areaOf
import io.github.muntasimulhaque.crayoner.core.band
import io.github.muntasimulhaque.crayoner.core.circle
import io.github.muntasimulhaque.crayoner.core.cloud
import io.github.muntasimulhaque.crayoner.core.ellipse
import io.github.muntasimulhaque.crayoner.core.poly
import io.github.muntasimulhaque.crayoner.core.rect
import io.github.muntasimulhaque.crayoner.core.round
import io.github.muntasimulhaque.crayoner.core.wavyEdge

/**
 * A day out: an ice cream, a cupcake, a car and a train. Sweet things and
 * moving things, all inanimate, all built from the same few shapes.
 *
 * The two sweets teach the two warm flesh tones the box carries: apricot and
 * peach are the ice cream, melon is the wall behind it, and the cupcake lives
 * on a wisteria wall over a chestnut table.
 */

/** Ice cream: two scoops on a cone, with a cherry and sprinkles. */
fun iceCreamPage(): Page = Page(
    id = "icecream",
    regions = listOf(
        area("wall", "wall", Crayons.MELON, rect(0.0, 0.0, 1.0, 1.0)),
        area("cone", "cone", Crayons.TAN, poly(0.375, 0.44, 0.625, 0.44, 0.50, 0.885)),
        area("scoop_a", "scoop", Crayons.CARNATION_PINK, circle(0.50, 0.415, 0.15)),
        area("scoop_b", "scoop", Crayons.APRICOT, circle(0.50, 0.255, 0.125)),
        area("cherry", "cherry", Crayons.RED, circle(0.50, 0.125, 0.045)),
        area(
            "sprinkles", "sprinkles", Crayons.ORANGE,
            ellipse(0.505, 0.400, 0.019, 0.008, 12.0),
            ellipse(0.400, 0.440, 0.017, 0.007, -14.0),
            ellipse(0.600, 0.445, 0.017, 0.007, 14.0),
            // One dash only, low on the top scoop and off center. Two dashes
            // opposite each other read as eyes, and this book has no faces.
            ellipse(0.442, 0.330, 0.017, 0.007, -18.0),
        ),
    ),
)

/** Cupcake: a teal wrapper, a red violet swirl, a cherry, sprinkles. */
fun cupcakePage(): Page = Page(
    id = "cupcake",
    regions = listOf(
        area("wall", "wall", Crayons.WISTERIA, rect(0.0, 0.0, 1.0, 1.0)),
        area("table", "table", Crayons.CHESTNUT, rect(0.0, 0.80, 1.0, 0.20)),
        area("wrapper", "wrapper", Crayons.BLUE_GREEN, poly(0.35, 0.56, 0.65, 0.56, 0.61, 0.82, 0.39, 0.82)),
        area(
            "frosting", "frosting", Crayons.RED_VIOLET,
            circle(0.50, 0.46, 0.155),
            circle(0.50, 0.34, 0.12),
            circle(0.50, 0.25, 0.085),
        ),
        area("cherry", "cherry", Crayons.RED, circle(0.50, 0.175, 0.042)),
        area(
            "sprinkles", "sprinkles", Crayons.YELLOW,
            ellipse(0.425, 0.435, 0.017, 0.007, 25.0),
            ellipse(0.575, 0.400, 0.017, 0.007, -25.0),
            ellipse(0.560, 0.275, 0.015, 0.006, 20.0),
            // One dash only on the top swirl, off center, for the same
            // reason the ice cream has one: no two dots may read as eyes.
            ellipse(0.445, 0.320, 0.015, 0.006, -10.0),
        ),
    ),
)

/** Car: a red car on a gray road, two sky blue windows, two wheels. */
fun carPage(): Page = Page(
    id = "car",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.16, 0.15, 0.8)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.70, 0.02, 2, 0))),
        area("road", "road", Crayons.GRAY, band(wavyEdge(0.76, 0.008, 2, 0))),
        area(
            "body", "car", Crayons.RED,
            round(0.12, 0.60, 0.76, 0.17, 0.06),
            round(0.32, 0.48, 0.36, 0.16, 0.05),
        ),
        area(
            "windows", "windows", Crayons.SKY_BLUE,
            round(0.35, 0.505, 0.13, 0.10, 0.025),
            round(0.51, 0.505, 0.13, 0.10, 0.025),
        ),
        area(
            "wheels", "wheels", Crayons.GRAY,
            circle(0.30, 0.77, 0.055),
            circle(0.70, 0.77, 0.055),
        ),
    ),
)

/** Train: a red orange engine with a sky blue window, black wheels, rails. */
fun trainPage(): Page = Page(
    id = "train",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        areaOf(
            "smoke", "smoke", Crayons.WHITE,
            cloud(0.20, 0.16, 1.15),
            cloud(0.36, 0.09, 0.65),
        ),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.74, 0.015, 3, 0))),
        area("track", "track", Crayons.BROWN, rect(0.04, 0.80, 0.92, 0.055)),
        area(
            "engine", "engine", Crayons.RED_ORANGE,
            round(0.14, 0.50, 0.46, 0.26, 0.06),
            round(0.60, 0.40, 0.24, 0.36, 0.04),
            round(0.20, 0.36, 0.10, 0.16, 0.03),
        ),
        area("window", "window", Crayons.SKY_BLUE, round(0.645, 0.455, 0.15, 0.12, 0.03)),
        area(
            "wheels", "wheels", Crayons.BLACK,
            circle(0.24, 0.755, 0.058),
            circle(0.38, 0.755, 0.058),
            circle(0.68, 0.755, 0.058),
        ),
    ),
)
