package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = LeafTeal,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFF38BDF8),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    error = ErrorRed,
    onError = Color.Black
)

@Composable
fun LeafMusicTheme(
    darkTheme: Boolean = true, // Default to gorgeous dark player aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
