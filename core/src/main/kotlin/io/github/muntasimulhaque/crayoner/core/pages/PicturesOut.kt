package io.github.muntasimulhaque.crayoner.core.pages

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.area
import io.github.muntasimulhaque.crayoner.core.areaOf
import io.github.muntasimulhaque.crayoner.core.band
import io.github.muntasimulhaque.crayoner.core.circle
import io.github.muntasimulhaque.crayoner.core.cloud
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
 *
 * The sprinkles are gone from both sweets, and that is a deliberate loss.
 * A sprinkle is a part of a picture a child would want to color on its own,
 * and there is no such thing as a sprinkle wide enough for that: at the size
 * one has to be to read as a sprinkle, it is narrower than the crayon the
 * child is holding and smaller than the fingertip holding it, so the only
 * thing a child can do with one is color over it. An area of a picture that
 * cannot be hit is not an area, it is printed texture, and a page is better
 * without it than with a part that answers nothing. The scoop and the
 * frosting are whole areas now, which is what a small hand wanted anyway.
 *
 * Two of these pages have weather and two do not. The car drives under a
 * cloud that is moving away from it, the train has no cloud but its own
 * smoke, and the two sweets are indoors where the sky is a wall.
 */

/** Ice cream: two scoops on a cone, with a cherry and sprinkles. */
fun iceCreamPage(): Page = Page(
    id = "icecream",
    regions = listOf(
        area("wall", "wall", Crayons.MELON, rect(0.0, 0.0, 1.0, 1.0)),
        area("cone", "cone", Crayons.TAN, poly(0.375, 0.44, 0.625, 0.44, 0.50, 0.885)),
        area("scoop_a", "scoop", Crayons.CARNATION_PINK, circle(0.50, 0.415, 0.15)),
        area("scoop_b", "scoop", Crayons.APRICOT, circle(0.50, 0.255, 0.125)),
        area("cherry", "cherry", Crayons.RED, circle(0.50, 0.115, 0.062)),
    ),
)

/** Cupcake: a teal wrapper, a red violet swirl, a cherry, sprinkles. */
fun cupcakePage(): Page = Page(
    id = "cupcake",
    regions = listOf(
        area("wall", "wall", Crayons.WISTERIA, rect(0.0, 0.0, 1.0, 1.0)),
        area("table", "table", Crayons.CHESTNUT, rect(0.0, 0.80, 1.0, 0.25)),
        area("wrapper", "wrapper", Crayons.BLUE_GREEN, poly(0.35, 0.56, 0.65, 0.56, 0.61, 0.82, 0.39, 0.82)),
        area(
            "frosting", "frosting", Crayons.RED_VIOLET,
            circle(0.50, 0.46, 0.155),
            circle(0.50, 0.34, 0.12),
            circle(0.50, 0.25, 0.085),
        ),
        area("cherry", "cherry", Crayons.RED, circle(0.50, 0.155, 0.058)),
    ),
)

/** Car: a red car on a gray road, two sky blue windows, two wheels. */
fun carPage(): Page = Page(
    id = "car",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // One cloud, up and to the left, behind the car's own roof line: the
        // car is driving out from under it.
        areaOf("cloud", "cloud", Crayons.WHITE, cloud(0.245, 0.135, 0.88)),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.70, 0.02, 2, 0))),
        area("road", "road", Crayons.GRAY, band(wavyEdge(0.76, 0.008, 2, 0))),
        area(
            "body", "car", Crayons.RED,
            round(0.12, 0.60, 0.76, 0.17, 0.06),
            round(0.32, 0.48, 0.36, 0.16, 0.05),
        ),
        // Two windows, each of them wide enough to hold a mark of its own.
        area(
            "windows", "windows", Crayons.SKY_BLUE,
            round(0.345, 0.500, 0.145, 0.115, 0.025),
            round(0.505, 0.500, 0.145, 0.115, 0.025),
        ),
        // Wheels with real hubs: a wheel the size of a pea is not a wheel a
        // child can color, and two of them are the whole of what makes the
        // shape read as a car rather than as a loaf.
        area(
            "wheels", "wheels", Crayons.GRAY,
            circle(0.29, 0.775, 0.082),
            circle(0.71, 0.775, 0.082),
        ),
    ),
)

/** Train: a red orange engine with a sky blue window, black wheels, rails. */
fun trainPage(): Page = Page(
    id = "train",
    regions = listOf(
        area("sky", "sky", Crayons.SKY_BLUE, rect(0.0, 0.0, 1.0, 1.0)),
        // The only weather on this page is the train's own smoke, which is
        // what smoke is: it climbs away behind the engine and thins as it
        // goes.
        areaOf(
            "smoke", "smoke", Crayons.WHITE,
            cloud(0.26, 0.17, 1.15),
            cloud(0.075, 0.08, 0.7),
        ),
        area("ground", "grass", Crayons.GREEN, band(wavyEdge(0.74, 0.015, 3, 0))),
        // A real track with real sleepers: the rails and ties are what make
        // a train a train, and the ballast under them is a mark of its own
        // rather than a line the wheels stand on.
        area(
            "track", "track", Crayons.BROWN,
            rect(0.02, 0.775, 0.96, 0.105),
        ),
        area(
            "engine", "engine", Crayons.RED_ORANGE,
            round(0.14, 0.50, 0.46, 0.26, 0.06),
            round(0.60, 0.40, 0.24, 0.36, 0.04),
            round(0.20, 0.36, 0.11, 0.16, 0.03),
        ),
        area("window", "window", Crayons.SKY_BLUE, round(0.640, 0.450, 0.16, 0.13, 0.03)),
        // Three wheels, each of them a circle a fingertip can find.
        area(
            "wheels", "wheels", Crayons.BLACK,
            circle(0.23, 0.755, 0.072),
            circle(0.39, 0.755, 0.072),
            circle(0.68, 0.755, 0.072),
        ),
    ),
)
