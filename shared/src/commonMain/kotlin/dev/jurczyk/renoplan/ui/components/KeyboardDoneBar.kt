package dev.jurczyk.renoplan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.keyboard_done

/**
 * A "Done" bar that sits on top of the keyboard while it is open and closes it. The iOS number pad has
 * no return key, and Compose cannot attach a native accessory view to it. Use it as a screen's
 * Scaffold `bottomBar`, so the content is laid out above it.
 */
@Composable
fun KeyboardDoneBar(modifier: Modifier = Modifier) {
    // `WindowInsets.isImeVisible` is Android-only; the keyboard inset works on both platforms.
    if (WindowInsets.ime.getBottom(LocalDensity.current) == 0) return
    val focusManager = LocalFocusManager.current
    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth().imePadding()) {
        KeyboardDoneRow(onDone = { focusManager.clearFocus() })
    }
}

@Composable
private fun KeyboardDoneRow(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDone) { Text(stringResource(Res.string.keyboard_done)) }
    }
}

@Preview
@Composable
private fun KeyboardDoneRowPreview() {
    RenoPlanTheme {
        Surface(tonalElevation = 3.dp) { KeyboardDoneRow(onDone = {}) }
    }
}
