package dev.jurczyk.renoplan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme

/** The app's card: a slightly raised surface with a hairline border. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = appCardColors(),
        border = appCardBorder(),
        content = content,
    )
}

/** A tappable [AppCard]. */
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = appCardColors(),
        border = appCardBorder(),
        content = content,
    )
}

@Composable
private fun appCardColors() =
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
private fun appCardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

/** A brand-coloured card for the one number that matters most on a screen, e.g. a total cost. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = HERO_BORDER_ALPHA)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

private const val HERO_BORDER_ALPHA = 0.6f

/** Small uppercase caption above a value or a group of cards, e.g. "ŁĄCZNY KOSZT". */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
    )
}

/** Label above a big number, e.g. "POWIERZCHNIA" / "75,40 m²". */
@Composable
fun StatValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

/**
 * Large title with an optional subtitle, used instead of a top app bar on the start screens of the
 * bottom bar tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .padding(start = 24.dp, top = 20.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(content = actions)
    }
}

/** A thick, rounded progress bar. */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth().height(6.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Single choice between a few options, drawn as a pill with the selected option filled. */
@Composable
fun <T> PillToggle(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: (T) -> Boolean = { true },
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val enabled = isEnabled(option)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(option) },
                        ).padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color =
                        when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
                        },
                )
            }
        }
    }
}

private const val DISABLED_ALPHA = 0.38f

@Preview
@Composable
private fun SurfacesPreview() {
    RenoPlanTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(title = "RenoPlan", subtitle = "Planowanie wykończenia mieszkania")
            PillToggle(listOf("Za m²", "Za metr"), selected = "Za m²", optionLabel = { it }, onSelect = {})
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatValue(label = "Łączny koszt", value = "1 913,64 zł")
                    AppProgressBar(progress = 0.38f)
                }
            }
            HeroCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Koszt razem", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("582,92 zł", style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}
