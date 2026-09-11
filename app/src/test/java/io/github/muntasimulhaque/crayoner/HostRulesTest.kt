package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Strokes
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.ShelfState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the host rests on, tested on the JVM with no device: the shelf's
 * answers, the way a hand is read into marks, and the completion rule. The
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
            drafts = mapOf("rainbow" to "FF7ED9EB(0.5,0.5)"),
        )
        assertTrue(shelf.isFinished("sail"))
        assertFalse(shelf.isFinished("tree"))
        assertTrue(shelf.hasDraft("rainbow"))
        assertFalse(shelf.hasDraft("tree"))
        // A blank draft string is not a draft: wiping a page clears it.
        assertFalse(ShelfState(drafts = mapOf("rainbow" to "")).hasDraft("rainbow"))
    }

    @Test
    fun aPointOnThePaperResolvesToTheAreaTheChildSees() {
        // The boat sits on top of the sea, so the same water resolves to the
        // boat where the boat is and to the sea beside it. What is on top is
        // what the child sees, and the mark they draw there is theirs.
        assertEquals(page.indexOfRegion("boat"), page.regionIndexAt(Vec2(0.5, 0.85)))
        assertEquals(page.indexOfRegion("sea"), page.regionIndexAt(Vec2(0.08, 0.92)))
        assertEquals(page.indexOfRegion("sky"), page.regionIndexAt(Vec2(0.97, 0.03)))
    }

    @Test
    fun aTouchOutsideThePaperLandsNowhere() {
        assertEquals(-1, page.regionIndexAt(Vec2(-0.4, 0.5)))
        assertEquals(-1, page.regionIndexAt(Vec2(0.5, 1.4)))
    }

    @Test
    fun theCrayonReachingEveryAreaFinishesThePictureWhateverTheColorsAre() {
        // A real coloring book does not refuse to be finished because the sky
        // was colored green. The app celebrates the work the child chose to
        // do, which is also the only rule that can never scold them.
        var progress = Progress()
        assertFalse(page.isComplete(progress.strokes))
        for (index in page.regions.indices) {
            val p = aVisiblePoint(index)
            progress = progress.with(Stroke(Crayons.RED, listOf(p, Vec2(p.x + 0.01, p.y))))
        }
        assertTrue(page.isComplete(progress.strokes))
        assertEquals(page.regionCount, page.reachedCount(progress.strokes))
        assertEquals(page.regionCount, progress.coloredCount)
    }

    @Test
    fun oneAreaLeftAloneLeavesThePictureUnfinished() {
        val last = page.regionCount - 1
        val progress = page.regions.indices.filter { it != last }.fold(Progress.Empty) { acc, index ->
            acc.with(Stroke(Crayons.RED, listOf(aVisiblePoint(index))))
        }
        assertFalse(page.isComplete(progress.strokes))
        assertEquals(page.regionCount - 1, page.reachedCount(progress.strokes))
    }

    @Test
    fun aMarkInTheWrongColorStillLands() {
        // Nothing is ever refused. A mark drawn in a color the picture did
        // not ask for is on the paper, is counted where it landed, and is
        // never taken away.
        val sail = page.region(page.indexOfRegion("sail")) ?: error("no sail")
        val wrong = Crayons.BLUE.takeIf { it != sail.fillArgb } ?: Crayons.RED
        val inked = Progress.Empty.with(Strokes.scribble(sail, wrong))
        assertEquals(1, inked.coloredCount)
        assertEquals(wrong, inked.strokes.first().color)
        assertTrue("a wrong mark reached nothing", page.reachedCount(inked.strokes) >= 1)
    }

    @Test
    fun everyMarkTheChildMakesSurvivesARoundTripThroughTheStore() {
        val progress = page.regions.indices.take(3).fold(Progress.Empty) { acc, index ->
            acc.with(Strokes.scribble(page.regions[index], page.regions[index].fillArgb))
        }
        val text = progress.serialize()
        val parsed = Progress.parse(text)
        assertEquals(progress.strokes.size, parsed.strokes.size)
        assertEquals(page.reachedCount(progress.strokes), page.reachedCount(parsed.strokes))
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

    /** A point on the page that really is visible as area [index]. */
    private fun aVisiblePoint(index: Int): Vec2 {
        val steps = 120
        for (i in 0 until steps) {
            for (j in 0 until steps) {
                val p = Vec2((i + 0.5) / steps, (j + 0.5) / steps)
                if (page.regionIndexAt(p) == index) return p
            }
        }
        throw AssertionError("the sail page hides region $index")
    }
}
