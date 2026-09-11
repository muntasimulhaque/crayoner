package io.github.muntasimulhaque.crayoner.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The app's one touch. Haptics need no permission and follow the system's own
 * haptics setting, so a device set to silent is silent.
 *
 * It is deliberately a single, small answer, and it is never a judgment: a
 * mark landing on the paper says the child's hand did something, which is
 * what a haptic is for. A toy that buzzes at every touch is a toy a parent
 * turns off.
 */
class Haptics(private val view: View) {

    /** A mark landing anywhere on the page. */
    fun paint() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
