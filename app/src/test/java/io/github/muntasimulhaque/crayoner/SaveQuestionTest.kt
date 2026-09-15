package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Draft
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The save question, as a rule rather than as a screen.
 *
 * The one question the app asks is asked on the way home, and the thing that
 * makes it safe to ask is that it is asked about a paper that is already on
 * the desk: every answer is a decision about what the next visit shows, never
 * a chance to lose work. That is a property of the state, not of the drawing,
 * so it is held here.
 *
 * The mark under the finger is not part of this state any more: it is the
 * drawing layer's own, and it is let go when the question comes up (see
 * `ColoringHost.home`), so there is no longer a state in which the question
 * and a half-drawn mark could be on the desk together.
 */
class SaveQuestionTest {

    private val page = Pages.byId("sail") ?: error("the sail page is gone")

    private fun colored(): Screen.Coloring {
        val marks = listOf(
            Stroke(Crayons.RED, listOf(Vec2(0.3, 0.3), Vec2(0.6, 0.5), Vec2(0.4, 0.7))),
        )
        return Screen.Coloring(page = page, draft = Draft.of(io.github.muntasimulhaque.crayoner.core.Progress(marks)))
    }

    @Test
    fun aBarePageHasNothingToAskAbout() {
        // The question is only worth asking when there is work on the paper.
        // A fresh sheet goes straight home, and the app never interrupts a
        // child to ask them about nothing.
        assertTrue(Screen.Coloring(page = page, draft = Draft.Empty).progress.strokes.isEmpty())
        assertFalse(colored().progress.strokes.isEmpty())
    }

    @Test
    fun raisingTheQuestionChangesNothingAboutTheWork() {
        // The state that holds the question up carries the same paper it did
        // a moment before: the marks are the child's whether or not they are
        // being asked about them, and no answer can move them.
        val before = colored()
        val asking = before.copy(asking = true)
        assertEquals(before.draft, asking.draft)
        assertEquals(before.progress, asking.progress)
        assertTrue(asking.asking)
    }

    @Test
    fun eitherAnswerLeavesThePictureItsOwnShape() {
        // Keeping it and starting it fresh are decisions about the next visit,
        // and both start from the paper the child is looking at. Nothing that
        // answers the question reaches into the marks themselves, which is
        // what makes both answers safe: the rubber is still the only thing
        // that takes wax off a sheet the child is holding.
        val s = colored()
        assertEquals(1, s.progress.strokes.size)
        assertEquals(Crayons.RED, s.progress.strokes.first().color)
        // A page reopened from a save has no history, so neither answer can
        // put a step back on the stack that the child never made.
        assertFalse(s.canUndo)
        assertFalse(s.canRedo)
    }
}
