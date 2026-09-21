package com.example.ui.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Calibrated tactile feedback for luxury user interactions in Good Dream Sanctuary.
 * Provides subtle, physical sensations for touch gestures, confirmations, and adjustments.
 */
fun HapticFeedback.performLuxuryClick() {
    runCatching {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

fun HapticFeedback.performLuxurySuccess() {
    runCatching {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

fun HapticFeedback.performLuxuryAdjustment() {
    runCatching {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
