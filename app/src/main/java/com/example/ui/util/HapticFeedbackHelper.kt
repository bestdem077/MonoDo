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

    /**
     * Confirmatory, uplifting haptic feedback when creating or adding a new task.
     */
    fun performTaskCreationHaptic(view: View?, composeHaptic: HapticFeedback? = null) {
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
     * Distinct, deliberate haptic feedback when deleting a task or subtask.
     */
    fun performDeleteHaptic(view: View?, composeHaptic: HapticFeedback? = null) {
        try {
            if (view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val performed = view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    if (!performed) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            } else {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /**
     * Subtle, tactile click for chip selection, filters, and toggles.
     */
    fun performSelectionHaptic(view: View?, composeHaptic: HapticFeedback? = null) {
        try {
            if (view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val performed = view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
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

