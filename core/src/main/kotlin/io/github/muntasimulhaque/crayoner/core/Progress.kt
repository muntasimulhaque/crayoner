package io.github.muntasimulhaque.crayoner.core

/**
 * The colors a child has put on one page, as region index to crayon color.
 * A missing index is a bare area of paper. This is the whole game state: a
 * page plus these colors is everything a renderer or a test needs.
 *
 * Serialization walks region ids, not indices, so a picture whose areas are
 * reordered in code still reads an old save correctly. A line whose id or
 * color no longer exists is dropped, never guessed at, which is why a
 * corrupt save can only lose colors, never crash a launch.
 */
data class Progress(val fills: Map<Int, Long> = emptyMap()) {

    val coloredCount: Int get() = fills.size

    fun colorOf(index: Int): Long? = fills[index]

    fun with(index: Int, argb: Long): Progress = Progress(fills + (index to argb))

    fun cleared(): Progress = Progress()

    /** The plain map the renderer and [Page.isComplete] read. */
    fun asMap(): Map<Int, Long> = fills

    fun serialize(page: Page): String = fills.entries
        .sortedBy { it.key }
        .mapNotNull { (index, argb) ->
            val id = page.region(index)?.id ?: return@mapNotNull null
            "$id:${argb.toString(16).uppercase()}"
        }
        .joinToString(";")

    companion object {
        val Empty: Progress = Progress()

        /**
         * Reads back a save written by [serialize]. Anything malformed, an
         * unknown region, a color that is not in the crayon box, is skipped;
         * the rest of the picture arrives intact.
         */
        fun parse(text: String?, page: Page): Progress {
            if (text.isNullOrBlank()) return Empty
            val fills = LinkedHashMap<Int, Long>()
            for (entry in text.split(';')) {
                val cut = entry.lastIndexOf(':')
                if (cut <= 0) continue
                val id = entry.substring(0, cut)
                val argb = entry.substring(cut + 1).toLongOrNull(16) ?: continue
                if (!Crayons.exists(argb)) continue
                val index = page.indexOfRegion(id)
                if (index < 0) continue
                fills[index] = argb
            }
            return Progress(fills)
        }
    }
}
