package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The marks a child's hand leaves: how they are read, how they are saved,
 * and how they come back. A mark is a line with a color on it, or a line
 * made with the rubber, and the app never reads more into it than that: no
 * area is ever counted, filled, or judged.
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
            "X()", "X(,)", "X(0.5;0.5)", "X", "x(0.5,0.5)",
            "FFEF2D57(" + "0.5,0.5;".repeat(5000) + ")",
        )) {
            Progress.parse(text)
        }
    }

    @Test
    fun aPointOffThePaperIsDropped() {
        val parsed = Progress.parse("FFEE204D(0.5,0.5;-0.2,0.5;0.5,1.4)")
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
    fun anEraserMarkSurvivesASaveAndKeepsItsMeaning() {
        // The eraser is a mark like any other: the same line, made with the
        // rubber. It has to come back from a save as an eraser and not as a
        // crayon named black, or a restored page would grow a dark line
        // where the child had rubbed one out.
        val progress = Progress()
            .with(strokeOver("sea", Crayons.BLUE))
            .with(Stroke(Stroke.ERASE_COLOR, listOf(Vec2(0.4, 0.8), Vec2(0.5, 0.82)), erase = true))
            .with(strokeOver("sail", Crayons.WHITE))
        val parsed = Progress.parse(progress.serialize())
        assertEquals(3, parsed.strokes.size)
        assertFalse(parsed.strokes[0].erase)
        assertTrue(parsed.strokes[1].erase)
        assertEquals(2, parsed.strokes[1].points.size)
        assertFalse(parsed.strokes[2].erase)
        assertEquals(Crayons.WHITE, parsed.strokes[2].color)
    }

    @Test
    fun marksSurviveAPageWhoseAreasWereRearranged() {
        // A mark belongs to the paper, not to an area: a save made against
        // an older order of regions still reads back whole, because nothing
        // in it names an area at all.
        val progress = Progress()
            .with(strokeOver("sea", Crayons.BLUE))
            .with(strokeOver("boat", Crayons.RED))
        val parsed = Progress.parse(progress.serialize())
        assertEquals(progress.strokes.map { it.color }, parsed.strokes.map { it.color })
        assertEquals(progress.strokes[0].points.size, parsed.strokes[0].points.size)
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
