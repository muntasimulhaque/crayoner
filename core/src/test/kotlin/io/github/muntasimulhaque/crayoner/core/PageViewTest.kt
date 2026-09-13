package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window a renderer looks at the paper through.
 *
 * There is no pinch in the app, so every window a child can reach is one of
 * these, and every one of them has to be a piece of the sheet: never the
 * desk beside it, never bigger than the paper, and never a different window
 * for the same place.
 */
class PageViewTest {

    @Test
    fun aPageOpensShowingTheWholeSheet() {
        val whole = PageView.Whole
        assertTrue(whole.isWhole)
        assertEquals(1.0, whole.scale, 1e-9)
        assertEquals(0.0, whole.left, 1e-9)
        assertEquals(0.0, whole.top, 1e-9)
        // The inverse pair both ways round: what the finger touches is the
        // page point the renderer draws there.
        val p = Vec2(0.3, 0.7)
        assertEquals(p.x, whole.onPage(whole.inWindow(p)).x, 1e-9)
        assertEquals(p.y, whole.onPage(whole.inWindow(p)).y, 1e-9)
    }

    @Test
    fun theWindowNeverLeavesThePaper() {
        // Every corner, and a spread of focuses in between: whatever the
        // child is looking at, the window is paper and never desk.
        for (i in 0..8) {
            for (j in 0..8) {
                val view = PageView.closeOn(Vec2(i / 8.0, j / 8.0))
                assertTrue("the window left the paper at $i,$j", view.left >= 0.0)
                assertTrue("the window left the paper at $i,$j", view.top >= 0.0)
                assertTrue("the window left the paper at $i,$j", view.left + view.span <= 1.0 + 1e-9)
                assertTrue("the window left the paper at $i,$j", view.top + view.span <= 1.0 + 1e-9)
            }
        }
    }

    @Test
    fun aCloseLookIsTwiceAsBigAndStillAllPaper() {
        val corner = PageView.closeOn(Vec2(0.25, 0.25))
        assertEquals(2.0, corner.scale, 1e-9)
        assertEquals(0.5, corner.span, 1e-9)
        // A corner look pins the window to that corner rather than padding
        // the paper with desk: this is the whole reason the clamp exists.
        assertEquals(0.0, corner.left, 1e-9)
        assertEquals(0.0, corner.top, 1e-9)

        val middle = PageView.closeOn(Vec2(0.5, 0.5))
        assertEquals(0.25, middle.left, 1e-9)
        assertEquals(0.25, middle.top, 1e-9)
        // In the middle, the point the child pressed is the middle of what
        // they are looking at.
        assertEquals(0.5, middle.inWindow(Vec2(0.5, 0.5)).x, 1e-9)
        assertEquals(0.5, middle.inWindow(Vec2(0.5, 0.5)).y, 1e-9)

        // A corner look is put back inside the sheet rather than showing the
        // desk: the point pressed is off center, and that is correct.
        assertEquals(0.5, corner.inWindow(Vec2(0.25, 0.25)).x, 1e-9)
        assertEquals(0.5, corner.inWindow(Vec2(0.25, 0.25)).y, 1e-9)
    }

    @Test
    fun aFocusIsSnappedSoAWindowIsAStableThing() {
        // Two looks at nearly the same place land on exactly the same window,
        // which is what lets the renderer keep the picture it already drew.
        val a = PageView.closeOn(Vec2(0.513, 0.512))
        val b = PageView.closeOn(Vec2(0.505, 0.518))
        assertEquals(a.left, b.left, 1e-9)
        assertEquals(a.top, b.top, 1e-9)
        val snapped = PageView.snap(Vec2(0.513, 0.512))
        assertEquals(0.5, snapped.x, 1e-9)
        assertEquals(0.5, snapped.y, 1e-9)
    }

    @Test
    fun aZoomIsHeldBetweenTheWholePageAndTheClosestLook() {
        assertEquals(PageView.WHOLE_ZOOM, PageView(0.2, Vec2(0.5, 0.5)).scale, 1e-9)
        assertEquals(PageView.MAX_ZOOM, PageView(9.0, Vec2(0.5, 0.5)).scale, 1e-9)
        assertTrue(PageView(0.2, Vec2(0.5, 0.5)).isWhole)
        assertFalse(PageView.closeOn(Vec2(0.5, 0.5)).isWhole)
    }

    @Test
    fun aCloseLookTheChildChoseKeepsTheMiddleOfThePaperInView() {
        val close = PageView(2.0, Vec2(0.5, 0.5))
        assertEquals(0.25, close.left, 1e-9)
        assertEquals(0.25, close.top, 1e-9)
        // The middle of the window is the middle of the page, so the child
        // is looking at the middle of what they were looking at.
        assertEquals(0.5, close.inWindow(Vec2(0.5, 0.5)).x, 1e-9)
        // The window's own corners are its own page corners, and back again.
        assertEquals(0.25, close.onPage(Vec2(0.0, 0.0)).x, 1e-9)
        assertEquals(0.75, close.onPage(Vec2(1.0, 1.0)).x, 1e-9)
        assertEquals(0.0, close.inWindow(Vec2(0.25, 0.25)).x, 1e-9)
        assertEquals(1.0, close.inWindow(Vec2(0.75, 0.75)).x, 1e-9)
    }

    @Test
    fun aWindowWithoutAFocusIsTheMiddleOfThePage() {
        val view = PageView.closeOn(null)
        assertEquals(0.25, view.left, 1e-9)
        assertEquals(0.25, view.top, 1e-9)
    }
}
