package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Track

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
    val activeColor = MaterialTheme.colorScheme.primary
    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrentTrack) activeColor.copy(alpha = 0.16f) else Color.Transparent,
        label = "trackBgColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onTrackClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("track_item_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!track.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.displayTitle,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
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
                color = if (isCurrentTrack) activeColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Real source icon
                val srcLower = (track.source ?: (if (track.id.startsWith("yt_")) "ytmusic" else if (track.id.startsWith("ym_")) "yandex_music" else if (track.id.startsWith("up_")) "upload" else "soundcloud")).lowercase()
                when {
                    srcLower.contains("soundcloud") || (!track.id.startsWith("yt_") && !track.id.startsWith("ym_") && !track.id.startsWith("up_")) -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_soundcloud),
                            contentDescription = "SoundCloud",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    srcLower.contains("yt") || srcLower.contains("youtube") -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_youtube),
                            contentDescription = "YouTube Music",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    srcLower.contains("ym") || srcLower.contains("yandex") -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_yandex),
                            contentDescription = "Яндекс Музыка",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Text(
                    text = track.displayArtist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (track.duration != null && track.duration > 0) {
                    Text(
                        text = " • ${track.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }

        // Like button
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .size(38.dp)
                .testTag("like_button_${track.id}")
        ) {
            Image(
                painter = painterResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                contentDescription = if (isLiked) "Удалить из любимых" else "Добавить в любимые",
                modifier = Modifier.size(20.dp)
            )
        }

        if (trailingAction != null) {
            trailingAction()
        } else {
            // Add to playlist
            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("add_to_playlist_${track.id}")
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "Добавить в плейлист",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
