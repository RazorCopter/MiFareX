package de.syss.MifareClassicTool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
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
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceDim = ColorTokens.DarkSurfaceDim,
    surfaceBright = ColorTokens.DarkSurfaceBright,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceHighDark,
    surfaceContainerHighest = ColorTokens.DarkSurfaceHighest,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
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
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceDim = ColorTokens.LightSurfaceDim,
    surfaceBright = ColorTokens.LightSurfaceBright,
    surfaceContainerLowest = ColorTokens.LightSurfaceLowest,
    surfaceContainerLow = SurfaceLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceHighLight,
    surfaceContainerHighest = ColorTokens.LightSurfaceHighest,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private object ColorTokens {
    val DarkSurfaceDim = androidx.compose.ui.graphics.Color(0xFF071116)
    val DarkSurfaceBright = androidx.compose.ui.graphics.Color(0xFF2C3B43)
    val DarkSurfaceHighest = androidx.compose.ui.graphics.Color(0xFF22363F)
    val LightSurfaceDim = androidx.compose.ui.graphics.Color(0xFFD5DCDF)
    val LightSurfaceBright = androidx.compose.ui.graphics.Color(0xFFF5FAFC)
    val LightSurfaceLowest = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val LightSurfaceHighest = androidx.compose.ui.graphics.Color(0xFFDCE5E9)
}

val MctxShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Branded Material 3 theme. Dynamic colors stay disabled to preserve safety semantics. */
@Composable
fun MctxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = MctxTypography,
        shapes = MctxShapes,
        content = content
    )
}
