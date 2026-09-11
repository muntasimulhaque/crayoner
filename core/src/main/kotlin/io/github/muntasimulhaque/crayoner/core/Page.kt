package io.github.muntasimulhaque.crayoner.core

/**
 * One area of a picture the child can color. A region is a list of shapes,
 * painted together in order and read as one: a cloud is three circles, a
 * sail is a triangle carrying its mast.
 *
 * A region is a piece of the print, not a slot to be filled: the picture is
 * one drawing made of areas, the child's wax goes wherever the hand takes
 * it, and the app never counts which areas have felt it. Areas still exist
 * because the picture is drawn from them and because a screen reader needs
 * one named target per part of the picture.
 *
 * [kind] names the thing for the screen reader and is resolved to a word in
 * res/values. [fillArgb] is the color the finished picture uses, and it is
 * always a crayon from [Crayons], so every area can be matched exactly.
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
 * zero and every later region covers what came before it. A finger resolves
 * the same way, topmost first, which is why what a child sees is always what
 * a child gets.
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
     * The region a touch at [p] lands on: the topmost one containing the
     * point, or -1 when the point is outside the paper. The ground covers the
     * whole square, so a touch on the page always lands somewhere, which is
     * how the first touch knows which color the child is reaching for.
     */
    fun regionIndexAt(p: Vec2): Int {
        for (i in regions.indices.reversed()) {
            if (regions[i].contains(p)) return i
        }
        return -1
    }
}
