package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Track
import com.example.data.model.UserSearchItem
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.components.TrackItem
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
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
    var userResults by remember { mutableStateOf<List<UserSearchItem>>(emptyList()) }
    var isUserSearchMode by remember { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val likedIds by repository.likedTrackIds.collectAsState()
    val searchHistory by repository.searchHistory.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    val quickQueries = listOf("lofi", "chillhop", "phonk", "synthwave", "rock", "ambient")

    // Live search suggestions debouncer
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.startsWith("Leaf:", ignoreCase = true)) {
            isUserSearchMode = true
            showSuggestions = false
            suggestions = emptyList()
        } else {
            isUserSearchMode = false
            if (q.length >= 2 && !hasSearched) {
                delay(250)
                val res = repository.getSuggestions(q)
                suggestions = res.getOrNull()?.filter { it.isNotBlank() && it != q } ?: emptyList()
                showSuggestions = suggestions.isNotEmpty()
            } else {
                suggestions = emptyList()
                showSuggestions = false
            }
        }
    }

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        focusManager.clearFocus()
        showSuggestions = false
        isSearching = true
        searchError = null
        hasSearched = true

        repository.addSearchQuery(trimmed)

        scope.launch {
            if (trimmed.startsWith("Leaf:", ignoreCase = true)) {
                isUserSearchMode = true
                val res = repository.searchUsers(trimmed)
                isSearching = false
                if (res.isSuccess) {
                    userResults = res.getOrNull() ?: emptyList()
                    searchResults = emptyList()
                } else {
                    searchError = res.exceptionOrNull()?.message ?: "Ошибка поиска пользователей"
                }
            } else {
                isUserSearchMode = false
                val res = repository.search(trimmed, selectedSource)
                isSearching = false
                if (res.isSuccess) {
                    searchResults = res.getOrNull() ?: emptyList()
                    userResults = emptyList()
                } else {
                    searchError = res.exceptionOrNull()?.message ?: "Ошибка поиска"
                }
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
            text = "Поиск",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        // Search text input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                hasSearched = false
            },
            placeholder = { Text("Треки, артисты или Leaf: имя...", color = TextTertiary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = accentColor)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        hasSearched = false
                        suggestions = emptyList()
                        showSuggestions = false
                    }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Очистить", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = ObsidianCard,
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        // Live Suggestions Overlay / List
        AnimatedVisibility(visible = showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    suggestions.take(5).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = item
                                    executeSearch(item)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Source Filters & User Search Quick Button
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // "Leaf: " Quick Toggle Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isUserSearchMode || searchQuery.startsWith("Leaf:", ignoreCase = true)) accentColor else ObsidianCard,
                modifier = Modifier.clickable {
                    searchQuery = "Leaf: "
                    isUserSearchMode = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = if (isUserSearchMode) Color.Black else accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Пользователи (Leaf:)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isUserSearchMode) Color.Black else TextPrimary
                    )
                }
            }

            // Sources with REAL ICONS
            val sources = listOf(
                "all" to ("Все" to null),
                "soundcloud" to ("SoundCloud" to R.drawable.ic_soundcloud),
                "ytmusic" to ("YouTube" to R.drawable.ic_youtube),
                "yandex_music" to ("Яндекс" to R.drawable.ic_yandex)
            )

            sources.forEach { (srcKey, pair) ->
                val (label, iconRes) = pair
                val isSelected = selectedSource == srcKey && !isUserSearchMode

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        isUserSearchMode = false
                        selectedSource = srcKey
                        if (searchQuery.isNotBlank() && !searchQuery.startsWith("Leaf:", ignoreCase = true)) {
                            executeSearch(searchQuery)
                        }
                    },
                    leadingIcon = if (iconRes != null) {
                        {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = label,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        containerColor = ObsidianCard
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Results Area
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
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isUserSearchMode) "Поиск пользователей..." else "Ищем треки...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                searchError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = searchError ?: "Ошибка", color = MaterialTheme.colorScheme.error)
                    }
                }

                // USER SEARCH RESULTS (when query starts with Leaf:)
                isUserSearchMode && hasSearched -> {
                    if (userResults.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Пользователи не найдены", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Проверьте правильность логина после «Leaf:»", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userResults, key = { it.login }) { userItem ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val avUrl = repository.resolveAvatarUrl(userItem.avatarUrl)
                                            if (!avUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = avUrl,
                                                    contentDescription = userItem.username,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = userItem.username?.takeIf { it.isNotBlank() } ?: userItem.login,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = TextPrimary
                                                )
                                                if (userItem.verified == true) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.CheckCircle, contentDescription = "Верифицирован", tint = accentColor, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text(
                                                text = "@${userItem.login}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }

                                        if (userItem.online == true) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(LeafGreen)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TRACK SEARCH RESULTS
                hasSearched && searchResults.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Попробуйте изменить запрос или источник", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                hasSearched && searchResults.isNotEmpty() -> {
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
                                onTrackClick = { playerManager.playTrack(track, searchResults) },
                                onLikeClick = { scope.launch { repository.toggleLike(track) } },
                                onAddToPlaylistClick = { onAddToPlaylist(track) }
                            )
                        }
                    }
                }

                // HISTORY & POPULAR QUERIES (when not searched yet)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Search History Section
                        if (searchHistory.isNotEmpty()) {
                            item {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "История поиска",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextSecondary
                                        )
                                        TextButton(onClick = { repository.clearSearchHistory() }) {
                                            Text("Очистить", color = TextTertiary, fontSize = 12.sp)
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        searchHistory.forEach { historyQuery ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        searchQuery = historyQuery
                                                        executeSearch(historyQuery)
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = historyQuery,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { repository.removeSearchQuery(historyQuery) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Popular Genres / Discovery Prompts
                        item {
                            Column {
                                Text(
                                    text = "Популярные жанры",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
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
                                                executeSearch(tag)
                                            }
                                        ) {
                                            Text(
                                                text = "# $tag",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
