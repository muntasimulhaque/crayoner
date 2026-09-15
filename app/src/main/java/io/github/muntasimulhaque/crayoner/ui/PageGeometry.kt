package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.graphics.Path
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Region

/**
 * The device half of the one renderer. A picture is printed the way a real
 * coloring book prints it: each area is colored in wax, and its outline is
 * drawn on top, so a later area's color covers an earlier area's line and
 * hidden edges vanish.
 *
 * The paths for one page at one width are built once and kept (see
 * [PageCanvas]), because a page is otherwise rebuilt on every touch of a
 * crayon, and the whole printed picture is kept as pixels (see [PageImage]);
 * a finger crossing the paper then costs one image and a handful of marks,
 * however rich the wax underneath is.
 *
 * Page units are isotropic, so one pixel scale serves both axes: a page
 * drawn [width] pixels across is [Page.ASPECT] times that tall, and a circle
 * stays a circle on the taller sheet.
 */
class PageGeometry(private val page: Page, private val width: Float) {

    /** One path per area, the union of its parts. */
    val outlines: List<Path> = page.regions.map { region ->
        unionOf(region.parts, width.toDouble())
    }

    /** The area at [index], for the wax its color is made of. */
    fun region(index: Int): Region? = page.region(index)
}

/**
 * One page's paths at one width, built once and kept for the frame it is
 * drawn in. The map is bounded, and the bound is the whole book: a wall with
 * sixteen cards whose pictures are still being made asks for every page's
 * paths at one width, and a cache that held six of them would rebuild the
 * other ten on the next frame, which is a scroll stutter made by arithmetic.
 * Sixteen pages at one width, and room for the sheet's width beside them.
 *
 * It is read from the frame and from the background renders alike, so it is
 * guarded: the union of an area's shapes is the expensive part of a page and
 * two threads asking for the same one is exactly the work this avoids.
 */
private val geometries = object : LinkedHashMap<Pair<String, Float>, PageGeometry>(40) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<Pair<String, Float>, PageGeometry>?,
    ): Boolean = size > GEOMETRY_LIMIT
}

private const val GEOMETRY_LIMIT = 40

internal fun liveGeometry(page: Page, width: Float): PageGeometry =
    synchronized(geometries) {
        geometries.getOrPut(page.id to width) { PageGeometry(page, width) }
    }
