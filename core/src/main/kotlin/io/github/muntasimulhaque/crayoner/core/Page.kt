package io.github.muntasimulhaque.crayoner.core

/**
 * One area of a picture the child can color. A region is a list of shapes,
 * painted together in order and read as one: a cloud is three circles, a
 * sail is a triangle carrying its mast. Grouping them keeps the number of
 * tappable areas low, which is what a small hand can hold.
 *
 * [id] is stable forever: saved progress is written against it, so
 * reordering regions in code never moves a child's colors. [kind] names the
 * thing for the screen reader and is resolved to a word in res/values.
 * [fillArgb] is the color the finished picture uses, and it is always a
 * crayon from [Crayons], so every area can be matched exactly.
 */
data class Region(
    val id: String,
    val kind: String,
    val fillArgb: Long,
    val parts: List<Shape>,
) {
    val bounds: Area get() = parts.boundsOrNull() ?: Area.Unit
    val area: Double get() = parts.totalArea()
    val centroid: Vec2 get() = parts.centroidOrNull() ?: bounds.center

    fun contains(p: Vec2): Boolean = parts.contains(p)
}

/**
 * One page of the coloring book: a name and the areas that make the picture.
 * Region order is paint order, first at the back, so the background is region
 * zero and every later region covers what came before it. Taps resolve the
 * same way, topmost first, which is why what a child sees is always what a
 * child gets.
 *
 * Region zero is the ground: it covers the whole paper, and both renderers
 * deliberately skip its outline, because a real coloring page prints no
 * border around the sheet. [isGround] states that rule in one place and the
 * structural tests hold every page to it.
 */
data class Page(
    val id: String,
    val regions: List<Region>,
) {
    val regionCount: Int get() = regions.size

    /** The region that covers the paper; it is never outlined. */
    val groundIndex: Int get() = 0

    fun isGround(index: Int): Boolean = index == groundIndex

    fun region(index: Int): Region? = regions.getOrNull(index)

    fun indexOfRegion(regionId: String): Int = regions.indexOfFirst { it.id == regionId }

    /**
     * The region a tap at [p] lands on: the topmost one containing the point,
     * or -1 when the point is outside the paper. The ground covers the whole
     * square, so a tap on the page always lands somewhere.
     */
    fun regionIndexAt(p: Vec2): Int {
        for (i in regions.indices.reversed()) {
            if (regions[i].contains(p)) return i
        }
        return -1
    }

    /** How many areas are still bare paper. */
    fun blankCount(fills: Map<Int, Long>): Int =
        regions.indices.count { fills[it] == null }

    /** How many areas carry the color the picture asks for. */
    fun matchCount(fills: Map<Int, Long>): Int =
        regions.indices.count { fills[it] == regions[it].fillArgb }

    /**
     * The picture is finished when every area has a color on it, whatever
     * color that is. A real coloring book does not refuse to be finished
     * because the sky was colored green: the child decides, and the app's
     * only job is to celebrate the work.
     */
    fun isComplete(fills: Map<Int, Long>): Boolean = blankCount(fills) == 0
}
