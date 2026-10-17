package dev.jurczyk.renoplan.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Hides the keyboard when the user taps outside a field or drags the content vertically, as iOS apps
 * do. The iOS number pad has no key to close it, so without this some screens could not get rid of it.
 *
 * Only real finger drags count: scrolling a focused field into view above the keyboard is also a
 * scroll, and reacting to it would close the keyboard as soon as it opened. Touches are observed,
 * never consumed, so buttons, fields and scrolling work as before.
 */
fun Modifier.hideKeyboardOnTapOrDrag(focusManager: FocusManager): Modifier =
    pointerInput(focusManager) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        .pointerInput(focusManager) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var pressed = true
                var dragged = false
                while (pressed && !dragged) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                    val move = change?.let { it.position - down.position }
                    dragged = move != null && abs(move.y) > viewConfiguration.touchSlop && abs(move.y) > abs(move.x)
                    pressed = change?.pressed == true
                }
                if (dragged) focusManager.clearFocus()
            }
        }
