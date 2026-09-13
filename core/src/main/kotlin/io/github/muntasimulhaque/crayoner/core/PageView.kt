package io.github.muntasimulhaque.crayoner.core

/**
 * The window a renderer looks at one page through.
 *
 * The page is always the same square of paper: a mark is a line of page
 * coordinates, and a closer look changes nothing about the paper or the work
 * on it. So a close look is a lens, not a mode: [zoom] is how many times
 * bigger the paper is drawn, and [focus] is the point of the page the window
 * is centered on.
 *
 * The window always stays on the paper. A focus near a corner is pulled back
 * so the window's own edge lands on the page's edge, which is why a close
 * look at a corner shows the corner and never the desk beside it.
 *
 * Both numbers are snapped, the focus to a sixteenth of the page and the
 * zoom to an eighth of a step. A window is therefore a stable thing: the
 * same place and the same closeness always produce the same window, and a
 * renderer can keep the picture it drew for it instead of drawing a slightly
 * different one for every pixel of a slider. Neither snap is visible: a
 * fingertip is coarser than a sixteenth of a page, and a slider cannot be
 * aimed finer than an eighth of a step.
 */
data class PageView(
    val zoom: Double = WHOLE_ZOOM,
    val focus: Vec2 = WHOLE_FOCUS,
) {
    /** How much bigger the paper is drawn, never under whole. */
    val scale: Double = snapZoom(zoom)

    /** The window's side, in page units. */
    val span: Double = 1.0 / scale

    /** The window's own corner, kept on the paper. */
    val left: Double = (focus.x - span / 2.0).coerceIn(0.0, 1.0 - span)
    val top: Double = (focus.y - span / 2.0).coerceIn(0.0, 1.0 - span)

    /** True while the whole sheet is in view, which is how a page opens. */
    val isWhole: Boolean = scale <= WHOLE_ZOOM + 1e-9

    /**
     * A number that names this window exactly, for a cache of pictures drawn
     * through it: two windows with the same key are the same window on the
     * same paper, and the picture drawn for one is the picture for both.
     */
    val key: Int = windowKey(scale, left, top)

    /** Where [p] falls in the window: 0 at the window's left and top edge. */
    fun inWindow(p: Vec2): Vec2 = Vec2((p.x - left) / span, (p.y - top) / span)

    /** The page point at [w], the inverse of [inWindow]. */
    fun onPage(w: Vec2): Vec2 = Vec2(left + w.x * span, top + w.y * span)

    companion object {
        const val WHOLE_ZOOM = 1.0

        val WHOLE_FOCUS: Vec2 = Vec2(0.5, 0.5)

        /** How much closer the close look brings the paper: half of it. */
        const val CLOSE_ZOOM = 2.0

        /** The most the paper is ever magnified. */
        const val MAX_ZOOM = 2.0

        /** How finely a focus is placed, in steps across the page. */
        const val FOCUS_STEPS = 16.0

        /** How finely the zoom is placed, in steps between whole and close. */
        const val ZOOM_STEPS = 8.0

        /** The whole sheet, which is how every page opens. */
        val Whole: PageView = PageView()

        /** A close look at [at], the middle of the page when there is none. */
        fun closeOn(at: Vec2?): PageView = PageView(CLOSE_ZOOM, snap(at ?: WHOLE_FOCUS))

        /** [p] rounded onto the focus grid, so a window is a stable thing. */
        fun snap(p: Vec2): Vec2 = Vec2(
            Math.round(p.x * FOCUS_STEPS) / FOCUS_STEPS,
            Math.round(p.y * FOCUS_STEPS) / FOCUS_STEPS,
        )

        /** [zoom] rounded onto the zoom grid, and held between the extremes. */
        private fun snapZoom(zoom: Double): Double {
            val stepped = Math.round(zoom * ZOOM_STEPS) / ZOOM_STEPS
            return stepped.coerceIn(WHOLE_ZOOM, MAX_ZOOM)
        }

        /** The one number that names a window, made from the snapped pair. */
        private fun windowKey(scale: Double, left: Double, top: Double): Int {
            val z = Math.round(scale * ZOOM_STEPS).toInt()
            val x = Math.round(left * FOCUS_STEPS).toInt().coerceIn(0, FOCUS_STEPS.toInt())
            val y = Math.round(top * FOCUS_STEPS).toInt().coerceIn(0, FOCUS_STEPS.toInt())
            return (z shl 16) or (x shl 8) or y
        }
    }
}
