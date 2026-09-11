package io.github.muntasimulhaque.crayoner.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The app's two touches. Haptics need no permission and follow the system's
 * own haptics setting, so a device set to silent is silent.
 *
 * They are deliberately few, and neither is a judgment: a mark landing on
 * the paper (the child's hand did something, and that is what a haptic is
 * for), and the seal going down on a finished picture. A toy that buzzes at
 * every touch is a toy a parent turns off.
 */
class Haptics(private val view: View) {

    /** A mark landing anywhere on the page. */
    fun paint() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** The seal, pressed down onto a picture the child says is done. */
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
