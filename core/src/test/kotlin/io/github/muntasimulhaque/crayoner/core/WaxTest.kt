package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wax material's geometry: the surface that goes down on an area and the
 * passes a hand makes over it. Both renderers draw these, so they are the
 * difference between a colored picture and a printed one, and their rules
 * have to hold on every page of the book.
 */
class WaxTest {

    @Test
    fun theSameAreaIsColoredTheSameWayEveryTime() {
        // Deterministic down to the last wobble, or the store art and the
        // screen would show two different pages and a redraw would shimmer.
        for (page in Pages.all) {
            for (region in page.regions) {
                assertEquals(
                    "${page.id}/${region.id} rubs differently on a second read",
                    Wax.passes(region),
                    Wax.passes(region),
                )
                assertEquals(
                    "${page.id}/${region.id} surfaces differently on a second read",
                    Wax.surface(region.fillArgb, 16, Wax.angleDeg(region), 0, Wax.seed(region)).toList(),
                    Wax.surface(region.fillArgb, 16, Wax.angleDeg(region), 0, Wax.seed(region)).toList(),
                )
            }
        }
    }

    @Test
    fun everyPassIsLongEnoughToCrossItsOwnArea() {
        // A pass that stopped inside the area would leave a bald patch at the
        // edge of the color, which is the one thing wax never does.
        for (page in Pages.all) {
            for (region in page.regions) {
                val b = region.bounds
                for (pass in Wax.passes(region)) {
                    val first = pass.points.first()
                    val last = pass.points.last()
                    assertTrue(
                        "${page.id}/${region.id} has a short pass",
                        first.x <= b.x + 1e-9 || last.x <= b.x + 1e-9 ||
                            first.x >= b.right - 1e-9 || last.x >= b.right - 1e-9 ||
                            first.y <= b.y + 1e-9 || last.y <= b.y + 1e-9 ||
                            first.y >= b.bottom - 1e-9 || last.y >= b.bottom - 1e-9,
                    )
                }
            }
        }
    }

    @Test
    fun thePassesCoverTheirOwnAreaAcross() {
        // The gap between neighboring passes, measured across the rubbing
        // direction, has to be smaller than the pass itself, or the area
        // would come out striped.
        for (page in Pages.all) {
            for (region in page.regions) {
                val spacing = Wax.spacing(region)
                val width = Wax.width(region)
                assertTrue(
                    "${page.id}/${region.id} is rubbed in stripes: $spacing apart, $width wide",
                    spacing <= width,
                )
            }
        }
    }

    @Test
    fun aTinyAreaIsRubbedWithATinyPassNotAHouseBrush() {
        // A sprinkle is a sixteenth of the page across. A pass as wide as the
        // sky's would drown it, so the pass narrows with the area it colors.
        val sprinkle = Region(
            id = "sprinkle",
            kind = "sprinkles",
            fillArgb = Crayons.ORANGE,
            parts = listOf(Ell(Vec2(0.5, 0.5), 0.019, 0.008, 12.0)),
        )
        val widest = Wax.passes(sprinkle).maxOf { it.width }
        assertTrue("a sprinkle is rubbed with a $widest wide pass", widest <= 0.02)
    }

    @Test
    fun theWholePictureIsNotRubbedInOneDirection() {
        // One angle for the whole book would look printed by a machine. Each
        // area chooses its own, so the picture reads as a hand that moved
        // around it.
        val angles = Pages.all.flatMap { it.regions }.map { Wax.angleDeg(it) }.toSet()
        assertTrue("the book rubs at only ${angles.size} angles", angles.size >= 4)
    }

    @Test
    fun theWaxIsTheVeryWaxEveryPictureWasDrawnWith() {
        // The material's numbers are held above; this holds the pixels. Every
        // tile the book is made of, each at its own angle and seed, comes out
        // of one hash: the store captures, the launcher art and the pictures
        // on the screen were all made of these tiles, so a change to the
        // generator's arithmetic is a change to the art, and it has to be a
        // decision rather than a side effect of making the walk over the
        // lattice cheaper. It was made cheaper once, and this number did not
        // move.
        var hash = 0xcbf29ce484222325uL
        fun mix(pixels: IntArray) {
            for (p in pixels) {
                hash = (hash xor (p.toULong() and 0xFFFFFFFFuL)) * 0x100000001b3uL
            }
        }
        var areas = 0
        for (page in Pages.all) {
            for (region in page.regions) {
                mix(Wax.surface(region.fillArgb, 96, Wax.angleDeg(region), 0, Wax.seed(region)))
                // And the fine tile a mark is made of, which is the same paper
                // at the scale of a line rather than of an area.
                mix(Wax.surface(region.fillArgb, 96, -24.0, 0, 0x5A17, fine = true))
                areas++
            }
        }
        assertTrue("the book lost its areas", areas > 90)
        assertEquals(
            "the wax tiles are not the ones the pictures were drawn with",
            0xb027c0588f287325uL,
            hash,
        )
    }

    @Test
    fun theSurfaceIsWaxNotAVeil() {
        // The wax surface has to be mostly down and honestly uneven: an
        // average near opaque, with real variation, so a colored area reads
        // as wax pressed into paper rather than as flat paint with speckle.
        //
        // The variation is the half that a high coverage alone cannot buy.
        // Run at a mean of 233 with a standard deviation of 11, a mark is
        // four percent away from flat: that is ink with a faint texture, the
        // mark a sign pen leaves, which is exactly what this app must not
        // draw. So the standard deviation is held here too, at a floor a real
        // crayon clears and a flat fill cannot.
        val pixels = Wax.surface(Crayons.RED, 64, -30.0, 0, 12345)
        assertEquals(64 * 64, pixels.size)
        val alphas = pixels.map { (it ushr 24) and 0xFF }
        val mean = alphas.average()
        val sd = kotlin.math.sqrt(alphas.sumOf { (it - mean) * (it - mean) } / alphas.size)
        assertTrue("the wax is see through: mean alpha $mean", mean > 180.0)
        assertTrue("the wax is a flat fill: mean alpha $mean", mean < 250.0)
        assertTrue("the wax has no tooth at all", alphas.max() - alphas.min() > 40)
        assertTrue(
            "the wax is flat ink with a texture, not wax: mean alpha $mean, sd $sd",
            sd >= 20.0,
        )
        // And it is the crayon's own color, never a gray veil over it.
        for (p in pixels) {
            assertEquals("the wax changed the color", Crayons.RED and 0xFFFFFF, (p and 0xFFFFFF).toLong())
        }
    }

    @Test
    fun aMarkIsMadeOfTheSameWaxAnAreaIs() {
        // A mark is the child's own wax, so it has to carry the same material
        // an area does: the same high mean and the same real variation, not
        // a narrower, flatter surface because it happens to be a line. This
        // is the bug that made a mark read as ink while the sample read as
        // crayon, and it lives on the fine tile a mark is drawn with.
        val fine = Wax.surface(Crayons.BLUE, 64, -24.0, 0, 0x5A17, fine = true)
        val alphas = fine.map { (it ushr 24) and 0xFF }
        val mean = alphas.average()
        val sd = kotlin.math.sqrt(alphas.sumOf { (it - mean) * (it - mean) } / alphas.size)
        assertTrue("the mark is see through: mean alpha $mean", mean > 180.0)
        assertTrue("the mark is flat ink: mean alpha $mean, sd $sd", sd >= 20.0)
    }

    @Test
    fun twoAreasOfOnePictureDoNotShareOneTexture() {
        // A wall and the grass under it were not waxed in step, and neither
        // were the two sides of one roof: each area's surface comes from its
        // own id.
        val patterns = Pages.all
            .flatMap { it.regions }
            .map { Wax.surface(it.fillArgb, 24, Wax.angleDeg(it), 0, Wax.seed(it)).toList() }
        assertTrue("every area in the book shares one surface", patterns.toSet().size > 40)
    }

    @Test
    fun theSurfaceTileHasNoSeamAtAnyAngle() {
        // A tile is repeated forever, so its left column has to continue from
        // its right column and its top row from its bottom row. If they do
        // not, every tile edge is a visible step, and a colored area or a
        // child's mark comes out covered in a grid of faint rectangles: the
        // bug this test exists because of, which no close reading of the code
        // caught and a store capture did.
        val size = 64
        for (angle in listOf(-90.0, -38.0, -24.0, -5.0, 0.0, 2.0, 37.0, 64.0, 90.0, 140.0, 180.0)) {
            for (fine in listOf(false, true)) {
                val pixels = Wax.surface(Crayons.RED, size, angle, 0, 4242, fine = fine)
                fun alpha(x: Int, y: Int) = (pixels[y * size + x] ushr 24) and 0xFF
                // The step across the seam, against the biggest step inside
                // the tile: a seam is a jump the material never makes on its
                // own.
                var seam = 0
                var inside = 0
                for (i in 0 until size) {
                    seam = maxOf(seam, kotlin.math.abs(alpha(0, i) - alpha(size - 1, i)))
                    seam = maxOf(seam, kotlin.math.abs(alpha(i, 0) - alpha(i, size - 1)))
                    inside = maxOf(inside, kotlin.math.abs(alpha(1, i) - alpha(2, i)))
                    inside = maxOf(inside, kotlin.math.abs(alpha(i, 1) - alpha(i, 2)))
                }
                assertTrue(
                    "the wax tile at $angle (fine=$fine) has a seam: $seam across an edge, " +
                        "against a typical step of $inside",
                    seam <= inside * 4 + 12,
                )
            }
        }
    }

    @Test
    fun aMarkIsStretchedAlongTheHandThatMadeIt() {
        // The drag is what makes a mark read as wax rather than as a printed
        // line. Measured on the wax that actually lands on the paper: shifted
        // a little along the direction the hand moved, the material looks
        // much more like itself than it does shifted the same distance across
        // the movement. A tile of round cells would score the same both ways
        // and read as a stain; this is a ratio, so it holds at every size and
        // every color.
        val size = 64
        val step = size / 8
        val report = StringBuilder()
        for (angle in listOf(0.0, -38.0, 37.0, 90.0)) {
            for (fine in listOf(false, true)) {
                val pixels = Wax.surface(Crayons.BLUE, size, angle, 0, 99, fine = fine)
                fun alpha(x: Int, y: Int) = (pixels[wrap(y, size) * size + wrap(x, size)] ushr 24) and 0xFF
                val along = Math.cos(Math.toRadians(angle))
                val across = Math.sin(Math.toRadians(angle))
                var alongDrag = 0.0
                var acrossDrag = 0.0
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val here = alpha(x, y)
                        alongDrag += kotlin.math.abs(
                            here - alpha(
                                (x + (along * step).toInt()),
                                (y + (across * step).toInt()),
                            ),
                        )
                        acrossDrag += kotlin.math.abs(
                            here - alpha(
                                (x + (-across * step).toInt()),
                                (y + (along * step).toInt()),
                            ),
                        )
                    }
                }
                report.append(
                    "\n  angle $angle fine=$fine: ${(alongDrag / acrossDrag * 100).toInt()}% " +
                        "of the across variation, along the drag",
                )
                assertTrue(
                    "the wax at $angle (fine=$fine) is not stretched along the drag:" +
                        " $alongDrag along it, $acrossDrag across it" + report,
                    alongDrag < acrossDrag * (if (fine) 0.85 else 0.95),
                )
            }
        }
    }
}

/** Wraps [v] onto 0 until [n], the way a tile repeats. */
private fun wrap(v: Int, n: Int): Int {
    val m = v % n
    return if (m < 0) m + n else m
}
