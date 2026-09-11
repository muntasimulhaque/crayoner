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
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.host.ShelfState
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.HomeScreen
import io.github.muntasimulhaque.crayoner.ui.PlayScreen
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin
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
     * The seven store captures. The set leads with the wall, walks the page
     * from bare lines to a finished picture, and shows the two things a hand
     * holds: the box of colors, and the rubber at work.
     */
    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()
        shot(scenario, outDir, "01_home") {
            HomeScreen(shelf = ShelfState(), onOpen = {}, onSound = {})
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
            onHome = {},
            onSound = {},
            onPeek = {},
        )
    }

    // -- Fixtures -----------------------------------------------------------

    private fun page(id: String): Page = Pages.byId(id) ?: error("no page $id")

    private fun blankState(id: String) = Screen.Coloring(page = page(id), progress = Progress.Empty)

    /**
     * A page a child has been working on: real marks, made the way a hand
     * makes them, in the colors the picture asks for, with the last area
     * still bare so the sheet reads as unfinished.
     */
    private fun coloredState(
        id: String,
        crayon: Long,
        erasing: Boolean = false,
    ): Screen.Coloring {
        val page = page(id)
        val last = page.regionCount - 1
        val strokes = page.regions.indices
            .filter { it != last }
            .flatMap { index -> sweeps(page.regions[index]) }
        return Screen.Coloring(
            page = page,
            progress = Progress(strokes),
            crayon = crayon,
            erasing = erasing,
        )
    }

    /** A page the child has taken all the way: every area has been colored. */
    private fun wholeState(id: String, crayon: Long): Screen.Coloring {
        val page = page(id)
        val strokes = page.regions.indices.flatMap { index -> sweeps(page.regions[index]) }
        return Screen.Coloring(
            page = page,
            progress = Progress(strokes),
            crayon = crayon,
        )
    }

    /**
     * One area, colored in by hand: a few long sweeps across it, each at a
     * slightly different angle and slightly different length, the way an arm
     * covers a shape. Some strokes run a little past the line, because a
     * three year old's do.
     */
    private fun sweeps(region: io.github.muntasimulhaque.crayoner.core.Region): List<Stroke> {
        val b = region.bounds
        if (b.w <= 0.0 || b.h <= 0.0) return emptyList()
        val angle = Math.toRadians(io.github.muntasimulhaque.crayoner.core.Wax.angleDeg(region))
        val dx = kotlin.math.cos(angle)
        val dy = kotlin.math.sin(angle)
        val nx = -dy
        val ny = dx
        val reach = kotlin.math.hypot(b.w, b.h) * 0.55
        val lanes = (minOf(b.w, b.h) / 0.055).toInt().coerceIn(2, 9)
        val seed = kotlin.math.abs(region.id.hashCode())
        return (0 until lanes).map { lane ->
            val t = (lane + 0.5) / lanes - 0.5
            val offX = b.center.x + nx * t * b.w * 0.9
            val offY = b.center.y + ny * t * b.h * 0.9
            val wobble = 0.012 + 0.004 * ((seed + lane) % 3)
            val points = ArrayList<Vec2>(24)
            val steps = 18
            for (i in 0..steps) {
                val u = i.toDouble() / steps
                val spread = (u - 0.5) * 2.0 * reach
                // The wrist wobbles along the sweep, and no two sweeps wobble
                // the same way.
                val w = sin(u * 3.0 * PI + lane) * wobble + sin(u * 7.0 * PI + seed) * wobble * 0.4
                points += Vec2(
                    offX + dx * spread + nx * w,
                    offY + dy * spread + ny * w,
                )
            }
            Stroke(region.fillArgb, points)
        }
    }

    /**
     * A page the child has gone over with the rubber: colored in, then rubbed
     * at, so the capture shows what an eraser mark really leaves behind. The
     * print comes back; the wax does not.
     */
    private fun erasedState(id: String): Screen.Coloring {
        val page = page(id)
        val last = page.regionCount - 1
        val colored = page.regions.indices
            .filter { it != last }
            .flatMap { index -> sweeps(page.regions[index]) }
        val rubbed = listOf(
            Stroke(Stroke.ERASE_COLOR, (0..24).map { i ->
                val t = i / 24.0
                Vec2(0.20 + t * 0.55, 0.42 + kotlin.math.sin(t * 5.0) * 0.05)
            }, erase = true),
        )
        return Screen.Coloring(
            page = page,
            progress = Progress(colored + rubbed),
            crayon = page.regions.last().fillArgb,
            erasing = true,
        )
    }

    /** Render one state, settle it, and copy the window's own pixels out. */
    private fun shot(
        scenario: ActivityScenario<ComponentActivity>,
        outDir: File,
        name: String,
        block: @Composable () -> Unit,
    ) {
        push(block)
        lateinit var bitmap: Bitmap
        scenario.onActivity { activity -> bitmap = captureWindow(activity) }
        File(outDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /** The activity's own window pixels: the truth the child actually sees. */
    private fun captureWindow(activity: ComponentActivity): Bitmap {
        val decor = activity.window.decorView
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        PixelCopy.request(activity.window, bitmap, { result ->
            if (result != PixelCopy.SUCCESS) {
                // Software draw as the fallback path; static scenes render fine.
                decor.draw(android.graphics.Canvas(bitmap))
            }
            latch.countDown()
        }, Handler(Looper.getMainLooper()))
        latch.await(10, TimeUnit.SECONDS)
        return bitmap
    }

    private companion object {
        const val SETTLE_MS = 900L
    }
}
