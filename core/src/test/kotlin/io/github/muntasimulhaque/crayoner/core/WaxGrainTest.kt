package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wax grain: the tile that turns a flat fill into a crayon. It has to be
 * deterministic (the store art and the screen must show the same page), it
 * has to be subtle (a loud grain reads as dirt, not wax), and it has to wrap
 * (a tiled shader with a visible seam would draw a grid across every area).
 */
class WaxGrainTest {

    @Test
    fun theGrainIsDeterministic() {
        val a = WaxGrain.pixels()
        val b = WaxGrain.pixels()
        assertTrue("the grain differs between runs", a.contentEquals(b))
        assertEquals(WaxGrain.SIZE * WaxGrain.SIZE, a.size)
    }

    @Test
    fun aDifferentSeedMakesADifferentTile() {
        val a = WaxGrain.pixels(1)
        val b = WaxGrain.pixels(2)
        assertTrue("two seeds made the same tile", !a.contentEquals(b))
    }

    @Test
    fun theGrainIsAWhisperNotAStain() {
        // Every pixel is either a faint dark speck or a fainter light one,
        // and the average alpha is low enough that a colored area still
        // reads as one calm color at arm's length.
        val pixels = WaxGrain.pixels()
        var totalAlpha = 0L
        for (p in pixels) {
            val alpha = (p ushr 24) and 0xFF
            assertTrue("a speck is too strong: $alpha", alpha <= 32)
            totalAlpha += alpha
        }
        val average = totalAlpha.toDouble() / pixels.size
        assertTrue("the grain is too strong on average: $average", average < 6.0)
        assertTrue("the grain is invisible: $average", average > 0.5)
    }

    @Test
    fun theTileWrapsWithoutASeam() {
        // The value noise is built on a lattice that wraps, so the first and
        // last columns of the tile are neighbors rather than strangers. A
        // seam here would print a faint grid over every colored area.
        val pixels = WaxGrain.pixels()
        var edgeDifference = 0L
        var innerDifference = 0L
        for (y in 0 until WaxGrain.SIZE) {
            val left = (pixels[y * WaxGrain.SIZE] ushr 24) and 0xFF
            val right = (pixels[y * WaxGrain.SIZE + WaxGrain.SIZE - 1] ushr 24) and 0xFF
            val inner = (pixels[y * WaxGrain.SIZE + WaxGrain.SIZE / 2] ushr 24) and 0xFF
            edgeDifference += kotlin.math.abs(left - right)
            innerDifference += kotlin.math.abs(left - inner)
        }
        assertTrue(
            "the tile has a seam at its edge: $edgeDifference vs $innerDifference",
            edgeDifference <= innerDifference,
        )
    }
}
