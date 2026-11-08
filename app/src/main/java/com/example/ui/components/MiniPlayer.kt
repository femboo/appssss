package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Track

/**
 * Yandex-Music-style floating mini player: rounded card with cover, title/artist,
 * heart, round play/pause and a thin progress line along the bottom edge.
 * A faint accent tint on the left is borrowed from the desktop player glass.
 */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onLikeClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("mini_player"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.16f), androidx.compose.ui.graphics.Color.Transparent)
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_music_note),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.displayTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.displayArtist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Like button
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                        ),
                        contentDescription = "Любимое",
                        tint = if (isLiked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Play / Pause — round, inverted like Yandex Music
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable(onClick = onPlayPauseClick)
                        .testTag("mini_player_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                            ),
                            contentDescription = if (isPlaying) "Пауза" else "Играть",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Следующий",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Thin progress line along the bottom edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(accent)
                )
            }
        }
    }
}
