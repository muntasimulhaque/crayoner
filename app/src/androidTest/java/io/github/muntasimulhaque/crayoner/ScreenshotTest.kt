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
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.host.ShelfState
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.HomeScreen
import io.github.muntasimulhaque.crayoner.ui.PlayScreen
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Compose renders of the shipped UI, for the Play Store listing and for
 * the human drift check after UI changes (AGENTS.md, Build).
 *
 * These are ordinary state renders, not a live playthrough: each screen is
 * handed straight to its composable inside a bare activity, which is only
 * possible because no composable in this app takes a ViewModel. The harness
 * deliberately avoids the compose test rule and everything under it: no
 * touch injection and no semantics queries are needed to render and copy
 * pixels, and dropping that machinery keeps these captures working on
 * whatever framework image the app targets, forever.
 *
 * The coloring captures show marks the way a hand really makes them: long
 * sweeps across an area, wobbling the way a wrist does, stopping where a
 * three year old would stop, and overlapping the printed lines now and then,
 * because that is what coloring in looks like.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private fun resolveOutDir(): File {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        if (path != null) {
            val dir = File(path)
            if (dir.isDirectory || dir.mkdirs()) return dir
            // Cold-booted emulators can lag mounting shared storage; fall
            // back rather than fail.
        }
        return File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath,
        ).apply { mkdirs() }
    }

    /** One activity hosts every scene: each is a state change pushed into it. */
    private val render = mutableStateOf<@Composable () -> Unit>({})

    private fun launch(): ActivityScenario<ComponentActivity> {
        var lastError: RuntimeException? = null
        repeat(3) { attempt ->
            try {
                val scenario = ActivityScenario.launch(ComponentActivity::class.java)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                    val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    activity.setContent {
                        CrayonerTheme {
                            render.value()
                        }
                    }
                }
                settle()
                return scenario
            } catch (e: RuntimeException) {
                lastError = e
                Thread.sleep(5000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("could not launch the host activity")
    }

    private fun push(block: @Composable () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { render.value = block }
        settle()
    }

    private fun settle() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(SETTLE_MS)
    }

    /**
     * The eight store captures. The set leads with the wall, walks the page
     * from bare lines to a finished picture, and shows the two things a hand
     * holds: the box of colors, and the rubber at work.
     */
    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()
        shot(scenario, outDir, "01_home", waitForWax = true) {
            HomeScreen(shelf = ShelfState(), onOpen = {})
        }
        shot(scenario, outDir, "02_blank") {
            play(blankState("sail"))
        }
        shot(scenario, outDir, "03_box") {
            play(blankState("kite").copy(crayon = Crayons.SKY_BLUE, boxOpen = true))
        }
        shot(scenario, outDir, "04_coloring") {
            play(coloredState("kite", crayon = Crayons.RED))
        }
        shot(scenario, outDir, "05_rubbed") {
            play(erasedState("house"))
        }
        shot(scenario, outDir, "06_peek") {
            play(coloredState("rainbow", crayon = Crayons.BLUE).copy(peeking = true))
        }
        shot(scenario, outDir, "07_whole") {
            play(wholeState("icecream", crayon = Crayons.CARNATION_PINK))
        }
        shot(scenario, outDir, "08_keep") {
            play(coloredState("tree", crayon = Crayons.GREEN).copy(asking = true))
        }
        scenario.close()
    }

    /** One coloring screen, with every callback a no-op. */
    @Composable
    private fun play(state: Screen.Coloring, soundOn: Boolean = true) {
        PlayScreen(
            state = state,
            soundOn = soundOn,
            onStrokeStart = {},
            onStrokeMove = {},
            onStrokeEnd = {},
            onPick = {},
            onErase = {},
            onOpenBox = {},
            onUndo = {},
            onRedo = {},
            onHome = {},
            onSound = {},
            onPeek = {},
            onKeep = {},
            onStartFresh = {},
            onDismissAsk = {},
        )
    }

    /** Render one state, wait for the screen to stop moving, and copy the
     *  window's own pixels out.
     *
     * The wall's pictures are made after the cards are already on screen,
     * so a capture taken a fixed moment after a state is pushed can catch
     * the wall half drawn: outline cards that fill in a breath later. Two
     * copies in a row that match mean the screen has stopped changing, and
     * whatever is in it is the state rather than a moment of it. It also
     * waits out the sample button's opening breath, which is an animation
     * on a blank page and nothing else.
     */
    private fun shot(
        scenario: ActivityScenario<ComponentActivity>,
        outDir: File,
        name: String,
        waitForWax: Boolean = false,
        block: @Composable () -> Unit,
    ) {
        push(block)
        var previous: String? = null
        var bitmap: Bitmap? = null
        for (attempt in 0 until SETTLE_ATTEMPTS) {
            Thread.sleep(SETTLE_STEP_MS)
            val shot = captureWindow(scenario)
            bitmap = shot
            // A window that has not drawn at all yet is the desk and nothing
            // else, and two frames of bare desk look as settled as two frames
            // of anything: a cold emulator warming up its first app can hold
            // the frame back for as long as it likes, so the wait is for a
            // frame with something in it before the two-frame rule applies.
            if (isBareDesk(shot)) {
                previous = null
                continue
            }
            // The wall is the one scene whose content arrives after its
            // frame does: sixteen pictures of wax are made in the background
            // while the cards are already on screen, and a wall of outline
            // cards is a perfectly still picture. Two matching frames would
            // therefore call it settled long before it is. Waiting for the
            // frame to be colored the way a drawn wall is colored is what
            // makes this capture the wall rather than the first breath of it,
            // on any speed of machine.
            if (waitForWax && waxFraction(shot) < ENOUGH_WAX) {
                previous = null
                continue
            }
            val signature = signatureOf(shot)
            if (signature == previous) break
            previous = signature
        }
        val settled = bitmap ?: return
        assertFalse("the $name capture is an empty desk", isBareDesk(settled))
        if (waitForWax) {
            // A run whose window never drew (a cold emulator, a process still
            // warming up) would otherwise hand back a perfectly still wall of
            // empty cards and call it a capture. That is the one failure a
            // person would have to notice by eye, so it fails here instead.
            assertTrue(
                "the wall capture has no pictures in it",
                waxFraction(settled) >= ENOUGH_WAX,
            )
        }
        File(outDir, "$name.png").outputStream().use { out ->
            settled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /**
     * True when a frame is nothing but the desk: the window has not drawn its
     * content yet. Every scene the harness captures has a sheet, a bar, a
     * tray or a plate in it, so a frame of one flat color is never a scene,
     * whatever the emulator is busy doing. The desk's own color is the one
     * the app draws, with a little room for the emulator's color management.
     */
    private fun isBareDesk(bitmap: Bitmap): Boolean {
        var desk = 0
        var looked = 0
        val step = 8
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (kotlin.math.abs(r - 0xF3) <= 6 &&
                    kotlin.math.abs(g - 0xE9) <= 6 &&
                    kotlin.math.abs(b - 0xD8) <= 6
                ) {
                    desk++
                }
                looked++
                x += step
            }
            y += step
        }
        return looked == 0 || desk * 100 >= looked * 99
    }

    /**
     * How much of a frame is plainly colored, which on the wall means
     * pictures rather than the paper and print they start as.
     *
     * The desk, the card stock and the ink are all near-neutrals, so a wide
     * gap between a pixel's brightest and darkest channels is wax and little
     * else. A frame is read on a grid rather than pixel by pixel, both to
     * keep the wait cheap and because a card's own edges are not the
     * question: a wall that is three quarters drawn is not a wall.
     */
    private fun waxFraction(bitmap: Bitmap): Double {
        var colored = 0
        var looked = 0
        val step = 4
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (maxOf(r, g, b) - minOf(r, g, b) > 45) colored++
                looked++
                x += step
            }
            y += step
        }
        return if (looked == 0) 0.0 else colored.toDouble() / looked
    }

    /** A cheap fingerprint of one frame: a grid of its own pixels. */
    private fun signatureOf(bitmap: Bitmap): String {
        val signature = StringBuilder()
        val step = 23
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                signature.append(bitmap.getPixel(x, y).toUInt().toString(16))
                x += step
            }
            y += step
        }
        return signature.toString()
    }

    /**
     * The activity's own window pixels: the truth the child actually sees.
     *
     * The wait for the copy must happen OFF the main thread. PixelCopy
     * answers on the main looper, so waiting for it from inside an activity
     * callback deadlocks every copy until its timeout and then hands back
     * whatever the render thread managed to fill in the meantime: on a cold
     * window that is an empty, transparent bitmap, which is how the wall
     * once came back as a blank page. Waiting on this thread lets the
     * listener land, and if the copy itself cannot be taken (a surface
     * that has not drawn yet), the decor view is drawn in software instead,
     * which is exactly the pixels the scene is made of.
     */
    private fun captureWindow(scenario: ActivityScenario<ComponentActivity>): Bitmap {
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
        if (!latch.await(10, TimeUnit.SECONDS)) {
            // A copy that never answered at all: draw the tree by hand.
            drawInSoftware(scenario, bitmap)
        }
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

    private companion object {
        /**
         * How long a scene is given to draw before it is copied out. The
         * first scene is the expensive one: the wall of sixteen pictures
         * has to build its wax tiles and render its visible cards before a
         * single frame exists, and a cold emulator draws that frame in
         * seconds rather than milliseconds. A copy taken too early is a
         * transparent PNG, which is worse than a slower run.
         */
        const val SETTLE_MS = 2200L

        /** How often a scene is looked at while it is settling, and how
         *  many looks it gets: a wall whose pictures are still being made
         *  changes for a few seconds after its cards are up. The budget is
         *  generous on purpose: every scene is a still state, so waiting
         *  longer can only catch the state more completely, and a software
         *  emulator under load can take twice the time a quiet one does to
         *  lay down a page of wax. */
        const val SETTLE_STEP_MS = 300L
        const val SETTLE_ATTEMPTS = 160

        /**
         * The share of a home frame that has to be plainly colored before the
         * wall counts as drawn. Measured off the store sets themselves: a
         * drawn wall on any of the three form factors is between 51 and 59
         * percent colored, and a wall with half its cards still bare is 41,
         * so the line sits above the half-drawn wall and below every drawn
         * one. It is a share and not a count because the form factors carry
         * very different numbers of cards and pixels.
         */
        const val ENOUGH_WAX = 0.45
    }
}
