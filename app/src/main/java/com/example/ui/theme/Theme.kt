package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Yandex-Music-like rounding: soft but not bubbly.
private val LeafShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LeafMusicTheme(
    isDark: Boolean = true,
    accentColor: Color = LeafGreen,
    isWhiteTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(isDark, accentColor, isWhiteTheme) {
        val effectiveAccent = if (!isDark && isWhiteTheme) {
            Color(0xFF18181B) // In light mode, white theme uses sleek deep graphite accent
        } else {
            accentColor
        }

        val onPrimaryColor = if (isDark && isWhiteTheme) {
            Color(0xFF111111)
        } else {
            Color.White
        }

        if (isDark) {
            darkColorScheme(
                primary = effectiveAccent,
                onPrimary = onPrimaryColor,
                primaryContainer = effectiveAccent.copy(alpha = 0.22f),
                onPrimaryContainer = Color.White,
                secondary = effectiveAccent,
                onSecondary = onPrimaryColor,
                secondaryContainer = Color(0xFF1F1F22),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFF38BDF8),
                background = DarkBackground,
                onBackground = TextPrimaryDark,
                surface = DarkSurface,
                onSurface = TextPrimaryDark,
                // Slightly lighter than before so flat rows / tiles read like Yandex Music cards
                surfaceVariant = Color(0xFF1A1A1C),
                onSurfaceVariant = TextSecondaryDark,
                outline = DarkBorder,
                outlineVariant = Color(0x14FFFFFF),
                error = ErrorRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = effectiveAccent,
                onPrimary = onPrimaryColor,
                primaryContainer = effectiveAccent.copy(alpha = 0.16f),
                onPrimaryContainer = Color.Black,
                secondary = effectiveAccent,
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
                outlineVariant = Color(0x14000000),
                error = ErrorRed,
                onError = Color.White
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LeafShapes,
        content = content
    )
}
