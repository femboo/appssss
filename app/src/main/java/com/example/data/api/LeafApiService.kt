package com.example.data.api

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
import com.example.data.model.Track
import com.example.data.model.UserProfile
import com.example.data.model.VerifyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

    // Stream
    @GET("api/stream/{track_id}")
    suspend fun getStream(@Path("track_id") trackId: String): Response<StreamResponse>

    // Activity ping
    @POST("api/streak/ping")
    suspend fun pingStreak(@Body body: Map<String, Int> = mapOf("seconds" to 15)): Response<Any>
}
