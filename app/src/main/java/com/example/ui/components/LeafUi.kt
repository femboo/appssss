package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpaceGroteskFontFamily

/**
 * Shared building blocks for the "Yandex Music"-style mobile UI
 * (flat lists, big rounded covers, pill chips, section headers with chevron)
 * with a little of the desktop Leaf look: soft accent glow in the background.
 */

/** Soft accent glow at the top of a screen — same idea as the desktop ambient background. */
fun Modifier.ambientGlow(color: Color, strength: Float = 0.20f): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = strength), Color.Transparent),
            center = Offset(size.width * 0.15f, 0f),
            radius = size.width * 1.15f
        )
    )
}

/** Big bold screen title (Space Grotesk, like desktop headings). */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.4).sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Section header: bold title (+ chevron when clickable) and optional grey subtitle. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Pill-shaped filter chip: selected = inverted (light on dark), like in Yandex Music. */
@Composable
fun LeafChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
        label = "chipBg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
        label = "chipFg"
    )
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** Round, glowing accent play button (desktop "accent-glow" look). */
@Composable
fun GlowPlayButton(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Rounded gradient tile used for covers that have no artwork (playlists, likes…). */
@Composable
fun GradientCover(
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.25f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
