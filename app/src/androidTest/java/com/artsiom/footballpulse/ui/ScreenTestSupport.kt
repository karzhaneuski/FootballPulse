package com.artsiom.footballpulse.ui

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry

internal fun string(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

// LoadingState has no text or test tag, so the indeterminate spinner is the only stable hook.
internal fun ComposeContentTestRule.onLoadingIndicator(): SemanticsNodeInteraction =
    onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))

/** Records invocations of a screen callback so tests can assert what was passed. */
internal class CallRecorder<T> {
    val calls = mutableListOf<T>()
    operator fun invoke(value: T) {
        calls += value
    }
}
