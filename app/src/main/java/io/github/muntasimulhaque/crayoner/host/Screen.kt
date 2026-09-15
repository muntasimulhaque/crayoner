package io.github.muntasimulhaque.crayoner.host

import io.github.muntasimulhaque.crayoner.core.Draft
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Progress

/**
 * Where in the book we are, and everything the screen needs to draw the
 * place. The host decides the place; the state is data, so the screenshot
 * harness can host every one of them with no ViewModel and no device state.
 */
sealed interface Screen {
    data object Home : Screen

    data class Coloring(
        val page: Page,
        /** The paper: every mark on it, and the one step back. */
        val draft: Draft,
        /** The crayon in hand; null until the child picks one up. */
        val crayon: Long? = null,
        /**
         * True while the rubber is in hand: the next mark takes wax off the
         * paper instead of putting it on. It never touches the printed line,
         * because on paper the print is under the wax.
         */
        val erasing: Boolean = false,
        /** The box of colors, held up over the page. */
        val boxOpen: Boolean = false,
        /** The sample held up big, while the child looks closely. */
        val peeking: Boolean = false,
        /**
         * True while the child is being asked whether to keep the picture
         * they are leaving. It is raised only when there is work on the page
         * and the child presses Home, and it is answered by Keep or by Start
         * fresh; there is no third answer and no way to be stuck in it,
         * because the system's own Back also puts the question away.
         */
        val asking: Boolean = false,
        /**
         * Counts the marks the hand has finished. It is what the one-shot
         * answers (the haptic, and the flattened layer of marks) read, so a
         * mark that lands over paper already colored still answers like a
         * mark. Stepping back and forward both bump it, because both change
         * the paper.
         */
        val marks: Long = 0L,
    ) : Screen {
        /** The marks on the paper, for anything that only draws them. */
        val progress: Progress get() = draft.progress

        /** True while there is a mark the child can take back. */
        val canUndo: Boolean get() = draft.canUndo

        /** True while there is a step back the child can put forward. */
        val canRedo: Boolean get() = draft.canRedo
    }
}

/** What the shelf needs to draw itself. */
data class ShelfState(
    /** The marks of the page still being worked on, by page id. */
    val drafts: Map<String, String> = emptyMap(),
    val soundOn: Boolean = true,
    /**
     * False until the saved shelf has been read (or its read has failed).
     * The shelf waits for it, so nothing it holds is drawn before it is
     * known. The screenshot harness hosts states directly and leaves it
     * true.
     */
    val loaded: Boolean = true,
)

/**
 * The shelf with one page's draft remembered, or let go when the draft is
 * blank. It is a function of the state, not of the store, so the rule that a
 * kept picture is the picture a child comes back to can be held without a
 * device: writing only to the disk was the bug that made Keep open blank
 * paper, because the shelf in memory still held the map it was born with.
 */
internal fun ShelfState.remembering(pageId: String, text: String): ShelfState = copy(
    drafts = if (text.isEmpty()) drafts - pageId else drafts + (pageId to text),
)
