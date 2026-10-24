package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Track
import com.example.ui.theme.HeartRed
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.LeafGreenGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TrackItem(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    onTrackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrentTrack) LeafGreenGlow else Color.Transparent,
        label = "trackBgColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onTrackClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("track_item_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!track.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.displayTitle,
                    modifier = Modifier.size(52.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LeafGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.displayTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isCurrentTrack) LeafGreen else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Source badge if available
                track.source?.let { src ->
                    val badgeLabel = when (src.lowercase()) {
                        "soundcloud" -> "SC"
                        "ytmusic" -> "YT"
                        "yandex_music" -> "YM"
                        "upload" -> "Файл"
                        else -> src.take(2).uppercase()
                    }
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = LeafGreen,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = track.displayArtist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (track.duration != null && track.duration > 0) {
                    Text(
                        text = " • ${track.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextTertiary
                    )
                }
            }
        }

        // Like button
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("like_button_${track.id}")
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Удалить из любимых" else "Добавить в любимые",
                tint = if (isLiked) HeartRed else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (trailingAction != null) {
            trailingAction()
        } else {
            // Add to playlist
            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("add_to_playlist_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Добавить в плейлист",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
