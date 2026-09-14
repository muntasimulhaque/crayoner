package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a finger lands on the paper.
 *
 * The sheet a child colors on is taller than it is wide, which makes this the
 * one conversion in the app that a reasonable person gets wrong: the frame on
 * screen is 1.2 times taller than it is wide, so it is tempting to divide a
 * finger's y by the frame's height. Page units are isotropic, one pixel scale
 * for both axes, so the number to divide by is the sheet's *width* on both
 * axes. Dividing y by the height landed every mark a fifth of the sheet above
 * the fingertip, which is the bug this file exists to refuse forever.
 */
class PagePointTest {

    /** A phone sheet, 1080 pixels across and 1296 tall. */
    private val width = 1080.0

    @Test
    fun theCornersOfTheFrameAreTheCornersOfThePaper() {
        val topLeft = pagePointOf(0.0, 0.0, width)
        assertEquals(0.0, topLeft.x, 1e-9)
        assertEquals(0.0, topLeft.y, 1e-9)
        val bottomRight = pagePointOf(width, width * Page.ASPECT, width)
        assertEquals(1.0, bottomRight.x, 1e-9)
        assertEquals(Page.ASPECT, bottomRight.y, 1e-9)
    }

    @Test
    fun aPointInTheMiddleOfTheFrameIsHalfwayDownThePaper() {
        // The one assertion that catches the bug: the middle of the sheet is
        // the middle of the sheet, whichever number anyone is tempted to
        // divide by.
        val middle = pagePointOf(width / 2.0, width * Page.ASPECT / 2.0, width)
        assertEquals(0.5, middle.x, 1e-9)
        assertEquals(Page.ASPECT / 2.0, middle.y, 1e-9)
    }

    @Test
    fun aFingerAtAPixelLandsOnThatPixelOfThePaper() {
        // Walk the frame and check the mark's own position: the point of the
        // paper under a finger is the finger's own place, at every scale.
        for (scale in listOf(0.25, 0.5, 1.0, 2.0, 3.0)) {
            val across = width * scale
            for (i in 0..10) {
                for (j in 0..10) {
                    val xPx = across * i / 10.0
                    val yPx = across * Page.ASPECT * j / 10.0
                    val p = pagePointOf(xPx, yPx, across)
                    assertEquals(p.toString(), i / 10.0, p.x, 1e-9)
                    assertEquals(p.toString(), Page.ASPECT * j / 10.0, p.y, 1e-9)
                }
            }
        }
    }

    @Test
    fun aFingerThatSlidesOffThePaperStaysOnIt() {
        // A hand coloring rests on the edge of the page, and a pointer can
        // travel a hair past it. Nothing off the paper is ever a mark's
        // position, so a hand at the edge of the sheet colors the edge of the
        // sheet rather than off it.
        val left = pagePointOf(-40.0, 100.0, width)
        assertEquals(0.0, left.x, 1e-9)
        val right = pagePointOf(width + 40.0, 100.0, width)
        assertEquals(1.0, right.x, 1e-9)
        val bottom = pagePointOf(100.0, width * Page.ASPECT + 40.0, width)
        assertEquals(Page.ASPECT, bottom.y, 1e-9)
        val top = pagePointOf(100.0, -40.0, width)
        assertEquals(0.0, top.y, 1e-9)
    }

    @Test
    fun aFingerOnThePaperAlwaysLandsOnAPrintedArea() {
        // The first touch of a fresh page picks up the color of the area
        // under it, which only works if a point of the paper always resolves
        // to an area. The ground covers the sheet, so every point does.
        val page = Pages.byId("sail") ?: error("the sail page is gone")
        for (i in 0..24) {
            for (j in 0..24) {
                val p = pagePointOf(
                    width * i / 24.0,
                    width * Page.ASPECT * j / 24.0,
                    width,
                )
                assertTrue("$p fell off the paper", page.regionIndexAt(p) >= 0)
            }
        }
    }

    @Test
    fun theScaleIsTheSameOnBothAxes() {
        // Isotropic: a square of the frame is a square of the paper, which is
        // what keeps a circle a circle and a mark as wide whichever way the
        // hand dragged it.
        val a = pagePointOf(100.0, 100.0, width)
        val b = pagePointOf(300.0, 500.0, width)
        assertEquals(200.0 / width, b.x - a.x, 1e-9)
        assertEquals(400.0 / width, b.y - a.y, 1e-9)
    }
}
