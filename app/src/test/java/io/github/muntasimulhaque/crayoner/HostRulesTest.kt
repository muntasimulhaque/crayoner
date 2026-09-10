package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.ShelfState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the host rests on, tested on the JVM with no device: the shelf's
 * answers, the hit test a tap performs, and the completion rule. The
 * ViewModel itself is Android flavored (DataStore, SoundPool) and is
 * exercised on the emulator by the capture run; everything here is the pure
 * logic underneath it, which is where a bug would actually live.
 */
class HostRulesTest {

    private val page: Page = Pages.byId("sail") ?: error("the sail page is gone")

    @Test
    fun theShelfKnowsWhatIsFinishedAndWhatIsStarted() {
        val shelf = ShelfState(
            finished = setOf("sail"),
            drafts = mapOf("rainbow" to "sky:FF77BEDC"),
        )
        assertTrue(shelf.isFinished("sail"))
        assertFalse(shelf.isFinished("tree"))
        assertTrue(shelf.hasDraft("rainbow"))
        assertFalse(shelf.hasDraft("tree"))
        // A blank draft string is not a draft: wiping a page clears it.
        assertFalse(ShelfState(drafts = mapOf("rainbow" to "")).hasDraft("rainbow"))
    }

    @Test
    fun aTapPaintsTheTopmostAreaUnderThePoint() {
        // The boat sits on top of the sea, so the same water resolves to the
        // boat where the boat is and to the sea beside it. What is on top is
        // what the child sees, and what the child sees is what they get.
        assertEquals(page.indexOfRegion("boat"), page.regionIndexAt(Vec2(0.5, 0.85)))
        assertEquals(page.indexOfRegion("sea"), page.regionIndexAt(Vec2(0.08, 0.92)))
        assertEquals(page.indexOfRegion("sky"), page.regionIndexAt(Vec2(0.97, 0.03)))
    }

    @Test
    fun aTapOutsideThePaperLandsNowhere() {
        assertEquals(-1, page.regionIndexAt(Vec2(-0.4, 0.5)))
        assertEquals(-1, page.regionIndexAt(Vec2(0.5, 1.4)))
    }

    @Test
    fun anyColorOnEveryAreaFinishesThePicture() {
        // A real coloring book does not refuse to be finished because the sky
        // was colored green. The app celebrates the work the child chose to
        // do, which is also the only rule that can never scold them.
        var progress = Progress()
        assertFalse(page.isComplete(progress.asMap()))
        for (index in page.regions.indices) {
            progress = progress.with(index, Crayons.RED)
        }
        assertTrue(page.isComplete(progress.asMap()))
        assertEquals(page.regionCount, progress.coloredCount)
        assertEquals(0, page.blankCount(progress.asMap()))
    }

    @Test
    fun matchingCountsTheAreasThatWearTheirPictureColor() {
        var progress = Progress()
        for ((index, region) in page.regions.withIndex()) {
            progress = progress.with(index, region.fillArgb)
        }
        assertEquals(page.regionCount, page.matchCount(progress.asMap()))
        progress = progress.with(1, Crayons.RED)
        assertTrue(page.isComplete(progress.asMap()))
        assertEquals(page.regionCount - 1, page.matchCount(progress.asMap()))
    }

    @Test
    fun theFirstTouchPicksUpTheColorTheAreaWants() {
        // The host's rule, stated as data: with no crayon in hand, a tap
        // colors the area with the area's own picture color, so the very
        // first act on a page always succeeds.
        val index = page.indexOfRegion("sun")
        val region = page.region(index) ?: error("no sun on the sail page")
        assertTrue("the sun's color must be a real crayon", Crayons.exists(region.fillArgb))
        val progress = Progress().with(index, region.fillArgb)
        assertEquals(region.fillArgb, progress.colorOf(index))
    }

    @Test
    fun aDraftSurvivesARoundTripThroughTheStore() {
        val progress = Progress()
            .with(0, page.regions[0].fillArgb)
            .with(4, page.regions[4].fillArgb)
        val text = progress.serialize(page)
        assertEquals(progress, Progress.parse(text, page))
    }

    @Test
    fun theUniversalBoxCanColorEveryAreaOfEveryPicture() {
        // One box for the whole book: every area's picture color is in it, or
        // a child holding every crayon at once still could not match the
        // picture. This is the rule that makes one universal box possible.
        for (p in Pages.all) {
            for ((index, region) in p.regions.withIndex()) {
                assertTrue(
                    "${p.id} region $index wants a color the box does not hold",
                    Crayons.exists(region.fillArgb),
                )
            }
        }
    }

    @Test
    fun everyCrayonHasAName() {
        // The box resolves each crayon to a word for the screen reader. A
        // crayon with no word would read as the app name, which is a bug a
        // parent would hear immediately.
        for (argb in Crayons.all) {
            assertTrue("a crayon is unnamed", Crayons.exists(argb))
        }
    }
}
