package dev.jurczyk.renoplan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Caption above a form field or a group of options. */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * A single-line text field with its [label] above it, an optional [suffix] (e.g. a unit) and
 * [supportingText] under it, shown as an error when [isError].
 */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            suffix =
                if (suffix != null) {
                    { Text(suffix, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    null
                },
            isError = isError,
            supportingText = if (supportingText != null) ({ Text(supportingText) }) else null,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = keyboardOptions,
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            // The label sits outside the field, so screen readers get it from here.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

/** A decimal number field with a unit suffix (e.g. "m"), showing [errorText] when [isError]. */
@Composable
fun UnitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    isError: Boolean,
    errorText: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    LabeledTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        suffix = unit,
        isError = isError,
        supportingText = if (isError) errorText else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}
