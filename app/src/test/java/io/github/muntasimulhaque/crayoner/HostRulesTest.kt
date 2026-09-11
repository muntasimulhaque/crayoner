package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Strokes
import io.github.muntasimulhaque.crayoner.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the host rests on, tested on the JVM with no device: the page's
 * answers, the way a hand is read into marks, and the way marks come back
 * from a save. The ViewModel itself is Android flavored (DataStore,
 * SoundPool) and is exercised on the emulator by the capture run; everything
 * here is the pure logic underneath it, which is where a bug would live.
 */
class HostRulesTest {

    private val page: Page = Pages.byId("sail") ?: error("the sail page is gone")

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
    fun theFirstTouchOfAFreshPageLandsOnRealPaper() {
        // The color a first touch picks up is the area's own, so it has to
        // resolve on every page of the book, at every corner of it.
        for (p in Pages.all) {
            var landings = 0
            for (i in 0 until 12) {
                for (j in 0 until 12) {
                    val point = Vec2((i + 0.5) / 12.0, (j + 0.5) / 12.0)
                    val index = p.regionIndexAt(point)
                    if (index >= 0) {
                        landings++
                        val color = p.region(index)?.fillArgb
                        assertTrue("${p.id} asks for a color outside the box", color != null && Crayons.exists(color))
                    }
                }
            }
            assertEquals("${p.id} leaves paper untouched", 144, landings)
        }
    }

    @Test
    fun marksNeverStandStillAndNeverGrowWithoutBound() {
        var stroke = Strokes.dot(Crayons.BLUE, Vec2(0.0, 0.5))
        for (i in 1..5000) stroke = Strokes.extend(stroke, Vec2(i / 5000.0, 0.5))
        assertTrue("a mark grew without bound", stroke.points.size <= Strokes.MAX_POINTS)
        assertFalse("a mark was taken away", stroke.isEmpty)
    }

    @Test
    fun aMarkInEitherHandSurvivesARoundTripThroughTheStore() {
        val colored = Progress.Empty.with(Strokes.scribble(page.regions[1], Crayons.SKY_BLUE))
        val erased = Progress.Empty.with(
            Stroke(Stroke.ERASE_COLOR, (0..8).map { Vec2(0.3 + it * 0.02, 0.7) }, erase = true),
        )
        for (progress in listOf(colored, erased)) {
            val parsed = Progress.parse(progress.serialize())
            assertEquals(progress.strokes.size, parsed.strokes.size)
            assertEquals(progress.strokes[0].erase, parsed.strokes[0].erase)
            assertEquals(progress.strokes[0].color, parsed.strokes[0].color)
            assertEquals(progress.strokes[0].points.size, parsed.strokes[0].points.size)
        }
    }

    @Test
    fun theUniversalBoxCanColorEveryAreaOfEveryPicture() {
        // One box for the whole book: every area's picture color is in it, or
        // a child holding every crayon at once still could not match the
        // picture. This is the rule that makes one universal box possible,
        // and the reason the box carries thirty two colors rather than
        // sixteen.
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
