package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun LeafMusicTheme(
    isDark: Boolean = true,
    accentColor: Color = LeafGreen,
    isWhiteTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(isDark, accentColor, isWhiteTheme) {
        val onPrimaryColor = if (isWhiteTheme) Color(0xFF111111) else Color.White
        if (isDark) {
            darkColorScheme(
                primary = accentColor,
                onPrimary = onPrimaryColor,
                primaryContainer = accentColor.copy(alpha = 0.25f),
                onPrimaryContainer = Color.White,
                secondary = accentColor,
                onSecondary = onPrimaryColor,
                secondaryContainer = Color(0xFF18181B),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFF38BDF8),
                background = DarkBackground,
                onBackground = TextPrimaryDark,
                surface = DarkSurface,
                onSurface = TextPrimaryDark,
                surfaceVariant = DarkCard,
                onSurfaceVariant = TextSecondaryDark,
                outline = DarkBorder,
                error = ErrorRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = accentColor,
                onPrimary = onPrimaryColor,
                primaryContainer = accentColor.copy(alpha = 0.25f),
                onPrimaryContainer = Color.Black,
                secondary = accentColor,
                onSecondary = onPrimaryColor,
                secondaryContainer = Color(0xFFE4E4E7),
                onSecondaryContainer = Color.Black,
                tertiary = Color(0xFF0284C7),
                background = LightBackground,
                onBackground = TextPrimaryLight,
                surface = LightSurface,
                onSurface = TextPrimaryLight,
                surfaceVariant = LightCard,
                onSurfaceVariant = TextSecondaryLight,
                outline = LightBorder,
                error = ErrorRed,
                onError = Color.White
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
