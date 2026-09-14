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
        val wax = waxBounds(shot)
        assertTrue(
            "the mark is not on the screen at all (the whole capture has no wax in it)",
            wax.found,
        )

        // The mark spans 0.8 of the page's width, so the capture measures the
        // sheet for us: no assumed size, no assumed layout, no assumed
        // density, and the same arithmetic in every shape the app has.
        val sheetWidth = wax.width / (markRight - markLeft)
        assertTrue(
            "the wax is ${wax.width} px across, which is not a sheet's width",
            sheetWidth > shot.width * 0.25,
        )

        // The paper's top edge, from a column of the sheet the mark does not
        // cross, so the mark itself cannot move the line it is measured from.
        val probeX = (wax.left - 0.05 * sheetWidth).toInt()
        assertTrue("the mark starts at the very edge of the capture", probeX > 0)
        val paperTop = paperTopAt(shot, probeX, wax.centerY.toInt())
        assertTrue("no paper above the mark on the sheet", paperTop >= 0)

        // Page units are isotropic: the mark's row is [markRow] of the page's
        // own width below the paper's top edge, and nothing else.
        val landed = (wax.centerY - paperTop) / sheetWidth
        assertTrue(
            "the mark landed at $landed of the page's width, not at $markRow: " +
                "wax y ${wax.top}..${wax.bottom} px, paper top $paperTop px, " +
                "sheet width ${sheetWidth.toInt()} px",
            kotlin.math.abs(landed - markRow) < 0.02,
        )

        val out = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        File(out, "touch_probe.png").outputStream().use { stream ->
            shot.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        scenario.close()
    }

    /** The bounding box of the wax in the capture: [found] false when none. */
    private class WaxBounds {
        var found = false
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = -1
        var bottom = -1
        val width: Double get() = (right - left + 1).toDouble()
        val centerY: Double get() = (top + bottom) / 2.0
    }

    /**
     * The mark, measured from the capture.
     *
     * The window is not empty of wax the way the paper is: the sample button
     * in the bar holds a finished picture, and on the sail page that picture
     * carries a red hull. Telling the two apart is not a matter of position
     * (the layout differs in every shape the app has) but of shape: the mark
     * under test is one straight line 0.8 of the sheet wide, so the rows that
     * hold it are the only rows in the window with a very long run of wax in
     * them, and a picture shrunk into a button is nowhere near that long.
     */
    private fun waxBounds(shot: Bitmap): WaxBounds {
        val rows = ArrayList<IntArray>(shot.height)
        var longestRun = 0
        for (y in 0 until shot.height) {
            val row = IntArray(shot.width)
            var run = 0
            var best = 0
            var started = -1
            var bestStart = -1
            for (x in 0 until shot.width) {
                if (isWax(shot.getPixel(x, y))) {
                    if (run == 0) started = x
                    run++
                    if (run > best) {
                        best = run
                        bestStart = started
                    }
                } else {
                    run = 0
                }
            }
            row[0] = best
            row[1] = bestStart
            rows += row
            if (best > longestRun) longestRun = best
        }
        val bounds = WaxBounds()
        // A row belongs to the mark when its own run is most of the mark's
        // widest run. Anything shorter is a picture, a printed line or a
        // stray pixel, and none of them may move the mark's own box.
        val markRows = longestRun * 0.5
        for (y in rows.indices) {
            val run = rows[y][0]
            if (run < markRows) continue
            bounds.found = true
            val start = rows[y][1]
            if (start < bounds.left) bounds.left = start
            if (start + run - 1 > bounds.right) bounds.right = start + run - 1
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

    /** The paper's own top edge, walking up [x] from just above [from]. */
    private fun paperTopAt(shot: Bitmap, x: Int, from: Int): Int {
        if (x !in 0 until shot.width) return -1
        var y = from.coerceIn(0, shot.height - 1)
        // Up to the paper first, since the column may start on a printed line
        // or on the mark's own row.
        var seen = 0
        while (y > 0 && !isPaper(shot.getPixel(x, y))) {
            y--
            seen++
            if (seen > 400) return -1
        }
        while (y > 0 && isPaper(shot.getPixel(x, y - 1))) y--
        return if (isPaper(shot.getPixel(x, y))) y else -1
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
