package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.components.GlowPlayButton
import com.example.ui.components.GradientCover
import com.example.ui.components.TrackItem
import com.example.ui.components.ambientGlow
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    repository: LeafRepository,
    playerManager: AudioPlayerManager,
    onBack: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val likedIds by repository.likedTrackIds.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    fun loadTracks() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val res = repository.getPlaylistTracks(playlist.id)
            isLoading = false
            if (res.isSuccess) {
                tracks = res.getOrNull() ?: emptyList()
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "Не удалось загрузить треки"
            }
        }
    }

    LaunchedEffect(playlist.id) {
        loadTracks()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .ambientGlow(accentColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("playlist_detail_back")) {
                Icon(
                    painter = painterResource(R.drawable.ic_backarrow),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "${tracks.size} треков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { loadTracks() }) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Обновить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero: gradient cover + big round play button (Yandex-style)
        if (tracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientCover(
                    modifier = Modifier.size(96.dp),
                    corner = 16.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bookshelf),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Плейлист",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tracks.size} треков",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                GlowPlayButton(
                    modifier = Modifier
                        .clickable { playerManager.playTrack(tracks.first(), tracks) }
                        .testTag("playlist_play_all"),
                    size = 60.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = "Слушать плейлист",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }

                tracks.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Плейлист пуст",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Найдите треки в поиске или в любимых и добавьте их в этот плейлист",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(tracks, key = { it.id }) { track ->
                            TrackItem(
                                track = track,
                                isCurrentTrack = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                isLiked = likedIds.contains(track.id),
                                onTrackClick = {
                                    playerManager.playTrack(track, tracks)
                                },
                                onLikeClick = {
                                    scope.launch {
                                        repository.toggleLike(track)
                                    }
                                },
                                onAddToPlaylistClick = {
                                    onAddToPlaylist(track)
                                },
                                trailingAction = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.removeTrackFromPlaylist(playlist.id, track.id)
                                                tracks = tracks.filter { it.id != track.id }
                                            }
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_trash),
                                            contentDescription = "Удалить из плейлиста",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
