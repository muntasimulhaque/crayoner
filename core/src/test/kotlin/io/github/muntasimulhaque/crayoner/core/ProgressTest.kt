package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The marks a child's hand leaves: how they are read, how they are saved,
 * and what it takes for the app to call a picture finished. A coloring page
 * is finished when the crayon has been everywhere on it, whatever colors
 * were chosen along the way.
 */
class ProgressTest {

    private val page = Pages.byId("sail") ?: error("the sail page is gone")

    /** A straight stroke across one area, with points close enough to keep. */
    private fun strokeOver(regionId: String, color: Long): Stroke {
        val index = page.indexOfRegion(regionId)
        val region = page.region(index) ?: error("no $regionId on the sail page")
        val c = region.centroid
        return Stroke(color, (0..6).map { Vec2(c.x - 0.03 + it * 0.01, c.y) })
    }

    @Test
    fun marksRoundTripThroughASave() {
        val progress = Progress()
            .with(strokeOver("sea", Crayons.BLUE))
            .with(strokeOver("sail", Crayons.WHITE))
        val text = progress.serialize()
        val parsed = Progress.parse(text)
        assertEquals(progress.strokes.size, parsed.strokes.size)
        assertEquals(progress.strokes[0].color, parsed.strokes[0].color)
        assertEquals(progress.strokes[0].points.size, parsed.strokes[0].points.size)
        // Points come back within a thousandth of a page unit: a tenth of a
        // pixel on a big tablet, which is not a difference anyone can see.
        for (i in progress.strokes[0].points.indices) {
            assertEquals(progress.strokes[0].points[i].x, parsed.strokes[0].points[i].x, 0.001)
            assertEquals(progress.strokes[0].points[i].y, parsed.strokes[0].points[i].y, 0.001)
        }
    }

    @Test
    fun aSaveWithAStrayLineLosesOnlyThatLine() {
        val good = strokeOver("sea", Crayons.BLUE)
        val hex = Crayons.BLUE.toString(16).uppercase()
        val other = Crayons.RED.toString(16).uppercase()
        val text = "garbage|FF0000FF(broken|$hex(0.5,0.9;0.52,0.9)" +
            "|$other(0.2,0.2)|$hex(9.0,9.0;-2.0,0.5)"
        val parsed = Progress.parse(text)
        assertEquals(2, parsed.strokes.size)
        assertEquals(Crayons.BLUE, parsed.strokes[0].color)
        assertEquals(2, parsed.strokes[0].points.size)
    }

    @Test
    fun aColorThatIsNotInTheBoxIsDropped() {
        // The crayon box is the only source of colors, so a save carrying
        // anything else is corrupt and is skipped rather than painted.
        assertTrue(Progress.parse("FF00FF00(0.5,0.5)").strokes.isEmpty())
    }

    @Test
    fun damagedTextNeverThrows() {
        for (text in listOf(
            null, "", "  ", "(", ")", "|", ";;", "a:b:c", "1:2",
            "FFEF2D57()", "FFEF2D57(,)", "FFEF2D57(0.5)", "🖍(0.5,0.5)",
            "FFEF2D57(" + "0.5,0.5;".repeat(5000) + ")",
        )) {
            Progress.parse(text)
        }
    }

    @Test
    fun aPointOffThePaperIsDropped() {
        val parsed = Progress.parse("FFEF2D57(0.5,0.5;-0.2,0.5;0.5,1.4)")
        assertEquals(1, parsed.strokes.size)
        assertEquals(1, parsed.strokes[0].points.size)
    }

    @Test
    fun theHandIsReadIntoShortMarksNotEveryPixel() {
        // A thousand finger events along a short stretch of paper make one
        // short mark, not a thousand points: a point is kept only when the
        // mark would really grow longer. This is what keeps a long scribble
        // from growing a save of a hundred thousand numbers.
        var stroke = Strokes.dot(Crayons.RED, Vec2(0.5, 0.5))
        val events = 1000
        val span = 0.05
        for (i in 1..events) {
            stroke = Strokes.extend(stroke, Vec2(0.5 + span * i / events, 0.5))
        }
        val most = (span / Strokes.MIN_STEP).toInt() + 2
        assertTrue("kept ${stroke.points.size} points for $events events", stroke.points.size <= most)
        assertTrue("kept too few points: ${stroke.points.size}", stroke.points.size >= most - 2)
    }

    @Test
    fun aStrokeNeverGrowsWithoutBound() {
        var stroke = Strokes.dot(Crayons.RED, Vec2(0.0, 0.5))
        for (i in 1..5000) {
            stroke = Strokes.extend(stroke, Vec2(i / 5000.0, 0.5))
        }
        assertTrue(stroke.points.size <= Strokes.MAX_POINTS)
        // A full stroke is never emptied: the child's mark stays where it is.
        assertFalse(stroke.isEmpty)
    }

    @Test
    fun aScribbleCoversItsOwnAreaAndStaysInsideIt() {
        // What a screen reader's action puts on the page: the area, in the
        // crayon in hand, and every point of it really inside the area.
        for (p in Pages.all) {
            for (region in p.regions) {
                val scribble = Strokes.scribble(region, Crayons.RED)
                assertFalse("${p.id}/${region.id} scribbles nothing", scribble.isEmpty)
                assertTrue(
                    "${p.id}/${region.id} scribbles outside itself",
                    scribble.points.all { region.contains(it) },
                )
                assertTrue(
                    "${p.id}/${region.id} scribbles too little",
                    scribble.length() > 0.02,
                )
                assertTrue(scribble.points.size <= Strokes.MAX_POINTS)
            }
        }
    }

    @Test
    fun theCrayonHasBeenEverywhereFinishesThePictureWhateverTheColorsAre() {
        // A real coloring book does not refuse to be finished because the
        // sky was colored green. Marks everywhere on the paper finish it,
        // and the app celebrates the work either way.
        var progress = Progress()
        assertFalse(page.isComplete(progress.strokes))
        for (index in page.regions.indices) {
            val p = visiblePoint(page, index)
            progress = progress.with(Stroke(Crayons.RED, listOf(p, Vec2(p.x + 0.01, p.y))))
            assertEquals(index + 1, page.reachedCount(progress.strokes))
        }
        assertTrue(page.isComplete(progress.strokes))
        assertEquals(page.regionCount, progress.coloredCount)
    }

    @Test
    fun oneAreaLeftAloneLeavesThePictureUnfinished() {
        val last = page.regionCount - 1
        val progress = page.regions.indices.filter { it != last }.fold(Progress.Empty) { acc, index ->
            acc.with(Stroke(Crayons.RED, listOf(visiblePoint(page, index))))
        }
        assertFalse(page.isComplete(progress.strokes))
        assertEquals(page.regionCount - 1, page.reachedCount(progress.strokes))
    }

    @Test
    fun clearedDropsEverything() {
        val progress = Progress().with(Stroke(Crayons.RED, listOf(Vec2(0.5, 0.5)))).cleared()
        assertTrue(progress.isEmpty)
        assertEquals(0, progress.coloredCount)
    }

    @Test
    fun anEmptySaveReadsAsBlankPaper() {
        assertEquals(Progress.Empty, Progress.parse(""))
        assertEquals(Progress.Empty, Progress.parse(null))
        assertNotEquals(Progress.Empty, Progress().with(Stroke(Crayons.RED, listOf(Vec2(0.1, 0.1)))))
    }
}

/** A point on [page] that really is visible as region [index], by the same
 * topmost-first rule a finger uses. Every region of every page has one, and
 * a structural test in PagesTest holds the whole book to that. */
internal fun visiblePoint(page: Page, index: Int): Vec2 {
    val steps = 120
    for (i in 0 until steps) {
        for (j in 0 until steps) {
            val p = Vec2((i + 0.5) / steps, (j + 0.5) / steps)
            if (page.regionIndexAt(p) == index) return p
        }
    }
    throw AssertionError("${page.id}: region $index is not visible anywhere")
}
