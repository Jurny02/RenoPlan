package dev.jurczyk.renoplan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = BackgroundLight,
        onSurface = OnBackgroundLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
    )

private val DarkColors =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        onTertiaryContainer = OnTertiaryContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = BackgroundDark,
        onSurface = OnBackgroundDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
    )

// Tabular figures keep columns of amounts and areas aligned.
private const val TABULAR_FIGURES = "tnum"

private fun TextStyle.styled(
    weight: FontWeight,
    letterSpacing: Float? = null,
): TextStyle =
    copy(
        fontWeight = weight,
        fontFeatureSettings = TABULAR_FIGURES,
        letterSpacing = letterSpacing?.sp ?: this.letterSpacing,
    )

private val AppTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.styled(FontWeight.Bold, letterSpacing = -1f),
            displayMedium = displayMedium.styled(FontWeight.Bold, letterSpacing = -1f),
            displaySmall = displaySmall.styled(FontWeight.Bold, letterSpacing = -0.5f),
            headlineLarge = headlineLarge.styled(FontWeight.Bold, letterSpacing = -0.5f),
            headlineMedium = headlineMedium.styled(FontWeight.Bold, letterSpacing = -0.5f),
            headlineSmall = headlineSmall.styled(FontWeight.Bold, letterSpacing = -0.25f),
            titleLarge = titleLarge.styled(FontWeight.Bold),
            titleMedium = titleMedium.styled(FontWeight.SemiBold),
            titleSmall = titleSmall.styled(FontWeight.SemiBold),
            bodyLarge = bodyLarge.styled(FontWeight.Normal),
            bodyMedium = bodyMedium.styled(FontWeight.Normal),
            bodySmall = bodySmall.styled(FontWeight.Normal),
            labelLarge = labelLarge.styled(FontWeight.SemiBold),
            labelMedium = labelMedium.styled(FontWeight.SemiBold),
            labelSmall = labelSmall.styled(FontWeight.Medium),
        )
    }

private val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun RenoPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
