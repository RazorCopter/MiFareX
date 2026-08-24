package de.syss.MifareClassicTool.ui.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Centralised haptic-feedback patterns for NFC operations.
 * Each function maps a semantic event to a distinct vibration style so
 * the operator can tell outcomes apart without looking at the screen.
 */
object MctxHaptics {

    /** Single light pulse — tag detected by the NFC adapter. */
    fun tagDetected(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** Double light tap — write completed successfully. */
    fun writeSuccess(view: View?) {
        view?.let {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            it.postDelayed({
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }, 120)
        }
    }

    /** Long buzz — write failed or error occurred. */
    fun writeError(view: View?) {
        view?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    /** Medium tap — confirmation of user action (e.g. button press, dialog confirm). */
    fun confirm(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Light tick — generic UI interaction feedback. */
    fun tick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
