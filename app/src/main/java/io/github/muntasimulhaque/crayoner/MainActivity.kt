package io.github.muntasimulhaque.crayoner

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.muntasimulhaque.crayoner.host.ColoringHost
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.HomeScreen
import io.github.muntasimulhaque.crayoner.ui.PlayScreen

/** The most the crayon box lets system font scaling grow its words. */
private const val MAX_FONT_SCALE = 1.3f

class MainActivity : ComponentActivity() {

    private val host: ColoringHost by lazy {
        ViewModelProvider(
            this,
            viewModelFactory {
                initializer { ColoringHost(application) }
            },
        )[ColoringHost::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        policeDebugBuild()
        keepBarsHidden()
        setContent {
            val screen by host.screen.collectAsStateWithLifecycle()
            val shelf by host.shelf.collectAsStateWithLifecycle()
            // A toy box, not a document: text follows the system font
            // setting, but only so far. Past this cap the words stop fitting
            // the fixed play surfaces and begin to overlap them, which
            // serves nobody, so the whole UI composes under a bounded
            // density instead.
            val system = LocalDensity.current
            val capped = remember(system.density, system.fontScale) {
                Density(density = system.density, fontScale = minOf(system.fontScale, MAX_FONT_SCALE))
            }
            CompositionLocalProvider(LocalDensity provides capped) {
                CrayonerTheme {
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(
                            shelf = shelf,
                            onOpen = host::open,
                            onSound = host::setSound,
                        )
                        is Screen.Coloring -> {
                            // Back is always the gentle answer: it puts down
                            // what is open rather than leaving the app.
                            BackHandler(enabled = true) {
                                when {
                                    s.peeking -> host.setPeek(false)
                                    s.confirmingClear -> host.askClear(false)
                                    s.celebrating -> host.home()
                                    else -> host.home()
                                }
                            }
                            PlayScreen(
                                state = s,
                                soundOn = shelf.soundOn,
                                onStrokeStart = host::beginStroke,
                                onStrokeMove = host::moveStroke,
                                onStrokeEnd = host::endStroke,
                                onPick = host::pickCrayon,
                                onHome = host::home,
                                onSound = host::setSound,
                                onPeek = host::setPeek,
                                onAskClear = host::askClear,
                                onClear = host::clearPage,
                                onColorArea = host::colorArea,
                            )
                        }
                    }
                }
            }
        }
    }

    /** Debug builds police themselves: disk and network on the main thread,
     *  leaks, unclosed resources. Release builds never see this. */
    private fun policeDebugBuild() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build(),
        )
    }

    override fun onStart() {
        super.onStart()
        keepBarsHidden()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) keepBarsHidden()
    }

    // A child's book owns the whole screen. Edge-to-edge is enforced at this
    // targetSdk, so without some handling the screen draws under the status
    // and navigation bars. Both are hidden for a surface with no
    // distractions; they only flash back transiently on a swipe.
    private fun keepBarsHidden() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
