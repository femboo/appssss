package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ObsidianCard
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class NavTab(val title: String) {
    HOME("Главная"),
    SEARCH("Поиск"),
    LIBRARY("Медиатека"),
    PROFILE("Профиль")
}

@Composable
fun MainScreen(
    repository: LeafRepository,
    playerManager: AudioPlayerManager
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoggedIn by repository.isLoggedIn.collectAsState()
    val playlists by repository.playlists.collectAsState()
    val likedIds by repository.likedTrackIds.collectAsState()

    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isLoadingAudio by playerManager.isLoading.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val playerError by playerManager.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var selectedPlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var trackForPlaylistAdd by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistFromAdd by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }

    // Show errors in snackbar
    LaunchedEffect(playerError) {
        playerError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            playerManager.clearError()
        }
    }

    // Add to playlist dialog
    trackForPlaylistAdd?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onSelectPlaylist = { pl ->
                trackForPlaylistAdd = null
                scope.launch {
                    val res = repository.addTrackToPlaylist(pl.id, track)
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Добавлено в «${pl.name}»")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Ошибка добавления")
                    }
                }
            },
            onCreateNewPlaylistClick = {
                showCreatePlaylistFromAdd = true
            },
            onDismiss = { trackForPlaylistAdd = null }
        )
    }

    if (showCreatePlaylistFromAdd) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistFromAdd = false },
            onCreate = { name ->
                showCreatePlaylistFromAdd = false
                val pendingTrack = trackForPlaylistAdd
                scope.launch {
                    val res = repository.createPlaylist(name)
                    if (res.isSuccess) {
                        val newPl = res.getOrNull()
                        if (newPl != null && pendingTrack != null) {
                            repository.addTrackToPlaylist(newPl.id, pendingTrack)
                            snackbarHostState.showSnackbar("Плейлист создан, трек добавлен")
                            trackForPlaylistAdd = null
                        } else {
                            snackbarHostState.showSnackbar("Плейлист «$name» создан")
                        }
                    }
                }
            }
        )
    }

    // Full Player Modal Sheet
    if (showFullPlayer && currentTrack != null) {
        val activeTrack = currentTrack!!
        FullPlayerSheet(
            track = activeTrack,
            isPlaying = isPlaying,
            isLoading = isLoadingAudio,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isLiked = likedIds.contains(activeTrack.id),
            queueCount = queue.size,
            onPlayPauseClick = { playerManager.togglePlayPause() },
            onNextClick = { playerManager.playNext() },
            onPreviousClick = { playerManager.playPrevious() },
            onSeekTo = { pos -> playerManager.seekTo(pos) },
            onLikeClick = {
                scope.launch { repository.toggleLike(activeTrack) }
            },
            onAddToPlaylistClick = {
                trackForPlaylistAdd = activeTrack
            },
            onDismiss = { showFullPlayer = false }
        )
    }

    // If user is not logged in, show AuthScreen!
    if (!isLoggedIn) {
        AuthScreen(
            repository = repository,
            onAuthSuccess = {
                selectedTab = NavTab.HOME
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(ObsidianSurface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini Player Bar (shown whenever there is an active track)
                AnimatedVisibility(
                    visible = currentTrack != null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    currentTrack?.let { activeTrack ->
                        MiniPlayer(
                            track = activeTrack,
                            isPlaying = isPlaying,
                            isLoading = isLoadingAudio,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            isLiked = likedIds.contains(activeTrack.id),
                            onPlayPauseClick = { playerManager.togglePlayPause() },
                            onNextClick = { playerManager.playNext() },
                            onLikeClick = {
                                scope.launch { repository.toggleLike(activeTrack) }
                            },
                            onClick = { showFullPlayer = true }
                        )
                    }
                }

                // Floating rounded Bottom Navigation
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .testTag("main_bottom_nav"),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(60.dp)
                    ) {
                        val activeColor = MaterialTheme.colorScheme.primary
                        val onPrimary = MaterialTheme.colorScheme.onPrimary

                        // Home
                        NavigationBarItem(
                            selected = selectedTab == NavTab.HOME && selectedPlaylistDetail == null,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedTab = NavTab.HOME
                            },
                            icon = {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_home),
                                    contentDescription = "Главная",
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            label = { Text("Главная", fontSize = 11.sp, fontWeight = if (selectedTab == NavTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = onPrimary,
                                selectedTextColor = activeColor,
                                indicatorColor = activeColor,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )

                        // Search
                        NavigationBarItem(
                            selected = selectedTab == NavTab.SEARCH && selectedPlaylistDetail == null,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedTab = NavTab.SEARCH
                            },
                            icon = {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_search),
                                    contentDescription = "Поиск",
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            label = { Text("Поиск", fontSize = 11.sp, fontWeight = if (selectedTab == NavTab.SEARCH) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = onPrimary,
                                selectedTextColor = activeColor,
                                indicatorColor = activeColor,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_search")
                        )

                        // Library (Playlists + Likes unified)
                        NavigationBarItem(
                            selected = selectedTab == NavTab.LIBRARY || selectedPlaylistDetail != null,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedTab = NavTab.LIBRARY
                            },
                            icon = {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_bookshelf),
                                    contentDescription = "Медиатека",
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            label = { Text("Медиатека", fontSize = 11.sp, fontWeight = if (selectedTab == NavTab.LIBRARY) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = onPrimary,
                                selectedTextColor = activeColor,
                                indicatorColor = activeColor,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )

                        // Profile
                        NavigationBarItem(
                            selected = selectedTab == NavTab.PROFILE && selectedPlaylistDetail == null,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedTab = NavTab.PROFILE
                            },
                            icon = {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_settings),
                                    contentDescription = "Профиль",
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            label = { Text("Профиль", fontSize = 11.sp, fontWeight = if (selectedTab == NavTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = onPrimary,
                                selectedTextColor = activeColor,
                                indicatorColor = activeColor,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                }
            }
        },
        containerColor = ObsidianBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedPlaylistDetail != null) {
                PlaylistDetailScreen(
                    playlist = selectedPlaylistDetail!!,
                    repository = repository,
                    playerManager = playerManager,
                    onBack = { selectedPlaylistDetail = null },
                    onAddToPlaylist = { track -> trackForPlaylistAdd = track }
                )
            } else {
                when (selectedTab) {
                    NavTab.HOME -> HomeScreen(
                        repository = repository,
                        playerManager = playerManager,
                        onNavigateToLibrary = { selectedTab = NavTab.LIBRARY },
                        onNavigateToLiked = { selectedTab = NavTab.LIBRARY },
                        onAddToPlaylist = { track -> trackForPlaylistAdd = track }
                    )
                    NavTab.SEARCH -> SearchScreen(
                        repository = repository,
                        playerManager = playerManager,
                        onAddToPlaylist = { track -> trackForPlaylistAdd = track }
                    )
                    NavTab.LIBRARY -> LibraryScreen(
                        repository = repository,
                        playerManager = playerManager,
                        onPlaylistClick = { pl -> selectedPlaylistDetail = pl },
                        onAddToPlaylist = { track -> trackForPlaylistAdd = track }
                    )
                    NavTab.PROFILE -> ProfileScreen(
                        repository = repository,
                        onLogoutSuccess = {
                            playerManager.stop()
                        }
                    )
                }
            }
        }
    }
}
