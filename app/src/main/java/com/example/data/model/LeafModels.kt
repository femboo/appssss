package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Track(
    @Json(name = "id") val id: String,
    @Json(name = "source") val source: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artwork_url") val artworkUrl: String? = null,
    @Json(name = "duration") val duration: Long? = 0L,
    @Json(name = "permalink_url") val permalinkUrl: String? = null
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "Без названия"

    val displayArtist: String
        get() = artist?.takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель"

    val formattedDuration: String
        get() {
            val ms = duration ?: 0L
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

@JsonClass(generateAdapter = true)
data class Playlist(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "track_count") val trackCount: Int = 0,
    @Json(name = "is_public") val isPublic: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "login") val login: String = "",
    @Json(name = "username") val username: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "background_url") val backgroundUrl: String? = null,
    @Json(name = "theme") val theme: String? = null,
    @Json(name = "theme_color") val themeColor: String? = null,
    @Json(name = "verified") val verified: Boolean? = false,
    @Json(name = "is_admin") val isAdmin: Boolean? = false,
    @Json(name = "badges") val badges: List<String>? = emptyList(),
    @Json(name = "subscription") val subscription: Map<String, Any>? = null
)

// Request bodies
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "login") val login: String,
    @Json(name = "username") val username: String,
    @Json(name = "gmail") val gmail: String,
    @Json(name = "password") val password: String,
    @Json(name = "password_confirm") val passwordConfirm: String,
    @Json(name = "agree_to_terms") val agreeToTerms: Boolean = true
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "login") val login: String,
    @Json(name = "gmail") val gmail: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class VerifyRequest(
    @Json(name = "gmail") val gmail: String,
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class ResendRequest(
    @Json(name = "gmail") val gmail: String
)

@JsonClass(generateAdapter = true)
data class CreatePlaylistRequest(
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class UpdateThemeRequest(
    @Json(name = "theme") val theme: String,
    @Json(name = "theme_color") val themeColor: String? = null
)

// Response wrappers
@JsonClass(generateAdapter = true)
data class PendingVerificationResponse(
    @Json(name = "pending_verification") val pendingVerification: Boolean? = true,
    @Json(name = "gmail") val gmail: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistsResponse(
    @Json(name = "playlists") val playlists: List<Playlist> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlaylistTracksResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "tracks") val tracks: List<Track> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LikedTracksResponse(
    @Json(name = "tracks") val tracks: List<Track> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TracksResponse(
    @Json(name = "tracks") val tracks: List<Track> = emptyList(),
    @Json(name = "country") val country: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "tracks") val tracks: List<Track> = emptyList(),
    @Json(name = "has_more") val hasMore: Boolean? = false,
    @Json(name = "sc_offset") val scOffset: Any? = null,
    @Json(name = "yt_offset") val ytOffset: Any? = null,
    @Json(name = "ym_offset") val ymOffset: Any? = null
)

@JsonClass(generateAdapter = true)
data class StreamResponse(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class GenericOkResponse(
    @Json(name = "ok") val ok: Boolean? = true,
    @Json(name = "removed") val removed: Int? = null
)

@JsonClass(generateAdapter = true)
data class ThemeItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "accent_color") val accentColor: String? = null,
    @Json(name = "required_tier") val requiredTier: String? = null
)

@JsonClass(generateAdapter = true)
data class ThemesResponse(
    @Json(name = "themes") val themes: List<ThemeItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AvatarResponse(
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SuggestionsResponse(
    @Json(name = "suggestions") val suggestions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UserSearchItem(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "login") val login: String = "",
    @Json(name = "username") val username: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "verified") val verified: Boolean? = false,
    @Json(name = "dev_badge") val devBadge: Boolean? = false,
    @Json(name = "online") val online: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class UsersSearchResponse(
    @Json(name = "users") val users: List<UserSearchItem> = emptyList()
)
