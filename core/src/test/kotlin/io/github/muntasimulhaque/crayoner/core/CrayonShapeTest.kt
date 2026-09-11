package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's one crayon. It is drawn by the tray on the device and by the
 * launcher icon generator, so its proportions are not a taste: they are the
 * thing that makes it read as a crayon and not as a pencil, a marker or a
 * bullet, and every one of them is pinned here.
 */
class CrayonShapeTest {

    private val outline = CrayonShape.outline()

    @Test
    fun theShapeFillsItsOwnUnitBoxExactly() {
        // One thickness across, the whole crayon along, tip at the top. A
        // caller scales by the thickness and both axes land, so no crayon
        // can come out fatter or thinner than a crayon.
        val minX = outline.minOf { it.x }
        val maxX = outline.maxOf { it.x }
        val minY = outline.minOf { it.y }
        val maxY = outline.maxOf { it.y }
        assertEquals(0.0, minX, 1e-9)
        assertEquals(1.0, maxX, 1e-9)
        assertEquals(0.0, minY, 1e-9)
        assertEquals(CrayonShape.LENGTH, maxY, 1e-9)
    }

    @Test
    fun theBodyIsAsLongAsACrayonStub() {
        // Three and a bit times its own thickness. Wider is a bullet,
        // longer is a pencil, and both are wrong.
        val ratio = CrayonShape.LENGTH / 1.0
        assertTrue("the crayon is $ratio thick-lengths long", ratio in 2.8..3.6)
    }

    @Test
    fun theTipIsAShortBluntConeNotAPoint() {
        // The cone is a little longer than the crayon is thick, which is what
        // a real crayon's cone is. Its nose is a rounded face about a quarter
        // of the body's width: a point is a pencil.
        assertTrue("the cone is too long", CrayonShape.TIP_LENGTH < 1.4)
        assertTrue("the cone is too short", CrayonShape.TIP_LENGTH > 0.85)
        val noseWidth = CrayonShape.NOSE * 2.0
        assertTrue("the nose is $noseWidth of the body", noseWidth in 0.15..0.4)
        // Nothing above the nose: no spike, no needle, and the tip is the
        // topmost thing on the crayon.
        assertTrue("the crayon comes to a point", outline.all { it.y >= -1e-9 })
    }

    @Test
    fun theConeIsAboveTheWrapperAndTheWrapperAboveTheBase() {
        assertTrue(CrayonShape.TIP_LENGTH < CrayonShape.WRAPPER_TOP)
        assertTrue(CrayonShape.WRAPPER_TOP < CrayonShape.WRAPPER_BOTTOM)
        assertTrue(CrayonShape.WRAPPER_BOTTOM < CrayonShape.BASE)
        assertTrue(CrayonShape.BASE < CrayonShape.LENGTH)
    }

    @Test
    fun theBodyIsSquaredAtItsBase() {
        // A crayon's base is flat, with only a hint of softness at the
        // corners, which is one of the things that says crayon and not
        // marker.
        val bottom = outline.filter { it.y > CrayonShape.LENGTH - 1e-9 }
        assertEquals("the base is not flat", 2, bottom.size)
        assertTrue("the base is a point", bottom[0].x - bottom[1].x > 0.8)
    }

    @Test
    fun theInsideIsInsideAndTheOutsideIsNot() {
        // Straight down the crayon's own middle, and out to either side of
        // it, which is what the renderers' fill and the icon's silhouette
        // both rely on.
        assertTrue(CrayonShape.contains(Vec2(0.5, CrayonShape.LENGTH * 0.6)))
        assertTrue(CrayonShape.contains(Vec2(0.25, CrayonShape.LENGTH * 0.6)))
        assertFalse(CrayonShape.contains(Vec2(0.5, CrayonShape.LENGTH + 0.05)))
        assertFalse(CrayonShape.contains(Vec2(-0.2, CrayonShape.LENGTH * 0.6)))
        assertFalse(CrayonShape.contains(Vec2(1.2, CrayonShape.LENGTH * 0.6)))
    }

    @Test
    fun theSilhouetteIsAStableRing() {
        // The outline is walked in order by both renderers, so it must close
        // on itself and hold no repeated point that would draw a hairline
        // across the crayon.
        assertTrue("the outline is too coarse", outline.size >= 8)
        assertEquals("the outline is open", outline.first().x, outline.last().x, 1e-9)
    }

    @Test
    fun theWrapperCoversTheBodyAndOnlyTheBody() {
        val band = CrayonShape.wrapperBand()
        assertEquals(0.0, band.x, 1e-9)
        assertEquals(1.0, band.right, 1e-9)
        assertTrue(band.y >= CrayonShape.TIP_LENGTH)
        assertTrue(band.bottom <= CrayonShape.BASE + 0.2)
    }

    @Test
    fun theCollarOverhangsTheBodyOnBothSides() {
        // A hand banded around a crayon is wider than the crayon: the collar
        // is what says held, so it must show on both sides.
        val collar = CrayonShape.collarBand()
        assertTrue(collar.x < 0.0)
        assertTrue(collar.right > 1.0)
        assertTrue(collar.y > CrayonShape.TIP_LENGTH)
        assertTrue(collar.bottom < CrayonShape.BASE)
    }
}
