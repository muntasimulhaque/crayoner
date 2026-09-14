package io.github.muntasimulhaque.crayoner

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Draft
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.ui.CRAYON_TIP_FRACTION
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.PlayScreen
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Where the wax lands, measured on a real screen.
 *
 * The pure mapping is held by `core/PagePointTest` with no device. This is
 * the other half of the same rule, and the half a unit test cannot see: a
 * mark drawn through the shipped UI has to sit on the row of the paper it was
 * given, on the real sheet, at the real laid-out size, after the whole
 * Compose tree has had its say.
 *
 * The measurement does not need to find the sheet. One mark is drawn straight
 * across the paper from page x 0.1 to 0.9, so the wax it leaves is exactly
 * 0.8 of the sheet's width long: the capture tells the test how wide the
 * sheet is, and the sheet's own scale then says where the mark's row has to
 * be. The row is checked against the paper's top edge, found by walking up a
 * column of the sheet that the mark does not cross, so the test holds the
 * mark's place on the page and not its place on the screen.
 */
@RunWith(AndroidJUnit4::class)
class TouchProbeTest {

    private val render = mutableStateOf<@Composable () -> Unit>({})

    @Test
    fun aMarkIsDrawnOnTheRowOfThePaperItWasGiven() {
        val page: Page = Pages.byId("sail") ?: error("the sail page is gone")
        val markRow = 0.60
        val markLeft = 0.10
        val markRight = 0.90
        val across = (0..20).map { Vec2(markLeft + it * (markRight - markLeft) / 20.0, markRow) }

        val scenario = launch()
        scenario.onActivity { activity ->
            render.value = {
                PlayScreen(
                    state = Screen.Coloring(
                        page = page,
                        draft = Draft.of(Progress.Empty.with(Stroke(Crayons.RED, across))),
                        crayon = Crayons.RED,
                    ),
                    soundOn = true,
                    onStrokeStart = {}, onStrokeMove = {}, onStrokeEnd = {},
                    onPick = {}, onErase = {}, onOpenBox = {}, onUndo = {},
                    onRedo = {}, onHome = {}, onSound = {}, onPeek = {},
                )
            }
        }
        settle()

        val shot = capture(scenario)
        val sheet = sheetBounds(shot)
        assertTrue("no sheet of paper found in the capture", sheet.found)
        assertTrue(
            "the sheet is ${sheet.width.toInt()} by ${sheet.height.toInt()} px, " +
                "which is not the page's own proportion",
            kotlin.math.abs(sheet.height / sheet.width - 1.2) < 0.03,
        )

        val wax = waxBounds(shot, sheet.width)
        assertTrue(
            "the mark is not on the screen at all (the whole capture has no wax in it)",
            wax.found,
        )

        // Page units are isotropic: the mark's row is [markRow] of the page's
        // own width below the paper's top edge, and nothing else.
        val landed = (wax.centerY - sheet.top) / sheet.width
        assertTrue(
            "the mark landed at $landed of the page's width, not at $markRow: " +
                "wax y ${wax.top}..${wax.bottom} px, paper top ${sheet.top} px, " +
                "sheet ${sheet.width.toInt()} by ${sheet.height.toInt()} px",
            kotlin.math.abs(landed - markRow) < 0.02,
        )

        // And the mark is the length it was given: from page x 0.10 to 0.90,
        // plus the tip's own radius at either end.
        val tip = sheet.width * CRAYON_TIP_FRACTION
        val expectedRun = sheet.width * (markRight - markLeft) + tip
        assertTrue(
            "the mark is ${wax.width.toInt()} px across, not ${expectedRun.toInt()}",
            kotlin.math.abs(wax.width - expectedRun) < sheet.width * 0.06,
        )

        val out = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        File(out, "touch_probe.png").outputStream().use { stream ->
            shot.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        scenario.close()
    }

    /** A box measured in the capture. */
    private open class Box {
        var found = false
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = -1
        var bottom = -1
        val width: Double get() = (right - left + 1).toDouble()
        val height: Double get() = (bottom - top + 1).toDouble()
        val centerY: Double get() = (top + bottom) / 2.0
    }

    /** The wax in the capture, as a box. */
    private class WaxBounds : Box()

    /**
     * The sheet of paper, found by its own edge rather than by its contents.
     *
     * A column through the page can no longer be relied on to find the top:
     * the pictures have printed lines and clouds above the mark, and a walk up
     * a column stops on the first one it meets, which held this probe at page
     * 0.25 of a sheet that was right in front of it.
     *
     * The paper is a bright rectangle on the desk, so its own top edge is the
     * first row of the longest unbroken band of rows that each carry a long
     * run of paper across them. Contiguity is what separates the sheet from
     * the coins in the bar and the capsule on the desk: they are the same
     * color as the paper, and they are separate bands of rows, so the tallest
     * single band is the sheet and nothing else. No printed line inside a
     * picture can imitate a run of paper as wide as the sheet.
     */
    private fun sheetBounds(shot: Bitmap): Box {
        data class Row(val y: Int, val start: Int, val end: Int)
        val paperRows = ArrayList<Row>()
        val shortest = shot.width * 0.35
        for (y in 0 until shot.height) {
            var run = 0
            var best = 0
            var start = -1
            var bestStart = -1
            var bestEnd = -1
            for (x in 0 until shot.width) {
                if (isPaper(shot.getPixel(x, y))) {
                    if (run == 0) start = x
                    run++
                    if (run > best) {
                        best = run
                        bestStart = start
                        bestEnd = x
                    }
                } else {
                    run = 0
                }
            }
            if (best >= shortest) paperRows += Row(y, bestStart, bestEnd)
        }
        // The tallest unbroken band of paper rows: the sheet, and not the
        // bar above it or the capsule below it, which are the same color and
        // a couple of rows tall each.
        var bestTop = -1
        var bestBottom = -1
        var start = 0
        while (start < paperRows.size) {
            var end = start
            while (end + 1 < paperRows.size && paperRows[end + 1].y == paperRows[end].y + 1) end++
            val top = paperRows[start].y
            val bottom = paperRows[end].y
            if (bestTop < 0 || bottom - top > bestBottom - bestTop) {
                bestTop = top
                bestBottom = bottom
            }
            start = end + 1
        }
        val box = Box()
        if (bestTop < 0) return box
        box.found = true
        box.top = bestTop
        box.bottom = bestBottom
        box.left = Int.MAX_VALUE
        box.right = -1
        for (row in paperRows) {
            if (row.y < bestTop || row.y > bestBottom) continue
            if (row.start < box.left) box.left = row.start
            if (row.end > box.right) box.right = row.end
        }
        return box
    }

    /**
     * The mark, measured from the capture.
     *
     * The window is not empty of wax the way the paper is: the sample button
     * in the bar holds a finished picture, and on the sail page that picture
     * carries a red hull, and the capsule below holds the crayon in hand,
     * which is red here too. Telling them apart is not a matter of position
     * (the layout differs in every shape the app has) but of length: the mark
     * is one straight line across most of the sheet, so the rows that hold it
     * are the only rows in the window with a run of wax near the sheet's own
     * width, and a picture shrunk into a button is nowhere near that long.
     */
    private fun waxBounds(shot: Bitmap, sheetWidth: Double): WaxBounds {
        val bounds = WaxBounds()
        val shortest = sheetWidth * 0.5
        for (y in 0 until shot.height) {
            var run = 0
            var best = 0
            var start = -1
            var bestStart = -1
            var bestEnd = -1
            for (x in 0 until shot.width) {
                if (isWax(shot.getPixel(x, y))) {
                    if (run == 0) start = x
                    run++
                    if (run > best) {
                        best = run
                        bestStart = start
                        bestEnd = x
                    }
                } else {
                    run = 0
                }
            }
            if (best < shortest) continue
            bounds.found = true
            if (bestStart < bounds.left) bounds.left = bestStart
            if (bestEnd > bounds.right) bounds.right = bestEnd
            if (y < bounds.top) bounds.top = y
            if (y > bounds.bottom) bounds.bottom = y
        }
        return bounds
    }

    /** True when the pixel is the book's own red wax, not paper and not ink. */
    private fun isWax(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // Crayons.RED is #EE204D. The band is wide enough for the wax grain
        // and for antialiasing, and narrow enough that no sky, sea, cloud or
        // printed line in the book can be mistaken for it.
        return r > 170 && g < 140 && b > 40 && b < 170 && r - g > 60
    }

    /** True when the pixel is the sheet's own paper. */
    private fun isPaper(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // CrayonerColors.Card is #FFFDF8, and the desk under it is #F6EFE3,
        // which is a clear step darker on the red channel.
        return r >= 250 && g >= 248 && b >= 240
    }

    private fun launch(): ActivityScenario<ComponentActivity> {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
            activity.setContent { CrayonerTheme { render.value() } }
        }
        settle()
        return scenario
    }

    private fun capture(scenario: ActivityScenario<ComponentActivity>): Bitmap {
        lateinit var shot: Bitmap
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            val latch = CountDownLatch(1)
            PixelCopy.request(
                activity.window,
                bitmap,
                { latch.countDown() },
                Handler(Looper.getMainLooper()),
            )
            latch.await(10, TimeUnit.SECONDS)
            shot = bitmap
        }
        return shot
    }

    private fun settle() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(900)
    }
}
