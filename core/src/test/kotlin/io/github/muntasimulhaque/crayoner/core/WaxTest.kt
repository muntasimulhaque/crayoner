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
    fun theSurfaceIsWaxNotAVeil() {
        // The wax surface has to be mostly down and honestly uneven: an
        // average near opaque, with real variation, so a colored area reads
        // as wax pressed into paper rather than as flat paint with speckle.
        val pixels = Wax.surface(Crayons.RED, 64, -30.0, 0, 12345)
        assertEquals(64 * 64, pixels.size)
        val alphas = pixels.map { (it ushr 24) and 0xFF }
        val mean = alphas.average()
        assertTrue("the wax is see through: mean alpha $mean", mean > 180.0)
        assertTrue("the wax is a flat fill: mean alpha $mean", mean < 250.0)
        assertTrue("the wax has no tooth at all", alphas.max() - alphas.min() > 40)
        // And it is the crayon's own color, never a gray veil over it.
        for (p in pixels) {
            assertEquals("the wax changed the color", Crayons.RED and 0xFFFFFF, (p and 0xFFFFFF).toLong())
        }
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
}
