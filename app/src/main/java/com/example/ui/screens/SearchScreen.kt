package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.components.TrackItem
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    repository: LeafRepository,
    playerManager: AudioPlayerManager,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf<String?>("all") }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val likedIds by repository.likedTrackIds.collectAsState()

    val sources = listOf(
        "all" to "Все",
        "soundcloud" to "SoundCloud",
        "ytmusic" to "YouTube Music",
        "yandex_music" to "Яндекс Музыка"
    )

    val quickQueries = listOf("lofi", "rock", "chillhop", "phonk", "synthwave", "electronic", "ambient")

    fun performSearch(query: String) {
        if (query.trim().isBlank()) return
        focusManager.clearFocus()
        isSearching = true
        searchError = null
        hasSearched = true

        scope.launch {
            val res = repository.search(query.trim(), selectedSource)
            isSearching = false
            if (res.isSuccess) {
                searchResults = res.getOrNull() ?: emptyList()
            } else {
                searchError = res.exceptionOrNull()?.message ?: "Ошибка поиска"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Title
        Text(
            text = "Поиск музыки",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        // Search text input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Треки, исполнители, жанры...", color = TextTertiary) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = LeafGreen)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Очистить", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LeafGreen,
                unfocusedBorderColor = ObsidianCard,
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Source filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sources.forEach { (srcKey, label) ->
                val isSelected = selectedSource == srcKey
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedSource = srcKey
                        if (searchQuery.isNotBlank()) performSearch(searchQuery)
                    },
                    label = {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LeafGreen,
                        containerColor = ObsidianCard
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isSearching -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = LeafGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Ищем треки на сервере...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                searchError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = searchError ?: "Ошибка",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Попробуйте повторить поиск или проверьте адрес сервера",
                            color = TextTertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                hasSearched && searchResults.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Попробуйте изменить запрос или источник",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                !hasSearched -> {
                    // Search discovery & Quick prompts
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp)
                    ) {
                        Text(
                            text = "Популярные запросы",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickQueries.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianCard,
                                    modifier = Modifier.clickable {
                                        searchQuery = tag
                                        performSearch(tag)
                                    }
                                ) {
                                    Text(
                                        text = "# $tag",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = LeafGreen,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Results list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults, key = { it.id }) { track ->
                            TrackItem(
                                track = track,
                                isCurrentTrack = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                isLiked = likedIds.contains(track.id),
                                onTrackClick = {
                                    playerManager.playTrack(track, searchResults)
                                },
                                onLikeClick = {
                                    scope.launch {
                                        repository.toggleLike(track)
                                    }
                                },
                                onAddToPlaylistClick = {
                                    onAddToPlaylist(track)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
