package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The steps back a child gets. They are a rule about the work, not about the
 * screen, so they are held here with no device anywhere near it: one press
 * takes back one finished mark, pressing again keeps walking back, and a
 * page is never surprised by a press.
 */
class DraftTest {

    private fun line(color: Long, y: Double, points: Int = 8): Stroke =
        Stroke(color, (0 until points).map { Vec2(0.05 + it * 0.1, y) })

    @Test
    fun aFreshSheetHasNothingToUndo() {
        val draft = Draft.Empty
        assertTrue(draft.isEmpty)
        assertFalse(draft.canUndo)
        assertEquals(0, draft.progress.strokes.size)
        // The press lands on nothing and the page does not move: there is no
        // way for a child to tell undo apart from a mark that did not happen.
        assertEquals(draft, draft.undo())
    }

    @Test
    fun onePressTakesBackOneMarkAndPressingAgainKeepsWalkingBack() {
        // A child who drew three marks they did not mean gets all three
        // back, one press each, rather than a single mark and a dead button.
        val draft = Draft.Empty
            .color(Crayons.RED, line(Crayons.RED, 0.2).points)
            .color(Crayons.BLUE, line(Crayons.BLUE, 0.4).points)
            .color(Crayons.GREEN, line(Crayons.GREEN, 0.6).points)
        assertEquals(3, draft.progress.strokes.size)

        val first = draft.undo()
        assertEquals(2, first.progress.strokes.size)
        assertEquals(Crayons.BLUE, first.progress.strokes.last().color)

        val second = first.undo()
        assertEquals(1, second.progress.strokes.size)
        assertEquals(Crayons.RED, second.progress.strokes.last().color)

        val third = second.undo()
        assertTrue(third.isEmpty)
        assertFalse(third.canUndo)
        // And a press on the empty page is a press that simply lands.
        assertEquals(third, third.undo())
    }

    @Test
    fun everyStepBackIsAPaperTheHandReallyMade() {
        // The page a press returns to is never invented: it is exactly the
        // paper that was on the desk before that mark was drawn.
        val one = line(Crayons.RED, 0.2)
        val two = line(Crayons.BLUE, 0.4)
        val three = line(Crayons.GREEN, 0.6)
        val draft = Draft.Empty.add(one).add(two).add(three)
        val afterTwo = draft.undo()
        assertEquals(Progress.Empty.with(one).with(two), afterTwo.progress)
        val afterOne = afterTwo.undo()
        assertEquals(Progress.Empty.with(one), afterOne.progress)
        val afterNone = afterOne.undo()
        assertEquals(Progress.Empty, afterNone.progress)
    }

    @Test
    fun drawingAgainAfterAStepBackLeavesNoPhantomFuture() {
        // The stack holds papers, not a redo queue. Drawing after a step back
        // starts afresh from the paper now on the desk, and no press can
        // bring a mark back that the child has drawn over.
        val draft = Draft.Empty
            .color(Crayons.RED, line(Crayons.RED, 0.2).points)
            .color(Crayons.BLUE, line(Crayons.BLUE, 0.4).points)
            .undo()
        assertEquals(1, draft.progress.strokes.size)
        val redrawn = draft.color(Crayons.GREEN, line(Crayons.GREEN, 0.6).points)
        assertEquals(2, redrawn.progress.strokes.size)
        assertEquals(Crayons.GREEN, redrawn.progress.strokes.last().color)
        // One press takes back the green mark, then the red one, and stops.
        assertEquals(1, redrawn.undo().progress.strokes.size)
        assertFalse(redrawn.undo().undo().canUndo)
    }

    @Test
    fun theStepsBackAreBounded() {
        // A page worked on for an hour must not grow a copy of itself, so the
        // history is a fixed depth. The paper is still whole; only the walk
        // back is finite.
        var draft = Draft.Empty
        for (i in 0 until Draft.UNDO_DEPTH + 6) {
            draft = draft.color(Crayons.RED, line(Crayons.RED, (i % 100) / 100.0).points)
        }
        assertEquals(Draft.UNDO_DEPTH, draft.past.size)
        var steps = 0
        while (draft.canUndo) {
            draft = draft.undo()
            steps++
            assertTrue("the walk back never ended", steps <= Draft.UNDO_DEPTH)
        }
        assertEquals(Draft.UNDO_DEPTH, steps)
    }

    @Test
    fun aPageReadBackFromASaveCarriesNoStepBack() {
        val progress = Progress.Empty.with(line(Crayons.RED, 0.2))
        val draft = Draft.of(progress)
        assertEquals(1, draft.progress.strokes.size)
        // Nothing happened in front of the child, so there is nothing for
        // the hand to take back: opening a saved picture and pressing undo
        // does nothing at all.
        assertFalse(draft.canUndo)
        assertTrue(draft.undo() === draft)
        // And nothing to put forward either, for the same reason.
        assertFalse(draft.canRedo)
        assertTrue(draft.redo() === draft)
    }

    @Test
    fun redoPutsBackExactlyWhatUndoTookOff() {
        // The two are one pair: whatever a step back took off the paper, a
        // step forward puts back on it, unchanged, wherever it was in the
        // stack of marks.
        val one = line(Crayons.RED, 0.2)
        val two = line(Crayons.BLUE, 0.4)
        val three = line(Crayons.GREEN, 0.6)
        val drawn = Draft.Empty.add(one).add(two).add(three)
        val back = drawn.undo().undo()
        assertEquals(1, back.progress.strokes.size)
        val forward = back.redo()
        assertEquals(2, forward.progress.strokes.size)
        assertEquals(Progress.Empty.with(one).with(two), forward.progress)
        val allTheWay = forward.redo()
        assertEquals(drawn.progress, allTheWay.progress)
        // And at the top of the stack there is nothing more to put back.
        assertFalse(allTheWay.canRedo)
        assertTrue(allTheWay.redo() === allTheWay)
    }

    @Test
    fun aFreshSheetHasNothingToRedo() {
        assertFalse(Draft.Empty.canRedo)
        assertTrue(Draft.Empty.redo() === Draft.Empty)
    }

    @Test
    fun walkingBackAndForwardKeepsEveryMark() {
        // A child who stepped back over three marks they did not mean can
        // walk the whole way forward again: nothing is lost by looking, and
        // the paper at the end is the paper they had.
        var draft = Draft.Empty
        for (i in 0 until 6) draft = draft.color(Crayons.RED, line(Crayons.RED, i / 10.0).points)
        val atTop = draft
        repeat(4) { draft = draft.undo() }
        repeat(4) { draft = draft.redo() }
        assertEquals(atTop.progress, draft.progress)
        assertTrue(draft.canUndo)
        assertFalse(draft.canRedo)
    }

    @Test
    fun drawingClearsTheStepsForward() {
        // A mark made now is the new truth of the paper. No press may ever
        // take it away to put back a mark the hand has already drawn over.
        val drawn = Draft.Empty
            .color(Crayons.RED, line(Crayons.RED, 0.2).points)
            .color(Crayons.BLUE, line(Crayons.BLUE, 0.4).points)
        val steppedBack = drawn.undo()
        assertTrue(steppedBack.canRedo)
        val drawnOver = steppedBack.color(Crayons.GREEN, line(Crayons.GREEN, 0.6).points)
        assertFalse(drawnOver.canRedo)
        assertEquals(2, drawnOver.progress.strokes.size)
        assertEquals(Crayons.GREEN, drawnOver.progress.strokes.last().color)
        // Stepping back from there reaches the red mark, and the blue mark
        // the child drew over is not waiting anywhere behind it: the only
        // thing a step forward can put back is the green mark just taken off.
        val back = drawnOver.undo()
        assertEquals(1, back.progress.strokes.size)
        assertEquals(Crayons.RED, back.progress.strokes.last().color)
        val again = back.redo()
        assertEquals(2, again.progress.strokes.size)
        assertEquals(Crayons.GREEN, again.progress.strokes.last().color)
        assertTrue(again.progress.strokes.none { it.color == Crayons.BLUE })
        assertFalse(again.canRedo)
    }

    @Test
    fun theStepsForwardAreBoundedLikeTheStepsBack() {
        var draft = Draft.Empty
        for (i in 0 until Draft.UNDO_DEPTH + 6) {
            draft = draft.color(Crayons.RED, line(Crayons.RED, (i % 100) / 100.0).points)
        }
        repeat(Draft.UNDO_DEPTH + 6) { draft = draft.undo() }
        assertTrue(draft.future.size <= Draft.UNDO_DEPTH)
        var steps = 0
        while (draft.canRedo) {
            draft = draft.redo()
            steps++
            assertTrue("the walk forward never ended", steps <= Draft.UNDO_DEPTH)
        }
    }

    @Test
    fun undoTakesBackAnEraserMarkAndRedoPutsItBack() {
        val erased = Draft.Empty
            .color(Crayons.RED, line(Crayons.RED, 0.3).points)
            .erase(line(Stroke.ERASE_COLOR, 0.5).points)
        assertEquals(2, erased.progress.strokes.size)
        val after = erased.undo()
        assertEquals(1, after.progress.strokes.size)
        assertTrue(!after.progress.strokes.last().erase)
        // The rubber's own mark is a mark like any other, and the pair walks
        // over it in both directions.
        val again = after.redo()
        assertEquals(erased.progress, again.progress)
        assertTrue(again.progress.strokes.last().erase)
    }

    @Test
    fun aMarkThatDoesNotExistChangesNothing() {
        val draft = Draft.Empty.color(Crayons.RED, emptyList())
        assertTrue("an empty mark landed on the paper", draft.isEmpty)
        assertTrue(draft.undo() === draft)
        assertTrue(draft.past.isEmpty())
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
    fun aPageReadBackFromASaveCarriesNoHistory() {
        val progress = Progress.Empty.with(line(Crayons.RED, 0.2))
        val draft = Draft.of(progress)
        assertEquals(1, draft.progress.strokes.size)
        // Nothing happened in front of the child, so there is nothing for
        // the hand to take back or to put forward: opening a saved picture
        // and pressing either press does nothing at all.
        assertFalse(draft.canUndo)
        assertTrue(draft.undo() === draft)
        assertFalse(draft.canRedo)
        assertTrue(draft.redo() === draft)
    }
}
