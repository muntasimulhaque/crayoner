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
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.host.ShelfState
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.HomeScreen
import io.github.muntasimulhaque.crayoner.ui.PlayScreen
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
     * The eight store captures, the Play listing maximum per form factor.
     * The set leads with the shelf, shows the page from bare lines to a
     * finished picture, includes the crayon being picked and the sample
     * held up, and closes on the finished picture.
     */
    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()
        shot(scenario, outDir, "01_home") {
            HomeScreen(shelf = ShelfState(), onOpen = {}, onSound = {})
        }
        shot(scenario, outDir, "02_home_progress") {
            HomeScreen(
                shelf = ShelfState(
                    finished = setOf("sail", "rainbow", "house"),
                    drafts = mapOf("balloon" to "envelope:FFD94B3F"),
                ),
                onOpen = {},
                onSound = { },
            )
        }
        shot(scenario, outDir, "03_blank") {
            PlayScreen(
                state = emptyState("sail"),
                soundOn = true,
                onTap = {},
                onPick = {},
                onHome = {},
                onSound = {},
                onPeek = {},
                onAskClear = {},
                onClear = {},
            )
        }
        shot(scenario, outDir, "04_picked") {
            PlayScreen(
                state = emptyState("sail").copy(crayon = Crayons.SKY),
                soundOn = true,
                onTap = {},
                onPick = {},
                onHome = {},
                onSound = {},
                onPeek = {},
                onAskClear = {},
                onClear = {},
            )
        }
        shot(scenario, outDir, "05_coloring") {
            PlayScreen(
                state = halfState("kite"),
                soundOn = true,
                onTap = {},
                onPick = {},
                onHome = {},
                onSound = {},
                onPeek = {},
                onAskClear = {},
                onClear = {},
            )
        }
        shot(scenario, outDir, "06_peek") {
            PlayScreen(
                state = halfState("rainbow").copy(peeking = true),
                soundOn = true,
                onTap = {},
                onPick = {},
                onHome = {},
                onSound = {},
                onPeek = {},
                onAskClear = {},
                onClear = {},
            )
        }
        shot(scenario, outDir, "07_most") {
            PlayScreen(
                state = almostState("house"),
                soundOn = false,
                onTap = {},
                onPick = {},
                onHome = {},
                onSound = {},
                onPeek = {},
                onAskClear = {},
                onClear = {},
            )
        }
        shot(scenario, outDir, "08_done") {
            PlayScreen(
                state = finishedState("icecream"),
                soundOn = true,
                onTap = {},
                onPick = {},
                onHome = {},
                onSound = {},
                onPeek = {},
                onAskClear = {},
                onClear = {},
            )
        }
        scenario.close()
    }

    // -- Fixtures -----------------------------------------------------------

    private fun page(id: String): Page = Pages.byId(id) ?: error("no page $id")

    private fun emptyState(id: String) = Screen.Coloring(page = page(id), progress = Progress.Empty)

    /**
     * The first few areas done, the rest still bare, one crayon in hand. The
     * child colors where the picture asks, so every done area wears its own
     * color and the crayon in hand is the next area's.
     */
    private fun halfState(id: String): Screen.Coloring {
        val page = page(id)
        val done = page.regions.indices.take((page.regionCount / 2).coerceAtLeast(2))
        val progress = done.fold(Progress.Empty) { acc, index ->
            acc.with(index, page.regions[index].fillArgb)
        }
        val next = page.regions.getOrNull(done.size)?.fillArgb ?: Crayons.RED
        return Screen.Coloring(page = page, progress = progress, crayon = next)
    }

    /** Everything but the last area, the crayon already wearing its color. */
    private fun almostState(id: String): Screen.Coloring {
        val page = page(id)
        val last = page.regionCount - 1
        val progress = (0 until last).fold(Progress.Empty) { acc, index ->
            acc.with(index, page.regions[index].fillArgb)
        }
        return Screen.Coloring(
            page = page,
            progress = progress,
            crayon = page.regions.last().fillArgb,
        )
    }

    private fun finishedState(id: String): Screen.Coloring {
        val page = page(id)
        val progress = page.regions.indices.fold(Progress.Empty) { acc, index ->
            acc.with(index, page.regions[index].fillArgb)
        }
        return Screen.Coloring(page = page, progress = progress, celebrating = true)
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
        const val SETTLE_MS = 700L
    }
}
