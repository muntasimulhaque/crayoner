package io.github.muntasimulhaque.crayoner

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.crayoner.core.Pages
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Where the wax lands when a real finger draws, measured on a real screen.
 *
 * The pure mapping is held by `core/PagePointTest` with no device, and the
 * renderer's side of it by the sheets. Neither can see the thing the child
 * actually sees: a touch on the glass, through the shipped Compose tree, on
 * the real laid-out sheet, with the real density, in whichever of the app's
 * two layouts the screen happens to be. That is what these probes measure.
 *
 * The row probe asks the biggest question: a mark drawn by a finger that
 * moved along one row of the glass must lie on that row. The dot probe asks
 * the whole of it: a tap must leave its wax under the fingertip in x and in
 * y, because sideways is the one direction a row can never catch. See
 * D-063 and D-076.
 */
@RunWith(AndroidJUnit4::class)
class TouchProbeTest {

    private val probe = PageProbe(Pages.byId("sail") ?: error("the sail page is gone"))
    private val sheet = PageProbe.Sheet()

    /**
     * A finger draws straight across two rows, far apart, and the wax has to
     * be on each row it drew on. No constant offset and no scale error can
     * hide between the two.
     */
    @Test
    fun aMarkIsDrawnOnTheRowOfTheGlassTheFingerDrewOn() {
        val scenario = probe.launch()
        probe.push(sheet)

        val shot = probe.capture(scenario)
        val band = probe.sheetBand(shot)
        assertTrue(
            "no sheet of paper found on the screen: the top and the bottom of " +
                "the page are what the finger has to land on",
            band.height > shot.height * 0.3,
        )

        // Two rows, far apart on the page, both well inside the sheet so the
        // tip's own width is not clipped by the paper's edge.
        val rows = listOf(
            band.top + band.height * 0.30,
            band.top + band.height * 0.68,
        )
        val rowsSeen = ArrayList<Double>()
        for (row in rows) {
            probe.drawAcross(scenario, sheet, row, DRAG_WIDTH_PX, DRAG_STEPS)
            val wax = probe.waxRows(probe.capture(scenario), row.toInt(), DRAG_WIDTH_PX)
            assertTrue(
                "a finger drew along screen row ${row.toInt()} and no wax was " +
                    "left anywhere near it",
                wax.found,
            )
            rowsSeen += wax.center
        }

        // Every mark on its own row, and the two rows told apart: a constant
        // offset large enough to matter would move the first one, and a scale
        // error would collapse the gap between them.
        for (i in rows.indices) {
            val off = rowsSeen[i] - rows[i]
            assertTrue(
                "the mark was drawn ${off.toInt()} px " +
                    (if (off < 0) "above" else "below") +
                    " the finger that drew it: finger row ${rows[i].toInt()}, " +
                    "wax row ${rowsSeen[i].toInt()}, " +
                    "sheet rows ${band.top}..${band.bottom}",
                kotlin.math.abs(off) <= TOLERANCE_PX,
            )
        }
        assertTrue(
            "the two marks came out ${(rowsSeen[1] - rowsSeen[0]).toInt()} px " +
                "apart where the fingers were ${(rows[1] - rows[0]).toInt()} px apart",
            kotlin.math.abs((rowsSeen[1] - rowsSeen[0]) - (rows[1] - rows[0])) <= TOLERANCE_PX,
        )

        val out = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        File(out, "touch_probe.png").outputStream().use { stream ->
            shot.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        scenario.close()
    }

    /**
     * A single tap, held for one beat and lifted: the dot it leaves has to
     * sit under the finger in both directions, not merely on its row.
     *
     * Three taps, spread over the sheet, none of them near the page's own
     * edge: a constant offset in either direction moves all three, and a
     * scale error moves the outer ones apart.
     */
    @Test
    fun aTapIsDrawnExactlyWhereTheFingerTouchedTheGlass() {
        val scenario = probe.launch()
        probe.push(sheet)

        val shot = probe.capture(scenario)
        val band = probe.sheetBand(shot)
        assertTrue(
            "no sheet of paper found on the screen: the top and the bottom of " +
                "the page are what the finger has to land on",
            band.height > shot.height * 0.3,
        )

        val taps = listOf(
            Pair(0.35, 0.30),
            Pair(0.62, 0.52),
            Pair(0.44, 0.74),
        )
        val seen = ArrayList<Pair<Double, Double>>()
        for ((fx, fy) in taps) {
            val x = shot.width * fx
            val y = band.top + band.height * fy
            probe.tap(scenario, sheet, x, y)
            val wax = probe.waxCentre(probe.capture(scenario), x.toInt(), y.toInt())
            assertTrue(
                "a finger tapped at (${x.toInt()}, ${y.toInt()}) and no dot of " +
                    "wax was left near it",
                wax.found,
            )
            seen += Pair(wax.cx, wax.cy)
        }
        for (i in taps.indices) {
            val x = shot.width * taps[i].first
            val y = band.top + band.height * taps[i].second
            val dx = seen[i].first - x
            val dy = seen[i].second - y
            assertTrue(
                "the dot landed ${dx.toInt()} px across and ${dy.toInt()} px down " +
                    "from the finger: finger (${x.toInt()}, ${y.toInt()}), " +
                    "wax (${seen[i].first.toInt()}, ${seen[i].second.toInt()}), " +
                    "sheet rows ${band.top}..${band.bottom}",
                kotlin.math.hypot(dx, dy) <= TOLERANCE_DOT_PX,
            )
        }
        scenario.close()
    }

    private companion object {
        /**
         * The tip's own half width, a page pixel of rounding, and the wax's
         * own broken edge. It is deliberately tight: the bug this holds at bay
         * moved a mark by a sixth of the sheet, which is more than a hundred
         * pixels on any phone, so a tolerance that has to be measured against
         * the sheet is the wrong test.
         */
        const val TOLERANCE_PX = 12.0

        /**
         * A single dot's own tolerance. The dot is one tip wide and the wax
         * breaks at its edge, so its measured center wobbles by a couple of
         * pixels; anything a finger could feel is more than that.
         */
        const val TOLERANCE_DOT_PX = 8.0

        /** How long the synthetic drag is, in pixels. */
        const val DRAG_WIDTH_PX = 420

        /** How many move events the drag is made of. */
        const val DRAG_STEPS = 12
    }
}
