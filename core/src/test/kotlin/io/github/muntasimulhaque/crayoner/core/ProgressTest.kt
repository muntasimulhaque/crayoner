package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saved colors: round trips, damaged saves, and the completion rule. */
class ProgressTest {

    private val page = Pages.byId("sail") ?: error("the sail page is gone")

    @Test
    fun colorsRoundTripByRegionId() {
        val progress = Progress()
            .with(0, page.regions[0].fillArgb)
            .with(3, page.regions[3].fillArgb)
        val text = progress.serialize(page)
        assertEquals(progress, Progress.parse(text, page))
    }

    @Test
    fun aSaveWithAStrayLineLosesOnlyThatLine() {
        val good = page.regions[4]
        val text = "sky:${page.regions[0].fillArgb.toString(16)};nonsense:123;sky:;:bad;" +
            "${good.id}:${good.fillArgb.toString(16)}"
        val parsed = Progress.parse(text, page)
        assertEquals(page.regions[0].fillArgb, parsed.colorOf(0))
        assertEquals(good.fillArgb, parsed.colorOf(4))
        assertEquals(2, parsed.coloredCount)
    }

    @Test
    fun aColorThatIsNotInTheBoxIsDropped() {
        // The crayon box is the only source of colors, so a save carrying
        // anything else is corrupt and is skipped rather than painted.
        assertTrue(Progress.parse("sky:FF00FF00", page).fills.isEmpty())
    }

    @Test
    fun damagedTextNeverThrows() {
        for (text in listOf(null, "", "  ", ":", ";;", "a:b:c", "1:2", "sky:zz", "🖍")) {
            Progress.parse(text, page)
        }
    }

    @Test
    fun everyAreaColoredFinishesThePictureWhateverTheColorsAre() {
        // A real coloring book does not refuse to be finished because the
        // sky was colored green. Any color on every area finishes it, and
        // the app celebrates the work either way.
        val anyColor = Crayons.RED
        var progress = Progress()
        assertFalse(page.isComplete(progress.asMap()))
        assertEquals(0, page.matchCount(progress.asMap()))
        for (index in page.regions.indices) {
            progress = progress.with(index, anyColor)
        }
        assertTrue(page.isComplete(progress.asMap()))
        assertEquals(0, page.blankCount(progress.asMap()))
        assertEquals(page.regionCount, progress.coloredCount)
    }

    @Test
    fun matchingCountsEveryAreaThatWearsItsPictureColor() {
        var progress = Progress()
        for ((index, region) in page.regions.withIndex()) {
            progress = progress.with(index, region.fillArgb)
        }
        assertEquals(page.regionCount, page.matchCount(progress.asMap()))
        assertEquals(0, page.blankCount(progress.asMap()))

        val wrong = Crayons.RED.takeIf { it != page.regions[1].fillArgb }
            ?: Crayons.BLUE
        progress = progress.with(1, wrong)
        assertEquals(page.regionCount - 1, page.matchCount(progress.asMap()))
        assertTrue("one mismatched area still leaves the picture finished", page.isComplete(progress.asMap()))
    }

    @Test
    fun clearedDropsEverything() {
        val progress = Progress().with(1, page.regions[1].fillArgb).cleared()
        assertTrue(progress.fills.isEmpty())
        assertEquals(0, progress.coloredCount)
    }

    @Test
    fun anEmptySaveReadsAsBlankPaper() {
        assertEquals(Progress.Empty, Progress.parse("", page))
        assertNull(Progress.Empty.colorOf(0))
    }
}
