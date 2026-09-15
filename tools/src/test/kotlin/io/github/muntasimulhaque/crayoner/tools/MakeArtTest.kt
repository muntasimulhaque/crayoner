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
    fun theBannerDrawsThePageItHalfColors() {
        // The half-finished sailboat is the whole left half of the banner, so
        // the regions it names have to be real regions of a real page, and
        // the finished half has to be some of them and not all of them: a
        // banner showing either a blank sheet or a finished picture is not
        // showing a child mid-coloring.
        val page = Pages.byId("sail") ?: error("the banner's page is gone")
        val started = listOf("sky", "cloud", "cloud_low", "sea")
            .map { id -> page.indexOfRegion(id) }
        assertTrue("the banner names regions the sail page does not have", started.all { it >= 0 })
        assertTrue("the banner colors nothing", started.isNotEmpty())
        assertTrue("the banner colors the whole page", started.size < page.regionCount)
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

    private fun generated(): BufferedImage = MakeArt.featureGraphic(rootDir())

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
}
