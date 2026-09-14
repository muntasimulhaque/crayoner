package io.github.muntasimulhaque.crayoner

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
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
import io.github.muntasimulhaque.crayoner.core.Strokes
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
 * Where the wax lands when a real finger draws, measured on a real screen.
 *
 * The pure mapping is held by `core/PagePointTest` with no device, and the
 * renderer's side of it by the sheets. Neither can see the thing the child
 * actually sees: a touch on the glass, through the shipped Compose tree, on
 * the real laid-out sheet, with the real density, in whichever of the app's
 * two layouts the screen happens to be. That is what this probe measures, and
 * it measures the only question worth asking about it: a mark drawn by a
 * finger that moved along one row of the glass must lie on that row.
 *
 * So it does not compute any geometry at all. It puts a finger down at a
 * known screen position, drags it straight across without changing its row,
 * lifts it, and then looks for the wax: the wax has to be on the row the
 * finger was on. It does it twice, at two rows far apart, so no constant
 * offset and no scale error can hide between the two. The check needs to know
 * nothing about the page, the sheet's size, the sheet's place or the layout,
 * which is why it holds in every shape the app has.
 *
 * With the bug this was written for (the finger's y divided by the frame's
 * height and read as a page unit, on a sheet 1.2 times taller than it is
 * wide), the wax on a phone landed a sixth of the sheet above the finger on
 * both rows, and the probe fails on both.
 */
@RunWith(AndroidJUnit4::class)
class TouchProbeTest {

    private val render = mutableStateOf<@Composable () -> Unit>({})
    private val page: Page = Pages.byId("sail") ?: error("the sail page is gone")

    /**
     * The whole page's own state, held here rather than in a host, so the
     * probe drives the shipped callbacks with the domain's own rules and no
     * ViewModel is needed. The rules are core's, so a mark made here is made
     * the way the app makes one.
     */
    private class Sheet {
        var draft: Draft = Draft.Empty
        var crayon: Long? = null
        var live: io.github.muntasimulhaque.crayoner.core.Stroke? = null

        fun begin(at: Vec2) {
            crayon = Crayons.RED
            live = Strokes.dot(Crayons.RED, at)
        }

        fun move(at: Vec2) {
            live = live?.let { Strokes.extend(it, at) }
        }

        fun end() {
            val mark = live ?: return
            live = null
            draft = draft.color(mark.color, mark.points)
        }
    }

    @Test
    fun aMarkIsDrawnOnTheRowOfTheGlassTheFingerDrewOn() {
        val scenario = launch()
        val sheet = Sheet()
        pushState(sheet)

        val shot = capture(scenario)
        val band = sheetBand(shot)
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
            drawAcross(scenario, sheet, row)
            val wax = waxRows(capture(scenario), row.toInt(), DRAG_WIDTH_PX)
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

    /** How far a mark may sit from the finger that drew it, in pixels. */
    private companion object {
        /**
         * The tip's own half width, a page pixel of rounding, and the wax's
         * own broken edge. It is deliberately tight: the bug this holds at bay
         * moved a mark by a sixth of the sheet, which is more than a hundred
         * pixels on any phone, so a tolerance that has to be measured against
         * the sheet is the wrong test.
         */
        const val TOLERANCE_PX = 12.0

        /** How long the synthetic drag is, in pixels. */
        const val DRAG_WIDTH_PX = 420

        /** How many move events the drag is made of. */
        const val DRAG_STEPS = 12
    }

    /**
     * Drag a finger straight across the screen on one row, and lift it.
     *
     * The touches are dispatched from [ActivityScenario.onActivity], which is
     * already on the main thread, so the events go in on the same thread the
     * app reads them on and the drag is one uninterrupted gesture with no
     * frame boundary in the middle of it.
     */
    private fun drawAcross(
        scenario: ActivityScenario<ComponentActivity>,
        sheet: Sheet,
        row: Double,
    ) {
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val cx = decor.width / 2f
            val y = row.toFloat()
            val downTime = SystemClock.uptimeMillis()
            decor.dispatchTouchEvent(
                event(downTime, downTime, MotionEvent.ACTION_DOWN, cx, y),
            )
            for (step in 1..DRAG_STEPS) {
                val x = cx + (step - 1) * DRAG_WIDTH_PX / DRAG_STEPS
                decor.dispatchTouchEvent(
                    event(downTime, downTime + step, MotionEvent.ACTION_MOVE, x, y),
                )
            }
            decor.dispatchTouchEvent(
                event(downTime, downTime + DRAG_STEPS + 1, MotionEvent.ACTION_UP, cx, y),
            )
        }
        settle()
        pushState(sheet)
    }

    private fun event(downTime: Long, at: Long, action: Int, x: Float, y: Float): MotionEvent {
        val event = MotionEvent.obtain(downTime, at, action, x, y, 0)
        // The finger's own size, so the app is read with one pointer and no
        // pressure, exactly as a real touch is.
        event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN)
        return event
    }

    /** Hand the sheet's own marks to the shipped screen. */
    private fun pushState(sheet: Sheet) {
        render.value = {
            PlayScreen(
                state = Screen.Coloring(
                    page = page,
                    draft = sheet.draft,
                    crayon = sheet.crayon,
                    live = sheet.live,
                    marks = sheet.draft.progress.strokes.size.toLong(),
                ),
                soundOn = false,
                onStrokeStart = { sheet.begin(it) },
                onStrokeMove = { sheet.move(it) },
                onStrokeEnd = { sheet.end() },
                onPick = {}, onErase = {}, onOpenBox = {}, onUndo = {},
                onRedo = {}, onHome = {}, onSound = {}, onPeek = {},
            )
        }
        settle()
    }

    /** A vertical band of the screen that is not the desk: the sheet. */
    private class Band {
        var found = false
        var top = 0
        var bottom = 0
        val height: Double get() = (bottom - top + 1).toDouble()
    }

    /**
     * The sheet's own rows, found down the middle of the screen.
     *
     * The desk is one color and everything a child looks at is not it, so the
     * longest unbroken run of not-desk rows down the center column is the
     * sheet: the bar above it holds a picture button but is mostly desk, and
     * the capsule below it is a row of coins the same way. Nothing about the
     * page's contents can hide this, because a picture is not desk either.
     */
    private fun sheetBand(shot: Bitmap): Band {
        val cx = shot.width / 2
        var start = -1
        var bestStart = -1
        var bestEnd = -1
        for (y in 0 until shot.height) {
            if (!isDesk(shot.getPixel(cx, y))) {
                if (start < 0) start = y
                if (bestStart < 0 || y - start > bestEnd - bestStart) {
                    bestStart = start
                    bestEnd = y
                }
            } else {
                start = -1
            }
        }
        val band = Band()
        if (bestStart < 0) return band
        band.found = true
        band.top = bestStart
        band.bottom = bestEnd
        return band
    }

    /** True when the pixel is the desk the whole app sits on. */
    private fun isDesk(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // CrayonerColors.Desk is #F6EFE3. The band is narrow: the paper is
        // #FFFDF8 and the cardboard is #EFE1C6, and both are outside it.
        return kotlin.math.abs(r - 0xF6) <= 3 &&
            kotlin.math.abs(g - 0xEF) <= 3 &&
            kotlin.math.abs(b - 0xE3) <= 3
    }

    /** Where the wax is, near the row a finger drew on. */
    private class Wax {
        var found = false
        var top = 0
        var bottom = 0
        val center: Double get() = (top + bottom) / 2.0
    }

    /**
     * The wax the drag left, found near [row].
     *
     * The window is not empty of the crayon's own color: the sample button
     * holds a finished picture, and the capsule below holds the crayon in
     * hand. The search is bounded to a window around the row the finger drew
     * on, which is what separates the mark from the two pictures, and it
     * counts only rows that carry a run of wax as long as the drag itself,
     * which is what separates it from a printed line.
     */
    private fun waxRows(shot: Bitmap, row: Int, runPx: Int): Wax {
        val wax = Wax()
        val from = (row - SEARCH_PX).coerceAtLeast(0)
        val to = (row + SEARCH_PX).coerceAtMost(shot.height - 1)
        val shortest = (runPx * 0.6).toInt()
        for (y in from..to) {
            var run = 0
            var best = 0
            for (x in 0 until shot.width) {
                if (isWax(shot.getPixel(x, y))) {
                    run++
                    if (run > best) best = run
                } else {
                    run = 0
                }
            }
            if (best < shortest) continue
            if (!wax.found) {
                wax.found = true
                wax.top = y
            }
            wax.bottom = y
        }
        return wax
    }

    /** How far around the finger's row the wax is looked for. */
    private val SEARCH_PX = 140

    /** True when the pixel is the book's own red wax, not paper and not ink. */
    private fun isWax(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // Crayons.RED is #EE204D. The band is wide enough for the wax grain
        // and for antialiasing, and narrow enough that no sky, sea, cloud or
        // printed line in the book can be mistaken for it. The sail page's
        // own reds are printed lines and a hull, not a band of wax this wide.
        return r > 170 && g < 140 && b > 40 && b < 175 && r - g > 60
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
        Thread.sleep(700)
    }
}
