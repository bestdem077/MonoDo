package com.example.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object HapticFeedbackHelper {
    /**
     * Subtle, satisfying haptic bump when completing or uncompleting a main task.
     */
    fun performTaskCompletionHaptic(view: View?, composeHaptic: HapticFeedback? = null) {
        try {
            if (view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val performed = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    if (!performed) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            } else {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /**
     * Light, crisp tick haptic feedback for subtasks and checklists.
     */
    fun performSubtaskToggleHaptic(view: View?, composeHaptic: HapticFeedback? = null) {
        try {
            if (view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val performed = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (!performed) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } else {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}
