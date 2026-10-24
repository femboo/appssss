package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import kotlinx.coroutines.launch

enum class NavTab(val title: String) {
    HOME("Главная"),
    SEARCH("Поиск"),
    LIBRARY("Медиатека"),
    PROFILE("Настройки")
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
    var selectedUserProfileLogin by remember { mutableStateOf<String?>(null) }
    var selectedUserProfileName by remember { mutableStateOf<String?>(null) }
    var selectedUserProfileAvatar by remember { mutableStateOf<String?>(null) }

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

    // Full Player Modal Sheet (with lyrics support!)
    if (showFullPlayer && currentTrack != null) {
        val activeTrack = currentTrack!!
        FullPlayerSheet(
            track = activeTrack,
            repository = repository,
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

    val activeAccent = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini Player Bar
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

                // Clean, soft-rounded Navigation Bar (spacious, never overlapping)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_nav"),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(68.dp)
                    ) {
                        val isDetailActive = selectedPlaylistDetail != null || selectedUserProfileLogin != null

                        // Home
                        val homeSelected = selectedTab == NavTab.HOME && !isDetailActive
                        NavigationBarItem(
                            selected = homeSelected,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedUserProfileLogin = null
                                selectedTab = NavTab.HOME
                            },
                            icon = {
                                Image(
                                    painter = painterResource(R.drawable.ic_home),
                                    contentDescription = "Главная",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Главная",
                                    fontSize = 11.sp,
                                    fontWeight = if (homeSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeAccent,
                                selectedTextColor = activeAccent,
                                indicatorColor = activeAccent.copy(alpha = 0.18f),
                                unselectedIconColor = onSurfaceVariant,
                                unselectedTextColor = onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )

                        // Search
                        val searchSelected = selectedTab == NavTab.SEARCH && !isDetailActive
                        NavigationBarItem(
                            selected = searchSelected,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedUserProfileLogin = null
                                selectedTab = NavTab.SEARCH
                            },
                            icon = {
                                Image(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Поиск",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Поиск",
                                    fontSize = 11.sp,
                                    fontWeight = if (searchSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeAccent,
                                selectedTextColor = activeAccent,
                                indicatorColor = activeAccent.copy(alpha = 0.18f),
                                unselectedIconColor = onSurfaceVariant,
                                unselectedTextColor = onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_search")
                        )

                        // Library
                        val librarySelected = (selectedTab == NavTab.LIBRARY || selectedPlaylistDetail != null) && selectedUserProfileLogin == null
                        NavigationBarItem(
                            selected = librarySelected,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedUserProfileLogin = null
                                selectedTab = NavTab.LIBRARY
                            },
                            icon = {
                                Image(
                                    painter = painterResource(R.drawable.ic_bookshelf),
                                    contentDescription = "Медиатека",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Медиатека",
                                    fontSize = 11.sp,
                                    fontWeight = if (librarySelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeAccent,
                                selectedTextColor = activeAccent,
                                indicatorColor = activeAccent.copy(alpha = 0.18f),
                                unselectedIconColor = onSurfaceVariant,
                                unselectedTextColor = onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )

                        // Profile / Settings
                        val profileSelected = selectedTab == NavTab.PROFILE && !isDetailActive
                        NavigationBarItem(
                            selected = profileSelected,
                            onClick = {
                                selectedPlaylistDetail = null
                                selectedUserProfileLogin = null
                                selectedTab = NavTab.PROFILE
                            },
                            icon = {
                                Image(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = "Настройки",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Настройки",
                                    fontSize = 11.sp,
                                    fontWeight = if (profileSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeAccent,
                                selectedTextColor = activeAccent,
                                indicatorColor = activeAccent.copy(alpha = 0.18f),
                                unselectedIconColor = onSurfaceVariant,
                                unselectedTextColor = onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                selectedUserProfileLogin != null -> {
                    UserProfileScreen(
                        userLogin = selectedUserProfileLogin!!,
                        initialUsername = selectedUserProfileName,
                        initialAvatarUrl = selectedUserProfileAvatar,
                        repository = repository,
                        playerManager = playerManager,
                        onBack = {
                            selectedUserProfileLogin = null
                            selectedUserProfileName = null
                            selectedUserProfileAvatar = null
                        },
                        onAddToPlaylist = { track -> trackForPlaylistAdd = track }
                    )
                }
                selectedPlaylistDetail != null -> {
                    PlaylistDetailScreen(
                        playlist = selectedPlaylistDetail!!,
                        repository = repository,
                        playerManager = playerManager,
                        onBack = { selectedPlaylistDetail = null },
                        onAddToPlaylist = { track -> trackForPlaylistAdd = track }
                    )
                }
                else -> {
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
                            onAddToPlaylist = { track -> trackForPlaylistAdd = track },
                            onOpenUserProfile = { login, name, av ->
                                selectedUserProfileLogin = login
                                selectedUserProfileName = name
                                selectedUserProfileAvatar = av
                            }
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
}
