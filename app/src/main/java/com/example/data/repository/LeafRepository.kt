package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.model.CreatePlaylistRequest
import com.example.data.model.LoginRequest
import com.example.data.model.PendingVerificationResponse
import com.example.data.model.Playlist
import com.example.data.model.RegisterRequest
import com.example.data.model.ResendRequest
import com.example.data.model.Track
import com.example.data.model.UserProfile
import com.example.data.model.VerifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
