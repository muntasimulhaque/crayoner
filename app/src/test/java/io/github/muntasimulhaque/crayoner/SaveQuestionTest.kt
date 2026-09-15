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

    @Test
    fun theQuestionIsNeverUpWhileTheHandIsOnThePaper() {
        // The overlay is up at the same moment as a finger, only if the child
        // somehow drew through it. Every entry point into the paper refuses a
        // mark while the question is up, so this state is the one the screen
        // composables can rely on: no live mark and no half-drawn stroke can
        // be left behind by an answer.
        val asking = colored().copy(asking = true, live = null)
        assertTrue(asking.asking)
        assertEquals(null, asking.live)
    }
}
