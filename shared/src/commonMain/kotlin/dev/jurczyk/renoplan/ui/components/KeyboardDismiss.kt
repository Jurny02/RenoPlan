package dev.jurczyk.renoplan.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Hides the keyboard when the user taps outside a field or drags content, as iOS apps do. The iOS
 * number pad has no key to close it, so without this some screens could not get rid of it.
 * Taps that a child handles (buttons, fields) are not affected.
 */
fun Modifier.hideKeyboardOnTapOrScroll(focusManager: FocusManager): Modifier =
    pointerInput(focusManager) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        .nestedScroll(
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput && available.y != 0f) focusManager.clearFocus()
                    return Offset.Zero
                }
            },
        )
