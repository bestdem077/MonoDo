package com.example.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object HapticFeedbackHelper {
    fun performSelectionHaptic(view: View, haptic: HapticFeedback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun performTaskCompletionHaptic(view: View, haptic: HapticFeedback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun performTaskCreationHaptic(view: View, haptic: HapticFeedback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun performDeleteHaptic(view: View, haptic: HapticFeedback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun performSubtaskToggleHaptic(view: View, haptic: HapticFeedback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun performSwipeActionHaptic(view: View, haptic: HapticFeedback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
