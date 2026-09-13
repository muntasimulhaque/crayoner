package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one step back a child gets. It is a rule about the work, not about the
 * screen, so it is held here with no device anywhere near it: one press of
 * undo, and a page that is never surprised by a press.
 */
class DraftTest {

    private fun line(color: Long, y: Double, points: Int = 8): Stroke =
        Stroke(color, (0 until points).map { Vec2(0.05 + it * 0.1, y) })

    @Test
    fun aFreshSheetHasNothingToUndo() {
        val draft = Draft.Empty
        assertTrue(draft.isEmpty)
        assertEquals(0, draft.progress.strokes.size)
        // The press lands on nothing and the page does not move: there is no
        // way for a child to tell undo apart from a mark that did not happen.
        assertEquals(draft, draft.undo())
    }

    @Test
    fun undoTakesBackOnlyTheMarkTheHandJustFinished() {
        val draft = Draft.Empty
            .color(Crayons.RED, line(Crayons.RED, 0.2).points)
            .color(Crayons.BLUE, line(Crayons.BLUE, 0.4).points)
            .color(Crayons.GREEN, line(Crayons.GREEN, 0.6).points)
        assertEquals(3, draft.progress.strokes.size)
        val after = draft.undo()
        assertEquals(2, after.progress.strokes.size)
        assertEquals(Crayons.BLUE, after.progress.strokes.last().color)
        // And only that: there is no stack of steps behind it.
        assertNull(after.last)
        assertEquals(2, after.undo().progress.strokes.size)
    }

    @Test
    fun undoTakesBackAnEraserMarkLikeAnyOtherMark() {
        val erased = Draft.Empty
            .color(Crayons.RED, line(Crayons.RED, 0.3).points)
            .erase(line(Stroke.ERASE_COLOR, 0.5).points)
        assertEquals(2, erased.progress.strokes.size)
        val after = erased.undo()
        assertEquals(1, after.progress.strokes.size)
        assertTrue(!after.progress.strokes.last().erase)
    }

    @Test
    fun aMarkThatDoesNotExistChangesNothing() {
        val draft = Draft.Empty.color(Crayons.RED, emptyList())
        assertTrue("an empty mark landed on the paper", draft.isEmpty)
        assertTrue(draft.undo() === draft)
    }

    @Test
    fun aFullPageStillLetsAMarkBeTakenBack() {
        var draft = Draft.Empty
        for (i in 0 until Strokes.MAX_STROKES) {
            draft = draft.color(Crayons.RED, line(Crayons.RED, (i % 100) / 100.0).points)
        }
        assertEquals(Strokes.MAX_STROKES, draft.progress.strokes.size)
        // A full page takes no more marks, and still gives the child one back.
        val fuller = draft.color(Crayons.BLUE, listOf(Vec2(0.5, 0.5)))
        assertEquals(Strokes.MAX_STROKES, fuller.progress.strokes.size)
        assertEquals(Strokes.MAX_STROKES - 1, draft.undo().progress.strokes.size)
    }

    @Test
    fun aMarkIsAlwaysAppendedAndNeverReshuffled() {
        // The order of the hand is the truth of the paper: later wax covers
        // earlier wax, and nothing here may reorder a child's work.
        val first = line(Crayons.RED, 0.2)
        val second = line(Crayons.BLUE, 0.4)
        val draft = Draft.Empty.add(first).add(second)
        assertEquals(listOf(Crayons.RED, Crayons.BLUE), draft.progress.strokes.map { it.color })
        assertEquals(first, draft.progress.strokes[0])
        assertEquals(second, draft.progress.strokes[1])
    }

    @Test
    fun aPageReadBackFromASaveCarriesNoStepBack() {
        val progress = Progress.Empty.with(line(Crayons.RED, 0.2))
        val draft = Draft.of(progress)
        assertEquals(1, draft.progress.strokes.size)
        // Nothing happened in front of the child, so there is nothing for
        // the hand to take back: opening a saved picture and pressing undo
        // does nothing at all.
        assertNull(draft.last)
        assertTrue(draft.undo() === draft)
    }
}
