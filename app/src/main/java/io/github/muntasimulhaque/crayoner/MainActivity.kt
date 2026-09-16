package io.github.muntasimulhaque.crayoner

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.muntasimulhaque.crayoner.host.ColoringHost
import io.github.muntasimulhaque.crayoner.host.Screen
import io.github.muntasimulhaque.crayoner.ui.CrayonerTheme
import io.github.muntasimulhaque.crayoner.ui.HomeScreen
import io.github.muntasimulhaque.crayoner.ui.PlayScreen

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
            val screen by host.screen
            // The sound switch alone: the shelf's map of drafts is written on
            // every saved mark, and the bar has no business recomposing for
            // that. The shelf itself is read where it is drawn, on the wall.
            val soundOn by host.soundOn
            // Text is composed at the reader's own system text size, whatever
            // it is. The lines that have room to grow use it (the keep
            // question, the wordmark); the wall's picture names are fitted to
            // their cards, which is the most any fixed surface can honestly
            // do. An earlier build capped the scale at 1.3 to stop words
            // spilling over the fixed play surfaces, which also stopped a
            // reader's own setting from mattering at all. See D-087.
            CrayonerTheme {
                when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        shelf = host.shelf.value,
                        onOpen = host::open,
                    )
                    is Screen.Coloring -> {
                        // Back is always the gentle answer: it puts down
                        // what is open rather than leaving the app, and
                        // the box of colors is one of the things that is
                        // open.
                        BackHandler(enabled = true) {
                            when {
                                s.asking -> host.dismissAsk()
                                s.boxOpen -> host.setBoxOpen(false)
                                s.peeking -> host.setPeek(false)
                                else -> host.home()
                            }
                        }
                        PlayScreen(
                            state = s,
                            soundOn = soundOn,
                            live = host.live,
                            onStrokeStart = host::beginStroke,
                            onStrokeMove = host::moveStroke,
                            onStrokeEnd = host::endStroke,
                            onPick = host::pickCrayon,
                            onErase = host::setErasing,
                            onOpenBox = host::setBoxOpen,
                            onUndo = host::undo,
                            onRedo = host::redo,
                            onHome = host::home,
                            onSound = host::setSound,
                            onPeek = host::setPeek,
                            onColorArea = host::colorArea,
                            onKeep = host::keepIt,
                            onStartFresh = host::startFresh,
                            onDismissAsk = host::dismissAsk,
                        )
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
