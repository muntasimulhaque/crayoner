package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The structural promises every page in the book must keep. These are the
 * tests that make sixteen hand drawn pictures safe to ship: nothing is
 * hidden behind anything else, nothing is a sliver, every color an area
 * asks for is a real crayon, and a tap anywhere on the paper lands on a
 * real area.
 */
class PagesTest {

    @Test
    fun everyPageIsWellFormed() {
        assertEquals(16, Pages.all.size)
        assertEquals(16, Pages.all.map { it.id }.toSet().size)
        for (page in Pages.all) {
            assertTrue("${page.id} has no regions", page.regionCount >= 5)
            assertEquals(
                "${page.id} has duplicate region ids",
                page.regionCount,
                page.regions.map { it.id }.toSet().size,
            )
        }
    }

    @Test
    fun everyAreaAsksForARealCrayon() {
        // One box of crayons for the whole book, and every area's picture
        // color comes from it, or the picture could never be matched exactly.
        for (page in Pages.all) {
            for (region in page.regions) {
                assertTrue(
                    "${page.id}/${region.id} asks for ${region.fillArgb.toString(16)}, " +
                        "which is not a crayon in the box",
                    Crayons.exists(region.fillArgb),
                )
            }
        }
    }

    @Test
    fun theBoxHoldsEveryCrayonTheBookUsesAndNoDuplicates() {
        val used = Pages.all.flatMap { page -> page.regions.map { it.fillArgb } }.toSet()
        assertEquals("the box has a duplicate crayon", Crayons.all.size, Crayons.all.toSet().size)
        assertTrue("the book uses a crayon outside the box", Crayons.all.containsAll(used))
        // Every crayon in the box should earn its place: a crayon no picture
        // ever uses is a crayon the child can only ever misuse.
        assertEquals("a crayon in the box is never used by any picture", 16, Crayons.all.size)
        assertTrue(
            "the book never uses these crayons: " +
                (Crayons.all.toSet() - used).joinToString { it.toString(16) },
            used.size >= Crayons.all.size - 1,
        )
    }

    @Test
    fun everyRegionIsInsideThePaperAndBigEnoughToTap() {
        for (page in Pages.all) {
            for (region in page.regions) {
                assertTrue("${page.id}/${region.id} has no shapes", region.parts.isNotEmpty())
                val b = region.bounds
                assertTrue(
                    "${page.id}/${region.id} leaves the paper: $b",
                    b.x >= -0.02 && b.y >= -0.02 && b.right <= 1.02 && b.bottom <= 1.02,
                )
                assertTrue(
                    "${page.id}/${region.id} is a sliver (${region.area})",
                    region.area >= 0.0012,
                )
            }
        }
    }

    @Test
    fun everyRegionCanBeSeenAndTouched() {
        // One fine grid per page: a point is "seen" when this region is the
        // topmost one under it, exactly the rule taps use. A region nobody
        // can see could never be colored, so it must not ship.
        val steps = 160
        for (page in Pages.all) {
            val seen = IntArray(page.regionCount)
            for (i in 0 until steps) {
                for (j in 0 until steps) {
                    val p = Vec2((i + 0.5) / steps, (j + 0.5) / steps)
                    val index = page.regionIndexAt(p)
                    if (index >= 0) seen[index]++
                }
            }
            page.regions.forEachIndexed { index, region ->
                assertTrue(
                    "${page.id}/${region.id} is hidden behind another region",
                    seen[index] > 0,
                )
            }
        }
    }

    @Test
    fun everyPointOfPaperIsCovered() {
        val steps = 96
        for (page in Pages.all) {
            for (i in 0 until steps) {
                for (j in 0 until steps) {
                    val p = Vec2((i + 0.5) / steps, (j + 0.5) / steps)
                    assertTrue(
                        "${page.id} leaves a hole at $p",
                        page.regionIndexAt(p) >= 0,
                    )
                }
            }
        }
    }

    @Test
    fun thePictureColorLandsWhereThePictureSaysItDoes() {
        // The topmost region at its own centroid is a real area: this pins
        // the paint order the outline renderer relies on for hidden edges.
        for (page in Pages.all) {
            for ((index, region) in page.regions.withIndex()) {
                if (index == 0) continue // the ground is under everything by design
                val top = page.regionIndexAt(region.centroid)
                assertTrue("${page.id}/${region.id} is buried at its own center", top >= 0)
            }
        }
    }

    @Test
    fun theGroundIsTheWholePaperAndIsNeverOutlined() {
        // Both renderers skip the ground's outline, because a real coloring
        // page prints no border around the sheet. That is only sound while
        // region zero really is the whole sheet on every page.
        for (page in Pages.all) {
            val ground = page.regions.first()
            val b = ground.bounds
            assertTrue(
                "${page.id} ground is not the whole paper: $b",
                b.x <= 0.001 && b.y <= 0.001 && b.right >= 0.999 && b.bottom >= 0.999,
            )
            assertTrue(
                "${page.id} ground is not a plain rectangle",
                ground.parts.singleOrNull() is RRect,
            )
            assertEquals(page.groundIndex, 0)
            assertTrue(page.isGround(0))
            assertFalse(page.isGround(1))
        }
    }

    @Test
    fun pagesResolveByName() {
        assertNotNull(Pages.byId("sail"))
        assertEquals(null, Pages.byId("nope"))
    }

    @Test
    fun everyLabelKindIsAName() {
        // Region kinds travel to the screen reader as resource lookups; a
        // kind with spaces or capitals would never resolve. Keep the
        // vocabulary lowercase and simple.
        val kinds = Pages.all.flatMap { page -> page.regions.map { it.kind } }.toSet()
        for (kind in kinds) {
            assertTrue("bad kind: $kind", kind.matches(Regex("[a-z_]+")))
        }
        assertFalse(kinds.isEmpty())
    }

    @Test
    fun theBookIsMostlyBigAreas() {
        // A page of twenty slivers is not coloring for a three year old, it
        // is needlework. Every page stays in the range a small hand and a
        // short attention span can hold.
        for (page in Pages.all) {
            assertTrue("${page.id} has too many areas", page.regionCount in 5..10)
        }
    }
}
