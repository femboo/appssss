package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.model.CreatePlaylistRequest
import com.example.data.model.LoginRequest
import com.example.data.model.PendingVerificationResponse
import com.example.data.model.Playlist
import com.example.data.model.RegisterRequest
import com.example.data.model.ResendRequest
import com.example.data.model.ThemeItem
import com.example.data.model.Track
import com.example.data.model.UpdateThemeRequest
import com.example.data.model.UserProfile
import com.example.data.model.VerifyRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AppThemeState(
    val isDark: Boolean = true,
    val colorTheme: String = "green",
    val accentHex: String = "#22C55E"
)

class LeafRepository private constructor(private val context: Context) {
    private val apiClient = ApiClient.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<Track>>(emptyList())
    val likedTracks: StateFlow<List<Track>> = _likedTracks.asStateFlow()

    private val _likedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val likedTrackIds: StateFlow<Set<String>> = _likedTrackIds.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Track>>(emptyList())
    val recommendations: StateFlow<List<Track>> = _recommendations.asStateFlow()

    private val _myWaveTracks = MutableStateFlow<List<Track>>(emptyList())
    val myWaveTracks: StateFlow<List<Track>> = _myWaveTracks.asStateFlow()

    private val _localDiscoverTracks = MutableStateFlow<List<Track>>(emptyList())
    val localDiscoverTracks: StateFlow<List<Track>> = _localDiscoverTracks.asStateFlow()

    private val _availableThemes = MutableStateFlow<List<ThemeItem>>(emptyList())
    val availableThemes: StateFlow<List<ThemeItem>> = _availableThemes.asStateFlow()

    private val themePrefs = context.getSharedPreferences("leaf_theme_prefs", Context.MODE_PRIVATE)
    private val searchPrefs = context.getSharedPreferences("leaf_search_history", Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(
        AppThemeState(
            isDark = themePrefs.getBoolean("is_dark", true),
            colorTheme = themePrefs.getString("color_theme", "green") ?: "green",
            accentHex = themePrefs.getString("accent_hex", "#22C55E") ?: "#22C55E"
        )
    )
    val themeState: StateFlow<AppThemeState> = _themeState.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: LeafRepository? = null

        fun getInstance(context: Context): LeafRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LeafRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        loadSearchHistoryInternal()
        scope.launch {
            checkAuthStatus()
        }
    }

    suspend fun checkAuthStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getService().getMe()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                _currentUser.value = profile
                _isLoggedIn.value = true
                if (!profile.themeColor.isNullOrBlank()) {
                    _themeState.value = _themeState.value.copy(accentHex = profile.themeColor)
                }
                syncLibrary()
                true
            } else {
                _currentUser.value = null
                _isLoggedIn.value = false
                false
            }
        } catch (_: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
            false
        }
    }

    suspend fun login(login: String, gmail: String, pass: String): Result<PendingVerificationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val resp = apiClient.getService().login(LoginRequest(login, gmail, pass))
                if (resp.isSuccessful && resp.body() != null) {
                    Result.success(resp.body()!!)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun verifyLogin(gmail: String, code: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val resp = apiClient.getService().verifyLogin(VerifyRequest(gmail, code))
                if (resp.isSuccessful && resp.body() != null) {
                    val user = resp.body()!!
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    syncLibrary()
                    Result.success(user)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun resendLogin(gmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().resendLogin(ResendRequest(gmail))
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseError(resp.errorBody()?.string())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        login: String,
        username: String,
        gmail: String,
        pass: String,
        passConfirm: String
    ): Result<PendingVerificationResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().register(
                RegisterRequest(login, username, gmail, pass, passConfirm, agreeToTerms = true)
            )
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                val err = parseError(resp.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyRegister(gmail: String, code: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val resp = apiClient.getService().verifyRegister(VerifyRequest(gmail, code))
                if (resp.isSuccessful && resp.body() != null) {
                    val user = resp.body()!!
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    syncLibrary()
                    Result.success(user)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun resendRegister(gmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().resendRegister(ResendRequest(gmail))
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseError(resp.errorBody()?.string())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiClient.getService().logout()
        } catch (_: Exception) {}
        apiClient.cookieJar.clearCookies()
        _currentUser.value = null
        _isLoggedIn.value = false
        _playlists.value = emptyList()
        _likedTracks.value = emptyList()
        _likedTrackIds.value = emptySet()
        Result.success(Unit)
    }

    // Synchronization
    suspend fun syncLibrary() = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            refreshLikedInternal()
            refreshPlaylistsInternal()
            refreshRecommendations()
            refreshMyWave()
            refreshLocalDiscover()
            refreshThemes()
        } catch (_: Exception) {
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun refreshPlaylists(): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        try {
            val list = refreshPlaylistsInternal()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshPlaylistsInternal(): List<Playlist> {
        val resp = apiClient.getService().getPlaylists()
        if (resp.isSuccessful && resp.body() != null) {
            val list = resp.body()!!.playlists
            _playlists.value = list
            return list
        }
        return emptyList()
    }

    suspend fun createPlaylist(name: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().createPlaylist(CreatePlaylistRequest(name))
            if (resp.isSuccessful && resp.body() != null) {
                val created = resp.body()!!
                _playlists.value = _playlists.value + created
                Result.success(created)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().deletePlaylist(id)
            if (resp.isSuccessful) {
                _playlists.value = _playlists.value.filter { it.id != id }
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistTracks(playlistId: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getPlaylistTracks(playlistId)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!.tracks)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().addTrackToPlaylist(playlistId, track)
            if (resp.isSuccessful) {
                // Increment playlist track count locally
                _playlists.value = _playlists.value.map { pl ->
                    if (pl.id == playlistId) pl.copy(trackCount = pl.trackCount + 1) else pl
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().removeTrackFromPlaylist(playlistId, trackId)
            if (resp.isSuccessful) {
                _playlists.value = _playlists.value.map { pl ->
                    if (pl.id == playlistId) pl.copy(trackCount = maxOf(0, pl.trackCount - 1)) else pl
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Liked songs
    suspend fun refreshLiked(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val list = refreshLikedInternal()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshLikedInternal(): List<Track> {
        val resp = apiClient.getService().getLiked()
        if (resp.isSuccessful && resp.body() != null) {
            val list = resp.body()!!.tracks
            _likedTracks.value = list
            _likedTrackIds.value = list.map { it.id }.toSet()
            return list
        }
        return emptyList()
    }

    suspend fun toggleLike(track: Track): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentlyLiked = _likedTrackIds.value.contains(track.id)
        val newLiked = !currentlyLiked
        
        // Optimistic UI update
        val prevIds = _likedTrackIds.value
        val prevTracks = _likedTracks.value
        if (newLiked) {
            _likedTrackIds.value = prevIds + track.id
            _likedTracks.value = listOf(track) + prevTracks.filter { it.id != track.id }
        } else {
            _likedTrackIds.value = prevIds - track.id
            _likedTracks.value = prevTracks.filter { it.id != track.id }
        }

        try {
            val resp = if (newLiked) {
                apiClient.getService().addLiked(track)
            } else {
                apiClient.getService().removeLiked(track.id)
            }
            if (resp.isSuccessful) {
                Result.success(newLiked)
            } else {
                // Revert optimistic update
                _likedTrackIds.value = prevIds
                _likedTracks.value = prevTracks
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            _likedTrackIds.value = prevIds
            _likedTracks.value = prevTracks
            Result.failure(e)
        }
    }

    // Search
    suspend fun search(query: String, source: String? = null): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val reqSource = if (source == "all" || source == null) null else source
            val resp = apiClient.getService().search(query, reqSource)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!.tracks)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Stream URL resolution
    suspend fun resolveStreamUrl(trackId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getStream(trackId)
            if (resp.isSuccessful && resp.body() != null) {
                val resolved = apiClient.resolveMediaUrl(resp.body()!!.url)
                Result.success(resolved)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string()) ?: "Не удалось получить аудиопоток"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pingStreak(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiClient.getService().pingStreak()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recommendations & Home Feed
    suspend fun refreshRecommendations(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getRecommendations()
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!.tracks
                _recommendations.value = list
                Result.success(list)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshMyWave(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getMyWave(0)
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!.tracks
                _myWaveTracks.value = list
                Result.success(list)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshLocalDiscover(country: String = "Russia"): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getLocalDiscover(country)
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!.tracks
                _localDiscoverTracks.value = list
                Result.success(list)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Themes & Profile Settings
    suspend fun refreshThemes(): Result<List<ThemeItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getThemes()
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!.themes
                _availableThemes.value = list
                Result.success(list)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setThemeLocally(isDark: Boolean, colorTheme: String, accentHex: String? = null) {
        val hex = accentHex ?: when (colorTheme.lowercase()) {
            "green" -> "#22C55E"
            "blue" -> "#3B82F6"
            "white" -> "#F2F2F2"
            "red" -> "#EF4444"
            "purple" -> "#A855F7"
            else -> "#22C55E"
        }
        _themeState.value = AppThemeState(isDark = isDark, colorTheme = colorTheme, accentHex = hex)
        themePrefs.edit()
            .putBoolean("is_dark", isDark)
            .putString("color_theme", colorTheme)
            .putString("accent_hex", hex)
            .apply()
    }

    suspend fun updateTheme(themeName: String, accentColor: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val isDark = themeName != "light"
        val detectedColorTheme = when (accentColor?.lowercase()) {
            "#22c55e", "#00e676" -> "green"
            "#3b82f6" -> "blue"
            "#f2f2f2", "#ffffff" -> "white"
            "#ef4444" -> "red"
            "#a855f7" -> "purple"
            else -> _themeState.value.colorTheme
        }
        val hex = accentColor ?: when (detectedColorTheme) {
            "green" -> "#22C55E"
            "blue" -> "#3B82F6"
            "white" -> "#F2F2F2"
            "red" -> "#EF4444"
            "purple" -> "#A855F7"
            else -> _themeState.value.accentHex
        }

        setThemeLocally(isDark, detectedColorTheme, hex)

        try {
            val resp = apiClient.getService().updateTheme(UpdateThemeRequest(if (isDark) "dark" else "light", hex))
            if (resp.isSuccessful) {
                _currentUser.value = _currentUser.value?.copy(theme = if (isDark) "dark" else "light", themeColor = hex)
            }
        } catch (_: Exception) {
            // Local state is already updated
        }
        Result.success(Unit)
    }

    // Search Suggestions
    suspend fun getSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiClient.getService().getSuggestions(query)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!.suggestions)
            } else {
                Result.success(emptyList())
            }
        } catch (_: Exception) {
            Result.success(emptyList())
        }
    }

    // Users Search for Leaf: prefix
    suspend fun searchUsers(query: String): Result<List<com.example.data.model.UserSearchItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.removePrefix("Leaf:").removePrefix("leaf:").trim()
            val resp = apiClient.getService().searchUsers(cleanQuery)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!.users)
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search History
    private fun loadSearchHistoryInternal() {
        val raw = searchPrefs.getString("queries", "") ?: ""
        val list = if (raw.isBlank()) emptyList() else raw.split(";;;").filter { it.isNotBlank() }
        _searchHistory.value = list
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(15)
        _searchHistory.value = limited
        searchPrefs.edit().putString("queries", limited.joinToString(";;;")).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = _searchHistory.value.filter { it != query }
        _searchHistory.value = current
        searchPrefs.edit().putString("queries", current.joinToString(";;;")).apply()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        searchPrefs.edit().remove("queries").apply()
    }

    suspend fun uploadAvatar(imageBytes: ByteArray, filename: String = "avatar.jpg"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val reqBody = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", filename, reqBody)
            val resp = apiClient.getService().uploadAvatar(part)
            if (resp.isSuccessful && resp.body() != null) {
                val avatarUrl = resp.body()!!.avatarUrl
                _currentUser.value = _currentUser.value?.copy(avatarUrl = avatarUrl)
                Result.success(avatarUrl ?: "")
            } else {
                Result.failure(Exception(parseError(resp.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resolveAvatarUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        return apiClient.resolveMediaUrl(rawUrl)
    }

    fun getBaseUrl(): String = apiClient.getBaseUrl()

    fun setBaseUrl(url: String) {
        apiClient.setBaseUrl(url)
    }

    private fun parseError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Произошла ошибка запроса"
        return try {
            val json = JSONObject(errorBody)
            if (json.has("detail")) {
                val detail = json.get("detail")
                if (detail is JSONObject) {
                    detail.optString("message", detail.toString())
                } else {
                    detail.toString()
                }
            } else {
                json.optString("message", errorBody)
            }
        } catch (_: Exception) {
            errorBody
        }
    }
}
