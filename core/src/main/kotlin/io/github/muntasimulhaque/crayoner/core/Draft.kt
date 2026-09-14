package io.github.muntasimulhaque.crayoner.core

/**
 * The page the child is working on, and the steps back they get.
 *
 * [progress] is the paper itself: every mark that is on it, always the whole
 * truth. [past] is the paper as it was before each finished mark, oldest
 * first, so one press of undo walks back one state and the child may keep
 * pressing: a hand that draws three marks it did not mean gets all three
 * back, one press each.
 *
 * The stack is bounded ([UNDO_DEPTH]), because a page a three year old has
 * worked on for an hour must not grow a second copy of itself in memory, and
 * because a step that reaches past the last dozen marks is not a step back
 * any more: the rubber is what changes a whole picture. Once the stack is
 * empty, a press simply lands.
 *
 * Drawing again after a step back starts a fresh stack from where the paper
 * now is, so the paper a press returns to is always a paper the hand really
 * made. A page read back from a save carries no stack at all: nothing
 * happened in front of the child, and undo is for the hand.
 *
 * Every operation here is total and quiet. Undoing an empty page changes
 * nothing, a mark is appended and never reshuffled, and a save that reads
 * back empty is an empty page. Nothing in this file can be surprised by a
 * hand, which is the whole reason it lives in :core and is tested there.
 */
data class Draft(
    /** The marks that are on the paper now, in the order they were made. */
    val progress: Progress = Progress.Empty,
    /**
     * The paper as it was before each finished mark, oldest first. The last
     * entry is what one press of undo puts back.
     */
    val past: List<Progress> = emptyList(),
) {
    val isEmpty: Boolean get() = progress.isEmpty

    /** True while there is at least one mark the child can take back. */
    val canUndo: Boolean get() = past.isNotEmpty()

    /** One finished mark made with the crayon, in page units. */
    fun color(crayon: Long, points: List<Vec2>): Draft =
        if (points.isEmpty()) this else add(Stroke(crayon, points))

    /** One finished mark made with the rubber. */
    fun erase(points: List<Vec2>): Draft =
        if (points.isEmpty()) this else add(Stroke(Stroke.ERASE_COLOR, points, erase = true))

    /** One mark, whichever end of the box made it. */
    fun add(stroke: Stroke?): Draft {
        if (stroke == null || stroke.isEmpty) return this
        val next = progress.with(stroke)
        // A full page takes no more marks and remembers no more: the sheet
        // is what it is, and nothing about it changes under the child.
        if (next === progress) return this
        return Draft(next, (past + progress).takeLast(UNDO_DEPTH))
    }

    /**
     * Takes the last finished mark off the paper, or does nothing at all
     * when there is nothing to take: a fresh sheet has no step to undo and
     * the press simply lands. Pressed again it keeps walking back, one
     * finished mark a press, until the paper is as it was when the stack
     * began.
     *
     * There is no start over and no confirm anywhere in the app, because the
     * rubber already exists for rubbing a page back to paper, and nothing a
     * child can reach may cost them their picture.
     */
    fun undo(): Draft =
        if (past.isEmpty()) this else Draft(past.last(), past.dropLast(1))

    companion object {
        val Empty: Draft = Draft()

        /**
         * How many finished marks a page can walk back. Deep enough that a
         * wandering hand is never stuck with what it drew, and shallow
         * enough that a long session never holds more than a dozen small
         * lists of marks.
         */
        const val UNDO_DEPTH = 16

        /**
         * A page read back from a save: no step back, because nothing
         * happened in front of the child.
         */
        fun of(progress: Progress): Draft = Draft(progress, emptyList())
    }
}
