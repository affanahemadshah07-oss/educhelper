package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class StudySemanticColors(
    val safe: Color,
    val safeContainer: Color,
    val onSafeContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color
)

val LocalStudyColors = staticCompositionLocalOf {
    StudySemanticColors(
        safe = EmeraldSafe,
        safeContainer = EmeraldSafeContainer,
        onSafeContainer = OnEmeraldSafeContainer,
        warning = AmberFocus,
        warningContainer = AmberFocusContainer,
        onWarningContainer = OnAmberFocusContainer,
        danger = CrimsonAlert,
        dangerContainer = CrimsonAlertContainer,
        onDangerContainer = OnCrimsonAlertContainer
    )
}

private val LightColorScheme = lightColorScheme(
    primary = OxfordIndigo,
    onPrimary = Color.White,
    primaryContainer = OxfordIndigoContainer,
    onPrimaryContainer = OnOxfordIndigoContainer,
    secondary = EmeraldSafe,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSafeContainer,
    onSecondaryContainer = OnEmeraldSafeContainer,
    tertiary = AmberFocus,
    onTertiary = Color.White,
    tertiaryContainer = AmberFocusContainer,
    onTertiaryContainer = OnAmberFocusContainer,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonAlertContainer,
    onErrorContainer = OnCrimsonAlertContainer,
    background = PaperBackground,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    surfaceContainer = PaperSurfaceContainer,
    surfaceContainerLow = PaperSurface,
    surfaceContainerHigh = PaperSurfaceVariant,
    outline = HairlineOutline,
    outlineVariant = PaperSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = OnMidnightPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = OnMidnightPrimaryContainer,
    secondary = EmeraldDark,
    onSecondary = Color(0xFF042F21),
    secondaryContainer = EmeraldDarkContainer,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberDark,
    onTertiary = Color(0xFF3E2400),
    tertiaryContainer = AmberDarkContainer,
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = CrimsonDark,
    onError = Color(0xFF450A0A),
    errorContainer = CrimsonDarkContainer,
    onErrorContainer = Color(0xFFFEE2E2),
    background = MidnightBackground,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightTextSecondary,
    surfaceContainer = MidnightSurfaceContainer,
    surfaceContainerLow = MidnightSurface,
    surfaceContainerHigh = MidnightSurfaceVariant,
    outline = MidnightOutline,
    outlineVariant = MidnightSurfaceVariant
)

private val StudyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        StudySemanticColors(
            safe = EmeraldDark,
            safeContainer = EmeraldDarkContainer,
            onSafeContainer = Color(0xFFD1FAE5),
            warning = AmberDark,
            warningContainer = AmberDarkContainer,
            onWarningContainer = Color(0xFFFEF3C7),
            danger = CrimsonDark,
            dangerContainer = CrimsonDarkContainer,
            onDangerContainer = Color(0xFFFEE2E2)
        )
    } else {
        StudySemanticColors(
            safe = EmeraldSafe,
            safeContainer = EmeraldSafeContainer,
            onSafeContainer = OnEmeraldSafeContainer,
            warning = AmberFocus,
            warningContainer = AmberFocusContainer,
            onWarningContainer = OnAmberFocusContainer,
            danger = CrimsonAlert,
            dangerContainer = CrimsonAlertContainer,
            onDangerContainer = OnCrimsonAlertContainer
        )
    }

    CompositionLocalProvider(LocalStudyColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudyShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    StudyHubTheme(darkTheme = darkTheme, content = content)
}
