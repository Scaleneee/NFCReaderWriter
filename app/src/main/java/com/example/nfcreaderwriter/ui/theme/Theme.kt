package com.example.nfcreaderwriter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NavyLight,
    onPrimary = Navy,
    primaryContainer = Navy,
    onPrimaryContainer = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = DarkBorder,
    onSurfaceVariant = DarkSecondary,
    outline = DarkBorder,
    error = DestructiveRed,
    onError = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = PureWhite,
    primaryContainer = NavyLight,
    onPrimaryContainer = Navy,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = Color(0xFFF5F6F7),
    onSurfaceVariant = LightSecondary,
    outline = LightBorder,
    error = DestructiveRed,
    onError = PureWhite
)

@Composable
fun NFCReaderWriterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
