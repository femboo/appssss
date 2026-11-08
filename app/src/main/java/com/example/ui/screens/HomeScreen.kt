package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.components.GlowPlayButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackItem
import com.example.ui.components.ambientGlow
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    repository: LeafRepository,
    playerManager: AudioPlayerManager,
    onNavigateToLibrary: () -> Unit,
    onNavigateToLiked: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val user by repository.currentUser.collectAsState()
    val recommendations by repository.recommendations.collectAsState()
    val myWaveTracks by repository.myWaveTracks.collectAsState()
    val localDiscoverTracks by repository.localDiscoverTracks.collectAsState()
    val likedTracks by repository.likedTracks.collectAsState()
    val playlists by repository.playlists.collectAsState()
    val likedIds by repository.likedTrackIds.collectAsState()
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    // Determine greeting based on device time
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..16 -> "Добрый день"
        in 17..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    val displayName = user?.username?.takeIf { it.isNotBlank() } ?: user?.login ?: ""
    val greetingText = if (displayName.isNotBlank()) "$greeting, $displayName" else greeting

    LaunchedEffect(Unit) {
        if (recommendations.isEmpty()) repository.refreshRecommendations()
        if (myWaveTracks.isEmpty()) repository.refreshMyWave()
        if (localDiscoverTracks.isEmpty()) repository.refreshLocalDiscover()
    }

    fun startWave() {
        if (myWaveTracks.isNotEmpty()) {
            playerManager.playTrack(myWaveTracks.first(), myWaveTracks)
        } else {
            scope.launch {
                val res = repository.refreshMyWave()
                res.getOrNull()?.firstOrNull()?.let { track ->
                    playerManager.playTrack(track, res.getOrNull())
                }
            }
        }
    }

    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .ambientGlow(accent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header: big title + greeting, refresh on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Главное",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = {
                    scope.launch {
                        repository.refreshRecommendations()
                        repository.refreshMyWave()
                        repository.refreshLocalDiscover()
                    }
                },
                modifier = Modifier.testTag("home_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить рекомендации",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // "Эхо" hero — the equivalent of Yandex's "Моя волна": big gradient card + round play button
        EchoHero(onClick = { startWave() })

        Spacer(modifier = Modifier.height(14.dp))

        // Quick access tiles (Любимые / Медиатека)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickTile(
                modifier = Modifier.weight(1f),
                title = "Любимые",
                subtitle = "${likedTracks.size} треков",
                onClick = onNavigateToLiked,
                cover = {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_likes_banner),
                        contentDescription = "Любимые",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )
            QuickTile(
                modifier = Modifier.weight(1f),
                title = "Медиатека",
                subtitle = "${playlists.size} плейлистов",
                onClick = onNavigateToLibrary,
                cover = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookshelf),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recommendations shelf
        SectionHeader(
            title = "Рекомендации",
            subtitle = "Собрано специально для вас"
        )

        if (recommendations.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(recommendations, key = { it.id }) { track ->
                    RecommendationCard(
                        track = track,
                        isCurrentTrack = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        onClick = {
                            playerManager.playTrack(track, recommendations)
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Загрузка персональных рекомендаций...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // "Популярно в сети"
        SectionHeader(
            title = "Популярно в сети",
            subtitle = "Треки, которые слушают прямо сейчас"
        )

        val feedTracks = if (localDiscoverTracks.isNotEmpty()) localDiscoverTracks else myWaveTracks
        if (feedTracks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                feedTracks.take(8).forEach { track ->
                    TrackItem(
                        track = track,
                        isCurrentTrack = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        isLiked = likedIds.contains(track.id),
                        onTrackClick = {
                            playerManager.playTrack(track, feedTracks)
                        },
                        onLikeClick = {
                            scope.launch { repository.toggleLike(track) }
                        },
                        onAddToPlaylistClick = {
                            onAddToPlaylist(track)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

/** Big "Моя волна"-like banner with concentric rings and a glowing play button. */
@Composable
private fun EchoHero(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.45f), surface)
                )
            )
            .clickable(onClick = onClick)
            .testTag("home_echo_hero")
    ) {
        // Decorative "sound wave" rings on the right side
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.86f, size.height * 0.5f)
            for (i in 1..5) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f - i * 0.022f),
                    radius = size.height * 0.22f * i,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp, end = 110.dp)
        ) {
            Text(
                text = "ЭХО",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Бесконечный поток",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Персональный поток треков под ваше настроение",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 2
            )
        }

        GlowPlayButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
            size = 64.dp
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = "Запустить Эхо",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Compact tile: square cover on the left, two lines of text — like Yandex "Мне нравится". */
@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cover: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp)) { cover() }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Flat shelf item: big rounded cover, title and artist below (no card background). */
@Composable
fun RecommendationCard(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    modifier = Modifier.size(34.dp)
                )
            }

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = if (isPlaying) "Играет" else "Пауза",
                        tint = activeColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.displayTitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isCurrentTrack) activeColor else MaterialTheme.colorScheme.onSurface,
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
}
