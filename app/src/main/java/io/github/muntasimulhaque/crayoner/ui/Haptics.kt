package io.github.muntasimulhaque.crayoner.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The app's three touches. Haptics need no permission and follow the
 * system's own haptics setting, so a device set to silent is silent.
 *
 * They are deliberately few: a color landing, a color landing right, and a
 * finished picture. A toy that buzzes at every tap is a toy a parent turns
 * off, and the two effects that matter are the two the child earned.
 */
class Haptics(private val view: View) {

    /** A color landing anywhere on the page. */
    fun paint() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** A color landing where the picture asked for it. */
    fun correct() {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(effect)
    }

    /** The whole picture, finished. */
    fun done() {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(effect)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
