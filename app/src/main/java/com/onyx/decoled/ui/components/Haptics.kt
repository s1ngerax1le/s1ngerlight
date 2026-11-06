package com.onyx.decoled.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object S1ngerHaptics {
    private val segmentTickType: HapticFeedbackType = resolveType("SegmentTick", HapticFeedbackType.TextHandleMove)
    private val segmentFrequentTickType: HapticFeedbackType = resolveType("SegmentFrequentTick", HapticFeedbackType.TextHandleMove)
    private val toggleOnType: HapticFeedbackType = resolveType("ToggleOn", HapticFeedbackType.LongPress)
    private val toggleOffType: HapticFeedbackType = resolveType("ToggleOff", HapticFeedbackType.TextHandleMove)
    private val confirmType: HapticFeedbackType = resolveType("Confirm", HapticFeedbackType.LongPress)
    private val rejectType: HapticFeedbackType = resolveType("Reject", HapticFeedbackType.LongPress)

    private fun resolveType(name: String, fallback: HapticFeedbackType): HapticFeedbackType {
        return try {
            val getter = HapticFeedbackType.Companion::class.java.getDeclaredMethod("get$name")
            getter.invoke(HapticFeedbackType.Companion) as HapticFeedbackType
        } catch (_: Throwable) {
            try {
                val field = HapticFeedbackType::class.java.getField(name)
                field.get(null) as HapticFeedbackType
            } catch (_: Throwable) {
                fallback
            }
        }
    }

    fun segmentTick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(segmentTickType)
    }

    fun segmentFrequentTick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(segmentFrequentTickType)
    }

    fun toggleOn(haptic: HapticFeedback) {
        haptic.performHapticFeedback(toggleOnType)
    }

    fun toggleOff(haptic: HapticFeedback) {
        haptic.performHapticFeedback(toggleOffType)
    }

    fun confirm(haptic: HapticFeedback) {
        haptic.performHapticFeedback(confirmType)
    }

    fun reject(haptic: HapticFeedback) {
        haptic.performHapticFeedback(rejectType)
    }
}
