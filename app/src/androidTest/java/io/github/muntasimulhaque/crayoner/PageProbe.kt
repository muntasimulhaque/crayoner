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
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Draft
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Strokes
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.PlayScreen
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The harness a touch probe drives, holding no judgments of its own: a bare
 * activity hosting the shipped screen with one page's state, synthetic
 * fingers on the glass, and a way to find the wax a finger left. The probes
 * own the numbers; this only answers where the wax is.
 *
 * The page's state is held here rather than in a host, so a probe drives the
 * shipped callbacks with the domain's own rules and no ViewModel is needed.
 * The rules are core's, so a mark made here is made the way the app makes
 * one.
 */
internal class PageProbe(private val page: Page) {

    val render = mutableStateOf<@Composable () -> Unit>({})

    /** The whole page's own state, with the domain's own rules. */
    class Sheet {
        var draft: Draft = Draft.Empty
        var crayon: Long? = null
        var live: Stroke? = null

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

    fun launch(): ActivityScenario<ComponentActivity> {
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

    /** Hand the sheet's own marks to the shipped screen. */
    fun push(sheet: Sheet) {
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

    /**
     * Tap the glass at one point and lift, one uninterrupted gesture with no
     * frame boundary inside it.
     */
    fun tap(
        scenario: ActivityScenario<ComponentActivity>,
        sheet: Sheet,
        x: Double,
        y: Double,
    ) {
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val downTime = SystemClock.uptimeMillis()
            decor.dispatchTouchEvent(
                event(downTime, downTime, MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat()),
            )
            decor.dispatchTouchEvent(
                event(downTime, downTime + 4, MotionEvent.ACTION_UP, x.toFloat(), y.toFloat()),
            )
        }
        settle()
        push(sheet)
    }

    /**
     * Drag a finger straight across the screen on one row, and lift it.
     *
     * The touches are dispatched from [ActivityScenario.onActivity], which is
     * already on the main thread, so the events go in on the same thread the
     * app reads them on and the drag is one uninterrupted gesture with no
     * frame boundary in the middle of it.
     */
    fun drawAcross(
        scenario: ActivityScenario<ComponentActivity>,
        sheet: Sheet,
        row: Double,
        widthPx: Int,
        steps: Int,
    ) {
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val cx = decor.width / 2f
            val y = row.toFloat()
            val downTime = SystemClock.uptimeMillis()
            decor.dispatchTouchEvent(
                event(downTime, downTime, MotionEvent.ACTION_DOWN, cx, y),
            )
            for (step in 1..steps) {
                val x = cx + (step - 1) * widthPx / steps
                decor.dispatchTouchEvent(
                    event(downTime, downTime + step, MotionEvent.ACTION_MOVE, x, y),
                )
            }
            decor.dispatchTouchEvent(
                event(downTime, downTime + steps + 1, MotionEvent.ACTION_UP, cx, y),
            )
        }
        settle()
        push(sheet)
    }

    private fun event(downTime: Long, at: Long, action: Int, x: Float, y: Float): MotionEvent {
        val event = MotionEvent.obtain(downTime, at, action, x, y, 0)
        // The finger's own size, so the app is read with one pointer and no
        // pressure, exactly as a real touch is.
        event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN)
        return event
    }

    /** A vertical band of the screen that is not the desk: the sheet. */
    class Band {
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
    fun sheetBand(shot: Bitmap): Band {
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

    /** Where the wax is, near the row a finger drew on. */
    class Wax {
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
    fun waxRows(shot: Bitmap, row: Int, runPx: Int): Wax {
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

    /**
     * The wax a tap left, as a box and its center, in a window around the
     * point the finger touched.
     *
     * The window is narrow enough that the sample button's own picture and
     * the red crayon in the capsule cannot be inside it, and wide enough for
     * a dot the width of the tip wherever the mapping put it. A tap's wax is
     * a compact round dot, so the box's own center is the dot's center.
     */
    class Dot {
        var found = false
        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        val cx: Double get() = (left + right) / 2.0
        val cy: Double get() = (top + bottom) / 2.0
    }

    fun waxCentre(shot: Bitmap, x: Int, y: Int): Dot {
        val dot = Dot()
        val from = (x - DOT_SEARCH_PX).coerceAtLeast(0)
        val to = (x + DOT_SEARCH_PX).coerceAtMost(shot.width - 1)
        val top = (y - DOT_SEARCH_PX).coerceAtLeast(0)
        val bottom = (y + DOT_SEARCH_PX).coerceAtMost(shot.height - 1)
        for (row in top..bottom) {
            for (col in from..to) {
                if (!isWax(shot.getPixel(col, row))) continue
                if (!dot.found) {
                    dot.found = true
                    dot.left = col
                    dot.right = col
                    dot.top = row
                    dot.bottom = row
                } else {
                    dot.left = minOf(dot.left, col)
                    dot.right = maxOf(dot.right, col)
                    dot.top = minOf(dot.top, row)
                    dot.bottom = maxOf(dot.bottom, row)
                }
            }
        }
        return dot
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

    fun capture(scenario: ActivityScenario<ComponentActivity>): Bitmap {
        var width = 1
        var height = 1
        scenario.onActivity { activity ->
            width = activity.window.decorView.width.coerceAtLeast(1)
            height = activity.window.decorView.height.coerceAtLeast(1)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        scenario.onActivity { activity ->
            PixelCopy.request(
                activity.window,
                bitmap,
                { result ->
                    if (result != PixelCopy.SUCCESS) drawInSoftware(scenario, bitmap)
                    latch.countDown()
                },
                Handler(Looper.getMainLooper()),
            )
        }
        // The wait is off the main thread on purpose: PixelCopy answers on
        // the main looper, so waiting from inside an activity callback would
        // time out every copy and hand back an unfilled bitmap.
        if (!latch.await(10, TimeUnit.SECONDS)) drawInSoftware(scenario, bitmap)
        return bitmap
    }

    /** The decor view's pixels, drawn without a graphics surface. */
    private fun drawInSoftware(
        scenario: ActivityScenario<ComponentActivity>,
        bitmap: Bitmap,
    ) {
        scenario.onActivity { activity ->
            activity.window.decorView.draw(android.graphics.Canvas(bitmap))
        }
    }

    fun settle() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(700)
    }

    private companion object {
        /** How far around a tap the dot is looked for. */
        const val DOT_SEARCH_PX = 120

        /** How far around the finger's row the wax is looked for. */
        const val SEARCH_PX = 140
    }
}
