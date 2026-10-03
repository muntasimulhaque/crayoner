package io.github.muntasimulhaque.crayoner.tools

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Pages
import java.awt.image.BufferedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The store art is drawn from the same pictures the app plays, so it can go
 * stale in a way no compiler catches: a page renamed in `:core` leaves the
 * banner pointing at a region that no longer exists, and the half-colored
 * sailboat quietly comes out fully colored or fully blank. That is the bug
 * this holds shut.
 *
 * The art is a generator, not a hand-edited asset, so what is checked here is
 * the generator's own inputs, and the drawing itself is checked by rendering
 * it: a banner that came out blank would pass every name check ever written.
 */
class MakeArtTest {

    @Test
    fun theBannerShowsThePageBeforeAndAfter() {
        // The banner is the same page twice: as the book prints it, and
        // finished. The page has to be the real one, and the finished sheet
        // has to carry every one of its areas in its own color, or the
        // banner is a blank sheet beside a blank sheet. The as-printed sheet
        // cannot contribute any of those colors (it is paper and ink only),
        // so every colored area counted here is one the finished sheet really
        // drew.
        val artwork = generated()
        val page = Pages.byId("sail") ?: error("the banner's page is gone")
        assertTrue("the banner's page lost its areas", page.regionCount >= 5)
        for (region in page.regions) {
            val fill = region.fillArgb.toInt() and 0xFFFFFF
            val n = count(artwork) { isWaxFamily(it and 0xFFFFFF, fill) }
            assertTrue(
                "the finished sheet is missing ${region.id}, or it is not " +
                    Integer.toHexString(fill),
                n > 40,
            )
        }
    }

    @Test
    fun theFeatureGraphicIsTheSizePlayAsksFor() {
        // A feature graphic is 1024 by 500 exactly, always, because Play
        // rejects anything else: the size is a fact about the store, not a
        // preference of this project.
        val artwork = generated()
        assertEquals(1024, artwork.width)
        assertEquals(500, artwork.height)
    }

    @Test
    fun theFeatureGraphicIsNotBlank() {
        // Every corner of a real banner is the brand coral, and the plate and
        // the words sit on top of it. This is the check that would have
        // caught a page that stopped rendering: a banner with the plate
        // missing is coral everywhere, which passes every other test here.
        val artwork = generated()
        val coral = Crayons.RED.toInt()
        val plate = count(artwork) { it == 0xFFFFFDF8.toInt() }
        assertTrue("the banner's plate is gone", plate > 10_000)
        val nearCoral = count(artwork) { closeTo(it, coral, 30) }
        assertTrue("the banner has no brand ground", nearCoral > 100_000)
    }

    @Test
    fun everyMarkOnTheBannerSurvivesTheCrop() {
        // The store shows this file in a card, and the card is a smaller box
        // than the file: the listing is sixteen by nine, so about 68 pixels
        // at each end of a 1024 by 500 banner are cut away. The left sheet
        // and the tip of the crayon used to stand in that band and came out
        // truncated on the live listing. Nothing drawn may hang over the crop
        // now, and the shadow counts: it is ink too, and it used to be what
        // reached the edge first.
        val artwork = generated()
        val ink = inkBox(artwork)
        assertTrue(
            "the banner's ink runs to $ink and ${MakeArt.SAFE} does not hold it",
            MakeArt.SAFE.encloses(ink),
        )
    }

    private fun generated(): BufferedImage = MakeArt.featureGraphic(rootDir())

    /**
     * The box the banner's ink really takes up: every pixel that is not the
     * brand ground the file fills first. The ground is one flat fill on
     * whole pixels, so anything else in the image is something this file
     * drew, down to the last faint step of a sheet's shadow.
     */
    private fun inkBox(image: BufferedImage): Window {
        val ground = image.getRGB(0, 0)
        var left = image.width
        var right = -1
        var top = image.height
        var bottom = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) == ground) continue
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        return Window(left, top, right - left + 1, bottom - top + 1)
    }

    /** The repository root, found from the working directory Gradle runs in. */
    private fun rootDir(): java.io.File {
        var dir: java.io.File? = java.io.File(".").absoluteFile
        while (dir != null && !java.io.File(dir, "core/src/main/kotlin").isDirectory) {
            dir = dir.parentFile
        }
        return dir ?: error("no repository root above ${java.io.File(".").absolutePath}")
    }

    private fun count(image: BufferedImage, test: (Int) -> Boolean): Int {
        var n = 0
        for (y in 0 until image.height) for (x in 0 until image.width) if (test(image.getRGB(x, y))) n++
        return n
    }

    private fun closeTo(argb: Int, other: Int, tolerance: Int): Boolean {
        for (shift in intArrayOf(16, 8, 0)) {
            val a = (argb ushr shift) and 0xFF
            val b = (other ushr shift) and 0xFF
            if (kotlin.math.abs(a - b) > tolerance) return false
        }
        return true
    }

    /**
     * True when [pixel] is [fill] as wax rather than as flat color: the same
     * hue, taken a little deeper on the paper's tooth, never brighter and
     * never a different color. The wax's own relief shades every pixel by
     * one factor, so the check is on the ratios rather than the values.
     */
    private fun isWaxFamily(pixel: Int, fill: Int): Boolean {
        var low = 1.0
        var high = 0.0
        for (shift in intArrayOf(16, 8, 0)) {
            val p = (pixel ushr shift) and 0xFF
            val f = (fill ushr shift) and 0xFF
            // A channel the fill itself barely has cannot be read as a ratio.
            if (f < 32) {
                if (p > f + 10) return false
                continue
            }
            val k = p.toDouble() / f
            if (k > 1.001 || k < 0.70) return false
            low = minOf(low, k)
            high = maxOf(high, k)
        }
        return high - low <= 0.12
    }
}
