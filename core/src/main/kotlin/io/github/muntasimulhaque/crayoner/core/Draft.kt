package io.github.muntasimulhaque.crayoner.core

/**
 * The page the child is working on, and the one step back they get.
 *
 * [progress] is the paper itself: every mark that is on it, always the whole
 * truth. [last] is the mark the hand finished most recently, which is what
 * one press of undo takes back. One entry and never a stack, because a three
 * year old's hand wanders over the paper and fifty steps of history is not a
 * thing they asked for; the mark just made is the one they are thinking
 * about.
 *
 * Every operation here is total and quiet. Undoing an empty page changes
 * nothing, a mark is appended and never reshuffled, and a save that reads
 * back empty is an empty page. Nothing in this file can be surprised by a
 * hand, which is the whole reason it lives in :core and is tested there.
 */
data class Draft(
    /** The marks that are on the paper now, in the order they were made. */
    val progress: Progress = Progress.Empty,
    /** The last mark finished, while it is still the last thing on paper. */
    val last: Stroke? = null,
) {
    val isEmpty: Boolean get() = progress.isEmpty

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
        return Draft(next, stroke)
    }

    /**
     * Takes the last mark off the paper, or does nothing at all when there
     * is nothing to take: an empty sheet has no step to undo and the press
     * simply lands.
     *
     * There is no start over and no confirm anywhere in the app, because the
     * rubber already exists for rubbing a page back to paper, and nothing a
     * child can reach may cost them their picture.
     */
    fun undo(): Draft {
        val slice = last ?: return this
        if (slice.isEmpty) return Draft(progress, null)
        return Draft(Progress(progress.strokes.dropLast(1)), null)
    }

    companion object {
        val Empty: Draft = Draft()

        /**
         * A page read back from a save. It carries no step back, because
         * nothing happened in front of the child: a mark restored from last
         * time is not the thing their hand just did, and undo is for the
         * hand.
         */
        fun of(progress: Progress): Draft = Draft(progress, null)
    }
}
