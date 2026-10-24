package com.example.data.api

import com.example.data.model.AvatarResponse
import com.example.data.model.CreatePlaylistRequest
import com.example.data.model.GenericOkResponse
import com.example.data.model.LikedTracksResponse
import com.example.data.model.LoginRequest
import com.example.data.model.PendingVerificationResponse
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTracksResponse
import com.example.data.model.PlaylistsResponse
import com.example.data.model.RegisterRequest
import com.example.data.model.ResendRequest
import com.example.data.model.SearchResponse
import com.example.data.model.StreamResponse
import com.example.data.model.ThemeItem
import com.example.data.model.ThemesResponse
import com.example.data.model.Track
import com.example.data.model.TracksResponse
import com.example.data.model.UpdateThemeRequest
import com.example.data.model.UserProfile
import com.example.data.model.VerifyRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface LeafApiService {

    // Auth
    @POST("api/register")
    suspend fun register(@Body req: RegisterRequest): Response<PendingVerificationResponse>

    @POST("api/register/verify")
    suspend fun verifyRegister(@Body req: VerifyRequest): Response<UserProfile>

    @POST("api/register/resend")
    suspend fun resendRegister(@Body req: ResendRequest): Response<GenericOkResponse>

    @POST("api/login")
    suspend fun login(@Body req: LoginRequest): Response<PendingVerificationResponse>

    @POST("api/login/verify")
    suspend fun verifyLogin(@Body req: VerifyRequest): Response<UserProfile>

    @POST("api/login/resend")
    suspend fun resendLogin(@Body req: ResendRequest): Response<GenericOkResponse>

    @GET("api/me")
    suspend fun getMe(): Response<UserProfile>

    @POST("api/logout")
    suspend fun logout(): Response<GenericOkResponse>

    // Recommendations & Discovery
    @GET("api/recommendations")
    suspend fun getRecommendations(): Response<TracksResponse>

    @GET("api/my-wave")
    suspend fun getMyWave(@Query("page") page: Int = 0): Response<TracksResponse>

    @GET("api/discover/local")
    suspend fun getLocalDiscover(@Query("country") country: String? = "Russia"): Response<TracksResponse>

    // Playlists
    @GET("api/playlists")
    suspend fun getPlaylists(): Response<PlaylistsResponse>

    @POST("api/playlists")
    suspend fun createPlaylist(@Body req: CreatePlaylistRequest): Response<Playlist>

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): Response<GenericOkResponse>

    @GET("api/playlists/{id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("id") id: String,
        @Query("sort") sort: String? = "added_desc"
    ): Response<PlaylistTracksResponse>

    @POST("api/playlists/{id}/tracks")
    suspend fun addTrackToPlaylist(
        @Path("id") id: String,
        @Body track: Track
    ): Response<GenericOkResponse>

    @DELETE("api/playlists/{id}/tracks/{track_id}")
    suspend fun removeTrackFromPlaylist(
        @Path("id") id: String,
        @Path("track_id") trackId: String
    ): Response<GenericOkResponse>

    // Liked
    @GET("api/liked")
    suspend fun getLiked(
        @Query("sort") sort: String? = "added_desc"
    ): Response<LikedTracksResponse>

    @POST("api/liked")
    suspend fun addLiked(@Body track: Track): Response<GenericOkResponse>

    @DELETE("api/liked/{track_id}")
    suspend fun removeLiked(@Path("track_id") trackId: String): Response<GenericOkResponse>

    // Search
    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("source") source: String? = null
    ): Response<SearchResponse>

    // Suggestions
    @GET("api/suggest")
    suspend fun getSuggestions(@Query("q") query: String): Response<com.example.data.model.SuggestionsResponse>

    // Users Search
    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<com.example.data.model.UsersSearchResponse>

    // Themes & Settings
    @GET("api/themes")
    suspend fun getThemes(): Response<ThemesResponse>

    @POST("api/settings/theme")
    suspend fun updateTheme(@Body req: UpdateThemeRequest): Response<GenericOkResponse>

    @Multipart
    @POST("api/account/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<AvatarResponse>

    @DELETE("api/account/avatar")
    suspend fun deleteAvatar(): Response<GenericOkResponse>

    // Stream
    @GET("api/stream/{track_id}")
    suspend fun getStream(@Path("track_id") trackId: String): Response<StreamResponse>

    // Lyrics
    @GET("api/lyrics")
    suspend fun getLyrics(
        @Query("title") title: String,
        @Query("artist") artist: String,
        @Query("duration") duration: Long? = null
    ): Response<com.example.data.model.LyricsResponse>

    // Public User Profiles & Library
    @GET("api/users/{login}/profile")
    suspend fun getUserProfile(@Path("login") login: String): Response<com.example.data.model.UserPublicProfile>

    @GET("api/users/{login}/library")
    suspend fun getUserLibrary(@Path("login") login: String): Response<com.example.data.model.UserLibraryResponse>

    @GET("api/users/{login}/liked")
    suspend fun getUserLikedTracks(@Path("login") login: String): Response<com.example.data.model.LikedTracksResponse>

    @GET("api/users/{login}/playlists/{id}/tracks")
    suspend fun getUserPlaylistTracks(
        @Path("login") login: String,
        @Path("id") playlistId: String
    ): Response<com.example.data.model.PlaylistTracksResponse>

    // Activity ping
    @POST("api/streak/ping")
    suspend fun pingStreak(@Body body: Map<String, Int> = mapOf("seconds" to 15)): Response<Any>
}

